ALTER TABLE periphery_channels
    ADD COLUMN presentation JSON;

ALTER TABLE controller_types
    ADD COLUMN presentation JSON;

ALTER TABLE controllers
    ADD COLUMN presentation JSON;
    