package com.matchiq.match.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "vacancy_id", nullable = false)
    private Long vacancyId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkillsJson;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkillsJson;

    @Column(name = "algorithm_version", nullable = false)
    private String algorithmVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.COMPLETED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
