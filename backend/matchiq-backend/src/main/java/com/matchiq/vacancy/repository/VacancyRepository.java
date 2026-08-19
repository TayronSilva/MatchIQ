package com.matchiq.vacancy.repository;

import com.matchiq.vacancy.domain.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

    List<Vacancy> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Vacancy> findByIdAndUserId(Long id, Long userId);
}
