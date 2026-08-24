package com.matchiq.tailor.repository;

import com.matchiq.tailor.domain.ResumeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeSessionRepository extends JpaRepository<ResumeSession, Long> {

    List<ResumeSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ResumeSession> findByIdAndUserId(Long id, Long userId);
}
