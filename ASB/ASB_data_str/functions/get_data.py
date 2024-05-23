import os
import json
import yaml
import datetime as dt
import pandas as pd
import numpy as np
from collections import namedtuple
import requests
import pickle

## TODO fix _id => Id

match_odds_dict = {
    'name':'MATCH_ODDS','selections':[1,2,3],'selections_dict':{1:'home',2:'away',3:'draw'}
}
over_under_dict = {
    'name':'OVER_UNDER','selections':[1,2],'selections_dict':{1:'over',2:'under'}
}
correct_score_dict = {
    'name':'CORRECT_SCORE',
    'selections':[300,301,302,303,310,311,312,313,320,321,322,323,330,331,332,333,340,304,344],
    'selections_dict':{
        300:{'score':{'h':0,'a':0}},301:{'score':{'h':0,'a':1}},302:{'score':{'h':0,'a':2}},303:{'score':{'h':0,'a':3}},
        310:{'score':{'h':1,'a':0}},311:{'score':{'h':1,'a':1}},312:{'score':{'h':1,'a':2}},313:{'score':{'h':1,'a':3}},
        320:{'score':{'h':2,'a':0}},321:{'score':{'h':2,'a':1}},322:{'score':{'h':2,'a':2}},323:{'score':{'h':2,'a':3}},
        330:{'score':{'h':3,'a':0}},331:{'score':{'h':3,'a':1}},332:{'score':{'h':3,'a':2}},333:{'score':{'h':3,'a':3}},
        340:{'score':'Any Other Home Win'},304:{'score':'Any Other Away Win'},344:{'score':'Any Other Draw'}
    }
}
correct_score_selections = [300,301,302,303,310,311,312,313,320,321,322,323,340,304,344]
market_dict = {
    1:match_odds_dict,
    2:over_under_dict,
    3:correct_score_dict,
    4:{'name':'ASIAN_HANDICAP'} # to be added later
}

CONFIG_FILE = 'utils/BFconfig.yaml'
with open(CONFIG_FILE, 'r') as stream:
    config = yaml.safe_load(stream)['betfair_api']

BETFAIR_BETTING_ENDPOINT = config['BETFAIR_BETTING_ENDPOINT'] # betting api-endpoint
BETFAIR_AUTH_ENDPOINT = config['BETFAIR_AUTH_ENDPOINT'] # auth login api-endpoint
BETFAIR_APP_KEY_DEV = config['BETFAIR_APP_KEY_DEV'] # betfair app key. To be replaced with you own app key
data_login = 'username='+config['username']+'&password='+config['password']

price_header = 'eventId,marketType,marketId,selection,selectionId,status,inplay,back_price_1,back_size_1,back_price_2,back_size_2,lay_price_1,lay_size_1,lay_price_2,lay_size_2,timestamp'
SEP = """
"""

# ------------------- LOGIN --------------------
header_login = {
    'Accept': 'application/json',
    'X-Application': BETFAIR_APP_KEY_DEV,
    'content-type': 'application/x-www-form-urlencoded'
}

def get_token():  
    # sending post request and saving response as response object
    r = requests.post(BETFAIR_AUTH_ENDPOINT, data=data_login, headers=header_login)
    login_response = json.loads(r.content, object_hook=lambda d: namedtuple('X', d.keys())(*d.values()))
    token = login_response.token
    if r.status_code == 200:
        return token
    print('ERROR RETRIEVING TOKEN')
    return

# ------------------- BETTING HEADER --------------------
header_betting = {
    'X-Application': BETFAIR_APP_KEY_DEV,
    'content-type': 'application/json',
    'X-Authentication': get_token()
}

def t_2_s(t):
    ts = str(t)
    return ts[:10]+'T'+ts[11:19]+'z'

def s_2_t(s):
    y,m,d = int(s[:4]),int(s[5:7]),int(s[8:10])
    hh = 0 if len(s) < 12 else int(s[11:13])
    mm = 0 if len(s) < 15 else int(s[14:16])
    ss = 0 if len(s) < 18 else int(s[17:19])
    return dt.datetime(y,m,d,hh,mm,ss)

def check_same(df1,df2):
    n = df1.shape[1]
    if (df1.shape[0] != 1) | (df2.shape[0] != 1):
        print('Dataframes have more than 1 row')
        return
    if (n != df2.shape[1])|(df1.shape[0] != df2.shape[0]):
        print('Error: the two dataframes have a different structure')
        return
    return (sum(((df1 == df2) | (df1.isnull() & df2.isnull())).iloc[0]) == n)
    

def get_events(t_from=dt.datetime.now(),t_to=dt.datetime.now()+dt.timedelta(days=1)):
    data_events = {
        'filter': {
            'eventTypeIds': [1],
            'marketStartTime': {
                'from': t_2_s(t_from),
                'to': t_2_s(t_to)
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
        return events_response 
    print('ERROR RETRIEVING EVENTS')
    return

def events_to_df(events_response,d = str(dt.datetime.now())[:13].replace('-','').replace(' ','')):
    # relies on the structure of events_response being
    # event(event=event(['id','name','countryCode','timezone','openDate']), 'marketCount')
    df = pd.DataFrame(events_response)
    values = []
    for i in range(len(events_response)):
        values = values + [(events_response[i][0])]
    events_df = pd.DataFrame(values).join(df[['marketCount']])
    events_df['id'] = events_df['id'].astype(int)
    events_df.to_csv('events/events_'+d+'.csv',index=False)
    return events_df

def get_markets_by_event(event_id,max_res=20):
    # ------------------- GET MARKET --------------------
    data_markets = {
        'filter': {
            'eventIds': [event_id],  # set a list of events' ids from the previous query
            'marketTypeCodes': ["MATCH_ODDS", "CORRECT_SCORE", "OVER_UNDER_25"],
        },
        'maxResults': max_res
    }
    # sending post request and saving response as response object
    r2 = requests.post(
        BETFAIR_BETTING_ENDPOINT + 'listMarketCatalogue/',
        data=json.dumps(data_markets),
        headers=header_betting
    )
    market_response = json.loads(r2.content, object_hook=lambda d: namedtuple('market', d.keys())(*d.values()))
    if r2.status_code == 200:
        return market_response
    else:
        print('ERROR RETRIEVING MARKETS')
        return

def get_market_type_id(x):
    if x == 'Correct Score':
        return 3
    if x[:10] == 'Over/Under':
        return 2
    if x == 'Match Odds':
        return 1
    if x == 'Asian Handicap':
        return 4
    
def add_market_type_id(df):
    if 'marketName' in df.columns:
        df['market_type_id'] = df['marketName'].apply(lambda x: get_market_type_id(x))
        df['market_type_spec'] = np.where(
            df['market_type_id'].isin([1,3]),np.nan,
            np.where(df['market_type_id']==2,
                     df['marketName'].apply(lambda x: (x[11:14])),
                     np.nan # later handicap
                    )
        )
        df['market_type_spec'] = df['market_type_spec'].astype(float)
    return df

def get_markets_df(events_list,d = str(dt.datetime.now())[:13].replace('-','').replace(' ','')):
    markets = pd.DataFrame(columns=['marketId','marketName','totalMatched','event_id'])
    for event_id in events_list:
        df_add = pd.DataFrame(get_markets_by_event(event_id))
        df_add['event_id'] = int(event_id)
        markets = add_market_type_id(markets.append(df_add))
    markets.to_csv('markets/markets_'+d+'.csv',index=False)
    return markets

def get_market_books(markets_list,print_books=False):
# ------------------- GET MARKET BOOKS --------------------
    data_mBook = {
        'marketIds': markets_list,
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
        return mBook_response
    else:
        print('ERROR RETRIEVING MARKETS BOOKS')
        return
#     return selections

def get_and_save_prices(events_df,markets,d = str(dt.datetime.now())[:10].replace('-','')):
    t0 = dt.datetime.now()
    t1 = t0 + dt.timedelta(hours = -1)
    t2 = t0 + dt.timedelta(hours = 2)
    events_list = list(events_df[(events_df.openDate>t_2_s(t1))&(events_df.openDate<t_2_s(t2))].id)
    for e in events_list:
        markets_sel = markets[markets.event_id==e]
        for m in [1,2,3]: # later 4
            prices_df = extract_prices_by_market(m,markets_sel[markets_sel.market_type_id==m])
            filename = 'prices/prices_'+str(d)+'_'+str(e)+'_'+str(m)+'.csv' 
            if not os.path.exists(filename):
                with open(filename, "w") as price_file:
                    price_file.write(price_header+SEP)
            if len(prices_df)>0:
                with open(filename, "a") as price_file:
                    prices_df.to_csv(price_file, header=False, index=False)
                  
    
def extract_prices_by_market(marketType,markets):
    columns = [
        'eventId','marketType','marketId','selection','selectionId','status','inplay',
        'back_price_1','back_size_1','back_price_2','back_size_2',
        'lay_price_1','lay_size_1','lay_price_2','lay_size_2'
    ]
    t = dt.datetime.now()
    prices = get_market_books(list(markets.marketId))
    selections = market_dict[marketType]['selections']
    prices_df = pd.DataFrame(columns=columns+['timestamp'])
    for i in range(len(prices)):
        marketId = prices[i][0]
        eventId = markets[markets.marketId==marketId].iloc[0]['event_id']
        status,inplay = prices[i][2],prices[i][6]
        l = len(prices[i]) - 1
        sn = len(prices[i][l])
        if sn != len(selections):
            print("Some selections are missing for market "+str(m)+" of event "+str(e))
        if status == 'OPEN': 
            for s in range(sn):
                selection = selections[s]
                selectionId = prices[i][l][s][0]
                b = len(prices[i][l][s]) - 1
                back_num = len(prices[i][l][s][b][0])
                lay_num = len(prices[i][l][s][b][1])
                b_price_1 = np.nan if back_num == 0 else prices[i][l][s][b][0][0][0]
                b_size_1 = np.nan if back_num == 0 else prices[i][l][s][b][0][0][1]
                b_price_2 = np.nan if back_num < 2 else prices[i][l][s][b][0][1][0]
                b_size_2 = np.nan if back_num < 2 else prices[i][l][s][b][0][1][1]
                l_price_1 = np.nan if lay_num == 0 else prices[i][l][s][b][1][0][0]
                l_size_1 = np.nan if lay_num == 0 else prices[i][l][s][b][1][0][1]
                l_price_2 = np.nan if lay_num < 2 else prices[i][l][s][b][1][1][0]
                l_size_2 = np.nan if lay_num < 2 else prices[i][l][s][b][1][1][1]

                data = [(eventId,marketType,marketId,selection,selectionId,
                         status,inplay,
                         b_price_1,b_size_1,b_price_2,b_size_2,
                         l_price_1,l_size_1,l_price_2,l_size_2,
                         t)]
                last_pr_name = 'prices/checks/prices_'+str(eventId)+'_'+str(marketType)+'_'+str(selection)+'.csv'
                prices_df_add = pd.DataFrame(data,columns=columns+['timestamp'])
                if not os.path.exists(last_pr_name):
                    with open(last_pr_name, "w") as last_pr_file:
                        prices_df_add.to_csv(last_pr_file, index=False)
                    prices_df = prices_df.append(prices_df_add)
                else:
                    last_price = pd.read_csv(last_pr_name)[columns]
                    if not check_same(prices_df_add[columns],last_price): 
                        prices_df = prices_df.append(prices_df_add)
                        prices_df_add.to_csv(last_pr_name, index=False)
                    
    return prices_df

def get_events_markets():
    events_df = events_to_df(get_events())
    markets = get_markets_df(events_df.id)
    return events_df,markets

def get_time_bounderies(events_df):
    m0, m1 = min(events_df.openDate), max(events_df.openDate)
    min_t = s_2_t(m0) + + dt.timedelta(hours = -1)
    max_t = s_2_t(m1) + + dt.timedelta(hours = 2)
    return min_t, max_t
