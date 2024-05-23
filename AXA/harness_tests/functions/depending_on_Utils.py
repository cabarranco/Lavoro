import pandas as pd
from functions.locl import fact
from Utils.pandas_df import clean_column_names

def messy_funct(df):
  df = clean_column_names(df)
  d_types = df.dtypes
  for col in df.columns:
    if d_types[col] == int:
      df[col+'_fact] = df[col].apply(lambda x :fact(x) if x > -1 and x < 10) else 0)
      return df
