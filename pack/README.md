# Modrinth Pack Integration

Mark built mod file `required` for both client and server in `modrinth.index.json`:

```json
"env": { "client": "required", "server": "required" }
```

Copy `client-overrides/` into pack root. Never add an `openrouter_api_key` or `OPENROUTER_API_KEY` value.
