import json
import logging
import yaml

logger  = logging.getLogger(__name__)

class Config(object):
    """
    Config class
    """
    def __init__(self, path: str = None, config: str = None):
        """
        Constructor, initialize from yaml config file
        :param path: path to the yaml file
        :param config: config string
        """
        assert path is not None or config != "", "Either path or config must be provided:
        self._config_path = path
        
        if not config:
            self._config = None
            self.read_config()
        else:
            try:
                self._config = json.loads(config)
            except Exception as e:
                logger.error("Config read error")
                logger.exception(e)
                raise e
     
    def read_config(self):
        """
        Read config file and set config
        """
        try:
            with open(self._config_path, "r") as f:
                self._config = yaml.safe_load(f)
        except Exception as e:
            logger.error("Config fiole open error")
            logger.exception(e)
            raise e
     
    def get_config(self):
        """
        Return config dict
        """
        return self._config
    
    def get_value(self, key: str, default: str = None):
        """
        Get config value for a key, otherwise return default value if not defined
        :param key: key for wich value is needed
        :param default: default value
        """
        config_value = Config.get_value_by_dot_notation(key, self._config)
        if config_value is None:
            if default is None:
                logger.warning("WARNING: key %s has no value", key)
            else:
                return default
        return config_value
    
    @staticmethod
    def get_value_by_dot_notation(nested_key: str, config_dict):
        """
        Get value accessed by nested keys that could be separated by dots
        e.g. model.early_stopping.patience
        :param nested_keys: keys
        :param config_dict: a dict of configs
        """
        val = config_dict
        try:
            for key in nested_keys.split("."):
                val = val[key]
            return val
        except Exception as e:
            logger.warning(e)
            return None

              