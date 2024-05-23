#!/bin/sh

echo $(date "+%H:%M:%S   %d/%m/%y") >> ./running_datetime_log.txt

/opt/asb-data-collector/bin/stop.sh

export LOG_DIR=/opt/asb-data-collector/log

nohup java -jar -Desa.maxConnections=7 \
        -Dapp.logDirectory=${LOG_DIR} \
        -Dspring.profiles.active=prod  \
        -Xms1G -Xmx1G \
        -XX:+HeapDumpOnOutOfMemoryError \
        asb-data-collector.jar > /dev/null 2>&1 &
