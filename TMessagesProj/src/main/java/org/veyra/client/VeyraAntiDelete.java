/* Veyra Project */
package org.veyra.client;

import android.content.Context;
import android.graphics.Typeface;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.telegram.messenger.*;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class VeyraAntiDelete {

    public static ArrayList<TLRPC.TL_dcOption> getTheDC(int dcId, ArrayList<TLRPC.TL_dcOption> options) {
        ArrayList<TLRPC.TL_dcOption> filtered = new ArrayList<>();
        for (TLRPC.TL_dcOption t : options) {
            if (t.id == dcId) filtered.add(t);
        }
        if (filtered.isEmpty()) return null;
        return filtered;
    }

    public static String getDCGeoDummy(int dcId) {
        switch (dcId) {
            case 1:
            case 3:
                return "USA, Miami";
            case 2:
            case 4:
                return "NLD, Amsterdam";
            case 5:
                return "SGP, Singapore";
            default:
                return "UNK, unknown";
        }
    }

    public static float getVideoRoundMult(int id) {
        switch (id) {
            case 0:
                return 0.5f;
            case 2:
                return 2f;
            case 3:
                return 4f;
            default:
                return 1f;
        }
    }

    public static int getVideoRoundSize(int id) {
        switch (id) {
            case 0:
                return 192;
            case 2:
                return 768;
            default:
                return 384;
        }
    }

    public static int getPhotoSizeMax(int id) {
        switch (id) {
            case 0:
                return 1280;
            case 1:
                return 2560;
            case 2:
                return 3840;
            default:
                return 1280;
        }
    }

    public static Typeface getFont(String font) {
        switch (font) {
            case "fonts/rmedium.ttf"://bold
            case "fonts/ritalic.ttf"://italic
            case "fonts/rmediumitalic.ttf"://bold italic
            case "fonts/rmono.ttf": //mono
            case "fonts/mw_bold.ttf"://normal bold
            case "fonts/rcondensedbold.ttf"://QR
                return AndroidUtilities.getTypeface(font);
            default:
                return Typeface.createFromFile(font);
        }
    }

//    public static int getNotificationIcon() {
//        switch (MessagesController.getGlobalAGramSettings().getInt("UIAppNotificationIconSelector", 0)) {
//            case 1:
//                return R.drawable.agram_eyez;
//            case 2:
//                return R.drawable.notification;
//            case 3:
//                return R.drawable.msg_report_xxx;
//            case 0:
//            default:
//                return R.drawable.agram_notification;
//        }
//    }

    public static String toJson(Object o) {
        return new GsonBuilder().disableHtmlEscaping().create().toJson(o);
    }

    public static String toJsonNestedMaps(Map<Integer, Map<String, String>> map) {
        final Gson gson = new Gson();
        final JsonObject jsonObject = new JsonObject();
        for (Integer i : map.keySet()) {
            jsonObject.add(i.toString(), gson.toJsonTree(map.get(i), Map.class));
        }
        return jsonObject.toString();
    }

//    public static int getMaxInternalAccountId(Map<Integer, Map<String, String>> map) {//SharedConfig.thAccounts
//        Integer[] ids;
//        Integer nextId = 0;
//        if (map == null || map.isEmpty()) {
//            if (SharedConfig.activeAccounts != null && !SharedConfig.activeAccounts.isEmpty())
//                ids = SharedConfig.activeAccounts.stream().toArray(Integer[]::new);
//            else return 0;
//        } else {
//            if (map.containsKey(-1)) nextId = Integer.valueOf(map.get(-1).get("nextAccountId"));
//            ids = map.keySet().stream().toArray(Integer[]::new);
//        }
//        Arrays.sort(ids);
//        if (map.containsKey(-1) && nextId.compareTo(ids[ids.length - 1]) <= 0) {
//            nextId = ids[ids.length - 1] + 1;
//            map.get(-1).put("nextAccountId", nextId.toString());
//            ApplicationLoader.applicationContext.getSharedPreferences("agram", Context.MODE_PRIVATE).edit()
//                .putString("thAccounts", VeyraAntiDelete.toJsonNestedMaps(map))
//                .apply();
//        }
//        return nextId;
//    }

//    public static boolean starrMark(boolean bool) {
//        switch (MessagesController.getGlobalAGramSettings().getInt("AGramStarrMark", 0)) {
//            case 0:
//                return false;
//            case 1:
//                return true;
//            default:
//                return bool;
//        }
//    }

//    public static float stickerSizeMult() {
//        switch (MessagesController.getGlobalAGramSettings().getInt("UIStickerSize", 2)) {
//            case 0:
//                return 4f;
//            case 1:
//                return 2f;
//            case 3:
//                return 0.5f;
//            case 2:
//            default:
//                return 1f;
//        }
//    }
}
