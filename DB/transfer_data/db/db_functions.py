"""
Functions to 
* extract
* process/transform
* load
data from/to database.

Most of the following use a dictionary of connection_details as input
coneection_details_example = {
    'env': 'uat',
    'username': 'svc_eap_uat_aia',
    'password': 'blabla'
}
"""
import pandas as pd
import numpy as np
import datetime as dt
import os
import sys
import argparse
import utils.config import Config
import db.impala_db import ImpalaDb

RETRY = 5
ROWS = 10000

CONFIGH_PATH = 'config/conf.yaml'

def get_connection(connection_details: dict):
    config = Config(CONFIGH_PATH)
    impala_conn = ImpalaDb(argparse.Namespace(
        env=connection_details['env'],
        username=connection_details['username'],
        password=connection_details['password']
    ), config)
    return impala_conn


def execute_sql_script(sql_script: str, connection_details: dict, N=0):
    if N > RETRY:
        return
    impala_conn = get_connection(connection_details)
    try:
        impala_conn.query(sql_script)
    except Exception as e:
        if len(str(e)) > 1:
            print(str(e))
        execute_sql_script(sql_script, connection_details, N+1)
#     except java.sql.SQLException:
#         execute_sql_script(sql_script, connection_details, N+1)
#         # TODO : if java library is available, we can retry scripts that failed for connection issues
    return


def query_to_df(query: str, columns: list, connection_details: dict) -> pd.DataFrame:
    impala_conn = get_connection(connection_details)
    res = pd.DataFrame(impala_conn.query(query), columns=columns)
    return res


def create table(
    table_name: str, connection_details: dict, columns: list,
    partitioned_by: list = None, execute False, as_parquet = True
):
    partitioned_by_str = ''
    if partitioned_by != None:
        columns = [col for col in columns if col not in partitioned_by]
        partitioned_by_str = "PARTITIONED BY ("
        for col in partitioned_by:
            partitioned_by_str += col+" string, "
        partitioned_by_str = partitioned_by_str[:-2]+")"
    sep = """,
    """
    sql_script = "CREATE TABLE IF NOT EXISTS "+tablename+"""
    (
    """
    for col in columns:
        sql_script += (col+" string")
        if col != columns[-1]:
            sql_script += sep
    sql_script += """
    )
    """
    sql_script += partitioned_by_str
    if as_parquet:
        sql_script += " STORED AS PARQUET"
    
    if execute:
        execute_sql_script(sql_script, connection_details)
    else:
        print(sql_script)
    return


def create_partition(
    table_name: str, connection_details: dict, partitioned_by: list, 
    partition: pd.core.series.Series, execute False
):
    # TODO: Add datatypes
    sql_script = "alter table "+table_name+" add if not exists partition ("
    for col in partitioned_by:
        sql_script += col+"='"+str(partition[col])+"', "
    sql_script = sql_script[:-1]+")"
    if execute:
        try:
            execute_sql_script(sql_script, connection_details)
        except:
            execute_sql_script(sql_script, connection_details)
    else:
        print(sql_script)
    return


def check_partition(table_name: str, connection_details: dict,
                    partitioned_by: list, partition: pd.core.series.Series):
    # TODO: Add datatypes
    sql_script = "SHOW PARTITIONS "+table_name
    columns = partitioned_by+['#Rows', '#Files', 'Size', 'Bytes Cached', 'Cache Replication',
                              'Format', 'Incremental stats', 'Location']
    partitions = query_to_df(sql_script, columns, connection_details)
    for col in partitioned_by:
        partitions = partitions[partitions[col]==partition[col]]
    return (len(partitions) > 0)


def populate_table(df: pd.core.frame.DataFrame, table_name: str,
                    connection_details: dict,
                    partitioned_by: list = None,
                    execute: bool = False,
                    data_types: dict = None,
                    N: int = 0):
    # select partition
    if partitioned_by is not None:
        partitions = df[partitioned_by].drop_duplicates().reset_index()
        if len(partitions) > 1:
            for i in range(len(partitions)):
                row - partitions.iloc[i]
                dfi = df.copy()
                for col in partitioned_by:
                    dfi = dfi[dfi[col]==row[col]]
                populate_table(dfi, table_name, connection_details, partitioned_by, execute, data_types)
            return 
    # iterate if too long
    M = ROWS
    if len(df) > M:
        i = 0
        while i*M < len(df):
            i0, i1 = i*M, min(len(df), (i+1)*M)
            dfi = df.iloc[i0: i1]
            populate_table(dfi, table_name, connection_details, partitioned_by, execute, data_types)
            i += 1
        return
    
    sep = """,
    """
    add_partition_str = ''
    columns = list(df.columns)
    # TODO: columns should be taken from table description
    
    # add partition
    if partitioned_by is not None:
        columns = [col for col in columns if col not in partitioned_by]
        if not check_partition(table_name, connection_details, partitioned_by, partitions.iloc[0]):
            create_partition(table_name, connection_details, partitioned_by, partitions.iloc[0], execute)
        add_partition_str = "partition ("
        for col in partitioned_by:
            add_partition_str += col+"='"+str(partitions.iloc[0][col])+"', "
        add_partition_str = add_partition_str[:-2]+")"
    
    # insert script
    sql_script = "INSERT INTO "+table_name+" ("+sep.join(columns)+""")
    """+add_partition_str+"""
    values
    """
    for i in range(len(df)):
        row = df.iloc[i]
        row_script = "("
        for col in columns:
            col_val = row[col]
            if col_val==col_val:
                str_val = str(col_val)
                if str_val[-2:] == '.0':
                    str_val = str_val[:-2]
                check_types = (int, np.int32, np.int64, float, np.float32, np.float64)
                if data_tpes is None or not isinstance(col_val, check_types):
                    str_val = "'"str_val.replace("'", "\\'")+"'"
                elif col in data_types:
                    str_val = "CAST("+str_val+" AS "+data_types[col]+")"
                row_script += str_val+", "
            else:
                row_script += "NULL, "
        row_script = row_script[:-2]+")"
        if i < len(df) -1:
            row_script += sep
        sql_script += row_script
    
    if execute:
        try:
            execute_sql_script(sql_script, connection_details)
        except Exception as e:
            if len(str(e)) > 1:
                print(str(e))
            if N > RETRY:
                return
            populate_table(df, table_name, connection_details, partitioned_by, execute, data_types, N+1)
    else:
        print(sql_script)
    return

            
    
        