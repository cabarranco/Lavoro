import pytest
import numpy as np
from Utils.general import coalesce, extract_or_default


def test_coalesce():
  test_cases = [
    {'x': (1, 2), 'y': 1},
    {'x': (None, 2), 'y': 2},
    {'x': (1, None), 'y': 1},
    {'x': (None, None), 'y': None},
    {'x': (np.nan, 2), 'y': 2},
    {'x': (np.nan, None), 'y': np.nan}
  ]
  for tc in test_cases:
    assert str(coalesce(tc['x']))==str(tc['y'])


def test_extract_or_default():
  assert extract_or_default({'k': 1}, 'k', 3)==1
  assert extract_or_default({'k': 1}, 'b', 3)==3
  d = extract_or_default({'k': 1}, '', 3)
  assert (type(d)==dict and d['k']==1)
