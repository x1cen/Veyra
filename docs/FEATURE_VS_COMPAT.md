# Feature vs Compat — تفکیک فیچر از سازگاری

> نکته ای که اقا گفت: ویرا تغییرات زیادی خورده که فقط برای سازگار شدن با 13.0.2 هست، نه فیچر.
> این برنچ `veyra` فقط فیچر رو نگه میداره.

## چی فیچره (نگه میداریم)

| دسته | فایل ها | توضیح |
|---|---|---|
| هسته فیچر | `VeyraConfig.java` | همه toggle ها |
| فیچر ماژول ها | `org.veyra.client.*`, `org.veyra.helpers.*` | anti-delete / ghost / edit+reaction history / aether / proxy |
| تنظیمات | `Veyra*Activity.java` (18 فایل) | UI ستینگ ها |

## چی compat ـه (نگه نمیداریم — upstream خودش درست میکنه)

| دسته | نمونه | چرا compat ـه |
|---|---|---|
| LAYER / TL schema | `TL_ephemeral.java`, `TLRPC.java`, `LAYER` bump | تلگرام هر ورژن TL عوض میکنه؛ upstream خودش میاره |
| welcome/ephemeral wiring | `EphemeralMessagesHelper`, `ChatMessageCell welcome` | وابسته به LAYER بالا |
| boringssl vendoring | `jni/boringssl/**` (16k فایل) | تلگرام vendor کرد — upstream داره |
| CMake/JNI drift | `CMakeLists.txt`, `TgNetWrapper.cpp` | build drift — upstream داره |
| BuildVars hardcode | `BUILD_VERSION = 7175` hardcode | workflow خودش replace میکنه — نباید hardcode بمونه |

## قانون طلایی برای آپدیت

- **هر وقت** تلگرام آپدیت داد → فقط `./scripts/veyra-rebase.sh` بزن (یا بذار cron روزانه خودش بزنه)
- اگه کانفلیکت خورد → فقط فایل هایی که `VeyraConfig` توش هوک داره رو درست کن؛ boringssl و LAYER رو دست نزن — اونا از upstream میان
