#!/bin/sh

#find current live games
egrep -o 'InplayResponse[(]eventId=[0-9]+' pulse.log  | cut -d'=' -f2 | sort -u | wc -l
#find no of market changes per min
grep '2020-06-04 04:13' pulse.log | grep -i 'esaclient'  | grep '"mc"' | wc -l
#find average time spent to compute a strat per min
grep '2020-06-04 04:13' pulse.log | grep -i 'End StrategyCriteriaEvaluator' | cut -d ' ' -f 13 | cut -d '=' -f 2 | cut -d 'm' -f 1 | awk '{ total += $1; count++ } END { print total/count }'