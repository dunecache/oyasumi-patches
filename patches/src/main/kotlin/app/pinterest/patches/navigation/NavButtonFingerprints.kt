package app.pinterest.patches.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

private const val NAV_BAR = "Lcom/pinterest/navigation/view/FloatingBottomNavBar;"

/**
 * The method that inflates a bottom-navigation tab, wires its click and long-click listeners,
 * adds it to the bar's tab list, and assigns its layout weight.
 *
 * On 14.38.0 this is `FloatingBottomNavBar.Q1(Lae0/o;, I, Lf82/l;, Lf82/j;, Lf82/n;)`. The class
 * name is not obfuscated; the method name and every parameter type are.
 *
 * It is the correct place to skip a tab rather than the factory it calls. The factory,
 * `e1(Lae0/o;, Lf82/l;, Lf82/j;, Lf82/n;)`, is the only method in the APK returning `Lf82/c1;`
 * and the only one in the bar reading the `Lde0/a;->SEARCH` enum constant, so the `methodCall`
 * filter below is a sound anchor for it — but it cannot be the patch target, because `Q1`
 * dereferences its result at instruction 2 (`Lf82/c1;->f()`, no null guard) and adds it to the
 * bar's tab list at instruction 20. Returning null from the factory would throw on the very next
 * instruction. Returning early from `Q1` cannot, because `Q1` returns `void` and has no value to
 * supply.
 *
 * Verified unique in the reference: exactly one method of `FloatingBottomNavBar` carries this
 * signature.
 */
object BottomNavTabAdderFingerprint : Fingerprint(
    definingClass = NAV_BAR,
    returnType = "V",
    parameters = listOf("Lae0/o;", "I", "Lf82/l;", "Lf82/j;", "Lf82/n;"),
    filters = listOf(
        methodCall(
            definingClass = NAV_BAR,
            name = "e1",
            parameters = listOf("Lae0/o;", "Lf82/l;", "Lf82/j;", "Lf82/n;"),
            returnType = "Lf82/c1;"
        )
    )
)