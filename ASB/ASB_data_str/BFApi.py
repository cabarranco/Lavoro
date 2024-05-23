# Betfair API example.
# You need to insert your own account information and generate an app key
# follow the instructions in the link below to generate an Application Key
# https://docs.developer.betfair.com/display/1smk3cen4v3lu3yomq5qye0ni/Application+Keys
#
# For any other doubts i suggest you to consult this link:
# https://docs.developer.betfair.com/display/1smk3cen4v3lu3yomq5qye0ni/Getting+Started
#
# All the filters used in the APIs' calls are examples used in the current strategies. Feel free to change it if you
# need for your situation

import json
from collections import namedtuple
import requests

# betting api-endpoint
BETFAIR_BETTING_ENDPOINT = "https://api.betfair.com/exchange/betting/rest/v1.0/"

# auth login api-endpoint
BETFAIR_AUTH_ENDPOINT = "https://identitysso.betfair.com/api/login"

# betfair app key. Replace with you own app key
BETFAIR_APP_KEY_DEV = "Wowip95D4KxDk2tr"

# ------------------- LOGIN --------------------

header_login = {
    'Accept': 'application/json',
    'X-Application': BETFAIR_APP_KEY_DEV,
    'content-type': 'application/x-www-form-urlencoded'
}

data_login = 'username=Mikelevirgo2&password=cheqai87'

# sending post request and saving response as response object
r = requests.post(BETFAIR_AUTH_ENDPOINT, data=data_login, headers=header_login)

login_response = json.loads(r.content, object_hook=lambda d: namedtuple('X', d.keys())(*d.values()))

token = login_response.token

if r.status_code == 200:
    print('TOKEN: ' + token)
else:
    print('ERROR RETRIEVING TOKEN')
    exit(1)

# ------------------- BETTING HEADER --------------------

header_betting = {
    'X-Application': BETFAIR_APP_KEY_DEV,
    'content-type': 'application/json',
    'X-Authentication': token
}

# ------------------- GET EVENTS --------------------

data_events = {
    'filter': {
        'eventTypeIds': [1],
        'marketStartTime': {
            'from': '2019-08-11T11:22:57Z',  # set a range of date of 2 days using this date format
            'to': '2019-08-31T12:52:57Z'
        },
        'marketCountries': ["IT", "GB", "ES", "DE", "FR", "PT"],
        'marketTypeCodes': ["MATCH_ODDS", "CORRECT_SCORE", "OVER_UNDER_25"],
    }
}

# sending post request and saving response as response object
r1 = requests.post(
    BETFAIR_BETTING_ENDPOINT + 'listEvents/',
    data=json.dumps(data_events),
    headers=header_betting
)

events_response = json.loads(r1.content, object_hook=lambda d: namedtuple('event', d.keys())(*d.values()))

if r1.status_code == 200:
    print(' *********************************************** EVENTS *************************************************\n')
    print(events_response)
else:
    print('ERROR RETRIEVING EVENTS')
    exit(1)

# ------------------- GET MARKET --------------------

data_markets = {
    'filter': {
        'eventIds': [29353536],  # set a list of events' ids from the previous query
        'marketTypeCodes': ["MATCH_ODDS", "CORRECT_SCORE", "OVER_UNDER_25"],
    },
    'maxResults': 2
}

# sending post request and saving response as response object
r2 = requests.post(
    BETFAIR_BETTING_ENDPOINT + 'listMarketCatalogue/',
    data=json.dumps(data_markets),
    headers=header_betting
)

market_response = json.loads(r2.content, object_hook=lambda d: namedtuple('market', d.keys())(*d.values()))

if r2.status_code == 200:
    print(' ************************************************ MARKETS ***********************************************\n')
    print(market_response)
else:
    print('ERROR RETRIEVING MARKETS')
    exit(1)

# ------------------- GET MARKET BOOKS --------------------
# In the market book you can find ex object which contains the back and lay size and price of the bet

data_mBook = {
    'marketIds': ['1.157024679'],
    'orderProjection': 'EXECUTABLE',
    'matchProjection': 'ROLLED_UP_BY_PRICE',
    'priceProjection': {
        'priceData': ['EX_BEST_OFFERS']
    }
}

# sending post request and saving response as response object
r3 = requests.post(
    BETFAIR_BETTING_ENDPOINT + 'listMarketBook/',
    data=json.dumps(data_mBook),
    headers=header_betting
)

mBook_response = json.loads(r3.content, object_hook=lambda d: namedtuple('marketBook', d.keys())(*d.values()))

if r3.status_code == 200:
    print(' ********************************************* MARKETS BOOKS ********************************************\n')
    print(mBook_response)
else:
    print('ERROR RETRIEVING MARKETS BOOKS')
    exit(1)
