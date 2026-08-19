package com.matchiq.recommendation.repository;

import com.matchiq.recommendation.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByMatchId(Long matchId);

    Optional<Recommendation> findByIdAndUserId(Long id, Long userId);

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
