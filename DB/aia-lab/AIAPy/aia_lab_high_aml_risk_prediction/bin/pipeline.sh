#!/bin/bash
# Created by CAB

set -e
set -x

export AIALAB="$( pwd )"
export PY_MODULES=${AIALAB}/AIAPy/aia-lab_high_aml_risk_prediction/modules
export PYTHONPATH=$PYTHONPATH:$AIAPY:$PY_MODULES

echo -e "\n\n start full pipeline... \n"
cd ${AIALAB}
# Allow execute rights
chmod +x ${AIALB}/AIAPy/aia_lab_high_aml_risk_prediction/bin/data_prep/data_prep.sh
chmod +x ${AIALB}/AIAPy/aia_lab_high_aml_risk_prediction/bin/models/model_training.sh
chmod +x ${AIALB}/AIAPy/aia_lab_high_aml_risk_prediction/bin/models/model_predict.sh

# Execute
bash ${AIALB}/AIAPy/aia_lab_high_aml_risk_prediction/bin/data_prep/data_prep.sh \
--data-folder-path /home/mount-vol/eap_data/mock_5000 \
--output-folder-path /home/wyoming/users/bla/data/aia-lab_high_aml_risk_prediction

bash ${AIALB}/AIAPy/aia_lab_high_aml_risk_prediction/bin/models/model_training.sh \
--output-folder-path /home/wyoming/users/bla/data/aia-lab_high_aml_risk_prediction

bash ${AIALB}/AIAPy/aia_lab_high_aml_risk_prediction/bin/models/model_predict.sh \
--output-folder-path /home/wyoming/users/bla/data/aia-lab_high_aml_risk_prediction

echo -e '\n finished full pipeline \n'
