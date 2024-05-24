import logging
import sys
import os


def initialize_logger(log_folder: str = None, log_file: str = None):
  logger = logging,getLogger(__name__)
  logger.setLevel(logging.INFO)
  if log_folder is not None:
    if not os.path.exists(log_folder):
      os.makedirs(log_folder)
  
  formatter = loggingFormatter("[%(asctime)s] p%(process)s {%(filename)s:%(lineno)d} %(levelname)s - %(message)s")
  stdout_handler = logging.StreamHandler(sys.stdout)
  stdout_handler.setFormatter(formatter)
  logger.addHandler(stdout_handler)
  
  if log_file is not None:
    log_file_path = os.path.join(log_folder, log_file)
    output_file_handler = logging.FileHandler(log_file_path)
    output_file_handler.setFormatter(formatter)
    logger.addHandler(output_file_handler)

return logger
