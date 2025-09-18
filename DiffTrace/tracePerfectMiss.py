from pprint import pprint

filepath = './out/'

full_names=dict()
class Oracle:
  def read2miss(self,filename):
    misslist=[]
    with open(filepath+filename,'r',encoding='utf8') as f:
      for e in f.readlines():
        full_name=e.strip()
        if 'github.com' in full_name:
          simple_name=full_name[19:-34].replace('.git/','/')
        else:
          simple_name=full_name
        if simple_name not in full_names:
          full_names[simple_name]=full_name
        misslist.append(simple_name)
    return misslist

  def __init__(self,name,toolname,total_cases,version='v2',lack=False):
    self.name=name
    self.toolname=toolname
    self.total_cases=total_cases
    self.old_stmt_miss=self.read2miss(f'{name}2-miss-{toolname}.txt')
    self.old_node_miss=self.read2miss(f'{name}4-miss-{toolname}.txt')
    if lack:
      self.new_stmt_miss=self.read2miss(f'{name}2-miss-{toolname}.txt')
      self.new_node_miss=self.read2miss(f'{name}4-miss-{toolname}.txt')
    else:
      self.new_stmt_miss=self.read2miss(f'{name}2-miss-{toolname}-{version}.txt')
      self.new_node_miss=self.read2miss(f'{name}4-miss-{toolname}-{version}.txt')

  def calc(self):
    kep_node = [e for e in self.old_node_miss if e in self.new_node_miss]
    add_node = [e for e in self.new_node_miss if e not in self.old_node_miss]
    self.add_node = add_node
    del_node = [e for e in self.old_node_miss if e not in self.new_node_miss]
    self.del_node = del_node
    kep_stmt = [e for e in self.old_stmt_miss if e in self.new_stmt_miss]
    add_stmt = [e for e in self.new_stmt_miss if e not in self.old_stmt_miss]
    self.add_stmt = add_stmt
    del_stmt = [e for e in self.old_stmt_miss if e not in self.new_stmt_miss]
    self.del_stmt = del_stmt
    old_not_stmt = [e for e in self.old_node_miss if e not in self.old_stmt_miss and e not in self.new_stmt_miss]
    new_not_stmt = [e for e in self.new_node_miss if e not in self.new_stmt_miss and e not in self.old_stmt_miss]
    not_stmt = []
    for e in sorted(old_not_stmt+new_not_stmt):
      if e not in not_stmt:not_stmt.append(e)
    all_node = kep_node+add_node+del_node
    all_node2 = kep_stmt+add_stmt+del_stmt+not_stmt
    
    assert(len(all_node)==len(all_node2))
    assert(len(kep_node)+len(del_node)==len(self.old_node_miss))
    assert(len(kep_node)+len(add_node)==len(self.new_node_miss))
    node_classes = {'kep_node':kep_node,'add_node':add_node,'del_node':del_node}
    stmt_classes = {'kep_stmt':kep_stmt,'add_stmt':add_stmt,'del_stmt':del_stmt,'not_stmt':not_stmt}
    self.res=dict()
    for node_class in node_classes.items():
      for stmt_class in stmt_classes.items():
        self.res[node_class[0]+'_'+stmt_class[0]]=[e for e in node_class[1] if e in stmt_class[1]]
    count = 0
    for e in self.res:
      count += len(self.res[e])
    assert(count==len(all_node))
    assert(len(self.res['del_node_kep_stmt'])+len(self.res['del_node_add_stmt'])+len(self.res['add_node_del_stmt'])==0)

  def present(self):
    old_stmt_miss=len(self.old_stmt_miss)
    old_node_miss=len(self.old_node_miss)
    new_stmt_miss=len(self.new_stmt_miss)
    new_node_miss=len(self.new_node_miss)
    total_cases=self.total_cases
    name='defect4j' if self.name=='defects' else self.name
    assert(old_stmt_miss+len(self.add_stmt)==new_stmt_miss+len(self.del_stmt))
    assert(old_node_miss+len(self.add_node)==new_node_miss+len(self.del_node))

    return (1-old_stmt_miss/total_cases,1-new_stmt_miss/total_cases,len(self.del_stmt),len(self.add_stmt),1-old_node_miss/total_cases,1-new_node_miss/total_cases,len(self.del_node),len(self.add_node))


fullname = {
  'RMD' : 'RM-ASTDiff +DiffFix',
  'RMD-rev' : 'RM-ASTDiff +DiffFix (rev oracle)',
  'GTS' : 'GumTree-simple +DiffFix',
  'GTS-rev' : 'GumTree-simple +DiffFix (rev oracle)',
  'GTG' : 'GumTree-greedy +DiffFix',
  'GTG-rev' : 'GumTree-greedy +DiffFix (rev oracle)',
  'IAM' : 'iASTMapper +DiffFix',
  'IAM-rev' : 'iASTMapper +DiffFix (rev oracle)',
  'DAT' : 'Diff Auto Tuning +DiffFix',
  'DAT-rev' : 'Diff Auto Tuning +DiffFix (rev oracle)'
}

def formatstr(s):
  if type(s) != str:
    s=f'{s:.2%}'
  s=s.replace('-','↓').replace('+','↑').replace('↑0.00%','EQUAL')
  return f'({s})'


version = 'v2.37'

print('--------- defects4j ---------')

for toolname in ['RMD','IAM','DAT','GTS','GTG']:
  defects4j=Oracle('defects',toolname,800,version)
  defects4j.calc()
  stmt0,stmt1,stmtdel,stmtadd,node0,node1,nodedel,nodeadd = defects4j.present()
  s1=f'{(stmtdel-stmtadd)/800:+.2%}'
  s1=formatstr(s1)
  s2=f'{(nodedel-nodeadd)/800:+.2%}'
  s2=formatstr(s2)
  print(f'{stmt1:.2%} {s1},{stmtdel},{stmtadd},{node1:.2%} {s2},{nodedel},{nodeadd}')


print('--------- refOracle ---------')

for toolname in ['RMD','IAM','DAT','GTS','GTG']:

  refOracle=Oracle('refOracle',toolname,187,version)
  refOracle.calc()
  stmt0,stmt1,stmtdel,stmtadd,node0,node1,nodedel,nodeadd = refOracle.present()
  s1=f'{(stmtdel-stmtadd)/187:+.2%}'
  s1=formatstr(s1)
  s2=f'{(nodedel-nodeadd)/187:+.2%}'
  s2=formatstr(s2)
  print(f'{stmt1:.2%} {s1},{stmtdel},{stmtadd},{node1:.2%} {s2},{nodedel},{nodeadd}')

print()

print('--------- defects4j ---------')

for toolname in ['RMD','IAM','DAT','GTS','GTG']:
  defects4j=Oracle('defects',toolname,800,version)
  defects4j.calc()
  stmt0,stmt1,stmtdel,stmtadd,node0,node1,nodedel,nodeadd = defects4j.present()
  s1=f'{(stmtdel-stmtadd)/800:+.2%}'
  s2=f'{(nodedel-nodeadd)/800:+.2%}'
  print(f'{stmt0:.2%},{node0:.2%}'.replace('%',''))
  print(f'{stmt1:.2%} ({s1}),{node1:.2%} ({s2})'.replace('%',''))

print('--------- refOracle ---------')

for toolname in ['RMD','IAM','DAT','GTS','GTG']:
  refOracle=Oracle('refOracle',toolname,187,version)
  refOracle.calc()
  stmt0,stmt1,stmtdel,stmtadd,node0,node1,nodedel,nodeadd = refOracle.present()
  s1=f'{(stmtdel-stmtadd)/187:+.2%}'
  s2=f'{(nodedel-nodeadd)/187:+.2%}'
  print(f'{stmt0:.2%},{node0:.2%}'.replace('%',''))
  print(f'{stmt1:.2%} ({s1}),{node1:.2%} ({s2})'.replace('%',''))