import datetime as dt
import pandas as pd
import numpy as np
import math as mt
import os
import os.path

def list_sub_folders(path,fullpath=False):
    folders = []
    for r, d, f in os.walk(path):
        for folder in d:
            add = os.path.join(r, folder) if fullpath else folder
            folders.append(add)           
    return folders

def list_files_in_folder(folder_path,fullname=False):
    files = []
    for r, d, f in os.walk(path):
        for file in f:
            add = os.path.join(r, file) if fullname else file[:-4]
            files.append(add)     
    return files
   
def add_date_id(df,col='cohort',month=True, forcedate=False):
    if col in df.columns:
        if str(df.dtypes[col])[:4] != 'date' :
            if forcedate:
                df[col] = df[col].apply(lambda x: dt.datetime.strptime(x[:10],'%Y-%m-%d'))
            else:
                print('Column '+col+' is not a date.')
                return
        if month:
            df['month_id'] = df[col].apply(lambda x: 100*x.year+x.month)
        else:
            df['date_id'] = df[col].apply(lambda x: 10000*x.year+100*x.month+x.day)
        return df

def add_month(month_str,n):
    y,m,d = month_str[:4],month_str[5:7],month_str[8:10]
    n1,n2 = n//12,n%12
    if int(m)+n2 > 12 :
        return str(int(y)+n1+1)+'-'+str(int(m)+n2+88)[1:]+'-'+d
    return str(int(y)+n1)+'-'+str(int(m)+n2+100)[1:]+'-'+d

months_dict = {
    '01':'Jan','02':'Feb','03':'Mar',
    '04':'Apr','05':'May','06':'Jun',
    '07':'Jul','08':'Aug','09':'Sep',
    '10':'Oct','11':'Nov','12':'Dec'
}

def n_to_bool_array(n,l=10):
    bn = bin(n).replace('0b','')
    l1 = len(bn)
    if l1 > l:
        l = l1
    l0 = l - l1
    a = []
    for i in range(l0):
        a = a + [False]
    for i in range(l1):
        b = ( bn[i] == '1' )
        a = a + [b]
    return a