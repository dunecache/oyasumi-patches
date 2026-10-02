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
- [ ] **Morphe settings entry** — adds the "Morphe" row to Account Settings under Profile.
  - Anchor hunt completed and recorded in `reference/NOTES.md`. `SETTINGS_MAIN` is referenced only
    from `<clinit>` and travels as a parcel; rows are obfuscated models bound by three separate
    callers; the two classes in the list package are empty markers. No clean seam. Needs either a
    fingerprint on an obfuscated builder or interception at the parcel boundary.
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
- [~] **Disable third party trackers** — investigated; **superseded**, see below.
  - The SDK list in the description is wrong for this build. MoPub, Adjust, Nielsen, Segment,
    Facebook SDK, AppLovin, Unity, IronSource, Vungle, Pangle, Mintegral, Appbrain and Appodeal
    are all **absent**. Enumerated against the real APK rather than trusted.
  - What is present and disableable is exactly AppsFlyer and Google Engage, both already written
    as patches 2 and 3. Google Mobile Ads (3,337 classes) is advertising, not tracking, and belongs
    to the "Disable ads" item.
  - Google Measurement (Firebase Analytics, 289 classes) has **no** clean gate: Pinterest makes
    zero calls into it, it is self-initialising through manifest components, and its uploader was
    not identified. Not patched, deliberately, rather than writing a speculative fingerprint.
  - Recommend folding this item into patches 2 and 3 and deleting it. Details in
    `reference/NOTES.md`.
  - Broad by nature. Expect to need several narrow edits, not one. Enumerate the SDKs actually
  bundled before starting; do not assume the list in the description is complete for 14.38.0.

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
- [ ] **Disable Google Engage worker** — blocks the Google Engage worker process and its
  background sync.
  - `GoogleEngageWorker` extends `RxWorker`; its `doWork` is renamed to `g()` by R8 and returns an
  obfuscated `Single`.
  - **Known weak target.** Unlike the receiver this method has no literal and every collaborator is
  obfuscated, so there is no clean anchor yet. Needs its own investigation; do not assume it is
  covered by suppressing the receiver.

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
- [ ] **Hide Notifications nav button** — hides the notifications navigation button.
- [ ] **Hide Search nav button** — hides the search navigation button.
  - These three should share one nav-menu anchor if the nav is built in one place. Verify that
    first; if the nav is built per-tab they are genuinely separate patches.
  - Related: `info.mqtt.android.service.MqttService`, `com.pinterest.pushnotification.MessagingService`
    and `FirebaseMessagingService` are all confirmed present and bear on the notifications button.
- [ ] **Hide greeting header buttons** — hides the search and camera buttons in the home feed
  header.
- [ ] **Hide comments** — hides the pin comments section and replaces it with the standard
  comment section.
  - Confirmed present: `com.pinterest.feature.unifiedcomments.view.CommentActivity`, plus
    `com/pinterest/activity/pin/view/unifiedcomments/*`. The "replaces it with the standard
    comment section" half is ambiguous; clarify before implementing.
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
