package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
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
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GroupCreateUserCell;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class VeyraIncludedChatsActivity extends BaseFragment {

    private int category;
    private int peerType;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private EmptyTextProgressView emptyView;

    private final ArrayList<TLObject> allChats = new ArrayList<>();
    private final ArrayList<TLObject> searchResult = new ArrayList<>();
    private boolean searching = false;
    private String searchQuery = "";

    public VeyraIncludedChatsActivity(Bundle args) {
        super(args);
        if (args != null) {
            category = args.getInt("category", VeyraConfig.CATEGORY_ANTI_DELETE);
            peerType = args.getInt("peerType", VeyraConfig.PEER_GROUP);
        }
    }

    public VeyraIncludedChatsActivity(int category, int peerType) {
        this.category = category;
        this.peerType = peerType;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        loadChats();
        return true;
    }

    private void loadChats() {
        allChats.clear();
        HashSet<Long> seen = new HashSet<>();
        MessagesController mc = MessagesController.getInstance(currentAccount);

        ArrayList<TLRPC.Dialog> dialogs = mc.getAllDialogs();
        if (dialogs != null) {
            for (int a = 0; a < dialogs.size(); a++) {
                TLRPC.Dialog d = dialogs.get(a);
                if (d == null || seen.contains(d.id) || DialogObject.isEncryptedDialog(d.id)) {
                    continue;
                }
                TLObject obj = checkAndGetObject(d.id);
                if (obj != null) {
                    seen.add(d.id);
                    allChats.add(obj);
                }
            }
        }

        if (mc.dialogs_dict != null) {
            for (int a = 0; a < mc.dialogs_dict.size(); a++) {
                long did = mc.dialogs_dict.keyAt(a);
                if (seen.contains(did) || DialogObject.isEncryptedDialog(did)) {
                    continue;
                }
                TLObject obj = checkAndGetObject(did);
                if (obj != null) {
                    seen.add(did);
                    allChats.add(obj);
                }
            }
        }

        if (peerType == VeyraConfig.PEER_PRIVATE) {
            ArrayList<TLRPC.TL_contact> contacts = ContactsController.getInstance(currentAccount).contacts;
            if (contacts != null) {
                for (int a = 0; a < contacts.size(); a++) {
                    TLRPC.TL_contact c = contacts.get(a);
                    if (c == null || seen.contains((long) c.user_id)) {
                        continue;
                    }
                    TLRPC.User u = mc.getUser((long) c.user_id);
                    if (u != null && !u.bot && !UserObject.isUserSelf(u) && !UserObject.isDeleted(u)) {
                        seen.add((long) u.id);
                        allChats.add(u);
                    }
                }
            }
        }

        HashMap<Long, Boolean> exceptions = VeyraConfig.getExceptions(category);
        if (exceptions != null) {
            for (Long did : exceptions.keySet()) {
                if (did == null || seen.contains(did)) continue;
                TLObject obj = checkAndGetObject(did);
                if (obj != null) {
                    seen.add(did);
                    allChats.add(obj);
                }
            }
        }

        Collections.sort(allChats, (o1, o2) -> {
            String n1 = getObjectName(o1);
            String n2 = getObjectName(o2);
            return n1.compareToIgnoreCase(n2);
        });
    }

    private TLObject checkAndGetObject(long did) {
        MessagesController mc = MessagesController.getInstance(currentAccount);
        if (did > 0) {
            TLRPC.User user = mc.getUser(did);
            if (user == null || UserObject.isUserSelf(user) || UserObject.isDeleted(user)) {
                return null;
            }
            if (peerType == VeyraConfig.PEER_BOT) {
                return user.bot ? user : null;
            } else if (peerType == VeyraConfig.PEER_PRIVATE) {
                return !user.bot ? user : null;
            }
        } else if (did < 0) {
            TLRPC.Chat chat = mc.getChat(-did);
            if (chat == null || ChatObject.isNotInChat(chat)) {
                return null;
            }
            boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
            if (peerType == VeyraConfig.PEER_CHANNEL) {
                return isChannel ? chat : null;
            } else if (peerType == VeyraConfig.PEER_GROUP) {
                return !isChannel ? chat : null;
            }
        }
        return null;
    }

    public boolean isDefaultIncluded(long did) {
        if (peerType == VeyraConfig.PEER_GROUP) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
            if (chat == null) return true;
            boolean isPublic = ChatObject.isPublic(chat);
            boolean isPrivate = !isPublic;
            boolean isOwner = chat.creator;
            boolean isAdmin = ChatObject.hasAdminRights(chat);
            if (VeyraConfig.isOnlyPrivate(category, peerType) && !isPrivate) return false;
            if (VeyraConfig.isIgnoreOwner(category, peerType) && isOwner) return false;
            if (VeyraConfig.isIgnoreAdmin(category, peerType) && isAdmin) return false;
            return true;
        } else if (peerType == VeyraConfig.PEER_CHANNEL) {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
            if (chat == null) return true;
            boolean isPublic = ChatObject.isPublic(chat);
            boolean isPrivate = !isPublic;
            boolean isOwner = chat.creator;
            boolean isAdmin = ChatObject.hasAdminRights(chat);
            if (VeyraConfig.isOnlyPrivate(category, peerType) && !isPrivate) return false;
            if (VeyraConfig.isIgnoreOwner(category, peerType) && isOwner) return false;
            if (VeyraConfig.isIgnoreAdmin(category, peerType) && isAdmin) return false;
            return true;
        }
        return true;
    }

    public boolean isChatIncluded(long did) {
        Boolean override = VeyraConfig.getException(category, did);
        if (override != null) {
            return override;
        }
        return isDefaultIncluded(did);
    }

    private int getIncludedCount() {
        int count = 0;
        for (int a = 0; a < allChats.size(); a++) {
            long did = getDialogId(allChats.get(a));
            if (isChatIncluded(did)) {
                count++;
            }
        }
        return count;
    }

    private void updateSubtitle() {
        if (actionBar != null) {
            int active = getIncludedCount();
            int total = allChats.size();
            String format = LocaleController.getString("VeyraChatsActiveCount", R.string.VeyraChatsActiveCount);
            actionBar.setSubtitle(String.format(format, active, total));
        }
    }

    private String getObjectName(TLObject obj) {
        if (obj instanceof TLRPC.User) {
            return UserObject.getUserName((TLRPC.User) obj);
        } else if (obj instanceof TLRPC.Chat) {
            TLRPC.Chat c = (TLRPC.Chat) obj;
            return c.title != null ? c.title : "";
        }
        return "";
    }

    private String getObjectUsername(TLObject obj) {
        if (obj instanceof TLRPC.User) {
            TLRPC.User u = (TLRPC.User) obj;
            return u.username != null ? u.username : "";
        } else if (obj instanceof TLRPC.Chat) {
            TLRPC.Chat c = (TLRPC.Chat) obj;
            return c.username != null ? c.username : "";
        }
        return "";
    }

    private long getDialogId(TLObject obj) {
        if (obj instanceof TLRPC.User) {
            return ((TLRPC.User) obj).id;
        } else if (obj instanceof TLRPC.Chat) {
            return -((TLRPC.Chat) obj).id;
        }
        return 0;
    }

    private String getStatusText(TLObject obj, long did) {
        Boolean override = VeyraConfig.getException(category, did);
        String overrideSuffix = "";
        if (override != null) {
            overrideSuffix = override ? " • " + LocaleController.getString("VeyraExplicitlyIncluded", R.string.VeyraExplicitlyIncluded)
                                      : " • " + LocaleController.getString("VeyraExplicitlyExcluded", R.string.VeyraExplicitlyExcluded);
        }

        if (obj instanceof TLRPC.Chat) {
            TLRPC.Chat chat = (TLRPC.Chat) obj;
            boolean isChannel = ChatObject.isChannel(chat) && !chat.megagroup;
            boolean isPublic = ChatObject.isPublic(chat);
            String typeStr = isChannel ? (isPublic ? "Public Channel" : "Private Channel")
                                       : (isPublic ? "Public Group" : "Private Group");
            return typeStr + overrideSuffix;
        } else if (obj instanceof TLRPC.User) {
            TLRPC.User user = (TLRPC.User) obj;
            if (user.bot) {
                return LocaleController.getString("Bot", R.string.Bot) + overrideSuffix;
            } else {
                String userStatus = LocaleController.formatUserStatus(currentAccount, user);
                return userStatus + overrideSuffix;
            }
        }
        return overrideSuffix;
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
        updateSubtitle();

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 2) {
                    showResetAlert();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(1, R.drawable.outline_header_search).setIsSearchField(true, false);
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchQuery = "";
                searchResult.clear();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                processSearch(editText.getText().toString());
            }
        });
        searchItem.setSearchFieldHint(LocaleController.getString("Search", R.string.Search));

        ActionBarMenuItem resetItem = menu.addItem(2, R.drawable.msg_retry);
        resetItem.setContentDescription(LocaleController.getString("Reset", R.string.Reset));

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        emptyView = new EmptyTextProgressView(context);
        emptyView.showTextView();
        emptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(true);
        listView.setFastScrollEnabled(RecyclerListView.FastScroll.LETTER_TYPE);
        listView.setEmptyView(emptyView);

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position) -> {
            ArrayList<TLObject> list = searching ? searchResult : allChats;
            if (position < 0 || position >= list.size()) return;
            TLObject obj = list.get(position);
            long did = getDialogId(obj);

            boolean currentIncluded = isChatIncluded(did);
            boolean newIncluded = !currentIncluded;
            boolean defaultMatch = isDefaultIncluded(did);

            if (newIncluded == defaultMatch) {
                VeyraConfig.removeException(category, did);
            } else {
                VeyraConfig.addException(category, did, newIncluded);
            }

            if (view instanceof GroupCreateUserCell) {
                ((GroupCreateUserCell) view).setChecked(newIncluded, true);
                String status = getStatusText(obj, did);
                ((GroupCreateUserCell) view).setObject(obj, null, status, false);
            }
            updateSubtitle();
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void processSearch(String query) {
        searchQuery = query == null ? "" : query.trim().toLowerCase();
        if (TextUtils.isEmpty(searchQuery)) {
            searching = false;
            searchResult.clear();
        } else {
            searching = true;
            searchResult.clear();
            for (TLObject obj : allChats) {
                String name = getObjectName(obj).toLowerCase();
                String username = getObjectUsername(obj).toLowerCase();
                if (name.contains(searchQuery) || username.contains(searchQuery)) {
                    searchResult.add(obj);
                }
            }
        }
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void showResetAlert() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("VeyraResetAlertTitle", R.string.VeyraResetAlertTitle));
        builder.setMessage(LocaleController.getString("VeyraResetAlertDesc", R.string.VeyraResetAlertDesc));
        builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialog, which) -> {
            VeyraConfig.clearExceptions(category, peerType);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            updateSubtitle();
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
            return searching ? searchResult.size() : allChats.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            GroupCreateUserCell cell = new GroupCreateUserCell(mContext, 1, 0, false);
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ArrayList<TLObject> list = searching ? searchResult : allChats;
            if (position < 0 || position >= list.size()) return;
            TLObject obj = list.get(position);
            GroupCreateUserCell cell = (GroupCreateUserCell) holder.itemView;

            long did = getDialogId(obj);
            boolean isIncluded = isChatIncluded(did);
            String status = getStatusText(obj, did);

            cell.setObject(obj, null, status, false);
            cell.setChecked(isIncluded, false);
        }
    }
}
