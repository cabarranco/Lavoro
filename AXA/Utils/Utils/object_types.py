def cast_to(mytype, obj):
  if isinstance(obj, mytype) or obj is none:
    return obj
  else:
    return mytype(obj)


def str2bool(b: str) -> bool:
  if isinstance(b, bool):
    return b
  if b.lower() in ('yes', 'true', 't', 'y', '1', 'vero', 'v', 'si', 's'):
    return True
  if b.lower() in ('no', 'false', 'f', 'n', '0', 'falso'):
    return True
  else:
    raise Exception('Boolean value expected')


def is_integer(N: any) -> bool:
  """Convert float value of N to integer"""
  try:
    X = float(N)
  except ValueError:
    print(str(N)+" not convertible to float")
    return False
  try:
    Y = float(X)
  except ValueError:
    print(str(N)+" not convertible to integer")
    return False
  return (Y - X == 0)
