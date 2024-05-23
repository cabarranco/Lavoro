import pandas as pd
import numpy as np
from sympy import Matrix
import numpy.linalg as linalg
from numpy.linalg import eig # eigen values and vectors


def normalize_dataframe(df, min_max=False, median=False):
    if min_max:
        return (df-df.min())/(df.max()-df.min())
    if median:
        return(df-df.median())/df.std()
    return (df-df.mean())/df.std()


def build_correlation_matrix(df):
    wdf = df[
        [col for col in df.columns if df.dtypes[col] in [int, float]]
    ]
    wdf = normalize_dataframe(wdf)
    return wdf.corr()


def diagonalise_df(df):
    dfM = df[[col for col in df.columns if df.dtypes[col] in [int, float]]]
    if len(dfM) != len(dfM.columns):
        return None
    M = []
    for i in range(len(dfM)):
        row = []
        for col in dfM.columns:
            row = row + [dfM.iloc[i][col]]
        M = M+[row]
    M = Matrix(M)
    P, D = M.diagonalize()
    Pinv = P.inv()
    return D, P, Pinv


def get_eigenvectors(df):
    eigenValues, eigenVectors = linalg.eig(df)
    idx = eigenValues.argsort()[::-1]   
    eigenValues = eigenValues[idx]
    eigenVectors = eigenVectors[:,idx]
    return eigenValues, eigenVectors


def select_principal_vectors(df, n=None, eps=None):
    if n is not None and eps is not None:
        columns = list(df.columns)
        idx = [i for i in range(len(df)) if df.iloc[i][columns[i]] > eps]
        df_sel = df[columns[idx]].iloc[idx]
        if len(df_sel) > n:
            columns = list(df_sel.columns)[:n]
            df_sel = df_sel[columns].iloc[:n]
        return df_sel
    if n is not None and eps is None:
        columns = list(df.columns)[:n]
        return df[columns].iloc[:n]
    if n is None and eps is not None:
        columns = list(df.columns)
        idx = [i for i in range(len(df)) if df.iloc[i][columns[i]] > eps]
        return df[columns[idx]].iloc[idx]
    return df

