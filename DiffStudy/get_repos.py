import requests
import json
import time

GITHUB_TOKEN = 'xxx'
HEADERS = {'Authorization': f'token {GITHUB_TOKEN}'} if GITHUB_TOKEN else {}

BASE_URL = 'https://api.github.com/search/repositories'
QUERY = 'language:Java'
SORT = 'stars'
ORDER = 'desc'
PER_PAGE = 100

results = []

for page in range(1, 3):
    params = {
        'q': QUERY,
        'sort': SORT,
        'order': ORDER,
        'per_page': PER_PAGE,
        'page': page
    }
    response = requests.get(BASE_URL, headers=HEADERS, params=params)
    if response.status_code == 200:
        data = response.json()
        for repo in data['items']:
            repo_info = {
                'name': repo['full_name'],
                'url': repo['html_url'],
                'description': repo['description'],
                'stars': repo['stargazers_count'],
                'language': repo['language']
            }
            results.append(repo_info)
    else:
        print(f"Error: {response.status_code}, {response.json()}")
        break
    time.sleep(1)

with open('top_200_java_repos.json', 'w', encoding='utf-8') as f:
    json.dump(results, f, ensure_ascii=False, indent=4)