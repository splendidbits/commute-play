create table api_accounts.accounts (
  id                            bigint not null,
  organisation_name             varchar(255),
  account_email                 varchar(255),
  password_hash                 varchar(255),
  api_key                       varchar(255),
  estimated_limit_day           bigint,
  message_limit_day             bigint,
  active                        boolean default false,
  time_enrolled                 timestamp without time zone,
  version_modified              timestamptz not null,
  constraint pk_accounts primary key (id)
);
create sequence account_id_seq increment by 1;

create table agency_alerts.agencies (
  id                            serial not null,
  name                          varchar(255),
  phone                         varchar(255),
  external_uri                  TEXT,
  utc_offset                    float,
  version_modified              timestamptz not null,
  constraint pk_agencies primary key (id)
);

create table agency_alerts.alerts (
  id                            bigint not null,
  route_id                      varchar(255),
  type                          varchar(11),
  message_title                 TEXT,
  message_subtitle              TEXT,
  message_body                  TEXT,
  external_uri                  TEXT,
  last_updated                  timestamp without time zone,
  version_modified              timestamptz not null,
  constraint ck_alerts_type check ( type in ('','DISRUPTION','APP','WEATHER','DETOUR','MAINTENANCE','INFORMATION')),
  constraint pk_alerts primary key (id)
);
create sequence alert_id_seq increment by 1;

create table device_information.devices (
  id                            bigint not null,
  device_id                     TEXT,
  token                         TEXT,
  app_key                       varchar(255),
  user_key                      varchar(255),
  account_id                    bigint,
  time_registered               timestamp without time zone,
  version_modified              timestamptz not null,
  constraint pk_devices primary key (id)
);
create sequence device_id_seq increment by 1;

create table agency_alerts.locations (
  id                            integer not null,
  alert_id                      bigint,
  name                          varchar(255),
  latitude                      varchar(255),
  longitude                     varchar(255),
  message                       TEXT,
  sequence                      integer,
  date                          timestamp without time zone,
  version_modified              timestamptz not null,
  constraint pk_locations primary key (id)
);
create sequence location_id_seq increment by 1;

create table api_accounts.platform_accounts (
  id                            integer not null,
  account_id                    bigint,
  package_uri                   varchar(255),
  authorisation_key             varchar(255),
  certificate_body              TEXT,
  platform                      varchar(4),
  version_modified              timestamptz not null,
  constraint ck_platform_accounts_platform check ( platform in ('GCM','APNS')),
  constraint pk_platform_accounts primary key (id)
);
create sequence platform_account_id_seq increment by 1;

create table agency_alerts.routes (
  id                            varchar(255) not null,
  agency_id                     integer,
  route_id                      varchar(255),
  route_name                    varchar(255),
  route_flag                    varchar(18),
  transit_type                  varchar(10),
  is_default                    boolean,
  is_sticky                     boolean,
  external_uri                  TEXT,
  version_modified              timestamptz not null,
  constraint ck_routes_route_flag check ( route_flag in ('PRIVATE','TEMPORARY_ROUTE','OWL','CLOSED_PERMANENTLY','CLOSED_TEMPORARILY')),
  constraint ck_routes_transit_type check ( transit_type in ('SPECIAL','BUS','SUBWAY','CABLE','FERRY','BIKE_SHARE','RAIL','LIGHT_RAIL')),
  constraint pk_routes primary key (id)
);
create sequence route_id_seq increment by 1;

create table device_information.subscriptions (
  id                            bigint not null,
  device_id                     bigint not null,
  route_id                      varchar(255),
  time_subscribed               timestamp without time zone,
  version_modified              timestamptz not null,
  constraint pk_subscriptions primary key (id)
);
create sequence subscriptions_id_seq increment by 1;

alter table agency_alerts.alerts add constraint fk_alerts_route_id foreign key (route_id) references agency_alerts.routes (id) on delete restrict on update restrict;
create index ix_alerts_route_id on agency_alerts.alerts (route_id);

alter table device_information.devices add constraint fk_devices_account_id foreign key (account_id) references api_accounts.accounts (id) on delete restrict on update restrict;
create index ix_devices_account_id on device_information.devices (account_id);

alter table agency_alerts.locations add constraint fk_locations_alert_id foreign key (alert_id) references agency_alerts.alerts (id) on delete restrict on update restrict;
create index ix_locations_alert_id on agency_alerts.locations (alert_id);

alter table api_accounts.platform_accounts add constraint fk_platform_accounts_account_id foreign key (account_id) references api_accounts.accounts (id) on delete restrict on update restrict;
create index ix_platform_accounts_account_id on api_accounts.platform_accounts (account_id);

alter table agency_alerts.routes add constraint fk_routes_agency_id foreign key (agency_id) references agency_alerts.agencies (id) on delete restrict on update restrict;
create index ix_routes_agency_id on agency_alerts.routes (agency_id);

alter table device_information.subscriptions add constraint fk_subscriptions_device_id foreign key (device_id) references device_information.devices (id) on delete restrict on update restrict;
create index ix_subscriptions_device_id on device_information.subscriptions (device_id);

alter table device_information.subscriptions add constraint fk_subscriptions_route_id foreign key (route_id) references agency_alerts.routes (id) on delete restrict on update restrict;
create index ix_subscriptions_route_id on device_information.subscriptions (route_id);

