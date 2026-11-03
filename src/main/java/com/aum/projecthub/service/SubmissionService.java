package com.aum.projecthub.service;

import com.aum.projecthub.dto.SubmissionRequest;
import com.aum.projecthub.dto.SubmissionResponse;
import com.aum.projecthub.exception.BadRequestException;
import com.aum.projecthub.exception.ConflictException;
import com.aum.projecthub.exception.ForbiddenException;
import com.aum.projecthub.exception.NotFoundException;
import com.aum.projecthub.model.Event;
import com.aum.projecthub.model.EventSubmission;
import com.aum.projecthub.model.Project;
import com.aum.projecthub.repository.EventSubmissionRepository;
import com.aum.projecthub.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionService {
    private final EventSubmissionRepository submissionRepository;
    private final ProjectRepository projectRepository;
    private final EventService eventService;

    public SubmissionService(EventSubmissionRepository submissionRepository, ProjectRepository projectRepository,
                             EventService eventService) {
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.eventService = eventService;
    }

    @Transactional
    public SubmissionResponse submit(Long projectId, SubmissionRequest request, String currentUserEmail) {
        Project project = findProjectById(projectId);
        checkOwner(project, currentUserEmail, "Only the project owner can enter it in an event");
        Event event = eventService.findEventById(request.eventId());

        if (!request.present() && !request.publish()) {
            throw new BadRequestException("Choose to present the project, publish it, or both");
        }
        if (!event.isAcceptingSubmissions()) {
            throw new BadRequestException("The submission deadline for this event has passed");
        }
        if (submissionRepository.existsByEventIdAndProjectId(event.getId(), projectId)) {
            throw new ConflictException("This project is already entered in that event");
        }

        EventSubmission submission = new EventSubmission(event, project, request.present(), request.publish());
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getForProject(Long projectId) {
        findProjectById(projectId);
        return submissionRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getForEvent(Long eventId) {
        eventService.findEventById(eventId);
        return submissionRepository.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional
    public void withdraw(Long submissionId, String currentUserEmail) {
        EventSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with id " + submissionId));
        checkOwner(submission.getProject(), currentUserEmail, "Only the project owner can withdraw it");

        if (!submission.getEvent().isAcceptingSubmissions()) {
            throw new ConflictException("The deadline has passed, so this entry can no longer be withdrawn");
        }
        submissionRepository.delete(submission);
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found with id " + id));
    }

    private void checkOwner(Project project, String currentUserEmail, String message) {
        if (!project.getOwner().getEmail().equals(currentUserEmail)) {
            throw new ForbiddenException(message);
        }
    }
}
