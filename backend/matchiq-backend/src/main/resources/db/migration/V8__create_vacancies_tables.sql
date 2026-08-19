CREATE TABLE vacancies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    company VARCHAR(255) NULL,
    location VARCHAR(120) NULL,
    work_modality VARCHAR(20) NULL,
    salary_range VARCHAR(100) NULL,
    url VARCHAR(500) NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vacancies_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_vacancies_user_created ON vacancies(user_id, created_at DESC);

CREATE TABLE vacancy_skills (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    level VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vacancy_skills_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies(id) ON DELETE CASCADE,
    CONSTRAINT fk_vacancy_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT uq_vacancy_skill UNIQUE (vacancy_id, skill_id)
);

CREATE INDEX idx_vacancy_skills_vacancy ON vacancy_skills(vacancy_id);
