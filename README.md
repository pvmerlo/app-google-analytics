# Google Analytics App for Enonic XP

This app extends the administration console by allowing to watch
Google Analytics statistics for current page or site.

Here's the documentation for this application:

* [Installing the App](docs/installing.md)


## Releases and Compatibility

| App version | Required XP version | Download | 
| ----------- | ------------------- | -------- |
| 1.2.0 | 6.3.0 | (http://repo.enonic.com/public/com/enonic/theme/superhero/1.1.0/superhero-1.1.0.jar) |

## Building and deploying

Build this application from the command line. Go to the root of the project and enter:

    ./gradlew clean build

To deploy the app, set `$XP_HOME` environment variable and enter:

    ./gradlew deploy
