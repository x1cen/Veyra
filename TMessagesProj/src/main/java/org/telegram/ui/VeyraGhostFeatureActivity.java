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
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;

public class VeyraGhostFeatureActivity extends BaseFragment {

    private final int category;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int masterHeaderRow = -1;
    private int masterToggleRow = -1;
    private int masterInfoRow = -1;

    private int chatTypesHeaderRow = -1;
    private int typePrivateRow = -1;
    private int typeGroupsRow = -1;
    private int typeChannelsRow = -1;
    private int typeBotsRow = -1;
    private int typeExtra1Row = -1;
    private int typeExtra2Row = -1;
    private int chatTypesInfoRow = -1;

    private int exceptionsHeaderRow = -1;
    private int addExceptionRow = -1;
    private int exceptionsStartRow = -1;
    private int exceptionsEndRow = -1;
    private int deleteAllExceptionsRow = -1;
    private int exceptionsInfoRow = -1;
    private int rowCount = 0;

    private final ArrayList<Long> exceptionsList = new ArrayList<>();

    public VeyraGhostFeatureActivity(int category) {
        super();
        this.category = category;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private boolean hasChatTypes() {
        return category == VeyraConfig.CATEGORY_GHOST_READ ||
               category == VeyraConfig.CATEGORY_GHOST_TYPING ||
               category == VeyraConfig.CATEGORY_GHOST_UPLOAD ||
               category == VeyraConfig.CATEGORY_GHOST_STORIES;
    }

    private void updateRows() {
        rowCount = 0;
        masterHeaderRow = -1;
        masterToggleRow = -1;
        masterInfoRow = -1;

        chatTypesHeaderRow = -1;
        typePrivateRow = -1;
        typeGroupsRow = -1;
        typeChannelsRow = -1;
        typeBotsRow = -1;
        typeExtra1Row = -1;
        typeExtra2Row = -1;
        chatTypesInfoRow = -1;

        exceptionsHeaderRow = -1;
        addExceptionRow = -1;
        exceptionsStartRow = -1;
        exceptionsEndRow = -1;
        deleteAllExceptionsRow = -1;
        exceptionsInfoRow = -1;

        // Section 1: Master Toggle
        masterHeaderRow = rowCount++;
        masterToggleRow = rowCount++;
        masterInfoRow = rowCount++;

        // Section 2: Chat Types (Only for features that filter by chat types)
        if (hasChatTypes()) {
            chatTypesHeaderRow = rowCount++;
            if (category == VeyraConfig.CATEGORY_GHOST_TYPING || category == VeyraConfig.CATEGORY_GHOST_UPLOAD) {
                typePrivateRow = rowCount++;
                typeGroupsRow = rowCount++;
            } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                typePrivateRow = rowCount++;
                typeGroupsRow = rowCount++;
                typeChannelsRow = rowCount++;
                typeBotsRow = rowCount++;
                typeExtra1Row = rowCount++; // Read on Reply
                typeExtra2Row = rowCount++; // Hide Voice / Video Read Receipts
            } else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
                typePrivateRow = rowCount++; // Users & Contacts
                typeChannelsRow = rowCount++; // Channels
            }
            chatTypesInfoRow = rowCount++;
        }

        // Section 3: Exceptions
        exceptionsHeaderRow = rowCount++;
        addExceptionRow = rowCount++;

        exceptionsList.clear();
        HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
        if (map != null) {
            for (Long did : map.keySet()) {
                exceptionsList.add(did);
            }
        }

        if (!exceptionsList.isEmpty()) {
            exceptionsStartRow = rowCount;
            rowCount += exceptionsList.size();
            exceptionsEndRow = rowCount;
            deleteAllExceptionsRow = rowCount++;
        }

        exceptionsInfoRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(getTitleForCategory());

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
        listView.setAdapter(listAdapter = new ListAdapter(context));

        listView.setOnItemClickListener((view, position) -> {
            if (position == masterToggleRow) {
                boolean val = !isMasterEnabled();
                setMasterEnabled(val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == typePrivateRow) {
                boolean val = !isTypeEnabled(VeyraConfig.PEER_PRIVATE);
                setTypeEnabled(VeyraConfig.PEER_PRIVATE, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == typeGroupsRow) {
                boolean val = !isTypeEnabled(VeyraConfig.PEER_GROUP);
                setTypeEnabled(VeyraConfig.PEER_GROUP, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == typeChannelsRow) {
                boolean val = !isTypeEnabled(VeyraConfig.PEER_CHANNEL);
                setTypeEnabled(VeyraConfig.PEER_CHANNEL, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == typeBotsRow) {
                boolean val = !isTypeEnabled(VeyraConfig.PEER_BOT);
                setTypeEnabled(VeyraConfig.PEER_BOT, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == typeExtra1Row) {
                VeyraConfig.setGhostReadOnReply(!VeyraConfig.ghostReadOnReply);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(VeyraConfig.ghostReadOnReply);
                }
            } else if (position == typeExtra2Row) {
                VeyraConfig.setGhostHideReadContents(!VeyraConfig.ghostHideReadContents);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(VeyraConfig.ghostHideReadContents);
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

    private String getTitleForCategory() {
        if (category == VeyraConfig.CATEGORY_GHOST_ONLINE) return LocaleController.getString("VeyraGhostHideOnline", R.string.VeyraGhostHideOnline);
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) return LocaleController.getString("VeyraGhostHideTyping", R.string.VeyraGhostHideTyping);
        if (category == VeyraConfig.CATEGORY_GHOST_UPLOAD) return "Hide Media Uploading";
        if (category == VeyraConfig.CATEGORY_GHOST_READ) return LocaleController.getString("VeyraGhostHideRead", R.string.VeyraGhostHideRead);
        if (category == VeyraConfig.CATEGORY_GHOST_STORIES) return LocaleController.getString("VeyraGhostHideStories", R.string.VeyraGhostHideStories);
        if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) return "Anonymous Channel Browsing";
        if (category == VeyraConfig.CATEGORY_GHOST_SECRET_READ) return "Secret Chat Read Receipts";
        if (category == VeyraConfig.CATEGORY_GHOST_READ_ON_REPLY) return "Mark Read on Reply";
        if (category == VeyraConfig.CATEGORY_GHOST_HIDE_CONTENTS) return "Hide Voice / Video Read Receipts";
        return "Ghost Mode Feature";
    }

    private boolean isMasterEnabled() {
        if (category == VeyraConfig.CATEGORY_GHOST_ONLINE) return VeyraConfig.ghostHideOnline;
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) return VeyraConfig.ghostHideTyping;
        if (category == VeyraConfig.CATEGORY_GHOST_UPLOAD) return VeyraConfig.ghostHideUpload;
        if (category == VeyraConfig.CATEGORY_GHOST_READ) return VeyraConfig.ghostHideRead;
        if (category == VeyraConfig.CATEGORY_GHOST_STORIES) return VeyraConfig.ghostHideStories;
        if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) return VeyraConfig.ghostHideChannelViews;
        if (category == VeyraConfig.CATEGORY_GHOST_SECRET_READ) return VeyraConfig.ghostHideSecretRead;
        if (category == VeyraConfig.CATEGORY_GHOST_READ_ON_REPLY) return VeyraConfig.ghostReadOnReply;
        if (category == VeyraConfig.CATEGORY_GHOST_HIDE_CONTENTS) return VeyraConfig.ghostHideReadContents;
        return false;
    }

    private void setMasterEnabled(boolean val) {
        if (category == VeyraConfig.CATEGORY_GHOST_ONLINE) VeyraConfig.setGhostHideOnline(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_TYPING) VeyraConfig.setGhostHideTyping(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_UPLOAD) VeyraConfig.setGhostHideUpload(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_READ) VeyraConfig.setGhostHideRead(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) VeyraConfig.setGhostHideStories(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) VeyraConfig.setGhostHideChannelViews(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_SECRET_READ) VeyraConfig.setGhostHideSecretRead(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_READ_ON_REPLY) VeyraConfig.setGhostReadOnReply(val);
        else if (category == VeyraConfig.CATEGORY_GHOST_HIDE_CONTENTS) VeyraConfig.setGhostHideReadContents(val);
    }

    private boolean isTypeEnabled(int peerType) {
        return VeyraConfig.isPeerEnabled(category, peerType);
    }

    private void setTypeEnabled(int peerType, boolean val) {
        VeyraConfig.setPeerEnabled(category, peerType, val);
    }

    private void openAddExceptionDialog() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("allowGlobalSearch", false);

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
        builder.setMessage("Are you sure you want to delete this exception?");
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
        builder.setTitle(LocaleController.getString("Delete", R.string.Delete));
        builder.setMessage("Are you sure you want to delete all exceptions for this feature?");
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
            if (map != null) {
                synchronized (map) {
                    map.clear();
                    VeyraConfig.saveExceptionsExplicit(category);
                }
            }
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
            return pos == masterToggleRow || pos == typePrivateRow || pos == typeGroupsRow || pos == typeChannelsRow
                    || pos == typeBotsRow || pos == typeExtra1Row || pos == typeExtra2Row || pos == addExceptionRow
                    || (pos >= exceptionsStartRow && pos < exceptionsEndRow) || pos == deleteAllExceptionsRow;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new HeaderCell(context, Theme.key_windowBackgroundWhiteBlueHeader, 21, 15, false, null);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                    view = new TextCheckCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new UserCell(context, 6, 0, false);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                default:
                    view = new TextInfoPrivacyCell(context);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == masterHeaderRow) {
                        headerCell.setText("Feature Control");
                    } else if (position == chatTypesHeaderRow) {
                        headerCell.setText(LocaleController.getString("VeyraChatTypes", R.string.VeyraChatTypes));
                    } else if (position == exceptionsHeaderRow) {
                        headerCell.setText(LocaleController.getString("NotificationsExceptions", R.string.NotificationsExceptions));
                    }
                    break;
                }
                case 1: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == masterToggleRow) {
                        checkCell.setTextAndCheck("Enable " + getTitleForCategory(), isMasterEnabled(), false);
                    } else if (position == typePrivateRow) {
                        String label;
                        if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
                            label = "Users & Contacts";
                        } else if (category == VeyraConfig.CATEGORY_GHOST_SECRET_READ) {
                            label = "Secret Chats";
                        } else {
                            label = LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate);
                        }
                        checkCell.setTextAndCheck(label, isTypeEnabled(VeyraConfig.PEER_PRIVATE), typeGroupsRow != -1 || typeChannelsRow != -1);
                    } else if (position == typeGroupsRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups), isTypeEnabled(VeyraConfig.PEER_GROUP), typeChannelsRow != -1 || typeBotsRow != -1);
                    } else if (position == typeChannelsRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels), isTypeEnabled(VeyraConfig.PEER_CHANNEL), typeBotsRow != -1 || typeExtra1Row != -1);
                    } else if (position == typeBotsRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots), isTypeEnabled(VeyraConfig.PEER_BOT), typeExtra1Row != -1);
                    } else if (position == typeExtra1Row) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraReadOnReply", R.string.VeyraReadOnReply), VeyraConfig.ghostReadOnReply, typeExtra2Row != -1);
                    } else if (position == typeExtra2Row) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraGhostHideReadContents", R.string.VeyraGhostHideReadContents), VeyraConfig.ghostHideReadContents, false);
                    }
                    break;
                }
                case 2: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == addExceptionRow) {
                        textCell.setTextAndIcon(LocaleController.getString("AddException", R.string.AddException), R.drawable.msg_add, false);
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    } else if (position == deleteAllExceptionsRow) {
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
                        userCell.setData(object, object == null ? (did > 0 ? "User " + did : "Chat " + did) : null, null, 0);
                    }
                    break;
                }
                case 4: {
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == masterInfoRow) {
                        infoCell.setText("Toggle stealth behavior for this specific action across Telegram.");
                    } else if (position == chatTypesInfoRow) {
                        if (category == VeyraConfig.CATEGORY_GHOST_TYPING || category == VeyraConfig.CATEGORY_GHOST_UPLOAD) {
                            infoCell.setText("Typing and upload indicators only apply to private chats and groups. Channels do not send user typing status.");
                        } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                            infoCell.setText("Choose which types of conversations will never receive read receipt acknowledgments.");
                        } else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
                            infoCell.setText("Choose whether to view stories anonymously for individual users or channels.");
                        } else {
                            infoCell.setText("Configure the scope and chat categories for this stealth rule.");
                        }
                    } else if (position == exceptionsInfoRow) {
                        infoCell.setText("Selected chats and contacts will bypass this stealth rule.");
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == masterHeaderRow || position == chatTypesHeaderRow || position == exceptionsHeaderRow) {
                return 0;
            } else if (position == masterToggleRow || position == typePrivateRow || position == typeGroupsRow
                    || position == typeChannelsRow || position == typeBotsRow || position == typeExtra1Row || position == typeExtra2Row) {
                return 1;
            } else if (position == addExceptionRow || position == deleteAllExceptionsRow) {
                return 2;
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                return 3;
            }
            return 4;
        }
    }
}
