import datetime as dt
import pandas as pd
import numpy as np
import os

from utils.db_connections import MOCK_DB_PATH, MOCK_DB_FOR, execute_query_mysql, query_to_df_mysql
from utils.db_connections import get_mongodb_client, query_to_list_mongodb, query_to_df_mongodb
from utils.frequent_functions import list_sub_folders

DEFAULT_MOCK = True

def show_databases_mysql(mock=DEFAULT_MOCK):
    if mock:
        return pd.DataFrame(list_sub_folders(MOCK_DB_PATH),columns=['Database'])
    return query_to_df_mysql("""show databases """)

def show_tables_mysql(schema,mock=DEFAULT_MOCK):
    if mock:
        return pd.DataFrame(list_files_in_folder(os.path.join(MOCK_DB_PATH,schema)),columns=['Tables_in_'+schema])
    return query_to_df_mysql("""show tables in """+schema)

def describe_table_mysql(table,schema,mock=DEFAULT_MOCK):
    if mock:
        file = os.path.join(MOCK_DN_PATH,schema,table+'_description.txt')
        return pd.read(file)
    return query_to_df_mysql("describe "+schema+"."+table)

def check_table_existence_mysql(table,schema,mock=DEFAULT_MOCK):
    tables = show_tables_mysql(schema,mock)
    return table in set(tables['Tables_in_'+schema])

def check_schema_existence_mysql(schema,mock=DEFAULT_MOCK):
    databases = show_databases_mysql()
    return schema in set(databases['Database'])

def list_server_content_mysql(mock=DEFAULT_MOCK):
    df = pd.DataFrame(columns=['Database','Table','Field','Type'])
    databases = show_databases_mysql(mock)
    for i in range(len(databases)):
        db = databases.iloc[i]['Database']
        tables = tables = show_tables_mysql(db,mock)
        for j in range(len(tables)):
            table = tables.iloc[j]['Tables_in_'+db]
            columns = describe_table_mysql(table,db,mock)
            columns['Database'] = db
            columns['Table'] = table
            df = df.append(columns[['Database','Table','Field','Type']])
    return df
    
def guess_column_type_mysql(dtype):
    dictionary = {'int':'int','boo':'bit','flo':'double','dat':'datetime'}
    if dtype[:3] in ['int','flo','boo','dat']:
        return dictionary[dtype[:3]]
    return 'varchar'

def add_column_command_mysql(col,dtype,df):
    dim = ""
    if dtype == 'int':
        dim = """("""+str(int(np.ceil(np.log10(max(df[col].abs())+1)))+1)+""")"""
    if dtype == 'varchar':
        dim = """("""+str(int(np.ceil(max([len(str(x)) for x in df[col]])))+1)+""")"""
    col_str = col+""" """+dtype+dim+""",
    """
    return col_str  

def create_table_from_df_mysql(df,table,schema,primary_id=None):
    if len(df) == 0 :
        print('DataFrame is empty')
        return
    l = int(np.ceil(np.log10(len(df))))+1
    query = """create table """+schema+"""."""+table+""" (
    """
    if not primary_id:
        primary_id = 'id'
        query = query + """id int("""+str(max(l,4))+"""),
        """
    for col in df.columns :
        dtype = guess_column_type_mysql(str(df.dtypes[col]))
        query_add = add_column_command_mysql(col,dtype,df)
        query = query + query_add
    query = query + """primary key ("""+primary_key+""")
    ) ; """
    execute_query_mysql(query)

def df_to_db_mysql(df,table,schema,primary_id=None,create=False,notifications=True):
    if not check_schema_existence_mysql(schema): 
        print("DB "+schema+" is not defined on this server")
        return
    if not check_table_existence_mysql(table,schema): 
        if not create: 
            print("Table "+table+" doesn't exists in schema "+schema)
            return
        create_table_from_df_mysql(df,table,schema,primary_id)
    df_to_table(df,table,schema,notifications)
    
def df_to_table(df,table,schema,notifications=True):
    # This function requires column names to avoid special characters
    # if df in input includes rows already in destination table, those will be ignored
    m = 5000
    l = len(df) 
    if l > m : 
        n = 1 + l//m
        for i in range(n):
            if i*m < l:
                dfi = df.iloc[i*m:min(l,(i+1)*m)]
                df_to_table(dfi,table,schema,notifications)
    else: 
        def clean_str(x):
            return str(x).replace("'","''")
        df_types = df.dtypes
        table_des = describe_table(table,schema,svr)
        table_columns = table_des['Field']
        query = """insert ignore into """+schema+"""."""+table+""" select * from (
        """
        for i in range(l):
            row = df.iloc[i]
            query_add = """select """
            line_union = """ union
            """
            line_end = line_union if i < l - 1 else " "
            for col in table_columns: 
                col_type = table_des[table_des.Field==col].iloc[0]['Type']
                if col in df.columns: 
                    val = row[col]
                    if col_type[:3] in ['int','dou','bit']: 
                        coll_add = str(val) if (val != None and val == val) else "NULL"
                        query_add = query_add + coll_add+""" `"""+col+"""`, """
                    else: 
                        if type(val) == str:
                            val = val.replace('%',' ')
                        coll_add = "'"+clean_str(val)+"'" if (val != None and val == val) else "NULL"
                        query_add = query_add + coll_add+""" `"""+col+"""`, """
                else:
                    if col == 'id':
                        max_id = query_to_df("select ifnull(max(id),0) id from "+schema+"."+table).iloc[0]['id']
                        query_add = query_add +str(max_id+i)+""" `"""+col+"""`, """
                    else:
                        query_add = query_add + """NULL `"""+col+"""`, """
                        if notifications:
                            print("Column "+col+" not included in the data-frame")
            query = query + query_add[:-2] + line_end
        query = query + """ 
        ) t 
        ; """
        execute_query(query)

def update_db_mysql(df,table,tab_id,schema):
    table_des = describe_table(table,schema,svr)
    if type(tab_id) == str:
        val_col = [col for col in df.columns if col != tab_id]
    if type(tab_id) == list:
        val_col = [col for col in df.columns if col not in tab_id]
    for i in range(len(df)):
        row = df.iloc[i]
        query = """update table """+schema+"""."""+table+"""
        set """
        for col in val_col:
            col_type = table_des[table_des.Field==col].iloc[0]['Type']
            col_val = row[col]
            if col_val == col_val:
                if col_type[:3] in ['int','dou','bit']:
                    col_val = str(col_val)
                else:
                    col_val = "'"+str(col_val)+"'"
            else:
                col_val = "null"
            query = query + col + " = " + col_val + ", " 
        query = query[:-2]
        query = query + " where "
        if type(tab_id) == str:
            query = query + tab_id +" = " + row[tab_id]
        if type(tab_id) == list:
            for id_col in tab_id:
                query = query + id_col +" = " + row[id_col] + " and "
            query = query[:-4]
        execute_query_mysql(query)   
    
def record_log_mysql(table,process,message):
    if not check_schema_existence_mysql('logs'):
        print("Schema 'logs' is not defined on this server")
        return
    if not check_table_existence_mysql(table,'logs'):
        print("Table '"+table+"' is not defined on schema 'logs'")
        return
    query = """insert into `logs`."""+table+""" (`process`, message, created)
    select '"""+process+"""' `process`, '"""+message+"""' message, now() created
    """
    execute_query_mysql(query)
        
    