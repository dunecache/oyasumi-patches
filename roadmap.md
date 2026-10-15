# Roadmap — Substack ("Morphe Lite")

Planning and working document for this repository. This file supersedes the old
`todo.md`.
Nothing here is implemented, fingerprinted, or device-verified until it lands in
`reference/NOTES.md` and the phase lists below. Do not treat any item as a
verified anchor: per `AGENTS.md`, every class, method, literal, and instruction
index must come from real disassembly of the pinned reference build.

Identity: **Morphe Lite** — not an APK, a patch collection built around
one sentence: *use the app's useful functionality without wasting
bandwidth, battery, storage, or attention.* For Substack it means read +
communicate without turning it into a media/social-feed app. Deliberately
beyond "remove ads + trackers."

Status legend for every phase list: `[ ]` unstarted, `[~]` anchor located but not
implemented, `[x]` implemented and applied to the pinned build. Only `[x]` after
the changed flow is exercised on a device; "applies cleanly" is weaker than that
and is called out per item.

## Pinned targets

| App | Package | Version | Version code | Format | Cache |
| --- | --- | --- | --- | --- | --- |
| Substack | `com.substack.app` | 3.7.2 | 109778 | APKM (split) | `~/apks/com.substack.app/3.7.2/` (`base.apk` + splits, `manifest.json`, `tools/`) |

Substack has **no** reference notes and **no** patches yet — every
Substack item below is feasibility-unknown until the DEX is mapped.

The shipped targets (ADM, 1DM, Djezzy, Truecaller) are documented in
`reference/NOTES.md` and listed in `README.md`; each is declared against the exact
build its fingerprints were derived from.

## Hard constraints already on record

Do not re-litigate these, design around them:

- One patch per change, one patch per commit. Anything that would become a
  shared settings-toggle dependency must be resolved before writing
  settings-gated patches, not during.
- Gradle build, bundle application, and device testing are not tracked as
  patches: the `app.morphe.patches` plugin `1.3.4` registry returns `401` for
  the available token, so compilation is delegated to CI.
- Bundled native libraries, offline feed assets, and the Firebase/WorkManager
  infrastructure are out of scope unless a patch forces the question.

## Substack roadmap

Everything here is blocked on Phase 0. No Substack fingerprint may be
written until then.

### Phase 0 — Pin, decompile, map (mandatory)

- [ ] Record package/version/sha in `reference/NOTES.md`:
  `com.substack.app`, 3.7.2, versionCode 109778, APKM split,
  `base.apk` sha256 `97aa805c…70c365` (from `manifest.json`).
- [ ] Declare the target in `patches/` constants (APKM, one entry —
  the patch surface is `base.apk`, shared across ABI splits).
- [ ] Map the surfaces every later item assumes: video/audio player
  + autoplay gate, image pipeline + rendition selection, feed vs.
  Notes vs. Chat navigation, notification categories, download/cache
  paths (note `FOREGROUND_SERVICE_MEDIA_*`, `RECORD_AUDIO`, `CAMERA`
  permissions — media/chat surface is real).
- [ ] Kill-or-confirm list: is there a client-side autoplay flag, a
  prefetch switch, a notification category enum, an offline store?
  Each S1–S4 item below starts as a spike against this map.

### S1 — Data Saver flagship (⭐⭐⭐⭐⭐)

Ship as separate narrow patches; keep Chats/DMs fully functional:

1. **Tap-to-play video** (never download/play until requested).
2. **Tap-to-play audio** (podcasts; same seam family as video).
3. **Disable media preloading** (video/audio/images/feed +
   recommendation prefetch — split per surface, one patch each).
4. **Image quality selector** (Cellular Original/Med/Low, Wi-Fi
   Original/High — only if client-side rendition selection exists).
5. **Wi-Fi-only media / don't cache video** (storage + bandwidth;
   verify cache ownership before claiming it).

### S2 — Reading (⭐⭐⭐⭐–⭐⭐⭐⭐⭐)

6. **Reader mode** (author/title/article/images only) — highest risk:
   needs a content-extraction seam; spike before committing. If the
   article view is server-rendered web content, this may be infeasible
   as a DEX patch.
7. **Subscriptions-only feed** (no algorithmic discovery) — same
   spike class as item 7's recommendation feed; commit only on a found seam.
8. **Hide Notes / Hide Recommendations** (standalone surface-hiding
   patches; the "build your own Substack" set with Hide Chat, Hide
   Video, Hide Podcasts — each one patch, each independently
   revertible).

### S3 — Notifications (⭐⭐⭐⭐–⭐⭐⭐⭐⭐)

9. **Essential-notifications-only** (keep DMs/chat replies/mentions;
   kill likes/restacks/recommendations/discovery). Depends on finding
   the category enum or channel mapping — spike first.
10. Never cripple communication to optimize consumption: DMs/Chat are
    out of scope for every saver patch. State this in each patch
    description.

### S4 — Offline (⭐⭐⭐⭐)

11. **Download article for offline** (text + necessary images +
    metadata; video/audio only on explicit request).
12. **Auto-download subscriptions** (Wi-Fi-only, storage cap) — needs
    an existing offline store to hook; if none exists, this is a
    feature build, not a patch, and is out of scope.

### S5 — UI

13. **AMOLED black**, a resource/theme edit and a new surface.

Explicitly deferred until spikes resolve: chronological/following-only
feeds, full reader mode, offline engine, notification channels (if no
client-side seam exists, these are not patches).

## v1 scope (small, extremely useful — ship these first)

Substack v1 (all post-Phase-0): Tap-to-play video · Disable media
preloading · Reader mode *(only if spike hits)* · Hide Notes · Hide
recommendations · Subscriptions-only feed *(only if spike hits)* ·
Essential notifications · Offline articles · Wi-Fi-only media.

## Working agreements (unchanged)

- Behavior spec → pin target → locate in disassembly → NOTES entry →
  narrowest fingerprint → smallest edit → compile → apply → device
  test. One patch per commit.
- Fingerprints pin stable strings/flags/types/instruction shape, never
  obfuscated names. Vendor SDK classes that ship obfuscated (`AFa1tSDK`
  and similar) are the cautionary case; prefer the pattern of an
  unobfuscated class plus a unique literal.
- `const/16` for `GONE` (`0x8`); name invoke arity from the descriptor;
  never `replaceInstructions` with a width-mismatched nop block
  (both crash classes are documented in NOTES).
- Verification ladder per patch: compiles in CI → fingerprint resolves
  on device → runtime effect observed. Only the last earns `[x]` for a
  *behaviour*.