create table splendidlog.applications (
  id                            bigint not null,
  name                          varchar(255),
  created_at                    timestamp without time zone,
  constraint pk_applications primary key (id)
);
create sequence log_application_id_seq increment by 1;

create table splendidlog.entries (
  id                            bigint not null,
  application_id                bigint,
  created_at                    timestamp without time zone,
  level                         varchar(255),
  tag                           varchar(255),
  message                       TEXT,
  constraint pk_entries primary key (id)
);
create sequence log_entry_gen increment by 1;

alter table splendidlog.entries add constraint fk_entries_application_id foreign key (application_id) references splendidlog.applications (id) on delete restrict on update restrict;
create index ix_entries_application_id on splendidlog.entries (application_id);

