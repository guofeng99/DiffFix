import json
import requests
import time

INPUT_FILE = 'top_200_java_repos_filtered.json'
OUTPUT_FILE = 'top_200_java_repos_info.json'
GITHUB_TOKEN = 'xxx'

headers = {
    'Authorization': f'token {GITHUB_TOKEN}',
    'Accept': 'application/vnd.github.v3+json'
}

def get_commit_count(owner, repo):
    url = f'https://api.github.com/repos/{owner}/{repo}'
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        data = response.json()
        return data.get('commits_url', '').split('{')[0]
    else:
        print(f"[Error] Repo info fetch failed for {owner}/{repo}: {response.status_code}")
        return None

def get_total_commits(owner, repo):
    url = f'https://api.github.com/repos/{owner}/{repo}/stats/contributors'
    response = requests.get(url, headers=headers)
    
    if response.status_code == 202:
        print(f"[Info] GitHub is computing stats for {owner}/{repo}, retrying later...")
        time.sleep(3)
        return get_total_commits(owner, repo)
    elif response.status_code == 200:
        stats = response.json()
        return sum(contributor['total'] for contributor in stats)
    else:
        print(f"[Error] Failed to get stats for {owner}/{repo}: {response.status_code}")
        return None

def enrich_repos_with_commits(input_path, output_path):
    with open(input_path, 'r', encoding='utf-8') as f:
        repos = json.load(f)

    enriched = []
    for i, repo in enumerate(repos):
        full_name = repo['name']
        print(f"[{i+1}/{len(repos)}] Processing {full_name}...")

        try:
            owner, repo_name = full_name.split('/')
            commit_count = get_total_commits(owner, repo_name)
            repo['commit_count'] = commit_count if commit_count is not None else 0
            enriched.append(repo)
            time.sleep(1)  
        except Exception as e:
            print(f"[Exception] Skipped {full_name}: {e}")

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(enriched, f, indent=4, ensure_ascii=False)

    print(f"\n✅ Done! Output saved to: {output_path}")

if __name__ == '__main__':
    enrich_repos_with_commits(INPUT_FILE, OUTPUT_FILE)
