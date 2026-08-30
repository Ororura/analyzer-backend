CREATE TABLE vacancies (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(128) NOT NULL,
    source VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    company VARCHAR(500),
    company_id VARCHAR(128),
    description TEXT,
    experience VARCHAR(255),
    employment VARCHAR(255),
    schedule VARCHAR(255),
    work_format VARCHAR(32),
    salary_from INTEGER,
    salary_to INTEGER,
    salary_currency VARCHAR(16),
    salary_gross BOOLEAN,
    location VARCHAR(500),
    url TEXT,
    published_at TIMESTAMPTZ,
    source_created_at TIMESTAMPTZ,
    source_updated_at TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    last_checked_at TIMESTAMPTZ NOT NULL,
    content_hash CHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    archived_at TIMESTAMPTZ,
    raw_payload JSONB,
    CONSTRAINT uk_vacancies_source_external UNIQUE (source, external_id)
);

CREATE TABLE vacancy_skills (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    skill VARCHAR(255) NOT NULL,
    normalized_skill VARCHAR(255) NOT NULL,
    CONSTRAINT uk_vacancy_skills_vacancy_normalized UNIQUE (vacancy_id, normalized_skill)
);

CREATE TABLE vacancy_requirements (
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    requirement TEXT NOT NULL,
    PRIMARY KEY (vacancy_id, list_order)
);

CREATE TABLE vacancy_responsibilities (
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    list_order INTEGER NOT NULL,
    responsibility TEXT NOT NULL,
    PRIMARY KEY (vacancy_id, list_order)
);

CREATE TABLE vacancy_sync_state (
    source VARCHAR(64) PRIMARY KEY,
    last_started_at TIMESTAMPTZ,
    last_successful_at TIMESTAMPTZ,
    last_finished_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    last_error VARCHAR(1000),
    market_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_vacancies_active_published ON vacancies (active, published_at DESC);
CREATE INDEX idx_vacancies_company ON vacancies (company);
CREATE INDEX idx_vacancies_company_id ON vacancies (company_id);
CREATE INDEX idx_vacancies_work_format ON vacancies (work_format);
CREATE INDEX idx_vacancies_salary_from ON vacancies (salary_from);
CREATE INDEX idx_vacancies_salary_to ON vacancies (salary_to);
CREATE INDEX idx_vacancies_content_hash ON vacancies (content_hash);
CREATE INDEX idx_vacancy_skills_normalized ON vacancy_skills (normalized_skill);
