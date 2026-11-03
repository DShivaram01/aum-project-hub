package com.aum.projecthub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

// a project entered in an event, to present it there, publish it, or both
@Entity
@Table(name = "event_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "project_id"}))
public class EventSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private boolean present;

    @Column(nullable = false)
    private boolean publish;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }

    public EventSubmission() {
    }

    public EventSubmission(Event event, Project project, boolean present, boolean publish) {
        this.event = event;
        this.project = project;
        this.present = present;
        this.publish = publish;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Project getProject() {
        return project;
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isPublish() {
        return publish;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
