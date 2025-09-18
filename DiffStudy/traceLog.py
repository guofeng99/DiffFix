from collections import defaultdict
from pprint import pprint
import re
import numpy as np

RMDfailed = []
RMDtimeout = []
DFXunmod = []
DFXmod = []


def traceLog(phaselogfile):
  phasecount = {}
  i=1

  with open(phaselogfile,'r',encoding='utf8') as f:
    caseinfo = None
    datas = set()
    for e in f.readlines():
      e=e.strip()
      if 'WARN' in e:
        RMDfailed.append(caseinfo)
        continue
      if 'Timeout' in e:
        RMDtimeout.append(caseinfo)
        continue
      if not '[main] INFO' in e:
        continue
      e = e.rsplit(' - ',1)[1]
      if e.startswith('Started for'):
        caseinfo = e.split('for')[-1].strip()
        print(i,caseinfo)
        i+=1
      elif e.startswith('Finished for'):
        if datas:
          DFXmod.append(caseinfo)
          phasecount[caseinfo]=datas
          datas = set()
        elif caseinfo not in RMDfailed and caseinfo not in RMDtimeout:
          DFXunmod.append(caseinfo)
      elif e.startswith('current pass:'):
        pat='current pass: (.*?), (remove|match)'
        mat=re.search(pat,e)
        if mat:
          datas.add(mat.groups()[0])

  return phasecount


def traceTimeout(phaselogfile):
  phasecount = {}
  i=1

  with open(phaselogfile,'r',encoding='utf8') as f:
    caseinfo = None
    datas = set()
    for e in f.readlines():
      e=e.strip()
      if 'WARN' in e:
        RMDfailed.append(caseinfo)
        continue
      if 'Timeout' in e:
        commit=e.split(':')[-1].strip()
        if commit in caseinfo and caseinfo not in RMDtimeout:
          RMDtimeout.append(caseinfo)
        continue
      if not '[main] INFO' in e:
        continue
      e = e.rsplit(' - ',1)[1]
      if e.startswith('Started for'):
        caseinfo = e.split('for')[-1].strip()
        # print(i,caseinfo)
        i+=1
      elif e.startswith('Finished for'):
        if datas:
          DFXmod.append(caseinfo)
          phasecount[caseinfo]=datas
          datas = set()
        elif caseinfo not in RMDfailed and caseinfo not in RMDtimeout:
          DFXunmod.append(caseinfo)

  return phasecount

# traceTimeout('info_recent_commits_timeout.log')

# print(len(RMDfailed),len(RMDtimeout)) # 2 102
# print(RMDfailed,RMDtimeout)
# with open('pass_commits.txt','w',encoding='utf8') as f:
#   for e in RMDfailed+RMDtimeout:
#     f.write(e+'\n')

phasecount = traceLog('info_recent_commits.log')
print(len(RMDfailed),len(RMDtimeout),len(DFXunmod),len(DFXmod))
print(sum([len(RMDfailed),len(RMDtimeout),len(DFXunmod),len(DFXmod)]))
# print(phasecount)
# print(DFXmod)

with open('fixed_commits.txt','w',encoding='utf8') as f:
  for e in DFXmod:
    f.write(e+'\n')