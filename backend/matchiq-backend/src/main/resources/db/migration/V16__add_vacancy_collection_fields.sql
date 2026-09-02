ALTER TABLE vacancies ADD COLUMN external_id VARCHAR(255);
ALTER TABLE vacancies ADD COLUMN last_seen_at TIMESTAMP;
ALTER TABLE vacancies ADD COLUMN removed BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_vacancies_user_source_external
    ON vacancies (user_id, source, external_id)
    WHERE external_id IS NOT NULL;
