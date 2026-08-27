package com.matchiq.application.repository;

import com.matchiq.application.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Application> findByUserIdAndVacancyId(Long userId, Long vacancyId);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndVacancyId(Long userId, Long vacancyId);

    void deleteByIdAndUserId(Long id, Long userId);
}
