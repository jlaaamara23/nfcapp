-- Remove known development seed logins so they cannot be used in production.
UPDATE attendance_records
SET corrected_by_user_id = NULL
WHERE corrected_by_user_id IN (
    SELECT id FROM users WHERE username IN ('admin', 'employee1', 'employee2')
);

UPDATE audit_logs
SET actor_user_id = NULL
WHERE actor_user_id IN (
    SELECT id FROM users WHERE username IN ('admin', 'employee1', 'employee2')
);

UPDATE employees
SET user_id = NULL
WHERE user_id IN (
    SELECT id FROM users WHERE username IN ('admin', 'employee1', 'employee2')
);

DELETE FROM user_roles
WHERE user_id IN (
    SELECT id FROM users WHERE username IN ('admin', 'employee1', 'employee2')
);

DELETE FROM users
WHERE username IN ('admin', 'employee1', 'employee2');
