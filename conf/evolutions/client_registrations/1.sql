# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table registrations (
  dev_uuid                  varchar(255) not null,
  registration_id           varchar(255),
  time_registered           varchar(255),
  constraint pk_registrations primary key (dev_uuid))
;

create table subscriptions (
  devu_id                   varchar(255) not null,
  registration_dev_uuid     varchar(255),
  time_subscribed           varchar(255),
  constraint pk_subscriptions primary key (devu_id))
;

alter table subscriptions add constraint fk_subscriptions_registration_1 foreign key (registration_dev_uuid) references registrations (dev_uuid);
create index ix_subscriptions_registration_1 on subscriptions (registration_dev_uuid);



# --- !Downs

drop table if exists registrations cascade;

drop table if exists subscriptions cascade;

