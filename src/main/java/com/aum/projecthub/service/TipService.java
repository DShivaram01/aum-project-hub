package com.aum.projecthub.service;

import com.aum.projecthub.dto.TipRequest;
import com.aum.projecthub.dto.TipResponse;
import com.aum.projecthub.exception.ForbiddenException;
import com.aum.projecthub.exception.NotFoundException;
import com.aum.projecthub.model.Project;
import com.aum.projecthub.model.Tip;
import com.aum.projecthub.model.User;
import com.aum.projecthub.repository.ProjectRepository;
import com.aum.projecthub.repository.TipRepository;
import com.aum.projecthub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipService {
    private final TipRepository tipRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TipService(TipRepository tipRepository, ProjectRepository projectRepository,
                      UserRepository userRepository) {
        this.tipRepository = tipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TipResponse> getTips(Long projectId) {
        findProjectById(projectId);
        return tipRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(TipResponse::from)
                .toList();
    }

    @Transactional
    public TipResponse addTip(Long projectId, TipRequest request, String currentUserEmail) {
        Project project = findProjectById(projectId);
        User author = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUserEmail));
        return TipResponse.from(tipRepository.save(new Tip(project, author, request.content().trim())));
    }

    // the author can take a tip back, and the project owner can clear tips off their project
    @Transactional
    public void deleteTip(Long tipId, String currentUserEmail) {
        Tip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new NotFoundException("Tip not found with id " + tipId));

        boolean isAuthor = tip.getAuthor().getEmail().equals(currentUserEmail);
        boolean isOwner = tip.getProject().getOwner().getEmail().equals(currentUserEmail);
        if (!isAuthor && !isOwner) {
            throw new ForbiddenException("Only the tip's author or the project owner can delete it");
        }
        tipRepository.delete(tip);
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found with id " + id));
    }
}
