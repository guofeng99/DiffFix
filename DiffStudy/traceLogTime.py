from collections import defaultdict
import numpy as np

iterations = []
spentd = {}

RMD_stages = ['ModelDiff execution (part1)','ModelDiff execution (part2)','Diff execution','MovedDiff execution','Fixing execution']

log_difffix='info_recent_commits_r12.log'

itercnt = 0

with open(log_difffix,'r',encoding='utf8') as f:
  caseinfo = None
  datas = None

  for e in f.readlines():
    e=e.strip()
    if ' - ' in e:
      e = e.rsplit(' - ',1)[1]
      if e.startswith('Started for'):
        caseinfo = e.split('for')[-1].strip()
        if caseinfo == 'airbnb_lottie-android/018425f68aaf':
          if itercnt!=0:
            iterations.append(spentd)
            spentd = {}
          itercnt+=1
        datas = defaultdict(int)
      elif e.startswith('Finished for '):
        spentd[caseinfo] = datas
      elif e.endswith('seconds') and 'current pass' not in e:
        stage,spent = e.split(':')
        spent = spent.strip().split(' ')[0]
        datas[stage] += int(spent)

  iterations.append(spentd)

del(iterations[0])
iterations = iterations[:10]

print(len(iterations))

total_ms = 0

avg_overhead_list = []

def calc(name):
  global total_ms
  results ={}

  overhead_list = []

  for datas in spentd.values():
    if RMD_stages[-1] in datas:
      break
    else:
      return
  
  item_len = len(spentd)
  spent_times = []
  overhead_times = []
  overhead_ratios = []
  total_spent = 0
  total_overhead = 0


  for case,datas in spentd.items():
    overhead_time = 0
    spent_time = 0
    for e in RMD_stages[:-1]:
      spent_time += datas[e]

    overhead_time = datas[RMD_stages[-1]]

    total_spent += spent_time
    total_overhead += overhead_time

    spent_times.append(spent_time)
    overhead_times.append(overhead_time)
    overhead_list.append((case,overhead_time))

    if spent_time == 0:
      overhead_ratios.append(0 if overhead_time==0 else 1)
    else:
      overhead_ratios.append(overhead_time/spent_time)

  results=(total_spent,total_overhead,total_overhead/total_spent,np.median(overhead_times),total_overhead/item_len,sum(overhead_ratios)/item_len,np.std(overhead_times),max(overhead_times))
  return results,overhead_list

results_each = []

def calc_iterations(name):
  global spentd
  iteration_results = []
  overhead_lists = []
  for e in iterations:
    spentd = e
    results,overhead_list = calc(name)
    iteration_results.append(results)
    overhead_lists.append(overhead_list)

  results = []
  for i in range(8):
    data_sum = 0
    for e in iteration_results:
      data_sum += e[i]
    results.append(data_sum/len(iterations))

  results_each.append((name,results))

  case_overhead = defaultdict(list)
  for overhead_list in overhead_lists:
    for case,overhead in overhead_list:
      case_overhead[case].append(overhead)

  for k,v in case_overhead.items():
    avg_overhead_list.append((k,sum(v)/len(v)))    
    
  
calc_iterations('RM-ASTDiff')

for name,results in results_each:
  # print(f'{name} {dataset} {len(spentd[dataset])}')
  # print(f'{results[0]},{results[1]},{results[2]:.2%}',end=',')
  # print(f'{results[3]:.2f},{results[4]:.2f},{results[5]:.2%}')
  tm = results[0]
  tf = results[1]
  print(f'{tm} {tf} {results[3]:.2f} {results[4]:.2f} {tm/tf:.2f}x {tf/(tm+tf):.2%} {results[-1]} {results[-2]:.2f}')
  # print(f'{results[4]:.2f}')