create table if not exists configuration_assignment (
    device_id int,
    configuration_id int,
    id int auto_increment(100, 1) primary key,

    foreign key (device_id) references driver_registry(id),
    foreign key (configuration_id) references configurations(id)
);


