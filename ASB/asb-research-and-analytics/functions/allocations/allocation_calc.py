import pandas as pd
import numpy as np
import math as mt

default_values = {
    'T':50, # total allocation - calculated_every_day
    'f':0.05, # fees
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

def get_allocations(
    param = prepare_param(default_values),
    pr_values = prepare_pr(example_values),
    rtrn_dict = True,
    print_log = True
):
    # this function returns the optimal allocations for a n-tailed qarb
    n = len(pr_values)
    if 'f1' not in param:
        param = prepare_param(param)
    f,f1,f2,f3 = param['f'],param['f1'],param['f2'],param['f3']
    m = param['min_all']
    T = param['T']
    for i in range(n):
        if 'P' not in pr_values['tail'+str(i)]:
            pr_values['tail'+str(i)]['P'] = 1/pr_values['tail'+str(i)]['O']
    O = [pr_values['tail'+str(i)]['O'] for i in range(n)]
    P = [pr_values['tail'+str(i)]['P'] for i in range(n)]
    S = [pr_values['tail'+str(i)]['S'] for i in range(n)]
    Pt = sum(P[1:])
    c0 = O[0]*f1+f
    c1 = 1/c0
    A0min = T*c1 # minimal acceptable value for A0
    if A0min > T-(n-1)*m:
        if print_log:
            print("No acceptable allocations combination could be find (A0min > T-n*m)")
        return
    b0 = 1 + Pt*f3
    d0 = 1 - b0*c1 - Pt
    a0 = (Pt*f2 + b0*c1)
    e = T*d0/a0
    A0t = min(T-(n-1)*m,A0min+e*c1) # theoretical value of A0
    a1 = T + A0t*f3 + e*f2
    A = [round(max(m,a1*P[i]),2) for i in range(1,n)]
    At = sum(A)
    A = [round(T - At,2)] + A
    if A[0] < A0min: # if this condition is violated, no allocations combination is acceptable
        if print_log:
            print("No acceptable allocations combination could be find (A[0] < A0min)")
        return
    if max([A[i] - S[i] for i in range(n)]) > 0:
        E = min([S[i]/A[i] for i in range(n)])
        A = [a*E for a in A]
        # if some of the allocations are bigger than the size, all values are scaled down
        R = A[0]*O[0]*(1-f)-sum(A)
        if R < 0:
            if print_log:
                print("No acceptable allocations combination could be find after re-sizing")
            return
    if rtrn_dict:
        allocations = {}
        for i in range(n):
            allocations['tail'+str(i)] = A[i]
        return allocations
    return A
    
def get_allocations_lay0(
    param = prepare_param(default_values),
    pr_values = prepare_pr(example_values),
    rtrn_dict = True,
    print_log = True
):
    # this function returns the optimal allocations for a n-tailed qarb with lay on position0 and back on the tails
    n = len(pr_values)
    if 'f1' not in param:
        param = prepare_param(param)
    f,f1,f2,f3 = param['f'],param['f1'],param['f2'],param['f3']
    m = param['min_all']
    T = param['T']
    for i in range(n):
        if 'P' not in pr_values['tail'+str(i)]:
            pr_values['tail'+str(i)]['P'] = 1/pr_values['tail'+str(i)]['O']
    O = [pr_values['tail'+str(i)]['O'] for i in range(n)]
    P = [pr_values['tail'+str(i)]['P'] for i in range(n)]
    S = [pr_values['tail'+str(i)]['S'] for i in range(n)]
    
    Pt = sum(P[1:])
    p0 = Pt*f2
    a0 = (O[0]-f)/(O[0]-1)
    b0 = p0*a0
    c0 = 1 + b0 + p0*f
    Tcs = T*b0/c0
    if O[0]*Tcs > T*f1:
        print("No acceptable allocations combination could be find")
        return
    A0 = (T-Tcs)/(O[0]-1)
    e = A0*f1 - Tcs
    a1 = f2*(T - f*Tcs + e)
    A = [round(max(m,a1*P[i]),2) for i in range(1,n)]
    A = [round(A0,2)] + A
    
    if max([A[i] - S[i] for i in range(n)]) > 0:
        E = min([S[i]/A[i] for i in range(n)])
        A = [a*E for a in A]
        # if some of the allocations are bigger than the size, all values are scaled down
        R = A[0]*(1-f)-sum(A[1:])
        if R < 0:
            if print_log:
                print("No acceptable allocations combination could be find after re-sizing")
            return
    if rtrn_dict:
        allocations = {}
        for i in range(n):
            allocations['tail'+str(i)] = A[i]
        return allocations
    return A

def get_allocations_general(
    selections,
    param = prepare_param(default_values),
    values = prepare_pr(example_values),
    rtrn_dict = True,
    print_log = False
):
    if selections[0]['side'] == 'Back':
        if set([selections[i]['side'] for i in selections if i>0]) == {'Back'}:
            return get_allocations(param, values, rtrn_dict, print_log)
    if selections[0]['side'] == 'Lay':
        if set([selections[i]['side'] for i in selections if i>0]) == {'Back'}:
            return get_allocations_lay0(param, values, rtrn_dict, print_log)
    return None

def get_arbitrage_allocaton_all_back(
    prices, sizes, total_at_risk, fees, min_alloc=2, min_return=0
):
    impl_probs = [1/x for x in prices]
    SIP = sum(impl_probs)
    if (1-SIP)*(1-fees) < min_return:
        return [0, 0]
    new_probs = [x/SIP for x in impl_probs]
#     allocations = [round(total_at_risk*x, 2) for x in new_probs] # simple version
    new_prices = [(x-1)*(1-fees) + 1 for x in prices]
    SIP1 = sum(new_prices)
    allocations = [round(total_at_risk*new_prices[1-i]/SIP1, 2) for i in range(2)]
    resize = min([sizes[i]/allocations[i] for i in range(2)])
    if resize < 1:
        allocations = [round(x*resize, 2) for x in allocations]
    if min(allocations) < min_alloc:
        return [0, 0]
    return allocations
        
    