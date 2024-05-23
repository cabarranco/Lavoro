import yaml
import pandas as pd
import sqlalchemy
from sqlalchemy import MetaData
import pymongo
import bson
from bson import ObjectId
from pandas.io.json import json_normalize

DB_CONFIG_FILE = 'utils/db_config.yaml'
MOCK_DB_PATH = 'mock_mysqldb'
MOCK_DB_FOR = '.csv'

with open(DB_CONFIG_FILE, 'r') as stream:
    config = yaml.safe_load(stream)
    conf_sql = config['data_analytics']
    conf_mdb = config['operations_slave']

host_sql,usern_sql,pw_sql,db_sql = conf_sql['host'],conf_sql['username'],conf_sql['password'],conf_sql['database']
host_mdb,usern_mdb,pw_mdb,db_mdb = conf_mdb['host'],conf_mdb['username'],conf_mdb['password'],conf_mdb['database']

connect_string_mysql = 'mysql+pymysql://'+usern_sql+':'+pw_sql+'@'+host_sql+'/'+db_sql
connect_string_mongodb = 'mongodb+srv://'+usern_mdb+':'+pw_mdb+'@'+host_mdb

def execute_query_mysql(query):
    metadata = MetaData()
    engine_mysql = sqlalchemy.create_engine(connect_string_mysql)
    connex = engine_mysql.connect()
    metadata.reflect(bind=engine)
    connex.execute(query)
    connex.close()

def query_to_df_mysql(query):
    metadata = MetaData()
    engine_mysql = sqlalchemy.create_engine(connect_string_mysql)
    connex = engine_mysql.connect()
    metadata.reflect(bind=engine)
    result_cursor = connex.execute(query)
    connex.close()
    return pd.DataFrame(result_cursor.fetchall(), columns=result_cursor.keys())

def get_mongodb_client():
    myclient = pymongo.MongoClient(connect_string_mongodb)
    return myclient

def query_to_list_mongodb(query={'db':'test','col':'test','query':None}):
    myclient = get_mongodb_client()
    res = list(myclient[query['db']][query['col']].find(query['query']))
    return res
    
def query_to_df_mongodb(query={'db':'test','col':'test','query':None}):
    res = query_to_list_mongodb(query)
    df = pd.DataFrame(json_normalize(res))
    return df

    
