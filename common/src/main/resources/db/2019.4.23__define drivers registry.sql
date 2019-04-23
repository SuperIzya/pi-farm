create table if not exists driver_registry(
    driver varchar(256),
    device varchar(256),

    primary key HASH (driver, device)
)