#!/bin/bash

set -e
set -x

export AIALAB="$( pwd )"
export PYTHONPATH=$PYTHONPATH:$AIALAB

cd ${AIALAB}

python -m AIAPy.aia_lab_high_aml_risk_prediction.modules.models.model_predict "$@"