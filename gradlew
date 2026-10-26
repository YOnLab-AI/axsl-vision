#!/bin/sh
set -eu
app_home=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then java="$JAVA_HOME/bin/java"; else java=java; fi
exec "$java" -classpath "$app_home/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
