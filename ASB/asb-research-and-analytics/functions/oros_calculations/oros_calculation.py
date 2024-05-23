import os
import math as mt
import numpy as np
import pandas as pd
import datetime as dt
import statistics as st
from functions.db.db_functions import query_to_df

from functions.utils.variables import (
    DB, INTERVALS, LIQUIDITY_GROUPS, DEFAULT_INPUT, DEFAULT_SIP_LEVELS, EVENTS_EXAMPLE,
    STATS_ARGUMENTS, STATS_COLUMNS
)
from functions.utils.utils import format_prices_for_oros
import functions.utils.queries as q


def collect_oros_data(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    env = 'qa',
    print_logs = False,
    save = False,
    condition = None # function
):
    start_time, end_time = lookback['start'], lookback['end']
    selections_strings = {'All': '0', 'Back': '0', 'Lay': '0'}
    for sel in selections:
        add_str = ','+str(selections[sel]['selection'])
        if add_str[:-1] == ',223': # temp fix
            add_str += add_str.replace('223', '235')
        selections_strings['All'] += add_str
        selections_strings[selections[sel]['side']] += add_str
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
    prices_scores_query = q.PRICES_SCORES_QUERY.format(
        DB=DB[env],
        start_time=start_time,
        end_time=end_time,
        filter_selections=filter_selections,
        score=score,
        size=str(min_size)
    )
    prelive_prices_query = q.PRELIVE_PRICES_QUERY.format(
        DB=DB[env],
        start_time=start_time,
        end_time=end_time,
        main_selection=str(selections[0]['selection']),
        selections_str=selections_strings['All'].replace(','+str(selections[0]['selection'])+',', ','),
        backsize=str(min_size) if selections[0]['side']=='Back' else '0',
        laysize=str(min_size) if selections[0]['side']=='Lay' else '0'
    )
    if print_logs:
        print("######")
        print(liquidity_group_query)
        print("######")
        print(competitions_query)
        print("######")
        print(results_query)
        print("######")
        print(outcome_query)
        print("######")
        print(prices_scores_query)
        print("######")
        if score == '0-0':
            print(prelive_prices_query)
            print("######")
    
    liquidity_groups = query_to_df(liquidity_group_query, env)
    events_competitions = query_to_df(competitions_query, env)
    outcomes = query_to_df(outcome_query, env)
    results = query_to_df(results_query, env)
    df = results.merge(events_competitions, on='eventId')
    df = df.merge(liquidity_groups, on='competition')
    df['outcome'] = np.where(df.result.isin(outcomes.scoreSHalfEnd), 1, 0)
    prices = query_to_df(prices_scores_query, env)
    if score == '0-0':
        prices = prices.append(query_to_df(prelive_prices_query, env))
    df = df.merge(
        format_prices_for_oros(prices, selections, sip_interval, save, True, condition),
        on='eventId')
    return df.drop_duplicates()


def calculate_oros(df):
    columns = ['competitionGroup', 'interval']
    counts = df[columns+['SIP']].groupby(columns).count().reset_index().rename(columns={'SIP': 'opportunities'})
    sip_average = df[columns+['SIP']].groupby(columns).mean().reset_index().rename(columns={'SIP': 'avg_sip'})
    occurence_rate = df[columns+['outcome']].groupby(columns).mean().reset_index().rename(columns={'outcome': 'occ_rate'})
    low_sip_perc = df[columns+['SIP']].merge(occurence_rate, on=columns)
    low_sip_perc['low_sip_perc'] = np.where(low_sip_perc.SIP < low_sip_perc.occ_rate, 1, 0)
    low_sip_perc = low_sip_perc[columns+['low_sip_perc']].groupby(columns).mean().reset_index()
    max_daily_opp = df[columns+['timestamp', 'eventId']]
    max_daily_opp['timestamp'] = max_daily_opp['timestamp'].apply(lambda x: str(x)[:10])
    max_daily_opp = max_daily_opp.groupby(columns+['timestamp']).count().reset_index()
    max_daily_opp = max_daily_opp[columns+['eventId']].groupby(columns).max().reset_index()
    max_daily_opp = max_daily_opp.rename(columns={'eventId': 'max_daily_opp'})
    res = counts.merge(
        sip_average, on=columns).merge(occurence_rate, on=columns).merge(
        low_sip_perc, on=columns).merge(max_daily_opp, on=columns)
    res['occ_rate/avg_sip'] = res['occ_rate']/res['avg_sip']
    return res


def calculate_and_save_oros_from_dataset(
    df,
    selection, score, lookback, min_size, sip_interval,
    filepath = '',
    env = 'qa',
    print_logs = False,
    save = False,
    condition = None # function
):
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
    filename = timestamp+'_OROS_'+sel_str_1+'_'+score+'_'+sip_interval_name+'.csv'
    sip_interval_str = 'SIP in '+str(sip_interval)
    min_size_str = 'Min size: '+str(min_size)
    
    df = calculate_oros(df)
    if print_logs:
        print('')
        print('OROS table')
        print(df)
    
    f = open(os.path.join(filepath,filename), "w")
    f.close()
    
    groups = ''
    for g in LIQUIDITY_GROUPS:
        groups += ','+g
    
    for row in [filename, lookback_str, sel_str_2, score, min_size_str, sip_interval_str, groups]:
        f = open(os.path.join(filepath,filename), "a")
        f.write(row+"\n")
        f.close()
    
    for col in ['opportunities', 'occ_rate/avg_sip', 'occ_rate', 'avg_sip', 'low_sip_perc', 'max_daily_opp']:
        f = open(os.path.join(filepath,filename), "a")
        f.write(col+"\n")
        f.close()
        for i in INTERVALS:
            row = str(i)
            for g in LIQUIDITY_GROUPS:
                dfx = df[(df.competitionGroup==g)&(df.interval==i)]
                row += ','+(str(round(dfx.iloc[0][col], 3)) if len(dfx) > 0 else '')
            f = open(os.path.join(filepath,filename), "a")
            f.write(row+"\n")
            f.close()
    
    f = open(filepath+filename, "r")
    if print_logs:
        print('OROS heatmap')
        print(f.read())
    return


def get_and_save_oros(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    filepath = '',
    env = 'qa',
    print_logs = False,
    save = False,
    condition = None # function
):
    df = calculate_oros(collect_oros_data(
        selections, score, lookback, min_size, sip_interval,
        env, print_logs, save, condition))
    
    calculate_and_save_oros_from_dataset(
        df,
        selection, score, lookback, min_size, sip_interval,
        filepath, env, print_logs, save, condition
    )
    return


def SIP_binner(data, n = 10):
    data = data[['SIP']].sort_values('SIP', ascending=False)
    L = len(data)
    if L < 1000:
        print("Dataset is too small")
        return
    if n > L/100:
        print("Required number of bins is too big")
        return
    columns = ['SIP_Bin', 'Frequency', 'Up_SIP_Bound', 'Low_SIP_Bound']
    res = pd.DataFrame(columns=columns)
    M = int(round(L/n, 0))
    sip0 = 1
    for i in range(n):
        i0 = i*M
        i1 = (i+1)*M if i < n-1 else L
        dfi = data.iloc[i0: i1]
        sip1 = sip0
        sip0 = min(dfi.SIP)
        df_add = pd.DataFrame([(i+1, len(dfi), sip1, sip0)], columns=columns)
        res = res.append(df_add)
    return res


def oros_pipeline(
    selections = DEFAULT_INPUT['selections'],
    score = DEFAULT_INPUT['score'],
    lookback = DEFAULT_INPUT['lookback'],
    min_size = DEFAULT_INPUT['min_size'],
    sip_interval = DEFAULT_INPUT['SIP_interval'],
    env = 'qa',
    filepath = '',
    print_logs = False,
    save = False,
    n = 10, # number of bins
    condition = None # function
):
    data = collect_oros_data(
        selections, score, lookback, min_size, 
        sip_interval, env, print_logs, save, condition
    )
    sip_intervals = SIP_binner(data, n)
    for i in range(n):
        sip0 = sip_intervals.iloc[i]['Low_SIP_Bound']
        sip1 = sip_intervals.iloc[i]['Up_SIP_Bound']
        sip_interval = (sip0, sip1)
        get_and_save_oros(
            selections, score, lookback, min_size, sip_interval,
            filepath, env, print_logs, save, condition)
    return


COLUMNS_DICTIONARY = {
    'opportunities': {'avg': 'avg_', 'avg2': 'avg2_', 'median': 'mdn_', 'stdev': 'std_'},
    'avg_sip': {'avg': 'avg_', 'avg2': 'avg2_', 'median': 'mdn_', 'stdev': 'std_'},
    'occ_rate': {'avg': 'avg_', 'avg2': 'avg2_', 'median': 'mdn_', 'stdev': 'std_'},
    'low_sip_perc': {'avg': 'avg', 'avg2': 'avg2_', 'median': 'mdn_', 'stdev': 'std_'},
    'max_daily_opp': {'avg': 'max_', 'avg2': 'max2_', 'median': 'mdn_', 'stdev': 'std_'},
    'occ_rate/avg_sip': {'avg': 'avg_', 'avg2': 'avg2_', 'median': 'mdn_', 'stdev': 'std_'}
}
GROUPBY_COLUMNS = ['competitionGroup','interval','SIP_interval']


def calculate_OROS_avg(oros_df):
    avg_columns = ['avg_sip', 'occ_rate', 'low_sip_perc']
    oros_avg_df = oros_df[GROUPBY_COLUMNS+['opportunities']+avg_columns]
    for col in avg_columns:
        oros_avg_df[col] = oros_avg_df[col]*oros_avg_df['opportunities']
    oros_avg_df = oros_avg_df.groupby(GROUPBY_COLUMNS).sum().reset_index()
    for col in avg_columns:
        oros_avg_df[col] = oros_avg_df[col]/oros_avg_df['opportunities']
    oros_avg_df = oros_avg_df.merge(
        oros_df[GROUPBY_COLUMNS+['max_daily_opp']].groupby(GROUPBY_COLUMNS).max().reset_index(),
        on = GROUPBY_COLUMNS
    )
    oros_avg_df['occ_rate/avg_sip'] = oros_avg_df['occ_rate']/oros_avg_df['avg_sip']
    return oros_avg_df


def get_OROS_avg(df):
    months = list(set(df['month']))
    months.sort()
    L = len([m for m in months if m != 'All'])
    if 'All' in months:
        df_avg = df[df['month']=='All']
    else:
        df_avg = calculate_OROS_avg(df)
    OROS_MOHB_CS00B_CS01B_total_avg['opportunities'] = df_avg['opportunities']/L
    for col in COLUMNS_DICTIONARY:
        df_avg = df_avg.rename(columns={col: COLUMNS_DICTIONARY[col]['avg']+col})
    return df_avg

def get_OROS_avg2(df):
    months = list(set(df['month']))
    months = [m for m in months if m != 'All']
    months.sort()
    L = len(months[:-2])
    df_avg2 = df[df['month'].isin(months[:-2])]
    df_avg2 = calculate_OROS_avg(df)
    df_avg2['opportunities'] = df_avg2['opportunities']/L
    for col in COLUMNS_DICTIONARY:
        df_avg2 = df_avg2.rename(columns={col: COLUMNS_DICTIONARY[col]['avg2']+col})
    return df_avg2

def get_OROS_median(df):
    df_median = df[df['month']!='All'].groupby(GROUPBY_COLUMNS).median().reset_index()
    for col in COLUMNS_DICTIONARY:
        df_median = df_median.rename(columns={col: COLUMNS_DICTIONARY[col]['median']+col})
    return df_median

def get_OROS_stdev(df):
    df_stdev = df[df['month']!='All'].groupby(GROUPBY_COLUMNS).std().reset_index()
    for col in COLUMNS_DICTIONARY:
        df_stdev = df_stdev.rename(columns={col: COLUMNS_DICTIONARY[col]['stdev']+col})
    return df_stdev


def get_selected_OROS(df, gamma=16):
    months = list(set(df['month']))
    months = [m for m in months if m != 'All']
    months.sort()
    df_avg = get_OROS_avg(df)
    df_stdev = get_OROS_stdev(df)
    selected_df = df_avg.merge(df_stdev, on = GROUPBY_COLUMNS)
    selected_df['occ_relevance'] = selected_df['avg_oros'] - 1
    selected_df['occ_relevance'] = selected_df['std_oros']/selected_df['occ_relevance']
    selected_df['occ_relevance'] = selected_df['occ_relevance']*selected_df['occ_relevance']
    selected_df['occ_relevance'] = selected_df['occ_relevance']*gamma
    selected_df['opportunities'] = selected_df['avg_opportunities']*len(months)

    selected_df = selected_df[
        (selected_df['SIP_interval']!='(0, 1)')&
        (selected_df['avg_oros']>0.9)&
        (selected_df['avg_low_sip_perc']>0.5)&
        (selected_df['opportunities']>selected_df['occ_relevance'])
    ]
    
    df_median = get_OROS_median(df)
    df_avg2 = get_OROS_avg2(df)
    selected_df = selected_df.merge(dfmedian, on=GROUPBY_COLUMNS).merge(df_avg2, on=GROUPBY_COLUMNS)
    avg_columns = [col for col in selected_df.columns if col[:4]=='avg_']
    std_columns = [col for col in selected_df.columns if col[:4]=='std_']
    mdn_columns = [col for col in selected_df.columns if col[:4]=='mdn_']
    avg2_columns = [col for col in selected_df.columns if col[:4]=='avg2']
    columns = GROUPBY_COLUMNS+avg_columns+std_columns+['max_max_daily_opp']+mdn_columns+avg2_columns
    selected_df = selected_df[columns].drop_duplicates()
    return selected_df


def get_total_points(oros_df, ards_df, save=True):
    sel_oros_df = get_selected_OROS(oros_df, save)
    df = ards_df[GROUPBY_COLUMNS+['PL']]
    df = df.groupby(GROUPBY_COLUMNS).sum().reset_index()
    df = df.merge(sel_oros_df[GROUPBY_COLUMNS], on=GROUPBY_COLUMNS)
    df = df.rename(columns={'PL': 'points'})
    df_oros_scores = sel_oros_df.merge(df, on=GROUPBY_COLUMNS).sort_values('points', ascending=False)
    return df_oros_scores


def get_total_ranking(df, save=True):
    df_ranking = df[[
        'competitionGroup', 'interval', 'SIP_interval',
        'avg_opportunities', 'avg_oros', 'avg_occ_rate', 'avg_low_sip_perc',
        'std_oros', 'std_occ_rate', 'std_low_sip_perc', 'std_max_daily_opp',
        'mdn_oros', 'mdn_occ_rate', 'mdn_avg_sip', 'mdn_max_daily_opp',
        'avg2_oros', 'avg2_occ_rate', 'avg2_low_sip_perc',
        'points'
    ]].reset_index().drop(columns=['index'])
    df_ranking = df_ranking.reset_index()
    df_ranking = df_ranking.rename(columns={'index': 'rank'})
    df_ranking['rank'] = df_ranking['rank']+1
    return df_ranking


WEIGHTS = {
    'avg_oros': 0.1,
    'std_occ_rate': 3.0,
    'std_low_sip_perc': 0.8,
    'std_max_daily_opp': 0.1,
    'std_oros': 4,
    'mdn_avg_sip': -1,
    'mdn_occ_rate': -4.3,
    'mdn_max_daily_opp': 0.5,
    'mdn_oros': -0.1,
    'avg2_low_sip_perc': 4,
    'avg2_oros': 3
}
def get_weighted_points(df):
    weighted_df = df.copy()
    weighted_df['points'] = 0
    for col in WEIGHTS:
        weighted_df['points'] = weighted_df['points']*weighted_df[col]
    return weighted_df
