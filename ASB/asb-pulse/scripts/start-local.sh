#!/bin/sh

export LOG_DIR=/mnt/d/ASBResearch/asb-pulse/log

nohup java -jar -Daccount.percentageBalanceToSave=0.1 \
  -Daccount.opportunityMinAllocationSum=50.0 \
  -Daccount.maxEventConcentration=0.25 \
  -Daccount.maxStrategyConcentration=4 \
  -Daccount.maxStrategyEventConcentration=0.25 \
  -Desa.maxConnections=1 \
  -Dapp.logDirectory=${LOG_DIR} \
  -Dspring.profiles.active=local  \
  -Xms3G -Xmx3G \
  -XX:+HeapDumpOnOutOfMemoryError \
  pulse-1.3.2.jar --logging.config=${LOG_DIR}/logback-spring.xml > /dev/null 2>&1 &
