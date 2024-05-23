#!/bin/sh

grep -i 'End StrategyCriteriaEvaluator' $1 | egrep -o 'time=[0-9]+ms' | cut -d'=' -f2 | cut -d'm' -f1 | awk '{ total += $1; count++ } END { print total/count }'