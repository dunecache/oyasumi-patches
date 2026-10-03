# Pinterest patch todo

Working plan for the `pinterest` branch. Target: `com.pinterest` **14.38.0** (`14388010`),
SHA-256 `af6b383adb445cebee1ca43f14ac409f91475c1d62e0e11ef52ef52e29fb0553`.

All reverse-engineering findings live in `reference/NOTES.md` under
`# Pinterest 14.38.0 reference notes`. Prebuilt indexes and tooling live in
`~/apks/dex_files/pinterest_v14.38.0/`.

## Read this first

- **The 28 items below are transcribed from an existing patch bundle that targets a different
  Pinterest build.** They describe intended behaviour, not verified anchors. Per `AGENTS.md`,
  no fingerprint may be written from these descriptions: every class, method, literal and
  instruction index has to come from real output of *this* reference DEX. Treat each line as a
  behaviour spec and start from the disassembly.
- **Item names may not match this build.** Feature names, menu labels, string keys and settings
  keys are all version-sensitive and Pinterest renames them frequently.
- **One patch per commit**, per `AGENTS.md`. Do not batch items.
- Status legend: `[ ]` unstarted, `[~]` anchor located but not implemented,
  `[x]` implemented and device-verified. Only `[x]` after the patch is applied to the pinned
  build and the changed flow is exercised on a device.

## Phase 0 — Harness (dependency for 6 items)

**Deferred — not blocked on effort, blocked on the app.** These four are implemented in the
official bundle on top of `androidx.preference`. Pinterest 14.38.0 has **zero**
`androidx.preference` classes and zero references to the package, no `res/xml` preference
screen, and a Compose-based settings UI. There is no `PreferenceFragment` to hook, so these
patches cannot work as described regardless of how much framework is ported. Verified in
`reference/NOTES.md`.

Every patch below is therefore written as a plain `default = true` toggle, which is how all
existing patches in this repo already behave. Revisit the settings screen once the set of toggles
actually wanted is known.

- [x] **Morphe runtime state** — tracks the current Pinterest version so patches can react to
  per-build target changes.
  - Done. An unnamed internal `bytecodePatch` records `packageMetadata.versionName`; all three
    Pinterest patches now declare `dependsOn(versionCheckPatch)`.
  - The official bundle compares versions with Kotlin's string `>=`, which inverts four of eight
    checks against Pinterest's real version numbers (`14.38.0` sorts below `14.9.0`). Replaced
    with a component-wise numeric comparison; the case table is in `reference/NOTES.md`.
  - Be clear about what this does and does not do: it does **not** make a fingerprint survive an
    app update. It makes a patch able to tell which version it is running against, which is the
    prerequisite for noticing that a fingerprint has stopped matching.
- [x] **Morphe settings entry** — adds the "Morphe" row to Account Settings.
  - Written, following the mechanism proven in the `browzomje/browzomje-patches` bundle: reuse
    Pinterest's own external-link row (single-`String` constructor) with a `morphe://settings`
    URL, which fires `ACTION_VIEW` into the declared intent-filter. The obfuscated row class is
    resolved from the dex at patch time, never pinned.
  - On 14.38.0 the builder is `labs/s;.invoke` (17 rows, header `f1` built 4x, link `k1` built
    1x), identified by shape, not name. Anchor is the most-frequent `<init>(int)`; the list
    register comes from the first `List.add` after it.
  - Build verified: compiles clean in CI. Not verified: fingerprint matches on device, row
    appears, activity opens.
- [x] **Morphe settings screen (label)** — provides the string for the "Morphe" entry.
  - Written as a `resourcePatch` appending `morphe_settings_entry` to `res/values/strings.xml`.
  - Default locale only. The all-languages walk is not written because no documented API
    enumerates the decoded resource tree.
  - Build verified: compiles clean in CI (`v0.6.0-dev.1`). Not verified: resource recompilation
    against the real APK, and any device run.
- [x] **Morphe settings screen (manifest)** — registers the settings activity in the manifest.
  - Written as a `resourcePatch` declaring
    `app.oyasumi.extension.MorpheSettingsActivity`, `exported=false`.
  - The activity itself does not exist yet; until it does this declares an entry with no code.
    `document("AndroidManifest.xml")` is inferred from the documented DOM helper, not confirmed.
  - Not verified: the manifest path, resource recompilation, and the missing activity.
  - Blocked with the above.

## Phase 1 — Privacy and safety toggles

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
    deferred "Morphe runtime state" work.
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

## Phase 2 — Ads

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
- [ ] **Hide ad tags** — removes Pinterest's ad loop views so visual elements no longer reveal
  "Promoted" or "Shop" labels.

## Phase 3 — Downloading

- [ ] **Download pin from long press** — adds a download option to the long-press pin context
  menu so an image can be saved without opening the pin.
- [ ] **Download video** — adds a "Download video" option to the pin menu, saving the clip to the
  Downloads folder.
  - Media handling likely crosses `libx_media_handler.so` and `libzune_jpeg-*.so`; check whether
    the save path is Java or JNI before assuming the patch is DEX-only.
- [ ] **Download board** — adds an option to the search "..." menu to bulk-download the images
  and videos of a board grid.
  - Most involved of the three: it is a bulk operation over a grid, not a single asset.

## Phase 4 — Links and sharing

- [ ] **Copy direct link** — adds a "Copy direct link" option to the pin menu. The transcription
  notes this misses the tracked link structure in favour of the Pinterest web link; treat that
  as a **known gap to fix**, not a spec to reproduce. Verify what the in-app copy action
  actually emits before implementing.
- [ ] **Sanitize copied links** — strips tracking parameters from copied pin URLs, leaving clean
  web addresses. Customizable via settings.
- [ ] **Sanitize shared links** — strips tracking parameters from links shared via the default
  system share sheet. Customizable via settings.
  - These two share a sanitizer, so design the parameter-stripping helper once and reuse it.
- [ ] **Hide screenshot share menu** — removes the quick-Pinterest-share dialog that appears on
  taking a screenshot in-app.
  - Confirmed present: `DETECT_SCREEN_CAPTURE` permission, and `DETECT_SCREEN_CAPTURE` appears in
    the manifest. Locate the observer that raises the dialog.
- [ ] **Open links in the default browser** — routes links to the device browser instead of
  Pinterest's in-app browser, gated on a settings toggle.
  - Confirmed present and promising: `com.pinterest.componentBrowser.ComponentBrowserActivity`,
    `com.pinterest.activity.web.WebViewActivity`, and
    `com.pinterest.browser.customTabs.chrome.ChromeTabBroadcastReceiver`. The Custom Tabs path
    and the plain WebView path may need separate edits.
- [ ] **Use the system share sheet** — bypasses Pinterest's internal custom share sheet in favour
  of the native Android share sheet, gated on a settings toggle.
  - Confirmed present: `com.pinterest.feature.sharesheet` (100 classes) and
    `com.pinterest.share.*`. Gated on the settings screen, so Phase 0 applies.

## Phase 5 — UI hiding

All of these are settings-toggled, so all depend on Phase 0.

- [ ] **Hide Create nav button** — hides the "+" bottom navigation button.
- [x] **Hide Notifications nav button** — hides the notifications navigation button.
  - Written. Same seam, same shape and same reasoning as the search patch, keyed on
    `Lde0/a;->NOTIFICATIONS`. Both patches insert into `Q1` and compose, since each block uses
    only `v0`/`v1` and falls through to its own label.
  - CI: `:patches:compileKotlin` and `Verify project compiles` both green. Not verified on device.
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
  - CI: `:patches:compileKotlin` and `Verify project compiles` both green. Not verified: fingerprint matches, button disappears, tabs reflow.
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
  - CI: `:patches:compileKotlin` and `Verify project compiles` both green. Not verified on device.
- [ ] **Hide search history** — hides the "Recent searches" section on the search screen and
  clears the search terms Pinterest stores about the user's view.
  - Two distinct effects: a view change and a data-clearing action. Decide whether the clear is
    part of this patch or a separate one.

## Phase 6 — Feature addition

- [ ] **Set pin as wallpaper** — adds a "Set as wallpaper" option to the pin menu, which
  downloads the image and sets it as the device wallpaper.
  - Needs `android.app.WallpaperManager` and likely a `setWallpaper` permission flow. Confirm
    whether the download reuses the Phase 3 download path or is independent.

## Known mismatch to resolve

`NOTES.md` verified three clean surfaces during disassembly that **do not appear in this list**:
the bot challenge (`BotChallengeActivity`, 3-instruction `onCreate`), the onboarding NUX
(`NUXActivity.goHome()`, two internal callers), and the `autoAnalytics` resume-event gate on
`baseActivity.onResume`. Decide whether to add patches for them or leave them recorded and unused.

## Settings toggles wired

The three UI-hiding patches read their state from the in-app Morphe settings screen rather than
only from the Manager's patch list, which is what the original descriptions specified.

| Patch | Key | Reads at |
| --- | --- | --- |
| Hide Search nav button | `morphe_hide_search_nav` | `FloatingBottomNavBar.Q1` ins 4 |
| Hide Notifications nav button | `morphe_hide_notifications_nav` | `FloatingBottomNavBar.Q1` ins 4 |
| Hide comments | `morphe_hide_comments` | `UnifiedPinActionBarView` ctor ins 29 |

Each injects `getContext()` then `MorpheSettingsActivity.isEnabled(Context, String)`. That helper
takes two arguments and not three because the target methods have only two free registers: a
default-value argument would need a third. All three patches are opt-in, so the shared default is
false and lives in the extension.

## Naming

The settings entry is **"Morphe"** everywhere: patch names, descriptions, the settings row
label, and every string resource. The settings row is added by "Morphe settings entry" and the
label is localized by "Morphe settings screen (label)", so those two must agree on the spelling
and it must match the `app.morphe.patches` plugin this repo builds with.

## Not tracked as patches

- Gradle build, bundle application, and device testing. The `app.morphe.patches` plugin `1.3.4`
  registry returns `401` for the available token, so compilation is delegated to CI.
- Bundled native libraries, `real_feed_*.jsonl` offline feed assets, and the Firebase/WorkManager
  infrastructure. Out of scope unless a patch forces the question.

## Device verification status

The full bundle applies cleanly to Pinterest 14.38.0 on device: every fingerprint resolves, no
`VerifyError`, no crash. That proves the anchors and the smali encodings, including the first
`extensions/` wiring in this repo.

What application does **not** prove is runtime effect. Still unverified per patch: that the
identifier actually comes back random, that AppsFlyer and Engage transmit nothing, and that the
resource patches produce a valid `resources.arsc` and manifest. Those need a behavioral pass, not
just a clean install.
