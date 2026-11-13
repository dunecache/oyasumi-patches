package app.drdriving.patches.coins

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess

/**
 * Six coin-spend methods on `Lcom/ansangha/drdriving/d;`. Every one follows the same
 * three-instruction pattern when it debits the spendable coin balance:
 *
 * ```smali
 * iget  vN, vR, Lcom/ansangha/drdriving/o;->iCoinC:I
 * sub-int/2addr vN, vS
 * iput  vN, vR, Lcom/ansangha/drdriving/o;->iCoinC:I
 * iget  vN, vR, Lcom/ansangha/drdriving/o;->iCoin:I
 * sub-int/2addr vN, vS
 * iput  vN, vR, Lcom/ansangha/drdriving/o;->iCoin:I
 * ```
 *
 * i.e. the reserve and the spendable balance are both decremented by the same amount.
 * `buyCoin` does the same for gold (`iGoldC` / `iGold`) and *increments* coins, so
 * it is a separate fingerprint.
 *
 * Why these fingerprints are stable: `iCoinC` and `iCoin` are unobfuscated field names
 * on the app-owned save-state class `o`, and the class/method names are unobfuscated
 * too, so nothing here depends on an obfuscated identifier. Each method's class + name
 * + descriptor already identifies it uniquely, and the two `fieldAccess` filters pin
 * the debit pair specifically rather than any other read of either field.
 *
 * `iCoinC` is read before `iCoin` in every one of these methods, so the filters are
 * declared in instruction order and the patcher's forward-only cursor is satisfied.
 */

/** `buyCar(I)V` — buy a car with coins. */
object BuyCarFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/d;",
    name = "buyCar",
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoinC",
            type = "I"
        ),
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoin",
            type = "I"
        )
    )
)

/** `processCarSelect()V` — coin cost of the car-select flow. */
object ProcessCarSelectFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/d;",
    name = "processCarSelect",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoinC",
            type = "I"
        ),
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoin",
            type = "I"
        )
    )
)

/** `processMultiEnding()V` — coin cost of the multi-ending flow. */
object ProcessMultiEndingFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/d;",
    name = "processMultiEnding",
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoinC",
            type = "I"
        ),
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoin",
            type = "I"
        )
    )
)

/** `rentCar(I)V` — rent a car with coins. */
object RentCarFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/d;",
    name = "rentCar",
    returnType = "V",
    parameters = listOf("I"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoinC",
            type = "I"
        ),
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoin",
            type = "I"
        )
    )
)

/** `upgradeCar(II)V` — upgrade a car with coins. */
object UpgradeCarFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/d;",
    name = "upgradeCar",
    returnType = "V",
    parameters = listOf("I", "I"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoinC",
            type = "I"
        ),
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoin",
            type = "I"
        )
    )
)

/**
 * `buyCoin(II)V` — the gold-for-coins exchange. It debits `iGoldC` / `iGold` by `p2`
 * (the gold cost) and credits `iCoinC` / `iCoin` by `p1` (the coins bought). The
 * patch replaces the balance-guard `iget` read, not the `if-ge` that consumes it.
 */
object BuyCoinFingerprint : Fingerprint(
    definingClass = "Lcom/ansangha/drdriving/d;",
    name = "buyCoin",
    returnType = "V",
    parameters = listOf("I", "I"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iGold",
            type = "I"
        ),
        fieldAccess(
            definingClass = "Lcom/ansangha/drdriving/o;",
            name = "iCoin",
            type = "I"
        )
    )
)