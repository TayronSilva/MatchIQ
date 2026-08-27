CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vacancy_id BIGINT NOT NULL,
    resume_id BIGINT,
    match_id BIGINT,
    status VARCHAR(20) NOT NULL,
    applied_at TIMESTAMP,
    interview_at TIMESTAMP,
    notes TEXT,
    documents_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
