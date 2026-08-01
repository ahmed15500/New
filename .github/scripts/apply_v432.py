from pathlib import Path

path = Path("app/src/main/java/com/ahmed/yawmeyaty/EcoWasteModernActivity.kt")
text = path.read_text(encoding="utf-8")

old = 'placeholder = { Text("01208097044") }'
new = 'placeholder = { Text("مثال: 01XXXXXXXXX") }'

if old in text:
    text = text.replace(old, new, 1)
elif new not in text:
    raise SystemExit("phone placeholder marker not found")

path.write_text(text, encoding="utf-8")
print("Applied Eco Waste v4.3.2 generic phone placeholder")
