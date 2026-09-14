package com.matchiq.match.service;

import com.matchiq.profile.domain.ProfessionalLevel;
import com.matchiq.profile.domain.Profile;
import com.matchiq.profile.domain.WorkModality;
import com.matchiq.vacancy.domain.Vacancy;
import com.matchiq.vacancy.domain.VacancySkill;
import com.matchiq.skill.domain.ResumeSkill;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Scoring determinístico 0-100 com 5 componentes:
 *
 * 1. Competências (0-40): razão de skills batentes vs exigidas
 * 2. Senioridade (0-20): nível profissional vs vaga
 * 3. Região/Modalidade (0-20): localização e tipo de trabalho
 * 4. Recência (0-10): quão recente é a vaga
 * 5. Preferências (0-10): expectativa salarial
 */
public class ScoringService {

    public record ScoreBreakdown(
            int competencias,
            int senioridade,
            int regiao,
            int recencia,
            int preferencias,
            int total,
            String rationale
    ) {}

    private ScoringService() {}

    public static ScoreBreakdown calculate(
            Profile profile,
            Vacancy vacancy,
            List<ResumeSkill> resumeSkills,
            List<VacancySkill> vacancySkills,
            Set<Long> matchedSkillIds
    ) {
        int competencias = scoreCompetencias(resumeSkills, vacancySkills, matchedSkillIds);
        int senioridade = profile != null ? scoreSenioridade(profile, vacancy) : 10;
        int regiao = profile != null ? scoreRegiao(profile, vacancy) : 10;
        int recencia = scoreRecencia(vacancy);
        int preferencias = profile != null ? scorePreferencias(profile, vacancy) : 5;

        int total = competencias + senioridade + regiao + recencia + preferencias;

        String rationale = buildRationale(competencias, senioridade, regiao, recencia, preferencias);

        return new ScoreBreakdown(competencias, senioridade, regiao, recencia, preferencias, total, rationale);
    }

    // 1. Competências: 0-40 pontos
    private static int scoreCompetencias(
            List<ResumeSkill> resumeSkills,
            List<VacancySkill> vacancySkills,
            Set<Long> matchedSkillIds
    ) {
        if (vacancySkills.isEmpty()) return 0;

        long matchedCount = vacancySkills.stream()
                .filter(vs -> matchedSkillIds.contains(vs.getSkillId()))
                .count();

        double ratio = (double) matchedCount / vacancySkills.size();
        return (int) Math.round(ratio * 40);
    }

    // 2. Senioridade: 0-20 pontos
    private static int scoreSenioridade(Profile profile, Vacancy vacancy) {
        if (profile.getProfessionalLevel() == null) return 10;

        String titleLower = vacancy.getTitle().toLowerCase();
        String descLower = vacancy.getDescription() != null ? vacancy.getDescription().toLowerCase() : "";

        boolean wantsJunior = titleLower.contains("junior") || titleLower.contains("júnior")
                || titleLower.contains("trainee") || titleLower.contains("estagi");
        boolean wantsPleno = titleLower.contains("pleno") || titleLower.contains("mid-level")
                || titleLower.contains("mid level");
        boolean wantsSenior = titleLower.contains("senior") || titleLower.contains("sênior")
                || titleLower.contains("lead") || titleLower.contains("principal")
                || titleLower.contains("staff") || titleLower.contains("sra.");

        ProfessionalLevel vacancyLevel = null;
        if (wantsSenior) vacancyLevel = ProfessionalLevel.SENIOR;
        else if (wantsPleno) vacancyLevel = ProfessionalLevel.PLENO;
        else if (wantsJunior) vacancyLevel = ProfessionalLevel.JUNIOR;

        if (vacancyLevel == null) return 15;

        if (profile.getProfessionalLevel() == vacancyLevel) return 20;

        int profileOrd = profile.getProfessionalLevel().ordinal();
        int vacancyOrd = vacancyLevel.ordinal();
        int diff = Math.abs(profileOrd - vacancyOrd);

        if (diff == 1) return 12;
        return 4;
    }

    // 3. Região/Modalidade: 0-20 pontos
    private static int scoreRegiao(Profile profile, Vacancy vacancy) {
        int modalityScore = scoreModalidade(profile.getWorkModality(), vacancy.getWorkModality());
        int locationScore = scoreLocalizacao(profile, vacancy);

        return modalityScore + locationScore;
    }

    private static int scoreModalidade(WorkModality profileMod, WorkModality vacancyMod) {
        if (profileMod == null || vacancyMod == null) return 10;

        if (profileMod == vacancyMod) return 14;

        if (profileMod == WorkModality.REMOTE && vacancyMod == WorkModality.HYBRID) return 10;
        if (profileMod == WorkModality.HYBRID && vacancyMod == WorkModality.REMOTE) return 10;

        if (profileMod == WorkModality.REMOTE && vacancyMod == WorkModality.ONSITE) return 2;
        if (profileMod == WorkModality.ONSITE && vacancyMod == WorkModality.REMOTE) return 2;

        return 6;
    }

    private static int scoreLocalizacao(Profile profile, Vacancy vacancy) {
        String profileLoc = profile.getDesiredLocation() != null ? profile.getDesiredLocation().toLowerCase() : "";
        String vacancyLoc = vacancy.getLocation() != null ? vacancy.getLocation().toLowerCase() : "";

        if (profileLoc.isEmpty() || vacancyLoc.isEmpty()) return 6;

        if (profileLoc.equals(vacancyLoc)) return 6;

        if (vacancyLoc.contains("remote") || vacancyLoc.contains("remoto")
                || vacancyLoc.contains("worldwide") || vacancyLoc.contains("global")) {
            return 6;
        }

        if (profileLoc.contains(vacancyLoc) || vacancyLoc.contains(profileLoc)) return 4;

        return 1;
    }

    // 4. Recência: 0-10 pontos
    private static int scoreRecencia(Vacancy vacancy) {
        LocalDateTime reference = vacancy.getLastSeenAt() != null ? vacancy.getLastSeenAt() : vacancy.getCreatedAt();
        if (reference == null) return 5;

        long hoursAgo = Duration.between(reference, LocalDateTime.now()).toHours();

        if (hoursAgo < 24) return 10;
        if (hoursAgo < 72) return 8;
        if (hoursAgo < 168) return 6;
        if (hoursAgo < 720) return 4;
        return 1;
    }

    // 5. Preferências (salarial): 0-10 pontos
    private static int scorePreferencias(Profile profile, Vacancy vacancy) {
        if (profile.getSalaryExpectation() == null || vacancy.getSalaryRange() == null) return 5;

        BigDecimal expected = profile.getSalaryExpectation();
        BigDecimal[] vacancyRange = parseSalaryRange(vacancy.getSalaryRange());

        if (vacancyRange == null) return 5;

        BigDecimal min = vacancyRange[0];
        BigDecimal max = vacancyRange[1];

        if (expected.compareTo(min) >= 0 && expected.compareTo(max) <= 0) return 10;

        if (expected.compareTo(min) >= 0) return 6;

        BigDecimal diff = min.subtract(expected).abs();
        double pctOfMin = diff.doubleValue() / min.doubleValue() * 100;
        if (pctOfMin <= 20) return 6;
        if (pctOfMin <= 40) return 3;

        return 1;
    }

    private static BigDecimal[] parseSalaryRange(String salaryRange) {
        try {
            String cleaned = salaryRange.toLowerCase()
                    .replace("r$", "").replace("$", "")
                    .replace(".", "").replace(",", ".")
                    .replace(" ", "")
                    .replace("/m", "").replace("/ano", "");

            String[] parts = cleaned.split("-");
            if (parts.length == 2) {
                BigDecimal min = new BigDecimal(parts[0].trim());
                BigDecimal max = new BigDecimal(parts[1].trim());
                return new BigDecimal[]{min, max};
            }

            if (parts.length == 1) {
                BigDecimal val = new BigDecimal(parts[0].trim());
                return new BigDecimal[]{val, val};
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static String buildRationale(int comp, int sen, int reg, int rec, int pref) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score determinístico: ");

        if (comp >= 30) sb.append("forte compatibilidade de skills. ");
        else if (comp >= 15) sb.append("compatibilidade parcial de skills. ");
        else sb.append("poucas skills em comum. ");

        if (sen >= 16) sb.append("nivelamento ideal. ");
        else if (sen >= 10) sb.append("nivelamento próximo. ");
        else sb.append("nivelamento distante. ");

        if (reg >= 18) sb.append("localização/modalidade compatíveis. ");
        else if (reg >= 10) sb.append("modalidade parcialmente compatível. ");
        else sb.append("localização desalinhada. ");

        if (rec >= 8) sb.append("vaga recente. ");
        else if (rec >= 5) sb.append("vaga moderadamente recente. ");
        else sb.append("vaga antiga. ");

        if (pref >= 8) sb.append("expectativa salarial compatível.");
        else if (pref >= 4) sb.append("expectativa salarial parcialmente compatível.");
        else sb.append("expectativa salarial incompatível.");

        return sb.toString();
    }
}
