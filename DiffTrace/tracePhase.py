from collections import defaultdict
from pprint import pprint
import re
import numpy as np

def tracePhase(phaselogfile):
  toolphasecount = {}
  toolphasecount['defects4j']={}
  toolphasecount['refOracle']={}

  totalphasecount = {}
  totalphasecount['defects4j']={}
  totalphasecount['refOracle']={}

  with open(phaselogfile,'r',encoding='utf8') as f:
    dataset = None
    caseinfo = None
    datas = None
    for e in f.readlines():
      e=e.strip()
      if ' - ' in e:
        e = e.rsplit(' - ',1)[1]
        if e.startswith('Started for'):
          caseinfo = e.split('for')[-1].strip()
          dataset = 'refOracle' if caseinfo.startswith('https://github.com') else 'defects4j'
          datas = set()
          toolphasecount[dataset][caseinfo] = defaultdict(dict)
        elif 'Fixing execution' in e:
          if 'iASTMapper Fixing execution' in e:
            toolname = 'IAM_Mod'
          elif 'Diff_Auto_Tuning Fixing execution' in e:
            toolname = 'DAT_Mod'
          elif 'GumTree-simple Fixing execution' in e:
            toolname = 'GTS_Mod'
          elif 'GumTree-greedy Fixing execution' in e:
            toolname = 'GTG_Mod'
          else:
            toolname = 'RMD_Mod'
          if datas:
            toolphasecount[dataset][caseinfo][toolname]=datas
            for phase in datas:
              if toolname not in totalphasecount[dataset]:
                totalphasecount[dataset][toolname] = defaultdict(list)
              totalphasecount[dataset][toolname][phase].append(caseinfo)
          datas = set()
        elif e.startswith('current pass:'):
          pat='current pass: (.*?), (remove|match)'
          mat=re.search(pat,e)
          if mat:
            datas.add(mat.groups()[0])

  return toolphasecount,totalphasecount

def tracePhaseTime(phaselogfile):
  iterations1 = []
  iterations = []

  toolphasetime = {}
  totalphasetime = {}
  totalphasetime['defects4j']={}
  totalphasetime['refOracle']={}

  with open(phaselogfile,'r',encoding='utf8') as f:
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
            iterations.append(totalphasetime)
            totalphasetime = {}
            totalphasetime['defects4j']={}
            totalphasetime['refOracle']={}
        elif e.startswith('Started for'):
          caseinfo = e.split('for')[-1].strip()
          caseinfo = '/'.join(caseinfo.rsplit(' ',1))
          dataset = 'refOracle' if caseinfo.startswith('https://github.com') else 'defects4j'
          datas = {}
        elif 'Fixing execution' in e:
          if 'iASTMapper Fixing execution' in e:
            toolname = 'IAM_Mod'
          elif 'Diff_Auto_Tuning Fixing execution' in e:
            toolname = 'DAT_Mod'
          elif 'GumTree-simple Fixing execution' in e:
            toolname = 'GTS_Mod'
          elif 'GumTree-greedy Fixing execution' in e:
            toolname = 'GTG_Mod'
          else:
            toolname = 'RMD_Mod'
          if datas:
            for phase in datas:
              if toolname not in totalphasetime[dataset]:
                totalphasetime[dataset][toolname] = defaultdict(int)
              totalphasetime[dataset][toolname][phase]+=datas[phase]
          datas = {}
        elif e.endswith('seconds') and e.startswith('current pass:'):
          pat='current pass: (.*?), execution: (.*?) milliseconds'
          mat=re.search(pat,e)
          if mat:
            a,b=mat.groups()
            datas[a]=int(b)

    iterations.append(totalphasetime)

  del(iterations[0])
  iterations = iterations[:10]

  totalphasetime = {}
  totalphasetime['defects4j']={}
  totalphasetime['refOracle']={}

  for iteration in iterations:
    for dataset,datasetphasetime in iteration.items():
      for toolname,toolphasetime in datasetphasetime.items():
        if toolname not in totalphasetime[dataset]:
          totalphasetime[dataset][toolname]=defaultdict(list)
        for phase,time in toolphasetime.items():
          totalphasetime[dataset][toolname][phase].append(time)

  for dataset,datasetphasetime in totalphasetime.items():
    for toolname,toolphasetime in datasetphasetime.items():
      for phase,times in toolphasetime.items():
        totalphasetime[dataset][toolname][phase]=np.mean(times)

  return toolphasetime,totalphasetime


toolphasecount,totalphasecount=tracePhase('info_phases_count_v2.37.log')
toolphasetime,totalphasetime=tracePhaseTime('info_phases_time_v2.37.log')

phases=['WarmUp','MatchByUnique','FixByChildren','FixByParent','FixByInner','FixByNearby','MatchByInner','MatchByNearby','MatchByChildren','MatchByParent']

tools=['RMD_Mod','IAM_Mod','DAT_Mod','GTS_Mod','GTG_Mod']

def countPhase():
  for dataset,datasetphasecount in totalphasecount.items():
    for toolname in tools:
      toolphasecount=datasetphasecount[toolname]
      for p in phases:
        print(len(toolphasecount[p]),end=',')
      print()

countPhase()

def timePhase():
  for dataset,datasetphasetime in totalphasetime.items():
    for toolname in tools:
      toolphasetime=datasetphasetime[toolname]
      for p in phases:
        print(f'{toolphasetime[p]:.1f}',end=',')
      print()
      
timePhase()