INSERT INTO users (username, email, password_hash, enabled, locked, failed_login_count)
SELECT
    'jlaaam',
    'jlaaam@sreamline.tech',
    '$2b$10$yt6jM85oquDCv7vsWzn3LOn4L433V1vHvlZBPl/COmLPJOxc0jbk2',
    TRUE,
    FALSE,
    0
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'jlaaam');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ADMIN'
WHERE u.username = 'jlaaam'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );
