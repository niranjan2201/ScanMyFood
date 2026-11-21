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
