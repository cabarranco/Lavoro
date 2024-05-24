import argparse
from Utils.object_types import strbool, cast_to


def parse_args(
  arguments: dict,
  description: str = ''
) -> object:
  parser = argparse.ArgumentParser(description=description)
  for arg in arguments:
    parser.add_argument(
      arguments[arg]['name'],
      help = arguments[arg]['help'],
      type = arguments[arg]['type'],
      required = arguments[arg]['required'],
      default = arguments[arg]['default']
    )
  args, unknown = parser.parse_known_args()
  return args


def import_arguments(
  arguments: dict,
  dbutils: object,
  getArgument: object
) -> object:
  dbutils.widgets.removeAll()
  for arg in arguments:
    d = arguments[arg]['default']
    t = (bool if type(d)==bool else arguments[arg]['type'])
    dbutils.widgets.text(arg, str(d))
    a = dbutils.widgets.get(arg)
    a = getArgument(arg, a)
    arguments[arg]['default'] = (
      None if a in ['None', ''] else (str2bool(a) if t==bool else cast_to(t, a))
    )
  job_args = parse_args(arguments)
  return job_args
