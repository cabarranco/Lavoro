#!/bin/sh

echo $(date "+%H:%M:%S   %d/%m/%y") >> ./running_datetime_log.txt

/opt/puslse/bin/stop.sh

export LOG_DIR=/mnt/volume_strategy_runner_1

nohup java -jar -Daccount.percentageBalanceToSave=0.1 \
  -Daccount.opportunityMinAllocationSum=50.0 \
  -Daccount.maxEventConcentration=0.25 \
  -Daccount.maxStrategyConcentration=4 \
  -Daccount.maxStrategyEventConcentration=0.25 \
  -Desa.maxConnections=1 \
  -Dapp.logDirectory=${LOG_DIR} \
  -Dspring.profiles.active=prod  \
  -Xms3G -Xmx3G \
  -XX:+HeapDumpOnOutOfMemoryError \
  pulse-1.2.11.jar --logging.config=${LOG_DIR}/logback-spring.xml > /dev/null 2>&1 &
