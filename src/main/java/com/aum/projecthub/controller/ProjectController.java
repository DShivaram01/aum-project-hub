package com.aum.projecthub.controller;

import com.aum.projecthub.dto.ProjectRequest;
import com.aum.projecthub.dto.ProjectResponse;
import com.aum.projecthub.dto.SubmissionRequest;
import com.aum.projecthub.dto.SubmissionResponse;
import com.aum.projecthub.dto.TipRequest;
import com.aum.projecthub.dto.TipResponse;
import com.aum.projecthub.model.ProjectStatus;
import com.aum.projecthub.service.ProjectService;
import com.aum.projecthub.service.SubmissionService;
import com.aum.projecthub.service.TipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final TipService tipService;
    private final SubmissionService submissionService;

    public ProjectController(ProjectService projectService, TipService tipService,
                             SubmissionService submissionService) {
        this.projectService = projectService;
        this.tipService = tipService;
        this.submissionService = submissionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request, Authentication authentication) {
        return projectService.createProject(request, authentication.getName());
    }

    // public endpoints get a null Authentication when nobody is logged in
    @GetMapping
    public List<ProjectResponse> getProjects(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) ProjectStatus status,
                                             @RequestParam(required = false) String skill,
                                             @RequestParam(required = false) String sort,
                                             Authentication authentication) {
        return projectService.getProjects(category, status, skill, sort, emailOf(authentication));
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id, Authentication authentication) {
        return projectService.getProject(id, emailOf(authentication));
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request,
                                         Authentication authentication) {
        return projectService.updateProject(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id, Authentication authentication) {
        projectService.deleteProject(id, authentication.getName());
    }

    @PutMapping("/{id}/upvote")
    public ProjectResponse upvote(@PathVariable Long id, Authentication authentication) {
        return projectService.upvote(id, authentication.getName());
    }

    @DeleteMapping("/{id}/upvote")
    public ProjectResponse removeUpvote(@PathVariable Long id, Authentication authentication) {
        return projectService.removeUpvote(id, authentication.getName());
    }

    @GetMapping("/{id}/tips")
    public List<TipResponse> getTips(@PathVariable Long id) {
        return tipService.getTips(id);
    }

    @PostMapping("/{id}/tips")
    @ResponseStatus(HttpStatus.CREATED)
    public TipResponse addTip(@PathVariable Long id, @Valid @RequestBody TipRequest request,
                              Authentication authentication) {
        return tipService.addTip(id, request, authentication.getName());
    }

    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> getSubmissions(@PathVariable Long id) {
        return submissionService.getForProject(id);
    }

    @PostMapping("/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionResponse submit(@PathVariable Long id, @Valid @RequestBody SubmissionRequest request,
                                     Authentication authentication) {
        return submissionService.submit(id, request, authentication.getName());
    }

    private String emailOf(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
