import requests
import json
import re
import time
import os

GITHUB_API_URL = "https://api.github.com/graphql"

def extract_owner_repo(repo_url: str):
    """Extract owner and repo name from GitHub URL"""
    match = re.match(r'https://github.com/([^/]+)/([^/]+)', repo_url)
    if not match:
        raise ValueError("Invalid GitHub repository URL.")
    return match.group(1), match.group(2)

query1="""
    query($owner: String!, $name: String!, $cursor: String) {
      repository(owner: $owner, name: $name) {
        defaultBranchRef {
          target {
            ... on Commit {
              history(first: 100, after: $cursor) {
                pageInfo {
                  hasNextPage
                  endCursor
                }
                edges {
                  node {
                    oid
                    message
                    url
                    committedDate
                    changedFilesIfAvailable
                  }
                }
              }
            }
          }
        }
      }
    }
    """

# query2=query1.replace("first: 100","first: 10")

def get_java_commits(repo_url: str, github_token: str):
    """Main function: retrieves commits that modified exactly one .java file"""
    owner, repo = extract_owner_repo(repo_url)

    if os.path.exists(f'commits_info/{owner}_{repo}.json'):
      print(f'Succeed: {owner}/{repo}')
      return

    headers = {
        "Authorization": f"Bearer {github_token}",
        "Content-Type": "application/json"
    }
    query = query1
    cursor = None
    has_next_page = True
    filtered_commits = []

    print(f"Fetching commits from repository {owner}/{repo}...")

    filename = f"{owner}_{repo}.json"

    succeed = 0

    while has_next_page:
        print(len(filtered_commits))
        variables = {"owner": owner, "name": repo, "cursor": cursor}
        response = requests.post(GITHUB_API_URL, json={"query": query, "variables": variables}, headers=headers)
        retry = 0
        while response.status_code != 200:
            retry+=1
            print('retry:',retry)
            time.sleep(30)
            response = requests.post(GITHUB_API_URL, json={"query": query.replace("first: 100","first: 1"), "variables": variables}, headers=headers)
        time.sleep(1)

        data = response.json()
        # print(data)
        history = data['data']['repository']['defaultBranchRef']['target']['history']

        for edge in history['edges']:
            commit = edge['node']
            if commit['changedFilesIfAvailable'] == 1:
                filtered_commits.append({
                    "oid": commit['oid'],
                    "message": commit['message'],
                    "date": commit['committedDate'],
                    "url": commit['url'],
                })
                succeed+=1


        has_next_page = history['pageInfo']['hasNextPage']
        cursor = history['pageInfo']['endCursor']

    
    with open('commits_info/'+filename, "w", encoding="utf-8") as f:
        json.dump(filtered_commits, f, indent=2, ensure_ascii=False)
    
    print(f"✅ Retrieved {len(filtered_commits)} matching commits. Saved to {filename}")

if __name__ == "__main__":
    github_token = "xxx"

    with open('java_repos.txt', 'r') as file:
        repo_urls = file.readlines()
    
    for repo_url in repo_urls:
        repo_url = repo_url.strip()
        get_java_commits(repo_url, github_token)