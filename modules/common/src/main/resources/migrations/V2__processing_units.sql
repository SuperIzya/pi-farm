ALTER TABLE periphery_types ADD COLUMN type VARCHAR(255) NOT NULL DEFAULT 'Int';

ALTER TABLE configurations ADD COLUMN name TEXT NOT NULL;
ALTER TABLE configurations ADD COLUMN description TEXT NOT NULL;

CREATE TABLE IF NOT EXISTS processing_units
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    params      JSON
);

CREATE TABLE IF NOT EXISTS processing_unit_connections
(
    processing_unit_id BIGSERIAL,
    direction VARCHAR(3) NOT NULL CHECK (direction IN ('in', 'out')),
    type VARCHAR(255) NOT NULL,
    units VARCHAR(255) NOT NULL,
    CONSTRAINT fk_processing_unit_connections_processing_unit_id FOREIGN KEY (processing_unit_id) REFERENCES processing_units(id)
);
