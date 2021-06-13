# amp-soe

Prerequisites:
- JDK 8
- sbt 1.3.x or higher

The project has the following sources:

* `QuickstartApp` -- contains the main method which bootstraps the application
* `QoERoutes` -- Akka HTTP `routes` defining exposed endpoints
* `QoERegistry` -- the actor which handles the event requests
* `JsonFormats` -- converts the JSON data from requests into Scala types and from Scala types into JSON responses

Once inside the project folder use the following command to run the code:
```
sbt run
```
and the following command to test the code
```
sbt test
```

The Javascript code are in the file `Title.html`.
Events that are getting fired to scala app in
* start
* ended
* playbackbitratechanged
* waiting
* resume


### Note
* `QoEBitrateSpec` Implemented the functionality to check if the number of bitrate switches is higher than 2 every 10 secs.
* `QoELongBufferingSpec` Implemented the functionality to check if buffering event longer than 1s
* `QoEFrequentBufferingSpec` Implemented the functionality to check if number of buffering events longer than 500ms is higher than 3 per 30 secs
* `QoEFrameSizeSpec` Partially implemented the functionality for available bitrate and highest possible bitrate.
  The code will give a warning if bitrate is lower than a certain threshold in full screen mode. 

