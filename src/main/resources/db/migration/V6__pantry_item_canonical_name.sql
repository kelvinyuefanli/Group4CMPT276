ALTER TABLE pantry_items
    ADD COLUMN canonical_name VARCHAR(255);

UPDATE pantry_items
SET canonical_name = LOWER(TRIM(ingredient_name))
WHERE canonical_name IS NULL;

CREATE INDEX idx_pantry_items_user_canonical
    ON pantry_items(user_id, canonical_name);
