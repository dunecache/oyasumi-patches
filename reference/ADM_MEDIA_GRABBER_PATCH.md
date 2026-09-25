# ADM browser media grabber patch design

## Scope

This document describes a design for a future Morphe patch targeting the verified ADM 14.0.27 reference APK. It is not an implemented patch and does not claim that every site can be captured.

Reference:

- Package: `com.dv.adm`
- Version: `14.0.27`
- Version code: `140027`
- Source: `~/storage/downloads/1DM/Programs/com.dv.adm_14.0.27-140027_minAPI26(arm64-v8a,armeabi-v7a,x86,x86_64)(nodpi)_apkmirror.com.apk`
- SHA-256: `6f1d3aee879fe58cbd77e8ef01b3ce6e4d3f77aadd3e8276ec8232d0bdf006c1`

The feature should only offer downloading for media that the user is authorized to access. It should not attempt to bypass DRM, license servers, access controls, or encrypted streams.

## Verified browser anchors

The following method paths and behaviors were found in `classes2.dex` for the reference build.

### Primary WebView setup

`Lcom/dv/get/Web;->S2()Landroid/webkit/WebView;` creates and configures the primary browser WebView. It installs:

- `Lcom/dv/get/Web$h;` as the `WebChromeClient`
- `Lcom/dv/get/Web$i;` as the `WebViewClient`
- `La2/m4;` as the long-click listener
- `La2/o4;` as the `DownloadListener`

This is the preferred initialization hook. A media-grabber extension can attach its per-page state after the existing clients and download listener are installed, without replacing them.

### Resource observation

`Lcom/dv/get/Web$i;->shouldInterceptRequest(Landroid/webkit/WebView; Ljava/lang/String;)Landroid/webkit/WebResourceResponse;` receives resource URLs. The method currently performs existing browser/ad-block handling and then delegates to the superclass.

`shouldInterceptRequest(WebView, WebResourceRequest)` delegates to the string overload after reading `WebResourceRequest.getUrl()`.

A capture observer should run at method entry or immediately before the existing return path. It must not consume or replace the response. The request headers should be copied only when needed for a later user-approved download and must never be logged.

### Existing download path

`Lcom/dv/get/Web;->C0(Lcom/dv/get/Web; Landroid/webkit/WebView; Ljava/lang/String;)V` is invoked by the existing `DownloadListener` flow. It launches `Lcom/dv/get/AEditor;` with:

- `android.intent.extra.TEXT`
- a generated filename
- `com.android.extra.filename`
- `ModeEdit`
- `Backup`
- the current WebView cookie string

This is the preferred path for a direct HTTP(S) media URL once the user selects it.

`Lcom/dv/get/Web;->c1(Lcom/dv/get/Web; Landroid/webkit/WebView;)V` is a related stream path. It launches `AEditor` and uses `.mp3` or `.mp4` filename suffixes, `Stream`, backup state, and cookies. It is useful for a direct progressive-media item, but it does not parse HLS manifests or subtitle renditions.

### Existing media preview

`Lcom/dv/get/Web;->n1(I)V` contains a media-preview path that builds HTML containing a `<video>` element with `type="application/x-mpegURL"` and an `<audio>` element with `type="audio/aac"`, then loads the HTML into a temporary WebView.

This proves that ADM has a media preview surface. It does not prove that the preview path can download every media format. It can be reused for a preview action after a URL has been classified, but it should not be treated as a capture implementation.

### File chooser is unrelated

`Lcom/dv/get/Web$h;->onShowFileChooser(...)` handles browser file uploads, camera intents, and `ACTION_GET_CONTENT`. It is not a media-grabber hook.

### Menu and button hooks

The browser menu is implemented by:

- `Lcom/dv/get/Web;->onCreateOptionsMenu(Landroid/view/Menu;)Z`
- `Lcom/dv/get/Web;->onOptionsItemSelected(Landroid/view/MenuItem;)Z`

The menu method already uses `Menu.add`, `MenuItem.setIcon`, and `setShowAsAction`. The selection method starts with `MenuItem.getItemId()` and dispatches through the existing item-ID branches.

A new button should be added as a separate menu item with an ID allocated so it cannot collide with the existing resource IDs. The handler should be invoked before the existing selection branches consume the event, and it should return only after the media dialog has handled the item.

## Proposed architecture

### 1. Per-page state

Use a small extension-owned state object, proposed as `AdmMediaGrabberState`, rather than adding fields to the obfuscated `Web` class. Store:

- the current page URL
- the active WebView identity
- normalized candidate URLs
- MIME type and extension hints
- whether the candidate came from a resource request, DOM inspection, or the download listener
- request headers and cookies only in memory until the user confirms a download

Clear or scope the state when the page changes, when the WebView is destroyed, and when a new tab becomes active. Do not use a process-global list that mixes media from different pages.

### 2. Network observer

Add an observer at the verified `Web$i.shouldInterceptRequest` hook.

For each URL:

1. Ignore `data:`, `about:`, `javascript:`, and other non-network schemes.
2. Normalize the URL without changing its query string.
3. Classify likely media by URL extension, response MIME type when available, and HLS/WebVTT markers.
4. Deduplicate candidates by URL and page.
5. Store only the URL and minimal metadata.
6. Return the original response unchanged.

The observer may run on a WebView worker thread. It must marshal any state mutation or UI notification to the main thread.

Suggested initial classification:

- Progressive video: `video/mp4`, `video/webm`, `video/x-matroska`, `video/quicktime`, and corresponding extensions
- Progressive audio: `audio/mpeg`, `audio/aac`, `audio/ogg`, `audio/flac`, `audio/webm`, and corresponding extensions
- HLS: `.m3u8` and `application/vnd.apple.mpegurl` or `application/x-mpegURL`
- Subtitles: `text/vtt`, `application/x-subrip`, `.vtt`, and `.srt`

Extension matching must be a hint, not the only test. Servers frequently use query strings or incorrect MIME types.

### 3. DOM and performance inspection

Network interception alone misses media created through JavaScript, Media Source Extensions, or URLs assembled at runtime. As an optional second phase, inject a small JavaScript collector after page load and collect only metadata:

- `video.currentSrc`
- `video.src`
- `source` elements associated with `video` and `audio`
- `track.src` and `track.kind`
- matching `performance.getEntriesByType('resource')` URLs

The collector should return URLs and MIME/element metadata, not file contents. It should not attempt to read encrypted media buffers.

A JavaScript bridge is required only if the collector is used. It should be installed for the current WebView and removed with the page state. DOM inspection cannot recover a `blob:` object as a normal downloadable URL.

### 4. Button and selection dialog

The button should be a browser action, not a global floating overlay. The smallest UI design is:

1. Add a `Media grabber` item in `onCreateOptionsMenu`.
2. Give it a dedicated non-colliding item ID and a stable title.
3. In `onOptionsItemSelected`, open an extension-owned dialog.
4. Show the current page and a list of captured video, audio, HLS, and subtitle candidates.
5. Let the user select one or more items and confirm the download.
6. Show progress and errors without blocking the WebView thread.

The dialog should include an empty state explaining that no network media candidates were observed. It should not automatically start downloads when the page loads.

### 5. Direct media downloads

For a selected direct HTTP(S) media URL, reuse the existing `Web.C0` or `AEditor` flow where possible. Preserve:

- the original URL
- the WebView cookie string
- the current user agent and other required request headers
- a sanitized filename
- the selected output directory

Do not pass an HLS manifest to a downloader that expects one progressive file. HLS requires a separate resolver.

### 6. HLS handling

For an `.m3u8` candidate, an extension-owned HLS resolver should:

1. Fetch the master or media playlist using the captured request context.
2. Parse variant streams and choose a user-selected quality.
3. Follow the selected media playlist and segment URIs.
4. Preserve segment order and retry transient failures.
5. Write a playable local playlist or use a downloader that supports HLS.
6. Keep subtitle and audio rendition metadata separate.

A master playlist can reference video, audio, and subtitle renditions through `#EXT-X-MEDIA` and `#EXT-X-STREAM-INF`. The resolver must resolve relative URIs against the playlist URL and must not assume that every segment is a direct file.

### 7. Subtitle handling

For a direct subtitle URL, download it as a separate file with the original language or track label when available.

For HLS subtitles:

- inspect `#EXT-X-MEDIA` entries with `TYPE=SUBTITLES`
- resolve the subtitle playlist URI
- download the subtitle segments
- convert segmented WebVTT to one valid WebVTT file, or preserve the playlist structure if the player requires it
- save the subtitle beside the video using a matching basename

Do not treat a subtitle URL as a video URL. A page can expose multiple language tracks for the same video.

## Patch implementation sequence

### Phase A: reference and fingerprints

Before writing the patch, capture the exact instruction output for:

- `Web.S2()Landroid/webkit/WebView;`
- `Web$i.shouldInterceptRequest(WebView,String)WebResourceResponse;`
- `Web.onCreateOptionsMenu(Menu)Z`
- `Web.onOptionsItemSelected(MenuItem)Z`
- `Web.C0(Web,WebView,String)V`
- `Web.c1(Web,WebView)V`

Use the exact class descriptors, return types, parameter lists, and the verified `setWebViewClient`, `setWebChromeClient`, `setDownloadListener`, `getItemId`, `AEditor`, and `CookieManager` calls as anchors. Do not rely on an obfuscated method name alone.

### Phase B: extension-only observer

Implement the extension and attach it from `S2` without changing the existing response behavior. Verify that ordinary browsing, popups, cookies, downloads, and ad blocking still work.

### Phase C: direct media candidates

Add URL classification and a read-only candidate dialog. Test a direct MP4, WebM, MP3, and subtitle file before implementing HLS.

### Phase D: download action

Route selected direct URLs through the existing `AEditor` path. Verify filenames, cookies, duplicate handling, cancellation, and output directories.

### Phase E: HLS and subtitles

Add the playlist resolver and segmented subtitle writer. Test master playlists, media playlists, relative segment URLs, alternate audio, WebVTT subtitles, SRT subtitles, redirects, cookies, and failed segments.

### Phase F: release patch

Only then add the user-facing Morphe patch declaration, extension packaging, and release metadata. Keep the observer disabled by default until device validation is complete if the capture surface is not yet stable.

## Format support matrix

| Source | Expected behavior |
|---|---|
| Direct MP4/WebM/M4V | Capture URL and offer the existing downloader path |
| Direct MP3/AAC/OGG/FLAC | Capture URL and offer the existing downloader path |
| Direct WebVTT/SRT | Capture and save as a separate subtitle file |
| HLS master playlist | Resolve variants, segments, audio, and subtitles |
| HLS media playlist | Download the selected rendition and referenced segments |
| `blob:` or MSE media | Report metadata only; no normal file URL is available |
| DRM/EME encrypted media | Do not capture or bypass |
| Cross-origin resource without usable headers | Show the candidate but report that authentication may be required |

## Validation checklist

- Browse ordinary pages with no media and confirm no UI or download side effects.
- Confirm the observer does not change response bodies, status codes, or ad-block behavior.
- Test direct video, direct audio, WebVTT, and SRT downloads.
- Test HLS master and media playlists with relative and absolute segment URLs.
- Test separate subtitle and alternate-audio renditions.
- Test pages that create media through JavaScript after page load.
- Test redirects, cookies, user-agent requirements, and authenticated requests without logging secrets.
- Test popup WebViews created by `Web$h.onCreateWindow`.
- Test cancellation, duplicate candidates, filename sanitization, and storage exhaustion.
- Confirm DRM, encrypted, and `blob:` cases fail safely.
- Compile the patch and apply the generated bundle to the pinned APK before publishing.

## Open questions

- Does the pinned `AEditor` implementation accept HLS manifests directly, or must the extension always materialize a local playlist?
- Does ADM preserve enough request headers for authenticated media, or is the WebView cookie string the only reusable context?
- Which subtitle formats beyond WebVTT and SubRip need conversion?
- Should a media grabber be a user setting, a per-page action, or both?
- How should a selected item be named when the URL has no extension?
- Can the extension safely allocate a menu ID without changing the app's resource table?

No media-grabber patch should be declared compatible with another ADM version until these anchors and the extension boundary are rechecked against that exact APK.
