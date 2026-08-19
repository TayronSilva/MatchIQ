CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    vacancy_id BIGINT NOT NULL,
    score INT NOT NULL,
    matched_skills TEXT NULL,
    missing_skills TEXT NULL,
    algorithm_version VARCHAR(20) NOT NULL DEFAULT 'v1',
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_matches_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies(id) ON DELETE CASCADE,
    CONSTRAINT uq_match_resume_vacancy UNIQUE (resume_id, vacancy_id)
);

CREATE INDEX idx_matches_user_created ON matches(user_id, created_at DESC);
