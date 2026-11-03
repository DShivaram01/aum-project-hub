package com.aum.projecthub.controller;

import com.aum.projecthub.service.SubmissionService;
import com.aum.projecthub.service.TipService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// deletes for things that are created under a project but addressed by their own id
@RestController
public class SubmissionController {
    private final SubmissionService submissionService;
    private final TipService tipService;

    public SubmissionController(SubmissionService submissionService, TipService tipService) {
        this.submissionService = submissionService;
        this.tipService = tipService;
    }

    @DeleteMapping("/api/submissions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable Long id, Authentication authentication) {
        submissionService.withdraw(id, authentication.getName());
    }

    @DeleteMapping("/api/tips/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTip(@PathVariable Long id, Authentication authentication) {
        tipService.deleteTip(id, authentication.getName());
    }
}
