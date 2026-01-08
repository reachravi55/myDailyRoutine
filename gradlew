#!/usr/bin/env sh
APP_HOME=$(cd "$(dirname "$0")"; pwd)
JAVA_EXE="$JAVA_HOME/bin/java"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec "$JAVA_EXE" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"