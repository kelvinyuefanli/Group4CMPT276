-- V1: SmartCart initial schema

-- ============================================================
-- Users & Preferences
-- ============================================================

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_preferences (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    serving_size         INT NOT NULL DEFAULT 2,
    dietary_restrictions TEXT,
    allergies            TEXT,
    UNIQUE (user_id)
);

-- ============================================================
-- Recipes
-- ============================================================

CREATE TABLE recipes (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(500) NOT NULL,
    instructions     TEXT,
    cook_time_minutes INT,
    servings          INT,
    cuisine           VARCHAR(100),
    source            VARCHAR(50) DEFAULT 'gemini',
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE recipe_ingredients (
    id              BIGSERIAL PRIMARY KEY,
    recipe_id       BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_name VARCHAR(255) NOT NULL,
    quantity        DOUBLE PRECISION,
    unit            VARCHAR(50),
    canonical_name  VARCHAR(255)
);

-- ============================================================
-- Meal Plans
-- ============================================================

CREATE TABLE meal_plans (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_start_date DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE meal_plan_recipes (
    id           BIGSERIAL PRIMARY KEY,
    meal_plan_id BIGINT NOT NULL REFERENCES meal_plans(id) ON DELETE CASCADE,
    recipe_id    BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    day_of_week  VARCHAR(10) NOT NULL,
    meal_type    VARCHAR(10) NOT NULL,
    CONSTRAINT chk_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    CONSTRAINT chk_meal CHECK (meal_type IN ('BREAKFAST','LUNCH','DINNER','SNACK'))
);

-- ============================================================
-- Grocery Lists
-- ============================================================

CREATE TABLE grocery_lists (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    meal_plan_id BIGINT REFERENCES meal_plans(id) ON DELETE SET NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE grocery_list_items (
    id              BIGSERIAL PRIMARY KEY,
    grocery_list_id BIGINT NOT NULL REFERENCES grocery_lists(id) ON DELETE CASCADE,
    ingredient_name VARCHAR(255) NOT NULL,
    quantity        DOUBLE PRECISION,
    unit            VARCHAR(50),
    checked         BOOLEAN NOT NULL DEFAULT FALSE
);

-- ============================================================
-- Pantry
-- ============================================================

CREATE TABLE pantry_items (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ingredient_name VARCHAR(255) NOT NULL,
    quantity        DOUBLE PRECISION,
    unit            VARCHAR(50),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Indexes
-- ============================================================

CREATE INDEX idx_user_preferences_user  ON user_preferences(user_id);
CREATE INDEX idx_recipes_cuisine        ON recipes(cuisine);
CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients(recipe_id);
CREATE INDEX idx_meal_plans_user        ON meal_plans(user_id);
CREATE INDEX idx_meal_plan_recipes_plan ON meal_plan_recipes(meal_plan_id);
CREATE INDEX idx_grocery_lists_user     ON grocery_lists(user_id);
CREATE INDEX idx_grocery_list_items_list ON grocery_list_items(grocery_list_id);
CREATE INDEX idx_pantry_items_user      ON pantry_items(user_id);
