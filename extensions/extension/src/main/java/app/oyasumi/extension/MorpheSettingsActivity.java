package app.oyasumi.extension;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

/**
 * The Morphe settings screen, opened from the "Morphe" row in Account Settings via the
 * `morphe://settings` intent-filter.
 *
 * <p>Material 3 widgets under the `Theme.Material3.DayNight.NoActionBar` theme declared for this
 * activity, so light and dark follow the system automatically. Colors come from framework theme
 * attributes with dark fallbacks.
 *
 * <p>Every widget call here was checked against Pinterest's own copy of the Material library,
 * because that copy is R8-stripped to what Pinterest itself uses and anything else is a
 * `NoSuchMethodError` at runtime. What survived the check: `MaterialTextView(Context)` plus the
 * `TextView` methods it inherits, and the two-argument constructors of `MaterialDivider` and
 * `MaterialSwitch` (called with a null `AttributeSet`, which is exactly what a single-argument
 * constructor does internally). What did not survive and is therefore not used: `MaterialToolbar`
 * entirely (its copy kept 10 methods, none of `setTitle`, navigation, or menu handling),
 * single-argument `MaterialDivider`/`MaterialSwitch` constructors, and `DynamicColors`, which is
 * absent, so there is no wallpaper-tinted dynamic color. All other calls (`setText`,
 * `setChecked`, `addView`, layout params) resolve to framework superclasses that cannot be
 * stripped.
 *
 * <p>No Pinterest resources are referenced, for the same reason the activity keeps a framework
 * theme.
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
        final float density = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(background);
        // The activity draws under a transparent status bar with no toolbar to absorb the
        // inset, so pad by the status bar height. Resolved from the framework, which always
        // carries this dimen, with a 24dp fallback that matches it on almost all devices.
        root.setPadding(0, statusBarHeight(density), 0, 0);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * density);
        body.setPadding(pad, (int) (8 * density), pad, pad);

        MaterialTextView title = new MaterialTextView(this);
        title.setText("Morphe");
        title.setTextSize(28);
        title.setTextColor(textPrimary);
        body.addView(title);

        MaterialTextView subtitle = new MaterialTextView(this);
        subtitle.setText(pinterestVersion() + "  •  patched with Oyasumi");
        subtitle.setTextSize(14);
        subtitle.setTextColor(textSecondary);
        body.addView(subtitle);

        body.addView(new MaterialDivider(this, null));

        MaterialTextView section = new MaterialTextView(this);
        section.setText("Toggles");
        section.setTextSize(13);
        section.setTextColor(textSecondary);
        body.addView(section);

        // Placeholder. The first settings-toggled patch replaces this with a real switch
        // bound to its own key through isEnabled() below.
        addToggleRow(body, "Placeholder", "No toggles yet.", "placeholder", false);

        root.addView(body);
        setContentView(root);
    }

    private int statusBarHeight(float density) {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return getResources().getDimensionPixelSize(resId);
        }
        return (int) (24 * density);
    }

    private String pinterestVersion() {
        try {
            return "Pinterest "
                    + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "Pinterest";
        }
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
            try (TypedArray a = obtainStyledAttributes(new int[]{attr})) {
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

        MaterialSwitch toggle = new MaterialSwitch(this, null);
        toggle.setChecked(prefs.getBoolean(key, def));
        toggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        row.addView(toggle);

        parent.addView(row);
    }
}
