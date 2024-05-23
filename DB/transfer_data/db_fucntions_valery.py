eap_env = "preprod"
username = "svc_eap_prod_aiab"
data_path = "" # TDB
table_name_ns = ""
database_folder_path = "/project/aiab/private/databases/"+table_name_ns
script_filder_parth = ""
parquet_file_name = table_name_ns+".parquet"
parquet_file_path = script_folder_path+"/"+parquet_file_name

password = ""
# import getpass
# password = getpass.getpass()

from aia_core.db.impala_db import ImpalaDB
from aia_code.utils.config import Config

import numpy as np
import pandas as pd
import datetime as dt
import logging
import argparse
from argparse import ArgumentParser
from datetime import datetime, timedelta
import os
from typing import List
import re
from pathlib import Path

logger = logging.getLogger(__name__)

# set config
conf_path = ""
if eap_env == "preprod":
    conf_path = str(Path("db.yaml"))
elif eap_env == "uat":
    conf_path = "input path"

conf = Config(conf_path)

use_sql_alchemy = False
use_connector = "impyla"

impala_connector = conf.get_value(f"connection.eap.{eap_env}.impala")
connector = conf.get_value(f"connector.{use_connector}")
sql_alchemy_config = cong.get_value("sql_alchemy")

config = {
    "use_sql_alchemy": use_sql_alchemy,
    "use_connector": use_connector,
    "connector": connector,
    "connection": impala_connection,
    "sql_alchemy": sql_alchemy_config,
    "user": username,
    "password": password
}

db = ImpalaDb(config=config)

df = pd.DataFrame()
if data_path.endswith('.csv'):
    df = pd.read_csv(data_path, index_col=0)
elif datapath.endswith('.pkl.gz'):
    df = pd.read_pickle(data_path)
# df.dtypes

for col in df.select_dtypes([datetime64[ns]]):
    df[col] = df[col].astype(str)

df.to_parquet(parquet_file_name)

column_names = list(df.columns)
column_names_str = ",".join(column_names)
table_name = "pc_aiab_analysis.".table_name_ns

type_dictionary = {
    "object": "STRING",
    "float64": "DOUBLE",
    "int64": "BIGINT",
    "datetime64[ns]": "TIMESTAMP",
    "bool": "bOOLEAN"
}

sql_query = "CREATE TABLE "+table_name+" ("
for column, col_type in zip(df.columns, df.dtypes):
    if column != df.columns[0]:
        sql_query = sql_query +", "
    sql_query = sql_query+str(column)+" "+type_dictionary[str(col_type)]
sql_query = sql_query+") STORED AS PARQUET"

db.insert_data(sql_query)

sql = "invalidate metadata "+table_name
db.insert_data(sql)

sql = "refresh "+table_name
db.insert_data(sql)

# ! hdfs dfs -ls $database_folder_path
# ! hdfs dfs -put -f $parquet_file_path $database_folder_path
# ! hdfs dfs -ls $database_folder_path

sql = """invalidate metadata pc_aiab_analysis.paytrack_adf_payments_confidence_ann"""
db.insert_data(sql)

sql = """refresh pc_aiab_analysis.paytrack_adf_payments_confidence_ann"""
db.insert_data(sql)

db.get_data("SELECT COUNT(1) FROM pc_aiab_analysis.paytrack_adf_payments_confidence_ann")
db.get_data("SELECT * FROM pc_aiab_analysis.paytrack_adf_payments_confidence_ann LIMIT 10")








        