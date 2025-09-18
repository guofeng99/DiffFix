from collections import defaultdict
from pprint import pprint
import re

def calcES(eslogfile):
  toolescount = {}
  toolescount['defects4j']={}
  toolescount['refOracle']={}

  totalescount = {}
  totalescount['defects4j']={}
  totalescount['refOracle']={}

  with open(eslogfile,'r',encoding='utf8') as f:
    dataset = None
    caseinfo = None
    for e in f.readlines():
      e=e.strip()
      if ' - ' in e:
        e = e.rsplit(' - ',1)[1]
        if e.startswith('Started for'):
          caseinfo = e.split('for')[-1].strip()
          dataset = 'refOracle' if caseinfo.startswith('https://github.com') else 'defects4j'
          escount = {}
          escount['total'] = {}
          # filename=0
        elif e.startswith('Finished for '):
          if escount:
            toolescount[dataset][caseinfo]=escount
            for toolname in escount['total']:
              if toolname not in totalescount[dataset]:
                totalescount[dataset][toolname] = defaultdict(int)
              totalescount[dataset][toolname]['essize']+=escount['total'][toolname]['essize']
              totalescount[dataset][toolname]['addcnt']+=escount['total'][toolname]['addcnt']
              totalescount[dataset][toolname]['delcnt']+=escount['total'][toolname]['delcnt']
              totalescount[dataset][toolname]['updcnt']+=escount['total'][toolname]['updcnt']
              totalescount[dataset][toolname]['movcnt']+=escount['total'][toolname]['movcnt']
              totalescount[dataset][toolname]['mulmovcnt']+=escount['total'][toolname]['mulmovcnt']
              totalescount[dataset][toolname]['movoutcnt']+=escount['total'][toolname]['movoutcnt']
              totalescount[dataset][toolname]['movincnt']+=escount['total'][toolname]['movincnt']
        elif e.startswith('file name:'):
          filename = e.split(':')[-1].strip()
          escount[filename] = defaultdict(dict)
        elif e.startswith('tool name:') and 'es size' in e:
          pat='tool name: (.*?), es size: (.*?), inscnt: (.*?), delcnt: (.*?), repcnt: (.*?), movcnt: (.*?), mulmovcnt: (.*?), movoutcnt: (.*?), movincnt: (.*?)$'
          mat=re.search(pat,e).groups()
          toolname = mat[0]
          essize=int(mat[1])
          inscnt=int(mat[2])
          delcnt=int(mat[3])
          repcnt=int(mat[4])
          movcnt=int(mat[5])
          mulmovcnt=int(mat[6])
          movoutcnt=int(mat[7])
          movincnt=int(mat[8])
          escount[filename][toolname]['essize']=essize
          escount[filename][toolname]['addcnt']=inscnt
          escount[filename][toolname]['delcnt']=delcnt
          escount[filename][toolname]['updcnt']=repcnt
          escount[filename][toolname]['movcnt']=movcnt
          escount[filename][toolname]['mulmovcnt']=mulmovcnt
          escount[filename][toolname]['movoutcnt']=movoutcnt
          escount[filename][toolname]['movincnt']=movincnt
          if toolname not in escount['total']:
            escount['total'][toolname] = defaultdict(int)
          escount['total'][toolname]['essize']+=essize
          escount['total'][toolname]['addcnt']+=inscnt
          escount['total'][toolname]['delcnt']+=delcnt
          escount['total'][toolname]['updcnt']+=repcnt
          escount['total'][toolname]['movcnt']+=movcnt
          escount['total'][toolname]['mulmovcnt']+=mulmovcnt
          escount['total'][toolname]['movoutcnt']+=movoutcnt
          escount['total'][toolname]['movincnt']+=movincnt

  return toolescount,totalescount

toolescountGOD,totalescountGOD=calcES('info_GOD.log')
toolescount0,totalescount0=calcES('info_Original.log')
toolescount1,totalescount1=calcES('info_v2.37.log')
toolescount2,totalescount2=calcES('info_MoveOpt.log')

def formatstr(s):
  s=s.replace('-','↓').replace('+','↑').replace('↑0.00%','EQUAL')
  return f'({s})'

def formatstr2(d):
  s=f'{d:+,}'
  return s.replace('-','–')


def compareESSize():
  for dataset in ['defects4j','refOracle']:
    print('----- '+dataset+' -----')
    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      es0=totalescount0[dataset][toolname]['essize']-totalescount0[dataset][toolname]['movincnt']
      es1=totalescount1[dataset][toolname+'_Mod']['essize']-totalescount1[dataset][toolname+'_Mod']['movincnt']
      es2=totalescount2[dataset][toolname+'_MT']['essize']-totalescount2[dataset][toolname+'_MT']['movincnt']
      s1=f'{(es1-es0)/es0:+.2%}'
      s1=formatstr(s1)
      s2=f'{(es2-es0)/es0:+.2%}'
      s2=formatstr(s2)
      # print(f'{es0} {es1} {s1} {es2} {s2}')
      # print(f'{es0}')
      print(f'{es0:,}')
      # print(f'{es1:,} ({es1-es0:+,})')
      print(f'{es1:,} {s1}')
      # print(f'{es1:,} {s1}')
      # print(f'{es2:,} {s2}')

# compareESSize()

def compareActionDelta():
  for dataset in ['defects4j','refOracle']:
    print('----- '+dataset+' -----')
    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      es1add=totalescount1[dataset][toolname+'_Mod']['addcnt']-totalescount0[dataset][toolname]['addcnt']
      es2add=totalescount2[dataset][toolname+'_MT']['addcnt']-totalescount0[dataset][toolname]['addcnt']
      es1del=totalescount1[dataset][toolname+'_Mod']['delcnt']-totalescount0[dataset][toolname]['delcnt']
      es2del=totalescount2[dataset][toolname+'_MT']['delcnt']-totalescount0[dataset][toolname]['delcnt']
      es1upd=totalescount1[dataset][toolname+'_Mod']['updcnt']-totalescount0[dataset][toolname]['updcnt']
      es2upd=totalescount2[dataset][toolname+'_MT']['updcnt']-totalescount0[dataset][toolname]['updcnt']
      es0mov=totalescount0[dataset][toolname]['essize']-totalescount0[dataset][toolname]['addcnt']-totalescount0[dataset][toolname]['delcnt']-totalescount0[dataset][toolname]['updcnt']
      es1mov=totalescount1[dataset][toolname+'_Mod']['essize']-totalescount1[dataset][toolname+'_Mod']['addcnt']-totalescount1[dataset][toolname+'_Mod']['delcnt']-totalescount1[dataset][toolname+'_Mod']['updcnt']
      es2mov=totalescount2[dataset][toolname+'_MT']['essize']-totalescount2[dataset][toolname+'_MT']['addcnt']-totalescount2[dataset][toolname+'_MT']['delcnt']-totalescount2[dataset][toolname+'_MT']['updcnt']
      es1mov-=es0mov
      es2mov-=es0mov
      print(f'{formatstr2(es1add)} {formatstr2(es1del)} {formatstr2(es1upd)} {formatstr2(es1mov)}')
      print(f'{formatstr2(es2add)} {formatstr2(es2del)} {formatstr2(es2upd)} {formatstr2(es2mov)}')

def compareActionDelta2():
  dfdeltas = {}
  mtdeltas = {}
  for dataset in ['defects4j','refOracle']:
    print('----- '+dataset+' -----')
    dfdelta = defaultdict(list)
    mtdelta = defaultdict(list)
    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      dfinc=dfdec=mtinc=mtdec=0
      for caseinfo in toolescount0[dataset]:
        es1cnt=toolescount1[dataset][caseinfo]['total'][toolname+'_Mod']['essize']-toolescount0[dataset][caseinfo]['total'][toolname]['essize']
        es2cnt=toolescount2[dataset][caseinfo]['total'][toolname+'_MT']['essize']-toolescount0[dataset][caseinfo]['total'][toolname]['essize']
        if es1cnt:
          dfdelta[toolname].append(es1cnt)
          if es1cnt<0:
            dfdec+=1
          else:
            dfinc+=1
        if es2cnt:
          mtdelta[toolname].append(es2cnt)
          if es2cnt<0:
            mtdec+=1
          else:
            mtinc+=1
      print(dfinc,dfdec)
      print(mtinc,mtdec)
    dfdeltas[dataset]=dfdelta
    mtdeltas[dataset]=mtdelta
  return dfdeltas,mtdeltas
    
# compareActionDelta2()

def classifyFiles():
    for dataset in ['defects4j','refOracle']:
      CFMs = []
      MMs = []
      AMs = []
      print('----- '+dataset+' -----')
      CCFM = 0
      CMM = 0
      CAM = 0
      for caseinfo,escount in toolescountGOD[dataset].items():
        hasCFM = False
        hasMM = False
        for filename,fileescounts in escount.items():
          if filename=='total':
            continue
          fileescount = fileescounts['GOD']
          if filename.endswith('.java'):
            filename = filename[:-5]
          filename = filename.replace('/','.')
          
          AMs.append(f'{caseinfo}: {filename}')

          if fileescount['movincnt']>0 or fileescount['movoutcnt']>0:
            # print(f'{caseinfo}: cross file mappings in {filename}')
            CFMs.append(f'{caseinfo}: {filename}')
            hasCFM = True
          
          if fileescount['mulmovcnt']>0:
            # print(f'{caseinfo}: multiple mappings in {filename}')
            MMs.append(f'{caseinfo}: {filename}')
            hasMM = True
        if hasCFM: CCFM += 1
        if hasMM: CMM += 1
        if hasCFM or hasMM: CAM+=1


      print(f'CCFM:{CCFM} CMM:{CMM} CAM:{CAM}')
    
      # with open(f'{dataset}-Files.txt','w',encoding='utf8') as f:
      #   for e in AMs: f.write(e+'\n')

      # a1=0
      # with open(f'{dataset}-Files_wo_CFM.txt','w',encoding='utf8') as f:
      #   for e in AMs:~
      #     if e not in CFMs:
      #       a1+=1
      #       f.write(e+'\n')

      # a2=0
      # with open(f'{dataset}-Files_wo_CFM_MM.txt','w',encoding='utf8') as f:
      #   for e in AMs:
      #     if e not in CFMs and e not in MMs:
      #       a2+=1
      #       f.write(e+'\n')

      # with open(f'{dataset}-CFMs.txt','w',encoding='utf8') as f:
      #   for e in CFMs: f.write(e+'\n')
      
      # with open(f'{dataset}-MMs.txt','w',encoding='utf8') as f:
      #   for e in MMs: f.write(e+'\n')

      # print(f'Dataset: {dataset}, file pair size: {len(AMs)}, file pair size (w/o CFM): {a1}, file pair size (w/o CFM,MM): {a2}, CFMs size: {len(CFMs)}, MMs size: {len(MMs)}')

compareESSize()
compareActionDelta()
compareActionDelta2()

# classifyFiles()