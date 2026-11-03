package com.aum.projecthub.service;

import com.aum.projecthub.dto.ProjectRequest;
import com.aum.projecthub.dto.ProjectResponse;
import com.aum.projecthub.exception.BadRequestException;
import com.aum.projecthub.exception.ForbiddenException;
import com.aum.projecthub.exception.NotFoundException;
import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.ProjectStatus;
import com.aum.projecthub.model.Upvote;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.EventSubmissionRepository;
import com.aum.projecthub.repository.JoinRequestRepository;
import com.aum.projecthub.repository.ProjectRepository;
import com.aum.projecthub.repository.TipRepository;
import com.aum.projecthub.repository.UpvoteRepository;
import com.aum.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final UpvoteRepository upvoteRepository;
    private final TipRepository tipRepository;
    private final EventSubmissionRepository submissionRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                          JoinRequestRepository joinRequestRepository, UpvoteRepository upvoteRepository,
                          TipRepository tipRepository, EventSubmissionRepository submissionRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.upvoteRepository = upvoteRepository;
        this.tipRepository = tipRepository;
        this.submissionRepository = submissionRepository;
    }

    public ProjectResponse createProject(ProjectRequest request, String currentUserEmail) {
        User owner = findUserByEmail(currentUserEmail);

        Project project = new Project(request.title(), request.description(), request.category(),
                request.requiredSkills(), owner);
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return ProjectResponse.from(projectRepository.save(project), 0, false);
    }

    // sort is "new" (default) or "hot" (most upvoted first, newest first among ties)
    public List<ProjectResponse> getProjects(String category, ProjectStatus status, String skill, String sort,
                                             String currentUserEmail) {
        boolean byUpvotes = sortByUpvotes(sort);
        List<Project> projects = projectRepository.search(cleanFilter(category), status, cleanFilter(skill));

        Map<Long, Long> upvotes = new HashMap<>();
        for (Object[] row : upvoteRepository.countPerProject()) {
            upvotes.put((Long) row[0], (Long) row[1]);
        }
        Set<Long> mine = currentUserEmail == null
                ? Set.of()
                : new HashSet<>(upvoteRepository.findProjectIdsUpvotedBy(currentUserEmail));

        List<ProjectResponse> responses = projects.stream()
                .map(project -> ProjectResponse.from(project, upvotes.getOrDefault(project.getId(), 0L),
                        mine.contains(project.getId())))
                .toList();
        if (byUpvotes) {
            // stable sort, so the search's newest-first order is kept for equal counts
            responses = responses.stream()
                    .sorted(Comparator.comparingLong(ProjectResponse::upvotes).reversed())
                    .toList();
        }
        return responses;
    }

    public ProjectResponse getProject(Long id, String currentUserEmail) {
        return toResponse(findProjectById(id), currentUserEmail);
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

        return toResponse(projectRepository.save(project), currentUserEmail);
    }

    @Transactional
    public void deleteProject(Long id, String currentUserEmail) {
        Project project = findProjectById(id);
        checkOwner(project, currentUserEmail);

        joinRequestRepository.deleteByProjectId(id);
        upvoteRepository.deleteByProjectId(id);
        tipRepository.deleteByProjectId(id);
        submissionRepository.deleteByProjectId(id);
        projectRepository.delete(project);
    }

    // upvoting twice is a no-op, so the button can't double count
    @Transactional
    public ProjectResponse upvote(Long id, String currentUserEmail) {
        Project project = findProjectById(id);
        User user = findUserByEmail(currentUserEmail);

        if (project.getOwner().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot upvote your own project");
        }
        if (upvoteRepository.findByProjectIdAndUserId(id, user.getId()).isEmpty()) {
            upvoteRepository.save(new Upvote(project, user));
        }
        return toResponse(project, currentUserEmail);
    }

    @Transactional
    public ProjectResponse removeUpvote(Long id, String currentUserEmail) {
        Project project = findProjectById(id);
        User user = findUserByEmail(currentUserEmail);

        upvoteRepository.findByProjectIdAndUserId(id, user.getId()).ifPresent(upvoteRepository::delete);
        return toResponse(project, currentUserEmail);
    }

    private ProjectResponse toResponse(Project project, String currentUserEmail) {
        long upvotes = upvoteRepository.countByProjectId(project.getId());
        boolean mine = currentUserEmail != null
                && upvoteRepository.existsByProjectIdAndUserEmail(project.getId(), currentUserEmail);
        return ProjectResponse.from(project, upvotes, mine);
    }

    private boolean sortByUpvotes(String sort) {
        if (sort == null || sort.isBlank() || sort.equalsIgnoreCase("new")) {
            return false;
        }
        if (sort.equalsIgnoreCase("hot")) {
            return true;
        }
        throw new BadRequestException("sort must be 'new' or 'hot'");
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found: " + email));
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
