package com.aum.projecthub.service;

import com.aum.projecthub.dto.ProjectRequest;
import com.aum.projecthub.dto.ProjectResponse;
import com.aum.projecthub.exception.NotFoundException;
import com.aum.projecthub.exception.ForbiddenException;
import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.ProjectStatus;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.JoinRequestRepository;
import com.aum.projecthub.repository.ProjectRepository;
import com.aum.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JoinRequestRepository joinRequestRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                          JoinRequestRepository joinRequestRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.joinRequestRepository = joinRequestRepository;
    }

    public ProjectResponse createProject(ProjectRequest request, String currentUserEmail) {
        User owner = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUserEmail));

        Project project = new Project(request.title(), request.description(), request.category(),
                request.requiredSkills(), owner);
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return ProjectResponse.from(projectRepository.save(project));
    }

    public List<ProjectResponse> getProjects(String category, ProjectStatus status, String skill) {
        List<Project> projects = projectRepository.search(cleanFilter(category), status, cleanFilter(skill));
        return projects.stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse getProject(Long id) {
        return ProjectResponse.from(findProjectById(id));
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request, String currentUserEmail) {
        Project project = findProjectById(id);
        checkOwner(project, currentUserEmail);

        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setCategory(request.category());
        project.setRequiredSkills(request.requiredSkills());
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void deleteProject(Long id, String currentUserEmail) {
        Project project = findProjectById(id);
        checkOwner(project, currentUserEmail);

        joinRequestRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found with id " + id));
    }

    private void checkOwner(Project project, String currentUserEmail) {
        if (!project.getOwner().getEmail().equals(currentUserEmail)) {
            throw new ForbiddenException("Only the project owner can do this");
        }
    }

    private String cleanFilter(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
