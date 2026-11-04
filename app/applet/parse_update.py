import re
import os

with open("/tmp/extracted_zip/update_system.sh", "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("$OWNER", "aboubacarsidikmoussagarba").replace("$NAME", "ZAgent")

pattern = re.compile(r"cat > \"?([^\s\"\']+)\"? << '\''?EOF'\''?\n(.*?)\nEOF", re.DOTALL)
matches = pattern.findall(content)
print(f"Found {len(matches)} heredoc files to write.")
for path, file_content in matches:
    path = path.strip()
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as out:
        out.write(file_content)
    print("Wrote:", path)
