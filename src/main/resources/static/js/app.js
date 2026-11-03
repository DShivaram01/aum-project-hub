
const app = document.getElementById("app");
const nav = document.getElementById("nav");

function h(tag, attrs, ...children) {
    const element = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs || {})) {
        if (value === null || value === undefined || value === false) {
            continue;
        }
        if (key.startsWith("on")) {
            element.addEventListener(key.substring(2).toLowerCase(), value);
        } else if (key === "className") {
            element.className = value;
        } else if (key === "value") {
            element.value = value;
        } else {
            element.setAttribute(key, value === true ? "" : value);
        }
    }
    for (const child of children.flat()) {
        if (child === null || child === undefined || child === false) {
            continue;
        }
        element.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return element;
}

function render(...nodes) {
    app.replaceChildren(...nodes.filter(Boolean));
}

function toast(message) {
    const box = document.getElementById("toast");
    box.textContent = message;
    box.classList.remove("hidden");
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => box.classList.add("hidden"), 3000);
}

function badge(status) {
    return h("span", {className: "badge " + status},
        h("span", {className: "dot", "aria-hidden": "true"}),
        status.charAt(0) + status.slice(1).toLowerCase());
}

// calendar-style date: short month over the day number
function dateBlock(value, lead) {
    const date = new Date(value);
    return h("div", {className: "date-block" + (lead ? " lead" : ""), "aria-hidden": "true"},
        h("span", {className: "date-month"}, date.toLocaleDateString(undefined, {month: "short"})),
        h("span", {className: "date-day"}, String(date.getDate())));
}

function countdown(value) {
    const closed = new Date(value) <= Date.now();
    return h("span", {className: "countdown" + (closed ? " closed" : "")}, timeLeft(value));
}

function formatDate(value) {
    return value
        ? new Date(value).toLocaleDateString(undefined, {month: "short", day: "numeric", year: "numeric"})
        : "";
}

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString([], {dateStyle: "medium", timeStyle: "short"}) : "";
}

function timeLeft(value) {
    const ms = new Date(value) - Date.now();
    if (ms <= 0) {
        return "Entries closed";
    }
    const days = Math.floor(ms / 86400000);
    if (days >= 1) {
        return "Closes in " + days + (days === 1 ? " day" : " days");
    }
    const hours = Math.max(1, Math.floor(ms / 3600000));
    return "Closes in " + hours + (hours === 1 ? " hour" : " hours");
}

// datetime-local inputs want "YYYY-MM-DDTHH:mm" in local time
function toLocalInput(value) {
    if (!value) {
        return null;
    }
    const date = new Date(value);
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 16);
}

function intentText(submission) {
    return [submission.present && "Presenting", submission.publish && "Publishing"].filter(Boolean).join(" + ");
}

function errorText(error) {
    return h("p", {className: "error"}, error.message);
}

function pageHeader(title, intro, action) {
    return h("div", {className: action ? "page-header with-action" : "page-header"},
        h("div", {}, h("h1", {}, title), intro ? h("p", {className: "page-intro"}, intro) : null),
        action);
}

function skillTags(skills) {
    const list = (skills || "").split(",").map((skill) => skill.trim()).filter(Boolean);
    if (list.length === 0) {
        return null;
    }
    return h("ul", {className: "skill-list"}, list.map((skill) => h("li", {className: "skill-tag"}, skill)));
}

function formData(form) {
    const data = {};
    for (const [key, value] of new FormData(form).entries()) {
        data[key] = value.trim() === "" ? null : value.trim();
    }
    return data;
}

function requireLogin() {
    if (!Api.getToken()) {
        location.hash = "#/login";
        return false;
    }
    return true;
}

function renderNav() {
    const user = Api.getUser();
    const path = (location.hash.slice(1) || "/").split("?")[0];
    const link = (href, text) => h("a", {
        href: "#" + href,
        className: path === href || (href !== "/" && path.startsWith(href + "/")) ? "active" : null
    }, text);
    const links = [link("/", "Browse"), link("/events", "Events")];

    if (user) {
        links.push(link("/new", "New project"));
        links.push(link("/me", "My account"));
        links.push(h("button", {
            onClick: () => {
                Api.logout();
                location.hash = "#/";
                toast("Logged out");
            }
        }, "Log out"));
    } else {
        links.push(link("/login", "Log in"));
        links.push(link("/register", "Register"));
    }
    nav.replaceChildren(...links);
}

let demoInfo;

async function demoBox() {
    if (demoInfo === undefined) {
        try {
            demoInfo = await Api.get("/api/demo");
        } catch (error) {
            demoInfo = null;
        }
    }
    if (!demoInfo || Api.getToken()) {
        return null;
    }

    // Labels look like "Faculty - Dr. Maya Patel"; the button shows the role, the tooltip shows the name.
    const buttons = demoInfo.accounts.map((account) => h("button", {
        className: "secondary",
        title: account.label,
        onClick: async () => {
            try {
                Api.saveLogin(await Api.post("/api/auth/login",
                    {email: account.email, password: demoInfo.password}));
                toast("Logged in as " + account.label);
                location.hash = "#/me";
            } catch (error) {
                toast(error.message);
            }
        }
    }, "Log in as " + account.label.split(" - ")[0]));

    return h("section", {className: "demo-panel"},
        h("div", {className: "demo-text"},
            h("h2", {}, "Try a demo account"),
            h("p", {}, "No sign-up needed. The faculty account reviews join requests, the student account sends them."),
            h("p", {className: "demo-note"}, "Demo password: ", h("code", {}, demoInfo.password),
                " · Demo environment uses HTTP. Use the provided demo accounts rather than personal credentials.")),
        h("div", {className: "demo-actions"}, buttons));
}

async function browsePage() {
    const params = new URLSearchParams(location.hash.split("?")[1] || "");
    const list = h("div", {className: "project-list"}, h("p", {className: "empty"}, "Loading projects..."));
    const results = h("div", {className: "results-bar"});

    const form = h("form", {
            className: "filter-bar",
            onSubmit: (event) => {
                event.preventDefault();
                const query = new URLSearchParams();
                for (const [key, value] of Object.entries(formData(form))) {
                    if (value) {
                        query.set(key, value);
                    }
                }
                location.hash = "#/?" + query.toString();
            }
        },
        h("input", {name: "skill", "aria-label": "Skill", placeholder: "Skill (e.g. Java)", value: params.get("skill") || ""}),
        h("input", {name: "category", "aria-label": "Category", placeholder: "Category (e.g. Data Science)",
            value: params.get("category") || ""}),
        h("select", {name: "status", "aria-label": "Status"},
            h("option", {value: ""}, "Any status"),
            h("option", {value: "OPEN", selected: params.get("status") === "OPEN"}, "Open"),
            h("option", {value: "CLOSED", selected: params.get("status") === "CLOSED"}, "Closed")),
        h("select", {name: "sort", "aria-label": "Sort"},
            h("option", {value: ""}, "Newest first"),
            h("option", {value: "hot", selected: params.get("sort") === "hot"}, "Most upvoted")),
        h("button", {type: "submit"}, "Filter"));

    render(
        pageHeader("Projects at AUM",
            "Research and side projects posted by AUM faculty and students. Ask to join, upvote the ones you like, or leave a tip."),
        await demoBox(),
        h("div", {className: "browse-layout"},
            h("div", {},
                h("section", {className: "filter-panel"}, h("h2", {className: "section-label"}, "Find projects"), form),
                results,
                list),
            await deadlinesPanel()));

    try {
        const projects = await Api.get("/api/projects?" + params.toString());
        results.replaceChildren(
            h("span", {}, projects.length + (projects.length === 1 ? " project" : " projects")),
            params.toString() ? h("a", {href: "#/"}, "Clear filters") : "");
        if (projects.length === 0) {
            list.replaceChildren(h("p", {className: "empty"}, "No projects match these filters."));
            return;
        }
        list.replaceChildren(...projects.map(projectCard));
    } catch (error) {
        list.replaceChildren(errorText(error));
    }
}

async function deadlinesPanel() {
    let events;
    try {
        events = (await Api.get("/api/events")).filter((event) => event.acceptingSubmissions).slice(0, 3);
    } catch (error) {
        return null;
    }
    if (events.length === 0) {
        return null;
    }
    return h("aside", {className: "deadlines"},
        h("h2", {}, "Upcoming deadlines"),
        h("ul", {className: "deadline-list"}, events.map((event, index) => h("li", {},
            dateBlock(event.submissionDeadline, index === 0),
            h("div", {className: "deadline-text"},
                h("a", {href: "#/events/" + event.id}, event.title),
                countdown(event.submissionDeadline))))),
        h("a", {href: "#/events", className: "quiet-link"}, "All events"));
}

function hotBadge(project) {
    return project.hot
        ? h("span", {className: "badge HOT"}, h("span", {className: "dot", "aria-hidden": "true"}), "Hot")
        : null;
}

function projectCard(project) {
    const card = h("article", {className: "project-card"},
        upvoteButton(project, (updated) => card.replaceWith(projectCard(updated))),
        h("div", {className: "project-body"},
            h("div", {className: "project-card-head"},
                h("h3", {className: "project-title"}, h("a", {href: "#/projects/" + project.id}, project.title)),
                h("div", {className: "card-badges"}, hotBadge(project), badge(project.status))),
            h("p", {className: "project-meta"},
                [project.category, project.ownerName, formatDate(project.createdAt)].filter(Boolean).join(" · ")),
            project.description ? h("p", {className: "project-excerpt"}, project.description) : null,
            skillTags(project.requiredSkills)));
    return card;
}

async function projectPage(id) {
    render(h("p", {className: "empty"}, "Loading..."));

    let project;
    try {
        project = await Api.get("/api/projects/" + id);
    } catch (error) {
        render(errorText(error));
        return;
    }

    const user = Api.getUser();
    const isOwner = user && user.id === project.ownerId;

    const details = h("section", {className: "card"},
        h("div", {className: "detail-head"},
            h("h1", {}, project.title),
            h("div", {className: "card-badges"}, hotBadge(project), badge(project.status))),
        h("p", {className: "project-meta"},
            [project.category, "Posted by " + project.ownerName, formatDate(project.createdAt)]
                .filter(Boolean).join(" · ")),
        h("p", {className: "detail-description"}, project.description),
        project.requiredSkills
            ? h("div", {className: "detail-skills"},
                h("h2", {className: "section-label"}, "Required skills"), skillTags(project.requiredSkills))
            : null,
        upvoteControl(project));

    const sections = [h("a", {href: "#/", className: "back-link"}, "← All projects"), details];

    if (isOwner) {
        details.append(h("div", {className: "actions"},
            h("button", {onClick: () => location.hash = "#/projects/" + id + "/edit"}, "Edit"),
            h("button", {className: "danger", onClick: () => deleteProject(id)}, "Delete")));
        sections.push(await ownerRequestsSection(id));
    } else if (user && project.status === "OPEN") {
        sections.push(joinSection(id));
    } else if (!user) {
        sections.push(h("p", {className: "meta login-hint"},
            h("a", {href: "#/login"}, "Log in"), " to request to join."));
    }
    sections.push(await projectEventsSection(project, isOwner));
    sections.push(await tipsSection(project, user, isOwner));

    render(...sections);
}

// Any logged-in user, student or faculty, can upvote; the server rejects upvotes on your own project.
function upvoteButton(project, onUpdated, text) {
    const user = Api.getUser();
    const isOwner = user && user.id === project.ownerId;
    const title = !user ? "Log in to upvote"
        : isOwner ? "You can't upvote your own project"
            : project.upvotedByMe ? "Remove your upvote" : "Upvote this project";

    const button = h("button", {
        type: "button",
        className: "upvote" + (text ? " inline" : "") + (project.upvotedByMe ? " on" : ""),
        "aria-pressed": project.upvotedByMe ? "true" : "false",
        "aria-label": title,
        title,
        disabled: isOwner,
        onClick: async (event) => {
            event.preventDefault();
            if (!user) {
                location.hash = "#/login";
                return;
            }
            button.disabled = true;
            try {
                const path = "/api/projects/" + project.id + "/upvote";
                onUpdated(await (project.upvotedByMe ? Api.del(path) : Api.put(path)));
            } catch (error) {
                toast(error.message);
                button.disabled = false;
            }
        }
    }, h("span", {className: "upvote-arrow", "aria-hidden": "true"}, "▲"),
        h("span", {className: "upvote-label"}, text || String(project.upvotes)));
    return button;
}

function upvoteControl(project) {
    const label = project.upvotes + (project.upvotes === 1 ? " upvote" : " upvotes");
    return h("div", {className: "upvotes"},
        upvoteButton(project, () => projectPage(project.id), project.upvotedByMe ? "Upvoted" : "Upvote"),
        h("span", {}, label));
}

function intentChecks() {
    return h("div", {className: "checks"},
        h("label", {className: "check"}, h("input", {type: "checkbox", name: "present", checked: true}),
            "Present it at the event"),
        h("label", {className: "check"}, h("input", {type: "checkbox", name: "publish"}),
            "Publish it (write-up, proceedings, journal)"));
}

function eventSelect(events, optional) {
    return h("label", {}, "Event",
        h("select", {name: "eventId", required: !optional},
            optional ? h("option", {value: ""}, "None for now") : null,
            events.map((event) => h("option", {value: event.id},
                event.title + " (due " + formatDate(event.submissionDeadline) + ")"))));
}

function readIntent(form) {
    const values = new FormData(form);
    return {
        eventId: values.get("eventId") ? Number(values.get("eventId")) : null,
        present: values.get("present") !== null,
        publish: values.get("publish") !== null
    };
}

async function projectEventsSection(project, isOwner) {
    const section = h("section", {className: "card"}, h("h2", {}, "Events"));

    try {
        const submissions = await Api.get("/api/projects/" + project.id + "/submissions");
        if (submissions.length === 0) {
            section.append(h("p", {className: "empty"}, "Not entered in any events yet."));
        }
        for (const submission of submissions) {
            const canWithdraw = isOwner && new Date(submission.submissionDeadline) > Date.now();
            section.append(h("div", {className: "row"},
                h("div", {className: "row-head"},
                    h("div", {},
                        h("a", {href: "#/events/" + submission.eventId}, submission.eventTitle),
                        h("span", {className: "badge"}, intentText(submission))),
                    canWithdraw ? h("button", {
                        className: "secondary small",
                        onClick: async () => {
                            try {
                                await Api.del("/api/submissions/" + submission.id);
                                toast("Entry withdrawn");
                                projectPage(project.id);
                            } catch (error) {
                                toast(error.message);
                            }
                        }
                    }, "Withdraw") : null),
                h("div", {className: "meta"}, formatDateTime(submission.eventStartsAt))));
        }

        if (isOwner) {
            const entered = new Set(submissions.map((submission) => submission.eventId));
            const open = (await Api.get("/api/events"))
                .filter((event) => event.acceptingSubmissions && !entered.has(event.id));
            if (open.length > 0) {
                const status = h("div");
                const form = h("form", {
                        className: "stack sub-form",
                        onSubmit: async (event) => {
                            event.preventDefault();
                            try {
                                await Api.post("/api/projects/" + project.id + "/submissions", readIntent(form));
                                toast("Project entered");
                                projectPage(project.id);
                            } catch (error) {
                                status.replaceChildren(errorText(error));
                            }
                        }
                    },
                    h("h3", {}, "Enter this project in an upcoming event"),
                    eventSelect(open, false),
                    intentChecks(),
                    h("button", {type: "submit"}, "Enter project"),
                    status);
                section.append(form);
            }
        }
    } catch (error) {
        section.append(errorText(error));
    }
    return section;
}

async function tipsSection(project, user, isOwner) {
    const section = h("section", {className: "card"},
        h("h2", {}, "Tips"),
        h("p", {className: "meta card-intro"}, "Suggestions, pointers, or things to watch out for on this idea."));

    try {
        const tips = await Api.get("/api/projects/" + project.id + "/tips");
        if (tips.length === 0) {
            section.append(h("p", {className: "empty"}, "No tips yet."));
        }
        for (const tip of tips) {
            const canDelete = user && (user.id === tip.authorId || isOwner);
            section.append(h("div", {className: "row"},
                h("p", {className: "tip-content"}, tip.content),
                h("div", {className: "row-head"},
                    h("span", {className: "meta"},
                        tip.authorName + " (" + tip.authorRole.toLowerCase() + ") · " + formatDate(tip.createdAt)),
                    canDelete ? h("button", {
                        className: "secondary small",
                        onClick: async () => {
                            try {
                                await Api.del("/api/tips/" + tip.id);
                                projectPage(project.id);
                            } catch (error) {
                                toast(error.message);
                            }
                        }
                    }, "Delete") : null)));
        }
    } catch (error) {
        section.append(errorText(error));
    }

    if (user) {
        const status = h("div");
        const form = h("form", {
                className: "stack sub-form",
                onSubmit: async (event) => {
                    event.preventDefault();
                    try {
                        await Api.post("/api/projects/" + project.id + "/tips", formData(form));
                        toast("Tip shared");
                        projectPage(project.id);
                    } catch (error) {
                        status.replaceChildren(errorText(error));
                    }
                }
            },
            h("textarea", {name: "content", required: true, maxlength: 1000, placeholder: "Share a tip",
                "aria-label": "Tip"}),
            h("button", {type: "submit"}, "Share tip"),
            status);
        section.append(form);
    } else {
        section.append(h("p", {className: "meta"}, h("a", {href: "#/login"}, "Log in"), " to share a tip."));
    }
    return section;
}

function joinSection(projectId) {
    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                try {
                    await Api.post("/api/projects/" + projectId + "/join-requests", formData(form));
                    toast("Request sent");
                    form.replaceChildren(h("p", {}, "Request sent. Track it under ", h("a", {href: "#/me"}, "My account"), "."));
                } catch (error) {
                    status.replaceChildren(errorText(error));
                }
            }
        },
        h("label", {}, "Why do you want to join?", h("textarea", {name: "message", maxlength: 1000})),
        h("button", {type: "submit"}, "Request to join"),
        status);

    return h("section", {className: "card"}, h("h2", {}, "Join this project"), form);
}

async function ownerRequestsSection(projectId) {
    const list = h("div", {className: "request-list"});
    const section = h("section", {className: "card"}, h("h2", {}, "Join requests"), list);

    try {
        const requests = await Api.get("/api/projects/" + projectId + "/join-requests");
        if (requests.length === 0) {
            list.append(h("p", {className: "empty"}, "No requests yet."));
        }
        for (const request of requests) {
            const row = h("div", {className: "request-row"},
                h("div", {className: "request-head"}, h("strong", {}, request.userName), badge(request.status)),
                h("div", {className: "meta"}, [request.userEmail, request.userSkills].filter(Boolean).join(" · ")),
                request.message ? h("p", {className: "request-message"}, request.message) : null);

            if (request.status === "PENDING") {
                row.append(h("div", {className: "actions"},
                    h("button", {onClick: () => decide(request.id, "ACCEPTED", projectId)}, "Accept"),
                    h("button", {className: "secondary", onClick: () => decide(request.id, "REJECTED", projectId)},
                        "Reject")));
            }
            list.append(row);
        }
    } catch (error) {
        list.append(errorText(error));
    }
    return section;
}

async function decide(requestId, status, projectId) {
    try {
        await Api.put("/api/join-requests/" + requestId, {status});
        toast("Request " + status.toLowerCase());
        projectPage(projectId);
    } catch (error) {
        toast(error.message);
    }
}

async function deleteProject(id) {
    if (!confirm("Delete this project and all its join requests?")) {
        return;
    }
    try {
        await Api.del("/api/projects/" + id);
        toast("Project deleted");
        location.hash = "#/";
    } catch (error) {
        toast(error.message);
    }
}

async function projectFormPage(id) {
    if (!requireLogin()) {
        return;
    }

    let project = {};
    let openEvents = [];
    try {
        if (id) {
            project = await Api.get("/api/projects/" + id);
        } else {
            openEvents = (await Api.get("/api/events")).filter((event) => event.acceptingSubmissions);
        }
    } catch (error) {
        render(errorText(error));
        return;
    }

    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                const data = formData(form);
                delete data.eventId;
                delete data.present;
                delete data.publish;
                try {
                    const saved = id
                        ? await Api.put("/api/projects/" + id, data)
                        : await Api.post("/api/projects", data);
                    const intent = id ? null : readIntent(form);
                    if (intent && intent.eventId) {
                        try {
                            await Api.post("/api/projects/" + saved.id + "/submissions", intent);
                        } catch (error) {
                            toast("Project created, but it wasn't entered in the event: " + error.message);
                            location.hash = "#/projects/" + saved.id;
                            return;
                        }
                    }
                    toast(id ? "Project updated" : "Project created");
                    location.hash = "#/projects/" + saved.id;
                } catch (error) {
                    status.replaceChildren(errorText(error));
                }
            }
        },
        h("label", {}, "Title", h("input", {name: "title", required: true, maxlength: 255, value: project.title})),
        h("label", {}, "Description",
            h("textarea", {name: "description", required: true, maxlength: 2000, value: project.description})),
        h("label", {}, "Category", h("input", {name: "category", maxlength: 255, value: project.category})),
        h("label", {}, h("span", {}, "Required skills ", h("span", {className: "hint"}, "(comma separated)")),
            h("input", {name: "requiredSkills", maxlength: 255, value: project.requiredSkills})),
        h("label", {}, "Status",
            h("select", {name: "status"},
                h("option", {value: "OPEN", selected: project.status !== "CLOSED"}, "Open"),
                h("option", {value: "CLOSED", selected: project.status === "CLOSED"}, "Closed"))),
        openEvents.length > 0
            ? h("fieldset", {}, h("legend", {}, "Enter it in an upcoming event (optional)"),
                eventSelect(openEvents, true), intentChecks())
            : null,
        h("button", {type: "submit"}, id ? "Save changes" : "Create project"),
        status);

    render(
        id ? h("a", {href: "#/projects/" + id, className: "back-link"}, "← Back to project") : null,
        pageHeader(id ? "Edit project" : "New project"),
        h("section", {className: "card form-card"}, form));
}

async function eventsPage() {
    const user = Api.getUser();
    const list = h("div", {className: "project-list"}, h("p", {className: "empty"}, "Loading..."));

    render(
        pageHeader("Upcoming events",
            "Showcases, hackathons and publications coming up. Enter a project to present or publish it.",
            user && user.role === "FACULTY"
                ? h("button", {onClick: () => location.hash = "#/events/new"}, "New event")
                : null),
        list);

    try {
        const events = await Api.get("/api/events");
        list.replaceChildren(...(events.length
            ? events.map(eventCard)
            : [h("p", {className: "empty"}, "No upcoming events.")]));
    } catch (error) {
        list.replaceChildren(errorText(error));
    }
}

function eventCard(event, index) {
    return h("article", {className: "event-card"},
        dateBlock(event.startsAt, index === 0),
        h("div", {className: "project-body"},
            h("div", {className: "project-card-head"},
                h("h3", {className: "project-title"}, h("a", {href: "#/events/" + event.id}, event.title)),
                countdown(event.submissionDeadline)),
            h("p", {className: "project-meta"},
                [formatDateTime(event.startsAt), event.location, event.createdByName].filter(Boolean).join(" · ")),
            h("p", {className: "event-meta"}, "Entries due " + formatDateTime(event.submissionDeadline) + " · "
                + event.submissionCount + (event.submissionCount === 1 ? " project entered" : " projects entered"))));
}

async function eventPage(id) {
    render(h("p", {className: "empty"}, "Loading..."));

    let event;
    let submissions;
    try {
        [event, submissions] = await Promise.all([
            Api.get("/api/events/" + id), Api.get("/api/events/" + id + "/submissions")]);
    } catch (error) {
        render(errorText(error));
        return;
    }

    const user = Api.getUser();
    const isCreator = user && user.id === event.createdById;

    const details = h("section", {className: "card"},
        h("div", {className: "detail-head event-head"},
            dateBlock(event.startsAt, true), h("h1", {}, event.title), countdown(event.submissionDeadline)),
        h("p", {className: "project-meta"},
            [formatDateTime(event.startsAt), event.location, "Organized by " + event.createdByName]
                .filter(Boolean).join(" · ")),
        event.description ? h("p", {className: "detail-description"}, event.description) : null,
        h("p", {className: "event-meta"}, "Entries due ",
            formatDateTime(event.submissionDeadline)));

    if (isCreator) {
        details.append(h("div", {className: "actions"},
            h("button", {onClick: () => location.hash = "#/events/" + id + "/edit"}, "Edit"),
            h("button", {className: "danger", onClick: () => deleteEvent(id)}, "Delete")));
    }

    const entryList = (title, items) => h("section", {className: "card"},
        h("h2", {}, title + " (" + items.length + ")"),
        items.length
            ? items.map((submission) => h("div", {className: "row"},
                h("a", {href: "#/projects/" + submission.projectId}, submission.projectTitle),
                h("div", {className: "meta"}, submission.ownerName)))
            : h("p", {className: "empty"}, "Nothing yet."));

    const sections = [h("a", {href: "#/events", className: "back-link"}, "← All events"), details,
        entryList("Presenting", submissions.filter((submission) => submission.present)),
        entryList("Publishing", submissions.filter((submission) => submission.publish))];

    if (user && event.acceptingSubmissions) {
        sections.push(await enterOwnProjectSection(event, submissions, user));
    } else if (!user && event.acceptingSubmissions) {
        sections.push(h("p", {className: "meta"}, h("a", {href: "#/login"}, "Log in"), " to enter one of your projects."));
    }
    render(...sections);
}

async function enterOwnProjectSection(event, submissions, user) {
    const section = h("section", {className: "card"}, h("h2", {}, "Enter one of your projects"));
    try {
        const entered = new Set(submissions.map((submission) => submission.projectId));
        const mine = (await Api.get("/api/projects"))
            .filter((project) => project.ownerId === user.id && !entered.has(project.id));
        if (mine.length === 0) {
            section.append(h("p", {className: "empty"}, "You have no projects left to enter. ",
                h("a", {href: "#/new"}, "Post a project"), " first."));
            return section;
        }

        const status = h("div");
        const form = h("form", {
                className: "stack",
                onSubmit: async (submitEvent) => {
                    submitEvent.preventDefault();
                    const intent = readIntent(form);
                    intent.eventId = event.id;
                    try {
                        await Api.post("/api/projects/" + new FormData(form).get("projectId") + "/submissions", intent);
                        toast("Project entered");
                        eventPage(event.id);
                    } catch (error) {
                        status.replaceChildren(errorText(error));
                    }
                }
            },
            h("label", {}, "Project",
                h("select", {name: "projectId", required: true},
                    mine.map((project) => h("option", {value: project.id}, project.title)))),
            intentChecks(),
            h("button", {type: "submit"}, "Enter project"),
            status);
        section.append(form);
    } catch (error) {
        section.append(errorText(error));
    }
    return section;
}

async function deleteEvent(id) {
    if (!confirm("Delete this event? Projects entered in it will be removed from it.")) {
        return;
    }
    try {
        await Api.del("/api/events/" + id);
        toast("Event deleted");
        location.hash = "#/events";
    } catch (error) {
        toast(error.message);
    }
}

async function eventFormPage(id) {
    if (!requireLogin()) {
        return;
    }

    let event = {};
    if (id) {
        try {
            event = await Api.get("/api/events/" + id);
        } catch (error) {
            render(errorText(error));
            return;
        }
    }

    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (submitEvent) => {
                submitEvent.preventDefault();
                const data = formData(form);
                for (const key of ["startsAt", "submissionDeadline"]) {
                    data[key] = data[key] ? new Date(data[key]).toISOString() : null;
                }
                try {
                    const saved = id
                        ? await Api.put("/api/events/" + id, data)
                        : await Api.post("/api/events", data);
                    toast(id ? "Event updated" : "Event created");
                    location.hash = "#/events/" + saved.id;
                } catch (error) {
                    status.replaceChildren(errorText(error));
                }
            }
        },
        h("label", {}, "Title", h("input", {name: "title", required: true, maxlength: 255, value: event.title})),
        h("label", {}, "Description",
            h("textarea", {name: "description", maxlength: 2000, value: event.description})),
        h("label", {}, "Location", h("input", {name: "location", maxlength: 255, value: event.location})),
        h("label", {}, "Starts at",
            h("input", {name: "startsAt", type: "datetime-local", required: true, value: toLocalInput(event.startsAt)})),
        h("label", {}, "Submission deadline",
            h("input", {
                name: "submissionDeadline", type: "datetime-local", required: true,
                value: toLocalInput(event.submissionDeadline)
            })),
        h("button", {type: "submit"}, id ? "Save changes" : "Create event"),
        status);

    render(
        h("a", {href: id ? "#/events/" + id : "#/events", className: "back-link"}, id ? "← Back to event" : "← All events"),
        pageHeader(id ? "Edit event" : "New event"),
        h("section", {className: "card form-card"}, form));
}

async function loginPage() {
    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                try {
                    Api.saveLogin(await Api.post("/api/auth/login", formData(form)));
                    toast("Logged in");
                    location.hash = "#/";
                } catch (error) {
                    status.replaceChildren(errorText(error));
                }
            }
        },
        h("label", {}, "Email", h("input", {name: "email", type: "email", required: true})),
        h("label", {}, "Password", h("input", {name: "password", type: "password", required: true})),
        h("button", {type: "submit"}, "Log in"),
        status);

    render(h("div", {className: "auth-wrap"},
        h("section", {className: "card"},
            h("h1", {}, "Log in"),
            h("p", {className: "page-intro"}, "Use your AUM Project Hub account."),
            form,
            h("p", {className: "auth-switch"}, "No account? ", h("a", {href: "#/register"}, "Register"))),
        await demoBox()));
}

function registerPage() {
    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                const data = formData(form);
                try {
                    await Api.post("/api/auth/register", data);
                    Api.saveLogin(await Api.post("/api/auth/login", {email: data.email, password: data.password}));
                    toast("Account created");
                    location.hash = "#/me";
                } catch (error) {
                    status.replaceChildren(errorText(error));
                }
            }
        },
        h("label", {}, "Name", h("input", {name: "name", required: true, maxlength: 255})),
        h("label", {}, "Email", h("input", {name: "email", type: "email", required: true, maxlength: 255})),
        h("label", {}, h("span", {}, "Password ", h("span", {className: "hint"}, "(8–72 characters)")),
            h("input", {name: "password", type: "password", required: true, minlength: 8, maxlength: 72})),
        h("label", {}, "Role",
            h("select", {name: "role"},
                h("option", {value: "STUDENT"}, "Student"),
                h("option", {value: "FACULTY"}, "Faculty"))),
        h("label", {}, "Department", h("input", {name: "department", maxlength: 255})),
        h("button", {type: "submit"}, "Create account"),
        status);

    render(h("div", {className: "auth-wrap"},
        h("section", {className: "card"},
            h("h1", {}, "Create an account"),
            h("p", {className: "page-intro"}, "Students can request to join projects. Faculty can post them."),
            form,
            h("p", {className: "auth-switch"}, "Already registered? ", h("a", {href: "#/login"}, "Log in")))));
}

async function accountPage() {
    if (!requireLogin()) {
        return;
    }

    let me;
    try {
        me = await Api.get("/api/users/me");
        Api.saveUser(me);
    } catch (error) {
        render(errorText(error));
        return;
    }

    const status = h("div");
    const profileForm = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                try {
                    Api.saveUser(await Api.put("/api/users/" + me.id, formData(profileForm)));
                    status.replaceChildren();
                    toast("Profile saved");
                } catch (error) {
                    status.replaceChildren(errorText(error));
                }
            }
        },
        h("label", {}, "Name", h("input", {name: "name", required: true, maxlength: 255, value: me.name})),
        h("label", {}, "Department", h("input", {name: "department", maxlength: 255, value: me.department})),
        h("label", {}, h("span", {}, "Skills ", h("span", {className: "hint"}, "(comma separated)")),
            h("input", {name: "skills", maxlength: 255, value: me.skills})),
        h("label", {}, "Bio", h("textarea", {name: "bio", maxlength: 1000, value: me.bio})),
        h("button", {type: "submit"}, "Save profile"),
        status);

    const myProjects = h("div", {className: "project-list"}, h("p", {className: "empty"}, "Loading..."));
    const myRequests = h("div", {className: "request-list"}, h("p", {className: "empty"}, "Loading..."));

    render(
        h("div", {className: "page-header"},
            h("h1", {}, me.name),
            h("p", {className: "page-intro"}, me.email + " · ", badge(me.role))),
        h("div", {className: "account-grid"},
            h("section", {className: "card"}, h("h2", {}, "Profile"), profileForm),
            h("div", {className: "account-main"},
                h("section", {className: "card"}, h("h2", {}, "My projects"), myProjects),
                h("section", {className: "card"}, h("h2", {}, "My join requests"), myRequests))));

    try {
        const all = await Api.get("/api/projects");
        const owned = all.filter((project) => project.ownerId === me.id);
        myProjects.replaceChildren(...(owned.length
            ? owned.map(projectCard)
            : [h("p", {className: "empty"}, "You have not posted any projects.")]));
    } catch (error) {
        myProjects.replaceChildren(errorText(error));
    }

    try {
        const requests = await Api.get("/api/join-requests/mine");
        myRequests.replaceChildren(...(requests.length
            ? requests.map(myRequestRow)
            : [h("p", {className: "empty"}, "You have not requested to join any projects.")]));
    } catch (error) {
        myRequests.replaceChildren(errorText(error));
    }
}

function myRequestRow(request) {
    const row = h("div", {className: "request-row"},
        h("div", {className: "request-head"},
            h("a", {href: "#/projects/" + request.projectId}, request.projectTitle), badge(request.status)),
        h("div", {className: "meta"}, "Sent " + formatDate(request.createdAt)));

    if (request.status === "PENDING") {
        row.append(h("div", {className: "actions"},
            h("button", {
                className: "secondary",
                onClick: async () => {
                    try {
                        await Api.del("/api/join-requests/" + request.id);
                        toast("Request withdrawn");
                        accountPage();
                    } catch (error) {
                        toast(error.message);
                    }
                }
            }, "Withdraw")));
    }
    return row;
}

function route() {
    renderNav();
    const path = (location.hash.slice(1) || "/").split("?")[0];
    let match;

    if (path === "/") {
        browsePage();
    } else if (path === "/login") {
        loginPage();
    } else if (path === "/register") {
        registerPage();
    } else if (path === "/new") {
        projectFormPage(null);
    } else if (path === "/me") {
        accountPage();
    } else if (path === "/events") {
        eventsPage();
    } else if (path === "/events/new") {
        eventFormPage(null);
    } else if ((match = path.match(/^\/events\/(\d+)\/edit$/))) {
        eventFormPage(match[1]);
    } else if ((match = path.match(/^\/events\/(\d+)$/))) {
        eventPage(match[1]);
    } else if ((match = path.match(/^\/projects\/(\d+)\/edit$/))) {
        projectFormPage(match[1]);
    } else if ((match = path.match(/^\/projects\/(\d+)$/))) {
        projectPage(match[1]);
    } else {
        render(pageHeader("Page not found"), h("a", {href: "#/"}, "Back to projects"));
    }
}

window.addEventListener("hashchange", route);
route();
