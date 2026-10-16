CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120) NOT NULL UNIQUE,
    code            VARCHAR(40) NOT NULL UNIQUE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE workplaces (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(160) NOT NULL,
    code            VARCHAR(40) NOT NULL UNIQUE,
    address         VARCHAR(255),
    timezone        VARCHAR(64) NOT NULL DEFAULT 'Asia/Jerusalem',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(80) NOT NULL UNIQUE,
    email               VARCHAR(160) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    locked              BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE employees (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    employee_number     VARCHAR(40) NOT NULL UNIQUE,
    full_name           VARCHAR(160) NOT NULL,
    department_id       BIGINT REFERENCES departments(id),
    job_title           VARCHAR(120),
    email               VARCHAR(160) NOT NULL UNIQUE,
    phone               VARCHAR(40),
    employment_status   VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    workplace_id        BIGINT REFERENCES workplaces(id),
    invitation_token    VARCHAR(120),
    invitation_expires  TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_employment_status CHECK (employment_status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED'))
);

CREATE TABLE nfc_stations (
    id                  BIGSERIAL PRIMARY KEY,
    station_code        VARCHAR(64) NOT NULL UNIQUE,
    name                VARCHAR(160) NOT NULL,
    workplace_id        BIGINT NOT NULL REFERENCES workplaces(id),
    registration_value  VARCHAR(120) NOT NULL UNIQUE,
    tag_uid_hint        VARCHAR(64),
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    replaced_by_id      BIGINT REFERENCES nfc_stations(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_nfc_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'REPLACED'))
);

CREATE TABLE work_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    workplace_id        BIGINT NOT NULL REFERENCES workplaces(id),
    name                VARCHAR(120) NOT NULL,
    day_of_week         INTEGER NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    grace_minutes       INTEGER NOT NULL DEFAULT 10,
    overnight           BOOLEAN NOT NULL DEFAULT FALSE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_grace CHECK (grace_minutes >= 0)
);

CREATE TABLE attendance_records (
    id                      BIGSERIAL PRIMARY KEY,
    employee_id             BIGINT NOT NULL REFERENCES employees(id),
    nfc_station_id          BIGINT REFERENCES nfc_stations(id),
    event_type              VARCHAR(20) NOT NULL,
    server_timestamp        TIMESTAMPTZ NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    open_session            BOOLEAN NOT NULL DEFAULT FALSE,
    paired_record_id        BIGINT REFERENCES attendance_records(id),
    worked_minutes          INTEGER,
    late_minutes            INTEGER,
    early_departure_minutes INTEGER,
    status                  VARCHAR(30) NOT NULL DEFAULT 'VALID',
    correction_reason       VARCHAR(500),
    corrected_by_user_id    BIGINT REFERENCES users(id),
    corrected_at            TIMESTAMPTZ,
    original_timestamp      TIMESTAMPTZ,
    client_request_id       VARCHAR(80),
    notes                   VARCHAR(500),
    CONSTRAINT chk_event_type CHECK (event_type IN ('CLOCK_IN', 'CLOCK_OUT')),
    CONSTRAINT chk_attendance_status CHECK (status IN ('VALID', 'CORRECTED', 'INVALID', 'LATE', 'EARLY_DEPARTURE'))
);

CREATE UNIQUE INDEX uq_employee_open_session
    ON attendance_records (employee_id)
    WHERE open_session = TRUE AND event_type = 'CLOCK_IN';

CREATE UNIQUE INDEX uq_attendance_client_request
    ON attendance_records (employee_id, client_request_id)
    WHERE client_request_id IS NOT NULL;

CREATE INDEX idx_attendance_employee_time ON attendance_records (employee_id, server_timestamp DESC);
CREATE INDEX idx_attendance_station_time ON attendance_records (nfc_station_id, server_timestamp DESC);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   BIGINT REFERENCES users(id),
    action          VARCHAR(80) NOT NULL,
    entity_type     VARCHAR(80) NOT NULL,
    entity_id       VARCHAR(80),
    details         TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE attendance_rules (
    id                      BIGSERIAL PRIMARY KEY,
    workplace_id            BIGINT UNIQUE REFERENCES workplaces(id),
    grace_minutes           INTEGER NOT NULL DEFAULT 10,
    overtime_after_minutes  INTEGER NOT NULL DEFAULT 480,
    allow_overnight         BOOLEAN NOT NULL DEFAULT TRUE,
    duplicate_window_seconds INTEGER NOT NULL DEFAULT 60,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO roles (name) VALUES ('ADMIN'), ('EMPLOYEE');

INSERT INTO departments (name, code) VALUES
    ('הנהלה', 'MGMT'),
    ('משאבי אנוש', 'HR'),
    ('ייצור', 'PROD'),
    ('שירות לקוחות', 'CS');

INSERT INTO workplaces (name, code, address, timezone) VALUES
    ('משרד ראשי תל אביב', 'TLV-HQ', 'תל אביב', 'Asia/Jerusalem'),
    ('סניף חיפה', 'HFA-01', 'חיפה', 'Asia/Jerusalem');
