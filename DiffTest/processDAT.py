import os,json
import pandas as pd
from pprint import pprint


os.chdir('../DAT_infos')

def filterByMinProp(j,prop,bound):
  mn = bound
  for e in j:
    if e[prop]!=None:
      mn = min(mn,e[prop])
  print(mn)
  r = [e for e in j if e[prop]==mn]
  return r

def selectByMinProp(l,prop):
  j.sort(key=lambda e:e[prop])
  conf = j[0]
  return conf

def changePropsToInt(l,props):
  for e in l:
    for p in props:
      if e[p]!=None:
        e[p] = int(e[p])

intProps = ['NRACTIONS','SIMACTIONS','TIME','bu_minsize']

for filepair in os.listdir('.'):
  print(filepair)
  for csvfile in os.listdir(filepair):
    if csvfile.endswith(".csv"):
      csv_data = pd.read_csv(f'{filepair}/{csvfile}', sep=",")
      j0 = json.loads(csv_data.to_json(orient="records"))
      j0 = list(j0)
      for e in j0:
        for p in e:
          if 'Unnamed' in p:
            del e[p]
            break
      changePropsToInt(j0,intProps)
      j = filterByMinProp(j0,'NRACTIONS',100000000)
      j = filterByMinProp(j,'SIMACTIONS',100000000)
      conf = selectByMinProp(j,'TIME')
      print(conf)
      with open(f'{filepair}/bestconf.json','w',encoding='utf8') as f:
        json.dump(conf,f)