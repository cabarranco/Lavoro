import pandas as pd
from google.oauth2 import service_account
from google.cloud import bigquery
import pandas_gbq

SCOPES = ["https://www.googleapis.com/auth/cloud-platform"]

kay_path = {
    'qa':'keys/asbresearch-qa-cd3be678b257.json',
    'prod':'keys/asbresearch-prod-910a3d64fcc9.json'
}

def query_to_df(query, db='qa'):
    credentials = service_account.Credentials.from_service_account_file(
        kay_path[db], scopes=SCOPES
    )
    bqclient = bigquery.Client(credentials=credentials, project=credentials.project_id)
    df = bqclient.query(query).to_dataframe()
    return df

def execute_query(query, db='qa'):
    credentials = service_account.Credentials.from_service_account_file(
        kay_path[db], scopes=SCOPES
    )
    bqclient = bigquery.Client(credentials=credentials, project=credentials.project_id)
    bqclient.query(query)
    return
