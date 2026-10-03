package app.oyasumi.extension;

import android.util.Log;

/**
 * Obfuscated names resolved <b>at patch time</b> from the dex being patched.
 *
 * <p>A hook sometimes needs to build a Pinterest object whose class name is obfuscated (the
 * settings external-link row, for example). Writing that name here by hand would break the patch
 * on every release. Instead the patch finds it by reading the dex and reports it here by calling
 * the setter, which is injected where a register is known free. At runtime the extension only
 * reads the field: no obfuscated name is ever hardcoded in Java.
 *
 * <p>A setter is used rather than having the patch rewrite this class's `<clinit>` because the
 * setter is verifiable and does not depend on how the patcher handles extension bytecode during
 * dex rewriting.
 *
 * <p>An empty field means its patch did not run or did not find its target; readers must treat
 * that as an error and log it.
 */
public final class MorpheRuntimeNames {

    private static final String TAG = "MorpheSettings";

    /**
     * Class of the Account Settings external-link row, the one with a `(String url)`
     * constructor. Set by "Morphe settings entry".
     */
    public static volatile String settingsRowClass = "";

    /** Destination of the "Morphe" row: the scheme handled by {@link MorpheSettingsActivity}. */
    public static final String SETTINGS_URI = "morphe://settings";

    private MorpheRuntimeNames() {}

    /** Called by injected code from "Morphe settings entry". */
    public static void setSettingsRowClass(String className) {
        settingsRowClass = className;
        Log.i(TAG, "settings row class resolved by the patch: " + className);
    }
}
