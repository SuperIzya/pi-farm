-- Add periphery_connection_name to inbound addresses
DROP INDEX IF EXISTS idx_proc_inbound_config;
ALTER TABLE configuration_processor_inbound ADD COLUMN periphery_connection_name TEXT NOT NULL DEFAULT '';
ALTER TABLE configuration_processor_inbound DROP COLUMN processing_unit;
ALTER TABLE configuration_processor_inbound ALTER COLUMN periphery_id RENAME TO periphery_name;
ALTER TABLE configuration_processor_inbound ALTER COLUMN name RENAME TO processor_connection_name;
ALTER TABLE configuration_processor_inbound DROP PRIMARY KEY;
ALTER TABLE configuration_processor_inbound ADD PRIMARY KEY (configuration_id, processor_id, controller_id, periphery_name, periphery_connection_name);

-- Add periphery_connection_name to outbound addresses
DROP INDEX IF EXISTS idx_proc_outbound_config;
ALTER TABLE configuration_processor_outbound ADD COLUMN periphery_connection_name TEXT NOT NULL DEFAULT '';
ALTER TABLE configuration_processor_outbound DROP COLUMN processing_unit;
ALTER TABLE configuration_processor_outbound ALTER COLUMN periphery_id RENAME TO periphery_name;
ALTER TABLE configuration_processor_outbound ALTER COLUMN name RENAME TO processor_connection_name;
ALTER TABLE configuration_processor_outbound DROP PRIMARY KEY;
ALTER TABLE configuration_processor_outbound ADD PRIMARY KEY (configuration_id, processor_id, controller_id, periphery_name, periphery_connection_name);

-- Recreate indexes on new key columns
CREATE INDEX idx_proc_inbound_config ON configuration_processor_inbound(configuration_id, processor_id);
CREATE INDEX idx_proc_outbound_config ON configuration_processor_outbound(configuration_id, processor_id);