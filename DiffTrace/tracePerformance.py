from collections import defaultdict
import numpy as np
from pprint import pprint

iterations = []
spentd = {}
spentd['defects4j'] = defaultdict(dict)
spentd['refOracle'] = defaultdict(dict)

RMD_stages = ['ModelDiff execution (part1)','ModelDiff execution (part2)','Diff execution','MovedDiff execution','Fixing execution']
IAM_stages = ['iASTMapper execution','iASTMapper Fixing execution']
DAT_stages = ['Diff_Auto_Tuning execution','Diff_Auto_Tuning Fixing execution']
GTS_stages = ['GumTree-simple execution','GumTree-simple Fixing execution']
GTG_stages = ['GumTree-greedy execution','GumTree-greedy Fixing execution']

log_difffix='info_v2.37_r12.log'

with open(log_difffix,'r',encoding='utf8') as f:
  dataset = None
  caseinfo = None
  datas = None

  for e in f.readlines():
    e=e.strip()
    if ' - ' in e:
      e = e.rsplit(' - ',1)[1]
      if e.startswith('Iteration: '):
        iteration = e.split(':')[-1].strip()
        if iteration != '0':
          iterations.append(spentd)
          spentd = {}
          spentd['defects4j'] = defaultdict(dict)
          spentd['refOracle'] = defaultdict(dict)
      elif e.startswith('Started for'):
        caseinfo = e.split('for')[-1].strip()
        caseinfo = '/'.join(caseinfo.rsplit(' ',1))
        datas = defaultdict(int)
      elif e.startswith('Finished for '):
        dataset = 'refOracle' if caseinfo.startswith('https://github.com') else 'defects4j'
        spentd[dataset][caseinfo] = datas
      elif e.endswith('seconds') and 'current pass' not in e:
        stage,spent = e.split(':')
        spent = spent.strip().split(' ')[0]
        # if stage in RMD_stages:
        #   datas[stage] = int(spent)
        # else:
        datas[stage] += int(spent)

  iterations.append(spentd)

del(iterations[0])
iterations = iterations[:10]

print(len(iterations))

total_ms = 0

avg_overhead_list = {}
avg_overhead_list['defects4j'] = []
avg_overhead_list['refOracle'] = []

avg_fixing_time = {}
avg_fixing_time['defects4j'] = defaultdict(list)
avg_fixing_time['refOracle'] = defaultdict(list)

simnamemap={
  'RM-ASTDiff': 'RMD',
  'iASTMapper': 'IAM',
  'Diff_Auto_Tuning': 'DAT',
  'GumTree-simple': 'GTS',
  'GumTree-greedy': 'GTG'
}

def calc(name,stages):
  global total_ms
  results ={}

  overhead_list = {}
  overhead_list['defects4j'] = []
  overhead_list['refOracle'] = []

  for dataset in spentd:
    dd = spentd[dataset]
    for datas in dd.values():
      if stages[-1] in datas:
        break
      else:
        return
    
    item_len = len(dd)
    spent_times = []
    overhead_times = []
    overhead_ratios = []
    total_spent = 0
    total_overhead = 0


    for case,datas in dd.items():
      overhead_time = 0
      spent_time = 0
      for e in stages[:-1]:
        spent_time += datas[e]

      overhead_time = datas[stages[-1]]

      total_spent += spent_time
      total_overhead += overhead_time

      spent_times.append(spent_time)
      overhead_times.append(overhead_time)
      overhead_list[dataset].append((case,overhead_time))

      if spent_time == 0:
        overhead_ratios.append(0 if overhead_time==0 else 1)
      else:
        overhead_ratios.append(overhead_time/spent_time)

    results[dataset]=(total_spent,total_overhead,total_overhead/total_spent,np.median(overhead_times),total_overhead/item_len,sum(overhead_ratios)/item_len,np.std(overhead_times),max(overhead_times))
  return results,overhead_list

results_each = {}
results_each['defects4j'] = []
results_each['refOracle'] = []

def calc_iterations(name,stages):
  global spentd
  iteration_results = []
  overhead_lists = []
  for e in iterations:
    spentd = e
    results,overhead_list = calc(name,stages)
    iteration_results.append(results)
    overhead_lists.append(overhead_list)

  for dataset in ['defects4j','refOracle']:
    iteration_results_dataset = [e[dataset] for e in iteration_results]
    overhead_lists_dataset = [e[dataset] for e in overhead_lists]
    results = []
    for i in range(8):
      data_sum = 0
      for e in iteration_results_dataset:
        data_sum += e[i]
      results.append(data_sum/len(iterations))

    results_each[dataset].append((name,results))

    case_overhead = defaultdict(list)
    for overhead_list in overhead_lists_dataset:
      for case,overhead in overhead_list:
        case_overhead[case].append(overhead)

    for k,v in case_overhead.items():
      avg_overhead_list[dataset].append((k,sum(v)/len(v)))  
      avg_fixing_time[dataset][simnamemap[name]].append(sum(v)/len(v))
    
  
calc_iterations('RM-ASTDiff',RMD_stages)
calc_iterations('iASTMapper',IAM_stages)
calc_iterations('Diff_Auto_Tuning',DAT_stages)
calc_iterations('GumTree-simple',GTS_stages)
calc_iterations('GumTree-greedy',GTG_stages)

for dataset,resultslist in results_each.items():
  print('--------- '+dataset+' ---------')
  for name,results in resultslist:
    tm = results[0]
    tf = results[1]
    print(f'{tm} {tf} {results[3]:.2f} {results[4]:.2f} {tm/tf:.2f}x {tf/(tm+tf):.2%} {results[-1]} {results[-2]:.2f}')

# import json

# with open('avg_fixing_time.json','w',encoding='utf8') as f:
#   json.dump(avg_fixing_time,f)