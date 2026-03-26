# Sleep With Me — Android Audio Player

A purpose-built sleep audio player for Android. Audio content streamed from Cloudflare R2, organized into collections. Designed for one thing: helping you fall asleep.

## Core Concept

- Audio content hosted on Cloudflare R2, discovered via a JSON manifest
- Plays sequentially through tracks in a collection, like an audiobook
- Remembers playback position per folder — pick up where you left off
- Sleep timer is always on and always visible
- Shake to extend — the primary interaction while half-asleep

## Features

### Playback
- Sequential track playback within a folder
- Persists position (folder, track, timestamp) across app restarts
- ExoPlayer for audio, supports Opus format
- Foreground service for background playback

### Sleep Timer (Always On)
- Countdown timer displayed prominently on screen
- Default: 30 minutes (configurable)
- When timer expires:
  - Volume fades down gradually over ~2-3 minutes
  - Once silent, playback pauses
  - Position is saved

### Shake to Extend
- Gentle shake resets the sleep timer back to full duration
- If volume fade is in progress: resets timer AND restores volume
- If playback stopped within last N minutes: resumes playback and resets timer
- Custom implementation using `TYPE_LINEAR_ACCELERATION` sensor
- Low threshold tuned for gentle/sleepy shakes, with debounce to avoid false triggers from rolling over

## Audio Format

- Format: Opus at 32 kbps
- ~670 MB projected for 49 hours of content
- Source files: MP3 at 64 kbps, converted with ffmpeg

## Content Hosting — Cloudflare R2

Audio files stored in a Cloudflare R2 bucket with public access. No server, no egress fees.

Bucket structure:
```
sleepwithme/
  manifest.json
  snore-trek/
    track01.opus
    track02.opus
    ...
```

### Manifest

The app hits a single known URL to discover all content:
`https://pub-ba3d7129166d4c409216692fa86cc9d6.r2.dev/manifest.json`

```json
{
  "version": 1,
  "collections": [
    {
      "id": "snore-trek",
      "title": "Snore Trek",
      "description": "~49 hours of sleep audio",
      "tracks": [
        {
          "id": "track01",
          "title": "Track 1",
          "url": "snore-trek/track01.opus",
          "duration_secs": 18490
        },
        {
          "id": "track02",
          "title": "Track 2",
          "url": "snore-trek/track02.opus",
          "duration_secs": 16092
        }
      ]
    }
  ]
}
```

- `url` values are relative to the manifest base URL
- App caches the manifest locally and refreshes periodically
- New collections = just add files to R2 and update the manifest

## Tech Stack

- **Language:** Kotlin
- **Audio:** ExoPlayer (Media3) — streams from R2, supports Opus natively
- **Persistence:** SharedPreferences or Room (playback position, settings, cached manifest)
- **Shake detection:** Custom implementation (~50-80 lines), using `Sensor.TYPE_LINEAR_ACCELERATION`
- **Background playback:** Foreground service with notification
- **Hosting:** Cloudflare R2 (free tier: 10 GB storage, 10M reads/month, zero egress)
- **Min SDK:** TBD (likely 26+)

## Content

### Snore Trek (Collection 1)
- 10 tracks, ~49 hours total
- Source: BunnyCDN streams, downloaded as MP3, converted to Opus 24 kbps

## UI

Minimal. One screen:
- Current collection / track info
- Play/pause
- Sleep timer countdown (large, prominent)
- Timer duration adjustment
- Track skip forward/back

No playlist management, no library browsing, no equalizer. The phone goes face-down on the nightstand.

## Stretch Goal: Rain Noise Mix-in

- Looping rain ambient track played simultaneously with main audio
- Mixed on the fly in the app (not baked into the audio files)
- ExoPlayer supports mixing multiple audio sources via `MergingMediaSource` or a parallel `MediaPlayer`/`SoundPool` for the loop
- Volume slider for rain level (independent of main audio)
- Rain audio: a short seamless loop file (~1-2 min), bundled in the app or fetched from R2
- Needs crossfade at loop point to avoid audible seam

## Open Questions

- Grace period duration for shake-to-resume after stop (5 min? 10 min?)
- Volume fade curve and duration
- Shake sensitivity threshold (needs real-device testing)
- Offline caching strategy — download tracks on wifi for offline playback?
