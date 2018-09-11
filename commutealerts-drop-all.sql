alter table if exists agency_alerts.alerts drop constraint if exists fk_alerts_route_id;
drop index if exists ix_alerts_route_id;

alter table if exists device_information.devices drop constraint if exists fk_devices_account_id;
drop index if exists ix_devices_account_id;

alter table if exists agency_alerts.locations drop constraint if exists fk_locations_alert_id;
drop index if exists ix_locations_alert_id;

alter table if exists api_accounts.platform_accounts drop constraint if exists fk_platform_accounts_account_id;
drop index if exists ix_platform_accounts_account_id;

alter table if exists agency_alerts.routes drop constraint if exists fk_routes_agency_id;
drop index if exists ix_routes_agency_id;

alter table if exists device_information.subscriptions drop constraint if exists fk_subscriptions_device_id;
drop index if exists ix_subscriptions_device_id;

alter table if exists device_information.subscriptions drop constraint if exists fk_subscriptions_route_id;
drop index if exists ix_subscriptions_route_id;

drop table if exists api_accounts.accounts cascade;
drop sequence if exists account_id_seq;

drop table if exists agency_alerts.agencies cascade;

drop table if exists agency_alerts.alerts cascade;

drop table if exists device_information.devices cascade;
drop sequence if exists device_id_seq;

drop table if exists agency_alerts.locations cascade;

drop table if exists api_accounts.platform_accounts cascade;
drop sequence if exists platform_account_id_seq;

drop table if exists agency_alerts.routes cascade;

drop table if exists device_information.subscriptions cascade;
drop sequence if exists subscriptions_id_seq;

