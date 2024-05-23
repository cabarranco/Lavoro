#!/bin/sh

JAVA_PID=$(jps | grep pulse | cut -d' ' -f1)
[ ! -z "$JAVA_PID" ] && while kill -15 $JAVA_PID  > /dev/null 2>&1; do sleep 1; done
