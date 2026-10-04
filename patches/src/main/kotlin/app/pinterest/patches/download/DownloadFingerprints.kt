package app.pinterest.patches.download

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * The builder for the pin long-press menu — the row list behind
 * `com.pinterest.feature.gridactions.modal.view.PinOverflowMenuModalImpl`.
 *
 * This is the `ArrayList`-appending method of the sharesheet presenter `Lfn1/f;`
 * (34 fields, 22 methods). The class declares six `(ArrayList)V` methods and they are
 * not interchangeable:
 *
 * - `G3(ArrayList)V`, 23 registers — the real builder, and the only one of the six
 *   that asks `Lkj1/c;` whether the pin is downloadable.
 * - `F3(ArrayList)V`, 4 registers — adds Download at index 0 plus the Instagram and
 *   Facebook rows when those apps are installed.
 * - `H3(ArrayList)V`, 6 registers — adds "Add to story" at index 0 and appends
 *   Download last.
 * - `L3(ArrayList)V`, 10 registers — not a builder: copies the list, `removeIf`s a
 *   fixed set of non-social row ids (`internal_send`, `SAVE_LINK`, `copy_link`,
 *   `more_apps`, `DOWNLOAD_IDEA_PIN`, `pincode`, `collage`) and reports
 *   `sharesheet_social_apps_count` under a `RENDER` pinalytics event.
 * - `M3(ArrayList)V`, 5 registers — filters the list by row id
 *   (`e.c.equalsIgnoreCase("copy_link")`), another removal pass.
 * - `O3(ArrayList)V`, 7 registers — publishes the list to
 *   `com.pinterest.feature.sharesheet.view.SharesheetModalAppListView`.
 *
 * `G3` identifies itself as the long-press menu in unobfuscated code: at ins 80-82 it
 * compares its location field against `Lin1/t1;->PIN_OVERFLOW_FEED_MODAL`.
 *
 * ## Why this is not the closeup drawer
 *
 * Two surfaces look alike and are not interchangeable. The *closeup drawer* is the
 * bottom sheet on the pin closeup screen (`closeup_drawer_bottom_sheet`,
 * `fragment_pin_closeup_drawer`, `Lbb1/t;`); its view package
 * `com/pinterest/feature/pin/closeup/view/drawer/` is unobfuscated and it has no
 * action rows of its own. Long-pressing a pin opens the overflow modal instead, which
 * is the surface this method feeds. Confusing the two is the most likely way to write
 * a patch that applies cleanly and changes nothing.
 *
 * ## Resolution, verified
 *
 * `.scratch/resolve_fp.py 'Lfn1/f;' G3 '(Ljava/util/ArrayList;)V'` against the pinned
 * APK: exactly one method, in `classes6.dex`, 23 registers, all three filters present.
 * The filters also survive a rename of `G3`, which is why they are not decoration:
 * re-run with an empty method name and the other five candidates each fail at least
 * two of the three. `L3` is the interesting near-miss — it loads `DOWNLOAD_IDEA_PIN`
 * but calls neither the eligibility predicate nor the row factory.
 */
object PinOverflowMenuFingerprint : Fingerprint(
    definingClass = "Lfn1/f;",
    name = "G3",
    returnType = "V",
    parameters = listOf("Ljava/util/ArrayList;"),
    filters = listOf(
        // The call the patch replaces. Filtered on rather than only searched for, so a
        // future rename of the class or method fails the fingerprint instead of silently
        // resolving to some other list builder.
        methodCall(
            definingClass = DOWNLOAD_ELIGIBILITY_CLASS,
            name = "e",
            parameters = listOf("Lcom/pinterest/api/model/pe;"),
            returnType = "Z"
        ),
        // The factory that builds the Download row: icon `Lnm0/d;->download_icon`,
        // title `Lgi0/b;->download`, action `Lkx1/o0;->DOWNLOAD`.
        methodCall(
            definingClass = "Lnj1/i0;",
            name = "d",
            parameters = listOf("Landroid/content/Context;"),
            returnType = "Lcom/pinterest/adapter/e;"
        ),
        // The row's analytics id. The only `const-string` in the row factory, and the
        // one literal in this method that no obfuscation pass can change.
        string(DOWNLOAD_ROW_ID)
    )
)

/**
 * The pin-overflow eligibility object, `Lkj1/c;`.
 *
 * `e(pe)` is the predicate this patch overrides, and it is the reason the Download row is
 * missing from a plain image pin. Its body requires, in order: a non-null pin,
 * `!pin.Y5()` (field `C1`, the idea-pin flag), `ye.p0(pe)` (video pin or product pin),
 * the `d()` experiment, and then `f()` plus a rollout bucket. `ye.p0(pe)` is the only
 * step an image pin fails, so the row is withheld from exactly the pins this patch is
 * for.
 *
 * Deliberately **not** patched here: `e(pe)` has a second caller,
 * `com/pinterest/ads/feature/owc/collageads/s;.a(Ljb0/z;, Lqw2/d;)Lqw2/o;`, so an edit
 * inside this class would also change the collage-ads surface. Overriding the call in
 * the menu instead keeps the change to one screen. See the patch for the reasoning.
 */
internal const val DOWNLOAD_ELIGIBILITY_CLASS = "Lkj1/c;"

/** The pin-overflow row id of the Download row, and the anchor literal for the menu. */
internal const val DOWNLOAD_ROW_ID = "DOWNLOAD_IDEA_PIN"