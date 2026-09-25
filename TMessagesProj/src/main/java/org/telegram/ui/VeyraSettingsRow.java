package org.telegram.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Veyra: generic row model used by VeyraSettingsBaseActivity to render
// categorized, richly-described settings screens without duplicating
// RecyclerView/Adapter boilerplate in every section activity.
public class VeyraSettingsRow {

    public enum Type { HEADER, TOGGLE, DETAIL, BUTTON, SHADOW, CATEGORY }

    public Type type;
    public String title;
    public String subtitle;
    public Supplier<String> valueSupplier;
    public BooleanSupplier getter;
    public Consumer<Boolean> setter;
    public Runnable onToggled;
    public Runnable onClick;
    public boolean needDivider = true;
    public boolean redText = false;
    public int icon;

    public static VeyraSettingsRow header(String title) {
        VeyraSettingsRow row = new VeyraSettingsRow();
        row.type = Type.HEADER;
        row.title = title;
        return row;
    }

    public static VeyraSettingsRow toggle(String title, String subtitle, BooleanSupplier getter, Consumer<Boolean> setter, boolean divider) {
        return toggle(title, subtitle, getter, setter, divider, null);
    }

    public static VeyraSettingsRow toggle(String title, String subtitle, BooleanSupplier getter, Consumer<Boolean> setter, boolean divider, Runnable onToggled) {
        VeyraSettingsRow row = new VeyraSettingsRow();
        row.type = Type.TOGGLE;
        row.title = title;
        row.subtitle = subtitle;
        row.getter = getter;
        row.setter = setter;
        row.needDivider = divider;
        row.onToggled = onToggled;
        return row;
    }

    public static VeyraSettingsRow detail(String title, Supplier<String> valueSupplier, boolean divider, Runnable onClick) {
        VeyraSettingsRow row = new VeyraSettingsRow();
        row.type = Type.DETAIL;
        row.title = title;
        row.valueSupplier = valueSupplier;
        row.needDivider = divider;
        row.onClick = onClick;
        return row;
    }

    public static VeyraSettingsRow detail(String title, String value, boolean divider) {
        return detail(title, () -> value, divider, null);
    }

    public static VeyraSettingsRow button(String title, boolean redText, boolean divider, Runnable onClick) {
        VeyraSettingsRow row = new VeyraSettingsRow();
        row.type = Type.BUTTON;
        row.title = title;
        row.redText = redText;
        row.needDivider = divider;
        row.onClick = onClick;
        return row;
    }

    // A clickable row that opens a sub-section (used on the main Veyra Settings hub).
    public static VeyraSettingsRow category(String title, String subtitle, Runnable onClick) {
        VeyraSettingsRow row = new VeyraSettingsRow();
        row.type = Type.CATEGORY;
        row.title = title;
        row.subtitle = subtitle;
        row.needDivider = true;
        row.onClick = onClick;
        return row;
    }

    public static VeyraSettingsRow shadow() {
        VeyraSettingsRow row = new VeyraSettingsRow();
        row.type = Type.SHADOW;
        return row;
    }
}
