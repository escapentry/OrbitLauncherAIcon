package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppRepository
import com.example.data.SquircleIconRenderer
import com.example.icons.AppCategory
import com.example.icons.ExternalIconPackLoader
import com.example.icons.FusionIconRegistry
import com.example.icons.FusionIconRenderer
import com.example.icons.IconPackMatcher
import com.example.icons.ImportedIconPackStore
import com.example.icons.OrbitPackStyle
import com.example.model.AppEntry
import com.example.ui.LauncherViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Orbit", appName)
  }

  @Test
  fun `apps chunk cleanly into horizontal pages`() {
    val apps = (1..25).map { idx -> entry("com.example.app$idx", "App $idx") }

    val pages = LauncherViewModel.chunkIntoHorizontalPages(apps, itemsPerPage = 16)
    assertEquals(2, pages.size)
    assertEquals(16, pages[0].size)
    assertEquals(9, pages[1].size)
  }

  @Test
  fun `search ranks prefix before word and acronym matches`() {
    val apps = listOf(
      entry("com.example.map", "Road Map"),
      entry("com.google.maps", "Google Maps"),
      entry("com.example.gm", "Gmail"),
    )

    val byAcronym = LauncherViewModel.filterAndRankApps(apps, "gm")
    assertEquals(listOf("Gmail", "Google Maps"), byAcronym.map { it.label })
  }

  @Test
  fun `home press navigates to start of apps then far-left time page`() {
    assertEquals(1, LauncherViewModel.resolveHomeTargetPage(currentPage = 3, appPageCount = 3))
    assertEquals(0, LauncherViewModel.resolveHomeTargetPage(currentPage = 1, appPageCount = 3))
    assertEquals(1, LauncherViewModel.resolveHomeTargetPage(currentPage = 0, appPageCount = 3))
  }

  @Test
  fun `dominant color extractor picks vibrant app hue over white or black`() {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(AndroidColor.WHITE)
    for (y in 16 until 48) {
      for (x in 16 until 48) {
        bitmap.setPixel(x, y, AndroidColor.rgb(29, 185, 84))
      }
    }

    val color = SquircleIconRenderer.extractVibrantColor(bitmap)
    bitmap.recycle()

    assertTrue(color.green > color.red && color.green > color.blue)
    assertTrue(color != Color.White)
  }

  @Test
  fun `icon palette extractor yields primary and distinct secondary colors for gradient splash`() {
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(AndroidColor.TRANSPARENT)
    for (y in 8 until 32) {
      for (x in 8 until 56) {
        bitmap.setPixel(x, y, AndroidColor.rgb(255, 120, 20)) // vivid orange
      }
    }
    for (y in 32 until 56) {
      for (x in 8 until 56) {
        bitmap.setPixel(x, y, AndroidColor.rgb(30, 144, 255)) // vivid blue
      }
    }

    val (primary, secondary) = SquircleIconRenderer.extractIconPalette(bitmap)
    bitmap.recycle()

    assertTrue(primary != secondary)
  }

  @Test
  fun `wallpaper preset resolves known ids and defaults safely`() {
    assertEquals(
      com.example.data.WallpaperPreset.MIDNIGHT_AURORA,
      com.example.data.WallpaperPreset.fromId("midnight_aurora"),
    )
    assertEquals(
      com.example.data.WallpaperPreset.ORBIT_NEBULA,
      com.example.data.WallpaperPreset.fromId("unknown_id"),
    )
  }

  @Test
  fun `thally ace apps are isolated from regular pages even when pages fill`() {
    val regularApps = (1..32).map { idx -> entry("com.example.app$idx", "App $idx") }
    val thallyApps = listOf(
      entry("com.aistudio.aiomedia.mvtqkp", "AIO Media"),
      entry("com.thallyace.explored", "Explored"),
      entry("com.thallyace.folklore", "Folklore"),
      entry("com.thallyace.nececity", "Nececity"),
      entry("com.thallyace.determinal", "Determinal"),
    )

    val (filteredRegular, dedicatedThally) = LauncherViewModel.partitionThallyAceApps(regularApps + thallyApps)
    val pages = LauncherViewModel.chunkIntoHorizontalPages(filteredRegular, itemsPerPage = 16)

    assertEquals(32, filteredRegular.size)
    assertEquals(5, dedicatedThally.size)
    assertEquals(2, pages.size)
    assertTrue(pages.flatten().none { it.packageName in LauncherViewModel.THALLY_ACE_PACKAGES })
    assertTrue(dedicatedThally.all { it.packageName in LauncherViewModel.THALLY_ACE_PACKAGES })
  }

  @Test
  fun `smart dock excludes thally ace packages from auto fill unless explicitly pinned`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = com.example.data.AppRepository(context)
    val regularApp = entry("com.example.browser", "Browser")
    val thallyApp = entry("com.thallyace.explored", "Explored").copy(launchCount = 99)

    val autoDock = repository.buildSmartDock(
      allApps = listOf(thallyApp, regularApp),
      maxCount = 4,
      pinnedComponentKeys = emptyList(),
      excludedSmartPackages = LauncherViewModel.THALLY_ACE_PACKAGES,
    )
    assertEquals(listOf("com.example.browser"), autoDock.map { it.packageName })

    val pinnedDock = repository.buildSmartDock(
      allApps = listOf(thallyApp, regularApp),
      maxCount = 4,
      pinnedComponentKeys = listOf(thallyApp.componentKey),
      excludedSmartPackages = LauncherViewModel.THALLY_ACE_PACKAGES,
    )
    assertEquals(listOf("com.thallyace.explored", "com.example.browser"), pinnedDock.map { it.packageName })
  }

  @Test
  fun `obsidian xml drawables render for all five thally ace apps`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val renderer = SquircleIconRenderer(context)
    val packages = listOf(
      "com.aistudio.aiomedia.mvtqkp",
      "com.thallyace.explored",
      "com.thallyace.folklore",
      "com.thallyace.nececity",
      "com.thallyace.determinal",
    )

    assertEquals(packages.toSet(), SquircleIconRenderer.THALLY_ACE_ICON_DRAWABLES.keys)
    for (pkg in packages) {
      val rendered = renderer.loadOrRender(
        packageName = pkg,
        activityName = "$pkg.MainActivity",
        componentKey = "$pkg/$pkg.MainActivity",
        targetSizePx = 128,
      )
      assertTrue("Expected rendered icon for $pkg", rendered != null)
      assertEquals(128, rendered!!.bitmap.width)
      assertEquals(128, rendered.bitmap.height)
    }
  }

  @Test
  fun `fusion icon registry maps and renders gradient material icons cleanly`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val renderer = SquircleIconRenderer(context)

      val requestedSampleAndSpecific = listOf(
        "app.clearsms", "app.jumia.android", "app.source.getapp", "com.aimatch.cleaner",
        "com.alarmclock.xtreme", "com.alarmclock.xtreme.free", "com.alibaba.intl.android.apps.poseidon",
        "com.alibaba.aliexpresshd", "com.amazon.mShop.android.shopping", "com.amazon.venezia",
        "com.android.calendar", "com.android.chrome", "com.android.contacts", "com.android.deskclock",
        "com.android.dialer", "com.android.documentsui", "com.android.gallery3d", "com.android.mms",
        "com.android.settings", "com.android.stk", "com.android.vending", "com.anydo",
        "com.apkpure.aegon", "com.aptoide.partners", "com.aurora.store", "com.avg.cleaner",
        "com.bitstrips.imoji", "com.brave.browser", "com.calendar.schedule.event", "com.carrefour.kenya",
        "com.chrome.beta", "com.chrome.canary", "com.chrome.dev", "com.clover.secscanner",
        "com.contacts.phone.dialer", "com.deventz.calendar.ken.g01", "com.digibites.accubattery",
        "com.discord", "com.drupe.swype.dialer", "com.ebay.mobile", "com.einnovation.temu",
        "com.eyecon.global", "com.facebook.katana", "com.facebook.lite", "com.facebook.mlite",
        "com.facebook.orca", "com.facebook.pages.app", "com.fdroid.fdroid", "com.glovo",
        "com.google.android.apps.adm", "com.google.android.apps.authenticator2", "com.google.android.apps.bard",
        "com.google.android.apps.books", "com.google.android.apps.chromecast.app",
        "com.google.android.apps.classroom", "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs", "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.slides", "com.google.android.apps.dynamite",
        "com.google.android.apps.fitness", "com.google.android.apps.googleassistant",
        "com.google.android.apps.healthdata", "com.google.android.apps.keep",
        "com.google.android.apps.magazines", "com.google.android.apps.maps",
        "com.google.android.apps.mapslite", "com.google.android.apps.meetings",
        "com.google.android.apps.messaging", "com.google.android.apps.nbu.files",
        "com.google.android.apps.nbu.paisa.user", "com.google.android.apps.photos",
        "com.google.android.apps.photosgo", "com.google.android.apps.podcasts",
        "com.google.android.apps.searchlite", "com.google.android.apps.subscriptions.red",
        "com.google.android.apps.tachyon", "com.google.android.apps.tasks",
        "com.google.android.apps.translate", "com.google.android.apps.travel.onthego",
        "com.google.android.apps.walletnfcrel", "com.google.android.apps.youtube.kids",
        "com.google.android.apps.youtube.music", "com.google.android.apps.youtube.music.pwa",
        "com.google.android.calendar", "com.google.android.calculator", "com.google.android.contacts",
        "com.google.android.deskclock", "com.google.android.dialer", "com.google.android.gm",
        "com.google.android.gm.lite", "com.google.android.gms", "com.google.android.googlequicksearchbox",
        "com.google.android.inputmethod.latin", "com.google.android.keep", "com.google.android.music",
        "com.google.android.play.games", "com.google.android.videos", "com.google.android.youtube",
        "com.google.android.youtube.lite", "com.google.earth", "com.handcent.app.nextsms",
        "com.hb.dialer.free", "com.hoffnung", "com.huawei.appmarket", "com.imo.android.imoim",
        "com.imo.android.imoimlite", "com.instagram.android", "com.instagram.lite", "com.jiji.app",
        "com.jupiter.tornado", "com.kakao.talk", "com.kilimall.shopping",
        "com.klinker.android.twitter_l", "com.linkedin.android", "com.linkedin.android.lite",
        "com.maf.carrefour", "com.microsoft.office.outlook", "com.microsoft.skydrive",
        "com.microsoft.todos", "com.mobisystems.editor.office_registered", "com.moez.QKSMS",
        "com.naivas.app", "com.oraimo.store", "com.p1.chompsms", "com.phonepvr.friends",
        "com.pinterest", "com.pinterest.twa", "com.reddit.frontpage",
        "com.samsung.android.app.clockpackage", "com.samsung.android.calendar",
        "com.samsung.android.dialer", "com.samsung.android.messaging",
        "com.sec.android.app.clockpackage", "com.sec.android.app.samsungapps", "com.shein.android",
        "com.simplemobiletools.calendar", "com.simplemobiletools.calendar.pro",
        "com.simplemobiletools.clock", "com.simplemobiletools.contacts",
        "com.simplemobiletools.contacts.pro", "com.simplemobiletools.dialer",
        "com.simplemobiletools.smsmessenger", "com.skype.m2", "com.skype.raider",
        "com.snapchat.android", "com.speedsoftware.rootexplorer", "com.ss.android.ugc.aweme",
        "com.ss.android.ugc.trill", "com.t9numpad.ime", "com.taptap.global", "com.tencent.ig",
        "com.tencent.mm", "com.textra", "com.thallyace.databasql", "com.thallyace.googlyplayer",
        "com.thallyace.luckypatcher", "com.thallyace.orbitaicon", "com.thallyace.orbitlauncher",
        "com.timetreeapp.schedule", "com.topjohnwu.magisk", "com.transsion.aibox",
        "com.transsion.applock", "com.transsion.camera", "com.transsion.deskclock",
        "com.transsion.fmradio", "com.transsion.manualguide", "com.transsion.palmstore",
        "com.transsion.phonemanager", "com.transsion.phonemaster", "com.transsion.soundrecorder",
        "com.truecaller", "com.tumblr", "com.twitter.android", "com.twitter.android.lite",
        "com.ubercab.eats", "com.uptodown", "com.viber.voip", "com.vkontakte.android",
        "com.whatsapp", "com.whatsapp.w4b", "com.xiaomi.mipicks", "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically.go", "com.zzkko", "dev.octoshrimpy.quik",
        "io.github.x0eg0.magisk", "jp.naver.line.android", "jp.naver.line.android.lite",
        "net.osmand", "org.fdroid.fdroid", "org.fossify.calendar", "org.fossify.clock",
        "org.fossify.contacts", "org.fossify.messages", "org.fossify.phone",
        "org.telegram.messenger", "org.telegram.messenger.web", "org.thoughtcrime.securesms",
        "xyz.klinker.messenger",
      )

      for (pkg in requestedSampleAndSpecific) {
        val spec = FusionIconRegistry.findSpec(pkg)
        assertNotNull("Missing FusionIconSpec for $pkg", spec)
        assertNotNull(
          "Drawable missing for $pkg",
          ContextCompat.getDrawable(context, spec!!.drawableRes),
        )
      }

      // Render a diverse batch end-to-end through SquircleIconRenderer in Orbit Fusion mode
      val renderSample = listOf(
        "com.whatsapp", "com.google.android.dialer", "com.instagram.android",
        "com.hoffnung", "com.jupiter.tornado", "com.thallyace.databasql",
        "com.naivas.app", "com.topjohnwu.magisk", "org.fossify.phone",
      )
      for (pkg in renderSample) {
        val rendered = renderer.loadOrRender(
          packageName = pkg,
          activityName = "$pkg.MainActivity",
          componentKey = "$pkg/$pkg.MainActivity",
          targetSizePx = 128,
          label = pkg,
          androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
          packId = ExternalIconPackLoader.PACK_ID_ORBIT_FUSION,
        )
        assertNotNull("Expected Fusion icon render for $pkg", rendered)
        assertEquals(128, rendered!!.bitmap.width)
        assertEquals(128, rendered.bitmap.height)
      }
    }

  @Test
  fun `intelligent keyword and android category rules match unknown apps`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val renderer = SquircleIconRenderer(context)

      val dialerMatch = IconPackMatcher.classify("com.vendor.superdialer", "My Dialer")
      assertEquals(AppCategory.DIALER, dialerMatch.category)

      val smsMatch = IconPackMatcher.classify("com.unknown.messenger", "Ultra SMS")
      assertEquals(AppCategory.SMS_MESSAGING, smsMatch.category)

      val gameByOs = IconPackMatcher.classify(
        packageName = "com.opaque.build",
        label = "Quest",
        androidCategory = ApplicationInfo.CATEGORY_GAME,
      )
      assertEquals(AppCategory.GAMES, gameByOs.category)

      val unknownApp = IconPackMatcher.classify(
        packageName = "com.opaque.widget",
        label = "Widget",
        androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
      )
      assertEquals(AppCategory.OTHER, unknownApp.category)
      assertNull(
        FusionIconRegistry.resolveForApp("com.opaque.widget", "Widget", ApplicationInfo.CATEGORY_UNDEFINED),
      )

      val dynamicDialer = renderer.loadOrRender(
        packageName = "com.vendor.superdialer",
        activityName = "com.vendor.superdialer.Main",
        componentKey = "com.vendor.superdialer/Main",
        targetSizePx = 128,
        label = "My Dialer",
        androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
        packId = ExternalIconPackLoader.PACK_ID_ORBIT_FUSION,
      )
      assertNotNull(dynamicDialer)
      assertEquals(128, dynamicDialer!!.bitmap.width)
    }

  @Test
  fun `external appfilter xml parser and intelligent category matcher associate dialer apps`() {
    val xml = """
      <resources>
        <iconback img1="iconback_squircle" />
        <iconmask img1="iconmask_squircle" />
        <scale factor="0.82" />
        <item component="ComponentInfo{com.google.android.dialer/com.google.android.dialer.DialerActivity}" drawable="custom_dialer_glyph" />
        <calendar component="ComponentInfo{com.google.android.calendar/.AllInOneActivity}" prefix="cal_day_" />
      </resources>
    """.trimIndent()

    val parsed = ExternalIconPackLoader.parseAppFilterXml(xml)
    assertEquals(listOf("iconback_squircle"), parsed.iconBacks)
    assertEquals("iconmask_squircle", parsed.iconMask)
    assertEquals(0.82f, parsed.scaleFactor)
    assertEquals("cal_day_", parsed.calendarPrefixes["com.google.android.calendar"])

    // An unknown third-party dialer associates with com.google.android.dialer in external icon packs
    val matchedDrawable = ExternalIconPackLoader.resolveDrawableNameFromFilter(
      packData = parsed,
      packageName = "com.custom.turbodialer",
      activityName = "com.custom.turbodialer.MainActivity",
      label = "Turbo Dialer",
      androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
    )
    assertEquals("custom_dialer_glyph", matchedDrawable)
  }

  @Test
  fun `thally folder disband preference persists while keeping apps on dedicated page`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val repository = AppRepository(context)

      repository.saveThallyFolderDisbanded(true)
      assertEquals(true, repository.loadThallyFolderDisbanded())

      repository.saveActiveIconPackId(ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT)
      assertEquals(ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT, repository.loadActiveIconPackId())
    }

  @Test
  fun `all non chrome web browsers map and render across fusion oreo and obsidian packs`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val renderer = SquircleIconRenderer(context)

      val nonChromeBrowsers = listOf(
        "org.mozilla.firefox",
        "org.mozilla.focus",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.gx",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "org.torproject.torbrowser",
        "com.transsion.phoenix",
        "mark.via.gp",
        "com.ecosia.android",
        "org.cromite.cromite",
        "org.bromite.bromite",
        "net.waterfox.android",
        "com.kiwibrowser.browser",
        "com.yandex.browser",
      )

      val packs = listOf(
        ExternalIconPackLoader.PACK_ID_ORBIT_FUSION,
        ExternalIconPackLoader.PACK_ID_ORBIT_OREO,
        ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN,
        ExternalIconPackLoader.PACK_ID_IPHONE,
      )

      for (pkg in nonChromeBrowsers) {
        val spec = FusionIconRegistry.findSpec(pkg)
        assertNotNull("Missing browser icon spec for $pkg", spec)
        assertEquals(AppCategory.BROWSER, spec!!.category)
        assertEquals(AppCategory.BROWSER, IconPackMatcher.classify(pkg, pkg).category)

        for (packId in packs) {
          val rendered = renderer.loadOrRender(
            packageName = pkg,
            activityName = "$pkg.MainActivity",
            componentKey = "$pkg/$pkg.MainActivity",
            targetSizePx = 128,
            label = pkg,
            androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
            packId = packId,
          )
          assertNotNull("Expected rendered browser icon for $pkg in $packId", rendered)
          assertEquals(128, rendered!!.bitmap.width)
          assertEquals(128, rendered.bitmap.height)
        }
      }
    }

  @Test
  fun `built in icon packs have clean names and distinct iphone oreo obsidian and fusion renderers`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val renderer = SquircleIconRenderer(context)

      val options = ExternalIconPackLoader.buildAvailableOptions(context)
      val builtInTitles = options.take(5).map { it.title }
      assertEquals(listOf("Fusion", "Oreo 8.1", "Obsidian", "iPhone", "System"), builtInTitles)

      assertEquals(OrbitPackStyle.FUSION, ExternalIconPackLoader.orbitStyleForPackId(ExternalIconPackLoader.PACK_ID_ORBIT_FUSION))
      assertEquals(OrbitPackStyle.OREO_CONTRAST, ExternalIconPackLoader.orbitStyleForPackId(ExternalIconPackLoader.PACK_ID_ORBIT_OREO))
      assertEquals(OrbitPackStyle.OBSIDIAN_DARK, ExternalIconPackLoader.orbitStyleForPackId(ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN))
      assertEquals(OrbitPackStyle.IPHONE, ExternalIconPackLoader.orbitStyleForPackId(ExternalIconPackLoader.PACK_ID_IPHONE))
      assertNull(ExternalIconPackLoader.orbitStyleForPackId(ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT))

      assertTrue(FusionIconRenderer.isWhitePlateOreoDrawable(R.drawable.ic_fusion_chrome))
      assertFalse(FusionIconRenderer.isWhitePlateOreoDrawable(R.drawable.ic_fusion_firefox))

      val chromeSpec = FusionIconRegistry.findSpec("com.android.chrome")!!
      val firefoxSpec = FusionIconRegistry.findSpec("org.mozilla.firefox")!!
      val dialerSpec = FusionIconRegistry.findSpec("com.google.android.dialer")!!
      for (style in OrbitPackStyle.entries) {
        val rawChrome = FusionIconRenderer.renderRawCanvasBitmap(context, chromeSpec, 192, style)
        val rawFirefox = FusionIconRenderer.renderRawCanvasBitmap(context, firefoxSpec, 192, style)
        val rawDialer = FusionIconRenderer.renderRawCanvasBitmap(context, dialerSpec, 192, style)
        assertNotNull("Expected raw canvas render for Chrome in $style", rawChrome)
        assertNotNull("Expected raw canvas render for Firefox in $style", rawFirefox)
        assertNotNull("Expected raw canvas render for Dialer in $style", rawDialer)
        assertEquals(192, rawChrome!!.width)
        assertEquals(192, rawFirefox!!.width)
        assertEquals(192, rawDialer!!.width)
        rawChrome.recycle()
        rawFirefox.recycle()
        rawDialer.recycle()
      }

      val chromeFusion = renderer.loadOrRender(
        packageName = "com.android.chrome",
        activityName = "com.google.android.apps.chrome.Main",
        componentKey = "com.android.chrome/Main",
        targetSizePx = 128,
        label = "Chrome",
        packId = ExternalIconPackLoader.PACK_ID_ORBIT_FUSION,
      )!!
      val chromeOreo = renderer.loadOrRender(
        packageName = "com.android.chrome",
        activityName = "com.google.android.apps.chrome.Main",
        componentKey = "com.android.chrome/Main",
        targetSizePx = 128,
        label = "Chrome",
        packId = ExternalIconPackLoader.PACK_ID_ORBIT_OREO,
      )!!
      val chromeObsidian = renderer.loadOrRender(
        packageName = "com.android.chrome",
        activityName = "com.google.android.apps.chrome.Main",
        componentKey = "com.android.chrome/Main",
        targetSizePx = 128,
        label = "Chrome",
        packId = ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN,
      )!!
      val chromeIphone = renderer.loadOrRender(
        packageName = "com.android.chrome",
        activityName = "com.google.android.apps.chrome.Main",
        componentKey = "com.android.chrome/Main",
        targetSizePx = 128,
        label = "Chrome",
        packId = ExternalIconPackLoader.PACK_ID_IPHONE,
      )!!

      assertTrue(chromeFusion !== chromeOreo)
      assertTrue(chromeOreo !== chromeObsidian)
      assertTrue(chromeObsidian !== chromeIphone)
      assertTrue(chromeFusion !== chromeIphone)
      assertEquals(128, chromeOreo.bitmap.width)
      assertEquals(128, chromeObsidian.bitmap.width)
      assertEquals(128, chromeIphone.bitmap.width)
    }

  @Test
  fun `zip icon pack import supports appfilter and axml without cross pack leakage`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val pngBytes = createSolidPngBytes(64, AndroidColor.MAGENTA)
      val altPngBytes = createSolidPngBytes(64, AndroidColor.CYAN)

      val zipFile = File(context.cacheDir, "Test_Imported_Pack.zip")
      ZipOutputStream(zipFile.outputStream()).use { zip ->
        zip.putNextEntry(ZipEntry("assets/appfilter.xml"))
        zip.write(
          """
          <resources>
            <item component="ComponentInfo{com.opaque.widget/com.opaque.widget.MainActivity}" drawable="custom_star" />
          </resources>
          """.trimIndent().toByteArray(Charsets.UTF_8)
        )
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("res/drawable-xxhdpi/custom_star.png"))
        zip.write(pngBytes)
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("res/drawable-xxhdpi/alt_orb.png"))
        zip.write(altPngBytes)
        zip.closeEntry()
      }

      val importResult = ImportedIconPackStore.importFromUri(context, Uri.fromFile(zipFile))
      assertTrue(importResult.isSuccess)
      val packInfo = importResult.getOrThrow()
      assertTrue(ImportedIconPackStore.isImportedPackId(packInfo.id))
      assertEquals(2, packInfo.iconCount)

      try {
        val listed = ImportedIconPackStore.listImportedPacks(context)
        assertTrue(listed.any { it.id == packInfo.id })

        // Choices for imported pack are strictly scoped to imported pack drawables only
        val importedChoices = ExternalIconPackLoader.listChoicesForActivePack(context, packInfo.id)
        assertEquals(setOf("custom_star", "alt_orb"), importedChoices.map { it.choiceKey }.toSet())

        // Cross-pack preview lookup strictly rejected
        val renderer = SquircleIconRenderer(context)
        assertNull(renderer.renderPackChoicePreview(ExternalIconPackLoader.PACK_ID_ORBIT_FUSION, "alt_orb", 96))
        assertNull(renderer.renderPackChoicePreview(ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT, "com.android.chrome", 96))
        assertNotNull(renderer.renderPackChoicePreview(packInfo.id, "alt_orb", 96))

        // Unsupported app in Orbit pack vs supported via appfilter in imported pack
        assertFalse(
          ExternalIconPackLoader.isAppSupportedInPack(
            context = context,
            packId = ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN,
            packageName = "com.opaque.widget",
            activityName = "com.opaque.widget.MainActivity",
            label = "Widget",
            androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
            thallyAcePackages = LauncherViewModel.THALLY_ACE_PACKAGES,
          )
        )
        assertTrue(
          ExternalIconPackLoader.isAppSupportedInPack(
            context = context,
            packId = packInfo.id,
            packageName = "com.opaque.widget",
            activityName = "com.opaque.widget.MainActivity",
            label = "Widget",
            androidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
            thallyAcePackages = LauncherViewModel.THALLY_ACE_PACKAGES,
          )
        )

        // Manual override inside imported pack works cleanly
        val overridden = ExternalIconPackLoader.resolveImportedPackDrawable(
          context = context,
          packId = packInfo.id,
          targetPackageName = "com.unmapped.app",
          targetActivityName = "com.unmapped.app.Main",
          targetLabel = "Unmapped",
          targetAndroidCategory = ApplicationInfo.CATEGORY_UNDEFINED,
          nativeDrawableProvider = { null },
          targetSizePx = 128,
          manualChoiceKey = "alt_orb",
        )
        assertNotNull(overridden)
      } finally {
        ImportedIconPackStore.deleteImportedPack(context, packInfo.id)
        zipFile.delete()
      }

      // Verify minimal compiled AXML appfilter binary parser as well
      val sampleAxml = buildMinimalAxmlItem(
        component = "ComponentInfo{org.mozilla.firefox/org.mozilla.firefox.App}",
        drawable = "firefox_icon",
      )
      val parsedAxml = ImportedIconPackStore.parseBinaryAppFilter(sampleAxml)
      assertNotNull(parsedAxml)
      assertEquals("firefox_icon", parsedAxml!!.packageToDrawable["org.mozilla.firefox"])
    }

  @Test
  fun `manual icon overrides remain strictly isolated to the active icon pack`() =
    kotlinx.coroutines.test.runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val repository = AppRepository(context)
      val compKey = "com.opaque.widget/com.opaque.widget.MainActivity"

      val saved = repository.setManualIconOverride(
        packId = ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN,
        componentKey = compKey,
        choiceKey = "org.mozilla.firefox",
      )
      assertEquals("org.mozilla.firefox", saved[ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN]?.get(compKey))
      // Must NOT cross-apply into Oreo, Fusion, or iPhone
      assertNull(saved[ExternalIconPackLoader.PACK_ID_ORBIT_OREO]?.get(compKey))
      assertNull(saved[ExternalIconPackLoader.PACK_ID_ORBIT_FUSION]?.get(compKey))
      assertNull(saved[ExternalIconPackLoader.PACK_ID_IPHONE]?.get(compKey))

      val cleared = repository.clearManualIconOverride(ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN, compKey)
      assertNull(cleared[ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN]?.get(compKey))
    }

  private fun createSolidPngBytes(size: Int, color: Int): ByteArray {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(color)
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    bmp.recycle()
    return out.toByteArray()
  }

  private fun buildMinimalAxmlItem(component: String, drawable: String): ByteArray {
    val strings = listOf("item", "component", "drawable", component, drawable)
    val stringBytes = ByteArrayOutputStream()
    val offsets = IntArray(strings.size)
    for ((idx, str) in strings.withIndex()) {
      offsets[idx] = stringBytes.size()
      val utf8 = str.toByteArray(Charsets.UTF_8)
      stringBytes.write(str.length)
      stringBytes.write(utf8.size)
      stringBytes.write(utf8)
      stringBytes.write(0)
    }
    while (stringBytes.size() % 4 != 0) stringBytes.write(0)

    val spHeaderSize = 28
    val offsetsTableSize = strings.size * 4
    val stringsStart = spHeaderSize + offsetsTableSize
    val spChunkSize = stringsStart + stringBytes.size()

    val spBuf = ByteBuffer.allocate(spChunkSize).order(ByteOrder.LITTLE_ENDIAN)
    spBuf.putShort(0x0001) // RES_STRING_POOL_TYPE
    spBuf.putShort(spHeaderSize.toShort())
    spBuf.putInt(spChunkSize)
    spBuf.putInt(strings.size)
    spBuf.putInt(0) // styleCount
    spBuf.putInt(0x0100) // UTF-8 flag
    spBuf.putInt(stringsStart)
    spBuf.putInt(0) // stylesStart
    for (off in offsets) spBuf.putInt(off)
    spBuf.put(stringBytes.toByteArray())

    val startElemChunkSize = 36 + (2 * 20)
    val seBuf = ByteBuffer.allocate(startElemChunkSize).order(ByteOrder.LITTLE_ENDIAN)
    seBuf.putShort(0x0102) // RES_XML_START_ELEMENT_TYPE
    seBuf.putShort(16)
    seBuf.putInt(startElemChunkSize)
    seBuf.putInt(1) // lineNumber
    seBuf.putInt(-1) // comment
    seBuf.putInt(-1) // ns
    seBuf.putInt(0) // name = "item"
    seBuf.putShort(0x0014) // attributeStart relative to +16 -> +36
    seBuf.putShort(0x0014) // attributeSize = 20
    seBuf.putShort(2) // attributeCount
    seBuf.putShort(0) // idIndex
    seBuf.putShort(0) // classIndex
    seBuf.putShort(0) // styleIndex

    // attr 0: component
    seBuf.putInt(-1)
    seBuf.putInt(1) // "component"
    seBuf.putInt(3) // rawValue index -> component
    seBuf.putShort(8)
    seBuf.put(0)
    seBuf.put(0x03)
    seBuf.putInt(3)

    // attr 1: drawable
    seBuf.putInt(-1)
    seBuf.putInt(2) // "drawable"
    seBuf.putInt(4) // rawValue index -> drawable
    seBuf.putShort(8)
    seBuf.put(0)
    seBuf.put(0x03)
    seBuf.putInt(4)

    val totalSize = 8 + spChunkSize + startElemChunkSize
    val root = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
    root.putShort(0x0003) // RES_XML_TYPE
    root.putShort(8)
    root.putInt(totalSize)
    root.put(spBuf.array())
    root.put(seBuf.array())
    return root.array()
  }

  private fun entry(pkg: String, label: String): AppEntry = AppEntry(
    packageName = pkg,
    activityName = "$pkg.MainActivity",
    componentKey = "$pkg/$pkg.MainActivity",
    label = label,
    normalizedLabel = AppEntry.buildNormalized(label),
    acronym = AppEntry.buildAcronym(label),
    sectionChar = AppEntry.buildSectionChar(label),
    isSystemApp = false,
  )
}
