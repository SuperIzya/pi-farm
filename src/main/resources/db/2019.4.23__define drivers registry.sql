create table if not exists driver_registry
(
    device varchar(256) not null,
    driver varchar(256) not null,
    primary key hash (device)
);

create table if not exists configurations
(
    name  varchar(256)   not null,
    graph varchar(16383) not null,

    primary key hash (name)
);
