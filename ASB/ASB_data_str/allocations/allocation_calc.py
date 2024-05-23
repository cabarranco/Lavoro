import pandas as pd
import numpy as np
import math as mt

default_values = {
    'T':50, # total allocation - calculated_every_day
    'f':0.05, # fees
    'e1':0.5, # minimal margin ~ T/100
    'min_all':2 # minimal allocation
}

example_values = {
    'tail0':{'O':1.27,'S':1424.94}, # MO
    'tail1':{'O':30,'S':45.29}, # CS00
    'tail2':{'O':44,'S':46.53}, # CS01
    'tail3':{'O':15,'S':20.31} # CS11
}

def prepare_param(param):
    param['f1'] = 1 - param['f']
    param['f2'] = 1/param['f1']
    param['f3'] = param['f']*param['f2']
    return param

def prepare_pr(pr_values):
    for i in range(len(pr_values)):
        tail = 'tail'+str(i)
        pr_values[tail]['P'] = 1/pr_values[tail]['O']
    return pr_values

def get_allocations(param = prepare_param(default_values), pr_values = prepare_pr(example_values), rtrn_dict = True):
    # this function returns the optimal allocations for a n-tailed qarb
    n = len(pr_values)
    f,f1,f2,f3 = param['f'],param['f1'],param['f2'],param['f3']
    m = param['min_all']
    T = param['T']
    e1 = param['e1']
    O = [pr_values['tail'+str(i)]['O'] for i in range(n)]
    P = [pr_values['tail'+str(i)]['P'] for i in range(n)]
    S = [pr_values['tail'+str(i)]['S'] for i in range(n)]
    Pt = sum(P[1:])
    c0 = O[0]*f1+f
    c1 = 1/c0
    A0min = T*c1 # minimal acceptable value for A0
    if A0min > T-n*m:
        print("No acceptable allocations combination could be find")
        return
    a0 = T + A0min*f3
    b0 = 1 + Pt*f3
    d0 = 1 - b0*c1 - Pt
    e0max = T*d0*c0/b0
    e0 = e0max - Pt*e1*f2
    A0t = min(T-n*m,A0min+e0*c1) # theoretical value of A0
    a1 = T + A0t*f3 + e1*f2
    A = [round(max(2,a1*P[i]),2) for i in range(1,n)]
    At = sum(A)
    A = [round(T - At,2)] + A
    if A[0] < A0min: # if this condition is violated, no allocations combination is acceptable
        print("No acceptable allocations combination could be find")
        return
    if max([A[i] - S[i] for i in range(n)]) > 0:
        E = min([S[i]/A[i] for i in range(n)])
        A = [a*E for a in A]
        # if some of the allocations are bigger than the size, all values are scaled down
    if rtrn_dict:
        allocations = {}
        for i in range(n):
            allocations['tail'+str(i)] = A[i]
        return allocations
    return A
    
    
    
    