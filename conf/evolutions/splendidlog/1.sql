-- # --- Created by Ebean DDL
-- # To stop Ebean DDL generation, remove this comment and start using Evolutions

-- # --- !Ups

create table logs (
  id                        serial not null,
  time_utc                  timestamp,
  level                     varchar(255),
  provider                  varchar(255),
  message                   varchar(255),
  constraint pk_logs primary key (id));

CREATE USER splendidlog NOSUPERUSER PASSWORD 'splendidlog';
GRANT ALL PRIVILEGES ON logs TO splendidlog;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO splendidlog;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO postgres;

# --- !Downs

drop table if exists logs cascade;

