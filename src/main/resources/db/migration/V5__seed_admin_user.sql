INSERT INTO users (email, password, name, is_admin, created_at, updated_at)
VALUES (
    'admin@smartcart.app',
    '$2b$10$LOU6ZBDHeznizwVRPEL83ugXvpP39oPuOfuPk1WQ8Ni5.2JiE6yJO',
    'SmartCart Admin',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
