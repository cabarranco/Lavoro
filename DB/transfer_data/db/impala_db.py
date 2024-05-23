import argparse
import logging
from datetime import datetime
import pandas as pd
from pandas import DataFrame
from db.jdbc_db import JdbcDb
from utils.config import Config
from utils.decorators import time_it

logger = logging.getLogger(__name__)


class ImpalaDb(JdbcDb):
    """
    Impala DB class
    """
    def __init__(self, args: argparse.Namespace, config: Config) -> None:
        """
        Constructor
        """
        jdbc_config = config.get_value("connection.jdbc.impala")
        eap_config = config.get_value(f"env.{args.env}.impala")
        self.resource_pool = eap_config.get("resource_pool")
        self._args = args
        
        url = f'{jdbc_config["prefix"]}{eap_config["host"]}:{eap_config["port"]}/{jdbc_config["database"]};' \
            f'AuthMech={jdbc_config["auth_mechanism"]};SSL={jdbc_config["ssl"]};UID={args.user};PWD={args.passwprd};'
            
        super(ImpalaDb, self).__init__(jdbc_config, url)
        
    @time_it
    def get_resource_pool(self):
        """
        Get resource pool
        """
        return self._resource_pool
    
    def get_party_id_and_name_from_party_data(self, job_config: dict) -> pd.DataFrame:
        """
        Insert-select ML recommendation pivoted from the view
        """
        self._job_config = job_config
        self._queries = self._job_config["queries"]
        database = self._job_config["database"]
        table = self._job_config["table"]
        partition = self._job_config["partition"]
        query = self.queries["select_party_id_and_name_query"].format(
            database=database, table=table, partition=partition,
            partition_str=self._args.partition_str
        )
        logger.info("query = %s", query)
        return pd.DataFrame(self.query(query), columns=['Party ID', 'Party Name'])
    
    def get models(self, job_config: dict) -> pd.DataFrame:
        """
        Get models from Impala
        """
        self._job_config = job_config
        self._queries = self._job_config["queries"]
        database = self._job_config["database"]
        table = self._job_config["table"]
        query = self.queries["select_model_query"].format(
            database=database, table=table
        )
        logger.info("query = %s", query)
        return pd.DataFrame(self.query(query), columns=['id', 'name', 'version'])
    
    def insert_model(self, job_config: dict, df: pd.DataFrame) -> pd.DataFrame:
        """
        Insert model to Impala
        """
        self._job_config = job_config
        self._queries = self._job_config["queries"]
        database = self._job_config["database"]
        table = self._job_config["table"]
        
        column_list = df.columns.tolist()
        data_columns = columns_list[:]
        placeholders = ', '.join('?' * len(columns_list))
        
        query = self.queries["insert_model_query_template"].format(
            database=database, table=table,
            columns=columns, placeholders=placeholders
        )
        logger.info("query = %s", query)
        data = df[data_columns].values.tolist()
        self.executemany(query, data)
        
    def insert_recommendations_trace(self, job_config:dict, df: pd.DataFrame):
        """
        Insert recommandations to Impala
        """
        self._job_config = job_config
        self._queries = self._job_config["queries"]
        database = self._job_config["database"]
        table = self._job_config["table"]
        partition = self._job_config["partition"]
        
        column_list = df.columns.tolist()
        data_columns = columns_list[:]
        placeholders = ', '.join('?' * len(columns_list))
        
        query = self.queries["insert_recommendations_trace_query_template"].format(
            database=database, table=table,
            columns=columns, placeholders=placeholders
        )
        logger.info("query = %s", query)
        data = df[data_columns].values.tolist()
        self.executemany(query, data)
        
        
        
        