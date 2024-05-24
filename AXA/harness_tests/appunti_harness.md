# Breve guida ed esempi per Harness
 Condizioni generale, bisogna stabilire un webhook tra i repo e Harness e salvare dei token (generati nel repo) tra i segreti di Harness.

## Costruire un processo
Si seleziona nella colonna a sinistra Pipelines e poi se ne crea una nuova.

* define git details
* define each stage
  * Overview
    * type: Build (or Deplot, Custom...)
    * name: ...
  * Infrastructure: Kupernetes
    * Select OS: Linux
    * Kubernetes Cluster: builds-deep-deep-ai-microservices
    * Namespace: builds-shared-axa-xl
  * Advanced: ConditionalExecution: <+trigger.targetBranch>=="main" (or <+<+trigger.targetBranch.contains("refs")>?false:true>)
  * Execution:
    * define each step
      * type: Run
      * name: ...
      * description: ...
      * Container Registry: jfrom-deep-deep-ai-microservices
      * Image: artifactory.platform.axaxl-cloud.com/docker/library/python:3.8
      * Shell: Bash
      * Command: (vedi esempi per altri) |
        ```
        python -m venv databricks_env
        . databricks_env/bin/activate
        pip install -r requirements_test.txt
        pip install databricks-cli
        export DATABRICKS_TOKEN=<+secrets.getValue("cb_db_prd_token")>
        export DATABRICKS_HOST=<+variable.databricks_prod_host>
        databricks job create --json-file jobs/create_pipeline.json
        
### Esempi su come costruire processi harness per CI/CD
Demo pytest
```
python -m venv broker_env
. broker_env/bin/activate
pip install -U pytest databricks-cli
export DATABRICKS_TOKEN=<+secrets.getValue("cb_db_prd_token")>
export DATABRICKS_HOST=<+variable.databricks_prod_host>
Jobid=$(databricks job create --json-file create_test_job.json | grep -i "job_id" | cut -d: -f 2)
echo $Jobid
databricks jobs run-now --job-id $Jobid
databricks jobs delete --job-id $Jobid
```

Preprare training
```
python -m venv broker_env
. broker_env/bin/activate
pip install databricks-cli
export TAG=<+trigger.payload.release.tag_name>
if [[ ! -n "$TAG" || TAG==null ]]
then
 echo "No tag"
else
 sed -i -e "s/git_branch/git_tag/g" jobs/create_training_pipeline.json
 sed -i -e "s/main/${TAG}/g" jobs/create_training_pipeline.json
fi
sed -i -e "s/{DEPLOY_ENV}/prod/g" jobs/create_training_pipeline.json
```

Deploy training
```
python -m venv broker_env
. broker_env/bin/activate
export DATABRICKS_TOKEN=<+secrets.getValue("cb_db_prd_token")>
export DATABRICKS_HOST=<+variable.databricks_prod_host>
# remove previous version of the job
Jobname=blbla_training
Jobid=$(databricks job list | grep -ml -w $Jobname | cut -f 1 -d " ")
until [ ! -n "$Jobid" ]; do
 databricks jobs delete --job-id $Jobid
 Jobid=$(databricks job list | grep -ml -w $Jobname | cut -f 1 -d " ")
done
databricks jobs create --json-file jobs/create_training_pipeline.json
```

### Examples of complete pipelines
Testing -> Deploying training pipeline

Testing step
 * buildEnvironment
   ```
   python -m venv test_env
   . broker_env/bin/activate
   pip install --upgrade pip
   pip install -r requirements.txt
   pip install -r requirements_test.txt
   pip install -U pytest databricks-cli
   ```
 * installPackage
   ```
   . broker_env/bin/activate
   # pip install
   ```
 * unitTests
   ```
   . test_env/bin/activate
   export DATABRICKS_TOKEN=<+secrets.getValue("cb_db_prd_token")>
   export DATABRICKS_HOST=<+variable.databricks_prod_host>
   Jobid=$(databricks job create --json-file tests/create_test_job.json | grep -i "job_id" | cut -d: -f 2)
   echo $Jobid
   if [ ! -n "$Jobid" ]
   then
    echo "Error: Runid not set or NULL"
    echo "check job definition"
    exit 1
   fi
   CONTR="TERMINATED"
   STATUS=$(databricks runs get-output --run-id $Runid | grep -i "life_cycle_state" | cut -d: -f 2 | cut -d'"' -f 2)
   until [ "$STATUS" == "$CONTR" ]; do
    sleep 5
    STATUS=$(databricks runs get-output --run-id $Runid | grep -i "life_cycle_state" | cut -d: -f 2 | cut -d'"' -f 2)
    echo $STATUS
   done
   databricks jobs delete --job-id $Jobid
   echo DONE
   # pytest tests/unit --junitxml=unit_tests.xml
   ```
 * integrationTests
   ```
   . test_env/bin/activate
   # pytest tests/integration --junitxml=integration_tests.xml
   ```
 
 Training step
 * prepare
   ```
   python -m venv deploy_env
   . broker_env/bin/activate
   pip install pytest databricks-cli
   export TAG=<+trigger.payload.release.tag_name>
   if [[ ! -n "$TAG" || TAG==null ]]
   then
    echo "No tag"
   else
    sed -i -e "s/git_branch/git_tag/g" jobs/create_training_pipeline.json
    sed -1 -e "s/main/${TAG}/g" jobs/create_training_pipeline.json
   fi
   sed -i -e "s/{DEPLOY_ENV}/prod/g" jobs/create_training_pipeline.json
   ```
 * deplot
   ```
   . broker_env/bin/activate
   export DATABRICKS_TOKEN=<+secrets.getValue("cb_db_prd_token")>
   export DATABRICKS_HOST=<+variable.databricks_prod_host>
   # remove previous version of the job
   Jobname=blbla_training
   Jobid=$(databricks job list | grep -ml -w $Jobname | cut -f 1 -d " ")
   until [ ! -n "$Jobid" ]; do
    databricks jobs delete --job-id $Jobid
    Jobid=$(databricks job list | grep -ml -w $Jobname | cut -f 1 -d " ")
   done
   databricks jobs create --json-file jobs/create_training_pipeline.json
   ```
