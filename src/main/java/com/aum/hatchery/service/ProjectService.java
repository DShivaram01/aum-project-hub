package com.aum.hatchery.service;

import com.aum.hatchery.dto.ProjectRequest;
import com.aum.hatchery.dto.ProjectResponse;
import com.aum.hatchery.exception.ProjectNotFoundException;
import com.aum.hatchery.exception.UnauthorizedActionException;
import com.aum.hatchery.exception.UserNotFoundException;
import com.aum.hatchery.model.Project;
import com.aum.hatchery.model.ProjectStatus;
import com.aum.hatchery.model.User;
import com.aum.hatchery.repository.JoinRequestRepository;
import com.aum.hatchery.repository.ProjectRepository;
import com.aum.hatchery.repository.UserRepository;
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
                .orElseThrow(() -> new UserNotFoundException(currentUserEmail));

        Project project = new Project(request.title(), request.description(), request.category(),
                request.requiredSkills(), owner);
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return ProjectResponse.from(projectRepository.save(project));
    }

    // Any filter can be null, which means "don't filter on this"
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

    // Both deletes run in one transaction, so we never end up with half the data removed
    @Transactional
    public void deleteProject(Long id, String currentUserEmail) {
        Project project = findProjectById(id);
        checkOwner(project, currentUserEmail);

        joinRequestRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private void checkOwner(Project project, String currentUserEmail) {
        if (!project.getOwner().getEmail().equals(currentUserEmail)) {
            throw new UnauthorizedActionException("Only the project owner can do this");
        }
    }

    // Turn a filter value into the form the search query expects: lowercase, or "" when not given
    private String cleanFilter(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
