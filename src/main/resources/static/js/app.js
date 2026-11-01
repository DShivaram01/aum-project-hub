
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
    return h("span", {className: "badge " + status}, status);
}

function formatDate(value) {
    return value ? new Date(value).toLocaleDateString() : "";
}

function errorText(error) {
    return h("p", {className: "error"}, error.message);
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
    const links = [h("a", {href: "#/"}, "Browse")];

    if (user) {
        links.push(h("a", {href: "#/new"}, "New project"));
        links.push(h("a", {href: "#/me"}, "My account"));
        links.push(h("button", {
            onClick: () => {
                Api.logout();
                location.hash = "#/";
                toast("Logged out");
            }
        }, "Log out"));
    } else {
        links.push(h("a", {href: "#/login"}, "Log in"));
        links.push(h("a", {href: "#/register"}, "Register"));
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

    const buttons = demoInfo.accounts.map((account) => h("button", {
        className: "secondary",
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
    }, "Log in as " + account.label));

    return h("div", {className: "card demo"},
        h("strong", {}, "Just looking around? "),
        "Use a demo account. Faculty can review join requests, students can send them.",
        h("div", {className: "actions"}, buttons),
        h("div", {className: "meta"}, "Demo password: " + demoInfo.password));
}

async function browsePage() {
    const params = new URLSearchParams(location.hash.split("?")[1] || "");
    const list = h("div", {}, h("p", {className: "empty"}, "Loading..."));

    const form = h("form", {
            className: "filters",
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
        h("input", {name: "skill", placeholder: "Skill, e.g. java", value: params.get("skill") || ""}),
        h("input", {name: "category", placeholder: "Category, e.g. Web", value: params.get("category") || ""}),
        h("select", {name: "status"},
            h("option", {value: ""}, "Any status"),
            h("option", {value: "OPEN", selected: params.get("status") === "OPEN"}, "Open"),
            h("option", {value: "CLOSED", selected: params.get("status") === "CLOSED"}, "Closed")),
        h("button", {type: "submit"}, "Filter"));

    render(h("h1", {}, "Projects"), await demoBox(), form, list);

    try {
        const projects = await Api.get("/api/projects?" + params.toString());
        if (projects.length === 0) {
            list.replaceChildren(h("p", {className: "empty"}, "No projects match these filters."));
            return;
        }
        list.replaceChildren(...projects.map(projectCard));
    } catch (error) {
        list.replaceChildren(errorText(error));
    }
}

function projectCard(project) {
    return h("div", {className: "card"},
        h("h3", {}, h("a", {href: "#/projects/" + project.id}, project.title), " ", badge(project.status)),
        h("div", {className: "meta"},
            [project.category, "by " + project.ownerName, formatDate(project.createdAt)].filter(Boolean).join(" · ")),
        project.requiredSkills ? h("p", {}, "Skills: " + project.requiredSkills) : null);
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

    const details = h("div", {className: "card"},
        h("h1", {}, project.title, " ", badge(project.status)),
        h("div", {className: "meta"},
            [project.category, "Posted by " + project.ownerName, formatDate(project.createdAt)]
                .filter(Boolean).join(" · ")),
        h("p", {}, project.description),
        project.requiredSkills ? h("p", {}, h("strong", {}, "Skills: "), project.requiredSkills) : null);

    const sections = [details];

    if (isOwner) {
        details.append(h("div", {className: "actions"},
            h("button", {onClick: () => location.hash = "#/projects/" + id + "/edit"}, "Edit"),
            h("button", {className: "danger", onClick: () => deleteProject(id)}, "Delete")));
        sections.push(await ownerRequestsSection(id));
    } else if (user && project.status === "OPEN") {
        sections.push(joinSection(id));
    } else if (!user) {
        sections.push(h("p", {}, h("a", {href: "#/login"}, "Log in"), " to request to join."));
    }

    render(...sections);
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

    return h("div", {className: "card"}, h("h3", {}, "Join this project"), form);
}

async function ownerRequestsSection(projectId) {
    const section = h("div", {className: "card"}, h("h3", {}, "Join requests"));

    try {
        const requests = await Api.get("/api/projects/" + projectId + "/join-requests");
        if (requests.length === 0) {
            section.append(h("p", {className: "empty"}, "No requests yet."));
        }
        for (const request of requests) {
            const row = h("div", {className: "card"},
                h("strong", {}, request.userName), " ", badge(request.status),
                h("div", {className: "meta"}, [request.userEmail, request.userSkills].filter(Boolean).join(" · ")),
                request.message ? h("p", {}, request.message) : null);

            if (request.status === "PENDING") {
                row.append(h("div", {className: "actions"},
                    h("button", {onClick: () => decide(request.id, "ACCEPTED", projectId)}, "Accept"),
                    h("button", {className: "secondary", onClick: () => decide(request.id, "REJECTED", projectId)},
                        "Reject")));
            }
            section.append(row);
        }
    } catch (error) {
        section.append(errorText(error));
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
    if (id) {
        try {
            project = await Api.get("/api/projects/" + id);
        } catch (error) {
            render(errorText(error));
            return;
        }
    }

    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                try {
                    const saved = id
                        ? await Api.put("/api/projects/" + id, formData(form))
                        : await Api.post("/api/projects", formData(form));
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
        h("label", {}, "Required skills (comma separated)",
            h("input", {name: "requiredSkills", maxlength: 255, value: project.requiredSkills})),
        h("label", {}, "Status",
            h("select", {name: "status"},
                h("option", {value: "OPEN", selected: project.status !== "CLOSED"}, "Open"),
                h("option", {value: "CLOSED", selected: project.status === "CLOSED"}, "Closed"))),
        h("button", {type: "submit"}, id ? "Save changes" : "Create project"),
        status);

    render(h("h1", {}, id ? "Edit project" : "New project"), form);
}

async function loginPage() {
    const status = h("div");
    const form = h("form", {
            className: "stack",
            onSubmit: async (event) => {
                event.preventDefault();
                try {
                    Api.saveLogin(await Api.post("/api/auth/login", formData(form)));
                    toast("Welcome back");
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

    render(h("h1", {}, "Log in"), form,
        h("p", {}, "No account? ", h("a", {href: "#/register"}, "Register")),
        await demoBox());
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
        h("label", {}, "Password (8-72 characters)",
            h("input", {name: "password", type: "password", required: true, minlength: 8, maxlength: 72})),
        h("label", {}, "Role",
            h("select", {name: "role"},
                h("option", {value: "STUDENT"}, "Student"),
                h("option", {value: "FACULTY"}, "Faculty"))),
        h("label", {}, "Department", h("input", {name: "department", maxlength: 255})),
        h("button", {type: "submit"}, "Create account"),
        status);

    render(h("h1", {}, "Register"), form);
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
        h("label", {}, "Skills (comma separated)", h("input", {name: "skills", maxlength: 255, value: me.skills})),
        h("label", {}, "Bio", h("textarea", {name: "bio", maxlength: 1000, value: me.bio})),
        h("button", {type: "submit"}, "Save profile"),
        status);

    const myProjects = h("div", {}, h("p", {className: "empty"}, "Loading..."));
    const myRequests = h("div", {}, h("p", {className: "empty"}, "Loading..."));

    render(
        h("h1", {}, me.name),
        h("p", {className: "meta"}, me.email + " · " + me.role),
        h("div", {className: "card"}, h("h3", {}, "Profile"), profileForm),
        h("div", {className: "card"}, h("h3", {}, "My projects"), myProjects),
        h("div", {className: "card"}, h("h3", {}, "My join requests"), myRequests));

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
    const row = h("div", {className: "card"},
        h("a", {href: "#/projects/" + request.projectId}, request.projectTitle), " ", badge(request.status),
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
    } else if ((match = path.match(/^\/projects\/(\d+)\/edit$/))) {
        projectFormPage(match[1]);
    } else if ((match = path.match(/^\/projects\/(\d+)$/))) {
        projectPage(match[1]);
    } else {
        render(h("h1", {}, "Page not found"), h("a", {href: "#/"}, "Back to projects"));
    }
}

window.addEventListener("hashchange", route);
route();
