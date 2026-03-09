# SmartCart Database

## Engine
PostgreSQL (hosted on Render). H2 available for local development via the `dev` profile.

## Migrations
Managed by Flyway. Migration files live in `src/main/resources/db/migration/`.

| Migration | Description |
|-----------|-------------|
| `V1__initial_schema.sql` | Core tables for users, recipes, meal plans, grocery lists, pantry |

## Schema (V1)

### users
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| password | VARCHAR(255) | BCrypt hash |
| name | VARCHAR(255) | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### user_preferences
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK -> users, UNIQUE |
| serving_size | INT | Default 2 |
| dietary_restrictions | TEXT | Comma-separated |
| allergies | TEXT | Comma-separated |

### recipes
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| title | VARCHAR(500) | |
| instructions | TEXT | |
| cook_time_minutes | INT | |
| servings | INT | |
| cuisine | VARCHAR(100) | |
| source | VARCHAR(50) | 'gemini' / 'manual' |
| created_at | TIMESTAMP | |

### recipe_ingredients
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| recipe_id | BIGINT | FK -> recipes |
| ingredient_name | VARCHAR(255) | |
| quantity | DOUBLE PRECISION | |
| unit | VARCHAR(50) | |
| canonical_name | VARCHAR(255) | For normalization/dedup |

### meal_plans
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK -> users |
| week_start_date | DATE | |
| created_at | TIMESTAMP | |

### meal_plan_recipes
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| meal_plan_id | BIGINT | FK -> meal_plans |
| recipe_id | BIGINT | FK -> recipes |
| day_of_week | VARCHAR(10) | MON-SUN |
| meal_type | VARCHAR(10) | BREAKFAST / LUNCH / DINNER / SNACK |

### grocery_lists
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK -> users |
| meal_plan_id | BIGINT | FK -> meal_plans (nullable) |
| created_at | TIMESTAMP | |

### grocery_list_items
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| grocery_list_id | BIGINT | FK -> grocery_lists |
| ingredient_name | VARCHAR(255) | |
| quantity | DOUBLE PRECISION | |
| unit | VARCHAR(50) | |
| checked | BOOLEAN | Default false |

### pantry_items
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK -> users |
| ingredient_name | VARCHAR(255) | |
| quantity | DOUBLE PRECISION | |
| unit | VARCHAR(50) | |
| updated_at | TIMESTAMP | |

## ER Diagram (Simplified)

```
users 1──* user_preferences
users 1──* meal_plans
users 1──* grocery_lists
users 1──* pantry_items

recipes 1──* recipe_ingredients
meal_plans 1──* meal_plan_recipes *──1 recipes
grocery_lists 1──* grocery_list_items
```

## Running Locally

```bash
# With H2 (no Postgres needed):
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# With local Postgres:
export DATABASE_URL=jdbc:postgresql://localhost:5432/smartcart
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=yourpassword
./mvnw spring-boot:run
```
