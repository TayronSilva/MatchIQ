CREATE TABLE resume_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    vacancy_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    content_markdown TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
