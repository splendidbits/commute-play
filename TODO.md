# Project TODO:

##ASAP
* ~~Finish automatic scheduling.~~
* ~~Make entire logging system dependency injectable~~
* ~~Implement TaskQueue task~~.

* Add non-dispatch message listener callback from client.
* Authenticate external app logging.
* Solidify URL endpoints. https://alerts.commuteapp.io/1.0/gcm/
* Create json agency alert endpoints with caching
* Change splendid log password
* Test alerts removal sends cancel_message
* Test alerts adding sends message
* Remove automatic evolutions
* TEST TEST TEST

## model updates
### subscriptions_registrations
* allow for multiple device-subscriptions for different agencies

### agency
* Make primary key to be composite of agency and routeid
* Add agency phone number to agency
* Add agency full name to agency
* add timezone *(America/Los_Angeles)*
* Add website URL to one agency

### route
* Add websiteURL
* Add transport_type
* Add issticky
* Add isdefaultsubscription

#### RouteFeature
* Feature istemporary
* Feature isowl
* Feature privateService

##Time permitting
* Fix quotas / accounts?
* Improve log display page css.

##Someday
* ~~Finish Akka broadcast support~~
* [Deploy to WAR?](https://github.com/play2war/play2-war-plugin)

#commuteIO#todo#gcm-server