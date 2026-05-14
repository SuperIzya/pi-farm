-- Drop old configuration tables (no data exists)
DROP TABLE IF EXISTS configuration_processors;
DROP TABLE IF EXISTS configuration_inbound_controllers;
DROP TABLE IF EXISTS configuration_outbound_controllers;
DROP TABLE IF EXISTS configurations;

-- Recreate configurations
CREATE TABLE configurations (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL
);

-- Processors per configuration
CREATE TABLE configuration_processors (
    id BIGSERIAL PRIMARY KEY,
    configuration_id BIGINT NOT NULL,
    processing_unit VARCHAR(255) NOT NULL,
    parameters JSON,
    CONSTRAINT fk_config_processors_config FOREIGN KEY (configuration_id)
        REFERENCES configurations(id) ON DELETE CASCADE
);

-- Inbound addresses per processor (ordered)
CREATE TABLE configuration_processor_inbound (
    processor_id BIGSERIAL NOT NULL,
    configuration_id BIGINT NOT NULL,
    processing_unit VARCHAR(255) NOT NULL,
    controller_id BIGINT NOT NULL,
    periphery_id VARCHAR(255) NOT NULL,
    name TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (configuration_id, processor_id, controller_id, periphery_id),
    CONSTRAINT fk_proc_inbound_processor FOREIGN KEY (processor_id)
        REFERENCES configuration_processors(id) ON DELETE CASCADE,
    CONSTRAINT fk_proc_inbound_controller FOREIGN KEY (controller_id)
        REFERENCES controllers(id) ON DELETE CASCADE
);

-- Outbound addresses per processor (ordered)
CREATE TABLE configuration_processor_outbound (
    processor_id BIGSERIAL NOT NULL,
    configuration_id BIGINT NOT NULL,
    processing_unit VARCHAR(255) NOT NULL,
    controller_id BIGINT NOT NULL,
    periphery_id VARCHAR(255) NOT NULL,
    name TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (configuration_id, processor_id, controller_id, periphery_id),
    CONSTRAINT fk_proc_outbound_processor FOREIGN KEY (processor_id)
        REFERENCES configuration_processors(id) ON DELETE CASCADE,
    CONSTRAINT fk_proc_outbound_controller FOREIGN KEY (controller_id)
        REFERENCES controllers(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_config_processors_config_id ON configuration_processors(configuration_id);
CREATE INDEX idx_proc_inbound_config ON configuration_processor_inbound(configuration_id, processing_unit);
CREATE INDEX idx_proc_outbound_config ON configuration_processor_outbound(configuration_id, processing_unit);