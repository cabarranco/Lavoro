import os
import math as mt
import numpy as np
import pandas as pd
import datetime as dt
from scipy.special import beta, betainc
from functions.utils.variables import STATS_ARGUMENTS, STATS_COLUMNS


def get_prob(p, n, k):
    return betainc(k+1, n-k+1, p)


def add_inv_prob(df):
    "calculates probability that true Wr can be more than the needed minWinRatio given current wins over total"
    df['p'] = df['minWinRatio'].apply(lambda x: [x])
    df['n'] = df['opportunities'].apply(lambda x: [x])
    df['k'] = df['won'].apply(lambda x: [x])
    df['prob'] = df['p']+df['n']+df['k']
    df['prob'] = df['prob'].apply(lambda x: get_prob(x[0], x[1], x[2]))
    df['prob'] = 1 - df['prob']
    df = df.drop(columns=['p', 'n', 'k'])
    return df


def select_first_timestamp_in_interval(df):
    columns = ['interval', 'eventId']
    df_min = df[columns+['timestamp']].groupby(columns).min().reset_index()
    return df.merge(df_min, on=columns+['timestamp']).drop_duplicates()


def format_prices_for_oros(
    prices, selections,
    sip_interval=(0, 1),
    save=False, select_first=True,
    condition=None # function
):
    sip0, sip1 = max(0, sip_interval[0]), min(1, sip_interval[1])
    columns = ['interval', 'eventId', 'timestamp']
    df = prices[columns].drop_duplicates()
    df['SIP'] = 0
    for sel in selections:
        sel_id = selections[sel]['selection']
        sel_side = selections[sel]['side']
        price_col = 'backPrice' if sel_side == 'Back' else 'layPrice'
        size_col = 'backSize' if sel_side == 'Back' else 'laySize'
        df_add = prices[prices.asbSelectionId.astype(str)==str(sel_id)][columns+[price_col, size_col]]
        df_add = df_add.rename(columns={price_col: 'price'+str(sel), size_col: 'size'+str(sel)})
        df = df.merge(df_add, on=columns, how='left')
        df['SIP'] = df['SIP'] + (1/df['price'+str(sel)] if sel_side == 'Back' else (1 - 1/df['price'+str(sel)]))
    sel_name = ''
    for sel in selections:
        sel_name += '_'+str(selections[sel]['selection'])+str(selections[sel]['side'])[0]
    if save:
        df.to_csv('data/formatted_prices'+sel_name+'.csv', index=False)
    if condition is not None:
        df = condition(df)
    df = df[(df.SIP>=sip0)&(df.SIP<sip1)].drop_duplicates()
    if select_first:
        df = select_first_timestamp_in_interval(df)
    return df


def format_prices_for_rds(prices):
    prices = prices.drop_duplicates()
    selections = prices.asbSelectionId.unique()
    group_columns = ['timestamp', 'eventId']
    df = prices[group_columns].drop_duplicates()
    for sel in selections:
        df_add = prices[
            prices.asbSelectionId.astype(str)==str(sel)
        ][group_columns+STATS_COLUMNS].drop_duplicates()
        for col in STATS_COLUMNS:
            df_add = df_add.rename(columns={col: str(sel)+'_'+col})
        df = df.merge(df_add, on=group_columns, how='left').drop_duplicates()
    return df.drop_duplicates()


def calc_drop_down(df):
    seq_loss_count = [0 for i in range(len(df))]
    seq_drop = [0 for i in range(len(df))]
    if df.iloc[0]['PL'] < 0:
        seq_loss_count[0] = 1
        seq_drop[0] = df.iloc[0]['PL']
    for i in range(1, len(df)):
        if df.iloc[i]['PL'] < 0:
            seq_loss_count[i] = 1 + seq_loss_count[i-1]
            seq_drop[i] = df.iloc[i]['PL'] + seq_drop[i-1]
    df['seq_loss_count'] = seq_loss_count
    df['seq_drop'] = seq_drop
    return df
