# Releasing

Modrinth project ID: `mbKNv70h`

The `build and release` GitHub Actions workflow publishes tags matching `v*`. Ordinary pushes and pull requests only build the mod.

## One-time configuration

The GitHub repository needs:

- Actions variable `MODRINTH_PROJECT_ID` set to `mbKNv70h`
- Actions secret `MODRINTH_TOKEN` with permission to create versions for the project

Keep the token in the personal shared Secret Bindings global named `MODRINTH_TOKEN`. Never put it in Git, release notes, workflow files, or shell arguments.

To rotate it safely:

```bash
secret-bindings globals set MODRINTH_TOKEN --scope personal --shared --prompt
gh secret set MODRINTH_TOKEN --repo MDerman/chad-jippity-shulker-sorter
```

Both commands read the value through hidden input.

## Publish a release

1. Update `mod_version` in `gradle.properties`.
2. Move the relevant entries from `Unreleased` into a dated section in `CHANGELOG.md`, such as `## [1.2.0] - 2026-10-01`.
3. Update versioned examples in `README.md` and `deploy/helm-values.example.yaml`.
4. Run the clean build:

   ```bash
   ./gradlew clean test build
   ```

5. Commit and push `main`, then wait for its GitHub Actions build to pass.
6. Create and push a matching annotated tag:

   ```bash
   git tag -a v1.2.0 -m "Release 1.2.0"
   git push origin v1.2.0
   ```

The workflow rejects a tag that does not match `mod_version`. It reads the matching changelog section, selects the release JAR, and creates a listed Fabric release for Minecraft 1.21.9, 1.21.10, and 1.21.11.

## Check the release

Confirm both locations after the tag workflow finishes:

- [GitHub Actions](https://github.com/MDerman/chad-jippity-shulker-sorter/actions)
- [Modrinth](https://modrinth.com/mod/mbKNv70h)

If publishing fails before Modrinth creates the version, fix the repository variable or secret and rerun the failed job. If Modrinth already created the version, publish a new version instead of reusing the tag.
