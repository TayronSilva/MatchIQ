package com.matchiq.tailor.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "vacancy_id", nullable = false)
    private Long vacancyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TailorStatus status = TailorStatus.PENDING;

    @Column(name = "content_markdown", columnDefinition = "TEXT")
    private String contentMarkdown;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
