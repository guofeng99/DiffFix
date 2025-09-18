import json
import requests
from datetime import datetime, timedelta
import time

# Replace this with your actual GitHub token
GITHUB_TOKEN = 'xxx'

# GitHub API headers with token
headers = {
    'Authorization': f'token {GITHUB_TOKEN}',
    'Accept': 'application/vnd.github.v3+json'
}

# API endpoint template
GITHUB_API_URL = "https://api.github.com/repos/{}/commits"

# Get latest commit date
def get_latest_commit_date(repo_full_name):
    url = GITHUB_API_URL.format(repo_full_name)
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        commits = response.json()
        if commits:
            commit_date = commits[0]['commit']['committer']['date']
            return datetime.strptime(commit_date, "%Y-%m-%dT%H:%M:%SZ")
    else:
        print(f"Failed to fetch commits for {repo_full_name}: {response.status_code}")
    return None

# Load the previously filtered JSON file
with open('top_200_java_repos_filtered1.json', 'r', encoding='utf-8') as file:
    repos = json.load(file)

# Date 4 years ago from 2025-4-16
three_years_ago = datetime.now() - timedelta(days=4*365)

# Filter repositories with commits in the last 4 years
filtered_repos = []
for repo in repos:
    latest_commit_date = get_latest_commit_date(repo['name'])
    if latest_commit_date:
        print(f"{repo['name']}: Last commit on {latest_commit_date.date()}")
        if latest_commit_date > three_years_ago:
            filtered_repos.append(repo)
    time.sleep(1)  # delay to be respectful of rate limits

# Save final filtered list
with open('top_200_java_repos_filtered.json', 'w', encoding='utf-8') as file:
    json.dump(filtered_repos, file, ensure_ascii=False, indent=4)