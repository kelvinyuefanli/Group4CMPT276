-- V3: Add onboarding-related columns to user_preferences

ALTER TABLE user_preferences ADD COLUMN preferred_cuisines TEXT;
ALTER TABLE user_preferences ADD COLUMN rotate_cuisines BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE user_preferences ADD COLUMN disliked_foods TEXT;
ALTER TABLE user_preferences ADD COLUMN meal_schedule TEXT;
ALTER TABLE user_preferences ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
