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
    define each step
      * type: Run
      * name: ...
      * description: ...
      * Container Registry: jfrom-deep-deep-ai-microservices
      * Image: artifactory.platform.axaxl-cloud.com/docker/library/python:3.8
      * Shell: Bash
      * Command: (vedi esempi per altri) |
        python -m venv databricks_env
        . databricks_env/bin/activate
        pip install -r requirements_test.txt
        pip install databricks-cli
        export DATABRICKS_TOKEN=<+secrets.getValue("cb_db_prd_token")>
        export DATABRICKS_HOST=<+variable.databricks_prod_host>
        databricks job create --json-file jobs/create_pipeline.json
        
## Esempi su come costruire processi harness per CI/CD



