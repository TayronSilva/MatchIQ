ALTER TABLE vacancies ADD COLUMN favorite BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_vacancies_user_favorite ON vacancies(user_id, favorite);
