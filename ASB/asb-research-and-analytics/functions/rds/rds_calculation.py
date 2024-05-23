import os
import math as mt
import numpy as np
import pandas as pd
import datetime as dt
import statistics as st
from functions.db.db_functions import query_to_df
from functions.utils.utils import (
    format_prices_for_rds, format_prices_for_oros,
    select_first_timestamp_in_interval,
    add_inv_prob
)
from functions.utils.variables import DB, STATS_COLUMNS, DEFAULT_PARAM, DEFAULT_INPUT
from functions.allocations.allocation_calc import get_allocations_general
import functions.utils.queries as q
from functions.oros_calculations.oros_calculation import SIP_binner

INITIAL_COLUMNS = [
    'year',
    'month',
    'competition',
    'competitionGroup',
    'interval',
    'eventId',
    'timestamp',
    'id',
    'secondsInPlay',
    'minsToEnd',
    'cumYCardsH',
    'cumRCardsH',
    'cumYCardsA',
    'cumRCardsA',
    'cumGoalsH',
    'cumGoalsA',
    'score',
    'previousScore',
    'volumeMO',
    'volumeCS',
    'volumeOU05',
    'volumeOU15',
    'volumeOU25',
    'volumeOU35',
    'volumeAH',
    'result',
    'outcome',
    'SIP'
]
STRATEGY_COLUMNS = ['price{n}', 'size{n}', 'alloc{n}']
FINAL_COLUMNS = ['stake', 'potential_return' ,'PL', 'tamip']
PRICE_COLUMNS = ['asbSelectionId', 'backPrice', 'layPrice', 'backSize', 'laySize']
STATS_COLUMNS_POST = ['{sel}_'+feature for feature in STATS_COLUMNS]
TIMES = [
    '2020-01-01', '2020-02-01', '2020-03-01', '2020-04-01', '2020-05-01', '2020-06-01',
    '2020-07-01', '2020-08-01', '2020-09-01', '2020-10-01', '2020-11-01', '2020-12-01',
    '2021-01-01', '2021-02-01', '2021-03-01', '2021-04-01', '2021-05-01', '2021-06-01',
    '2021-07-01', '2021-08-01', '2021-09-01', '2021-10-01', '2021-11-01', '2021-12-01'
]


def get_features(df):
    columns = [col for col in df.columns if col not in PRICE_COLUMNS]
    return df[columns].drop_duplicates()


def add_allocations(df, selections, T=DEFAULT_INPUT['max_allocation']):
    param = DEFAULT_PARAM
    param['T'] = T
    allocations = {}
    side_main = selections[0]['side']
    for sel in selections:
        allocations[sel] = []
    for i in range(len(df)):
#         if i % 10000 == 0:
#             print(i)
        row = df.iloc[i]
        pr_values = {}
        for sel in selections:
            s = str(sel)
            pr_values['tail'+s] = {
                'O': row['price'+s], 'S': row['size'+s], 'P': 1/row['price'+s]
            }
#         print(pr_values)
        allocations_dict = get_allocations_general(selections, param, pr_values, True, False)
        for sel in selections:
            s = str(sel)
            all_i = 0 if allocations_dict is None else allocations_dict['tail'+s]
            allocations[sel] += [all_i]
    for sel in selections:
        s = str(sel)
        df['alloc'+s] = allocations[sel]
    
    df['stake'] = df['alloc0']
    if side_main == 'Lay':
        df['stake'] = df['alloc0']*(df['price0']-1)
    for sel in selections:
        if sel > 0:
            df['stake'] = df['stake'] + df['alloc'+str(sel)]
    
    f = param['f']
    if side_main == 'Lay':
        df['potential_return'] = df['alloc0']*(1-f)
    if side_main == 'Back':
        df['potential_return'] = df['alloc0']*(df['price0'] - 1)*(1-f)
    for sel in selections:
        if sel > 0:
            df['potential_return'] = df['potential_return'] - df['alloc'+str(sel)]
    
    if 'outcome' in df.columns:
        df['PL'] = np.where(df['outcome']==1, df['potential_return'], -df['stake'])
    
    return df


def check_tamip(df, tamip):
    df['tamip'] = np.where(df['stake'] > 0, df['stake'], np.nan)
    df['tamip'] = np.where(
        df['stake'] > 0,
        np.where(df['potential_return']/df['tamip'] > tamip, 1, 0),
        np.nan
    )
    return df


def get_time_intervals(lookback):
    t0, t3 = lookback['start'], lookback['end']
    t1 = min([t for t in TIMES if t > t0])
    t2 = max([t for t in TIMES if t < t3])
    if t2 < t1:
        time_intervals = [(t0, t3)]
    if t2 == t1:
        time_intervals = [(t0, t1)] + [(t2, t3)]
    if t2 > t1:
        selected_times = [t for t in TIMES if (t >= t1 and t <= t2)]
        time_intervals = []
        for i in range(1, len(selected_times)):
            time_intervals = time_intervals + [(selected_times[i-1], selected_times[i])]
        time_intervals = [(t0, t1)] + time_intervals + [(t2, t3)]
    return time_intervals


def get_extra_prices(time_interval, env = 'qa', add_prices=[]):
    if len(add_prices)==0:
        return pd.DataFrame()
    if add_prices=='all':
        filter_selections = ''
    elif len(add_prices)>0:
        filter_selections='0'
        for sel in add_prices:
            filter_selections=filter_selections+','+str(sel)
        filter_selections = q.FILTER_SELECTIONS.format(
            selections_str=filter_selections)
    
    prices_query = q.PRICES_QUERY.format(
        DB=DB[env], start_time=time_interval[0], end_time=time_interval[1],
        filter_selections=filter_selections)
    extra_prices = query_to_df(prices_query, env)
    return extra_prices.drop_duplicates()


def collect_rds_data(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    max_allocation = DEFAULT_INPUT['max_allocation'],
    tamip = DEFAULT_INPUT['tamip'],
    regions = [],
    env = 'qa',
    condition = None, # function
    add_allocation = True
):
    """
    receives:
    * lookback period
    * selections (legs)
    * score
    * min size
    * sip interval
    * regions
    * max at risk
    * tamip
    * region
    * condition (function)
    
    returns:
    * timestamp
    * month
    * year
    * competition
    * competition group
    * SIP
    * sip_bin (interval)
    * occurence (outcome)
    * net_profit
    * tamip
    """
    join_columns = ['eventId', 'timestamp', 'interval']
    start_time, end_time = lookback['start'], lookback['end']
    selections_strings = {'All': '0', 'Back': '0', 'Lay': '0'}
    strategy_columns = []
    for sel in selections:
        add_str = ','+str(selections[sel]['selection'])
#         if add_str[:-1] == ',223': # temp fix
#             add_str += add_str.replace('223', '235')
        selections_strings['All'] += add_str
        selections_strings[selections[sel]['side']] += add_str
        strategy_columns = strategy_columns + [col.format(n=str(sel)) for col in STRATEGY_COLUMNS]
    liquidity_group_query = q.LIQUIDITY_GROUPS_QUERY.format(DB=DB[env])
    competitions_query = q.COMPETITIONS_QUERY.format(
        DB=DB[env], start_time=start_time, end_time=end_time)
    results_query = q.RESULTS_QUERY.format(
        DB=DB[env], start_time=start_time, end_time=end_time)
    outcome_query = q.OUTCOME_QUERY.format(
        DB=DB[env],
        selections_back=selections_strings['Back'],
        selections_lay=selections_strings['Lay']
    )
    filter_selections = q.FILTER_SELECTIONS.format(selections_str=selections_strings['All'])
    prices_features_query = q.PRICES_FEATURES_QUERY.format(
        DB=DB[env],
        start_time=start_time,
        end_time=end_time,
        filter_selections=filter_selections,
        score=score,
        size=str(min_size)
    )
    
    liquidity_groups = query_to_df(liquidity_group_query, env)
    events_competitions = query_to_df(competitions_query, env)
    outcomes = query_to_df(outcome_query, env)
    results = query_to_df(results_query, env)
    
    prices_features = query_to_df(prices_features_query, env).drop_duplicates()
    prices = format_prices_for_oros(
        prices_features[join_columns+PRICE_COLUMNS], selections, sip_interval,
        False, False, condition
    )
    features = get_features(prices_features)
    res = prices.merge(features, on=join_columns)
    res['year'] = res['timestamp'].apply(lambda x: str(x)[:4])
    res['month'] = res['timestamp'].apply(lambda x: str(x)[5:7])
    res['sip_interval'] = str(sip_interval)
    res = res.merge(results, on='eventId').merge(events_competitions, on='eventId')
    res = res.merge(liquidity_groups, on='competition')
    
    if len(regions) > 0:
        res = res[(
            res.interval.isin([int(r[1]) for r in regions])
        )&(
            res.competitionGroup.isin([r[0] for r in regions])
        )]
    
    res['outcome'] = np.where(res.result.isin(outcomes.scoreSHalfEnd), 1, 0)
    
    If     
        res = add_allocations(res, selections, max_allocation)
        res = check_tamip(res, tamip)
    
    all_columns = INITIAL_COLUMNS+strategy_columns+FINAL_COLUMNS
    
    res = res[[col for col in all_columns if col in res.columns]]
    
    return res.drop_duplicates()


def aggregate_rds(df, select_first=True, periodisation=''):
    df_sel = df[(df.alloc0>0)&(df.tamip>0)]
    if select_first:
        df_sel = select_first_timestamp_in_interval(df_sel)
    groupby_col = ['competitionGroup', 'interval']
    if 'SIP_interval' in df_sel.columns:
        groupby_col = groupby_col + ['SIP_interval']
    if periodisation == 'yearly':
        groupby_col = groupby_col + ['year']
    if periodisation == 'monthly':
        groupby_col = groupby_col + ['year'] + ['month']
    if periodisation == 'quarterly':
        df_sel['quarter'] = df_sel['month'].apply(lambda x: (x-1)//3)
        groupby_col = groupby_col + ['year'] + ['quarter']
    
    calc_columns = ['opportunities', 'won', 'P', 'L', 'PL', 'PLnorm', 'winRatio', 'posRet']
    columns = groupby_col + calc_columns
    res = pd.DataFrame(columns=columns)
    combinations = df_sel[groupby_col].drop_duplicates()
    for i in range(len(combinations)):
        df_sel_i = df_sel.merge(combinations.iloc[i: i+1], on=groupby_col)
        df_sel_i['PLnorm'] = df_sel_i['PL']/df_sel_i['stake']
        opportunities = len(df_sel_i)
        won = len(df_sel_i[df_sel_i.outcome==1])
        P = sum(df_sel_i[df_sel_i.outcome==1].PL)
        L = sum(df_sel_i[df_sel_i.outcome==0].PL)
        PL = sum(df_sel_i.PL)
        PLnorm = sum(df_sel_i.PLnorm)
        posRet = np.mean(df_sel_i[df_sel_i.outcome==1]['PLnorm'])
        winRatio = won/opportunities
        res_add = pd.DataFrame([
            (opportunities, won, P, L, PL, PLnorm, winRatio, posRet)
        ], columns=calc_columns)
        for col in groupby_col:
            res_add[col] = combinations.iloc[i][col]
        res = res.append(res_add)
    
    res['minWinRatio'] = 1+res['posRet']
    res['minWinRatio'] = 1/res['minWinRatio']
    res = add_inv_prob(res)
    res['tamip'] = res['posRet']*res['minWinRatio']/res['winRatio']
    # add % > tamip
    return res


def get_rds(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    max_allocation = DEFAULT_INPUT['max_allocation'],
    tamip = DEFAULT_INPUT['tamip'],
    regions = [],
    env = 'qa',
    add_prices = [],
    split_time_intervals = False, 
    add_allocation = True,
    condition = None # function
):
    """
    returns rds data
    """
    rds = pd.DataFrame()
    time_intervals = [(lookback['start'], lookback['end'])]
    if split_time_intervals:
        time_intervals = get_time_intervals(lookback)

    for j in range(len(time_intervals)):
        time_interval = time_intervals[j]
        start_time, end_time = time_interval[0], time_interval[1]
        sub_lookback = {'start': start_time, 'end': end_time}
    
        df = collect_rds_data(
            selections, score, sub_lookback, min_size,
            sip_interval, max_allocation, tamip, regions, 
            env, add_allocation, condition
        )

        if len(add_prices)>0:
            group_columns = ['timestamp', 'eventId']
            added_prices = get_extra_prices(time_interval, env, add_prices)
            added_prices = format_prices_for_rds(added_prices)
            df = df.merge(added_prices, on=group_columns, how='left')
        
        rds = rds.append(df)
    return rds.drop_duplicates()


def save_rds(
    rds,
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    max_allocation = DEFAULT_INPUT['max_allocation'],
    tamip = DEFAULT_INPUT['tamip'],
    regions = [],
    filepath = 'output/'
):
    """
    creates the csv to store rds data
    """
    t = dt.datetime.now()
    t0, t1 = lookback['start'], lookback['end']
    lookback_str = t0[:10]+','+t1[:10]
    timestamp = str(t)[:16].replace(' ','.').replace('-','').replace(':','')
    sel_str_1, sel_str_2 = '', ''
    for sel in selections:
        selection = selections[sel]
        sel_id = str(selection['selection'])
        side = selection['side']
        sel_str_1 += '.'+sel_id+side[0]
        sel_str_2 += ','+sel_id+side
    sel_str_1, sel_str_2 = sel_str_1[1:], sel_str_2[1:]
    sip_interval_name = str(sip_interval[0])+'-'+str(sip_interval[1])
    filename = timestamp+'_RDS_'+sel_str_1+'_'+score+'_'+sip_interval_name+'.csv'
    sip_interval_str = 'SIP in '+str(sip_interval)
    min_size_str = 'Min size: '+str(min_size)
    max_allocation_str = 'Max possible stake at risk: '+str(max_allocation)
    tamip_str = 'Min acceptable return: '+str(tamip)
    
    f = open(os.path.join(filepath,filename), "w")
    f.close()
    
    groups = 'All regions'
    if len(regions) > 0:
        groups = 'Regions: '
        for g in regions:
            groups += ' '+g
    
    for row in [
        filename, lookback_str, sel_str_2, score,
        min_size_str, sip_interval_str, groups,
        max_allocation_str, tamip_str
    ]:
        f = open(os.path.join(filepath,filename), "a")
        f.write(row+"\n")
        f.close()
    
    
    columns_str = ''
    for col in rds.columns:
        columns_str = columns_str + col + ','
    f = open(os.path.join(filepath,filename), "a")
    f.write(columns_str[:-1]+"\n")
    f.close()
    
    for i in range(len(rds)):
        row = ''
        for col in rds.columns:
            row += str(rds.iloc[i][col])+','
        f = open(os.path.join(filepath,filename), "a")
        f.write(row[:-1]+"\n")
        f.close()
    return


def get_and_save_rds(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    max_allocation = DEFAULT_INPUT['max_allocation'],
    tamip = DEFAULT_INPUT['tamip'],
    regions = [],
    env = 'qa',
    filepath = 'output/',
    add_prices = [],
    split_time_intervals = False,
    add_allocations = True,
    condition = None # function
):
    """
    creates the csv to store rds data
    returns rds data
    """
    rds = get_rds(selections, score, lookback, min_size, sip_interval,
                  max_allocation, tamip, regions, env, add_prices,
                  split_time_intervals, add_allocations, condition)
    save_rds(rds, selections, score, lookback, min_size, sip_interval,
             max_allocation, tamip, regions, filepath)
    return rds


def get_and_save_ards(
    rds,
    select_first=True,
    periodisation='', 
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    max_allocation = DEFAULT_INPUT['max_allocation'],
    tamip = DEFAULT_INPUT['tamip'],
    regions = [],
    env = 'qa',
    filepath = 'output/',
    print_logs = False,
    initial_capital = 0
):
    """
    creates the csv to store ards data
    returns ards data
    """
    t = dt.datetime.now()
    t0, t1 = lookback['start'], lookback['end']
    lookback_str = t0[:10]+','+t1[:10]
    timestamp = str(t)[:16].replace(' ','.').replace('-','').replace(':','')
    sel_str_1, sel_str_2 = '', ''
    for sel in selections:
        selection = selections[sel]
        sel_id = str(selection['selection'])
        side = selection['side']
        sel_str_1 += '.'+sel_id+side[0]
        sel_str_2 += ','+sel_id+side
    sel_str_1, sel_str_2 = sel_str_1[1:], sel_str_2[1:]
    sip_interval_name = str(sip_interval[0])+'-'+str(sip_interval[1])
    filename = timestamp+'_ARDS'+periodisation+'_'+sel_str_1+'_'+score+'_'+sip_interval_name+'.csv'
    sip_interval_str = 'SIP in '+str(sip_interval)
    min_size_str = 'Min size: '+str(min_size)
    max_allocation_str = 'Max possible stake at risk: '+str(max_allocation)
    tamip_str = 'Min acceptable return: '+str(tamip)
    periodisation_str = periodisation if periodisation == '' else 'Periodisation: '+periodisation
    
    df = rds.copy()
    if 'SIP_interval' not in df.columns:
        df = df[(df.SIP>=sip_interval[0])&(df.SIP<sip_interval[1])]
        df['SIP_interval'] = str(sip_interval)
    df = aggregate_rds(df, select_first, periodisation)
    if initial_capital > 0:
        df['initial_capital'] = initial_capital
        df['ROC'] = df['PL']/initial_capital - 1
    
    f = open(os.path.join(filepath, filename), "w")
    f.close()
    
    groups = 'All regions'
    if len(regions) > 0:
        groups = 'Regions: '
        for g in regions:
            groups += ' '+g
    
    for row in [
        filename, lookback_str, sel_str_2, score,
        min_size_str, sip_interval_str, groups,
        max_allocation_str, tamip_str,
        periodisation_str
    ]:
        f = open(os.path.join(filepath,filename), "a")
        f.write(row+"\n")
        f.close()
    
    columns_str = ''
    for col in df.columns:
        columns_str = columns_str + col + ','
    f = open(os.path.join(filepath,filename), "a")
    f.write(columns_str[:-1]+"\n")
    f.close()
    for i in range(len(df)):
        row = ''
        for col in df.columns:
            row += str(df.iloc[i][col])+','
        f = open(os.path.join(filepath,filename), "a")
        f.write(row[:-1]+"\n")
        f.close()
    
    if print_logs:
        print('ARDS '+filepath+filename)
        f = open(filepath+filename, "r")
        print(f.read())
    return df


def rds_pipeline(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    max_allocation = DEFAULT_INPUT['max_allocation'],
    tamip = DEFAULT_INPUT['tamip'],
    regions = [],
    env = 'qa',
    filepath = '',
    print_logs = False,
    add_prices = [],
    initial_capital = None,
    n = 10, # number of bins
    condition = None # function
):
    sip_intervals = SIP_binner(
        selections, score, lookback, min_size,
        sip_interval, env, print_logs, save, n)
    for i in range(n):
        sip0 = sip_intervals.iloc[i]['Low_SIP_Bound']
        sip1 = sip_intervals.iloc[i]['UP_SIP_Bound']
        sip_interval = (sip0, sip1)
        rds = get_and_save_rds(
            selections, score, lookback, min_size, sip_interval, max_allocation,
            tamip, regions, env, filepath, print_logs, add_prices, False, condition)
        get_and_save_ards(
            rds, selections, score, lookback, min_size, sip_interval, max_allocation,
            tamip, regions, env, filepath, print_logs, initial_capital)
    return
