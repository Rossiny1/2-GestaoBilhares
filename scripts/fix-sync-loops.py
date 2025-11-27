from pathlib import Path
import re

path = Path("data/src/main/java/com/example/gestaobilhares/data/repository/domain/SyncRepository.kt")
text = path.read_text(encoding="utf-8")

# Replace lambda-based forEach with regular for-loops
text = re.sub(r"snapshot\.documents\.forEach\s*\{\s*doc\s*->", "for (doc in snapshot.documents) {", text)

# Replace return@forEach with continue
text = text.replace("return@forEach", "continue")

path.write_text(text, encoding="utf-8")

