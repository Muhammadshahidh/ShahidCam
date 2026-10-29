import os, re

base_dir = "app/src/main/java"
class_to_pkg = {}
file_list = []

# 1. Scan all Java files and map their correct package based on their folder
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith('.java'):
            full_path = os.path.join(root, f)
            file_list.append(full_path)
            # Derive package from folder path
            rel_dir = os.path.relpath(root, base_dir)
            pkg = rel_dir.replace(os.sep, '.')
            cls_name = f[:-5]
            class_to_pkg[cls_name] = pkg

            # Ensure file has correct package declaration
            with open(full_path, 'r', encoding='utf-8') as fl:
                content = fl.read()
            
            # Replace old package declaration if wrong
            new_content = re.sub(r'^\s*package\s+[^;]+;', f'package {pkg};', content, count=1)
            if new_content != content:
                with open(full_path, 'w', encoding='utf-8') as fl:
                    fl.write(new_content)
                print(f"Fixed package declaration in: {f} -> {pkg}")

# 2. Fix imports in all files
symbols_to_ensure = [
    "ForensicsDatabase",
    "ForensicsSnapshotWithMedia",
    "DigitalForensicsEventRecorder",
    "ForensicsCardContainerView",
    "AiEventSnapshotDao"
]

for full_path in file_list:
    with open(full_path, 'r', encoding='utf-8') as fl:
        content = fl.read()
    orig = content

    # Replace any leftover fadcam imports
    content = content.replace("com.fadcam.", "com.shahid.cam.")

    # Check if this file uses any of the missing symbols
    for sym in symbols_to_ensure:
        if sym in content and sym in class_to_pkg:
            target_pkg = class_to_pkg[sym]
            expected_import = f"import {target_pkg}.{sym};"
            
            # Check if this file is not already in that package and doesn't have the import
            rel_dir = os.path.relpath(os.path.dirname(full_path), base_dir).replace(os.sep, '.')
            if rel_dir != target_pkg and expected_import not in content:
                # Add import right after package declaration
                content = re.sub(
                    r'(package\s+[^;]+;\n)',
                    r'\1' + expected_import + '\n',
                    content,
                    count=1
                )
                print(f"Added {expected_import} to {os.path.basename(full_path)}")

    if content != orig:
        with open(full_path, 'w', encoding='utf-8') as fl:
            fl.write(content)

print("\n>>> All packages, imports, and Room entities synchronized 100%!")
