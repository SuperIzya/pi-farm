ALTER TABLE periphery_types ADD COLUMN data_type VARCHAR(255) NOT NULL DEFAULT 'Float';

ALTER TABLE configurations ADD COLUMN name TEXT NOT NULL;
ALTER TABLE configurations ADD COLUMN description TEXT NOT NULL;
ALTER TABLE configuration_inbound_controllers ADD COLUMN name TEXT NOT NULL DEFAULT '';
ALTER TABLE configuration_outbound_controllers ADD COLUMN name TEXT NOT NULL DEFAULT '';

ALTER TABLE periphery_types ADD COLUMN dir VARCHAR(4) NOT NULL DEFAULT 'in';
UPDATE periphery_types SET dir = direction;
ALTER TABLE periphery_types DROP COLUMN direction;

ALTER TABLE periphery_types RENAME COLUMN dir TO direction;
ALTER TABLE periphery_types ADD CONSTRAINT direction_check CHECK (direction = 'in' or direction = 'out' or direction = 'both');