package com.example.icons

import android.content.pm.ApplicationInfo
import android.os.Build
import java.util.Locale

enum class AppCategory(val displayName: String) {
  DIALER("Phone & Dialer"),
  SMS_MESSAGING("SMS & Messaging"),
  CONTACTS("Contacts"),
  CALENDAR("Calendar & Events"),
  CLOCK("Clock & Alarms"),
  CALCULATOR("Calculator"),
  BROWSER("Web Browser"),
  CAMERA("Camera"),
  GALLERY("Photos & Gallery"),
  FILES("Files & Storage"),
  MUSIC_AUDIO("Music & Audio"),
  VIDEO_MEDIA("Video & Streaming"),
  EMAIL("Email"),
  NOTES_TASKS("Notes & Tasks"),
  MAPS_TRAVEL("Maps & Travel"),
  SHOPPING("Shopping & Deals"),
  FOOD_DELIVERY("Food & Delivery"),
  APP_STORE("App Store"),
  CLEANER_SECURITY("Cleaner & Security"),
  SETTINGS_SYSTEM("Settings & System"),
  WALLET_FINANCE("Wallet & Finance"),
  KEYBOARD("Keyboard & IME"),
  DEVELOPER_ROOT("Root, Shell & DB"),
  GAMES("Games"),
  SOCIAL_CHAT("Social & Chat"),
  PRODUCTIVITY_OFFICE("Docs & Office"),
  READING_NEWS("Books & News"),
  AI_ASSISTANT("AI & Assistant"),
  HEALTH_FITNESS("Health & Fitness"),
  WEATHER("Weather & Storm"),
  LAUNCHER("Launcher"),
  OTHER("Utility"),
}

data class IconMatchResult(
  val category: AppCategory,
  val matchedTokens: List<String>,
  val matchedByRule: String,
)

/**
 * Intelligent keyword, package-token, and Android category matcher used for both
 * the built-in Orbit Fusion icon pack and external third-party Android icon packs.
 */
object IconPackMatcher {

  private data class KeywordRule(
    val category: AppCategory,
    val keywords: Set<String>,
    val ruleLabel: String,
  )

  private val orderedRules: List<KeywordRule> = listOf(
    KeywordRule(
      category = AppCategory.DIALER,
      keywords = setOf("dialer", "phone", "call", "caller", "truecaller", "drupe", "eyecon", "voip", "telecom"),
      ruleLabel = "keyword:dialer_phone",
    ),
    KeywordRule(
      category = AppCategory.SMS_MESSAGING,
      keywords = setOf("sms", "mms", "clearsms", "nextsms", "qksms", "chompsms", "quik", "textra", "smsmessenger", "securesms"),
      ruleLabel = "keyword:sms_messaging",
    ),
    KeywordRule(
      category = AppCategory.CONTACTS,
      keywords = setOf("contacts", "contact", "addressbook", "people", "phonebook"),
      ruleLabel = "keyword:contacts",
    ),
    KeywordRule(
      category = AppCategory.CALENDAR,
      keywords = setOf("calendar", "schedule", "agenda", "timetree", "holidays", "event"),
      ruleLabel = "keyword:calendar",
    ),
    KeywordRule(
      category = AppCategory.CLOCK,
      keywords = setOf("clock", "deskclock", "clockpackage", "alarm", "alarmclock", "timer", "stopwatch"),
      ruleLabel = "keyword:clock_alarm",
    ),
    KeywordRule(
      category = AppCategory.CALCULATOR,
      keywords = setOf("calculator", "calc"),
      ruleLabel = "keyword:calculator",
    ),
    KeywordRule(
      category = AppCategory.BROWSER,
      keywords = setOf(
        "browser", "chrome", "brave", "firefox", "fenix", "focus", "klar",
        "sbrowser", "emmx", "edge", "opera", "vivaldi", "duckduckgo",
        "torbrowser", "kiwi", "kiwibrowser", "bromite", "cromite", "vanadium",
        "fennec", "mulch", "waterfox", "iceraven", "ironfox", "privacybrowser",
        "yandex", "phoenix", "via", "soulbrowser", "alohamobile", "globalbrowser",
        "ecosia", "qwant", "puffin", "tunnybrowser", "hermitcrab", "ucmobile",
        "ucturbo", "jelly", "webview", "surf",
      ),
      ruleLabel = "keyword:browser",
    ),
    KeywordRule(
      category = AppCategory.CAMERA,
      keywords = setOf("camera", "opencamera", "gcam", "shutter"),
      ruleLabel = "keyword:camera",
    ),
    KeywordRule(
      category = AppCategory.GALLERY,
      keywords = setOf("gallery", "gallery3d", "photos", "photosgo", "album", "collage"),
      ruleLabel = "keyword:gallery_photos",
    ),
    KeywordRule(
      category = AppCategory.DEVELOPER_ROOT,
      keywords = setOf("magisk", "root", "rootexplorer", "luckypatcher", "patcher", "terminal", "determinal", "shell", "sqlite", "databasql", "adb"),
      ruleLabel = "keyword:root_dev",
    ),
    KeywordRule(
      category = AppCategory.FILES,
      keywords = setOf("files", "documentsui", "explorer", "explored", "filemanager", "archive", "unzip"),
      ruleLabel = "keyword:files",
    ),
    KeywordRule(
      category = AppCategory.EMAIL,
      keywords = setOf("gmail", "gm", "outlook", "mail", "email", "inbox"),
      ruleLabel = "keyword:email",
    ),
    KeywordRule(
      category = AppCategory.WALLET_FINANCE,
      keywords = setOf("wallet", "paisa", "pay", "mpesa", "bank", "finance", "cash", "billing"),
      ruleLabel = "keyword:wallet_finance",
    ),
    KeywordRule(
      category = AppCategory.APP_STORE,
      keywords = setOf("vending", "playstore", "fdroid", "aptoide", "apkpure", "aurora", "uptodown", "palmstore", "mipicks", "appmarket", "samsungapps", "getapp", "venezia"),
      ruleLabel = "keyword:app_store",
    ),
    KeywordRule(
      category = AppCategory.CLEANER_SECURITY,
      keywords = setOf("cleaner", "avg", "phonemaster", "phonemanager", "secscanner", "scanner", "antivirus", "applock", "authenticator", "accubattery", "battery", "adm"),
      ruleLabel = "keyword:cleaner_security",
    ),
    KeywordRule(
      category = AppCategory.FOOD_DELIVERY,
      keywords = setOf("glovo", "eats", "ubereats", "delivery", "food", "bolt", "doordash"),
      ruleLabel = "keyword:food_delivery",
    ),
    KeywordRule(
      category = AppCategory.SHOPPING,
      keywords = setOf("shop", "shopping", "jumia", "kilimall", "carrefour", "naivas", "aliexpress", "alibaba", "amazon", "ebay", "temu", "shein", "jiji", "oraimo", "zzkko", "mshop"),
      ruleLabel = "keyword:shopping",
    ),
    KeywordRule(
      category = AppCategory.MAPS_TRAVEL,
      keywords = setOf("maps", "mapslite", "earth", "osmand", "navigation", "gps", "travel", "onthego", "transit", "flight"),
      ruleLabel = "keyword:maps_travel",
    ),
    KeywordRule(
      category = AppCategory.KEYBOARD,
      keywords = setOf("keyboard", "ime", "numpad", "t9numpad", "inputmethod", "gboard"),
      ruleLabel = "keyword:keyboard",
    ),
    KeywordRule(
      category = AppCategory.MUSIC_AUDIO,
      keywords = setOf("music", "fmradio", "radio", "podcast", "podcasts", "soundrecorder", "recorder", "audio", "spotify", "deezer"),
      ruleLabel = "keyword:music_audio",
    ),
    KeywordRule(
      category = AppCategory.VIDEO_MEDIA,
      keywords = setOf("youtube", "videos", "video", "player", "googlyplayer", "aiomedia", "cinema", "netflix", "vlc"),
      ruleLabel = "keyword:video_media",
    ),
    KeywordRule(
      category = AppCategory.NOTES_TASKS,
      keywords = setOf("keep", "notes", "note", "memo", "todos", "todo", "tasks", "task", "anydo", "checklist"),
      ruleLabel = "keyword:notes_tasks",
    ),
    KeywordRule(
      category = AppCategory.PRODUCTIVITY_OFFICE,
      keywords = setOf("docs", "sheets", "slides", "office", "skydrive", "onedrive", "drive", "translate", "classroom", "editors"),
      ruleLabel = "keyword:productivity_office",
    ),
    KeywordRule(
      category = AppCategory.READING_NEWS,
      keywords = setOf("books", "magazines", "news", "manualguide", "reader", "epub", "pdf"),
      ruleLabel = "keyword:books_news",
    ),
    KeywordRule(
      category = AppCategory.AI_ASSISTANT,
      keywords = setOf("bard", "gemini", "aibox", "googleassistant", "assistant", "copilot"),
      ruleLabel = "keyword:ai_assistant",
    ),
    KeywordRule(
      category = AppCategory.HEALTH_FITNESS,
      keywords = setOf("fitness", "health", "healthdata", "workout", "steps", "pulse"),
      ruleLabel = "keyword:health_fitness",
    ),
    KeywordRule(
      category = AppCategory.WEATHER,
      keywords = setOf("weather", "tornado", "storm", "forecast", "climate"),
      ruleLabel = "keyword:weather",
    ),
    KeywordRule(
      category = AppCategory.GAMES,
      keywords = setOf("game", "games", "taptap", "pubg", "tencent", "arcade", "puzzle"),
      ruleLabel = "keyword:games",
    ),
    KeywordRule(
      category = AppCategory.LAUNCHER,
      keywords = setOf("orbitlauncher", "orbitaicon", "launcher", "homescreen"),
      ruleLabel = "keyword:launcher",
    ),
    KeywordRule(
      category = AppCategory.SOCIAL_CHAT,
      keywords = setOf("whatsapp", "telegram", "discord", "facebook", "orca", "mlite", "instagram", "snapchat", "tiktok", "musically", "aweme", "twitter", "reddit", "pinterest", "linkedin", "tumblr", "vkontakte", "viber", "skype", "imo", "imoim", "kakao", "line", "wechat", "friends", "bitstrips", "imoji", "dynamite", "meetings", "tachyon", "chat"),
      ruleLabel = "keyword:social_chat",
    ),
    KeywordRule(
      category = AppCategory.SETTINGS_SYSTEM,
      keywords = setOf("settings", "stk", "gms", "chromecast", "hoffnung", "system", "toolkit", "config"),
      ruleLabel = "keyword:settings_system",
    ),
  )

  fun tokenize(packageName: String, label: String): List<String> {
    val rawParts = ArrayList<String>()
    val combined = "$packageName $label"
    for (segment in combined.split(Regex("[^A-Za-z0-9]+"))) {
      if (segment.isBlank()) continue
      rawParts.add(segment.lowercase(Locale.ROOT))
      // Split camelCase / PascalCase / uppercase runs like "ClearSMS", "AccuBattery", "RightDialer"
      val camel = segment.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
      for (part in camel.split(' ')) {
        val lower = part.lowercase(Locale.ROOT)
        if (lower.isNotBlank() && lower !in rawParts) {
          rawParts.add(lower)
        }
      }
    }
    return rawParts
  }

  fun classify(
    packageName: String,
    label: String,
    androidCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
  ): IconMatchResult {
    val pkgLower = packageName.lowercase(Locale.ROOT)
    FusionIconRegistry.findSpec(pkgLower)?.let { spec ->
      return IconMatchResult(
        category = spec.category,
        matchedTokens = listOf(pkgLower),
        matchedByRule = "exact_package",
      )
    }

    val tokens = tokenize(packageName, label)
    val tokenSet = tokens.toSet()

    // Check explicit keyword matches first, allowing substring match on compound words (e.g. "mydialer" contains "dialer")
    for (rule in orderedRules) {
      val exactHit = tokens.firstOrNull { it in rule.keywords }
      if (exactHit != null) {
        return IconMatchResult(
          category = rule.category,
          matchedTokens = listOf(exactHit),
          matchedByRule = rule.ruleLabel,
        )
      }
      val substringHit = tokens.firstOrNull { token ->
        token.length >= 4 && rule.keywords.any { kw -> kw.length >= 4 && token.contains(kw) }
      }
      if (substringHit != null) {
        return IconMatchResult(
          category = rule.category,
          matchedTokens = listOf(substringHit),
          matchedByRule = rule.ruleLabel,
        )
      }
    }

    // Fallback to Android OS ApplicationInfo.category
    val byOsCategory = mapAndroidCategory(androidCategory)
    if (byOsCategory != AppCategory.OTHER) {
      return IconMatchResult(
        category = byOsCategory,
        matchedTokens = tokens.take(3),
        matchedByRule = "android_category:$androidCategory",
      )
    }

    // Generic heuristic on "messages" / "messaging"
    if (tokenSet.any { it.contains("messag") || it.contains("chat") }) {
      return IconMatchResult(
        category = AppCategory.SMS_MESSAGING,
        matchedTokens = listOf("messaging"),
        matchedByRule = "keyword:fallback_messaging",
      )
    }

    return IconMatchResult(
      category = AppCategory.OTHER,
      matchedTokens = tokens.take(3),
      matchedByRule = "default",
    )
  }

  fun mapAndroidCategory(androidCategory: Int): AppCategory {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return AppCategory.OTHER
    return when (androidCategory) {
      ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
      ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC_AUDIO
      ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO_MEDIA
      ApplicationInfo.CATEGORY_IMAGE -> AppCategory.GALLERY
      ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL_CHAT
      ApplicationInfo.CATEGORY_NEWS -> AppCategory.READING_NEWS
      ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS_TRAVEL
      ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY_OFFICE
      else -> AppCategory.OTHER
    }
  }

  /**
   * Canonical package identifiers per category used when matching unmatched apps
   * against third-party icon packs' `appfilter.xml`.
   */
  fun canonicalPackagesForCategory(category: AppCategory): List<String> = when (category) {
    AppCategory.DIALER -> listOf(
      "com.google.android.dialer",
      "com.android.dialer",
      "com.samsung.android.dialer",
      "org.fossify.phone",
    )
    AppCategory.SMS_MESSAGING -> listOf(
      "com.google.android.apps.messaging",
      "com.android.mms",
      "com.samsung.android.messaging",
      "org.fossify.messages",
    )
    AppCategory.CONTACTS -> listOf(
      "com.google.android.contacts",
      "com.android.contacts",
      "com.samsung.android.app.contacts",
      "org.fossify.contacts",
    )
    AppCategory.CALENDAR -> listOf(
      "com.google.android.calendar",
      "com.android.calendar",
      "com.samsung.android.calendar",
      "org.fossify.calendar",
    )
    AppCategory.CLOCK -> listOf(
      "com.google.android.deskclock",
      "com.android.deskclock",
      "com.sec.android.app.clockpackage",
      "org.fossify.clock",
    )
    AppCategory.CALCULATOR -> listOf(
      "com.google.android.calculator",
      "com.android.calculator2",
      "com.sec.android.app.popupcalculator",
    )
    AppCategory.BROWSER -> listOf(
      "org.mozilla.firefox",
      "com.sec.android.app.sbrowser",
      "com.microsoft.emmx",
      "com.opera.browser",
      "com.brave.browser",
      "com.duckduckgo.mobile.android",
      "com.vivaldi.browser",
      "com.android.chrome",
      "com.android.browser",
    )
    AppCategory.CAMERA -> listOf(
      "com.google.android.GoogleCamera",
      "com.android.camera2",
      "com.android.camera",
      "com.sec.android.app.camera",
    )
    AppCategory.GALLERY -> listOf(
      "com.google.android.apps.photos",
      "com.android.gallery3d",
      "com.sec.android.gallery3d",
    )
    AppCategory.FILES -> listOf(
      "com.google.android.apps.nbu.files",
      "com.android.documentsui",
      "com.google.android.documentsui",
      "com.sec.android.app.myfiles",
    )
    AppCategory.MUSIC_AUDIO -> listOf(
      "com.google.android.apps.youtube.music",
      "com.google.android.music",
      "com.spotify.music",
    )
    AppCategory.VIDEO_MEDIA -> listOf(
      "com.google.android.youtube",
      "com.google.android.videos",
      "org.videolan.vlc",
    )
    AppCategory.EMAIL -> listOf(
      "com.google.android.gm",
      "com.microsoft.office.outlook",
      "com.android.email",
    )
    AppCategory.NOTES_TASKS -> listOf(
      "com.google.android.keep",
      "com.google.android.apps.tasks",
      "com.anydo",
    )
    AppCategory.MAPS_TRAVEL -> listOf(
      "com.google.android.apps.maps",
      "com.google.earth",
      "net.osmand",
    )
    AppCategory.SHOPPING -> listOf(
      "com.amazon.mShop.android.shopping",
      "com.ebay.mobile",
      "com.alibaba.aliexpresshd",
    )
    AppCategory.FOOD_DELIVERY -> listOf(
      "com.ubercab.eats",
      "com.glovo",
    )
    AppCategory.APP_STORE -> listOf(
      "com.android.vending",
      "org.fdroid.fdroid",
      "com.aurora.store",
    )
    AppCategory.CLEANER_SECURITY -> listOf(
      "com.google.android.apps.authenticator2",
      "com.avg.cleaner",
      "com.google.android.apps.adm",
    )
    AppCategory.SETTINGS_SYSTEM -> listOf(
      "com.android.settings",
      "com.android.stk",
    )
    AppCategory.WALLET_FINANCE -> listOf(
      "com.google.android.apps.walletnfcrel",
      "com.google.android.apps.nbu.paisa.user",
    )
    AppCategory.KEYBOARD -> listOf(
      "com.google.android.inputmethod.latin",
    )
    AppCategory.DEVELOPER_ROOT -> listOf(
      "com.topjohnwu.magisk",
      "com.speedsoftware.rootexplorer",
    )
    AppCategory.GAMES -> listOf(
      "com.google.android.play.games",
    )
    AppCategory.SOCIAL_CHAT -> listOf(
      "com.whatsapp",
      "org.telegram.messenger",
      "com.facebook.katana",
      "com.instagram.android",
    )
    AppCategory.PRODUCTIVITY_OFFICE -> listOf(
      "com.google.android.apps.docs",
      "com.google.android.apps.docs.editors.docs",
    )
    AppCategory.READING_NEWS -> listOf(
      "com.google.android.apps.books",
      "com.google.android.apps.magazines",
    )
    AppCategory.AI_ASSISTANT -> listOf(
      "com.google.android.apps.bard",
      "com.google.android.apps.googleassistant",
    )
    AppCategory.HEALTH_FITNESS -> listOf(
      "com.google.android.apps.fitness",
      "com.google.android.apps.healthdata",
    )
    AppCategory.WEATHER -> listOf(
      "com.google.android.apps.weather",
    )
    AppCategory.LAUNCHER -> listOf(
      "com.thallyace.orbitlauncher",
    )
    AppCategory.OTHER -> emptyList()
  }

  /**
   * Drawable resource name keywords searched inside an external icon pack
   * when no `appfilter.xml` entry directly matched.
   */
  fun drawableKeywordsForCategory(category: AppCategory): List<String> = when (category) {
    AppCategory.DIALER -> listOf("dialer", "phone", "com_android_dialer", "com_google_android_dialer", "ic_phone")
    AppCategory.SMS_MESSAGING -> listOf("messages", "messaging", "sms", "mms", "com_android_mms")
    AppCategory.CONTACTS -> listOf("contacts", "people", "com_android_contacts")
    AppCategory.CALENDAR -> listOf("calendar", "com_android_calendar", "google_calendar")
    AppCategory.CLOCK -> listOf("clock", "deskclock", "alarm", "com_android_deskclock")
    AppCategory.CALCULATOR -> listOf("calculator", "calc", "com_android_calculator2")
    AppCategory.BROWSER -> listOf(
      "browser", "firefox", "samsung_internet", "edge", "opera", "brave",
      "duckduckgo", "vivaldi", "internet", "web", "chrome",
    )
    AppCategory.CAMERA -> listOf("camera", "com_android_camera")
    AppCategory.GALLERY -> listOf("gallery", "photos", "google_photos")
    AppCategory.FILES -> listOf("files", "file_manager", "documents", "explorer")
    AppCategory.MUSIC_AUDIO -> listOf("music", "audio", "radio", "youtube_music")
    AppCategory.VIDEO_MEDIA -> listOf("video", "youtube", "vlc", "player")
    AppCategory.EMAIL -> listOf("gmail", "mail", "email", "outlook")
    AppCategory.NOTES_TASKS -> listOf("keep", "notes", "tasks", "todo")
    AppCategory.MAPS_TRAVEL -> listOf("maps", "navigation", "google_maps")
    AppCategory.SHOPPING -> listOf("shopping", "amazon", "store", "shop")
    AppCategory.FOOD_DELIVERY -> listOf("ubereats", "food", "delivery")
    AppCategory.APP_STORE -> listOf("play_store", "playstore", "vending", "fdroid", "store")
    AppCategory.CLEANER_SECURITY -> listOf("security", "cleaner", "antivirus", "authenticator")
    AppCategory.SETTINGS_SYSTEM -> listOf("settings", "com_android_settings")
    AppCategory.WALLET_FINANCE -> listOf("wallet", "google_wallet", "pay")
    AppCategory.KEYBOARD -> listOf("keyboard", "gboard")
    AppCategory.DEVELOPER_ROOT -> listOf("terminal", "magisk", "root_explorer", "root")
    AppCategory.GAMES -> listOf("play_games", "games", "gamepad")
    AppCategory.SOCIAL_CHAT -> listOf("chat", "whatsapp", "telegram")
    AppCategory.PRODUCTIVITY_OFFICE -> listOf("drive", "docs", "office")
    AppCategory.READING_NEWS -> listOf("books", "news")
    AppCategory.AI_ASSISTANT -> listOf("gemini", "assistant")
    AppCategory.HEALTH_FITNESS -> listOf("fit", "health", "fitness")
    AppCategory.WEATHER -> listOf("weather", "storm")
    AppCategory.LAUNCHER -> listOf("launcher", "home")
    AppCategory.OTHER -> emptyList()
  }
}
