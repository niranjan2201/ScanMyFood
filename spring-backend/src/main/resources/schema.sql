-- ============================================================
-- ScanMyFood Database Schema
-- Auto-runs on startup via spring.sql.init.mode=always
-- Uses IF NOT EXISTS so it's safe to re-run
-- ============================================================

-- ----------------------------------------------------------------
-- users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id                 VARCHAR(255) PRIMARY KEY,
    email                   VARCHAR(255),
    display_name            VARCHAR(255),
    is_onboarding_complete  BOOLEAN NOT NULL DEFAULT FALSE,
    dietary_preference      VARCHAR(50),
    country                 VARCHAR(10),
    height_feet             INTEGER,
    height_inches           INTEGER,
    weight_kg               DOUBLE PRECISION,
    goal                    VARCHAR(50),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- health_conditions  (reference / seed data table)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_conditions (
    name                    VARCHAR(255) PRIMARY KEY,
    description             TEXT,
    nutrients_to_limit      TEXT[],
    nutrients_to_increase   TEXT[],
    ingredients_to_limit    TEXT[],
    dietary_tags_to_limit   TEXT[]
);

-- ----------------------------------------------------------------
-- user_health_conditions  (join table)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_health_conditions (
    id              SERIAL PRIMARY KEY,
    user_id         VARCHAR(255) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    condition_name  VARCHAR(255) NOT NULL REFERENCES health_conditions(name) ON DELETE CASCADE,
    date_added      TIMESTAMP,
    date_removed    TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    severity        VARCHAR(20)
);

-- ----------------------------------------------------------------
-- daily_intake  (food scans / meals logged per user per day)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS daily_intake (
    id                  SERIAL PRIMARY KEY,
    user_id             VARCHAR(255) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    intake_name         VARCHAR(255),
    image_url           TEXT,
    source_of_intake    VARCHAR(100),
    portion             DOUBLE PRECISION DEFAULT 1.0,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    created_ts          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_ts          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- food_item  (individual items within a daily_intake row)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS food_item (
    id                  SERIAL PRIMARY KEY,
    daily_intake_id     INTEGER NOT NULL REFERENCES daily_intake(id) ON DELETE CASCADE,
    item_name           VARCHAR(255),
    canonical_name      VARCHAR(255),
    quantity_value      DOUBLE PRECISION,
    quantity_unit       VARCHAR(50),
    portion             DOUBLE PRECISION DEFAULT 1.0,
    created_ts          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- food_item_dictionary  (nutritional lookup by canonical name)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS food_item_dictionary (
    id                              SERIAL PRIMARY KEY,
    canonical_name                  VARCHAR(255) NOT NULL UNIQUE,
    calories_value_per_100g         DOUBLE PRECISION,
    calories_unit                   VARCHAR(20),
    protein_value_per_100g          DOUBLE PRECISION,
    protein_unit                    VARCHAR(20),
    total_carbohydrate_value_per_100g DOUBLE PRECISION,
    total_carbohydrate_unit         VARCHAR(20),
    total_fat_value_per_100g        DOUBLE PRECISION,
    total_fat_unit                  VARCHAR(20),
    dietary_fiber_value_per_100g    DOUBLE PRECISION,
    dietary_fiber_unit              VARCHAR(20),
    total_sugars_value_per_100g     DOUBLE PRECISION,
    total_sugars_unit               VARCHAR(20),
    sodium_value_per_100g           DOUBLE PRECISION,
    sodium_unit                     VARCHAR(20),
    iron_value_per_100g             DOUBLE PRECISION,
    iron_unit                       VARCHAR(20),
    verified_ind                    BOOLEAN DEFAULT FALSE,
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- food_item_alias  (locale/country display name → canonical name)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS food_item_alias (
    id              SERIAL PRIMARY KEY,
    canonical_name  VARCHAR(255) NOT NULL REFERENCES food_item_dictionary(canonical_name) ON DELETE CASCADE,
    country_code    VARCHAR(10) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    UNIQUE (canonical_name, country_code)
);

-- ----------------------------------------------------------------
-- product_label  (scanned product nutrition label data)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_label (
    id                              SERIAL PRIMARY KEY,
    product_name                    VARCHAR(255),
    category                        VARCHAR(100),
    total_quantity_value            DOUBLE PRECISION,
    total_quantity_unit             VARCHAR(20),
    serving_size_value              DOUBLE PRECISION,
    serving_size_unit               VARCHAR(20),
    energy_value_per_100g           DOUBLE PRECISION,
    energy_unit                     VARCHAR(20),
    protein_value_per_100g          DOUBLE PRECISION,
    protein_unit                    VARCHAR(20),
    total_carbohydrate_value_per_100g DOUBLE PRECISION,
    total_carbohydrate_unit         VARCHAR(20),
    total_fat_value_per_100g        DOUBLE PRECISION,
    total_fat_unit                  VARCHAR(20),
    saturated_fat_value_per_100g    DOUBLE PRECISION,
    saturated_fat_unit              VARCHAR(20),
    trans_fat_value_per_100g        DOUBLE PRECISION,
    trans_fat_unit                  VARCHAR(20),
    dietary_fiber_value_per_100g    DOUBLE PRECISION,
    dietary_fiber_unit              VARCHAR(20),
    total_sugars_value_per_100g     DOUBLE PRECISION,
    total_sugars_unit               VARCHAR(20),
    added_sugars_value_per_100g     DOUBLE PRECISION,
    added_sugars_unit               VARCHAR(20),
    sodium_value_per_100g           DOUBLE PRECISION,
    sodium_unit                     VARCHAR(20),
    iron_value_per_100g             DOUBLE PRECISION,
    iron_unit                       VARCHAR(20),
    potassium_value_per_100g        DOUBLE PRECISION,
    potassium_unit                  VARCHAR(20),
    calcium_value_per_100g          DOUBLE PRECISION,
    calcium_unit                    VARCHAR(20),
    added_by                        VARCHAR(255),
    created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- product_primary_concern  (AI-identified concerns for a label)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_primary_concern (
    id                  SERIAL PRIMARY KEY,
    product_label_id    INTEGER NOT NULL REFERENCES product_label(id) ON DELETE CASCADE,
    issue               TEXT,
    explanation         TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- product_concern_recommendation  (recommendations per concern)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_concern_recommendation (
    id          SERIAL PRIMARY KEY,
    concern_id  INTEGER NOT NULL REFERENCES product_primary_concern(id) ON DELETE CASCADE,
    food        VARCHAR(255),
    quantity    VARCHAR(100),
    reasoning   TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- meal_dictionary  (AI-generated composite meal definitions)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS meal_dictionary (
    id                          SERIAL PRIMARY KEY,
    canonical_meal_name         VARCHAR(255) NOT NULL UNIQUE,
    display_meal_name           VARCHAR(255),
    recipe_items                JSONB,
    standard_quantity_value     DOUBLE PRECISION,
    standard_quantity_unit      VARCHAR(20),
    calories_value              DOUBLE PRECISION,
    calories_unit               VARCHAR(20),
    protein_value               DOUBLE PRECISION,
    protein_unit                VARCHAR(20),
    total_carbohydrate_value    DOUBLE PRECISION,
    total_carbohydrate_unit     VARCHAR(20),
    total_fat_value             DOUBLE PRECISION,
    total_fat_unit              VARCHAR(20),
    dietary_fiber_value         DOUBLE PRECISION,
    dietary_fiber_unit          VARCHAR(20),
    total_sugars_value          DOUBLE PRECISION,
    total_sugars_unit           VARCHAR(20),
    sodium_value                DOUBLE PRECISION,
    sodium_unit                 VARCHAR(20),
    verified_ind                BOOLEAN DEFAULT FALSE,
    created_by_user_id          VARCHAR(255),
    created_at                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
-- Indexes for common query patterns
-- ================================================================
CREATE INDEX IF NOT EXISTS idx_daily_intake_user_id      ON daily_intake(user_id);
CREATE INDEX IF NOT EXISTS idx_daily_intake_created_ts   ON daily_intake(created_ts);
CREATE INDEX IF NOT EXISTS idx_food_item_daily_intake_id ON food_item(daily_intake_id);
CREATE INDEX IF NOT EXISTS idx_food_item_canonical_name  ON food_item(canonical_name);
CREATE INDEX IF NOT EXISTS idx_food_item_alias_display   ON food_item_alias(display_name, country_code);
CREATE INDEX IF NOT EXISTS idx_user_hc_user_id           ON user_health_conditions(user_id);
CREATE INDEX IF NOT EXISTS idx_product_concern_label_id  ON product_primary_concern(product_label_id);

-- ----------------------------------------------------------------
-- Verified Reference Seed Data (IFCT 2017 & USDA FoodData Central)
-- ----------------------------------------------------------------
INSERT INTO food_item_dictionary (
    canonical_name, calories_value_per_100g, calories_unit,
    protein_value_per_100g, protein_unit,
    total_carbohydrate_value_per_100g, total_carbohydrate_unit,
    total_fat_value_per_100g, total_fat_unit,
    dietary_fiber_value_per_100g, dietary_fiber_unit,
    total_sugars_value_per_100g, total_sugars_unit,
    sodium_value_per_100g, sodium_unit,
    iron_value_per_100g, iron_unit, verified_ind
) VALUES
('cooked_white_rice', 130.0, 'kcal', 2.7, 'g', 28.2, 'g', 0.3, 'g', 0.4, 'g', 0.1, 'g', 1.0, 'mg', 0.2, 'mg', TRUE),
('raw_white_rice', 345.0, 'kcal', 6.8, 'g', 78.2, 'g', 0.5, 'g', 1.3, 'g', 0.2, 'g', 5.0, 'mg', 0.7, 'mg', TRUE),
('cooked_brown_rice', 111.0, 'kcal', 2.6, 'g', 23.0, 'g', 0.9, 'g', 1.8, 'g', 0.4, 'g', 5.0, 'mg', 0.4, 'mg', TRUE),
('roti', 297.0, 'kcal', 9.6, 'g', 55.7, 'g', 3.7, 'g', 9.6, 'g', 1.0, 'g', 180.0, 'mg', 3.5, 'mg', TRUE),
('rajma_curry', 143.0, 'kcal', 5.7, 'g', 17.8, 'g', 4.2, 'g', 4.0, 'g', 1.5, 'g', 320.0, 'mg', 1.8, 'mg', TRUE),
('dal_tadka', 115.0, 'kcal', 6.5, 'g', 14.2, 'g', 3.5, 'g', 3.2, 'g', 0.8, 'g', 280.0, 'mg', 1.6, 'mg', TRUE),
('chana_masala', 166.0, 'kcal', 7.3, 'g', 21.0, 'g', 5.2, 'g', 5.6, 'g', 2.0, 'g', 350.0, 'mg', 2.2, 'mg', TRUE),
('paneer_curry', 195.0, 'kcal', 8.5, 'g', 7.2, 'g', 15.0, 'g', 2.1, 'g', 2.8, 'g', 410.0, 'mg', 1.1, 'mg', TRUE),
('paneer_raw', 265.0, 'kcal', 18.3, 'g', 1.2, 'g', 20.8, 'g', 0.0, 'g', 1.2, 'g', 22.0, 'mg', 0.2, 'mg', TRUE),
('idli', 136.0, 'kcal', 4.6, 'g', 27.5, 'g', 0.4, 'g', 1.4, 'g', 0.5, 'g', 190.0, 'mg', 0.8, 'mg', TRUE),
('plain_dosa', 206.0, 'kcal', 4.0, 'g', 29.0, 'g', 8.0, 'g', 1.8, 'g', 0.6, 'g', 210.0, 'mg', 1.2, 'mg', TRUE),
('sambar', 75.0, 'kcal', 2.8, 'g', 10.5, 'g', 2.2, 'g', 2.5, 'g', 2.0, 'g', 310.0, 'mg', 0.9, 'mg', TRUE),
('curd', 61.0, 'kcal', 3.5, 'g', 4.7, 'g', 3.3, 'g', 0.0, 'g', 4.7, 'g', 46.0, 'mg', 0.1, 'mg', TRUE),
('ghee', 900.0, 'kcal', 0.0, 'g', 0.0, 'g', 100.0, 'g', 0.0, 'g', 0.0, 'g', 0.0, 'mg', 0.0, 'mg', TRUE),
('cooking_oil', 884.0, 'kcal', 0.0, 'g', 0.0, 'g', 100.0, 'g', 0.0, 'g', 0.0, 'g', 0.0, 'mg', 0.0, 'mg', TRUE),
('butter', 717.0, 'kcal', 0.9, 'g', 0.1, 'g', 81.1, 'g', 0.0, 'g', 0.1, 'g', 576.0, 'mg', 0.0, 'mg', TRUE),
('boiled_egg', 155.0, 'kcal', 12.6, 'g', 1.1, 'g', 10.6, 'g', 0.0, 'g', 1.1, 'g', 124.0, 'mg', 1.8, 'mg', TRUE),
('cooked_chicken_breast', 165.0, 'kcal', 31.0, 'g', 0.0, 'g', 3.6, 'g', 0.0, 'g', 0.0, 'g', 74.0, 'mg', 1.0, 'mg', TRUE),
('raw_chicken_breast', 120.0, 'kcal', 22.5, 'g', 0.0, 'g', 2.6, 'g', 0.0, 'g', 0.0, 'g', 65.0, 'mg', 0.7, 'mg', TRUE),
('banana', 89.0, 'kcal', 1.1, 'g', 22.8, 'g', 0.3, 'g', 2.6, 'g', 12.2, 'g', 1.0, 'mg', 0.3, 'mg', TRUE),
('apple', 52.0, 'kcal', 0.3, 'g', 13.8, 'g', 0.2, 'g', 2.4, 'g', 10.4, 'g', 1.0, 'mg', 0.1, 'mg', TRUE),
('cow_milk', 61.0, 'kcal', 3.2, 'g', 4.8, 'g', 3.3, 'g', 0.0, 'g', 5.1, 'g', 43.0, 'mg', 0.0, 'mg', TRUE),
('cooked_oats', 71.0, 'kcal', 2.5, 'g', 12.0, 'g', 1.5, 'g', 1.7, 'g', 0.3, 'g', 49.0, 'mg', 0.9, 'mg', TRUE),
('boiled_potato', 87.0, 'kcal', 1.9, 'g', 20.1, 'g', 0.1, 'g', 1.8, 'g', 0.9, 'g', 4.0, 'mg', 0.3, 'mg', TRUE),
('salad_greens', 20.0, 'kcal', 0.8, 'g', 3.8, 'g', 0.2, 'g', 1.2, 'g', 2.5, 'g', 10.0, 'mg', 0.4, 'mg', TRUE)
ON CONFLICT (canonical_name) DO UPDATE SET
    calories_value_per_100g = EXCLUDED.calories_value_per_100g,
    protein_value_per_100g = EXCLUDED.protein_value_per_100g,
    total_carbohydrate_value_per_100g = EXCLUDED.total_carbohydrate_value_per_100g,
    total_fat_value_per_100g = EXCLUDED.total_fat_value_per_100g,
    dietary_fiber_value_per_100g = EXCLUDED.dietary_fiber_value_per_100g,
    total_sugars_value_per_100g = EXCLUDED.total_sugars_value_per_100g,
    sodium_value_per_100g = EXCLUDED.sodium_value_per_100g,
    iron_value_per_100g = EXCLUDED.iron_value_per_100g,
    verified_ind = TRUE;

INSERT INTO food_item_alias (canonical_name, country_code, display_name) VALUES
('cooked_white_rice', 'IN', 'Chawal'),
('cooked_white_rice', 'US', 'White Rice (Cooked)'),
('roti', 'IN', 'Roti / Chapati'),
('rajma_curry', 'IN', 'Rajma'),
('dal_tadka', 'IN', 'Dal Tadka'),
('chana_masala', 'IN', 'Chole'),
('paneer_curry', 'IN', 'Paneer Curry'),
('boiled_egg', 'IN', 'Ubla Anda'),
('boiled_egg', 'US', 'Hard-Boiled Egg'),
('curd', 'IN', 'Dahi')
ON CONFLICT (canonical_name, country_code) DO NOTHING;

