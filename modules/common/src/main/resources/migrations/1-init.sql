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
CREATE INDEX idx_peripheries_type_id ON peripheries (type_id);


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

CREATE INDEX idx_controller_type_peripheries_controller_type_id ON controller_type_peripheries (controller_type_id);


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

CREATE INDEX idx_controllers_type_id ON controllers (type_id);
CREATE INDEX idx_controller_peripheries_controller_id ON controller_peripheries (controller_id);
