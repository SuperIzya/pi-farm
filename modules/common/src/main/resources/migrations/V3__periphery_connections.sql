-- 1. Create periphery_connections table (no PK, bulk ops by periphery_type_id)
CREATE TABLE IF NOT EXISTS periphery_connections
(
    periphery_type_id BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,
    direction         VARCHAR(4)   NOT NULL CHECK (direction IN ('in', 'out', 'both')),
    units             VARCHAR(32)  NOT NULL,
    type              VARCHAR(255) NOT NULL,

    CONSTRAINT fk_periphery_connections_type FOREIGN KEY (periphery_type_id)
        REFERENCES periphery_types(id) ON DELETE CASCADE,
    CONSTRAINT uq_periphery_connections_name UNIQUE (periphery_type_id, name)
);

CREATE INDEX IF NOT EXISTS idx_periphery_connections_type_id
    ON periphery_connections (periphery_type_id);

-- 2. Data migration: move each periphery type's single connection into the new table
INSERT INTO periphery_connections (periphery_type_id, name, direction, units, type)
SELECT id, name, direction, units, data_type
FROM periphery_types;

-- 3. Drop migrated columns from periphery_types
ALTER TABLE periphery_types DROP COLUMN IF EXISTS direction;
ALTER TABLE periphery_types DROP COLUMN IF EXISTS units;
ALTER TABLE periphery_types DROP COLUMN IF EXISTS data_type;