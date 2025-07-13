CREATE TABLE IF NOT EXISTS periphery_types
(
    id BIGSERIAL PRIMARY KEY,
    units VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    picture TEXT NOT NULL,
    direction VARCHAR(3) NOT NULL CHECK(direction IN('in', 'out', 'both') )
);

CREATE TABLE IF NOT EXISTS peripheries
(
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL,
    CONSTRAINT fk_periphery_type FOREIGN KEY (type_id)
        REFERENCES periphery_types(id)
);
CREATE INDEX IF NOT EXISTS idx_peripheries_type_id ON peripheries (type_id);


CREATE TABLE IF NOT EXISTS controller_types
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT         NOT NULL,
    code        TEXT         NOT NULL,
    schema      TEXT
);

CREATE TABLE IF NOT EXISTS controller_type_peripheries
(
    controller_type_id BIGINT NOT NULL,
    periphery_type_id  BIGINT NOT NULL,
    PRIMARY KEY (controller_type_id, periphery_type_id),
    CONSTRAINT fk_controller_type FOREIGN KEY (controller_type_id) REFERENCES controller_types (id),
    CONSTRAINT fk_periphery_type FOREIGN KEY (periphery_type_id) REFERENCES periphery_types (id)
);

CREATE INDEX IF NOT EXISTS idx_controller_type_peripheries_controller_type_id ON controller_type_peripheries (controller_type_id);


CREATE TABLE IF NOT EXISTS controllers
(
    id      BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL,
    CONSTRAINT fk_controller_type FOREIGN KEY (type_id) REFERENCES controller_types (id)
);

CREATE TABLE IF NOT EXISTS controller_peripheries
(
    controller_id BIGINT NOT NULL,
    periphery_id  BIGINT NOT NULL,
    PRIMARY KEY (controller_id, periphery_id),
    CONSTRAINT fk_controller FOREIGN KEY (controller_id) REFERENCES controllers (id),
    CONSTRAINT fk_periphery FOREIGN KEY (periphery_id) REFERENCES peripheries (id)
);

CREATE INDEX IF NOT EXISTS idx_controllers_type_id ON controllers (type_id);
CREATE INDEX IF NOT EXISTS idx_controller_peripheries_controller_id ON controller_peripheries (controller_id);

-- Create configurations table for base data
CREATE TABLE IF NOT EXISTS configurations (
                                id SERIAL PRIMARY KEY,
                                processing_unit VARCHAR(255) NOT NULL,
                                additional JSONB
);

-- Create join table for inbound controllers
CREATE TABLE IF NOT EXISTS configuration_inbound_controllers (
                                                   configuration_id INTEGER REFERENCES configurations(id) ON DELETE CASCADE,
                                                   controller_id INTEGER REFERENCES controllers(id) ON DELETE CASCADE,
                                                   PRIMARY KEY (configuration_id, controller_id)
);

-- Create join table for outbound controllers
CREATE TABLE IF NOT EXISTS configuration_outbound_controllers (
                                                    configuration_id INTEGER REFERENCES configurations(id) ON DELETE CASCADE,
                                                    controller_id INTEGER REFERENCES controllers(id) ON DELETE CASCADE,
                                                    PRIMARY KEY (configuration_id, controller_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_config_processing_unit ON configurations(processing_unit);
CREATE INDEX IF NOT EXISTS idx_config_inbound ON configuration_inbound_controllers(configuration_id);
CREATE INDEX IF NOT EXISTS idx_config_outbound ON configuration_outbound_controllers(configuration_id);
