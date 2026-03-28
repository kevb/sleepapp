# Sleep With Me App

Android app for playing Sleep With Me podcast collections with ambient sounds and a sleep timer.

## Audio content pipeline

Audio is hosted as GitHub release assets on `audio-v2` (not in git). Source audio is scraped from Supercast RSS feeds, concatenated into ~5h chunks, and encoded to opus 32kbps mono.

### Adding a new collection

1. **Find episodes** — use the scraper to search the RSS feeds:
   ```
   cd scraper && python3 scrape.py --search "keyword" --channel main
   ```

2. **Add to download_collections.py** — add an entry to the `COLLECTIONS` dict with search terms, then download:
   ```
   python3 download_collections.py new-collection
   ```
   MP3s go to `scraper/audio/new-collection/`

3. **Encode to opus** — add the collection name/title to `encode.py` `COLLECTIONS` dict, then:
   ```
   python3 encode.py new-collection
   ```
   Outputs `audio/new-collection/opus-32/trackNN.opus` + per-collection manifest

4. **Rebuild combined manifest** — regenerate with all collections (see encode.py `all` or the inline python used previously). Track URLs must be flat: `new-collection-track01.opus`

5. **Upload to existing release** — no need to recreate:
   ```
   gh release upload audio-v2 audio/new-collection/opus-32/track*.opus --clobber
   gh release upload audio-v2 manifest.json --clobber
   ```
   Existing tracks are untouched; only new files + manifest are uploaded.

6. **App update** — only needed if manifest URL/tag changes. If still on `audio-v2`, the app picks up new collections automatically on next manifest refresh.

### Local files are disposable

The `audio/` and `scraper/audio/` directories are gitignored. Once uploaded to a GitHub release, local copies can be deleted. The scraper can re-download from RSS feeds and re-encode if needed.

### Supercast RSS feeds (private, authenticated via URL token)

- Main feed: `https://feeds.supercast.com/feeds/1AFFdS7EquDarYJyDVtPt9F5`
- Story Only: `https://feeds.supercast.com/feeds/KeYsC6GZnoG99uLEYw15L6hs`
- All Intro / All Nights: `https://feeds.supercast.com/feeds/mLzj6wcTSiSRzAwohtdtJ8Lt`
- Bonus: `https://feeds.supercast.com/feeds/UyqXHpqmwBUC7sJ2Gr55XV6x`

## Project structure

- `app/` — Android app (Kotlin, Jetpack Compose, ExoPlayer)
- `scraper/` — Python scripts for downloading and encoding audio (gitignored)
- `audio/` — Encoded opus files and manifests (gitignored)
