create table if not exists driver_registry(
    device varchar(256) not null,
    driver varchar(256) not null,

    primary key HASH (device)
)