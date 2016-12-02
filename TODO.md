
##TODO: commute-alerts-dispatcher

###Before Next Release
* upgrade postgresql alerts table in commutegcm database
    - alerts table add high_priority field.
    - alerts table modify type enum from 'APP' to 'IN_APP'.
    
* confirm in-app messages json file on splendidbits user htdocs
    * standardise scp process for that.
    
* destroy and re-create jks store file by following the instructions in 'java_keystore_import_guide'

* replace nginx config file(s)
	* check root htdocs location for inapp messages.
	* verify http location for the in-app alerts feed.
		- https://api.commuteapp.io/alerts/v1/agency/0/route/inapp/alerts
	
	
	
###Later
* add caching to above endpoints
* alerts / routes / agencies public json feeds
* Remove code TODO:s


###Someday
* stats return for push-services?
* Fix quotas / accounts?
* Improve log display page css.
