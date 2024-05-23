import pandas as pd

DB = {'qa': 'asbresearch-qa', 'prod': 'asbresearch-prod'}

INTERVALS = [1, 2, 3, 4, 5, 6]
LIQUIDITY_GROUPS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'L']
LIQUIDITY_GROUPS_DICT = {
    10: 'A', 9: 'B', 8: 'C', 7: 'D', 6: 'E',
    5: 'F', 4: 'G', 3: 'H', 2: 'I', 1: 'L'
}

DEFAULT_PARAM = {
    'T': 1000, # total allocation - calculated_every_day
    'f': 0.02, # fees
    'f1': 1 - 0.02, 'f2': 1/(1 - 0.02), 'f3': 0.02/(1 - 0.02),
    'min_all':2 # minimal allocation
}

DEFAULT_INPUT = {
    'selections': {
        0:{'selection': 3, 'side': 'Lay'},
        1:{'selection': 300, 'side': 'Back'},
        2:{'selection': 311, 'side': 'Back'}
    },
    'score': '0-0',
    'lookback': {'start': '2020-01-01', 'end': '2020-12-01'},
    'min_size': 2,
    'SIP_interval': (0, 1),
    'max_allocation': 50,
    'tamip': 0
}

DEFAULT_SIP_LEVELS = pd.DataFrame([
    (0.01, 0.63),
    (0.62, 0.68),
    (0.68, 0.72),
    (0.72, 0.75),
    (0.75, 0.78),
    (0.78, 0.8),
    (0.8, 0.83),
    (0.83, 0.85),
    (0.85, 0.89),
    (0.89, 1)
], columns = ['SIP0', 'SIP1'])

EVENTS_EXAMPLE = [30063193, 29929983, 29884782]

STATS_ARGUMENTS = ['delta', 'mu', 'sigma']

STATS_COLUMNS = [
    'backPrice',
    'layPrice',
    'backSize',
    'laySize',
    'spreadPrice',
    'deltaBackPrice',
    'deltaLayPrice',
    'deltaBackSize',
    'deltaLaySize',
    'deltaSpreadPrice',
    'muBackPrice',
    'muLayPrice',
    'muBackSize',
    'muLaySize',
    'muSpreadPrice',
    'sigmaBackPrice',
    'sigmaLayPrice',
    'sigmaBackSize',
    'sigmaLaySize',
    'sigmaSpreadPrice'
]
