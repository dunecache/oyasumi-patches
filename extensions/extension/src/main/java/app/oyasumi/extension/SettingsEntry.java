package app.oyasumi.extension;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Appends the "Morphe" entry to the Account Settings list.
 *
 * <p>The row class to build has an obfuscated name that changes per release, so it is never
 * written here. The patch resolves it from the dex and deposits it in
 * {@link MorpheRuntimeNames}; this method only reads that field. The row takes the destination
 * URL as its single constructor argument, and clicking it makes Pinterest fire a generic
 * `ACTION_VIEW` that resolves to the `morphe://` intent-filter.
 *
 * @param list the mutable settings-entry list, taken from the patched bytecode.
 */
public final class SettingsEntry {

    private static final String TAG = "MorpheSettings";

    private SettingsEntry() {}

    public static void appendMorpheSettingsEntry(Object list) {
        String rowClassName = MorpheRuntimeNames.settingsRowClass;
        if (rowClassName == null || rowClassName.isEmpty()) {
            Log.e(TAG, "the patch did not resolve the settings row class "
                    + "(settingsRowClass empty): Morphe entry not added. "
                    + "Open the settings with: adb shell am start -a android.intent.action.VIEW "
                    + "-d \"morphe://settings\"");
            return;
        }

        if (!(list instanceof List)) {
            Log.e(TAG, "expected a List, got "
                    + (list == null ? "null" : list.getClass().getName()));
            return;
        }
        try {
            Class<?> rowClass = Class.forName(rowClassName);
            Constructor<?> ctor = rowClass.getConstructor(String.class);
            ctor.setAccessible(true);
            Object row = ctor.newInstance(MorpheRuntimeNames.SETTINGS_URI);

            @SuppressWarnings("unchecked")
            List<Object> entries = (List<Object>) list;
            entries.add(row);
            Log.i(TAG, "Morphe entry added to the Settings");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "settings row class " + rowClassName
                    + " missing: resolved on a dex other than the installed one", e);
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "the Settings list is immutable", e);
        } catch (Throwable t) {
            Log.e(TAG, "could not add the Morphe entry", t);
        }
    }
}
