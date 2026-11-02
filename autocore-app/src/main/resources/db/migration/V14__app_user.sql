-- V14__app_user.sql
-- Skapar tabellen app_user för autentisering och seedar tre testkonton (ADMIN, WORKSHOP, READ_ONLY).
-- Lösenorden är hashade med BCrypt i DelegatingPasswordEncoder-format ({bcrypt}...).
-- Klartextlösenorden finns INTE i denna fil och får aldrig committas till Git.

CREATE TABLE app_user
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role          TEXT NOT NULL,
    enabled       INTEGER NOT NULL DEFAULT 1
);

-- Testkonton för demonstration.
INSERT INTO app_user (username, password_hash, role, enabled)
VALUES ('admin',    '{bcrypt}$2a$10$rAPneqeqAryg21gjrROfauD/YdcFXeHGYn.c9DfTMg8Flz6rUyn3W', 'ADMIN', 1),
       ('workshop', '{bcrypt}$2a$10$YFRuoUxNgziS3JyyysLXLuKLhcGYHZKy/l0Fw2lX0tpWRHYXbGKqK', 'WORKSHOP', 1),
       ('readonly', '{bcrypt}$2a$10$ylz55mI74nk4oMu2jXd5Ee9O4L56d0b/GmTweDLxxIXFEoHcQBMpG', 'READ_ONLY', 1);