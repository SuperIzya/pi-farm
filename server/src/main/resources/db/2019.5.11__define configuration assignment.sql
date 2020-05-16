create table if not exists configuration_assignment (
    device varchar(256),
    configuration varchar(256),

    foreign key (device) references driver_registry(device),
    foreign key (configuration) references configurations(name)
);


