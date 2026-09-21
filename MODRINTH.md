# Chad Jippity Shulker Sorter

AI-assisted, server-authoritative shulker organization for Fabric 1.21.9-1.21.11.

Press `J` to classify carried items with GLM 5.3 through OpenRouter, inspect the category preview, and let the server safely merge, capacity-check, label, and move stacks. Press `Shift+J` to undo or `Alt+J` for offline deterministic sorting.

Requires Fabric API, Architectury API, Mod Menu, and YACL on the client. Install the same mod JAR, Fabric API, and Architectury API on the server. The OpenRouter key stays client-side; the server needs no key and makes no OpenRouter requests.

Features: natural-language rules, strict Structured Outputs, stale-inventory hash validation, `[LOCKED]` boxes, `[KEEP]` loose items, automatic labels, semantic cache, preview, undo, tooltips, and deterministic fallback.

Run `/chadinit <API_KEY>` to save an OpenRouter key in the local Minecraft instance config.

Based on DennisTheGamer's MIT-licensed ShulkerSorter.
