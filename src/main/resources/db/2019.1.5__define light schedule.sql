create table if not exists light_schedule(
  start_time time,
  end_time time,
  status bit,

  primary key HASH (start_time, end_time, status)
)
