package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class VeyraSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;

    // Privacy section
    private int privacyHeaderRow;
    private int onlineModeRow;
    private int readOnReplyRow;
    private int antiDeleteRow;
    private int ghostModeRow;
    private int hideTypingRow;
    private int blockSecretChatRow;
    private int privacySectionRow;

    // Controls section
    private int controlsHeaderRow;
    private int confirmCallRow;
    private int confirmLinkRow;
    private int cleanUrlsRow;
    private int disableUndoRow;
    private int disableLinkPreviewRow;
    private int disableVibrationRow;
    private int controlsSectionRow;

    // UI section
    private int uiHeaderRow;
    private int persianCalendarRow;
    private int showProfileIdRow;
    private int bypassRestrictionsRow;
    private int noAdsRow;
    private int unlimitedLimitsRow;
    private int uiSectionRow;

    // About section
    private int aboutHeaderRow;
    private int versionRow;
    private int githubRow;
    private int aboutSectionRow;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;

        privacyHeaderRow = rowCount++;
        onlineModeRow = rowCount++;
        readOnReplyRow = rowCount++;
        antiDeleteRow = rowCount++;
        ghostModeRow = rowCount++;
        hideTypingRow = rowCount++;
        blockSecretChatRow = rowCount++;
        privacySectionRow = rowCount++;

        controlsHeaderRow = rowCount++;
        confirmCallRow = rowCount++;
        confirmLinkRow = rowCount++;
        cleanUrlsRow = rowCount++;
        disableUndoRow = rowCount++;
        disableLinkPreviewRow = rowCount++;
        disableVibrationRow = rowCount++;
        controlsSectionRow = rowCount++;

        uiHeaderRow = rowCount++;
        persianCalendarRow = rowCount++;
        showProfileIdRow = rowCount++;
        bypassRestrictionsRow = rowCount++;
        noAdsRow = rowCount++;
        unlimitedLimitsRow = rowCount++;
        uiSectionRow = rowCount++;

        aboutHeaderRow = rowCount++;
        versionRow = rowCount++;
        githubRow = rowCount++;
        aboutSectionRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("VeyraSettings", R.string.VeyraSettings));
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

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
            if (position == onlineModeRow) {
                String[] options = isFarsi
                        ? new String[]{"نمایش آنلاین (پیش‌فرض)", "مخفی کردن آنلاین (همیشه آفلاین)", "همیشه آنلاین نشان بده"}
                        : new String[]{"Show Online (Default)", "Hide Online (Always Offline)", "Always Show Online"};
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                builder.setTitle(isFarsi ? "وضعیت آنلاین" : "Online Status");
                builder.setItems(options, (dialog, which) -> {
                    VeyraConfig.setOnlineMode(which);
                    MessagesController.getInstance(currentAccount).updateOnlineStatus();
                    listAdapter.notifyItemChanged(position);
                });
                builder.show();
            } else if (position == readOnReplyRow) {
                VeyraConfig.setReadOnReply(!VeyraConfig.readOnReply);
                ((TextCheckCell) view).setChecked(VeyraConfig.readOnReply);
            } else if (position == antiDeleteRow) {
                VeyraConfig.setAntiDelete(!VeyraConfig.antiDelete);
                ((TextCheckCell) view).setChecked(VeyraConfig.antiDelete);
            } else if (position == ghostModeRow) {
                VeyraConfig.setGhostMode(!VeyraConfig.ghostMode);
                ((TextCheckCell) view).setChecked(VeyraConfig.ghostMode);
            } else if (position == hideTypingRow) {
                VeyraConfig.setHideTyping(!VeyraConfig.hideTyping);
                ((TextCheckCell) view).setChecked(VeyraConfig.hideTyping);
            } else if (position == blockSecretChatRow) {
                VeyraConfig.setBlockSecretChat(!VeyraConfig.blockSecretChat);
                ((TextCheckCell) view).setChecked(VeyraConfig.blockSecretChat);
            } else if (position == confirmCallRow) {
                VeyraConfig.setConfirmCall(!VeyraConfig.confirmCall);
                ((TextCheckCell) view).setChecked(VeyraConfig.confirmCall);
            } else if (position == confirmLinkRow) {
                VeyraConfig.setConfirmLink(!VeyraConfig.confirmLink);
                ((TextCheckCell) view).setChecked(VeyraConfig.confirmLink);
            } else if (position == cleanUrlsRow) {
                VeyraConfig.setCleanUrls(!VeyraConfig.cleanUrls);
                ((TextCheckCell) view).setChecked(VeyraConfig.cleanUrls);
            } else if (position == disableUndoRow) {
                VeyraConfig.setDisableUndo(!VeyraConfig.disableUndo);
                ((TextCheckCell) view).setChecked(VeyraConfig.disableUndo);
            } else if (position == disableLinkPreviewRow) {
                VeyraConfig.setDisableLinkPreviewByDefault(!VeyraConfig.disableLinkPreviewByDefault);
                ((TextCheckCell) view).setChecked(VeyraConfig.disableLinkPreviewByDefault);
            } else if (position == disableVibrationRow) {
                VeyraConfig.setDisableVibration(!VeyraConfig.disableVibration);
                ((TextCheckCell) view).setChecked(VeyraConfig.disableVibration);
            } else if (position == persianCalendarRow) {
                VeyraConfig.setPersianCalendar(!VeyraConfig.persianCalendar);
                ((TextCheckCell) view).setChecked(VeyraConfig.persianCalendar);
            } else if (position == showProfileIdRow) {
                VeyraConfig.setShowProfileId(!VeyraConfig.showProfileId);
                ((TextCheckCell) view).setChecked(VeyraConfig.showProfileId);
            } else if (position == bypassRestrictionsRow) {
                VeyraConfig.setIgnoreContentRestrictions(!VeyraConfig.ignoreContentRestrictions);
                ((TextCheckCell) view).setChecked(VeyraConfig.ignoreContentRestrictions);
            } else if (position == githubRow) {
                Browser.openUrl(getParentActivity(), "https://github.com/x1cen/Veyra");
            } else if (position == noAdsRow || position == unlimitedLimitsRow || position == versionRow) {
                BulletinFactory.of(VeyraSettingsActivity.this).createSimpleBulletin(
                        R.drawable.msg_info,
                        isFarsi ? "این قابلیت به طور پیش‌فرض فعال و بهینه‌سازی شده است" : "This feature is active and permanently optimized"
                ).show();
            }
        });

        return fragmentView;
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
            int position = holder.getAdapterPosition();
            return position != privacyHeaderRow && position != privacySectionRow &&
                    position != controlsHeaderRow && position != controlsSectionRow &&
                    position != uiHeaderRow && position != uiSectionRow &&
                    position != aboutHeaderRow && position != aboutSectionRow &&
                    position != noAdsRow && position != unlimitedLimitsRow;
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
            boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
            switch (holder.getItemViewType()) {
                case 1: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == privacyHeaderRow) {
                        headerCell.setText(isFarsi ? "امنیت و حریم خصوصی" : "Privacy & Security");
                    } else if (position == controlsHeaderRow) {
                        headerCell.setText(isFarsi ? "کنترل‌ها و تعاملات" : "Controls & Interaction");
                    } else if (position == uiHeaderRow) {
                        headerCell.setText(isFarsi ? "رابط کاربری و ویژگی‌ها" : "UI & Features");
                    } else if (position == aboutHeaderRow) {
                        headerCell.setText(isFarsi ? "درباره ویرا" : "About Veyra");
                    }
                    break;
                }
                case 2: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == readOnReplyRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "سین فقط با پاسخ دادن" : "Mark as Read on Reply",
                                isFarsi ? "پیام‌های دریافتی تا ارسال پاسخ برای مخاطب خوانده نمی‌شوند" : "Messages are not marked as read until you reply",
                                VeyraConfig.readOnReply, true, true
                        );
                    } else if (position == antiDeleteRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "آنتی‌دیلیت پیام‌ها" : "Anti-Delete Messages",
                                isFarsi ? "نگه‌داری پیام‌های حذف‌شده با برچسب قرمز 'حذف شده'" : "Keep deleted messages with 'Deleted' label",
                                VeyraConfig.antiDelete, true, true
                        );
                    } else if (position == ghostModeRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "حالت روح (Ghost Mode)" : "Full Ghost Mode",
                                isFarsi ? "عدم ارسال وضعیت خوانده شدن برای هیچ پیامی" : "Never send read receipts",
                                VeyraConfig.ghostMode, true, true
                        );
                    } else if (position == hideTypingRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "مخفی‌سازی وضعیت نوشتن" : "Hide Typing Status",
                                isFarsi ? "جلوگیری از ارسال وضعیت 'در حال نوشتن...' به دیگران" : "Don't broadcast 'typing...' status",
                                VeyraConfig.hideTyping, true, true
                        );
                    } else if (position == blockSecretChatRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "مسدودسازی سکرت چت ورودی" : "Block Incoming Secret Chats",
                                isFarsi ? "رد خودکار درخواست‌های سکرت چت از دیگران" : "Auto-decline incoming secret chat requests",
                                VeyraConfig.blockSecretChat, true, false
                        );
                    } else if (position == confirmCallRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "تأییدیه قبل از تماس" : "Confirm Before Calling",
                                isFarsi ? "نمایش هشدار تأیید قبل از شروع تماس صوتی یا تصویری" : "Show confirmation dialog before voice/video calls",
                                VeyraConfig.confirmCall, true, true
                        );
                    } else if (position == confirmLinkRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "تأییدیه باز کردن لینک‌ها" : "Confirm External Links",
                                isFarsi ? "نیاز به تأیید کاربر قبل از باز شدن لینک در مرورگر" : "Require confirmation before opening links",
                                VeyraConfig.confirmLink, true, true
                        );
                    } else if (position == cleanUrlsRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "پاک‌سازی ترکر از لینک‌ها" : "Clean Tracking Parameters",
                                isFarsi ? "حذف خودکار utm_*, fbclid, gclid, si هنگام کپی لینک" : "Auto-strip utm_*, fbclid, gclid, si from copied URLs",
                                VeyraConfig.cleanUrls, true, true
                        );
                    } else if (position == disableUndoRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "حذف تأخیر ۵ ثانیه‌ای لغو عملیات" : "Skip 5s Undo Countdown",
                                isFarsi ? "اجرای فوری عملیات بدون نوار انتظار ۵ ثانیه" : "Execute actions instantly without undo toast",
                                VeyraConfig.disableUndo, true, true
                        );
                    } else if (position == disableLinkPreviewRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "غیرفعال‌سازی پیش‌نمایش لینک به‌طور پیش‌فرض" : "Disable Link Preview by Default",
                                isFarsi ? "جلوگیری از ارسال درخواست پیش‌نمایش به سرور هنگام تایپ لینک" : "Prevent server from fetching link previews while typing",
                                VeyraConfig.disableLinkPreviewByDefault, true, true
                        );
                    } else if (position == disableVibrationRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "غیرفعال کردن ویبره" : "Disable Vibration",
                                isFarsi ? "قطع تمام فیدبک‌های لرزشی و هپتیک اپلیکیشن" : "Disable all haptic feedback and vibrations globally",
                                VeyraConfig.disableVibration, true, false
                        );
                    } else if (position == persianCalendarRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "تقویم خورشیدی (شمسی)" : "Persian Solar Calendar",
                                isFarsi ? "نمایش تاریخ‌ها با ماه‌های خورشیدی و ارقام فارسی" : "Display dates in Persian Solar Hijri calendar",
                                VeyraConfig.persianCalendar, true, true
                        );
                    } else if (position == showProfileIdRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "نمایش شناسه تلگرام و DC در پروفایل" : "Show Telegram ID & Datacenter",
                                isFarsi ? "دسترسی و کپی سریع User ID و دیتاسنتر در پروفایل کاربران" : "Show and copy User ID and DC in user profiles",
                                VeyraConfig.showProfileId, true, true
                        );
                    } else if (position == bypassRestrictionsRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "نادیده گرفتن محدودیت‌های محتوایی اندروید" : "Bypass Android Content Restrictions",
                                isFarsi ? "نمایش کانال‌ها و محتوای فیلترشده مخصوص اندروید" : "View channels and content restricted only on Android",
                                VeyraConfig.ignoreContentRestrictions, true, false
                        );
                    }
                    break;
                }
                case 3: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == githubRow) {
                        textCell.setText(isFarsi ? "مخزن گیت‌هاب (x1cen/Veyra)" : "GitHub Repository (x1cen/Veyra)", false);
                    }
                    break;
                }
                case 4: {
                    TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                    if (position == onlineModeRow) {
                        boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
                        String[] modeNames = isFarsi
                                ? new String[]{"نمایش آنلاین (پیش‌فرض)", "مخفی کردن آنلاین", "همیشه آنلاین"}
                                : new String[]{"Show Online (Default)", "Hide Online", "Always Online"};
                        String current = modeNames[Math.min(VeyraConfig.onlineMode, 2)];
                        detailCell.setTextAndValue(
                                isFarsi ? "وضعیت آنلاین" : "Online Status",
                                current,
                                true
                        );
                    } else if (position == noAdsRow) {
                        detailCell.setTextAndValue(
                                isFarsi ? "بدون تبلیغات اسپانسری" : "Ad-Free Experience",
                                isFarsi ? "تمام پست‌های اسپانسری بدون نیاز به پرمیوم حذف شده‌اند" : "All sponsored ads permanently disabled",
                                true
                        );
                    } else if (position == unlimitedLimitsRow) {
                        detailCell.setTextAndValue(
                                isFarsi ? "سقف‌های ارتقاء یافته" : "Unlocked Limits",
                                isFarsi ? "۱۰۰ پین، ۵۰۰ استیکر دلخواه، ۱۰۰۰ گیف، ۳۰ فولدر" : "100 pins, 500 stickers, 1000 GIFs, 30 folders",
                                false
                        );
                    } else if (position == versionRow) {
                        detailCell.setTextAndValue(
                                isFarsi ? "نسخه Veyra" : "Veyra Version",
                                "1.0.0 (arm64-v8a)",
                                true
                        );
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == privacyHeaderRow || position == controlsHeaderRow ||
                    position == uiHeaderRow || position == aboutHeaderRow) {
                return 1;
            } else if (position == readOnReplyRow || position == antiDeleteRow ||
                    position == ghostModeRow || position == hideTypingRow ||
                    position == blockSecretChatRow || position == confirmCallRow ||
                    position == confirmLinkRow || position == cleanUrlsRow ||
                    position == disableUndoRow || position == disableLinkPreviewRow ||
                    position == disableVibrationRow || position == persianCalendarRow ||
                    position == showProfileIdRow || position == bypassRestrictionsRow) {
                return 2;
            } else if (position == githubRow) {
                return 3;
            } else if (position == onlineModeRow || position == noAdsRow || position == unlimitedLimitsRow || position == versionRow) {
                return 4;
            } else {
                return 0;
            }
        }
    }
}
