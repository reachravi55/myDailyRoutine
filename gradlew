#!/usr/bin/env sh
##############################################################################
## Gradle start up script for UN*X
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec "$JAVA_HOME/bin/java" $DEFAULT_JVM_OPTS   -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"