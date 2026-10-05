# Roadmap — Pinterest + Substack ("Morphe Lite")

Planning and working document for this repository. This file supersedes the old
`todo.md`: the Pinterest working plan now lives here alongside the roadmap.
Nothing here is implemented, fingerprinted, or device-verified until it lands in
`reference/NOTES.md` and the phase lists below. Do not treat any item as a
verified anchor: per `AGENTS.md`, every class, method, literal, and instruction
index must come from real disassembly of the pinned reference build.

Identity: **Morphe Lite** — not an APK, a patch collection built around
one sentence: *use the app's useful functionality without wasting
bandwidth, battery, storage, or attention.* For Pinterest that means
browse + save Pins without the video/recommendation/data waste. For
Substack it means read + communicate without turning it into a
media/social-feed app. Deliberately beyond "remove ads + trackers."

Status legend for every phase list: `[ ]` unstarted, `[~]` anchor located but not
implemented, `[x]` implemented and applied to the pinned build. Only `[x]` after
the changed flow is exercised on a device; "applies cleanly" is weaker than that
and is called out per item.

## Pinned targets

| App | Package | Version | Version code | Format | Cache |
| --- | --- | --- | --- | --- | --- |
| Pinterest | `com.pinterest` | 14.38.0 | 14388010 | APK (standalone re-pack) | `~/apks/com.pinterest_14.38.0-…apk`, dex under `~/apks/dex_files/pinterest_v14.38.0/` |
| Substack | `com.substack.app` | 3.7.2 | 109778 | APKM (split) | `~/apks/com.substack.app/3.7.2/` (`base.apk` + splits, `manifest.json`, `tools/`) |

Pinterest findings live in `reference/NOTES.md` under
`# Pinterest 14.38.0 reference notes`; the target declaration is
`app/pinterest/patches/shared/Constants.kt`.
Substack has **no** reference notes and **no** patches yet — every
Substack item below is feasibility-unknown until the DEX is mapped.

## Where we stand

Pinterest ships 10 named patches plus one internal patch that is not in the
Manager's list, and one more written but not yet run on a device. Two further
patches were written and reverted; both retractions are recorded in
`reference/NOTES.md`. All are
build-verified; see the table for how far each has been taken.

| # | Patch | Default | What it does | Anchor | Runtime effect |
| --- | --- | --- | --- | --- | --- |
| 1 | Neutralize advertising ID | on | Returns a random advertising ID instead of the real one | `Lvi2/b;->b(Context)` (Pinterest's own wrapper) → extension | unverified |
| 2 | Disable AppsFlyer tracking | on | Neutralizes the AppsFlyer attribution SDK | `com.appsflyer.internal.AFa1tSDK`, six methods | unverified |
| 3 | Disable Google Engage | on | Stops Pinterest publishing user actions to Google | `GoogleEngageBroadcastReceiver.onReceive` | unverified |
| 4 | Disable email confirmation dialog | on | Hides the "Confirm your email" prompt and related screen | `android_settings_email_verification` experiment gate, polarity at `Lvj1/u0;.F1(Z)V` | unverified |
| 5 | Hide Search nav button | off | Hides the search button in the bottom navigation bar | `FloatingBottomNavBar.Q1` | unverified |
| 6 | Hide Notifications nav button | off | Hides the notifications button in the bottom navigation bar | `Lde0/a;->NOTIFICATIONS` → `FloatingBottomNavBar.Q1` | unverified |
| 7 | Hide comments | off | Hides the comments button on a pin | `UnifiedPinActionBarView` three-argument constructor | unverified |
| 8 | Morphe settings entry | on | Adds the "Morphe" row to Account Settings, opening `morphe://settings` | `labs/s;.invoke` builder, external-link row resolved from the dex | unverified |
| 9 | Morphe settings screen (label) | on | Renames the reused row string to "Morphe" in every shipped language | `settings_menu_teen_safety_resources`, 48 `res/values*` dirs | unverified |
| 10 | Morphe settings screen (manifest) | on | Registers the Morphe settings activity with a `morphe://` intent-filter | `AndroidManifest.xml` `<application>` | unverified |
| 11 | Hide Create nav button † | off | Hides the create (+) button in the bottom navigation bar | `FloatingBottomNavBar.Q1`, CREATE tab — the same seam as the search and notifications patches | unverified |
| 12 | Force original image download † | off | Prefers the `originals` rendition over stock's 736x-first choice | `Lvu2/d1;.b()`, the only reader of the 736x field in the APK | unverified |
| — | Morphe runtime state (internal) | n/a | Records the version being patched so patches can branch on it; never listed, never toggled | `packageMetadata.versionName` | n/a |

† Written and statically verified, but never run on a device. It is the only
surviving patch from the three written this session, because it edits a method
that *returns* the chosen value rather than one that feeds a boolean onward.

The settings harness was rebuilt rather than deferred: there is no
`PreferenceFragment` to hook (Phase 0), so the settings screen is an injected
extension activity reached through Pinterest's own external-link row, and the
UI-hiding patches read in-app keys from it instead of Manager-gated settings.

### Device verification status

The full bundle applies cleanly to Pinterest 14.38.0 on device: every fingerprint
resolves, no `VerifyError`, no crash. That proves the anchors and the smali
encodings, including the first `extensions/` wiring in this repo.

What application does **not** prove is runtime effect. Still unverified: that the
identifier actually comes back random, that AppsFlyer and Engage transmit
nothing, and that the resource patches produce a valid `resources.arsc` and
manifest. Those need a behavioral pass, not just a clean install.

Substack: nothing. Phase 0 below is mandatory before any patch.

## Hard constraints already on record

Do not re-litigate these, design around them:

- Pinterest 14.38.0 has **no global ad kill-switch literal**
  (`ads_enabled`, `enable_ads`, `hide_ads`, `no_ads`, `ad_free`,
  `adBlock` all return nothing; `ad_block` occurs once inside
  obfuscated GMS internals). Ad/promo removal is several narrow edits,
  not one toggle (Phase 2).
- Media handling may cross native code (`libx_media_handler.so`,
  `libzune_jpeg-*.so`, `libquikklycore.so`, `libcronet`). Any
  quality/prefetch/autoplay patch must first establish the save path
  is Java/DEX-side before assuming a DEX-only edit works. Partly
  answered: the pin **download/save** path is entirely DEX-side (Phase 3),
  but playback, decoding and rendition selection are still unexamined.
- Pinterest's inlined resources are not reachable through an `R$string;`
  class. Field references to `R$` in this APK only ever hit
  `net/quikkly/android/R$string;`; the app's own ids are `sget` constants
  in per-package holders (`Lgi0/b;`, `Lpf0/c;`, `Lnm0/d;`, …). Searching
  for a resource by name means searching field *names*, which is what
  `.scratch/fielduse.py` is for.
- One patch per change, one patch per commit. Anything that would become a
  shared settings-toggle dependency must be resolved before writing
  settings-gated patches, not during.
- Gradle build, bundle application, and device testing are not tracked as
  patches: the `app.morphe.patches` plugin `1.3.4` registry returns `401` for
  the available token, so compilation is delegated to CI.
- Bundled native libraries, `real_feed_*.jsonl` offline feed assets, and the
  Firebase/WorkManager infrastructure are out of scope unless a patch forces
  the question.

## Pinterest working plan

**These items were transcribed from an existing patch bundle that targets a
different Pinterest build.** They describe intended behaviour, not verified
anchors. Per `AGENTS.md`, no fingerprint may be written from these
descriptions: every class, method, literal and instruction index has to come
from real output of *this* reference DEX. Treat each line as a behaviour spec
and start from the disassembly.

**Item names may not match this build.** Feature names, menu labels, string
keys and settings keys are all version-sensitive and Pinterest renames them
frequently.

### Phase 0 — Harness (dependency for 6 items)

**Deferred as a `PreferenceFragment` port — blocked on the app, not on effort.**
The original bundle implements these four on top of `androidx.preference`.
Pinterest 14.38.0 has **zero** `androidx.preference` classes and zero references
to the package, no `res/xml` preference screen, and a Compose-based settings UI.
There is no `PreferenceFragment` to hook, so those patches cannot work as
described regardless of how much framework is ported. Verified in
`reference/NOTES.md`.

The harness was replaced instead: an injected `MorpheSettingsActivity` extension,
declared in the manifest, reached through Pinterest's own external-link settings
row. Same mechanism as `browzomje/browzomje-patches`, re-derived for this build.
Every patch below that would have been settings-gated reads its key from that
screen.

- [x] **Morphe runtime state** — tracks the current Pinterest version so patches can react to
  per-build target changes.
  - Internal `bytecodePatch`, so it never appears in the patch list and nothing can toggle it;
    `PatchLoader` only loads named patches. All Pinterest patches declare
    `dependsOn(versionCheckPatch)`.
  - Records `packageMetadata.versionName`. The official bundle compares versions with Kotlin's
    string `>=`, which inverts four of eight checks against Pinterest's real version numbers
    (`14.38.0` sorts below `14.9.0`). Replaced with a component-wise numeric comparison; the case
    table is in `reference/NOTES.md`.
  - Be clear about what this does and does not do: it does **not** make a fingerprint survive an
    app update. It makes a patch able to tell which version it is running against, which is the
    prerequisite for noticing that a fingerprint has stopped matching.
- [x] **Morphe settings entry** — adds the "Morphe" row to Account Settings.
  - Reuses Pinterest's own external-link row (single-`String` constructor) with a
    `morphe://settings` URL, which fires `ACTION_VIEW` into the declared intent-filter. The
    obfuscated row class is resolved from the dex at patch time, never pinned.
  - On 14.38.0 the builder is `labs/s;.invoke` (17 rows, header `f1` built 4x, link `k1` built
    1x), identified by shape, not name. Anchor is the most-frequent `<init>(int)`; the list
    register comes from the first `List.add` after it.
  - Runtime effect unverified: the row appearing and the activity opening have not been
    exercised on a device.
- [x] **Morphe settings screen (label)** — provides the string for the "Morphe" entry.
  - Written as a `resourcePatch` that **renames the reused row's own string resource**
    (`settings_menu_teen_safety_resources`) to `Morphe`, rather than adding a new `<string>`.
    A new resource ID was rejected: it is not in the original ARSC and can fail the
    repackaging step, while rewriting existing text cannot.
  - Walks all 48 `res/values*` directories derived from the reference `resources.arsc`, so a
    non-English device does not keep showing the original label. Missing directories are
    skipped, and the patch fails loudly if the string is found nowhere.
  - Side effect accepted: the real "Teen safety resources" row is rare, but where it does appear
    it will also read "Morphe".
  - Runtime effect unverified: resource recompilation against the real APK has not been run.
- [x] **Morphe settings screen (manifest)** — registers the settings activity in the manifest.
  - Declares `app.oyasumi.extension.MorpheSettingsActivity` with a `morphe://` intent-filter
    (`VIEW` + `DEFAULT` + `BROWSABLE`), `exported=true`, `android:label="Morphe"`, and
    `Theme.Material3.DayNight.NoActionBar`.
  - The Material 3 theme is deliberate and load-bearing: inheriting
    `Theme.Pinterest.NoActionbar` makes the first framework widget construction crash while
    resolving Pinterest design-token attributes that only exist inside Pinterest's own overlays.
  - The activity itself now exists (`extensions/.../MorpheSettingsActivity.java`). An earlier
    revision declared an entry with no code behind it; that gap is closed.

### Phase 1 — Privacy and safety toggles

Lowest risk, and items 6/7 are described as safe and recommended to keep on. Good first
implementations because they are narrow, self-contained edits.

- [x] **Neutralize advertising ID** — returns a random advertising ID instead of the real one.
  - Written. `Lvi2/b;->b(Context)` is Pinterest's own wrapper; the fetch is redirected to an
    extension method while the app's own caching is left intact. Fingerprint verified to match
    exactly one method in the reference.
  - The transcribed `ad_tracking` literal does not exist in this build; the description's
    "random/empty" behaviour is what was implemented.
  - Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified: any device run, and `extensions/` is the first extension module in this repo,
    so its wiring is unproven here.
- [x] **Disable AppsFlyer tracking** — neutralizes the AppsFlyer attribution SDK.
  - Written. Six methods on the single concrete implementation
    (`com.appsflyer.internal.AFa1tSDK`, the only class extending `AppsFlyerLib`) are replaced with
    a type-correct immediate return. Twelve members are called from five Pinterest call sites;
    six transmit, and those six are the patch.
  - Skipping `init` alone would have crashed: `getAppsFlyerUID` dereferences the SDK core with no
    null check, and Pinterest calls it. Recorded in `reference/NOTES.md`.
  - Main known fragility: the whole patch hangs off one **obfuscated** class name
    (`AFa1tSDK`), which changes when AppsFlyer updates. This is the strongest argument for the
    version-state work in Phase 0.
  - Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified: any device run.
- [x] **Disable Google Engage** — stops Pinterest publishing user actions to Google.
  - Written. `GoogleEngageBroadcastReceiver.onReceive` is the only enqueue point for
    `GoogleEngageWorker`, so suppressing it stops publishing entirely.
  - First patch in the bundle with **no** obfuscated anchor: the class is not obfuscated and the
    WorkManager job name occurs exactly once in the APK.
  - Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified: any device run.
- [x] **Disable Google Engage worker** — covered by the receiver patch, no separate patch written.
  - Verified: the only scheduling site in the APK is the receiver's `onReceive` (sole
    `const-class` for the worker). The two other references are the WorkManager `WorkerFactory`
    rebuild (`Lpr/n9;`) and the injected holder (`Lpo0/f;`); neither schedules.
  - With the receiver suppressed no job is ever enqueued, so the worker never runs. Writing a
    second patch against its R8-renamed `g()` would add fragility for no effect.
  - Edge case noted, not handled: a job enqueued *before* the user installs the patched build
    would still run once. No action — it cannot re-enqueue itself.

- [x] **Disable email confirmation dialog** — hides the "Confirm your email" prompt.
  - Written. The `android_settings_email_verification` experiment gate is forced to `false`,
    covering all five call sites. Polarity verified at `Lvj1/u0;.F1(Z)V`.
  - Build verified: compiles clean in CI. Not verified on device.

### Phase 2 — Ads

- [ ] **Disable ads** — removes sponsored pins from the search, related, related and home feeds,
  and removes the "hide product ads" and "hide board mod/ads in search" flow, both exposed as
  custom options on the settings screen.
  - **Hardest item on this list.** `NOTES.md` records that no global ad kill-switch literal
    exists in 14.38.0: `ads_enabled`, `enable_ads`, `hide_ads`, `no_ads`, `ad_free` and
    `adBlock` all return nothing. `ad_block` appears once, inside obfuscated Google Mobile Ads
    internals. Pinterest ad code is spread over ~10 packages.
  - The central ad render entry point is still unmapped. Locate it before writing anything.
  - The two settings-screen options are a separate concern from feed filtering; consider
    deciding explicitly whether they ship together or as two patches.
  - Related roadmap item: **Hide Promoted Pins** (below). Keep them separate — that patch hides
    promoted/shopping/related-product content, this one kills the ad SDK path. Decide the
    boundary in NOTES before writing either.
- [ ] **Hide ad tags** — removes Pinterest's ad loop views so visual elements no longer reveal
  "Promoted" or "Shop" labels.

### Phase 3 — Downloading

**Spiked, and the transcription is wrong about the direction of the work.** Pinterest
14.38.0 already ships pin download: the row, the click dispatch, the HTTP call, the
MediaStore write, the runtime storage-permission request and the toasts are all upstream
DEX-side code. What is missing is only *eligibility* — the row is withheld from plain
image pins. That also answers the JNI question on this path: no native code is involved.
Findings and dead ends in `reference/NOTES.md` under *Phase 3 — pin download already
exists upstream*.

- [ ] **Download pin from long press** — adds a download option to the long-press pin context
  menu so an image can be saved without opening the pin.
  - **Written, shipped in v0.6.0-dev.24, reported broken, REVERTED.** Full correction in
    `reference/NOTES.md` under *RETRACTED*. Three separate errors, all mine:
    1. **The trace was wrong.** The tap goes through `Lnj1/o0;.e(View,String)`, which posts a
       `Lm02/e(pinId)` event and returns. `Lnj1/q;.b(...)` — the one in the old trace — is the
       *idea-pin* download with its `DownloadManager` progress modal, not the plain-image path.
       The old brief also cited `nj1/r.smali` and `nj1/m.smali` as a fragment and its state;
       they are a three-value enum and an in-progress-download record.
    2. **There is a second, independent gate.** `Lfn1/f;.G3` calls `ye.p0(pin)` at ins 105 and
       jumps to ins 235 — past the download block — if it is false. Forcing
       `Lkj1/c;.e()` cannot add the row when `p0` is false. Worse: since the row *did* appear on
       device, `p0` was **true** for that pin, so it was a video or product pin where the row
       is native. The patch was never exercised.
    3. **The event has no subscriber for a feed pin.** Only `Ltt/a2;` (the pin closeup) and
       `Lbb1/x1;` (the promoted/ad pin cell) subscribe to `Lm02/e`, and both filter by pin id. A
       feed pin matches neither, so the request goes nowhere — the share sheet on screen was
       that. Forcing both gates would make a row appear that still does not download.
  - What works is stock: `DownloadActionView`, injected into the pin closeup action bar by
    `Ls11/b;.a(...)` from `Ls11/a0;`/`Ls11/r;`. Pinterest already downloads pins from the
    closeup.
  - **Re-scoped, not implemented.** The tractable shape is "surface the closeup's existing
    download", not "extend the feed row". Doing it from the feed menu means writing the
    subscriber — new logic, not a fingerprint.
- [x] **Download video** — adds a "Download video" option to the pin menu, saving the clip to the
  Downloads folder.
  - **Already native; no patch written.** `pin_video_download_success`,
    `downloading_video_modal_view`, `downloading_video_spinner` and `share_pin_title_when_download`
    exist, and the download completion handler branches on media type rather than assuming
    video. The row ships for video pins already, since video pins are exactly what the
    eligibility predicate accepts. Verified statically; a device pass should confirm the row
    is present on a video pin.
- [ ] **Download board** — adds an option to the search "..." menu to bulk-download the images
  and videos of a board grid.
  - **Not implementable as a patch; re-scoped out of Phase 3.** Nothing exists upstream: the
    pin overflow menu has exactly eight rows and none is board-level, and
    `rg -ril download` over the whole of the `gridactions` and `board` feature packages returns
    zero files. Evidence in `reference/NOTES.md`.
  - Multi-select already does bulk *share*, *collage*, *vote* and *move-to-board*, so the
    missing piece is specifically the download loop, not the selection machinery.
  - Cheapest honest version would still be a new row plus a click handler inside a
    42-register enum-dispatch method, an N-item loop with its own progress and failure
    handling, a MediaStore write per asset, and the video container question. That is a
    feature build; if it is wanted it needs its own scope and risk note.

### Phase 4 — Links and sharing

- [~] **Copy direct link** — adds a "Copy direct link" option to the pin menu. The transcription
  notes this misses the tracked link structure in favour of the Pinterest web link; treat that
  as a **known gap to fix**, not a spec to reproduce. Verify what the in-app copy action
  actually emits before implementing.
  - Verified: the option is **already native**. `Lnj1/i0;.c(Context)` builds a `copy_link` row
    (icon `copy_link_with_background`, action `LINK`). Nothing needs adding.
  - The gap is which URL it emits, and that is not reachable from the menu: the click path runs
    `Lnj1/o0;.h` → `Lnj1/i0;.b` → `Lpr/z0;.a` builder → `androidx/recyclerview/widget/l;.m` →
    `Ldr2/d;.a`, i.e. it configures the generic send pipeline with send type `COPY_LINK`. The
    shared URL comes from `SendableObject.e()`, which every send target reads.
  - Full trace and the five decoy clipboard sites are in `reference/NOTES.md`. Not forced.
- [ ] **Sanitize copied links** — strips tracking parameters from copied pin URLs, leaving clean
  web addresses. Customizable via settings.
- [ ] **Sanitize shared links** — strips tracking parameters from links shared via the default
  system share sheet. Customizable via settings.
  - These two share a sanitizer, so design the parameter-stripping helper once and reuse it.
- [ ] **Hide screenshot share menu** — removes the quick-Pinterest-share dialog that appears on
  taking a screenshot in-app.
  - Confirmed present: `DETECT_SCREEN_CAPTURE` permission, and `DETECT_SCREEN_CAPTURE` appears in
    the manifest. Locate the observer that raises the dialog.
- [~] **Open links in the default browser** — routes links to the device browser instead of
  Pinterest's in-app browser.
  - **Partially mapped, deliberately not written.** Established: both browser surfaces are
    internal and `exported=false`, so this is a launch-decision change and not an intent-filter
    change; Custom Tabs is an app-lifetime connection (`Lke0/c;` with
    `onCustomTabsServiceConnected` and an `a(ResolveInfo)Z` provider check), so it is opt-in per
    device; and `no_browsers_found` in `Lad2/b;` hints that a plain `ACTION_VIEW` fallback exists
    in some branch.
  - Not established: which method makes the choice, and whether there is a third option at all.
    If the app only ever has Custom Tabs or its own browser, "default browser" is new code, not a
    gate flip — the same verdict as Phase 3's board item.
  - Full map and the exact next reads are in `reference/NOTES.md`. `open_external` is a red
    herring: a deeplink query parameter, not a launch mode.
- [ ] **Use the system share sheet** — bypasses Pinterest's internal custom share sheet in favour
  of the native Android share sheet.
  - **Written, then reverted. It never did what it said.** `getShowInSharesheet()` is not the
    decision — it is a boolean that four callers feed into something else. Forcing it false
    changes what the sheet *contains*, not whether it *appears*.
    - `Lnj1/t0;.a` uses the result as one conjunct of a guard deciding whether to construct
      `Lnj1/z;`, the share-sheet config object. Skip the config and `t0.a` still builds
      `Lin1/e1;`, the sheet.
    - `Lfn1/f;.N3` is a refresh; `Lr11/a;.a` builds the social-app list.
  - A first version forced the shared accessor and silently disabled the download row, because
    `G3` reads the same accessor and jumps past the download block. Narrowing it to the three
    presentation sites fixed that conflict and kept the original mistake.
  - **Where the real decision is:** not in the accessor, and not in its four callers. The callers
    of `Lnj1/t0;.h` are `Lnc0/m;`, `Lnj1/s0;` and `Lza1/i;`. Note `Liw2/d0;.j(...)`, reached from
    `Lnj1/o0;.h`, is *not* the system chooser — it carries `"more_apps"` and calls
    `queryIntentActivities`, so it is Pinterest's own multi-app flow. Full correction in
    `reference/NOTES.md` under *RETRACTED*.

### Phase 5 — UI hiding

All of these are settings-toggled through the Phase 0 screen, so all depend on it.

- [x] **Hide Create nav button** — hides the "+" bottom navigation button.
  - Written as `Hide Create nav button`, opt-in. Same seam as the search and notifications
    patches: `GONE` the tab's view in `FloatingBottomNavBar.Q1`, never skip creating it.
  - CREATE is an ordinary member of that loop, not a floating button: the bar's descriptor
    table (`ae0/k;.<clinit>`) builds five tabs in order — HOME, SEARCH, CREATE, NOTIFICATIONS,
    PROFILE — each handed to the same method, so the create button's view is the same register.
  - Fingerprint is the existing `BottomNavTabAdderFingerprint`, already resolved and applied on
    device for the other two tabs. Injected smali parses; the whole class reassembles and
    round-trips through `smali`/`baksmali` with the block at instruction 4.
  - Adds the `morphe_hide_create_nav` toggle to the Phase 0 settings screen.
  - Not verified: that the button disappears and the remaining tabs reflow on a device.
- [x] **Hide Notifications nav button** — hides the notifications navigation button.
  - Written. Same seam, same shape and same reasoning as the search patch, keyed on
    `Lde0/a;->NOTIFICATIONS`. Both patches insert into `Q1` and compose, since each block uses
    only `v0`/`v1` and falls through to its own label.
  - CI: `:patches:compileKotlin` and `Verify project compiles` both green. Fingerprint resolves
    on device; runtime effect unverified.
- [x] **Hide Search nav button** — hides the search navigation button.
  - Written. Hides the tab's view in `FloatingBottomNavBar.Q1`; the tab is still created.
  - **Skipping the tab instead throws on every app start.** The tab index is a `forEachIndexed`
    counter incremented unconditionally at `O1` instruction 312 and passed to `Q1` at 322, then used
    positionally for `h.add(index, tab)` and for `addView`. Dropping one tab desynchronises the
    counter from the bar's list, so the next tab does `h.add(2, ...)` on a list of size 1.
  - Letting the tab be created and setting its view `GONE` keeps every index consistent, and a
    `GONE` child of a horizontal `LinearLayout` takes no space so the others still reflow.
  - `const/16` for the visibility, not `const/4`: `const/4 v0, 0x8` assembles silently but
    decodes as `-8`, giving a blank gap rather than a removed button.
  - CI: `:patches:compileKotlin` and `Verify project compiles` both green. Fingerprint resolves
    on device; not verified that the button disappears and tabs reflow.
- [ ] **Hide greeting header buttons** — hides the search and camera buttons in the home feed
  header.
- [x] **Hide comments** — hides the comments button on a pin.
  - Written. Sets the comments module wrapper in `UnifiedPinActionBarView`'s three-argument
    constructor to `GONE`.
  - **No legacy comments UI exists in 14.38.0** to fall back to, so the transcription's "replace
    with the standard comment section" cannot be implemented as described. Verified: no
    legacy/unified experiment literal, and `CommentsLibraryLocation` is referenced only by its own
    `<clinit>` and parcel plumbing.
  - The dump confirmed the only comments affordance on a pin closeup is the action-module button.
  - CI: `:patches:compileKotlin` and `Verify project compiles` both green. Fingerprint resolves
    on device; runtime effect unverified.
- [ ] **Hide search history** — hides the "Recent searches" section on the search screen and
  clears the search terms Pinterest stores about the user's view.
  - Two distinct effects: a view change and a data-clearing action. Decide whether the clear is
    part of this patch or a separate one.

### Phase 6 — Feature addition

- [ ] **Set pin as wallpaper** — adds a "Set as wallpaper" option to the pin menu, which
  downloads the image and sets it as the device wallpaper.
  - Needs `android.app.WallpaperManager` and likely a `setWallpaper` permission flow. Confirm
    whether the download reuses the Phase 3 download path or is independent.

### Known mismatch to resolve

`NOTES.md` verified three clean surfaces during disassembly that **do not appear in this list**:
the bot challenge (`BotChallengeActivity`, 3-instruction `onCreate`), the onboarding NUX
(`NUXActivity.goHome()`, two internal callers), and the `autoAnalytics` resume-event gate on
`baseActivity.onResume`. Decide whether to add patches for them or leave them recorded and unused.

### Settings toggles wired

The three UI-hiding patches read their state from the in-app Morphe settings screen rather than
only from the Manager's patch list, which is what the original descriptions specified.

| Patch | Key | Reads at |
| --- | --- | --- |
| Hide Search nav button | `morphe_hide_search_nav` | `FloatingBottomNavBar.Q1` ins 4 |
| Hide Notifications nav button | `morphe_hide_notifications_nav` | `FloatingBottomNavBar.Q1` ins 4 |
| Hide comments | `morphe_hide_comments` | `LegacyPromotedCloseupActionButtonModule.createView` ins 106, `Lsa1/i.<init>` ins 108, injected `Lsa1/i.setVisibility` |

Each injects `getContext()` then `MorpheSettingsActivity.isEnabled(Context, String)`. That helper
takes two arguments and not three because the target methods have only two free registers: a
default-value argument would need a third. All three patches are opt-in, so the shared default is
false and lives in the extension.

### Naming

The settings entry is **"Morphe"** everywhere: patch names, descriptions, the settings row
label, and every string resource. The settings row is added by "Morphe settings entry" and the
label is localized by "Morphe settings screen (label)", so those two must agree on the spelling
and it must match the `app.morphe.patches` plugin this repo builds with.

## Pinterest roadmap

Ordered by value × feasibility. Each item is one patch unless noted. Items already written are
cross-referenced to the working-plan phase above.

### P0 — Finish verification (before new features)

- [ ] Behavioral pass on the applied bundle: confirm the identifier comes
  back random, AppsFlyer/Engage transmit nothing, resource patches
  produce valid `resources.arsc`/manifest. Application proving
  fingerprints resolve is not proof of runtime effect.

### P1 — Data Saver flagship (⭐⭐⭐⭐⭐)

Single master concept, shipped as **separate narrow patches**, not one
flag (one patch per change; each must be independently revertible). None
of these has a phase-list entry yet, so each starts as a spike:

1. **Disable video autoplay** (standalone, highest value). Enforce
   never-autoplay beyond the existing cellular-only setting. Spikes:
   locate the autoplay gate (50%-visible Pin behavior is the
   user-visible symptom, not the anchor).
2. **Disable video prefetch / preload**. Technical core of the saver.
3. **Disable feed prefetch** (next-page + image prefetch). Potentially
   the most technically interesting patch; prefetch is likely
   entangled with the feed pager, so expect several candidate seams
   and pick the narrowest.
4. **Image quality selector** (Cellular Low/Med/Original, Wi-Fi
   Med/High/Original). **Spike resolved: the ladder is client-side.**
   A pin's `images` field is a size-keyed map and `Lau2/w;.d(Map)` reads
   `736x`/`345x`/`236x`/`originals` out of it, so the server seals nothing. The
   choice then funnels through one method, `Lvu2/d1;.b()`, which prefers 736x
   and falls back to originals. The blocker is no longer the seam, it is the
   shape: a selector needs a network input, a user setting, and a decision per
   render site across eight callers — so it stays a spike. Cheap subset if
   wanted is the inverse of P4.14, capping the ladder rather than promoting
   originals. Deliberately not written: it would be the opposite preference to
   P4.14 on the same method, so the two need mutual exclusion or a documented
   precedence, which is a design decision rather than an implementation detail.
5. **Don't preload external-site previews**. Small, isolated, good
   first Data Saver slice.
6. Explicit non-goal: never touch messaging/DM traffic.

Proposed modes (Off / Cellular-only / Always; Aggressive / Balanced /
Images-only) are a **settings UX decision for after** the individual
patches land, and become feasible now that the Phase 0 screen exists. Do
not build the mode UI first.

### P2 — Feed control (⭐⭐⭐⭐–⭐⭐⭐⭐⭐)

7. **Hide Promoted Pins** (dedicated patch; overlaps Phase 2 "Disable
   ads" — keep them separate, and fix the boundary in NOTES first).
8. **Hide "More like this" / Related Pins**. Recommendation-surface
   hiding, same family as Hide comments.
9. **Following-only / chronological-ish feed** — research spike only.
   Killer feature *if* the client exposes the feed-mode switch;
   if the feed is server-assembled with no client selector, this
   becomes "hide recommendations," not a feed-mode patch. Do not
   commit to it until the seam is found.
10. **Open Following by default** — same spike as 9; likely the same
    seam or nothing.

### P3 — Links and sharing (⭐⭐⭐⭐, maps to Phase 4)

11. **Disable in-app browser / open externally** → Phase 4 "Open links
    in the default browser", partially mapped and not patch-shaped yet: both
    surfaces are unexported, and a third launch option may not exist.
12. **Sanitize shared/copied URLs** → Phase 4 "Sanitize copied links" +
    "Sanitize shared links"; one shared sanitizer helper for both paths.
13. **Copy image URL / source URL** → Phase 4 "Copy direct link"
    (developer/researcher value; verify what the in-app copy action
    actually emits first — known gap recorded in Phase 4).

### P4 — Power features (⭐⭐⭐⭐)

14. **Force original image download** — **written as `Force original image
    download`.** Originals do reach the client: the pin's `images` map carries an
    `originals` entry beside the rest of the ladder, and `Lvu2/d1;.b()` already
    reads it as its second preference. The patch swaps two `iget-object` reads so
    originals is tried first. Same opcode and format, so no width-mismatched
    method; and the chain still falls through to every other field and the EMPTY
    singleton, so a pin with no originals renders exactly as before. Opt-in,
    because it *increases* data use — the opposite of this project's usual
    direction.
15. **Save image without watermark/UI** (long-press save of the actual
    asset) → Phase 3 "Download pin from long press", which was written,
    shipped and **reverted**: the row can be made to appear but cannot be made
    to download from the feed menu. The Java-vs-JNI question *is* answered —
    this save path is entirely DEX-side — and the rendition question in item 14
    is still open.
16. **Download video / board** → "Download video" is already native and
    needs no patch; "Download board" remains the only Phase 3 work left.

### P5 — UI (⭐⭐⭐–⭐⭐⭐⭐, all settings-gated → Phase 0)

17. **Hide Create button / nav tabs** → Phase 5 "Hide Create nav button"
    (generalize the proven Search/Notifications seam; `GONE`, never skip
    creation — the `forEachIndexed` desync crash is on record).
18. **AMOLED true black** (`#000000`; resource/theme edit, new surface).
    No phase entry yet.
19. **Compact pin grid** (2/3/4 columns; tablet value; layout-level,
    needs its own spike). No phase entry yet.
20. **Hide Notes / social content** (conditional on the app actually
    shipping it in this build — verify presence first). No phase entry yet.

Explicitly deferred: Chronological feed as committed scope (spike
only), Aggressive/Balanced/Images-only mode UI (after patches land).

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

### S1 — Data Saver flagship (⭐⭐⭐⭐⭐, same doctrine as Pinterest)

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
   spike class as Pinterest item 9; commit only on a found seam.
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

13. **AMOLED black**, shared doctrine with Pinterest item 18.

Explicitly deferred until spikes resolve: chronological/following-only
feeds, full reader mode, offline engine, notification channels (if no
client-side seam exists, these are not patches).

## v1 scope (small, extremely useful — ship these first)

Pinterest v1: Disable video autoplay · Disable feed prefetch ·
Hide Promoted Pins · Disable in-app browser · Clean shared URLs ·
Download image *(Phase 3, written, awaiting device pass)* ·
Download original image *(different thing — rendition selection, still a spike)* ·
Following-only feed *(only if spike hits)* · Hide recommendations ·
Compact grid · AMOLED black.

Substack v1 (all post-Phase-0): Tap-to-play video · Disable media
preloading · Reader mode *(only if spike hits)* · Hide Notes · Hide
recommendations · Subscriptions-only feed *(only if spike hits)* ·
Essential notifications · Offline articles · Wi-Fi-only media.

## Working agreements (unchanged)

- Behavior spec → pin target → locate in disassembly → NOTES entry →
  narrowest fingerprint → smallest edit → compile → apply → device
  test. One patch per commit.
- Fingerprints pin stable strings/flags/types/instruction shape, never
  obfuscated names (the AppsFlyer `AFa1tSDK` fragility is the cautionary
  tale; prefer the GoogleEngage pattern: unobfuscated class + unique
  literal).
- `const/16` for `GONE` (`0x8`); name invoke arity from the descriptor;
  never `replaceInstructions` with a width-mismatched nop block
  (both crash classes are documented in NOTES).
- Verification ladder per patch: compiles in CI → fingerprint resolves
  on device → runtime effect observed. Only the last earns `[x]` for a
  *behaviour*, and nothing in this bundle has cleared it yet.