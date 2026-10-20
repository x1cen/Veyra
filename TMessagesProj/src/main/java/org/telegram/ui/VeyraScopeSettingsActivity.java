package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.VeyraConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;

public class VeyraScopeSettingsActivity extends BaseFragment {

    private int category;
    private int peerType;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int optionsHeaderRow = -1;
    private int onlyPrivateRow = -1;
    private int ignoreOwnerRow = -1;
    private int ignoreAdminRow = -1;
    private int onlyPrivateInfoRow = -1;

    private int inclusionHeaderRow = -1;
    private int includedChatsRow = -1;
    private int resetRow = -1;

    private int customizedHeaderRow = -1;
    private int customizedStartRow = -1;
    private int customizedEndRow = -1;
    private int clearAllCustomizedRow = -1;

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

    @Override
    public void onResume() {
        super.onResume();
        updateRows();
    }

    private void updateRows() {
        rowCount = 0;
        optionsHeaderRow = -1;
        onlyPrivateRow = -1;
        ignoreOwnerRow = -1;
        ignoreAdminRow = -1;
        onlyPrivateInfoRow = -1;

        inclusionHeaderRow = -1;
        includedChatsRow = -1;
        resetRow = -1;

        customizedHeaderRow = -1;
        customizedStartRow = -1;
        customizedEndRow = -1;
        clearAllCustomizedRow = -1;
        lastDividerRow = -1;

        if (peerType == VeyraConfig.PEER_GROUP || peerType == VeyraConfig.PEER_CHANNEL) {
            optionsHeaderRow = rowCount++;
            onlyPrivateRow = rowCount++;
            ignoreOwnerRow = rowCount++;
            ignoreAdminRow = rowCount++;
            onlyPrivateInfoRow = rowCount++;
        }

        inclusionHeaderRow = rowCount++;
        includedChatsRow = rowCount++;
        resetRow = rowCount++;

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
            customizedHeaderRow = rowCount++;
            customizedStartRow = rowCount;
            rowCount += exceptionsList.size();
            customizedEndRow = rowCount;
            clearAllCustomizedRow = rowCount++;
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
            title = LocaleController.getString("VeyraChatTypePrivate", R.string.VeyraChatTypePrivate);
        } else if (peerType == VeyraConfig.PEER_GROUP) {
            title = LocaleController.getString("VeyraChatTypeGroups", R.string.VeyraChatTypeGroups);
        } else if (peerType == VeyraConfig.PEER_CHANNEL) {
            title = LocaleController.getString("VeyraChatTypeChannels", R.string.VeyraChatTypeChannels);
        } else {
            title = LocaleController.getString("VeyraChatTypeBots", R.string.VeyraChatTypeBots);
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
        listView.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), AndroidUtilities.dp(16));
        listView.setClipToPadding(false);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position) -> {
            if (position == onlyPrivateRow) {
                boolean val = !VeyraConfig.isOnlyPrivate(category, peerType);
                VeyraConfig.setOnlyPrivate(category, peerType, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == ignoreOwnerRow) {
                boolean val = !VeyraConfig.isIgnoreOwner(category, peerType);
                VeyraConfig.setIgnoreOwner(category, peerType, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == ignoreAdminRow) {
                boolean val = !VeyraConfig.isIgnoreAdmin(category, peerType);
                VeyraConfig.setIgnoreAdmin(category, peerType, val);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(val);
                }
            } else if (position == includedChatsRow) {
                presentFragment(new VeyraIncludedChatsActivity(category, peerType));
            } else if (position == resetRow || position == clearAllCustomizedRow) {
                showResetAlert();
            } else if (position >= customizedStartRow && position < customizedEndRow) {
                int index = position - customizedStartRow;
                if (index >= 0 && index < exceptionsList.size()) {
                    long did = exceptionsList.get(index);
                    showRemoveCustomizationAlert(did);
                }
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void showRemoveCustomizationAlert(long dialogId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraRemoveCustomizationTitle", R.string.VeyraRemoveCustomizationTitle));
        builder.setMessage(LocaleController.getString("VeyraRemoveCustomizationDesc", R.string.VeyraRemoveCustomizationDesc));
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

    private void showResetAlert() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraResetAlertTitle", R.string.VeyraResetAlertTitle));
        builder.setMessage(LocaleController.getString("VeyraResetAlertDesc", R.string.VeyraResetAlertDesc));
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
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
            return pos == onlyPrivateRow || pos == ignoreOwnerRow || pos == ignoreAdminRow
                    || pos == includedChatsRow || pos == resetRow
                    || (pos >= customizedStartRow && pos < customizedEndRow)
                    || pos == clearAllCustomizedRow;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextCheckCell(mContext);
                    break;
                case 1:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 2:
                    HeaderCell headerCell = new HeaderCell(mContext);
                    headerCell.setBackgroundColor(0);
                    view = headerCell;
                    break;
                case 3:
                    view = new TextCell(mContext);
                    break;
                case 4:
                    view = new UserCell(mContext, 4, 0, false, false);
                    break;
                case 5:
                default:
                    view = new View(mContext);
                    view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(12)));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int topRad = 0;
            int bottomRad = 0;
            boolean isCardItem = false;

            if (position == onlyPrivateRow) {
                topRad = 14;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == ignoreOwnerRow) {
                topRad = 0;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == ignoreAdminRow) {
                topRad = 0;
                bottomRad = 14;
                isCardItem = true;
            } else if (position == includedChatsRow) {
                topRad = 14;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == resetRow) {
                topRad = 0;
                bottomRad = 14;
                isCardItem = true;
            } else if (position >= customizedStartRow && position < customizedEndRow) {
                topRad = (position == customizedStartRow) ? 14 : 0;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == clearAllCustomizedRow) {
                topRad = 0;
                bottomRad = 14;
                isCardItem = true;
            }

            if (isCardItem) {
                int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
                int selColor = Theme.getColor(Theme.key_listSelector);
                holder.itemView.setBackground(Theme.createRadSelectorDrawable(bgColor, selColor, topRad, bottomRad));
            }

            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == onlyPrivateRow) {
                        if (peerType == VeyraConfig.PEER_GROUP) {
                            checkCell.setTextAndCheck(LocaleController.getString("VeyraOnlyPrivateGroups", R.string.VeyraOnlyPrivateGroups), VeyraConfig.isOnlyPrivate(category, peerType), false);
                        } else if (peerType == VeyraConfig.PEER_CHANNEL) {
                            checkCell.setTextAndCheck(LocaleController.getString("VeyraOnlyPrivateChannels", R.string.VeyraOnlyPrivateChannels), VeyraConfig.isOnlyPrivate(category, peerType), false);
                        }
                    } else if (position == ignoreOwnerRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraIgnoreOwners", R.string.VeyraIgnoreOwners), VeyraConfig.isIgnoreOwner(category, peerType), false);
                    } else if (position == ignoreAdminRow) {
                        checkCell.setTextAndCheck(LocaleController.getString("VeyraIgnoreAdmins", R.string.VeyraIgnoreAdmins), VeyraConfig.isIgnoreAdmin(category, peerType), false);
                    }
                    break;
                }
                case 1: {
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    infoCell.setText(LocaleController.getString("VeyraScopePolicyDesc", R.string.VeyraScopePolicyDesc));
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == optionsHeaderRow) {
                        headerCell.setText(LocaleController.getString("VeyraScopePolicy", R.string.VeyraScopePolicy));
                    } else if (position == inclusionHeaderRow) {
                        headerCell.setText(LocaleController.getString("VeyraIncludedChatsTitle", R.string.VeyraIncludedChatsTitle));
                    } else if (position == customizedHeaderRow) {
                        headerCell.setText(LocaleController.getString("VeyraManualCustomizations", R.string.VeyraManualCustomizations));
                    }
                    break;
                }
                case 3: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == includedChatsRow) {
                        textCell.setTextAndIcon(LocaleController.getString("VeyraIncludedChats", R.string.VeyraIncludedChats), R.drawable.msg_select, false);
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    } else if (position == resetRow) {
                        textCell.setTextAndIcon(LocaleController.getString("VeyraResetToDefault", R.string.VeyraResetToDefault), R.drawable.msg_retry, false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    } else if (position == clearAllCustomizedRow) {
                        textCell.setText(LocaleController.getString("NotificationsDeleteAllException", R.string.NotificationsDeleteAllException), false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                }
                case 4: {
                    UserCell userCell = (UserCell) holder.itemView;
                    int index = position - customizedStartRow;
                    if (index >= 0 && index < exceptionsList.size()) {
                        long did = exceptionsList.get(index);
                        TLObject object;
                        if (DialogObject.isUserDialog(did)) {
                            object = MessagesController.getInstance(currentAccount).getUser(did);
                        } else {
                            object = MessagesController.getInstance(currentAccount).getChat(-did);
                        }
                        Boolean excVal = VeyraConfig.getException(category, did);
                        String statusStr = (excVal != null && excVal)
                                ? LocaleController.getString("VeyraExplicitlyIncluded", R.string.VeyraExplicitlyIncluded)
                                : LocaleController.getString("VeyraExplicitlyExcluded", R.string.VeyraExplicitlyExcluded);
                        userCell.setData(object, null, statusStr, 0);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == onlyPrivateRow || position == ignoreOwnerRow || position == ignoreAdminRow) {
                return 0;
            } else if (position == onlyPrivateInfoRow) {
                return 1;
            } else if (position == optionsHeaderRow || position == inclusionHeaderRow || position == customizedHeaderRow) {
                return 2;
            } else if (position == includedChatsRow || position == resetRow || position == clearAllCustomizedRow) {
                return 3;
            } else if (position >= customizedStartRow && position < customizedEndRow) {
                return 4;
            }
            return 5;
        }
    }
}
