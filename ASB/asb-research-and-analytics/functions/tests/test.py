import numpy as np
import pandas as pd
import logging
from unittest import TestCase


def test_allocation():
    from functions.allocations.allocation_calc import get_allocations_general
    strategy1 = {
        0:{'selection': 2232, 'side': 'Back'},
        1:{'selection': 322, 'side': 'Back'}
    }
    case1 = {
        'tail0': {'O': 1.3,'S': 158.93},
        'tail1': {'O': 30, 'S': 20.01}
    }
    res1 = [47.85, 2.15]
    strategy2 = {
        0:{'selection': 3, 'side': 'Lay'},
        1:{'selection': 300, 'side': 'Back'},
        2:{'selection': 311, 'side': 'Back'}
    }
    case2 = {
        'tail0': {'O': 3.4,'S': 67.07},
        'tail1': {'O': 12.5, 'S': 30.49},
        'tail2': {'O': 6.8, 'S': 79.42}
    }
    res2 = [15.67, 4.37, 8.03]
    assert res1 = get_allocations_general(strategy1, values=case1, rtrn_dict=False)
    assert res2 = get_allocations_general(strategy2, values=case2, rtrn_dict=False)


if __name__ == "__main__":
    logging.info("starting tests")
    test_allocation()
    logging.info("ran all tests")
    