ALTER TABLE periphery_types ADD COLUMN IF NOT EXISTS data_type VARCHAR(255) NOT NULL DEFAULT 'Float';

ALTER TABLE configurations ADD COLUMN IF NOT EXISTS name TEXT NOT NULL;
ALTER TABLE configurations ADD COLUMN IF NOT EXISTS description TEXT NOT NULL;
ALTER TABLE configuration_inbound_controllers ADD COLUMN IF NOT EXISTS name TEXT NOT NULL DEFAULT '';
ALTER TABLE configuration_outbound_controllers ADD COLUMN IF NOT EXISTS name TEXT NOT NULL DEFAULT '';

ALTER TABLE periphery_types ADD COLUMN IF NOT EXISTS dir VARCHAR(4) NOT NULL DEFAULT 'in';
UPDATE periphery_types SET dir = direction;
ALTER TABLE periphery_types DROP COLUMN IF EXISTS direction;

ALTER TABLE periphery_types RENAME COLUMN dir TO direction;
ALTER TABLE periphery_types ADD CONSTRAINT IF NOT EXISTS direction_check CHECK (direction = 'in' or direction = 'out' or direction = 'both');