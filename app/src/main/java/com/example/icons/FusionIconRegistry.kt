package com.example.icons

import androidx.annotation.DrawableRes
import com.example.R
import java.util.Locale

data class FusionIconSpec(
  @param:DrawableRes val drawableRes: Int,
  val category: AppCategory,
  val gradientStart: Int,
  val gradientMid: Int,
  val gradientEnd: Int,
  val haloGlow: Int = 0x55FFFFFF,
  val badgeText: String? = null,
)

data class OrbitIconChoice(
  val key: String,
  val label: String,
  val category: AppCategory,
  val spec: FusionIconSpec,
)

/**
 * Researched Apple x Android Fusion Material gradient icon specifications for every requested
 * package ID, plus intelligent category fallbacks for any unmatched installed app.
 */
object FusionIconRegistry {

  private fun spec(
    @DrawableRes drawableRes: Int,
    category: AppCategory,
    start: Long,
    mid: Long,
    end: Long,
    halo: Long = 0x48FFFFFF,
    badge: String? = null,
  ): FusionIconSpec = FusionIconSpec(
    drawableRes = drawableRes,
    category = category,
    gradientStart = start.toInt(),
    gradientMid = mid.toInt(),
    gradientEnd = end.toInt(),
    haloGlow = halo.toInt(),
    badgeText = badge,
  )

  val PACKAGE_SPECS: Map<String, FusionIconSpec> = mapOf(
    // SMS & Messaging
    "app.clearsms" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF10B981, 0xFF06B6D4, 0xFF0284C7),
    "com.android.mms" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF38BDF8, 0xFF2563EB, 0xFF1D4ED8),
    "com.google.android.apps.messaging" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF22D3EE, 0xFF0EA5E9, 0xFF2563EB),
    "com.handcent.app.nextsms" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF0EA5E9, 0xFF14B8A6, 0xFF0F766E),
    "com.moez.qksms" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF14B8A6, 0xFF10B981, 0xFF059669),
    "com.p1.chompsms" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF22C55E, 0xFF16A34A, 0xFF15803D),
    "com.samsung.android.messaging" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF38BDF8, 0xFF0EA5E9, 0xFF0369A1),
    "com.simplemobiletools.smsmessenger" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.textra" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF0EA5E9, 0xFF06B6D4, 0xFF10B981),
    "dev.octoshrimpy.quik" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF06B6D4, 0xFF0D9488, 0xFF115E59),
    "org.fossify.messages" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF34D399, 0xFF10B981, 0xFF047857),
    "xyz.klinker.messenger" to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF6366F1, 0xFF4F46E5, 0xFF0EA5E9),

    // Shopping, Marketplaces & Delivery
    "app.jumia.android" to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFFFFB020, 0xFFF97316, 0xFFEA580C),
    "com.alibaba.intl.android.apps.poseidon" to spec(R.drawable.ic_fusion_commerce_globe, AppCategory.SHOPPING, 0xFFFF7A00, 0xFFF97316, 0xFFC2410C),
    "com.alibaba.aliexpresshd" to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFFFF3D00, 0xFFEF4444, 0xFFEA580C),
    "com.amazon.mshop.android.shopping" to spec(R.drawable.ic_fusion_parcel_shop, AppCategory.SHOPPING, 0xFF1E293B, 0xFF0F172A, 0xFF1E3A5F, 0x44FF9900),
    "com.carrefour.kenya" to spec(R.drawable.ic_fusion_grocery, AppCategory.SHOPPING, 0xFF2563EB, 0xFF1D4ED8, 0xFFEF4444),
    "com.ebay.mobile" to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFFEF4444, 0xFF3B82F6, 0xFFF59E0B),
    "com.einnovation.temu" to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFFFF6B00, 0xFFF97316, 0xFFFB7185),
    "com.glovo" to spec(R.drawable.ic_fusion_delivery, AppCategory.FOOD_DELIVERY, 0xFFFACC15, 0xFFF59E0B, 0xFF10B981),
    "com.jiji.app" to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFF22C55E, 0xFF16A34A, 0xFF15803D),
    "com.kilimall.shopping" to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFFF43F5E, 0xFFEF4444, 0xFFF97316),
    "com.maf.carrefour" to spec(R.drawable.ic_fusion_grocery, AppCategory.SHOPPING, 0xFF1D4ED8, 0xFF4F46E5, 0xFFE11D48),
    "com.naivas.app" to spec(R.drawable.ic_fusion_grocery, AppCategory.SHOPPING, 0xFFF97316, 0xFF16A34A, 0xFF15803D),
    "com.oraimo.store" to spec(R.drawable.ic_fusion_audio_store, AppCategory.SHOPPING, 0xFF84CC16, 0xFF22C55E, 0xFF059669),
    "com.shein.android" to spec(R.drawable.ic_fusion_fashion, AppCategory.SHOPPING, 0xFF1E1B4B, 0xFF4C1D95, 0xFFEC4899),
    "com.ubercab.eats" to spec(R.drawable.ic_fusion_delivery, AppCategory.FOOD_DELIVERY, 0xFF1E293B, 0xFF065F46, 0xFF10B981),
    "com.zzkko" to spec(R.drawable.ic_fusion_fashion, AppCategory.SHOPPING, 0xFF18181B, 0xFF312E81, 0xFFDB2777),

    // App Stores & Package Sources
    "app.source.getapp" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFF38BDF8, 0xFF3B82F6, 0xFF6366F1),
    "com.amazon.venezia" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFFF59E0B, 0xFF0D9488, 0xFF0F766E),
    "com.android.vending" to spec(R.drawable.ic_fusion_playstore, AppCategory.APP_STORE, 0xFF0EA5E9, 0xFF2563EB, 0xFF4338CA),
    "com.apkpure.aegon" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFF34D399, 0xFF10B981, 0xFF06B6D4),
    "com.aptoide.partners" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFFFF8A00, 0xFFF97316, 0xFFEA580C),
    "com.aurora.store" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFF22D3EE, 0xFF6366F1, 0xFF7C3AED),
    "com.fdroid.fdroid" to spec(R.drawable.ic_fusion_fdroid, AppCategory.APP_STORE, 0xFF1976D2, 0xFF0EA5E9, 0xFF8BC34A),
    "com.huawei.appmarket" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFFEF4444, 0xFFDC2626, 0xFFBE123C),
    "com.sec.android.app.samsungapps" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFFC026D3, 0xFF7C3AED, 0xFF4F46E5),
    "com.transsion.palmstore" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFFF97316, 0xFF14B8A6, 0xFF0D9488),
    "com.uptodown" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8),
    "com.xiaomi.mipicks" to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFFFF9800, 0xFFF97316, 0xFFEA580C),
    "org.fdroid.fdroid" to spec(R.drawable.ic_fusion_fdroid, AppCategory.APP_STORE, 0xFF2563EB, 0xFF0EA5E9, 0xFF10B981),

    // Cleaners, Battery & Security
    "com.aimatch.cleaner" to spec(R.drawable.ic_fusion_cleaner, AppCategory.CLEANER_SECURITY, 0xFF22D3EE, 0xFF06B6D4, 0xFF10B981),
    "com.avg.cleaner" to spec(R.drawable.ic_fusion_cleaner, AppCategory.CLEANER_SECURITY, 0xFF22C55E, 0xFF10B981, 0xFF06B6D4),
    "com.clover.secscanner" to spec(R.drawable.ic_fusion_security_scan, AppCategory.CLEANER_SECURITY, 0xFF10B981, 0xFF0D9488, 0xFF4F46E5),
    "com.digibites.accubattery" to spec(R.drawable.ic_fusion_battery, AppCategory.CLEANER_SECURITY, 0xFF34D399, 0xFF10B981, 0xFF0284C7),
    "com.google.android.apps.adm" to spec(R.drawable.ic_fusion_find_device, AppCategory.CLEANER_SECURITY, 0xFF34D399, 0xFF10B981, 0xFF0F766E),
    "com.google.android.apps.authenticator2" to spec(R.drawable.ic_fusion_authenticator, AppCategory.CLEANER_SECURITY, 0xFF334155, 0xFF312E81, 0xFF4F46E5),
    "com.transsion.applock" to spec(R.drawable.ic_fusion_lock, AppCategory.CLEANER_SECURITY, 0xFF6366F1, 0xFF4338CA, 0xFF0284C7),
    "com.transsion.phonemanager" to spec(R.drawable.ic_fusion_security_scan, AppCategory.CLEANER_SECURITY, 0xFF10B981, 0xFF06B6D4, 0xFF2563EB),
    "com.transsion.phonemaster" to spec(R.drawable.ic_fusion_cleaner, AppCategory.CLEANER_SECURITY, 0xFF06B6D4, 0xFF0EA5E9, 0xFF2563EB),

    // Clocks & Alarms
    "com.alarmclock.xtreme" to spec(R.drawable.ic_fusion_alarm, AppCategory.CLOCK, 0xFFF43F5E, 0xFFEA580C, 0xFFF59E0B),
    "com.alarmclock.xtreme.free" to spec(R.drawable.ic_fusion_alarm, AppCategory.CLOCK, 0xFFFB7185, 0xFFF97316, 0xFFFBBF24),
    "com.android.deskclock" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF38BDF8, 0xFF2563EB, 0xFF4F46E5),
    "com.google.android.deskclock" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF60A5FA, 0xFF2563EB, 0xFF4338CA),
    "com.samsung.android.app.clockpackage" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF818CF8, 0xFF4F46E5, 0xFF0EA5E9),
    "com.sec.android.app.clockpackage" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF6366F1, 0xFF4F46E5, 0xFF06B6D4),
    "com.simplemobiletools.clock" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.transsion.deskclock" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF0EA5E9, 0xFF3B82F6, 0xFF4F46E5),
    "org.fossify.clock" to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF34D399, 0xFF10B981, 0xFF0D9488),

    // Calendars
    "com.android.calendar" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFF38BDF8, 0xFF2563EB, 0xFF1D4ED8),
    "com.calendar.schedule.event" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFF8B5CF6, 0xFF6366F1, 0xFF0EA5E9),
    "com.deventz.calendar.ken.g01" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFFDC2626, 0xFF16A34A, 0xFF15803D),
    "com.google.android.calendar" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFF3B82F6, 0xFF2563EB, 0xFF0EA5E9),
    "com.samsung.android.calendar" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFFF43F5E, 0xFFEC4899, 0xFF2563EB),
    "com.simplemobiletools.calendar" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.simplemobiletools.calendar.pro" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C, badge = "PRO"),
    "com.timetreeapp.schedule" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFF34D399, 0xFF10B981, 0xFF059669),
    "org.fossify.calendar" to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFF10B981, 0xFF14B8A6, 0xFFF59E0B),

    // Web Browsers (Chrome + All Popular Non-Chrome Browsers)
    "app.vanadium.browser" to spec(R.drawable.ic_fusion_browser_shield, AppCategory.BROWSER, 0xFF0EA5E9, 0xFF2563EB, 0xFF1E3A8A),
    "com.alohamobile.browser" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF38BDF8, 0xFF0284C7, 0xFF0369A1),
    "com.alohamobile.browser.lite" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF22D3EE, 0xFF0EA5E9, 0xFF2563EB, badge = "LITE"),
    "com.android.browser" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF38BDF8, 0xFF2563EB, 0xFF4F46E5),
    "com.android.chrome" to spec(R.drawable.ic_fusion_chrome, AppCategory.BROWSER, 0xFF38BDF8, 0xFF2563EB, 0xFF1E3A8A),
    "com.brave.browser" to spec(R.drawable.ic_fusion_brave, AppCategory.BROWSER, 0xFFFF5500, 0xFFF97316, 0xFFDB2777),
    "com.brave.browser_beta" to spec(R.drawable.ic_fusion_brave, AppCategory.BROWSER, 0xFFFF6B00, 0xFFEA580C, 0xFFE11D48, badge = "BETA"),
    "com.brave.browser_nightly" to spec(R.drawable.ic_fusion_brave, AppCategory.BROWSER, 0xFF4F46E5, 0xFF9333EA, 0xFFF97316, badge = "DEV"),
    "com.chimbori.hermitcrab" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFF14B8A6, 0xFF0EA5E9, 0xFF2563EB),
    "com.chrome.beta" to spec(R.drawable.ic_fusion_chrome, AppCategory.BROWSER, 0xFF22D3EE, 0xFF2563EB, 0xFF312E81, badge = "BETA"),
    "com.chrome.canary" to spec(R.drawable.ic_fusion_chrome, AppCategory.BROWSER, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C, badge = "CNRY"),
    "com.chrome.dev" to spec(R.drawable.ic_fusion_chrome, AppCategory.BROWSER, 0xFF4F46E5, 0xFF1D4ED8, 0xFF06B6D4, badge = "DEV"),
    "com.cloudmosa.puffinfree" to spec(R.drawable.ic_fusion_phoenix, AppCategory.BROWSER, 0xFF34D399, 0xFF0EA5E9, 0xFF2563EB),
    "com.duckduckgo.mobile.android" to spec(R.drawable.ic_fusion_duckduckgo, AppCategory.BROWSER, 0xFFFF5722, 0xFFDE5833, 0xFFC2410C),
    "com.ecosia.android" to spec(R.drawable.ic_fusion_ecosia, AppCategory.BROWSER, 0xFF4ADE80, 0xFF16A34A, 0xFF0EA5E9),
    "com.kiwibrowser.browser" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF22D3EE, 0xFF0EA5E9, 0xFF0284C7),
    "com.mi.globalbrowser" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF10B981, 0xFF06B6D4, 0xFF2563EB),
    "com.mi.globalbrowser.mini" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF34D399, 0xFF0EA5E9, 0xFF0284C7, badge = "LITE"),
    "com.microsoft.emmx" to spec(R.drawable.ic_fusion_edge, AppCategory.BROWSER, 0xFF34D399, 0xFF0EA5E9, 0xFF1D4ED8),
    "com.microsoft.emmx.beta" to spec(R.drawable.ic_fusion_edge, AppCategory.BROWSER, 0xFF22D3EE, 0xFF0284C7, 0xFF4338CA, badge = "BETA"),
    "com.microsoft.emmx.canary" to spec(R.drawable.ic_fusion_edge, AppCategory.BROWSER, 0xFFFBBF24, 0xFF10B981, 0xFF0EA5E9, badge = "CNRY"),
    "com.microsoft.emmx.dev" to spec(R.drawable.ic_fusion_edge, AppCategory.BROWSER, 0xFF6366F1, 0xFF0EA5E9, 0xFF0D9488, badge = "DEV"),
    "com.mx.browser" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF3B82F6, 0xFF2563EB, 0xFF1D4ED8),
    "com.mycompany.app.soulbrowser" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFF8B5CF6, 0xFF6366F1, 0xFF0EA5E9),
    "com.oh.bro" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFF38BDF8, 0xFF0EA5E9, 0xFF2563EB),
    "com.opera.browser" to spec(R.drawable.ic_fusion_opera, AppCategory.BROWSER, 0xFFFF1B2D, 0xFFEF4444, 0xFFB91C1C),
    "com.opera.gx" to spec(R.drawable.ic_fusion_opera, AppCategory.BROWSER, 0xFFFF004D, 0xFFDB2777, 0xFF7C3AED, badge = "GX"),
    "com.opera.mini.native" to spec(R.drawable.ic_fusion_opera, AppCategory.BROWSER, 0xFFFB7185, 0xFFFF1B2D, 0xFFEA580C, badge = "MINI"),
    "com.opera.touch" to spec(R.drawable.ic_fusion_opera, AppCategory.BROWSER, 0xFFF43F5E, 0xFFEC4899, 0xFF9333EA),
    "com.qwant.liberty" to spec(R.drawable.ic_fusion_search, AppCategory.BROWSER, 0xFF6366F1, 0xFF3B82F6, 0xFFEC4899),
    "com.sec.android.app.sbrowser" to spec(R.drawable.ic_fusion_samsung_browser, AppCategory.BROWSER, 0xFF8B5CF6, 0xFF6366F1, 0xFF2563EB),
    "com.sec.android.app.sbrowser.beta" to spec(R.drawable.ic_fusion_samsung_browser, AppCategory.BROWSER, 0xFFA855F7, 0xFF6366F1, 0xFF0EA5E9, badge = "BETA"),
    "com.stoutner.privacybrowser.standard" to spec(R.drawable.ic_fusion_browser_shield, AppCategory.BROWSER, 0xFF1E293B, 0xFF2563EB, 0xFF0284C7),
    "com.tencent.ibibo.mtt" to spec(R.drawable.ic_fusion_phoenix, AppCategory.BROWSER, 0xFF38BDF8, 0xFF2563EB, 0xFF0EA5E9),
    "com.transsion.phoenix" to spec(R.drawable.ic_fusion_phoenix, AppCategory.BROWSER, 0xFFFF7A00, 0xFFEF4444, 0xFFEC4899),
    "com.uc.browser.en" to spec(R.drawable.ic_fusion_phoenix, AppCategory.BROWSER, 0xFFFF9800, 0xFFF97316, 0xFFEA580C),
    "com.ucmobile.intl" to spec(R.drawable.ic_fusion_phoenix, AppCategory.BROWSER, 0xFFFF8A00, 0xFFF97316, 0xFF2563EB),
    "com.ucturbo" to spec(R.drawable.ic_fusion_phoenix, AppCategory.BROWSER, 0xFFF97316, 0xFF6366F1, 0xFF2563EB),
    "com.vivaldi.browser" to spec(R.drawable.ic_fusion_vivaldi, AppCategory.BROWSER, 0xFFEF4444, 0xFFE11D48, 0xFFBE123C),
    "com.vivaldi.browser.snapshot" to spec(R.drawable.ic_fusion_vivaldi, AppCategory.BROWSER, 0xFF1E293B, 0xFFE11D48, 0xFF4F46E5, badge = "DEV"),
    "com.yandex.browser" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFFEF4444, 0xFFF43F5E, 0xFFF59E0B),
    "com.yandex.browser.lite" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFFF97316, 0xFFEF4444, 0xFFE11D48, badge = "LITE"),
    "company.thebrowser.arc" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFFEC4899, 0xFF6366F1, 0xFF38BDF8),
    "idm.internet.download.manager" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF0EA5E9, 0xFF2563EB, 0xFF1D4ED8),
    "idm.internet.download.manager.plus" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF2563EB, 0xFF4F46E5, 0xFF7C3AED, badge = "PRO"),
    "io.github.forkmaintainers.iceraven" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFF38BDF8, 0xFF2563EB, 0xFF312E81),
    "mark.via" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFF0EA5E9, 0xFF2563EB, 0xFF1D4ED8),
    "mark.via.gp" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFF38BDF8, 0xFF0EA5E9, 0xFF2563EB),
    "mobi.mgeek.tunnybrowser" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF22C55E, 0xFF10B981, 0xFF0EA5E9),
    "net.onecook.browser" to spec(R.drawable.ic_fusion_via, AppCategory.BROWSER, 0xFF6366F1, 0xFF4F46E5, 0xFF0EA5E9),
    "net.waterfox.android" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFF06B6D4, 0xFF2563EB, 0xFF1E3A8A),
    "org.bromite.bromite" to spec(R.drawable.ic_fusion_browser_shield, AppCategory.BROWSER, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8),
    "org.cromite.cromite" to spec(R.drawable.ic_fusion_browser_shield, AppCategory.BROWSER, 0xFF22D3EE, 0xFF2563EB, 0xFF4F46E5),
    "org.ironfoxoss.ironfox" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFF97316, 0xFF475569, 0xFF1E293B),
    "org.lineageos.jelly" to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF22D3EE, 0xFF14B8A6, 0xFF059669),
    "org.mozilla.fenix" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFFF9500, 0xFFF97316, 0xFF7C3AED, badge = "DEV"),
    "org.mozilla.fennec_fdroid" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFFF7A00, 0xFFEF4444, 0xFF9333EA),
    "org.mozilla.firefox" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFFF9800, 0xFFFF5500, 0xFF7C3AED),
    "org.mozilla.firefox_beta" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFFF6B00, 0xFFEC4899, 0xFF6366F1, badge = "BETA"),
    "org.mozilla.focus" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFEC4899, 0xFF8B5CF6, 0xFF4F46E5),
    "org.mozilla.klar" to spec(R.drawable.ic_fusion_firefox, AppCategory.BROWSER, 0xFFEC4899, 0xFF9333EA, 0xFF4338CA),
    "org.torproject.torbrowser" to spec(R.drawable.ic_fusion_tor, AppCategory.BROWSER, 0xFF9333EA, 0xFF7C3AED, 0xFF4C1D95),
    "org.torproject.torbrowser_alpha" to spec(R.drawable.ic_fusion_tor, AppCategory.BROWSER, 0xFFA855F7, 0xFF6D28D9, 0xFF312E81, badge = "DEV"),
    "us.spotco.fennec_dos" to spec(R.drawable.ic_fusion_browser_shield, AppCategory.BROWSER, 0xFF8B5CF6, 0xFF4F46E5, 0xFF1E1B4B),
    "us.spotco.mulch" to spec(R.drawable.ic_fusion_browser_shield, AppCategory.BROWSER, 0xFF06B6D4, 0xFF0284C7, 0xFF1E3A8A),

    // Contacts & Dialers
    "com.android.contacts" to spec(R.drawable.ic_fusion_contacts, AppCategory.CONTACTS, 0xFF38BDF8, 0xFF0284C7, 0xFF0369A1),
    "com.android.dialer" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF34D399, 0xFF0EA5E9, 0xFF2563EB),
    "com.contacts.phone.dialer" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF22D3EE, 0xFF10B981, 0xFF059669),
    "com.drupe.swype.dialer" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF2DD4BF, 0xFF0EA5E9, 0xFF6366F1),
    "com.eyecon.global" to spec(R.drawable.ic_fusion_caller_id, AppCategory.DIALER, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8),
    "com.google.android.contacts" to spec(R.drawable.ic_fusion_contacts, AppCategory.CONTACTS, 0xFF60A5FA, 0xFF2563EB, 0xFF1E40AF),
    "com.google.android.dialer" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF34D399, 0xFF10B981, 0xFF2563EB),
    "com.hb.dialer.free" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF6366F1, 0xFF3B82F6, 0xFF06B6D4),
    "com.samsung.android.dialer" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF34D399, 0xFF10B981, 0xFF0F766E),
    "com.simplemobiletools.contacts" to spec(R.drawable.ic_fusion_contacts, AppCategory.CONTACTS, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.simplemobiletools.contacts.pro" to spec(R.drawable.ic_fusion_contacts, AppCategory.CONTACTS, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C, badge = "PRO"),
    "com.simplemobiletools.dialer" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.truecaller" to spec(R.drawable.ic_fusion_caller_id, AppCategory.DIALER, 0xFF38BDF8, 0xFF0087FF, 0xFF1D4ED8),
    "org.fossify.contacts" to spec(R.drawable.ic_fusion_contacts, AppCategory.CONTACTS, 0xFF34D399, 0xFF10B981, 0xFF06B6D4),
    "org.fossify.phone" to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF10B981, 0xFF14B8A6, 0xFF0F766E),

    // Files & Root Tools
    "com.android.documentsui" to spec(R.drawable.ic_fusion_files, AppCategory.FILES, 0xFF38BDF8, 0xFF2563EB, 0xFF1D4ED8),
    "com.google.android.apps.nbu.files" to spec(R.drawable.ic_fusion_files, AppCategory.FILES, 0xFF2563EB, 0xFF06B6D4, 0xFFF59E0B),
    "com.speedsoftware.rootexplorer" to spec(R.drawable.ic_fusion_root_files, AppCategory.DEVELOPER_ROOT, 0xFF2563EB, 0xFF1E3A8A, 0xFFF59E0B, badge = "#"),
    "com.thallyace.databasql" to spec(R.drawable.ic_fusion_database, AppCategory.DEVELOPER_ROOT, 0xFF22D3EE, 0xFF0284C7, 0xFF4F46E5),
    "com.thallyace.luckypatcher" to spec(R.drawable.ic_fusion_patcher, AppCategory.DEVELOPER_ROOT, 0xFF10B981, 0xFF059669, 0xFFF59E0B),
    "com.topjohnwu.magisk" to spec(R.drawable.ic_fusion_magisk, AppCategory.DEVELOPER_ROOT, 0xFF10B981, 0xFF0D9488, 0xFF065F46),
    "io.github.x0eg0.magisk" to spec(R.drawable.ic_fusion_magisk, AppCategory.DEVELOPER_ROOT, 0xFF34D399, 0xFF0D9488, 0xFF0F766E),

    // Gallery, Photos & Camera
    "com.android.gallery3d" to spec(R.drawable.ic_fusion_gallery, AppCategory.GALLERY, 0xFFFB7185, 0xFFEC4899, 0xFF8B5CF6),
    "com.google.android.apps.photos" to spec(R.drawable.ic_fusion_photos, AppCategory.GALLERY, 0xFF1E293B, 0xFF334155, 0xFF0F172A),
    "com.google.android.apps.photosgo" to spec(R.drawable.ic_fusion_gallery, AppCategory.GALLERY, 0xFF38BDF8, 0xFF2563EB, 0xFF6366F1, badge = "GO"),
    "com.transsion.camera" to spec(R.drawable.ic_fusion_camera, AppCategory.CAMERA, 0xFF334155, 0xFF1E293B, 0xFF0284C7),

    // System, Settings & Launchers
    "com.android.settings" to spec(R.drawable.ic_fusion_settings, AppCategory.SETTINGS_SYSTEM, 0xFF475569, 0xFF334155, 0xFF0F172A),
    "com.android.stk" to spec(R.drawable.ic_fusion_sim, AppCategory.SETTINGS_SYSTEM, 0xFF14B8A6, 0xFF0284C7, 0xFF4F46E5),
    "com.google.android.gms" to spec(R.drawable.ic_fusion_core_service, AppCategory.SETTINGS_SYSTEM, 0xFF14B8A6, 0xFF0EA5E9, 0xFF4F46E5),
    "com.hoffnung" to spec(R.drawable.ic_fusion_core_service, AppCategory.SETTINGS_SYSTEM, 0xFF334155, 0xFF0F766E, 0xFF0D9488),
    "com.thallyace.orbitaicon" to spec(R.drawable.ic_fusion_orbit, AppCategory.LAUNCHER, 0xFF22D3EE, 0xFF2563EB, 0xFF4F46E5),
    "com.thallyace.orbitlauncher" to spec(R.drawable.ic_fusion_orbit, AppCategory.LAUNCHER, 0xFF4F46E5, 0xFF2563EB, 0xFF22D3EE),

    // Productivity, Notes, Tasks, Office, Drive & Cloud
    "com.anydo" to spec(R.drawable.ic_fusion_tasks, AppCategory.NOTES_TASKS, 0xFF38BDF8, 0xFF2563EB, 0xFF4F46E5),
    "com.google.android.apps.books" to spec(R.drawable.ic_fusion_books, AppCategory.READING_NEWS, 0xFF38BDF8, 0xFF0EA5E9, 0xFF14B8A6),
    "com.google.android.apps.classroom" to spec(R.drawable.ic_fusion_classroom, AppCategory.PRODUCTIVITY_OFFICE, 0xFF22C55E, 0xFF16A34A, 0xFFF59E0B),
    "com.google.android.apps.docs" to spec(R.drawable.ic_fusion_drive, AppCategory.PRODUCTIVITY_OFFICE, 0xFF2563EB, 0xFF0284C7, 0xFF0D9488),
    "com.google.android.apps.docs.editors.docs" to spec(R.drawable.ic_fusion_docs, AppCategory.PRODUCTIVITY_OFFICE, 0xFF60A5FA, 0xFF2563EB, 0xFF1D4ED8),
    "com.google.android.apps.docs.editors.sheets" to spec(R.drawable.ic_fusion_sheets, AppCategory.PRODUCTIVITY_OFFICE, 0xFF34D399, 0xFF16A34A, 0xFF15803D),
    "com.google.android.apps.docs.editors.slides" to spec(R.drawable.ic_fusion_slides, AppCategory.PRODUCTIVITY_OFFICE, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.google.android.apps.keep" to spec(R.drawable.ic_fusion_notes, AppCategory.NOTES_TASKS, 0xFFFACC15, 0xFFF59E0B, 0xFFD97706),
    "com.google.android.keep" to spec(R.drawable.ic_fusion_notes, AppCategory.NOTES_TASKS, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    "com.google.android.apps.magazines" to spec(R.drawable.ic_fusion_news, AppCategory.READING_NEWS, 0xFF2563EB, 0xFF4F46E5, 0xFFEF4444),
    "com.google.android.apps.subscriptions.red" to spec(R.drawable.ic_fusion_cloud, AppCategory.PRODUCTIVITY_OFFICE, 0xFF2563EB, 0xFF7C3AED, 0xFFEC4899),
    "com.google.android.apps.tasks" to spec(R.drawable.ic_fusion_tasks, AppCategory.NOTES_TASKS, 0xFF60A5FA, 0xFF2563EB, 0xFF1D4ED8),
    "com.google.android.apps.translate" to spec(R.drawable.ic_fusion_translate, AppCategory.PRODUCTIVITY_OFFICE, 0xFF3B82F6, 0xFF2563EB, 0xFF4F46E5),
    "com.google.android.calculator" to spec(R.drawable.ic_fusion_calculator, AppCategory.CALCULATOR, 0xFF6366F1, 0xFF3B82F6, 0xFF06B6D4),
    "com.google.android.inputmethod.latin" to spec(R.drawable.ic_fusion_keyboard, AppCategory.KEYBOARD, 0xFF0EA5E9, 0xFF2563EB, 0xFF4F46E5),
    "com.t9numpad.ime" to spec(R.drawable.ic_fusion_keyboard, AppCategory.KEYBOARD, 0xFF475569, 0xFF334155, 0xFF0EA5E9),
    "com.microsoft.skydrive" to spec(R.drawable.ic_fusion_cloud, AppCategory.PRODUCTIVITY_OFFICE, 0xFF38BDF8, 0xFF0284C7, 0xFF0369A1),
    "com.microsoft.todos" to spec(R.drawable.ic_fusion_tasks, AppCategory.NOTES_TASKS, 0xFF38BDF8, 0xFF2563EB, 0xFF4338CA),
    "com.mobisystems.editor.office_registered" to spec(R.drawable.ic_fusion_docs, AppCategory.PRODUCTIVITY_OFFICE, 0xFFF97316, 0xFFEA580C, 0xFF4F46E5),
    "com.transsion.manualguide" to spec(R.drawable.ic_fusion_books, AppCategory.READING_NEWS, 0xFF14B8A6, 0xFF0EA5E9, 0xFF2563EB),

    // Email
    "com.google.android.gm" to spec(R.drawable.ic_fusion_email, AppCategory.EMAIL, 0xFFEF4444, 0xFFDC2626, 0xFFFB7185),
    "com.google.android.gm.lite" to spec(R.drawable.ic_fusion_email, AppCategory.EMAIL, 0xFFF43F5E, 0xFFEF4444, 0xFFF97316, badge = "GO"),
    "com.microsoft.office.outlook" to spec(R.drawable.ic_fusion_email, AppCategory.EMAIL, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8),

    // AI, Search & Smart Home
    "com.google.android.apps.bard" to spec(R.drawable.ic_fusion_gemini, AppCategory.AI_ASSISTANT, 0xFF0EA5E9, 0xFF6366F1, 0xFFEC4899),
    "com.google.android.apps.googleassistant" to spec(R.drawable.ic_fusion_assistant, AppCategory.AI_ASSISTANT, 0xFF38BDF8, 0xFF2563EB, 0xFF6366F1),
    "com.google.android.apps.searchlite" to spec(R.drawable.ic_fusion_search, AppCategory.BROWSER, 0xFF2563EB, 0xFF0EA5E9, 0xFF22D3EE, badge = "GO"),
    "com.google.android.googlequicksearchbox" to spec(R.drawable.ic_fusion_search, AppCategory.BROWSER, 0xFF3B82F6, 0xFF2563EB, 0xFF06B6D4),
    "com.google.android.apps.chromecast.app" to spec(R.drawable.ic_fusion_home, AppCategory.SETTINGS_SYSTEM, 0xFF3B82F6, 0xFF10B981, 0xFFF59E0B),
    "com.transsion.aibox" to spec(R.drawable.ic_fusion_gemini, AppCategory.AI_ASSISTANT, 0xFF06B6D4, 0xFF4F46E5, 0xFF7C3AED),

    // Maps, Earth & Travel
    "com.google.android.apps.maps" to spec(R.drawable.ic_fusion_maps, AppCategory.MAPS_TRAVEL, 0xFF10B981, 0xFF0EA5E9, 0xFF2563EB),
    "com.google.android.apps.mapslite" to spec(R.drawable.ic_fusion_maps, AppCategory.MAPS_TRAVEL, 0xFF06B6D4, 0xFF10B981, 0xFF059669, badge = "GO"),
    "com.google.android.apps.travel.onthego" to spec(R.drawable.ic_fusion_travel, AppCategory.MAPS_TRAVEL, 0xFF38BDF8, 0xFF0284C7, 0xFF2563EB),
    "com.google.earth" to spec(R.drawable.ic_fusion_earth, AppCategory.MAPS_TRAVEL, 0xFF0F172A, 0xFF0284C7, 0xFF22D3EE),
    "net.osmand" to spec(R.drawable.ic_fusion_maps, AppCategory.MAPS_TRAVEL, 0xFFF97316, 0xFFEA580C, 0xFFF59E0B),

    // Wallet & Finance
    "com.google.android.apps.nbu.paisa.user" to spec(R.drawable.ic_fusion_wallet, AppCategory.WALLET_FINANCE, 0xFF2563EB, 0xFF0EA5E9, 0xFF10B981),
    "com.google.android.apps.walletnfcrel" to spec(R.drawable.ic_fusion_wallet, AppCategory.WALLET_FINANCE, 0xFF1E293B, 0xFF1D4ED8, 0xFF0D9488),

    // Health & Fitness
    "com.google.android.apps.fitness" to spec(R.drawable.ic_fusion_fitness, AppCategory.HEALTH_FITNESS, 0xFFF43F5E, 0xFFF59E0B, 0xFF10B981),
    "com.google.android.apps.healthdata" to spec(R.drawable.ic_fusion_health, AppCategory.HEALTH_FITNESS, 0xFFFB7185, 0xFFEC4899, 0xFF14B8A6),

    // Video, Music, Radio, Podcasts & Sound
    "com.google.android.apps.podcasts" to spec(R.drawable.ic_fusion_podcast, AppCategory.MUSIC_AUDIO, 0xFF7C3AED, 0xFF6366F1, 0xFFF59E0B),
    "com.google.android.apps.youtube.kids" to spec(R.drawable.ic_fusion_youtube, AppCategory.VIDEO_MEDIA, 0xFFEF4444, 0xFFF43F5E, 0xFFF59E0B, badge = "KIDS"),
    "com.google.android.apps.youtube.music" to spec(R.drawable.ic_fusion_music_disc, AppCategory.MUSIC_AUDIO, 0xFFEF4444, 0xFFDC2626, 0xFFFB7185),
    "com.google.android.apps.youtube.music.pwa" to spec(R.drawable.ic_fusion_music_disc, AppCategory.MUSIC_AUDIO, 0xFFE11D48, 0xFFBE123C, 0xFFC026D3),
    "com.google.android.music" to spec(R.drawable.ic_fusion_music_disc, AppCategory.MUSIC_AUDIO, 0xFFF97316, 0xFFEA580C, 0xFFF59E0B),
    "com.google.android.videos" to spec(R.drawable.ic_fusion_video, AppCategory.VIDEO_MEDIA, 0xFF6366F1, 0xFF2563EB, 0xFF0EA5E9),
    "com.google.android.youtube" to spec(R.drawable.ic_fusion_youtube, AppCategory.VIDEO_MEDIA, 0xFFEF4444, 0xFFDC2626, 0xFFB91C1C),
    "com.google.android.youtube.lite" to spec(R.drawable.ic_fusion_youtube, AppCategory.VIDEO_MEDIA, 0xFFE11D48, 0xFFDC2626, 0xFFFB7185, badge = "GO"),
    "com.thallyace.googlyplayer" to spec(R.drawable.ic_fusion_video, AppCategory.VIDEO_MEDIA, 0xFF7C3AED, 0xFF4F46E5, 0xFF06B6D4),
    "com.transsion.fmradio" to spec(R.drawable.ic_fusion_radio, AppCategory.MUSIC_AUDIO, 0xFFF43F5E, 0xFFEC4899, 0xFFF59E0B),
    "com.transsion.soundrecorder" to spec(R.drawable.ic_fusion_recorder, AppCategory.MUSIC_AUDIO, 0xFFDC2626, 0xFFE11D48, 0xFFFB7185),

    // Games
    "com.google.android.play.games" to spec(R.drawable.ic_fusion_gamepad, AppCategory.GAMES, 0xFF22C55E, 0xFF16A34A, 0xFF059669),
    "com.taptap.global" to spec(R.drawable.ic_fusion_gamepad, AppCategory.GAMES, 0xFF06B6D4, 0xFF14B8A6, 0xFF10B981),
    "com.tencent.ig" to spec(R.drawable.ic_fusion_gamepad, AppCategory.GAMES, 0xFF334155, 0xFF1E293B, 0xFFD97706),

    // Weather
    "com.jupiter.tornado" to spec(R.drawable.ic_fusion_storm, AppCategory.WEATHER, 0xFF312E81, 0xFF1D4ED8, 0xFF0EA5E9),

    // Social, Video Call & Community Messaging
    "com.bitstrips.imoji" to spec(R.drawable.ic_fusion_avatar, AppCategory.SOCIAL_CHAT, 0xFF4ADE80, 0xFF22C55E, 0xFF10B981),
    "com.discord" to spec(R.drawable.ic_fusion_discord, AppCategory.SOCIAL_CHAT, 0xFF5865F2, 0xFF6366F1, 0xFF7C3AED),
    "com.facebook.katana" to spec(R.drawable.ic_fusion_facebook, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF1877F2, 0xFF1D4ED8),
    "com.facebook.lite" to spec(R.drawable.ic_fusion_facebook, AppCategory.SOCIAL_CHAT, 0xFF60A5FA, 0xFF2563EB, 0xFF1D4ED8, badge = "LITE"),
    "com.facebook.mlite" to spec(R.drawable.ic_fusion_messenger, AppCategory.SOCIAL_CHAT, 0xFF22D3EE, 0xFF0EA5E9, 0xFF6366F1, badge = "LITE"),
    "com.facebook.orca" to spec(R.drawable.ic_fusion_messenger, AppCategory.SOCIAL_CHAT, 0xFF0084FF, 0xFFA033FF, 0xFFFF5280),
    "com.facebook.pages.app" to spec(R.drawable.ic_fusion_analytics_flag, AppCategory.SOCIAL_CHAT, 0xFFF97316, 0xFFEA580C, 0xFF4F46E5),
    "com.google.android.apps.dynamite" to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFF34D399, 0xFF10B981, 0xFF059669),
    "com.google.android.apps.meetings" to spec(R.drawable.ic_fusion_videocall, AppCategory.SOCIAL_CHAT, 0xFF0EA5E9, 0xFF10B981, 0xFFF59E0B),
    "com.google.android.apps.tachyon" to spec(R.drawable.ic_fusion_videocall, AppCategory.SOCIAL_CHAT, 0xFF2563EB, 0xFF0EA5E9, 0xFF22D3EE),
    "com.imo.android.imoim" to spec(R.drawable.ic_fusion_videocall, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF0284C7, 0xFF0369A1),
    "com.imo.android.imoimlite" to spec(R.drawable.ic_fusion_videocall, AppCategory.SOCIAL_CHAT, 0xFF22D3EE, 0xFF0EA5E9, 0xFF0284C7, badge = "LITE"),
    "com.instagram.android" to spec(R.drawable.ic_fusion_instagram, AppCategory.SOCIAL_CHAT, 0xFF7C3AED, 0xFFEC4899, 0xFFF59E0B),
    "com.instagram.lite" to spec(R.drawable.ic_fusion_instagram, AppCategory.SOCIAL_CHAT, 0xFFEC4899, 0xFFF43F5E, 0xFFF97316, badge = "LITE"),
    "com.kakao.talk" to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFFFACC15, 0xFFF59E0B, 0xFFD97706),
    "com.klinker.android.twitter_l" to spec(R.drawable.ic_fusion_social_feather, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF0284C7, 0xFF2563EB),
    "com.linkedin.android" to spec(R.drawable.ic_fusion_linkedin, AppCategory.SOCIAL_CHAT, 0xFF0EA5E9, 0xFF0A66C2, 0xFF1E40AF),
    "com.linkedin.android.lite" to spec(R.drawable.ic_fusion_linkedin, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8, badge = "LITE"),
    "com.phonepvr.friends" to spec(R.drawable.ic_fusion_friends, AppCategory.SOCIAL_CHAT, 0xFF6366F1, 0xFF8B5CF6, 0xFFFB7185),
    "com.pinterest" to spec(R.drawable.ic_fusion_pinterest, AppCategory.SOCIAL_CHAT, 0xFFEF4444, 0xFFDC2626, 0xFFE11D48),
    "com.pinterest.twa" to spec(R.drawable.ic_fusion_pinterest, AppCategory.SOCIAL_CHAT, 0xFFF43F5E, 0xFFE11D48, 0xFFFB7185, badge = "LITE"),
    "com.reddit.frontpage" to spec(R.drawable.ic_fusion_reddit, AppCategory.SOCIAL_CHAT, 0xFFFF5722, 0xFFFF4500, 0xFFEA580C),
    "com.skype.m2" to spec(R.drawable.ic_fusion_videocall, AppCategory.SOCIAL_CHAT, 0xFF22D3EE, 0xFF0EA5E9, 0xFF0284C7),
    "com.skype.raider" to spec(R.drawable.ic_fusion_videocall, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8),
    "com.snapchat.android" to spec(R.drawable.ic_fusion_snapchat, AppCategory.SOCIAL_CHAT, 0xFFFACC15, 0xFFFBBF24, 0xFFF59E0B),
    "com.ss.android.ugc.aweme" to spec(R.drawable.ic_fusion_tiktok, AppCategory.SOCIAL_CHAT, 0xFF0F172A, 0xFF1E1B4B, 0xFF271530),
    "com.ss.android.ugc.trill" to spec(R.drawable.ic_fusion_tiktok, AppCategory.SOCIAL_CHAT, 0xFF0F172A, 0xFF18181B, 0xFF31102F),
    "com.tencent.mm" to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFF22C55E, 0xFF16A34A, 0xFF15803D),
    "com.tumblr" to spec(R.drawable.ic_fusion_notes, AppCategory.SOCIAL_CHAT, 0xFF0F172A, 0xFF1E3A8A, 0xFF312E81),
    "com.twitter.android" to spec(R.drawable.ic_fusion_x_social, AppCategory.SOCIAL_CHAT, 0xFF0F172A, 0xFF1E293B, 0xFF0284C7),
    "com.twitter.android.lite" to spec(R.drawable.ic_fusion_x_social, AppCategory.SOCIAL_CHAT, 0xFF1E293B, 0xFF0369A1, 0xFF0EA5E9, badge = "LITE"),
    "com.viber.voip" to spec(R.drawable.ic_fusion_dialer, AppCategory.SOCIAL_CHAT, 0xFF8B5CF6, 0xFF7C3AED, 0xFF6D28D9),
    "com.vkontakte.android" to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFF0077FF, 0xFF2563EB, 0xFF1D4ED8),
    "com.whatsapp" to spec(R.drawable.ic_fusion_whatsapp, AppCategory.SOCIAL_CHAT, 0xFF25D366, 0xFF16A34A, 0xFF15803D),
    "com.whatsapp.w4b" to spec(R.drawable.ic_fusion_whatsapp, AppCategory.SOCIAL_CHAT, 0xFF14B8A6, 0xFF0F766E, 0xFF16A34A, badge = "BIZ"),
    "com.zhiliaoapp.musically" to spec(R.drawable.ic_fusion_tiktok, AppCategory.SOCIAL_CHAT, 0xFF0F172A, 0xFF1E1B4B, 0xFF2A1432),
    "com.zhiliaoapp.musically.go" to spec(R.drawable.ic_fusion_tiktok, AppCategory.SOCIAL_CHAT, 0xFF0F172A, 0xFF1E293B, 0xFF312E81, badge = "GO"),
    "jp.naver.line.android" to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFF22C55E, 0xFF16A34A, 0xFF059669),
    "jp.naver.line.android.lite" to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFF34D399, 0xFF10B981, 0xFF059669, badge = "LITE"),
    "org.telegram.messenger" to spec(R.drawable.ic_fusion_telegram, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF0EA5E9, 0xFF0284C7),
    "org.telegram.messenger.web" to spec(R.drawable.ic_fusion_telegram, AppCategory.SOCIAL_CHAT, 0xFF22D3EE, 0xFF0284C7, 0xFF2563EB),
    "org.thoughtcrime.securesms" to spec(R.drawable.ic_fusion_signal, AppCategory.SOCIAL_CHAT, 0xFF3B82F6, 0xFF2563EB, 0xFF1D4ED8),
  )

  private val CATEGORY_FALLBACKS: Map<AppCategory, FusionIconSpec> = mapOf(
    AppCategory.DIALER to spec(R.drawable.ic_fusion_dialer, AppCategory.DIALER, 0xFF34D399, 0xFF10B981, 0xFF0284C7),
    AppCategory.SMS_MESSAGING to spec(R.drawable.ic_fusion_sms, AppCategory.SMS_MESSAGING, 0xFF38BDF8, 0xFF0EA5E9, 0xFF2563EB),
    AppCategory.CONTACTS to spec(R.drawable.ic_fusion_contacts, AppCategory.CONTACTS, 0xFF38BDF8, 0xFF0284C7, 0xFF1D4ED8),
    AppCategory.CALENDAR to spec(R.drawable.ic_fusion_calendar, AppCategory.CALENDAR, 0xFF3B82F6, 0xFF2563EB, 0xFF4F46E5),
    AppCategory.CLOCK to spec(R.drawable.ic_fusion_clock, AppCategory.CLOCK, 0xFF6366F1, 0xFF4F46E5, 0xFF0EA5E9),
    AppCategory.CALCULATOR to spec(R.drawable.ic_fusion_calculator, AppCategory.CALCULATOR, 0xFF6366F1, 0xFF3B82F6, 0xFF06B6D4),
    AppCategory.BROWSER to spec(R.drawable.ic_fusion_earth, AppCategory.BROWSER, 0xFF38BDF8, 0xFF0EA5E9, 0xFF2563EB),
    AppCategory.CAMERA to spec(R.drawable.ic_fusion_camera, AppCategory.CAMERA, 0xFF334155, 0xFF1E293B, 0xFF0284C7),
    AppCategory.GALLERY to spec(R.drawable.ic_fusion_gallery, AppCategory.GALLERY, 0xFFFB7185, 0xFFEC4899, 0xFF8B5CF6),
    AppCategory.FILES to spec(R.drawable.ic_fusion_files, AppCategory.FILES, 0xFF38BDF8, 0xFF2563EB, 0xFF1D4ED8),
    AppCategory.MUSIC_AUDIO to spec(R.drawable.ic_fusion_music_disc, AppCategory.MUSIC_AUDIO, 0xFFF43F5E, 0xFFE11D48, 0xFF7C3AED),
    AppCategory.VIDEO_MEDIA to spec(R.drawable.ic_fusion_video, AppCategory.VIDEO_MEDIA, 0xFF7C3AED, 0xFF4F46E5, 0xFF06B6D4),
    AppCategory.EMAIL to spec(R.drawable.ic_fusion_email, AppCategory.EMAIL, 0xFFEF4444, 0xFFDC2626, 0xFFFB7185),
    AppCategory.NOTES_TASKS to spec(R.drawable.ic_fusion_notes, AppCategory.NOTES_TASKS, 0xFFFBBF24, 0xFFF59E0B, 0xFFEA580C),
    AppCategory.MAPS_TRAVEL to spec(R.drawable.ic_fusion_maps, AppCategory.MAPS_TRAVEL, 0xFF10B981, 0xFF0EA5E9, 0xFF2563EB),
    AppCategory.SHOPPING to spec(R.drawable.ic_fusion_shopping, AppCategory.SHOPPING, 0xFFF97316, 0xFFEA580C, 0xFFEF4444),
    AppCategory.FOOD_DELIVERY to spec(R.drawable.ic_fusion_delivery, AppCategory.FOOD_DELIVERY, 0xFFFACC15, 0xFFF59E0B, 0xFF10B981),
    AppCategory.APP_STORE to spec(R.drawable.ic_fusion_appstore, AppCategory.APP_STORE, 0xFF22D3EE, 0xFF2563EB, 0xFF6366F1),
    AppCategory.CLEANER_SECURITY to spec(R.drawable.ic_fusion_security_scan, AppCategory.CLEANER_SECURITY, 0xFF10B981, 0xFF06B6D4, 0xFF2563EB),
    AppCategory.SETTINGS_SYSTEM to spec(R.drawable.ic_fusion_settings, AppCategory.SETTINGS_SYSTEM, 0xFF475569, 0xFF334155, 0xFF0F172A),
    AppCategory.WALLET_FINANCE to spec(R.drawable.ic_fusion_wallet, AppCategory.WALLET_FINANCE, 0xFF1E293B, 0xFF2563EB, 0xFF10B981),
    AppCategory.KEYBOARD to spec(R.drawable.ic_fusion_keyboard, AppCategory.KEYBOARD, 0xFF0EA5E9, 0xFF2563EB, 0xFF4F46E5),
    AppCategory.DEVELOPER_ROOT to spec(R.drawable.ic_fusion_root_files, AppCategory.DEVELOPER_ROOT, 0xFF10B981, 0xFF0D9488, 0xFF1E293B),
    AppCategory.GAMES to spec(R.drawable.ic_fusion_gamepad, AppCategory.GAMES, 0xFF22C55E, 0xFF10B981, 0xFF06B6D4),
    AppCategory.SOCIAL_CHAT to spec(R.drawable.ic_fusion_chat, AppCategory.SOCIAL_CHAT, 0xFF38BDF8, 0xFF6366F1, 0xFF8B5CF6),
    AppCategory.PRODUCTIVITY_OFFICE to spec(R.drawable.ic_fusion_docs, AppCategory.PRODUCTIVITY_OFFICE, 0xFF3B82F6, 0xFF2563EB, 0xFF4F46E5),
    AppCategory.READING_NEWS to spec(R.drawable.ic_fusion_books, AppCategory.READING_NEWS, 0xFF0EA5E9, 0xFF14B8A6, 0xFF2563EB),
    AppCategory.AI_ASSISTANT to spec(R.drawable.ic_fusion_gemini, AppCategory.AI_ASSISTANT, 0xFF0EA5E9, 0xFF6366F1, 0xFFEC4899),
    AppCategory.HEALTH_FITNESS to spec(R.drawable.ic_fusion_health, AppCategory.HEALTH_FITNESS, 0xFFFB7185, 0xFFEC4899, 0xFF14B8A6),
    AppCategory.WEATHER to spec(R.drawable.ic_fusion_storm, AppCategory.WEATHER, 0xFF38BDF8, 0xFF2563EB, 0xFF312E81),
    AppCategory.LAUNCHER to spec(R.drawable.ic_fusion_orbit, AppCategory.LAUNCHER, 0xFF22D3EE, 0xFF2563EB, 0xFF4F46E5),
    AppCategory.OTHER to spec(R.drawable.ic_fusion_core_service, AppCategory.OTHER, 0xFF38BDF8, 0xFF4F46E5, 0xFF7C3AED),
  )

  fun findSpec(packageName: String): FusionIconSpec? =
    PACKAGE_SPECS[packageName.lowercase(Locale.ROOT)]

  fun resolveForApp(
    packageName: String,
    label: String,
    androidCategory: Int,
  ): FusionIconSpec? {
    findSpec(packageName)?.let { return it }
    val match = IconPackMatcher.classify(packageName, label, androidCategory)
    // Only auto-apply a Fusion fallback when an intelligent keyword or Android OS category matched
    return if (match.category != AppCategory.OTHER) {
      CATEGORY_FALLBACKS[match.category]
    } else {
      null
    }
  }

  fun categorySpec(category: AppCategory): FusionIconSpec =
    CATEGORY_FALLBACKS.getValue(category)

  /**
   * Look up a FusionIconSpec by either its key in PACKAGE_SPECS or "cat:<NAME>" fallback key.
   */
  fun findSpecByChoiceKey(choiceKey: String): FusionIconSpec? {
    val clean = choiceKey.trim()
    if (clean.startsWith("cat:")) {
      val catName = clean.removePrefix("cat:")
      val category = AppCategory.entries.firstOrNull { it.name == catName } ?: return null
      return CATEGORY_FALLBACKS[category]
    }
    return findSpec(clean)
  }

  /**
   * Curated deduplicated catalog of icons belonging strictly to the built-in Orbit icon packs
   * (Fusion, Oreo 8.0 Contrast, Obsidian Dark) so unsupported apps can manually select any icon
   * within the active Orbit pack without crossing into any other pack.
   */
  val ORBIT_CATALOG_CHOICES: List<OrbitIconChoice> by lazy {
    val labelOverrides = mapOf(
      "org.mozilla.firefox" to "Firefox",
      "com.sec.android.app.sbrowser" to "Samsung Internet",
      "com.microsoft.emmx" to "Microsoft Edge",
      "com.opera.browser" to "Opera",
      "com.opera.gx" to "Opera GX",
      "com.opera.mini.native" to "Opera Mini",
      "com.brave.browser" to "Brave",
      "com.duckduckgo.mobile.android" to "DuckDuckGo",
      "com.vivaldi.browser" to "Vivaldi",
      "org.torproject.torbrowser" to "Tor Browser",
      "com.transsion.phoenix" to "Phoenix",
      "mark.via.gp" to "Via Browser",
      "com.ecosia.android" to "Ecosia",
      "org.cromite.cromite" to "Cromite / Privacy",
      "com.android.chrome" to "Chrome",
      "com.whatsapp" to "WhatsApp",
      "org.telegram.messenger" to "Telegram",
      "org.thoughtcrime.securesms" to "Signal",
      "com.discord" to "Discord",
      "com.instagram.android" to "Instagram",
      "com.facebook.katana" to "Facebook",
      "com.facebook.orca" to "Messenger",
      "com.zhiliaoapp.musically" to "TikTok",
      "com.snapchat.android" to "Snapchat",
      "com.reddit.frontpage" to "Reddit",
      "com.twitter.android" to "X / Social",
      "com.linkedin.android" to "LinkedIn",
      "com.pinterest" to "Pinterest",
      "com.google.android.dialer" to "Phone / Dialer",
      "com.truecaller" to "Caller ID",
      "com.google.android.apps.messaging" to "SMS / Messages",
      "com.google.android.contacts" to "Contacts",
      "com.google.android.calendar" to "Calendar",
      "com.google.android.deskclock" to "Clock",
      "com.alarmclock.xtreme" to "Alarm",
      "com.google.android.calculator" to "Calculator",
      "com.transsion.camera" to "Camera",
      "com.google.android.apps.photos" to "Photos",
      "com.android.gallery3d" to "Gallery",
      "com.google.android.apps.nbu.files" to "Files",
      "com.speedsoftware.rootexplorer" to "Root Explorer",
      "com.topjohnwu.magisk" to "Magisk",
      "com.thallyace.databasql" to "SQLite / DB",
      "com.thallyace.luckypatcher" to "Patcher",
      "com.google.android.gm" to "Email / Mail",
      "com.google.android.apps.keep" to "Notes",
      "com.google.android.apps.tasks" to "Tasks",
      "com.google.android.apps.docs" to "Drive / Cloud",
      "com.google.android.apps.docs.editors.docs" to "Docs",
      "com.google.android.apps.docs.editors.sheets" to "Sheets",
      "com.google.android.apps.docs.editors.slides" to "Slides",
      "com.google.android.apps.translate" to "Translate",
      "com.google.android.apps.maps" to "Maps",
      "com.google.earth" to "Earth / Globe",
      "com.google.android.apps.travel.onthego" to "Travel",
      "com.android.vending" to "Play Store",
      "org.fdroid.fdroid" to "F-Droid",
      "com.aurora.store" to "App Store",
      "com.google.android.youtube" to "YouTube",
      "com.google.android.apps.youtube.music" to "Music",
      "com.google.android.videos" to "Video Player",
      "com.transsion.fmradio" to "FM Radio",
      "com.transsion.soundrecorder" to "Recorder",
      "com.google.android.apps.podcasts" to "Podcasts",
      "com.google.android.apps.walletnfcrel" to "Wallet / Pay",
      "com.digibites.accubattery" to "Battery",
      "com.avg.cleaner" to "Cleaner",
      "com.clover.secscanner" to "Security Scan",
      "com.google.android.apps.authenticator2" to "Authenticator",
      "com.transsion.applock" to "App Lock",
      "com.android.settings" to "Settings",
      "com.android.stk" to "SIM Toolkit",
      "com.google.android.inputmethod.latin" to "Keyboard",
      "com.google.android.play.games" to "Games",
      "com.google.android.apps.bard" to "Gemini / AI",
      "com.google.android.apps.googleassistant" to "Assistant",
      "com.google.android.googlequicksearchbox" to "Search",
      "com.google.android.apps.fitness" to "Fitness",
      "com.google.android.apps.healthdata" to "Health",
      "com.jupiter.tornado" to "Weather / Storm",
      "com.google.android.apps.books" to "Books",
      "com.google.android.apps.magazines" to "News",
      "app.jumia.android" to "Shopping",
      "com.naivas.app" to "Grocery",
      "com.glovo" to "Delivery",
      "com.shein.android" to "Fashion",
      "com.thallyace.orbitlauncher" to "Orbit Emblem",
    )

    val seenVisualSignature = HashSet<String>()
    val result = ArrayList<OrbitIconChoice>(labelOverrides.size)

    for ((pkg, label) in labelOverrides) {
      val spec = PACKAGE_SPECS[pkg] ?: continue
      val sig = "${spec.drawableRes}_${spec.gradientStart}_${spec.badgeText}"
      if (seenVisualSignature.add(sig)) {
        result.add(OrbitIconChoice(key = pkg, label = label, category = spec.category, spec = spec))
      }
    }
    for (category in AppCategory.entries) {
      val spec = CATEGORY_FALLBACKS[category] ?: continue
      val sig = "${spec.drawableRes}_${spec.gradientStart}_${spec.badgeText}"
      if (seenVisualSignature.add(sig)) {
        result.add(
          OrbitIconChoice(
            key = "cat:${category.name}",
            label = category.displayName,
            category = category,
            spec = spec,
          )
        )
      }
    }
    result
  }
}
