import os
from collections import defaultdict

vd = defaultdict(dict)
id = defaultdict(list)

for e in os.listdir('verified_results'):
  if e.endswith('.txt'):
    name = e[:-4]
    with open('verified_results/'+e,'r',encoding='utf8') as f:
      for l in f.readlines():
        a,b = l.strip().split(' ')
        assert(b in ['1','2','3'])
        if b not in vd[name]:
          vd[name][b]=[]
        vd[name][b].append(a)
        id[a+':'+b].append(name)

succeed = []
failed = []

for name,v in vd.items():
  if '2' in v or '3' in v:
    failed.append(name)
    if '3' in v and 'AS' in v['3']:
      # print('/'.join(name.split('.')[1].rsplit('_',1)))
      print('compare("'+'/'.join(name.split('.')[1].rsplit('_',1))+'");')
  else:
    succeed.append(name)

la= len(vd)
ls = len(succeed)
lf = len(failed)
print(la,ls,lf)
print(f'{ls/la:.2%},{lf/la:.2%}')

cid = [(a,len(b)) for a,b in id.items()]
cid.sort(key = lambda e:e[1],reverse=True)

type1 = []
type2 = []
type3 = []

for a,b in cid:
  n,c = a.split(':')
  if c=='1':
    type1.append((n,b))
  elif c=='2':
    type2.append((n,b))
  else:
    type3.append((n,b))

def process(tp):
  for a,b in tp:
    print(f'{a}: {b}',end=', ')
  print()

process(type1)
process(type2)
process(type3)

'''
*Type1* MS: 181, ME: 51, MD: 26, WS: 24,
AE: 11, MT: 8, WT: 3, MB: 2, WE: 2, WMM: 1, 
*Type2* WMM: 4, ME: 1, WS: 1, WE: 1, AE: 1, MD: 1, 
*Type3* AS: 9, AT: 2, MD: 1, AE: 1, MS: 1,
'''

