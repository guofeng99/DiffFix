import json
import random
import requests
import os
import time
import re

GITHUB_TOKEN = 'xxx'

def process_commits(name):
    input_file = f"commits_info/{name}.json"
    output_file = f"selected_commits_info/{name}.json"

    if os.path.exists(output_file):
        return
    
    waitcnt = 0
    while not os.path.exists(input_file):
        time.sleep(60)
        waitcnt+=1
        print('Wait for selecting:',waitcnt)

    print(f"Selecting commits from repository {name}...")

    with open(input_file, "r", encoding="utf-8") as f:
        repos = json.load(f)

    selected = []
    headers = {
        "Accept": "application/vnd.github.v3+json",
        "Authorization": f"Bearer {GITHUB_TOKEN}"
    }

    def is_single_java_file(commit_data):
        files = commit_data.get("files", [])
        return len(files) == 1 and files[0]["filename"].endswith(".java")
    
    def has_added_and_deleted(commit_data):
        stats = commit_data.get("stats", {"additions":0,"deletions": 0})
        return stats['additions']>0 and stats['deletions']>0
    
    def is_single_parent(commit_data):
        parents = commit_data.get("parents", [])
        return len(parents) == 1

    while repos and len(selected) < 100:
        entry = random.choice(repos)
        commit_url = entry["url"]
        try:
            api_url = commit_url.replace(
                "https://github.com/",
                "https://api.github.com/repos/"
            ).replace("/commit/", "/commits/")

            response = requests.get(api_url, headers=headers)
            if response.status_code == 403:
                print("Rate limit hit, sleeping 60 seconds...")
                time.sleep(60)
                continue
            elif response.status_code != 200:
                print(f"Failed to fetch commit: {response.status_code}")
                time.sleep(60)
                continue
            
            time.sleep(1)

            repos.remove(entry)

            commit_data = response.json()
            if is_single_java_file(commit_data) and has_added_and_deleted(commit_data) and is_single_parent(commit_data):
                entry['details'] = commit_data["files"][0]
                entry['details']['parent_sha'] = commit_data["parents"][0]["sha"]
                selected.append(entry)
                print(f'Progress {len(selected)}/100')

        except Exception as e:
            print(f"Error fetching commit data: {e}")

    print(f"Collected {len(selected)} matching commits.")
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(selected, f, indent=2)
    print(f"Saved results to {output_file}")

def extract_owner_repo(repo_url: str):
    """Extract owner and repo name from GitHub URL"""
    match = re.match(r'https://github.com/([^/]+)/([^/]+)', repo_url)
    if not match:
        raise ValueError("Invalid GitHub repository URL.")
    return match.group(1), match.group(2)

with open('java_repos.txt', 'r') as file:
    repo_urls = file.readlines()

for repo_url in repo_urls:
    repo_url = repo_url.strip()
    owner,repo = extract_owner_repo(repo_url)
    process_commits(f'{owner}_{repo}')