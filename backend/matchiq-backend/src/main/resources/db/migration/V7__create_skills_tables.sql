CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(30) NOT NULL DEFAULT 'TECHNICAL',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE resume_skills (
    id BIGSERIAL PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    level VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_resume_skills_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
    CONSTRAINT fk_resume_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT uq_resume_skill UNIQUE (resume_id, skill_id)
);

CREATE INDEX idx_resume_skills_resume ON resume_skills(resume_id);
