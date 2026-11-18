"""Write docs/privacidad/index.html from the in-app policy (content/PrivacyPolicy.kt).

The Google Play listing needs the policy at a public URL with the same text the app shows.
Run after every change to PrivacyPolicy.kt: python3 scripts/privacy_page.py
"""
import html
import re
from pathlib import Path

root = Path(__file__).resolve().parent.parent
src = (root / "app/src/main/java/pe/favio/ciclo/content/PrivacyPolicy.kt").read_text(encoding="utf-8")
STR = r'"((?:[^"\\]|\\.)*)"'
updated = re.search(r"UPDATED = " + STR, src).group(1)
issues = re.search(r"ISSUES_URL = " + STR, src).group(1)
summary = re.search(r"SUMMARY =\s*" + STR, src).group(1)
body = []
for title, block in re.findall(r"PolicySection\(\s*" + STR + r",\s*listOf\((.*?)\),\s*\)", src, re.S):
    body.append(f"  <h2>{html.escape(title)}</h2>")
    body += [f"  <p>{html.escape(p)}</p>" for p in re.findall(STR, block)]
page = f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Política de privacidad de Ciclo</title>
<meta name="description" content="{html.escape(summary)}">
<link rel="stylesheet" href="../style.css">
</head>
<body>
<main>
  <p class="muted"><a href="../">Ciclo</a></p>
  <h1>Política de privacidad</h1>
  <p class="muted">Vigente desde el {updated}. Aplica a la app Ciclo para Android (pe.favio.ciclo).</p>
  <p class="lead">{html.escape(summary)}</p>
{chr(10).join(body)}
  <p><a href="{issues}">{issues.removeprefix("https://")}</a></p>
</main>
</body>
</html>
"""
out = root / "docs/privacidad/index.html"
out.write_text(page, encoding="utf-8")
print(out, len(body), "blocks")
