-- Единый IdP (auth-service): привязка локального пользователя к субъекту OIDC.
ALTER TABLE users ADD COLUMN auth_subject UUID NULL;

ALTER TABLE users ADD CONSTRAINT uk_users_auth_subject UNIQUE (auth_subject);
