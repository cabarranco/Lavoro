import math as mt
import numpy as np
import pandas as pd
import datetime as dt
import statistics as st

from functions.db.db_functions import query_to_df, execute_query
from functions.utils.queries import (
    EXTRACT_DATA_FOR_LIQUIDITY_GROUPS_QUERY,
    CREATE_LIQUIDITY_GROUPS_QUERY,
    POPULATE_LIQUIDITY_GROUPS_QUERY
)
from functions.utils.variables import DB, LIQUIDITY_GROUPS_DICT

COLUMNS = {
    'uploadDate': 'S',
    'competition': 'S',
    'medianVol': 'F',
    'averageVol': 'F',
    'minVol': 'F',
    'maxVol': 'F',
    'inSampleEvents': 'I',
    'competitionGroup': 'S'
}


def get_extract_query(env='qa'):
    t = dt.datetime.now() - dt.timedelta(days = 730)
    start_date = str(t)[:10]
    query = EXTRACT_DATA_FOR_LIQUIDITY_GROUPS_QUERY.format(
        DB=DB[env],
        start_date=start_date
    )
    return query


def get_populate_query(df):
    L = len(df)
    endl = """
    """
    q = ""
    for i in range(L):
        q_i = "("
        for col in COLUMNS:
            q_add = str(df.iloc[i][col])
            if COLUMNS[col] == 'S':
                q_add = '"'+q_add+'"'
            q_add = q_add+(',' if col != 'competitionGroup' else ')')
            q_i = q_i+q_add
        q_i = q_i+(','+endl if i < L-1 else ';')
        q = q+q_i
    q = POPULATE_LIQUIDITY_GROUPS_QUERY+q
    return q


def calculate_liquidity_groups(df_in):
    max_matched = 0
    uploadDate = str(dt.datetime.now())[:10]
    df_out = pd.DataFrame(columns=COLUMNS)
    competitions = list(set(df_in.competition))
    for competition in competitions:
        df_competition = df_in[df_in.competition==competition]
        L = list(df_competition.totalMatched)
        numEvents = len(L)
        maxVol = round(max(L), 2)
        minVol = round(min(L), 2)
        averageVol = round(st.mean(L), 2)
        medianVol = round(st.median(L), 2)
        if medianVol > max_matched:
            max_matched = medianVol
        data = [(uploadDate, competition, medianVol, averageVol, minVol, maxVol, numEvents, '')]
        df_add = pd.DataFrame(data, columns=COLUMNS)
        df_out = df_out.append(df_add)
    base = mt.exp(mt.log(max_matched)/10)
    def add_liquidity_group(m):
        if m <= 1:
            return ''
        score = round(mt.log(m, base), 0)
        if score == 0:
            return ''
        return LIQUIDITY_GROUPS_DICT[score]
    
    df_out['competitionGroup'] = df_out['medianVol'].apply(lambda x: add_liquidity_group(x))
    return df_out[df_out.competitionGroup!='']


def update_liquidity_groups(env='qa'):
    df = query_to_df(get_extract_query(env))
    df = calculate_liquidity_groups(df)
    execute_query(CREATE_LIQUIDITY_GROUPS_QUERY.format(DB=DB[env]))
    execute_query(get_populate_query(df), env)
    return
