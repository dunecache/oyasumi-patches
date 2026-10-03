package app.oyasumi.extension;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

/**
 * The Morphe settings screen, opened from the "Morphe" row in Account Settings via the
 * `morphe://settings` intent-filter.
 *
 * <p>Material 3, using only components verified present in the target APK
 * (`MaterialToolbar`, `MaterialDivider`, `MaterialTextView`, `MaterialSwitch`) under the
 * `Theme.Material3.DayNight.NoActionBar` theme declared for this activity in the manifest.
 * Colors come from framework theme attributes (`windowBackground`, `textColorPrimary`,
 * `textColorSecondary`), so light and dark follow the system automatically with no hardcoded
 * palette. The selected switch tint comes from the theme's `colorPrimary` the same way.
 *
 * <p>Deliberately no dynamic color: Pinterest does not bundle `DynamicColors`, so there is
 * nothing to resolve a wallpaper palette with. No Pinterest resources are referenced either,
 * for the same reason the activity keeps a framework theme.
 *
 * <p>The switches here are placeholders for the settings-toggled patches that do not exist yet;
 * each of those patches will read its own key from the same preferences file. Adding a real
 * toggle later means adding one row here and one read there, nothing else.
 */
public final class MorpheSettingsActivity extends Activity {

    /** Preferences file every Morphe toggle reads and writes. */
    public static final String PREFS = "morphe_settings";

    private int background;
    private int textPrimary;
    private int textSecondary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveThemeColors();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("Morphe");
        // No navigation icon: the system back button/gesture already finishes the activity,
        // and every icon would reference a drawable that must exist in Pinterest's resources.
        root.addView(toolbar);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        body.setPadding(pad, pad, pad, pad);

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText("Toggles appear here as their patches land.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(textSecondary);
        body.addView(subtitle);

        body.addView(new MaterialDivider(this));

        // Placeholder. The first settings-toggled patch replaces this with a real switch
        // bound to its own key through isEnabled() below.
        addToggleRow(body, "Placeholder", "No toggles yet.", "placeholder", false);

        root.addView(body);
        setContentView(root);
    }

    /** Reads a Morphe toggle. Settings-toggled patches call this instead of reading prefs directly. */
    public static boolean isEnabled(android.content.Context context, String key, boolean def) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(key, def);
    }

    private void resolveThemeColors() {
        background = resolveAttr(android.R.attr.windowBackground, Color.parseColor("#121212"));
        textPrimary = resolveAttr(android.R.attr.textColorPrimary, Color.WHITE);
        textSecondary = resolveAttr(android.R.attr.textColorSecondary, Color.parseColor("#9A9A9A"));
    }

    private int resolveAttr(int attr, int fallback) {
        TypedValue out = new TypedValue();
        if (getTheme().resolveAttribute(attr, out, true)) {
            if (out.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && out.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return out.data;
            }
            try (android.content.res.TypedArray a =
                         obtainStyledAttributes(new int[]{attr})) {
                return a.getColor(0, fallback);
            }
        }
        return fallback;
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

        MaterialTextView labelView = new MaterialTextView(this);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setTextColor(textPrimary);
        textBlock.addView(labelView);

        MaterialTextView descView = new MaterialTextView(this);
        descView.setText(description);
        descView.setTextSize(14);
        descView.setTextColor(textSecondary);
        textBlock.addView(descView);

        row.addView(textBlock);

        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setChecked(prefs.getBoolean(key, def));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        row.addView(toggle);

        parent.addView(row);
    }
}
