# Chad Jippity Shulker Sorter

Fabric mod for Minecraft 1.21.9-1.21.11. GLM 5.3 groups inventory items through OpenRouter on the client; the server validates inventory state and performs every item mutation deterministically.

Forked from [ShulkerSorter](https://github.com/dennisthegamer/ShulkerSorter) under MIT license.

## Features

- `J`: create semantic sort plan, preview it, then sort
- `Shift+J`: undo last server-authoritative sort
- `Alt+J`: use original deterministic sorter without an AI request
- Full inventory + carried-shulker snapshot with SHA-256 stale-state rejection
- Structured Outputs through the OpenRouter Chat Completions API
- Stack merging, 27-slot capacity validation, automatic labels, `[LOCKED]`, `[KEEP]`
- Client-side model/rules/key UI; API key never enters Minecraft packets or server config
- `OPENROUTER_API_KEY`, session-only paste, or opt-in local persistence
- Instruction-scoped semantic cache for known and modded item IDs
- Fabric client/server networking; same JAR runs on client and dedicated server

## Requirements

- Java 21
- Fabric Loader 0.18.3+
- Fabric API
- Architectury API 18.x on Minecraft 1.21.9-1.21.10, or 19.x on Minecraft 1.21.11
- Minecraft 1.21.9-1.21.11
- Mod Menu and YACL for in-game configuration

Install mod and dependencies on client and server. Dedicated server needs no OpenRouter key and makes no OpenRouter requests.

## Configuration

Open Mod Menu, choose Chad Jippity Shulker Sorter, then configure:

- OpenRouter model under `openrouter_model`, default `z-ai/glm-5.3`
- natural-language sorting instructions
- preview confirmation
- semantic cache
- session or remembered API key
- deterministic sorter, tooltip, feedback, lock/keep, and category settings

Key precedence:

1. `OPENROUTER_API_KEY`
2. session key pasted into masked in-game screen
3. locally remembered key when explicitly enabled

Remembered key lives in `.minecraft/config/aishulkersorter.toml` as plaintext under `openrouter_api_key`. Default is off. Keys are never logged, bundled, packetized, or sent to Minecraft server.

Run `/chadinit <API_KEY>` to save a key from Minecraft. This client-side command enables local key persistence and removes itself from recent command history after it runs.

## Authority Model

Client sends only snapshot hash and semantic assignments. Server independently rebuilds inventory snapshot, rejects stale state, requires exact one-time reference coverage, rejects boxed refs marked loose, calculates stack splits/capacity, saves undo state, and applies changes after validation completes.

## Build

```bash
./gradlew test build
```

Artifact:

```text
fabric/build/libs/aishulkersorter-fabric-1.1.0+mc1.21.9-1.21.11.jar
```

## Packaging

- `pack/client-overrides/config/aishulkersorter.toml`: key-free Modrinth client defaults
- `deploy/helm-values.example.yaml`: server chart integration sketch
- `.agents/plans/ai-shulker-sorter.md`: original implementation plan

## License

MIT. See `LICENSE` and `NOTICE`.
