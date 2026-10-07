package org.telegram.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class VeyraGhostExceptionsActivity extends BaseFragment {

    private final int category;
    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int infoRow;
    private int headerRow;
    private int addExceptionRow;
    private int exceptionsStartRow;
    private int exceptionsEndRow;
    private int deleteAllRow;
    private int rowCount;

    private final ArrayList<Long> exceptionsList = new ArrayList<>();

    public VeyraGhostExceptionsActivity() {
        this(VeyraConfig.CATEGORY_GHOST_TYPING);
    }

    public VeyraGhostExceptionsActivity(int category) {
        super();
        this.category = category;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;
        infoRow = rowCount++;
        headerRow = rowCount++;
        addExceptionRow = rowCount++;

        exceptionsList.clear();
        HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
        synchronized (map) {
            exceptionsList.addAll(map.keySet());
        }

        // Sort: Users/Bots first, Groups second, Channels last
        Collections.sort(exceptionsList, (d1, d2) -> {
            int p1 = getPeerPriority(d1);
            int p2 = getPeerPriority(d2);
            if (p1 != p2) return Integer.compare(p1, p2);
            return Long.compare(Math.abs(d1), Math.abs(d2));
        });

        if (!exceptionsList.isEmpty()) {
            exceptionsStartRow = rowCount;
            rowCount += exceptionsList.size();
            exceptionsEndRow = rowCount;
            deleteAllRow = rowCount++;
        } else {
            exceptionsStartRow = -1;
            exceptionsEndRow = -1;
            deleteAllRow = -1;
        }

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private int getPeerPriority(long did) {
        if (did > 0) return 1; // User or Bot
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
        if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup) {
            return 3; // Channel
        }
        return 2; // Group or Supergroup
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
            actionBar.setTitle("Chat Action Exceptions");
        } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
            actionBar.setTitle("Read Receipts Exceptions");
        } else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
            actionBar.setTitle("Channel Browsing Exceptions");
        } else {
            actionBar.setTitle("Ghost Mode Exceptions");
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position) -> {
            if (position == addExceptionRow) {
                Bundle args = new Bundle();
                args.putBoolean("onlySelect", true);
                if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                    args.putBoolean("checkCanWrite", true);
                }
                args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_DEFAULT);

                DialogsActivity activity = new DialogsActivity(args);
                activity.setDelegate((fragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
                    if (dids != null && !dids.isEmpty()) {
                        long did = dids.get(0).dialogId;
                        if (category == VeyraConfig.CATEGORY_GHOST_TYPING && DialogObject.isChatDialog(did)) {
                            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                            if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup) {
                                AndroidUtilities.runOnUIThread(() -> BulletinFactory.of(VeyraGhostExceptionsActivity.this).createErrorBulletin("Channels do not support chat actions").show());
                                return true;
                            }
                        }
                        showAddActionDialog(did);
                    }
                    return true;
                });
                presentFragment(activity);
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                int index = position - exceptionsStartRow;
                if (index >= 0 && index < exceptionsList.size()) {
                    long did = exceptionsList.get(index);
                    showDeleteExceptionAlert(did);
                }
            } else if (position == deleteAllRow) {
                if (getParentActivity() == null) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
                builder.setMessage("Delete all exceptions?");
                builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
                    HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
                    synchronized (map) {
                        map.clear();
                    }
                    VeyraConfig.clearGhostExceptions();
                    updateRows();
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            }
        });

        return fragmentView;
    }

    private void showAddActionDialog(long dialogId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Configure Exception");
        String[] options;
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
            options = new String[]{"Never Send Chat Action (Always Hide)", "Always Send Action (Bypass Ghost)"};
        } else {
            options = new String[]{"Always Hide (Never Send)", "Always Send (Bypass Ghost)"};
        }
        builder.setItems(options, (dialog, which) -> {
            boolean shouldHide = (which == 0);
            VeyraConfig.addException(category, dialogId, shouldHide);
            updateRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDeleteExceptionAlert(long dialogId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage("Remove this chat from exceptions?");
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            VeyraConfig.removeException(category, dialogId);
            updateRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos == addExceptionRow || (pos >= exceptionsStartRow && pos < exceptionsEndRow) || pos == deleteAllRow;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextInfoPrivacyCell(context);
                    break;
                case 1:
                    view = new HeaderCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                default:
                    view = new UserCell(context, 4, 0, false);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                        infoCell.setText("Manage chats where typing and uploading indicators are always hidden or sent normally. Broadcast channels are excluded as they do not support typing actions.");
                    } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                        infoCell.setText("Manage chats where read receipts are always hidden or sent normally.");
                    } else {
                        infoCell.setText("Chats added here have specific custom stealth rules applied.");
                    }
                    break;
                }
                case 1: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(LocaleController.getString("NotificationsExceptions", R.string.NotificationsExceptions));
                    break;
                }
                case 2: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == addExceptionRow) {
                        textCell.setTextAndIcon(LocaleController.getString("NotificationsAddAnException", R.string.NotificationsAddAnException), R.drawable.msg_contact_add, !exceptionsList.isEmpty());
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    } else if (position == deleteAllRow) {
                        textCell.setText(LocaleController.getString("NotificationsDeleteAllException", R.string.NotificationsDeleteAllException), false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                }
                case 3: {
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
                        Boolean val = VeyraConfig.getException(category, did);
                        String statusStr = (val != null && val) ? "Always Hidden" : "Always Sent";
                        userCell.setData(object, null, statusStr, 0);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == infoRow) {
                return 0;
            } else if (position == headerRow) {
                return 1;
            } else if (position == addExceptionRow || position == deleteAllRow) {
                return 2;
            }
            return 3;
        }
    }
}
