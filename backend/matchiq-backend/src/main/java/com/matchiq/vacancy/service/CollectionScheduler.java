package com.matchiq.vacancy.service;

import com.matchiq.user.domain.User;
import com.matchiq.user.repository.UserRepository;
import com.matchiq.vacancy.collector.CollectOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coleta periódica automática de vagas para todos os usuários ativos.
 * Roda em background com intervalo configurável (default: 6 horas).
 * Cada usuário respeita seu próprio cooldown (45min entre coletas).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionScheduler {

    private final VacancyCollectorService collectorService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelayString = "${matchiq.collection.interval-hours:6}h",
               initialDelayString = "${matchiq.collection.initial-delay-minutes:5}m")
    public void collectForAllUsers() {
        log.info("Iniciando coleta periódica automática...");
        List<User> users = userRepository.findAll();
        int collected = 0;
        int skipped = 0;

        for (User user : users) {
            try {
                CollectOutcome outcome = collectorService.collectAll(user.getId());
                if (outcome.cooldownActive()) {
                    skipped++;
                } else {
                    collected++;
                    log.info("Coleta para usuario {} concluida: {} vagas novas",
                            user.getId(), outcome.newJobs());
                }
            } catch (Exception e) {
                log.warn("Falha na coleta periódica para usuario {}: {}",
                        user.getId(), e.getMessage());
            }
        }

        log.info("Coleta periódica finalizada: {} usuarios coletados, {} ignorados (cooldown)",
                collected, skipped);
    }
}
