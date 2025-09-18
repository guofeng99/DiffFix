commits = []
with open('fixed_commits.txt','r',encoding='utf8') as f:
  commits = f.read().strip().split('\n')

from collections import defaultdict
from random import choice

commitsd = defaultdict(list)

for e in commits:
  a,b = e.split('/')
  commitsd[a].append(e)

# print(len(commitsd))
  
random_commits = []

while len(random_commits)<320:
  for a,b in commitsd.items():
    if len(random_commits)==320:
      break
    if b:
      c = choice(b)
      b.remove(c)
      random_commits.append(c)

print(random_commits)

with open('random_commits.txt','w',encoding='utf8') as f:
  for e in random_commits:
    f.write(e+'\n')