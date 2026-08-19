package com.matchiq.analysis.repository;

import com.matchiq.analysis.domain.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByMatchId(Long matchId);

    Optional<Analysis> findByIdAndUserId(Long id, Long userId);

    List<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
