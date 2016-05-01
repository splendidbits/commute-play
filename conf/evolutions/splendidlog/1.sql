# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table splendidlog.applications (
  id                            integer not null,
  application_name              varchar(255),
  created_at                    timestamp without time zone,
  constraint pk_applications primary key (id)
);
create sequence splendidlog_app_id_seq increment by 1;

create table splendidlog.logs (
  log_id                        bigint not null,
  application_id                integer,
  log_time                      timestamp without time zone,
  log_level                     varchar(255),
  log_tag                       varchar(255),
  log_message                   TEXT,
  constraint pk_logs primary key (log_id)
);
create sequence splendidlog_entry_gen increment by 1;

alter table splendidlog.logs add constraint fk_logs_application_id foreign key (application_id) references splendidlog.applications (id) on delete restrict on update restrict;
create index ix_logs_application_id on splendidlog.logs (application_id);


# --- !Downs

alter table if exists splendidlog.logs drop constraint if exists fk_logs_application_id;
drop index if exists ix_logs_application_id;

drop table if exists splendidlog.applications cascade;
drop sequence if exists splendidlog_app_id_seq;

drop table if exists splendidlog.logs cascade;
drop sequence if exists splendidlog_entry_gen;

