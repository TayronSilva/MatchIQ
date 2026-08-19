ALTER TABLE profiles
    ADD COLUMN professional_level VARCHAR(20) NULL,
    ADD COLUMN years_of_experience INT NULL,
    ADD COLUMN work_modality VARCHAR(20) NULL,
    ADD COLUMN desired_location VARCHAR(120) NULL,
    ADD COLUMN salary_expectation NUMERIC(12,2) NULL;
