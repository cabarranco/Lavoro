#!/bin/bash

set -e
set -x

export AIALAB="$( pwd )"
export PYTHONPATH=$PYTHONPATH:$AIALAB

echo -e "\n\n start prepare data... \n"
cd ${AIALAB}

python -m AIAPy.aia_lab_high_aml_risk_prection.modules.data_prep.data_prep "$@"

echo -e '\n finished prepare data \n'