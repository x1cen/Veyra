package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
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
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listAdapter = new RowAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position < 0 || position >= rows.size()) {
                return;
            }
            VeyraSettingsRow row = rows.get(position);
            if (row.type == VeyraSettingsRow.Type.TOGGLE && row.getter != null && row.setter != null) {
                boolean newVal = !row.getter.getAsBoolean();
                row.setter.accept(newVal);
                ((TextCheckCell) view).setChecked(newVal);
                if (row.onToggled != null) {
                    row.onToggled.run();
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
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new ShadowSectionCell(mContext);
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
            switch (holder.getItemViewType()) {
                case 1: {
                    ((HeaderCell) holder.itemView).setText(row.title);
                    break;
                }
                case 2: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    boolean checked = row.getter != null && row.getter.getAsBoolean();
                    if (row.subtitle != null) {
                        checkCell.setTextAndValueAndCheck(row.title, row.subtitle, checked, true, row.needDivider);
                    } else {
                        checkCell.setTextAndCheck(row.title, checked, row.needDivider);
                    }
                    break;
                }
                case 3: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setText(row.title, row.needDivider);
                    if (row.redText) {
                        textCell.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
                    }
                    break;
                }
                case 4: {
                    TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                    String value = row.valueSupplier != null ? row.valueSupplier.get() : row.subtitle;
                    detailCell.setTextAndValue(row.title, value, row.needDivider);
                    break;
                }
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
                case CATEGORY:
                    return 4;
                default:
                    return 0;
            }
        }
    }
}
