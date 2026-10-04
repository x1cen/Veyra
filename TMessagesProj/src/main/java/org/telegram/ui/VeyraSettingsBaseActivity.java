package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

// Veyra: shared base for all categorized Veyra Settings sub-screens
// (Chat List, Composing, Media, Privacy, About, etc). Subclasses only
// need to supply the row list; this class owns the RecyclerView/Adapter
// plumbing, click routing and cell binding so each section stays a
// small declarative list instead of hand-rolled adapter boilerplate.
public abstract class VeyraSettingsBaseActivity extends BaseFragment {

    protected RecyclerListView listView;
    protected RowAdapter listAdapter;
    protected List<VeyraSettingsRow> rows = new ArrayList<>();

    protected abstract String getScreenTitle();
    protected abstract List<VeyraSettingsRow> buildRows();

    protected void reloadRows() {
        rows = buildRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        rows = buildRows();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Veyra: rebuild rows whenever this screen becomes visible again,
        // e.g. after popping back from a sub-screen (Settings Lock setup,
        // online-mode picker, etc). Without this, rows that were built
        // once in onFragmentCreate() keep showing stale state (an applied
        // change is saved correctly but the row/label/button visibility
        // only catches up after fully leaving and re-entering Settings).
        reloadRows();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getScreenTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), AndroidUtilities.dp(16));
        listView.setClipToPadding(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listAdapter = new RowAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position < 0 || position >= rows.size()) {
                return;
            }
            VeyraSettingsRow row = rows.get(position);
            if (row.type == VeyraSettingsRow.Type.TOGGLE && row.getter != null && row.setter != null) {
                if (row.onClick != null) {
                    boolean isRightSide = LocaleController.isRTL ? x <= AndroidUtilities.dp(76) : x >= view.getMeasuredWidth() - AndroidUtilities.dp(76);
                    if (isRightSide) {
                        boolean newVal = !row.getter.getAsBoolean();
                        row.setter.accept(newVal);
                        ((TextCheckCell) view).setChecked(newVal);
                        if (row.onToggled != null) {
                            row.onToggled.run();
                        }
                    } else {
                        row.onClick.run();
                    }
                } else {
                    boolean newVal = !row.getter.getAsBoolean();
                    row.setter.accept(newVal);
                    ((TextCheckCell) view).setChecked(newVal);
                    if (row.onToggled != null) {
                        row.onToggled.run();
                    }
                }
            } else if (row.onClick != null) {
                row.onClick.run();
            }
        });

        return fragmentView;
    }

    protected class RowAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public RowAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= rows.size()) {
                return false;
            }
            VeyraSettingsRow.Type type = rows.get(position).type;
            return type == VeyraSettingsRow.Type.TOGGLE || type == VeyraSettingsRow.Type.DETAIL ||
                    type == VeyraSettingsRow.Type.BUTTON || type == VeyraSettingsRow.Type.CATEGORY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 1:
                    HeaderCell headerCell = new HeaderCell(mContext);
                    headerCell.setBackgroundColor(0);
                    view = headerCell;
                    break;
                case 2:
                    view = new TextCheckCell(mContext);
                    break;
                case 3:
                    view = new TextSettingsCell(mContext);
                    break;
                case 4:
                    view = new TextDetailSettingsCell(mContext);
                    break;
                case 5:
                    view = new CategoryCell(mContext);
                    break;
                default:
                    view = new View(mContext);
                    view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(12)));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= rows.size()) {
                return;
            }
            VeyraSettingsRow row = rows.get(position);

            if (row.type != VeyraSettingsRow.Type.HEADER && row.type != VeyraSettingsRow.Type.SHADOW) {
                boolean isTop = (position == 0 || rows.get(position - 1).type == VeyraSettingsRow.Type.HEADER || rows.get(position - 1).type == VeyraSettingsRow.Type.SHADOW);
                boolean isBottom = (position == rows.size() - 1 || rows.get(position + 1).type == VeyraSettingsRow.Type.HEADER || rows.get(position + 1).type == VeyraSettingsRow.Type.SHADOW);

                int topRad = isTop ? 14 : 0;
                int bottomRad = isBottom ? 14 : 0;
                boolean needDivider = !isBottom && row.needDivider;

                int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
                int selColor = Theme.getColor(Theme.key_listSelector);
                holder.itemView.setBackground(Theme.createRadSelectorDrawable(bgColor, selColor, topRad, bottomRad));

                switch (holder.getItemViewType()) {
                    case 2: {
                        TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                        boolean checked = row.getter != null && row.getter.getAsBoolean();
                        if (row.subtitle != null) {
                            checkCell.setTextAndValueAndCheck(row.title, row.subtitle, checked, true, needDivider);
                        } else {
                            checkCell.setTextAndCheck(row.title, checked, needDivider);
                        }
                        break;
                    }
                    case 3: {
                        TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                        textCell.setText(row.title, needDivider);
                        if (row.redText) {
                            textCell.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
                        }
                        break;
                    }
                    case 4: {
                        TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                        String value = row.valueSupplier != null ? row.valueSupplier.get() : row.subtitle;
                        detailCell.setTextAndValue(row.title, value, needDivider);
                        break;
                    }
                    case 5: {
                        CategoryCell catCell = (CategoryCell) holder.itemView;
                        int cTop = row.iconColorTop != 0 ? row.iconColorTop : 0xFF4F85F6;
                        int cBot = row.iconColorBottom != 0 ? row.iconColorBottom : 0xFF3568E8;
                        catCell.set(cTop, cBot, row.icon, row.title, row.subtitle, needDivider);
                        break;
                    }
                }
            } else if (holder.getItemViewType() == 1) {
                ((HeaderCell) holder.itemView).setText(row.title);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position < 0 || position >= rows.size()) {
                return 0;
            }
            switch (rows.get(position).type) {
                case HEADER:
                    return 1;
                case TOGGLE:
                    return 2;
                case BUTTON:
                    return 3;
                case DETAIL:
                    return 4;
                case CATEGORY:
                    return 5;
                default:
                    return 0;
            }
        }
    }

    public static class CategoryCell extends FrameLayout {
        private final FrameLayout iconLayout;
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final ImageView arrowView;
        private boolean needDivider;

        public CategoryCell(Context context) {
            super(context);

            iconLayout = new FrameLayout(context);
            addView(iconLayout, LayoutHelper.createFrame(28, 28, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL, 16, 0, 16, 0));

            iconView = new ImageView(context);
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconView.setColorFilter(new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.SRC_IN));
            iconLayout.addView(iconView, LayoutHelper.createFrame(20, 20, Gravity.CENTER));

            LinearLayout textLayout = new LinearLayout(context);
            textLayout.setOrientation(LinearLayout.VERTICAL);

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            textLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setVisibility(GONE);
            textLayout.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            if (LocaleController.isRTL) {
                addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 44, 0, 60, 0));
            } else {
                addView(textLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 60, 0, 44, 0));
            }

            arrowView = new ImageView(context);
            arrowView.setImageResource(R.drawable.msg_arrowright);
            arrowView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
            if (LocaleController.isRTL) {
                arrowView.setScaleX(-1.0f);
            }
            addView(arrowView, LayoutHelper.createFrame(24, 24, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 16, 0, 16, 0));

            setWillNotDraw(false);
        }

        public void set(int iconColorTop, int iconColorBottom, int icon, CharSequence title, CharSequence subtitle, boolean divider) {
            GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{iconColorTop, iconColorBottom});
            gd.setCornerRadius(AndroidUtilities.dp(10));
            iconLayout.setBackground(gd);
            iconView.setImageResource(icon);
            titleView.setText(title);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(VISIBLE);
            } else {
                subtitleView.setVisibility(GONE);
            }
            needDivider = divider;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(subtitleView.getVisibility() == VISIBLE ? 58 : 50), MeasureSpec.EXACTLY)
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needDivider) {
                int left = LocaleController.isRTL ? 0 : AndroidUtilities.dp(60);
                int right = LocaleController.isRTL ? getMeasuredWidth() - AndroidUtilities.dp(60) : getMeasuredWidth();
                canvas.drawLine(left, getMeasuredHeight() - 1, right, getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }
}
