import os, glob, re

print(">>> Fixing compilation issues across the repository...")

# 1. Clean MainActivity.java from navigation_lab references
for root, dirs, files in os.walk('.'):
    for f in files:
        if f.endswith('.java'):
            p = os.path.join(root, f)
            with open(p, 'r', encoding='utf-8') as fl:
                c = fl.read()
            orig = c
            
            # Remove navigation_lab occurrences
            c = re.sub(r'case\s+R\.id\.navigation_lab:\s*[^;]*;\s*return\s+true;', '', c)
            c = re.sub(r'case\s+R\.id\.navigation_lab:[^:]*?break;', '', c, flags=re.DOTALL)
            c = re.sub(r'if\s*\([^)]*navigation_lab[^)]*\)\s*\{[^}]*\}', '', c)
            
            # Remove unused lab / forensic imports
            c = re.sub(r'import\s+[\w\.]*Forensic[\w\.]*;\n?', '', c)
            c = re.sub(r'import\s+[\w\.]*LabFragment;\n?', '', c)
            
            if c != orig:
                with open(p, 'w', encoding='utf-8') as fl:
                    fl.write(c)
                print(f"Cleaned Java file: {p}")

# 2. Check and fix mobile_navigation.xml or navigation graph if present
for root, dirs, files in os.walk('.'):
    for f in files:
        if f.endswith('.xml') and ('nav' in f or 'menu' in f):
            p = os.path.join(root, f)
            with open(p, 'r', encoding='utf-8') as fl:
                c = fl.read()
            orig = c
            # Remove navigation_lab fragment tag
            c = re.sub(r'<fragment[^>]*android:id="@[+]id/navigation_lab"[^>]*/>', '', c)
            c = re.sub(r'<fragment[^>]*android:id="@[+]id/navigation_lab"[^>]*>.*?</fragment>', '', c, flags=re.DOTALL)
            c = re.sub(r'<item[^>]*android:id="@[+]id/navigation_lab"[^>]*/>', '', c)
            if c != orig:
                with open(p, 'w', encoding='utf-8') as fl:
                    fl.write(c)
                print(f"Cleaned XML file: {p}")

# 3. Ensure Workflow uploads the actual .aab or .apk only when generated
for wf in glob.glob('.github/workflows/*.yml') + glob.glob('.github/workflows/*.yaml'):
    with open(wf, 'r', encoding='utf-8') as fl:
        wc = fl.read()
    # Ensure it looks for release aab properly
    wc = re.sub(r'path:\s*app/build/outputs/bundle/\*\*/\*\.aab', 'path: app/build/outputs/bundle/**/*.aab\n          if-no-files-found: error', wc)
    with open(wf, 'w', encoding='utf-8') as fl:
        fl.write(wc)
    print(f"Updated workflow: {wf}")

print("\n>>> All fixes applied!")
