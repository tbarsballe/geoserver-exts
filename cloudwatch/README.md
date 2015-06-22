Gs-cloudwatch is a GeoServer Extension that reports various GeoServer metrics to the Amazon Web Services cloudwatch service. This will allow better autoscaling of a GeoServer cluster or notification when GeoServer is under heavy load.

These instructions assume that the Opengeo Suite installer for Linux was used to install GeoServer. Paths may be different for other systems.

#Install

- Run `mvn compile`

- Copy `target/cloudwatch-1.0-SNAPSHOT.jar` to the GeoServer lib directory (`/usr/share/opengeo/geoserver/WEB-INF/lib/`)

- Run `mvn dependency:copy-dependencies`

- Copy `target/dependency/*.jar` to the GeoServer lib directory.

#Configuration

- Generate an AWS access key in the AWS console.
- Edit `/usr/share/tomcat7/setenv.sh` (create it if necessary) and add the following environment variables:
```
	AWS_ACCESS_KEY_ID
	AWS_SECRET_KEY
	AUTOSCALINGGROUPNAME
```
- Restart Tomcat.

#Todo

- Add more metrics.
- Add per instance metrics.
- Configuration file for selecting metrics.
