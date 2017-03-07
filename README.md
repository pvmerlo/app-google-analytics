# Google Analytics App for Enonic XP

This app extends the administration console by allowing to watch
Google Analytics statistics for current page or site.

Here's the documentation for this application:

* [Installing the App](docs/installing.md)


## Releases and Compatibility

| App version | Required XP version | Download |
| ----------- | ------------------- | -------- |
| 1.7.0 | 6.9.0 | |
| 1.6.2 | 6.8.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.6.2/ga-1.6.2.jar) |
| 1.6.1 | 6.8.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.6.1/ga-1.6.1.jar) |
| 1.6.0 | 6.8.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.6.0/ga-1.6.0.jar) |
| 1.5.0 | 6.7.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.5.0/ga-1.5.0.jar) |
| 1.4.0 | 6.6.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.4.0/ga-1.4.0.jar) |
| 1.3.0 | 6.4.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.3.0/ga-1.3.0.jar) |
| 1.2.0 | 6.3.1 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.2.0/ga-1.2.0.jar) |
| 1.1.0 | 6.3.0 | [Download](http://repo.enonic.com/public/com/enonic/app/ga/1.1.0/ga-1.1.0.jar) |


## Building and deploying

Build this application from the command line. Go to the root of the project and enter:

    ./gradlew clean build

To deploy the app, set `$XP_HOME` environment variable and enter:

    ./gradlew deploy


## Releasing new version

To release a new version of this app, please follow the steps below:

1. Update `version` (and possibly `xpVersion`) in  `gradle.properties`.

2. Compile and deploy to our Maven repository:

    ./gradlew clean build uploadArchives

3. Update `README.md` file with new version information and compatibility.

4. Tag the source code using `git tag` command (where `X.X.X` is the released version):

    git tag vX.X.X

5. Update `gradle.properties` with the next snapshot version and commit changes.

6. Push the updated code to GitHub.

    git push origin master --tags
