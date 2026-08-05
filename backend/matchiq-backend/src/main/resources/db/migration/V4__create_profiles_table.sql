CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    bio TEXT,
    birth_date DATE,
    country VARCHAR(100),
    city VARCHAR(100),
    gender VARCHAR(20),
    looking_for VARCHAR(20),
    avatar_url VARCHAR(500),
    occupation VARCHAR(100),
    education VARCHAR(100),
    height INTEGER,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE profile_interests (
    profile_id BIGINT NOT NULL REFERENCES profiles(id),
    interest VARCHAR(50),
    PRIMARY KEY (profile_id, interest)
);
