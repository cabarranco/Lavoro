def coalesce(a: object, b: object) -> object:
  """Returns the first non null value among the arguments"""
  if a==a:
    if a is None:
      return b
    return a
  if b is None:
    return a
  return b


def extract_or_default(dictionary: dict, key: str, default: object) -> object:
  """Extract a value from a dictionary if the key exists,
  otherwise it returns a default value.
  If key is an empty string, it returns the whole dictionary."""
  try:
    if key=="":
      return dictionary
    else:
      return dictionary[key]
  except:
    return default
