# Changelog

## Unreleased

### Changed

- Replaced the OpenAI client with OpenRouter and made `z-ai/glm-5.3` the default model
- Switched config to `openrouter_model`, `OPENROUTER_API_KEY`, and `openrouter_api_key`
- Added the client-side `/chadinit <API_KEY>` setup command

## [1.0.0] - 2026-08-10

### Added

- Fabric 1.21.9-1.21.11 client/server mod based on MIT-licensed ShulkerSorter
- Client-only OpenAI Responses API integration with strict JSON Schema output
- Natural-language sorting rules, model setting, masked key screen, API test, and opt-in key persistence
- Stable inventory refs, full-state SHA-256 hash, semantic cache, and paginated confirmation preview
- Server-side stale-state, ref coverage, duplicate, origin, stack, and shulker-capacity validation
- Atomic deterministic packing, labels, multiplayer undo, `[LOCKED]`, `[KEEP]`, loose items, and offline fallback
- Modrinth defaults, Helm values example, CI build, unit tests, and dedicated-server smoke coverage
- Compile/runtime Architectury split so 1.21.11 development uses compatible 19.x client mixins
