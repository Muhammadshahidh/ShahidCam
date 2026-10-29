import os, glob, re

# 1. Update GitHub Actions workflow to safely find any .aab or .apk
workflow_files = glob.glob('.github/workflows/*.yml') + glob.glob('.github/workflows/*.yaml')
for wf in workflow_files:
    with open(wf, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix upload path to catch all generated bundles
    if 'actions/upload-artifact' in content:
        content = re.sub(
            r'path:\s*app/build/outputs/bundle/\*\*/\*\.aab',
            'path: app/build/outputs/**/*',
            content
        )
        content = re.sub(
            r'path:\s*app/build/outputs/bundle/[^\n]+',
            'path: app/build/outputs/**/*',
            content
        )
        with open(wf, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated workflow path in: {wf}")

print("Workflow paths checked!")
