# TikTok Video Sticker Maker
## MVP + Product Requirements Document (PRD)

---

# 1. Product Overview

## Product Name
TikTok Video Sticker Maker

## Product Summary
An Android application that allows users to:

1. Import TikTok videos via share or link
2. Download watermark-free video streams
3. Trim, crop, and edit short clips
4. Convert clips into WhatsApp stickers, GIFs, or MP4 loops
5. Export sticker packs directly into WhatsApp

The system uses a **hybrid architecture**:

- Android app handles all editing, processing, and exports
- Cloud backend (Render-hosted FastAPI) resolves TikTok URLs and returns direct video streams

---

# 2. Goals

## Primary Goals

- Enable fast conversion of TikTok videos into WhatsApp stickers
- Provide lightweight editing tools (trim + crop)
- Automate sticker pack creation for WhatsApp
- Keep backend minimal and cost-efficient
- Ensure smooth on-device video processing

## Secondary Goals

- GIF export support
- Offline editing after download
- Fast share-to-app workflow
- Simple UX optimized for low-end Android devices

---

# 3. Non-Goals

The MVP will NOT include:

- User accounts or authentication
- Cloud storage or syncing
- Social feed or community features
- AI-powered editing (background removal, face tracking)
- Advanced multi-track video editing
- iOS support
- Sticker marketplace

---

# 4. Final System Architecture (Updated)

## High-Level Architecture

```text
Android App (Kotlin)
│
├── Share Intent Receiver
├── Video Downloader (direct CDN fetch)
├── Video Editor (trim + crop)
├── FFmpeg Processing Layer
├── Sticker Generator (WebP)
├── GIF Exporter
├── WhatsApp Integration Module
└── Local Storage Manager

Backend (Render - FastAPI)
│
├── TikTok URL Resolver
├── yt-dlp integration
├── Returns direct MP4 stream URL
└── Lightweight API layer
```

---

# 5. Recommended Tech Stack

## Android App

### Language
- Kotlin

### UI
- Jetpack Compose

### Video Processing
- FFmpegKit
- Android Media3 / ExoPlayer

### Networking
- Retrofit
- OkHttp

### Architecture
- MVVM + Repository Pattern
- Hilt (Dependency Injection)
- Kotlin Coroutines

### Local Storage
- Room Database

---

## Backend (Render Deployment)

### Hosting
- Render Web Service (Docker-based)

### Framework
- FastAPI

### Language
- Python

### Video Resolver
- yt-dlp

### System Tools
- ffmpeg

### Deployment
- Docker + GitHub auto-deploy

---

# 6. System Workflow

```text
User opens TikTok
        ↓
User taps Share
        ↓
Selects "Sticker Maker App"
        ↓
Android receives TikTok URL
        ↓
App sends URL to Render backend
        ↓
Backend resolves direct MP4 stream
        ↓
App downloads video directly from CDN
        ↓
User trims & crops clip
        ↓
FFmpeg converts clip to WebP
        ↓
User exports:
    • WhatsApp Sticker Pack
    • GIF
    • MP4 Loop
```

---

# 7. MVP Scope

## Feature 1 — TikTok Import

### Description
Users import TikTok videos via share intent or paste link.

### Requirements
- Android must support ACTION_SEND intent
- Detect TikTok URLs automatically
- Validate input URLs

---

## Feature 2 — URL Resolution (Render Backend)

### Description
Backend resolves TikTok URL into direct MP4 stream.

### Requirements
- FastAPI endpoint `/resolve`
- Uses yt-dlp to extract stream
- Returns direct CDN URL (not hosted file)

### Example Response
```json
{
  "video_url": "https://cdn.tiktok.com/...",
  "title": "video title"
}
```

---

## Feature 3 — Video Download

### Description
Android downloads video directly from CDN URL.

### Requirements
- Background download support
- Progress indicator
- Local file storage

---

## Feature 4 — Clip Editor

### Description
User selects portion of video for sticker creation.

### Requirements
- Trim start/end selection
- Preview playback
- Max clip duration (default 6–8 seconds)

---

## Feature 5 — Crop Tool

### Description
Crop video into sticker format.

### Requirements
- Square crop (1:1)
- Drag + zoom interface
- Real-time preview

---

## Feature 6 — Sticker Export

### Description
Convert clip into WhatsApp-compatible sticker.

### Requirements
- Export animated WebP
- Resize to 512x512
- Optimize FPS (10–15 FPS)
- Generate sticker metadata
- Register sticker pack in WhatsApp

---

## Feature 7 — GIF Export

### Description
Allow users to export clips as GIFs.

### Requirements
- Convert video to GIF via FFmpeg
- Shareable output file

---

# 8. Backend Design (Render)

## Role of Backend

The backend ONLY:
- resolves TikTok URLs
- returns direct video stream links

It does NOT:
- store videos
- process video editing
- serve media files

---

## API Endpoint

### POST /resolve

Request:
```json
{
  "url": "https://www.tiktok.com/..."
}
```

Response:
```json
{
  "video_url": "https://cdn.tiktok.com/video.mp4",
  "title": "Sample Video"
}
```

---

## Backend Flow

```text
Receive TikTok URL
        ↓
yt-dlp extracts metadata
        ↓
Resolve direct MP4 stream
        ↓
Return URL to Android app
```

---

# 9. Android App Architecture

## Modules

### app
Main UI and navigation

### downloader
Handles video downloads

### editor
Trim + crop logic

### ffmpeg
Media processing layer

### stickers
Sticker generation + WhatsApp integration

### data
Room database + repositories

### network
API communication with Render backend

---

# 10. Video Processing Pipeline

```text
MP4 Downloaded
        ↓
Trim clip (max 6–8s)
        ↓
Crop to square
        ↓
Resize to 512x512
        ↓
Reduce FPS (10–15)
        ↓
Convert to WebP
        ↓
Compress
        ↓
Export sticker
```

---

# 11. FFmpeg Responsibilities

- Trim video segments
- Resize video to sticker dimensions
- Convert MP4 → WebP
- Generate GIF output
- Optimize compression

### Example Command
```bash
ffmpeg -i input.mp4 \
-vf "fps=15,scale=512:512:force_original_aspect_ratio=decrease" \
-loop 0 -an output.webp
```

---

# 12. WhatsApp Integration

## Requirements
- WebP animated stickers
- 512x512 resolution
- Sticker pack metadata
- Content Provider integration

## Flow
```text
Generate sticker assets
        ↓
Create metadata.json
        ↓
Register sticker pack
        ↓
Open WhatsApp pack installer
```

---

# 13. Data Storage

## Local Storage Paths

Videos:
```
/app/videos/
```

Stickers:
```
/app/stickers/
```

GIFs:
```
/app/gifs/
```

---

# 14. Database Schema

## videos
| Field | Type |
|------|------|
| id | INTEGER |
| source_url | TEXT |
| local_path | TEXT |
| duration | INTEGER |
| created_at | INTEGER |

---

## stickers
| Field | Type |
|------|------|
| id | INTEGER |
| video_id | INTEGER |
| path | TEXT |
| type | TEXT |
| created_at | INTEGER |

---

# 15. Performance Targets

| Metric | Target |
|------|--------|
| App startup | < 3s |
| Video download | < 15s |
| Sticker export | < 10–15s |
| Crash rate | < 1% |

---

# 16. MVP Feature List

## Must Have
- TikTok share intent
- URL resolution (Render backend)
- Video download
- Trim tool
- Crop tool
- WebP sticker export
- WhatsApp integration

## Nice to Have
- GIF export
- MP4 loop export
- Basic effects

---

# 17. Future Features

- AI background removal
- Face tracking stickers
- Sticker templates
- Cloud backup
- Multi-platform export (Telegram, Instagram)

---

# 18. Deployment Strategy

## Backend
- Render Web Service
- Docker deployment
- GitHub auto-deploy pipeline

## Android
- APK testing phase
- Google Play release (later)

---

# 19. Risks

## TikTok API Changes
- Mitigation: use yt-dlp updates

## Backend downtime
- Mitigation: keep backend stateless

## WhatsApp limitations
- Mitigation: strict optimization of WebP

---

# 20. Monetization

- Ads (interstitial + rewarded export)
- Premium subscription:
  - remove ads
  - unlimited exports
  - higher quality stickers

---

# 21. Success Metrics

- Daily active users
- Stickers created per session
- Share intent usage rate
- Export success rate

---

# 22. Final Recommendation

The best production-ready approach is:

- Render FastAPI backend for URL resolution only
- Kotlin Android app for all media processing
- FFmpegKit for conversion pipeline
- Direct CDN downloads (no backend media storage)

This ensures:
- low cost
- scalability
- fast performance
- maintainable architecture

---

# End of Document

