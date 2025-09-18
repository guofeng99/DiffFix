from collections import defaultdict
import numpy as np

RMD_stages = ['ModelDiff execution (part1)','ModelDiff execution (part2)','Diff execution','MovedDiff execution','Fixing execution']
IAM_stages = ['iASTMapper execution','iASTMapper Fixing execution']
DAT_stages = ['Diff_Auto_Tuning execution','Diff_Auto_Tuning Fixing execution']
GTS_stages = ['GumTree-simple execution','GumTree-simple Fixing execution']
GTG_stages = ['GumTree-greedy execution','GumTree-greedy Fixing execution']

def readlog(optLevel):
  iterations = []
  spentd = {}
  spentd['defects4j'] = defaultdict(dict)
  spentd['refOracle'] = defaultdict(dict)

  if optLevel<2:
    logfile = 'info_v2.37_opt0,1_r12.log'
  elif optLevel==2:
    logfile = 'info_v2.37_opt12_r12.log'
  else:
    logfile = 'info_v2.37_r12.log'

  with open(logfile,'r',encoding='utf8') as f:
    dataset = None
    caseinfo = None
    datas = None
    hasMeet = False
    readUntilOpt = False
    for e in f.readlines():
      e=e.strip()
      if ' - ' in e:
        e = e.rsplit(' - ',1)[1]
        if readUntilOpt and not e.startswith('OptLevel: '):
          continue
        if e.startswith('OptLevel: '):
          optLevel_= int(e.split(':')[-1].strip())
          if optLevel_ != optLevel:
            if hasMeet:
              break
            else:
              readUntilOpt = True
              continue
          else:
            hasMeet = True
            readUntilOpt = False
        elif e.startswith('Iteration: '):
          iteration = e.split(':')[-1].strip()
          if iteration != '0':
            iterations.append(spentd)
            spentd = {}
            spentd['defects4j'] = defaultdict(dict)
            spentd['refOracle'] = defaultdict(dict)
        elif e.startswith('Started for'):
          caseinfo = e.split('for')[-1].strip()
          caseinfo = '/'.join(caseinfo.rsplit(' ',1))
          dataset = 'refOracle' if caseinfo.startswith('https://github.com') else 'defects4j'
          datas = defaultdict(int)
        elif e.startswith('Finished for '):
          spentd[dataset][caseinfo] = datas
        elif e.endswith('milliseconds') and 'current pass' not in e:
          stage,spent = e.split(':')
          spent = spent.strip().split(' ')[0]
          datas[stage] += int(spent)

    iterations.append(spentd)

  del(iterations[0])
  iterations = iterations[:10]

  return iterations

def calc(stages,spentd):
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

    # results[dataset]=(total_spent,total_overhead,total_overhead/total_spent,total_spent/item_len,total_overhead/item_len,sum(overhead_ratios)/item_len)
    results[dataset]=(total_spent,total_overhead,total_overhead/total_spent,np.median(overhead_times),total_overhead/item_len,sum(overhead_ratios)/item_len)
  
  return results,overhead_list

avg_overhead_list = {}
avg_overhead_list['defects4j'] = []
avg_overhead_list['refOracle'] = []

def calc_iterations(name,stages,iterations):
  results_each = {}

  iteration_results = []
  overhead_lists = []

  for spentd in iterations:
    results,overhead_list = calc(stages,spentd)
    iteration_results.append(results)
    overhead_lists.append(overhead_list)

  for dataset in ['defects4j','refOracle']:
    iteration_results_dataset = [e[dataset] for e in iteration_results]
    results = []
    for i in range(len(iteration_results_dataset[0])):
      data_sum = 0
      for e in iteration_results_dataset:
        data_sum += e[i]
      results.append(data_sum/len(iteration_results_dataset))

    results_each[dataset]=results

  return results_each

stagesmap={
  'RM-ASTDiff':RMD_stages,
  'iASTMapper':IAM_stages,
  'Diff_Auto_Tuning':DAT_stages,
  'GumTree-simple':GTS_stages,
  'GumTree-greedy':GTG_stages
}

simnamemap={
  'RM-ASTDiff': 'RMD',
  'iASTMapper': 'IAM',
  'Diff_Auto_Tuning': 'DAT',
  'GumTree-simple': 'GTS',
  'GumTree-greedy': 'GTG'
}

for dataset in ['defects4j','refOracle']:
  print('--------- '+dataset+' ---------')
  for tool in ['RM-ASTDiff','iASTMapper','Diff_Auto_Tuning','GumTree-simple','GumTree-greedy']:
    for i in [0,1,2,3]:
      iterations=readlog(i)
      results_each=calc_iterations(tool,stagesmap[tool],iterations)
      results = results_each[dataset]
      print(f'{results[4]:.2f}',end=' ')
    print()

# iterations=readlog(3)
# for dataset in ['defects4j','refOracle']:
#   print('--------- '+dataset+' ---------')
#   for tool in ['RM-ASTDiff','iASTMapper','Diff_Auto_Tuning','GumTree-simple','GumTree-greedy']:
#     results_each=calc_iterations(tool,stagesmap[tool],iterations)
#     results=results_each[dataset]
#     tm = results[0]
#     tf = results[1]
#     print(f'{tm} {tf} {results[3]:.2f} {results[4]:.2f} {tf/tm:.2%} {tf/(tm+tf):.2%}')