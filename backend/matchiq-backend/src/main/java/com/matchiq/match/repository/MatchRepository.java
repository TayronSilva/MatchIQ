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

    int deleteByIdAndUserId(Long id, Long userId);

    int deleteByUserId(Long userId);

    List<Match> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Match> findByUserIdAndResumeIdOrderByCreatedAtDesc(Long userId, Long resumeId);

    List<Match> findByResumeId(Long resumeId);

    List<Match> findByVacancyId(Long vacancyId);

    void deleteByResumeId(Long resumeId);

    void deleteByVacancyId(Long vacancyId);
}
