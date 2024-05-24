import json
import logging
import yaml

logger = logging.getLogger(__name__)

class Config(object):
  def __init__(self, parth: str = None, config: str = None):
    assert path is not None or config != "", "Either path or config must be provided"
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
    try:
      with open(self._config_path, "r") as f:
        self._config = yaml.safe_load(f)
    except Exception as e:
      logger.error("Config file open error")
      logger.exception(e)
      raise e

  def get_config(self):
    return self._config

  def get_value(self, key: str, default: str = None):
    config_value = Config.get_value_by_dot_notation(key, self._config)
    if config_value is None:
      if default is None:
        logger.warning("WARNING: key %s has no value", key)
      else:
        return default
    return config_value

  @staticmethod
  def get_value_by_dot_notation(nested_keys: str, config_dict: dict):
    val = config_dict
    try:
      for key in nested_keys.split("."):
        val = val[key]
      return val
    except Exception as e:
      logger.warning(e)
      return None
