package com.aum.projecthub.controller;

import com.aum.projecthub.dto.ProjectRequest;
import com.aum.projecthub.dto.ProjectResponse;
import com.aum.projecthub.model.ProjectStatus;
import com.aum.projecthub.service.ProjectService;
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

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request, Authentication authentication) {
        return projectService.createProject(request, authentication.getName());
    }

    @GetMapping
    public List<ProjectResponse> getProjects(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) ProjectStatus status,
                                             @RequestParam(required = false) String skill) {
        return projectService.getProjects(category, status, skill);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectService.getProject(id);
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
}
