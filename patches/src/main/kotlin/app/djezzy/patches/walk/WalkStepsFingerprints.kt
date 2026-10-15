package app.djezzy.patches.walk

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * The Walk & Win counter is rendered from a persisted total, not from the pedometer stream.
 * A device run established that: every number on the card — steps, distance, calories and
 * the accumulated-time reading — stayed frozen while the stream was demonstrably
 * delivering forced values, and none of them moved across Start Walk. So a stream event
 * only reaches storage during a session, and the displayed figure is read back out of it.
 * Delivering 10,000 without walking therefore cannot be done over the event channel at
 * all; the stored total is what has to be forced.
 *
 * `shared_preferences_android` ships two independent backends and which one Dart calls is
 * not readable from the AOT snapshot, so both are hooked. Each logs unconditionally, which
 * is the point: an earlier version logged only on a key match and that made "the hook
 * never applied" indistinguishable from "the hook applied and the app never reads that key
 * through it". A device run now names the live backend and every key it asks for.
 *
 * Neither declares a Fingerprint-level `definingClass`. That reduces matching to a single
 * `classMap` lookup which returns null with no fallback, before the indexed candidate
 * search, so it costs the fallback without adding anything the name, return type,
 * parameter list and filters do not already give.
 */

/**
 * The legacy backend. `LegacySharedPreferencesPlugin` has no per-type getter at all — its
 * only reads are `getAllPrefs`, `getAll` and `getKeys`, and all of them funnel through
 * `getAllPrefs`, which hands Dart the whole map and lets Dart pick the key. So the map is
 * amended on the way out rather than any single getter being hooked.
 *
 * `startsWith` and `transformPref` are what identify this as the map builder rather than a
 * same-named helper: `transformPref` is the plugin's own per-entry encoder, so it also
 * proves the loop is the pref-copying loop.
 */
object LegacyPreferenceMapFingerprint : Fingerprint(
    name = "getAllPrefs",
    returnType = "Ljava/util/Map;",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Set;"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/SharedPreferences;",
            name = "getAll",
            parameters = emptyList(),
            returnType = "Ljava/util/Map;"
        ),
        methodCall(
            definingClass = "Ljava/util/String;",
            name = "startsWith",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z"
        ),
        methodCall(
            definingClass = "Lio/flutter/plugins/sharedpreferences/LegacySharedPreferencesPlugin;",
            name = "transformPref",
            parameters = listOf("Ljava/lang/String;", "Ljava/lang/Object;"),
            returnType = "Ljava/lang/Object;"
        ),
        opcode(Opcode.RETURN_OBJECT)
    )
)

/**
 * The async backend. This plugin version stores through DataStore, so its `getInt` never
 * touches `android.content.SharedPreferences.getInt` and needs a hook of its own.
 *
 * `getInt` is a suspend wrapper whose whole body is two null checks, a coroutine start and
 * a return, so the key is already in a register before anything is read and the boxed
 * `Long` can simply be returned without starting the coroutine.
 *
 * The `const-string` is Kotlin's null-check message for the *options* parameter, emitted
 * from the declared parameter name. It is unique to this two-parameter overload, and R8
 * has no reason to change it.
 */
object AsyncIntPreferenceFingerprint : Fingerprint(
    name = "getInt",
    returnType = "Ljava/lang/Long;",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lio/flutter/plugins/sharedpreferences/SharedPreferencesPigeonOptions;"
    ),
    filters = listOf(
        string("options"),
        opcode(Opcode.RETURN_OBJECT)
    )
)
