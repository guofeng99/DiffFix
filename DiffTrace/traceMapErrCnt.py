from collections import defaultdict
from pprint import pprint
import re

def calcErr(errlogfile):
  toolerrcount = {}
  toolerrcount['defects4j']={}
  toolerrcount['refOracle']={}

  totalerrcount = {}
  totalerrcount['defects4j']={}
  totalerrcount['refOracle']={}

  with open(errlogfile,'r',encoding='utf8') as f:
    dataset = None
    caseinfo = None
    for e in f.readlines():
      e=e.strip()
      if ' - ' in e:
        e = e.rsplit(' - ',1)[1]
        if e.startswith('Started for'):
          caseinfo = e.split('for')[-1].strip()
          dataset = 'refOracle' if caseinfo.startswith('https://github.com') else 'defects4j'
          errcount = {}
          errcount['total'] = {}
        elif e.startswith('Finished for '):
          if errcount:
            toolerrcount[dataset][caseinfo]=errcount
            for toolname in errcount['total']:
              if toolname not in totalerrcount[dataset]:
                totalerrcount[dataset][toolname] = defaultdict(int)
              totalerrcount[dataset][toolname]['mmcnt']+=errcount['total'][toolname]['mmcnt']
              totalerrcount[dataset][toolname]['amcnt']+=errcount['total'][toolname]['amcnt']
              totalerrcount[dataset][toolname]['wmcnt']+=errcount['total'][toolname]['wmcnt']
              totalerrcount[dataset][toolname]['mmmcnt']+=errcount['total'][toolname]['mmmcnt']
              totalerrcount[dataset][toolname]['ammcnt']+=errcount['total'][toolname]['ammcnt']
              totalerrcount[dataset][toolname]['wmmcnt']+=errcount['total'][toolname]['wmmcnt']
              totalerrcount[dataset][toolname]['othercnt']+=errcount['total'][toolname]['othercnt']
        elif e.startswith('file name:'):
          filename = e.split(':')[-1].strip()
          errcount[filename] = defaultdict(dict)
        elif e.startswith('tool name:') and 'missingMappingCnt' in e:
          pat='tool name: (.*?), missingMappingCnt: (.*?), arbitraryMappingCnt: (.*?), wrongMappingCnt: (.*?), missingMulMappingCnt: (.*?), arbitraryMulMappingCnt: (.*?), wrongMulMappingCnt: (.*?), otherCase: (.*?)$'
          mat=re.search(pat,e).groups()
          toolname = mat[0]
          mmcnt=int(mat[1])
          amcnt=int(mat[2])
          wmcnt=int(mat[3])
          mmmcnt=int(mat[4])
          ammcnt=int(mat[5])
          wmmcnt=int(mat[6])
          othercnt=int(mat[7])
          errcount[filename][toolname]['mmcnt']=mmcnt
          errcount[filename][toolname]['amcnt']=amcnt
          errcount[filename][toolname]['wmcnt']=wmcnt
          errcount[filename][toolname]['mmmcnt']=mmmcnt
          errcount[filename][toolname]['ammcnt']=ammcnt
          errcount[filename][toolname]['wmmcnt']=wmmcnt
          errcount[filename][toolname]['othercnt']=othercnt
          if toolname not in errcount['total']:
            errcount['total'][toolname] = defaultdict(int)
          errcount['total'][toolname]['mmcnt']+=mmcnt
          errcount['total'][toolname]['amcnt']+=amcnt
          errcount['total'][toolname]['wmcnt']+=wmcnt
          errcount['total'][toolname]['mmmcnt']+=mmmcnt
          errcount['total'][toolname]['ammcnt']+=ammcnt
          errcount['total'][toolname]['wmmcnt']+=wmmcnt
          errcount['total'][toolname]['othercnt']+=othercnt

  return toolerrcount,totalerrcount

def formatstr(s):
  s=s.replace('-','↓').replace('+','↑').replace('↑0.00%','EQUAL')
  return f'({s})'

def formatstr2(d):
  s=f'{d:+,}'
  return s.replace('-','–')


toolerrcount0,totalerrcount0=calcErr('info_Original.log')
toolerrcount1,totalerrcount1=calcErr('info_v2.37.log')
toolerrcount2,totalerrcount2=calcErr('info_MoveOpt.log')

def showErrCnt():
  for dataset in ['defects4j','refOracle']:
    print('----- '+dataset+' -----')
    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      mmcnt=totalerrcount0[dataset][toolname]['mmcnt']
      amcnt=totalerrcount0[dataset][toolname]['amcnt']
      wmcnt=totalerrcount0[dataset][toolname]['wmcnt']
      mmmcnt=totalerrcount0[dataset][toolname]['mmmcnt']
      ammcnt=totalerrcount0[dataset][toolname]['ammcnt']
      wmmcnt=totalerrcount0[dataset][toolname]['wmmcnt']
      othercnt=totalerrcount0[dataset][toolname]['othercnt']

      print(f'{toolname} {mmcnt+mmmcnt} {amcnt+ammcnt} {wmcnt+wmmcnt}')

      # print(f'{toolname} {mmcnt} {amcnt} {wmcnt} {mmmcnt} {ammcnt} {wmmcnt} {othercnt}')

# showErrCnt()

def showErrCntDelta():
  for dataset in ['defects4j','refOracle']:
    print('----- '+dataset+' -----')
    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      mmcnt=totalerrcount0[dataset][toolname]['mmcnt']
      amcnt=totalerrcount0[dataset][toolname]['amcnt']
      wmcnt=totalerrcount0[dataset][toolname]['wmcnt']
      mmmcnt=totalerrcount0[dataset][toolname]['mmmcnt']
      ammcnt=totalerrcount0[dataset][toolname]['ammcnt']
      wmmcnt=totalerrcount0[dataset][toolname]['wmmcnt']
      othercnt=totalerrcount0[dataset][toolname]['othercnt']

      mmcnt1=totalerrcount1[dataset][toolname+'_Mod']['mmcnt']
      amcnt1=totalerrcount1[dataset][toolname+'_Mod']['amcnt']
      wmcnt1=totalerrcount1[dataset][toolname+'_Mod']['wmcnt']
      mmmcnt1=totalerrcount1[dataset][toolname+'_Mod']['mmmcnt']
      ammcnt1=totalerrcount1[dataset][toolname+'_Mod']['ammcnt']
      wmmcnt1=totalerrcount1[dataset][toolname+'_Mod']['wmmcnt']
      othercnt1=totalerrcount1[dataset][toolname+'_Mod']['othercnt']


      mmcnt2=totalerrcount2[dataset][toolname+'_MT']['mmcnt']
      amcnt2=totalerrcount2[dataset][toolname+'_MT']['amcnt']
      wmcnt2=totalerrcount2[dataset][toolname+'_MT']['wmcnt']
      mmmcnt2=totalerrcount2[dataset][toolname+'_MT']['mmmcnt']
      ammcnt2=totalerrcount2[dataset][toolname+'_MT']['ammcnt']
      wmmcnt2=totalerrcount2[dataset][toolname+'_MT']['wmmcnt']
      othercnt2=totalerrcount2[dataset][toolname+'_MT']['othercnt']

      # print(f'{toolname} {formatstr2(mmcnt1-mmcnt)} {formatstr2(amcnt1-amcnt)} {formatstr2(wmcnt1-wmcnt)}')
      # print(f'{toolname} {formatstr2(mmcnt2-mmcnt)} {formatstr2(amcnt2-amcnt)} {formatstr2(wmcnt2-wmcnt)}')

      # print(f'{toolname} {formatstr2(mmcnt2+mmmcnt2-mmcnt-mmmcnt)} {formatstr2(amcnt2+ammcnt2-amcnt-ammcnt)} {formatstr2(wmcnt2+wmmcnt2-wmcnt-wmmcnt)}')
      # print(f'{toolname} {formatstr2(mmcnt1+mmmcnt1-mmcnt-mmmcnt)} {formatstr2(amcnt1+ammcnt1-amcnt-ammcnt)} {formatstr2(wmcnt1+wmmcnt1-wmcnt-wmmcnt)}')
      
      print(f'{mmcnt+mmmcnt:,}|{amcnt+ammcnt:,}|{wmcnt+wmmcnt:,}')
      print(f'{mmcnt1+mmmcnt1:,} ({formatstr2(mmcnt1+mmmcnt1-mmcnt-mmmcnt)})|{amcnt1+ammcnt1:,} ({formatstr2(amcnt1+ammcnt1-amcnt-ammcnt)})|{wmcnt1+wmmcnt1:,} ({formatstr2(wmcnt1+wmmcnt1-wmcnt-wmmcnt)})')

      # print(f'{toolname} {mmcnt2+mmmcnt2} ({formatstr2(mmcnt2+mmmcnt2-mmcnt-mmmcnt)}) {amcnt2+ammcnt2} ({formatstr2(amcnt2+ammcnt2-amcnt-ammcnt)}) {wmcnt2+wmmcnt2} ({formatstr2(wmcnt2+wmmcnt2-wmcnt-wmmcnt)})')
      # print(f'{toolname} {mmcnt1+mmmcnt1} ({formatstr2(mmcnt1+mmmcnt1-mmcnt-mmmcnt)}) {amcnt1+ammcnt1} ({formatstr2(amcnt1+ammcnt1-amcnt-ammcnt)}) {wmcnt1+wmmcnt1} ({formatstr2(wmcnt1+wmmcnt1-wmcnt-wmmcnt)})')

      # print(f'{toolname} {formatstr2(mmcnt1-mmcnt)} {formatstr2(amcnt1-amcnt)} {formatstr2(wmcnt1-wmcnt)} {formatstr2(mmmcnt1-mmmcnt)} {formatstr2(ammcnt1-ammcnt)} {formatstr2(wmmcnt1-wmmcnt)} {formatstr2(othercnt1-othercnt)}')
      # print(f'{toolname} {formatstr2(mmcnt2-mmcnt)} {formatstr2(amcnt2-amcnt)} {formatstr2(wmcnt2-wmcnt)} {formatstr2(mmmcnt2-mmmcnt)} {formatstr2(ammcnt2-ammcnt)} {formatstr2(wmmcnt2-wmmcnt)} {formatstr2(othercnt2-othercnt)}')

showErrCntDelta()

def getPerfectDiff():
  tag = {'defects4j':'defects4','refOracle':'refOracle4'}
  ver = 'v2.37'
  for dataset in ['defects4j','refOracle']:
    # print('----- '+dataset+' -----')
    cases=toolerrcount0[dataset].keys()

    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      errcases=[]
      for case in cases:
        errcnt=sum(toolerrcount0[dataset][case]['total'][toolname].values())
        if errcnt!=0:
          errcases.append(case)
      with open(f'out/{tag[dataset]}-miss-{toolname}.txt','w',encoding='utf8') as f:
        for case in errcases:
          f.write(case+'\n')

    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      errcases=[]
      for case in cases:
        errcnt=sum(toolerrcount1[dataset][case]['total'][toolname+'_Mod'].values())
        if errcnt!=0:
          errcases.append(case)
      with open(f'out/{tag[dataset]}-miss-{toolname}-{ver}.txt','w',encoding='utf8') as f:
        for case in errcases:
          f.write(case+'\n')

    for toolname in ['RMD', 'IAM', 'DAT', 'GTS', 'GTG']:
      errcases=[]
      for case in cases:
        errcnt=sum(toolerrcount2[dataset][case]['total'][toolname+'_MT'].values())
        if errcnt!=0:
          errcases.append(case)
      with open(f'out/{tag[dataset]}-miss-{toolname}-MoveOpt.txt','w',encoding='utf8') as f:
        for case in errcases:
          f.write(case+'\n')

# getPerfectDiff()