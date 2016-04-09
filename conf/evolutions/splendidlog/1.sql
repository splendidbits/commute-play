# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table splendidlog.applications (
  id                            serial not null,
  application_name              varchar(255),
  created_at                    timestamp,
  constraint pk_applications primary key (id)
);

create table splendidlog.logs (
  id                            serial not null,
  application_id                integer,
  log_time                      timestamp,
  log_level                     varchar(255),
  log_tag                       varchar(255),
  log_message                   TEXT,
  constraint pk_logs primary key (id)
);

alter table splendidlog.logs add constraint fk_logs_application_id foreign key (application_id) references splendidlog.applications (id) on delete restrict on update restrict;
create index ix_logs_application_id on splendidlog.logs (application_id);


# --- !Downs

alter table if exists splendidlog.logs drop constraint if exists fk_logs_application_id;
drop index if exists ix_logs_application_id;

drop table if exists splendidlog.applications cascade;

drop table if exists splendidlog.logs cascade;

