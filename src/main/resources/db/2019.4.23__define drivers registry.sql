create table if not exists driver_registry
(
    device varchar(256) not null,
    driver varchar(256) not null,
    id     int auto_increment (100, 1),

    primary key hash (id),
    constraint uq_device unique (device)
);

create table if not exists configurations
(
    name  varchar(256)   not null,
    graph varchar(16383) not null,
    id    int auto_increment (100, 1),

    primary key hash (id),
    constraint uq_name unique (name)
);
