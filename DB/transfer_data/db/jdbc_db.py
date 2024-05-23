import logging
from typing import Dict, List
import jaydebeapi
from utils.decorators import time_it

logger = logging.getLogger(__name__)

class JdbcDb(object):
    """
    Generic JDBC class for connecting to a DB by Jaydebe API
    """
    def __init__(self, jdbc_config: Dict, url: str = None):
        """
        Constructor
        :param jdbc_config: JDBC config
        :param url: connection url
        """
        self._config = jdbc_config
        self._url = url
        logger.info("Connecting to DB...")
        self._connection = self.connect()
        logger.info("...connected")
        self._cursor = self._connection.cursor()
        
    def __enter__(self):
        """
        This allows us to use the with clause
        """
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """
        This will automatically close the connection once it exits from with clause
        :param exc_type: exception type
        :param exc_val: exception value
        :param exc_tb: exception traceback
        """
        logger.info("Closing DB connection")
        self.close()
        logger.info("...closed")
    
    @time_it
    def connect(self):
        """
        Connect to the DB
        :return: the connection object
        """
        return jaydebeapi.connect(self._config.get("driver"), self._url)
    
    @property
    def connection(self):
        """
        Connection property
        """
        return self._connection
    
    @property
    def cursor(self):
        """
        Cursor property
        """
        return self._cursor
    
    @time_it
    def close(self):
        """
        Close the connection
        """
        self.connection.close()
    
    @time_it
    def execute(self, sql: str):
        """
        Execute SQL query using the cursor
        :param sql: SQL query
        """
        self.cursor.execute(sql)
    
    @time_it
    def executemany(self, sql: str, data: List):
        """
        Execute SQL query using the cursor
        :param sql: sql query
        :param data: list of values
        """
        self.cursor.executemany(sql, data)
    
    @time_it
    def fetch_all(self):
        """
        Fetch multiple result rows
        """
        return self.cursor.fetchall()
    
    @time_it
    def fetchone(self):
        """
        Fetch a single row
        """
        return self.cursor.fetchone()
    
    @time_it
    def query(self, sql):
        """
        Execute the query and return results
        :param sql: SQL query
        """
        self.cursor,execute(sql)
        return self.fetchall()
    
         
                     