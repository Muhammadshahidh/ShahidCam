path = "app/build.gradle.kts"

with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

# Add '=' for Kotlin DSL syntax
c = c.replace("compileSdk 35", "compileSdk = 35")
c = c.replace("targetSdk 35", "targetSdk = 35")

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("build.gradle.kts syntax fixed with '=' !")
