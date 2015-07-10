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
### Interval in milliseconds at which to send metrics
#GS_CW_INTERVAL=10000

### AWS Authentication
export AWS_ACCESS_KEY_ID=MY_KEY        # EDIT THIS
export AWS_SECRET_KEY=MY_SECRET_KEY    # EDIT THIS

### Instance specific settings
export GS_CW_ENABLE_PER_INSTANCE_METRICS=true
#export GS_CW_INSTANCE_ID=hal9000    # This overrides the AWS instance identifier.

### EC2 Autoscaling
#export GS_CW_AUTOSCALING_GROUP_NAME=testgroup

### JMX metrics
#export GS_CW_JMX=true

### Geoserver metrics
#export GS_CW_WATCH_WMS=true
#export GS_CW_WATCH_WPS=true
#export GS_CW_WATCH_WFS=true
#export GS_CW_WATCH_CSW=true
#export GS_CW_WATCH_OSW=true
#export GS_CW_WATCH_WCS100=true
#export GS_CW_WATCH_WCS111=true
#export GS_CW_WATCH_WCS20=true
```
- Insert the AWS access key and secret key variables.
- Optionally edit the instance ID or autoscaling group name.
- Uncomment any desired metrics.
- Restart Tomcat.

#Todo

- Add more metrics.
- Improve configuration.
