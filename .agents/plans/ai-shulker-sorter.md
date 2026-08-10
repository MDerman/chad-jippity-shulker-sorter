Yes. I’d build this as a **fork of ShulkerSorter, Fabric-only initially, with the AI entirely client-side and the authoritative item-moving engine server-side**.

ShulkerSorter is MIT-licensed, already has a dedicated `mc1.21.9-1.21.11` branch, and already implements the annoying parts: inventory/shulker inspection, stack merging, capacity validation, multiplayer packets, loose-item support, labels and undo. ([[GitHub](https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11)][1])

## Architecture

```text
Minecraft CLIENT
│
├── Inventory snapshot
│     ├── normal inventory
│     └── contents of carried shulkers
│
├── AIShulkerSorter client config
│     ├── OpenAI API key       <-- HERE
│     ├── sorting instructions
│     └── model
│
├── OpenAI API
│     ↓
│   semantic sorting plan
│
└── Fabric custom packet
      ↓
────────────────────────────────────────
Minecraft SERVER
│
├── validates snapshot hasn't changed
├── validates every item still exists
├── deterministic packing algorithm
├── merges stacks
├── checks 27-slot capacities
├── updates shulker contents
├── renames shulkers
└── sends success / error
```

So **the API key never goes to your Minecraft server**.

Fabric supports exactly this sort of client→server custom-payload architecture. ([[Fabric Documentation](https://docs.fabricmc.net/develop/networking?utm_source=chatgpt.com)][2])

---

# 1. Start by forking ShulkerSorter

Don't rewrite the Minecraft plumbing.

Fork:

```text
dennisthegamer/ShulkerSorter
```

and work from:

```text
mc1.21.9-1.21.11
```

That branch explicitly targets 1.21.9–1.21.11 with Java 21. ([[GitHub](https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11)][1])

I'd simplify the project initially:

```text
ai-shulker-sorter/
├── common/
│   └── src/main/java/
│       ├── inventory/
│       ├── sorting/
│       ├── networking/
│       └── model/
│
├── fabric/
│   └── src/main/java/
│       ├── client/
│       │   ├── OpenAiClient.java
│       │   ├── AiSortPlanner.java
│       │   ├── ConfigScreen.java
│       │   └── Keybinds.java
│       │
│       └── server/
│           └── SortPacketHandler.java
│
└── build.gradle
```

You don't need NeoForge unless you actually use it.

---

# 2. Change what the AI is responsible for

This is the important design choice.

### Don't ask GPT for this

```json
{
  "move_from": "shulker_3_slot_12",
  "move_to": "shulker_1_slot_7",
  "count": 64
}
```

That's unnecessarily dangerous and brittle.

Instead ask it:

```json
{
  "groups": [
    {
      "name": "Redstone",
      "items": ["item_14", "item_18", "item_22"]
    },
    {
      "name": "Travel",
      "items": ["item_4", "item_9", "item_31"]
    },
    {
      "name": "Valuables",
      "items": ["item_2", "item_6"]
    }
  ]
}
```

GPT answers only:

> **What belongs together?**

Your Java code answers:

> **How do we physically pack it?**

---

# 3. Create an inventory snapshot

When you press something like `J`, the client creates:

```text
InventorySnapshot
├── loose inventory
│   ├── rockets ×64
│   ├── steak ×32
│   └── diamond pickaxe
│
└── shulkers
    ├── slot 10
    │   ├── cobblestone ×64
    │   ├── comparator ×17
    │   └── diamonds ×24
    │
    ├── slot 11
    │   ├── oak_planks ×64
    │   └── rockets ×42
    │
    └── slot 12
        └── ...
```

But don't send huge raw Minecraft objects to the LLM.

Normalize it into something like:

```json
{
  "items": [
    {
      "ref": "a1",
      "id": "minecraft:firework_rocket",
      "name": "Firework Rocket",
      "count": 106
    },
    {
      "ref": "a2",
      "id": "minecraft:diamond_pickaxe",
      "name": "Diamond Pickaxe",
      "count": 1,
      "enchanted": true
    },
    {
      "ref": "a3",
      "id": "minecraft:comparator",
      "name": "Redstone Comparator",
      "count": 17
    }
  ],
  "available_shulkers": 6
}
```

Identical items can be aggregated, which keeps the request tiny.

---

# 4. Give the player a natural-language rule box

This is where this becomes significantly nicer than ShulkerSorter.

In Mod Menu:

```text
AI Shulker Sorter

API key:     ••••••••••••••••

Sorting instructions:

┌──────────────────────────────────────────────┐
│ Keep food, rockets, tools and ender pearls  │
│ in my inventory.                            │
│                                              │
│ Make sensible shulkers for building,        │
│ redstone, valuables, mob drops, farming     │
│ etc.                                        │
│                                              │
│ Keep different wood families together.      │
│ Put rare items together rather than filling │
│ their own shulker.                          │
└──────────────────────────────────────────────┘

[ Test API ] [ Save ]
```

Fabric key mappings are client-side by design, so the AI-trigger/keybind portion belongs naturally in the client initializer. ([[Fabric Documentation](https://docs.fabricmc.net/develop/key-mappings?utm_source=chatgpt.com)][3])

---

# 5. Call OpenAI directly from the Minecraft client

Use the **Responses API + Structured Outputs**.

Structured Outputs lets you force the model response to conform to your JSON schema, rather than parsing arbitrary prose. ([[OpenAI Platform](https://platform.openai.com/docs/guides/structured-outputs)][4])

Your schema would essentially require:

```text
SortPlan
├── categories[]
│   ├── id
│   ├── label
│   └── itemRefs[]
│
└── keepLoose[]
```

The system instruction can be short:

```text
You organize Minecraft items.

Group the supplied item references into practical shulker-box
categories.

Do not invent item references.
Every supplied reference must appear exactly once.

Prefer useful gameplay groupings rather than purely alphabetical
sorting.

Respect the user's sorting preferences.
```

Then:

```text
USER PREFERENCES
Keep rockets, tools, food and pearls loose.
Group building blocks according to material where sensible.

INVENTORY
...
```

Because you're on Java 21, you can simply use `java.net.http.HttpClient`; you don't need to drag a large SDK into the Minecraft mod.

---

# 6. API key: yes, client-side — but do it carefully

For **your own private Minecraft setup**, I'd support:

```text
1. OPENAI_API_KEY environment variable        preferred
2. Paste key into config screen               convenient
3. Paste for this Minecraft session only      safest convenient option
```

I'd have:

```text
Remember API key on this computer: [ ]
```

Default **off**.

If enabled:

```text
.minecraft/
└── config/
    └── aishulkersorter-client.toml
```

and absolutely:

```text
DON'T:
- put it in fabric.mod.json
- put it in your Modrinth pack
- put it in Git
- send it in Minecraft packets
- print it to logs
- send it to your server
```

OpenAI recommends keeping API keys out of client-side distributed applications and recommends environment variables/backend storage instead. ([[OpenAI Help Center](https://help.openai.com/en/articles/5112595-best-practices-for-api-key?utm_source=chatgpt.com)][5])

A local mod used only by **you** is somewhat different from shipping a key embedded in an app: the user supplies their own key on their own machine. But another malicious mod/process could still read a plaintext config file.

So I'd personally use:

```text
OPENAI_API_KEY
```

and have the config UI display:

```text
API key: Found in environment ✓
```

---

# 7. Server should NEVER trust the AI plan

Before calling the API, calculate:

```text
snapshotHash = SHA256(canonicalInventoryState)
```

Client sends:

```text
AiSortRequest
{
    snapshotHash,
    assignments,
    categoryNames,
    keepLoose
}
```

Server then independently:

1. Reads the player's actual inventory.
2. Reads the actual shulker contents.
3. Creates its own snapshot.
4. Confirms `snapshotHash`.
5. Confirms every referenced item exists.
6. Confirms no item appears twice.
7. Ignores any counts supplied by the client.
8. Calculates packing itself.
9. Verifies total capacity.
10. Applies changes atomically.

So a malicious client can't say:

```text
I have 64 diamonds
```

when it actually has 3.

The **server is authoritative**.

This is basically an evolution of what ShulkerSorter already does: its current multiplayer survival design executes sorting server-side through custom networking. ([[GitHub](https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11)][1])

---

# 8. Keep ShulkerSorter's deterministic packing engine

After AI categorization:

```text
GPT:

stone → Building
stone_bricks → Building
oak_planks → Building
diamond → Valuables
emerald → Valuables
comparator → Redstone
observer → Redstone
```

Then existing-ish ShulkerSorter logic does:

```text
Building #1
├── stone x64
├── stone x64
├── stone_bricks x42
└── ...

Building #2
├── oak_planks x64
└── ...

Redstone #1
├── comparator x17
├── observer x31
└── ...

Valuables #1
├── diamond x24
└── emerald x13
```

That preserves ShulkerSorter's stack merging, capacity checks, affinity and labeling machinery rather than making GPT solve bin-packing. The existing project already has capacity validation and stack merging. ([[GitHub](https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11)][1])

---

# 9. Preserve the really useful existing features

I'd keep:

```text
J             AI sort
Shift + J     Undo

[LOCKED]      don't touch shulker
[KEEP]        keep loose

includeLooseItems = true
autoLabel = true
```

ShulkerSorter already implements `[LOCKED]`, `[KEEP]`, undo, loose-item inclusion and automatic labels, so these are easy wins from the fork. ([[GitHub](https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11)][1])

And add:

```text
Alt + J       normal deterministic sort
```

So if OpenAI is unavailable:

```text
AI sorting failed
→ Use normal sorter instead? [Yes]
```

That's a very useful fallback.

---

# 10. Add a preview before applying

I would definitely make AI sorting optionally show:

```text
AI SORT PLAN

Inventory
  ✓ Diamond Pickaxe
  ✓ Rockets
  ✓ Steak
  ✓ Ender Pearls

Shulkers

[1] Building — Stone
    Stone
    Cobblestone
    Deepslate
    Stone Bricks
    Andesite

[2] Building — Wood
    Oak Logs
    Oak Planks
    Spruce Logs
    Spruce Planks

[3] Redstone
    Repeaters
    Comparators
    Pistons
    Observers

[4] Valuables
    Diamonds
    Emeralds
    Ancient Debris

             [ Sort ] [ Cancel ]
```

Initially I'd require confirmation.

Later:

```text
☑ Sort immediately without preview
```

---

# 11. Your Modrinth pack

Your generated artifact becomes:

```text
ai-shulker-sorter-1.0.0+mc1.21.11.jar
```

Because it has both client and dedicated-server functionality, mark it:

```json
"env": {
  "client": "required",
  "server": "required"
}
```

The Modrinth `.mrpack` format explicitly supports per-file client/server requirements like this. ([[Modrinth Help Center](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack?utm_source=chatgpt.com)][6])

Importantly:

```text
client-overrides/
    config/
        ai-shulker-sorter.toml
```

can contain **defaults**, e.g.:

```toml
model = "..."
preview = true
includeLooseItems = true
apiKey = ""
```

but **never your actual key**.

Modrinth also has separate `client-overrides` and `server-overrides` specifically for this purpose. ([[Modrinth Help Center](https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack?utm_source=chatgpt.com)][6])

---

# 12. Your Helm chart

Your server deployment only needs the same JAR:

```text
Minecraft server pod

/mods/
├── fabric-api.jar
├── architectury-api.jar       # if fork still needs it
└── ai-shulker-sorter.jar
```

There is **no**:

```text
OPENAI_API_KEY
```

Kubernetes Secret.

There is **no outbound OpenAI traffic from the server**.

Conceptually your Helm values just need to result in the mod being present:

```yaml
minecraft:
  mods:
    - ai-shulker-sorter
```

or, if your chart installs the `.mrpack`, just ensure the mod is marked server-required.

Modrinth explicitly documents server-side installation of Modrinth packs, including through `itzg/minecraft-server` if that's what your Helm deployment uses. ([[Modrinth Help Center](https://support.modrinth.com/en/articles/8802250-modpacks-on-modrinth?utm_source=chatgpt.com)][7])

---

# 13. One further improvement I'd make

Don't invoke GPT every time for items it already understands.

Maintain a **client-side semantic cache**:

```json
{
  "minecraft:comparator": "Redstone",
  "minecraft:repeater": "Redstone",
  "minecraft:observer": "Redstone",
  "minecraft:diamond": "Valuables",
  "minecraft:emerald": "Valuables"
}
```

Then next sort:

```text
286 item stacks
↓
87 unique item types
↓
76 already classified
↓
11 unknown/new items sent to GPT
```

For modded Minecraft this gets even better:

```text
create:brass_casing
create:precision_mechanism
ae2:logic_processor
mekanism:alloy_reinforced
```

GPT can semantically classify unfamiliar mod items while remembering the result.

You could then have a button:

```text
Forget AI sorting memory
```

---

## The implementation order I'd use

```text
PHASE 1
Fork ShulkerSorter 1.21.11
     ↓
Get existing deterministic sorting working
client + your server

PHASE 2
Create InventorySnapshot + stable ItemRef
     ↓
Add snapshot hash

PHASE 3
Create OpenAI client
     ↓
Structured Output → category assignments

PHASE 4
Client sends:
snapshotHash + assignments
     ↓
Server validates
     ↓
Existing sorter packs items

PHASE 5
Add config UI:
API key
instructions
model
preview

PHASE 6
Add preview + undo + fallback sorter

PHASE 7
Add semantic cache

PHASE 8
Build JAR
     ↓
Modrinth pack: client required + server required
     ↓
Helm/Minecraft server: same JAR
```

### The nice separation

The final system is essentially:

```text
              INTELLIGENCE
                   │
                   ▼
            OpenAI on client
          "what goes together?"
                   │
                   ▼
             SortPlan JSON
                   │
        ───────────┼───────────
                   │ packet
                   ▼
               SERVER
                   │
                   ▼
         deterministic algorithm
      "how do I safely move it?"
                   │
                   ▼
               SHULKERS
```

**This is the version I'd build.** It keeps your key completely out of Kubernetes/Minecraft-server configuration, reuses almost all the difficult work in the MIT ShulkerSorter project, and confines the LLM to the one thing it's actually better at: semantic organization. ([[GitHub](https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11)][1])

If you're going to implement it with Codex, the next useful step would be to give Codex the ShulkerSorter repository and a fairly precise implementation spec based on the above, rather than starting with “make an AI Minecraft mod.”

[1]: https://github.com/dennisthegamer/ShulkerSorter/tree/mc1.21.9-1.21.11 "GitHub - dennisthegamer/ShulkerSorter at mc1.21.9-1.21.11 · GitHub"
[2]: https://docs.fabricmc.net/develop/networking?utm_source=chatgpt.com "Networking"
[3]: https://docs.fabricmc.net/develop/key-mappings?utm_source=chatgpt.com "Key Mappings"
[4]: https://platform.openai.com/docs/guides/structured-outputs "Structured model outputs | OpenAI API"
[5]: https://help.openai.com/en/articles/5112595-best-practices-for-api-key?utm_source=chatgpt.com "Best Practices for API Key Safety | OpenAI Help Center"
[6]: https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack?utm_source=chatgpt.com "Modrinth Modpack Format (.mrpack)"
[7]: https://support.modrinth.com/en/articles/8802250-modpacks-on-modrinth?utm_source=chatgpt.com "Modpacks on Modrinth"
