-- Temporary guest user for pre-auth development.
-- Will be removed once the auth epic is implemented.

INSERT INTO users (email, password, name) VALUES ('guest@smartcart.local', '', 'Guest');
INSERT INTO user_preferences (user_id, serving_size) VALUES (1, 2);
