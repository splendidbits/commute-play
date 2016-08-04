


# Project TODO:

## TODO:
* Commit module dependencies under correct private / public branches
* change push_services internal branch to use splendid_log

* verify unexpected results from GCM.
	- 1100 Recipients in a message.
	- Correct Recipients to update behaviour.
	- Correct Recipients to fail behaviour.
	- Correct re-try behaviour
	- Correct fetch unprocessed tasks behaviour.

* Fix and verify active TaskQueue "active message" collections are cleaned.
* add caching to above endpoints
* Add expiresAt and startsAt attributes on Task
* alerts / routes / agencies public json feeds
* Remove code TODO:s

##ASAP
* ~~Finish automatic scheduling.~~
* ~~Make entire logging system dependency injectable~~
* ~~Implement TaskQueue task~~.
* ~~split push-services into own submodule~~
* ~~Add non-dispatch message listener callback from client~~.
* ~~Solidify URL endpoints. https://alerts.commuteapp.io/1.0/gcm/~~
* ~~Authenticate external app logging.~~
* ~~Change splendid log password~~
* ~~Test alerts removal sends cancel_message~~
* ~~Test alerts adding sends message~~
* ~~Remove automatic evolutions~~
* ~~add @Version to all models~~
* ~~rename agency_updates schema to agency_alerts~~
* ~~Refresh device tokens on response from GCM.~~
* ~~Finish Akka broadcast support~~

## model updates
### subscriptions_registrations
* ~~allow for multiple device-subscriptions for different agencies~~

### agency
* ~~Add agency phone number to agency~~
* ~~add timezone *(America/Los_Angeles)*~~
* ~~Add website URL to one agency~~

### route
* ~~Add websiteURL~~
* ~~Add transport_type~~
* ~~isSticky~~
* ~~isDefaultSubscription~~

#### RouteFeature
* ~~regular~~
* ~~istemporary~~
* ~~isOwl~~
* ~~privateService~~


##Someday
* stats return for push-services?
* Fix quotas / accounts?
* Improve log display page css.

* [Deploy to WAR?](https://github.com/play2war/play2-war-plugin)

#commuteIO#todo#gcm-server