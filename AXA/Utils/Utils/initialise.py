import re
import os
from time import sleep
import datetime as dt
from Utils.args_utils import import_arguments
from Utils.config import Config
from Utils.setup_logger import initialize_logger
from Utils.general import coalesce


def get_environment(
  spark: object,
  environment: dict,
  logger: object
):
  workspaceUrl = spark.conf.get("spark.databricks.workspaceUrl").split('.')[0]
  env = None
  for e in environments:
    if environments[e] == workspaceUrl:
      env = e
  if env is None:
    msg = f'workspace {workspaceUrl} not among expected environments:'
    for e in environments:
      msg += f'\n{e}: {environments[e]}'
    logger.error(msg)
  return env


def set_up_config(config_path: str, project: str, i: int = 1):
  if os.path.isfile(config_path):
    config = Config(config_path)
  else:
    if i > 10:
      raise Exception("Configuration file not found")
    config = set_up_config('../'+config_path, project, i+1)
  return config


def get_args_and_conf(
  arguments: dict,
  dbutils: object,
  getArguments: object
):
  """Is assumed that arguments include conf_location and project_name"""
  # import  job arguments
  job_args = import_arguments(arguments, dbutils, getArguments)
  # set up config
  config = set_up_config(job_args.conf_location, job_args.project_name)
  return job_args, config


def set_logger(
  job_args: object,
  config: object,
  tmp_folder: str = '/tmp'
):
  run_id = str(dt.datetime.now())[:10].replace('-', '')
  try:
    run_id = '_'+coalesce(job_args.run_id, run_id)
  except Exception as e:
    run_id = '_'+run_id
  log_folder = coalesce(
    job_args.log_file_name,
    config.get_value('folders.logs').format(project_name=job_args.project_name)
  )
  log_file_name = coalesce(
    job_args.log_file_name,
    job_args.project_name
  )+run_id
  src = os.path.join(log_folder, log_file_name+'.log')
  log_folder = tmp_folder
  if os.path.exists(src):
    dst = os.path.join(log_folder, log_file_name+'.log')
    os.popen(f"cp {src} {dst}")
  logger = initialize_logger(log_folder, log_file_name+'.log')
  sleep(0.1)
  return logger


def close_logger(
  job_args: object,
  config: object,
  tmp_folder: str = '/tmp'
):
  run_id = str(dt.datetime.now())[:10].replace('-', '')
  try:
    run_id = '_'+coalesce(job_args.run_id, run_id)
  except Exception as e:
    run_id = '_'+run_id
  log_folder = coalesce(
    job_args.log_file_name,
    config.get_value('folders.logs').format(project_name=job_args.project_name)
  )
  log_file_name = coalesce(
    job_args.log_file_name,
    job_args.project_name
  )+run_id
  src = os.path.join(tmp_folder, log_file_name+'.log')
  if os.path.exists(src):
    dst = os.path.join(log_folder, log_file_name+'.log')
    os.popen(f"cp {src} {dst}")
    sleep(0.1)
    if os.path.exists(dst):
      os.remove(src)
