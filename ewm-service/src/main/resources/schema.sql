DROP SEQUENCE IF EXISTS user_sequence;
DROP SEQUENCE IF EXISTS category_sequence;
DROP SEQUENCE IF EXISTS event_sequence;
DROP SEQUENCE IF EXISTS request_sequence;
DROP SEQUENCE IF EXISTS compilation_sequence;
DROP SEQUENCE IF EXISTS comment_sequence;

CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE category_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE event_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE request_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE compilation_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE comment_sequence START WITH 1 INCREMENT BY 1;

DROP TABLE IF EXISTS compilation_events;
DROP TABLE IF EXISTS requests;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS compilations;

CREATE TABLE users (
    id BIGINT PRIMARY KEY DEFAULT nextval('user_sequence'),
    name VARCHAR(250) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('category_sequence'),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE events (
    id BIGINT PRIMARY KEY DEFAULT nextval('event_sequence'),
    title VARCHAR(120) NOT NULL,
    annotation VARCHAR(2000) NOT NULL,
    description VARCHAR(7000) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    created_on TIMESTAMP NOT NULL,
    published_on TIMESTAMP,
    state VARCHAR(32),
    initiator_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    lat DOUBLE PRECISION,
    lon DOUBLE PRECISION,
    paid BOOLEAN NOT NULL,
    participant_limit INTEGER NOT NULL,
    request_moderation BOOLEAN NOT NULL,
    views BIGINT NOT NULL DEFAULT 0,
    confirmed_requests BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_event_user FOREIGN KEY (initiator_id) REFERENCES users(id),
    CONSTRAINT fk_event_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE requests (
    id BIGINT PRIMARY KEY DEFAULT nextval('request_sequence'),
    created TIMESTAMP NOT NULL,
    event_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,

    CONSTRAINT fk_request_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_request_user FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT uc_request_unique UNIQUE (event_id, requester_id)
);

CREATE TABLE comments (
    id BIGINT PRIMARY KEY DEFAULT nextval('comment_sequence'),
    event_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    text TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,
    moderator_id BIGINT,
    moderation_reason TEXT,

    CONSTRAINT fk_comment_event     FOREIGN KEY (event_id)   REFERENCES events(id),
    CONSTRAINT fk_comment_author    FOREIGN KEY (author_id)  REFERENCES users(id),
    CONSTRAINT fk_comment_moderator FOREIGN KEY (moderator_id) REFERENCES users(id),
    CONSTRAINT ck_comment_text_len CHECK (length(trim(text)) BETWEEN 1 AND 5000)
);

CREATE INDEX idx_comments_event_status_created_at ON comments(event_id, status, created_at DESC);
CREATE INDEX idx_comments_author_created_at       ON comments(author_id, created_at DESC);
CREATE INDEX idx_comments_status_created_at       ON comments(status, created_at DESC);

CREATE TABLE compilations (
    id BIGINT PRIMARY KEY DEFAULT nextval('compilation_sequence'),
    title VARCHAR(50) NOT NULL,
    pinned BOOLEAN DEFAULT FALSE
);

CREATE TABLE compilation_events (
    compilation_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    PRIMARY KEY (compilation_id, event_id),

    CONSTRAINT fk_compilation FOREIGN KEY (compilation_id) REFERENCES compilations(id) ON DELETE CASCADE,
    CONSTRAINT fk_compilation_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);