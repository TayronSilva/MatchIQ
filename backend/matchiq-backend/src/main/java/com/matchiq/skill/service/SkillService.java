package com.matchiq.skill.service;

import com.matchiq.common.exception.ResourceNotFoundException;
import com.matchiq.common.exception.SkillAlreadyExistsException;
import com.matchiq.skill.domain.Skill;
import com.matchiq.skill.dto.CreateSkillRequest;
import com.matchiq.skill.dto.SkillResponse;
import com.matchiq.skill.dto.UpdateSkillRequest;
import com.matchiq.skill.mapper.SkillMapper;
import com.matchiq.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository repository;
    private final SkillMapper mapper;

    @Transactional
    public SkillResponse create(CreateSkillRequest request) {
        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new SkillAlreadyExistsException("Skill already exists: " + request.getName());
        }

        Skill skill = mapper.toEntity(request);
        Skill saved = repository.save(skill);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse findById(Long id) {
        Skill skill = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
        return mapper.toResponse(skill);
    }

    @Transactional
    public SkillResponse update(Long id, UpdateSkillRequest request) {
        Skill skill = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));

        mapper.updateEntity(skill, request);
        Skill updated = repository.save(skill);
        return mapper.toResponse(updated);
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + id));
        repository.delete(skill);
    }
}
