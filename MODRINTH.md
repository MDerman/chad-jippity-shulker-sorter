# Chad Jippity Shulker Sorter

AI-assisted, server-authoritative shulker organization for Fabric 1.21.9-1.21.11.

Press `J` to classify carried items with OpenAI, inspect category preview, and let server safely merge, capacity-check, label, and move stacks. Press `Shift+J` to undo or `Alt+J` for offline deterministic sorting.

Requires Fabric API, Architectury API, Mod Menu, and YACL on client. Install same mod JAR, Fabric API, and Architectury API on server. OpenAI key stays client-side; server needs no key and makes no OpenAI requests.

Features: natural-language rules, strict Structured Outputs, stale-inventory hash validation, `[LOCKED]` boxes, `[KEEP]` loose items, automatic labels, semantic cache, preview, undo, tooltips, and deterministic fallback.

Based on DennisTheGamer's MIT-licensed ShulkerSorter.
