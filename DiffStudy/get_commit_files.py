import os
import json
import time
from urllib.parse import urlparse
import requests

GITHUB_TOKEN = 'xxx'
headers = {
    "Accept": "application/vnd.github.v3+json",
    "Authorization": f"Bearer {GITHUB_TOKEN}"
}

def process_commits(json_name):
    """
    Read commit entries from the given JSON file and download before/after files locally.
    """
    
    with open('selected_commits_info/'+json_name, 'r', encoding='utf-8') as f:
        repos = json.load(f)

    print(f"Processing commits from repository {json_name.replace('.json','')}...")

    for entry in repos:
        oid = entry.get("oid")
        details = entry.get("details", {})
        raw_url = details.get("raw_url")
        parent_sha = details.get("parent_sha")
        filename = details.get("filename")
        prev_filename = filename
        if 'previous_filename' in details:
          prev_filename = details['previous_filename']

        print(entry['url'])

        # Skip entries missing required fields
        if not all([oid, raw_url, parent_sha, filename]):
            print(f"Skipping entry due to missing fields: oid={oid}")
            continue

        # Construct parent raw URL
        p_raw_url = raw_url.replace(oid, parent_sha)

        if filename != prev_filename:
         p_raw_url = p_raw_url.replace(filename.replace('/','%2F'),prev_filename.replace('/','%2F'))

        # Construct unique directory name
        base_dir = os.path.join("commit_files", json_name.replace('.json',''), oid[:12])

        # Create before/after subdirectories
        after_dir = os.path.join(base_dir, "after")
        before_dir = os.path.join(base_dir, "before")
        os.makedirs(after_dir, exist_ok=True)
        os.makedirs(before_dir, exist_ok=True)

        # Replace '/' in filename with '_'
        safe_filename = filename.replace("/", "_")
        safe_prevfilename = prev_filename.replace("/", "_")

        # Local file paths
        after_path = os.path.join(after_dir, safe_filename)
        before_path = os.path.join(before_dir, safe_prevfilename)

        if os.path.exists(after_path) and os.path.exists(before_path):
            continue

        # Download and save files
        for url, path in [(raw_url, after_path), (p_raw_url, before_path)]:
            try:
                response = requests.get(url, headers=headers)
                response.raise_for_status()
                with open(path, "wb") as wf:
                    wf.write(response.content)
                print(f"Saved: {path}")
                time.sleep(1)
            except requests.RequestException as e:
                print(f"Failed to download ({url}): {e}")
                exit()

if __name__ == "__main__":
    for e in os.listdir('selected_commits_info'):
      process_commits(e)
