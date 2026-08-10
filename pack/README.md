# Modrinth Pack Integration

Mark built mod file `required` for both client and server in `modrinth.index.json`:

```json
"env": { "client": "required", "server": "required" }
```

Copy `client-overrides/` into pack root. Never add `api_key` or `OPENAI_API_KEY` value.
