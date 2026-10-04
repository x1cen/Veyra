package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.VeyraConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VeyraScopeSettingsActivity extends BaseFragment {

    private int category;
    private int peerType;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int onlyPrivateRow = -1;
    private int onlyPrivateInfoRow = -1;
    private int exceptionsHeaderRow = -1;
    private int addExceptionRow = -1;
    private int exceptionsStartRow = -1;
    private int exceptionsEndRow = -1;
    private int deleteAllExceptionsRow = -1;
    private int lastDividerRow = -1;
    private int rowCount = 0;

    private final ArrayList<Long> exceptionsList = new ArrayList<>();

    public VeyraScopeSettingsActivity(Bundle args) {
        super(args);
        if (args != null) {
            category = args.getInt("category", VeyraConfig.CATEGORY_ANTI_DELETE);
            peerType = args.getInt("peerType", VeyraConfig.PEER_GROUP);
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;
        onlyPrivateRow = -1;
        onlyPrivateInfoRow = -1;
        exceptionsHeaderRow = -1;
        addExceptionRow = -1;
        exceptionsStartRow = -1;
        exceptionsEndRow = -1;
        deleteAllExceptionsRow = -1;
        lastDividerRow = -1;

        if (peerType == VeyraConfig.PEER_GROUP || peerType == VeyraConfig.PEER_CHANNEL) {
            onlyPrivateRow = rowCount++;
            onlyPrivateInfoRow = rowCount++;
        }

        exceptionsHeaderRow = rowCount++;
        addExceptionRow = rowCount++;

        exceptionsList.clear();
        HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
        for (Long did : map.keySet()) {
            if (peerType == VeyraConfig.PEER_PRIVATE && did > 0) {
                TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(did);
                if (u == null || !u.bot) exceptionsList.add(did);
            } else if (peerType == VeyraConfig.PEER_BOT && did > 0) {
                TLRPC.User u = MessagesController.getInstance(currentAccount).getUser(did);
                if (u != null && u.bot) exceptionsList.add(did);
            } else if (peerType == VeyraConfig.PEER_GROUP && did < 0) {
                TLRPC.Chat c = MessagesController.getInstance(currentAccount).getChat(-did);
                if (c == null || !ChatObject.isChannel(c) || c.megagroup) exceptionsList.add(did);
            } else if (peerType == VeyraConfig.PEER_CHANNEL && did < 0) {
                TLRPC.Chat c = MessagesController.getInstance(currentAccount).getChat(-did);
                if (c != null && ChatObject.isChannel(c) && !c.megagroup) exceptionsList.add(did);
            }
        }

        if (!exceptionsList.isEmpty()) {
            exceptionsStartRow = rowCount;
            rowCount += exceptionsList.size();
            exceptionsEndRow = rowCount;
            deleteAllExceptionsRow = rowCount++;
        }

        lastDividerRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        String title;
        if (peerType == VeyraConfig.PEER_PRIVATE) {
            title = LocaleController.getString("PrivateChats", R.string.PrivateChats);
        } else if (peerType == VeyraConfig.PEER_GROUP) {
            title = LocaleController.getString("Groups", R.string.Groups);
        } else if (peerType == VeyraConfig.PEER_CHANNEL) {
            title = LocaleController.getString("Channels", R.string.Channels);
        } else {
            title = LocaleController.getString("Bots", R.string.Bots);
        }
        actionBar.setTitle(title);

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
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position) -> {
            if (position == onlyPrivateRow) {
                boolean val = !VeyraConfig.isOnlyPrivate(category, peerType);
                VeyraConfig.setOnlyPrivate(category, peerType, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == addExceptionRow) {
                openAddExceptionDialog();
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                int index = position - exceptionsStartRow;
                if (index >= 0 && index < exceptionsList.size()) {
                    long did = exceptionsList.get(index);
                    showDeleteExceptionAlert(did);
                }
            } else if (position == deleteAllExceptionsRow) {
                showDeleteAllExceptionsAlert();
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void openAddExceptionDialog() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("allowGlobalSearch", false);

        if (peerType == VeyraConfig.PEER_GROUP) {
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_GROUPS_ONLY);
            if (VeyraConfig.isOnlyPrivate(category, peerType)) {
                args.putBoolean("onlyPrivateGroups", true);
            }
        } else if (peerType == VeyraConfig.PEER_CHANNEL) {
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_CHANNELS_ONLY);
            if (VeyraConfig.isOnlyPrivate(category, peerType)) {
                args.putBoolean("onlyPrivateChannels", true);
            }
        } else if (peerType == VeyraConfig.PEER_BOT) {
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_USERS_ONLY);
            args.putBoolean("onlyBots", true);
        } else {
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_USERS_ONLY);
            args.putBoolean("onlyUsers", true);
        }

        DialogsActivity activity = new DialogsActivity(args);
        activity.setDelegate((fragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids != null && !dids.isEmpty()) {
                long did = dids.get(0).dialogId;
                VeyraConfig.addException(category, did, true);
                updateRows();
            }
            return true;
        });
        presentFragment(activity);
    }

    private void showDeleteExceptionAlert(long dialogId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage(LocaleController.getString("NotificationsDeleteExceptionConfirmation", R.string.NotificationsDeleteExceptionConfirmation));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            VeyraConfig.removeException(category, dialogId);
            updateRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private void showDeleteAllExceptionsAlert() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("NotificationsDeleteAllExceptionTitle", R.string.NotificationsDeleteAllExceptionTitle));
        builder.setMessage(LocaleController.getString("NotificationsDeleteAllExceptionAlert", R.string.NotificationsDeleteAllExceptionAlert));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            VeyraConfig.clearExceptions(category, peerType);
            updateRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos == onlyPrivateRow || pos == addExceptionRow || (pos >= exceptionsStartRow && pos < exceptionsEndRow) || pos == deleteAllExceptionsRow;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 2:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new UserCell(mContext, 4, 0, false, false);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                default:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (peerType == VeyraConfig.PEER_GROUP) {
                        checkCell.setTextAndCheck("Only Private Groups", VeyraConfig.isOnlyPrivate(category, peerType), false);
                    } else if (peerType == VeyraConfig.PEER_CHANNEL) {
                        checkCell.setTextAndCheck("Only Private Channels", VeyraConfig.isOnlyPrivate(category, peerType), false);
                    }
                    break;
                }
                case 1: {
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (peerType == VeyraConfig.PEER_GROUP) {
                        infoCell.setText("When enabled, only private groups will be included by default.");
                    } else if (peerType == VeyraConfig.PEER_CHANNEL) {
                        infoCell.setText("When enabled, only private channels will be included by default.");
                    }
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(LocaleController.getString("NotificationsExceptions", R.string.NotificationsExceptions));
                    break;
                }
                case 3: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == addExceptionRow) {
                        textCell.setTextAndIcon(LocaleController.getString("NotificationsAddAnException", R.string.NotificationsAddAnException), R.drawable.msg_contact_add, true);
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    } else if (position == deleteAllExceptionsRow) {
                        textCell.setText(LocaleController.getString("NotificationsDeleteAllException", R.string.NotificationsDeleteAllException), false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                }
                case 4: {
                    UserCell userCell = (UserCell) holder.itemView;
                    int index = position - exceptionsStartRow;
                    if (index >= 0 && index < exceptionsList.size()) {
                        long did = exceptionsList.get(index);
                        TLObject object;
                        if (DialogObject.isUserDialog(did)) {
                            object = MessagesController.getInstance(currentAccount).getUser(did);
                        } else {
                            object = MessagesController.getInstance(currentAccount).getChat(-did);
                        }
                        userCell.setData(object, null, null, 0);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == onlyPrivateRow) {
                return 0;
            } else if (position == onlyPrivateInfoRow) {
                return 1;
            } else if (position == exceptionsHeaderRow) {
                return 2;
            } else if (position == addExceptionRow || position == deleteAllExceptionsRow) {
                return 3;
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                return 4;
            }
            return 5;
        }
    }
}
