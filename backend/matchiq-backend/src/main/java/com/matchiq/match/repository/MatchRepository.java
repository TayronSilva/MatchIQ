package com.matchiq.match.repository;

import com.matchiq.match.domain.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByResumeIdAndVacancyId(Long resumeId, Long vacancyId);

    Optional<Match> findByIdAndUserId(Long id, Long userId);

    List<Match> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Match> findByUserIdAndResumeIdOrderByCreatedAtDesc(Long userId, Long resumeId);
}
