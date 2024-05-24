import pytest
import numpy as np
from Utils.object_types import cast_to, str2bool, is_integer


def test_cast_to():
  assert cast_to('1', int)==1
  assert cast_to(1, str)=='1'


def test_str2bool():
  for v in [
    'yes', 'YES', 'Yes', 'Y', 'y',
    'TRUE', 'True', 'true', '1'
  ]:
    assert str2bool(v)==True
  for f in [
    'false', 'False', 'False', '0',
    'NO', 'No', 'no', 'n', 'N'
  ]:
    assert str2bool(f)==False
