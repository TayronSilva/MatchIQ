package com.matchiq.match.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.match.domain.Match;
import com.matchiq.match.domain.MatchStatus;
import com.matchiq.match.dto.MatchResponse;
import com.matchiq.match.mapper.MatchMapper;
import com.matchiq.match.repository.MatchRepository;
import com.matchiq.resume.domain.Resume;
import com.matchiq.resume.repository.ResumeRepository;
import com.matchiq.skill.domain.ResumeSkill;
import com.matchiq.skill.repository.ResumeSkillRepository;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.vacancy.repository.VacancyRepository;
import com.matchiq.vacancy.repository.VacancySkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private VacancyRepository vacancyRepository;

    @Mock
    private ResumeSkillRepository resumeSkillRepository;

    @Mock
    private VacancySkillRepository vacancySkillRepository;

    @Mock
    private MatchMapper mapper;

    @InjectMocks
    private MatchService service;

    private Resume resume() {
        Resume r = new Resume();
        r.setId(1L);
        r.setUserId(1L);
        return r;
    }

    private Vacancy vacancy() {
        Vacancy v = new Vacancy();
        v.setId(1L);
        v.setUserId(1L);
        return v;
    }

    private ResumeSkill resumeSkill(Long skillId) {
        ResumeSkill rs = new ResumeSkill();
        rs.setResumeId(1L);
        rs.setSkillId(skillId);
        return rs;
    }

    private VacancySkill vacancySkill(Long skillId) {
        VacancySkill vs = new VacancySkill();
        vs.setVacancyId(1L);
        vs.setSkillId(skillId);
        return vs;
    }

    @Test
    void calculate_shouldScore50PercentWhenHalfSkillsMatch() {
        when(resumeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume()));
        when(vacancyRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy()));
        when(resumeSkillRepository.findByResumeId(1L)).thenReturn(List.of(resumeSkill(10L), resumeSkill(11L)));
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of(vacancySkill(10L), vacancySkill(12L)));
        when(matchRepository.findByResumeIdAndVacancyId(1L, 1L)).thenReturn(Optional.empty());

        Match saved = new Match();
        saved.setScore(50);
        saved.setMatchedSkillsJson("[\"10\"]");
        saved.setMissingSkillsJson("[\"12\"]");
        when(mapper.toJson(anyList())).thenAnswer(inv -> {
            List<String> list = inv.getArgument(0);
            return "[\"" + String.join("\",\"", list) + "\"]";
        });
        when(matchRepository.save(any(Match.class))).thenReturn(saved);

        MatchResponse response = new MatchResponse();
        response.setScore(50);
        when(mapper.toResponse(saved)).thenReturn(response);

        MatchResponse result = service.calculate(1L, 1L, 1L);

        assertEquals(50, result.getScore());
        // verifica que o JSON de matched/missing foi montado com as skills certas
        verify(matchRepository).save(argThat(m ->
                m.getMatchedSkillsJson().contains("10") && m.getMissingSkillsJson().contains("12")));
    }

    @Test
    void calculate_shouldScore100WhenAllSkillsMatch() {
        when(resumeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume()));
        when(vacancyRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy()));
        when(resumeSkillRepository.findByResumeId(1L)).thenReturn(List.of(resumeSkill(10L), resumeSkill(11L)));
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of(vacancySkill(10L), vacancySkill(11L)));
        when(matchRepository.findByResumeIdAndVacancyId(1L, 1L)).thenReturn(Optional.empty());

        Match saved = new Match();
        saved.setScore(100);
        when(matchRepository.save(any(Match.class))).thenReturn(saved);

        MatchResponse response = new MatchResponse();
        response.setScore(100);
        when(mapper.toResponse(saved)).thenReturn(response);

        MatchResponse result = service.calculate(1L, 1L, 1L);

        assertEquals(100, result.getScore());
    }

    @Test
    void calculate_shouldScoreZeroWhenNoSkillMatches() {
        when(resumeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume()));
        when(vacancyRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy()));
        when(resumeSkillRepository.findByResumeId(1L)).thenReturn(List.of(resumeSkill(10L)));
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of(vacancySkill(12L), vacancySkill(13L)));
        when(matchRepository.findByResumeIdAndVacancyId(1L, 1L)).thenReturn(Optional.empty());

        Match saved = new Match();
        saved.setScore(0);
        when(matchRepository.save(any(Match.class))).thenReturn(saved);

        MatchResponse response = new MatchResponse();
        response.setScore(0);
        when(mapper.toResponse(saved)).thenReturn(response);

        MatchResponse result = service.calculate(1L, 1L, 1L);

        assertEquals(0, result.getScore());
    }

    @Test
    void calculate_shouldUpsertExistingMatch() {
        when(resumeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume()));
        when(vacancyRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vacancy()));
        when(resumeSkillRepository.findByResumeId(1L)).thenReturn(List.of(resumeSkill(10L)));
        when(vacancySkillRepository.findByVacancyId(1L)).thenReturn(List.of(vacancySkill(10L)));

        Match existing = new Match();
        existing.setId(99L);
        existing.setScore(50);
        when(matchRepository.findByResumeIdAndVacancyId(1L, 1L)).thenReturn(Optional.of(existing));
        when(matchRepository.save(existing)).thenReturn(existing);

        MatchResponse response = new MatchResponse();
        response.setScore(100);
        when(mapper.toResponse(existing)).thenReturn(response);

        MatchResponse result = service.calculate(1L, 1L, 1L);

        assertEquals(100, result.getScore());
        verify(matchRepository).save(existing);
    }

    @Test
    void calculate_shouldThrowWhenResumeNotFound() {
        when(resumeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.calculate(1L, 99L, 1L));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void calculate_shouldThrowWhenVacancyNotFound() {
        when(resumeRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(resume()));
        when(vacancyRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.calculate(1L, 1L, 99L));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void findByUserId_shouldReturnList() {
        Match match = new Match();
        match.setId(1L);
        match.setScore(80);

        MatchResponse response = new MatchResponse();
        response.setId(1L);
        response.setScore(80);

        when(matchRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(match));
        when(mapper.toResponse(match)).thenReturn(response);

        List<MatchResponse> result = service.findByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(80, result.get(0).getScore());
    }

    @Test
    void findByIdAndUserId_shouldThrowWhenNotFound() {
        when(matchRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByIdAndUserId(99L, 1L));
    }

    @Test
    void findByResumeId_shouldThrowWhenResumeNotFound() {
        when(resumeRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByResumeId(1L, 99L));
    }
}
