package app.oyasumi.extension;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * The Morphe settings screen, opened from the "Morphe" row in Account Settings via the
 * `morphe://settings` intent-filter.
 *
 * <p>Minimal on purpose: a dark list of switches persisted to `SharedPreferences`, using only
 * framework widgets and no Pinterest resources, so it cannot break when Pinterest reorganises
 * its own UI. The switches here are placeholders for the settings-toggled patches that do not
 * exist yet; each of those patches will read its own key from this same file. Adding a real
 * toggle later means adding one row here and one read there, nothing else.
 *
 * <p>Kept free of Pinterest references for the same reason the manifest gives this activity a
 * framework theme: anything resolved against Pinterest's theme or resources is something a
 * Pinterest update can move.
 */
public final class MorpheSettingsActivity extends Activity {

    /** Preferences file every Morphe toggle reads and writes. */
    public static final String PREFS = "morphe_settings";

    private static final int BACKGROUND = Color.parseColor("#121212");
    private static final int TEXT_PRIMARY = Color.WHITE;
    private static final int TEXT_SECONDARY = Color.parseColor("#9A9A9A");
    private static final int DIVIDER = Color.parseColor("#2A2A2A");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Morphe");
        title.setTextSize(22);
        title.setTextColor(TEXT_PRIMARY);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Toggles appear here as their patches land.");
        subtitle.setTextSize(13);
        subtitle.setTextColor(TEXT_SECONDARY);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(subtitle);

        addDivider(root);

        // Placeholder. The first settings-toggled patch replaces this with a real switch
        // bound to its own key through isEnabled()/setEnabled() below.
        addToggleRow(root, "Placeholder", "No toggles yet.", "placeholder", false);

        setContentView(root);
    }

    /** Reads a Morphe toggle. Settings-toggled patches call this instead of reading prefs directly. */
    public static boolean isEnabled(android.content.Context context, String key, boolean def) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, def);
    }

    private void addDivider(LinearLayout parent) {
        TextView divider = new TextView(this);
        divider.setBackgroundColor(DIVIDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2);
        params.setMargins(0, 24, 0, 24);
        divider.setLayoutParams(params);
        parent.addView(divider);
    }

    private void addToggleRow(LinearLayout parent, String label, String description,
                              final String key, boolean def) {
        final SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        final float density = getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int rowPad = (int) (12 * density);
        rowParams.setMargins(0, rowPad, 0, rowPad);
        row.setLayoutParams(rowParams);

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textBlock.setLayoutParams(textParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setTextColor(TEXT_PRIMARY);
        textBlock.addView(labelView);

        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextSize(13);
        descView.setTextColor(TEXT_SECONDARY);
        textBlock.addView(descView);

        row.addView(textBlock);

        Switch toggle = new Switch(this);
        toggle.setChecked(prefs.getBoolean(key, def));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        row.addView(toggle);

        parent.addView(row);
    }
}
