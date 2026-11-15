package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.WallpaperPreset
import com.example.data.WallpaperState
import com.example.icons.ExternalIconPackLoader
import com.example.icons.IconPackOption

@Composable
fun LauncherAtmosphericBackground(
  wallpaperState: WallpaperState,
  blurMixProvider: () -> Float,
  scaleProvider: () -> Float,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer {
        val s = scaleProvider()
        scaleX = s
        scaleY = s
      },
  ) {
    // Crisp sharp wallpaper (visible on Far-Left Time Page)
    WallpaperLayerContent(
      wallpaperState = wallpaperState,
      modifier = Modifier.fillMaxSize(),
    )

    // Heavy frosted blur layer GPU-crossfaded across App Pages (1+)
    val isNativeBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurModifier =
      if (isNativeBlurSupported) {
        Modifier
          .fillMaxSize()
          .blur(56.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
      } else {
        Modifier.fillMaxSize()
      }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { alpha = blurMixProvider() },
    ) {
      WallpaperLayerContent(
        wallpaperState = wallpaperState,
        modifier = blurModifier,
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = if (isNativeBlurSupported) {
                listOf(
                  Color(0x78060A12),
                  Color(0x90060A12),
                  Color(0xAC060A12),
                )
              } else {
                listOf(
                  Color(0xB4060A12),
                  Color(0xC6060A12),
                  Color(0xD8060A12),
                )
              }
            )
          ),
      )
    }
  }
}

@Composable
fun WallpaperLayerContent(
  wallpaperState: WallpaperState,
  modifier: Modifier = Modifier,
) {
  val customBmp = wallpaperState.customBitmap
  if (wallpaperState.activePreset == WallpaperPreset.CUSTOM_PHOTO && customBmp != null) {
    Image(
      bitmap = customBmp,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      filterQuality = FilterQuality.High,
      modifier = modifier,
    )
  } else {
    WallpaperPresetArtwork(
      preset = wallpaperState.activePreset,
      modifier = modifier,
    )
  }
}

@Composable
private fun WallpaperPresetArtwork(
  preset: WallpaperPreset,
  modifier: Modifier = Modifier,
) {
  when (preset) {
    WallpaperPreset.ORBIT_NEBULA,
    WallpaperPreset.CUSTOM_PHOTO -> {
      Image(
        painter = painterResource(id = R.drawable.img_launcher_backdrop),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
      )
    }

    WallpaperPreset.MIDNIGHT_AURORA -> {
      AtmosphericMeshCanvas(
        baseColors = listOf(Color(0xFF060B17), Color(0xFF0B1F3A), Color(0xFF071321)),
        primaryGlow = Color(0xAA10B981),
        secondaryGlow = Color(0x9922D3EE),
        accentGlow = Color(0x886366F1),
        modifier = modifier,
      )
    }

    WallpaperPreset.OBSIDIAN_DUSK -> {
      AtmosphericMeshCanvas(
        baseColors = listOf(Color(0xFF0B0A16), Color(0xFF231738), Color(0xFF140D1B)),
        primaryGlow = Color(0x99F59E0B),
        secondaryGlow = Color(0x88EC4899),
        accentGlow = Color(0x887C3AED),
        modifier = modifier,
      )
    }

    WallpaperPreset.EMERALD_MIST -> {
      AtmosphericMeshCanvas(
        baseColors = listOf(Color(0xFF051014), Color(0xFF0D2E35), Color(0xFF07191E)),
        primaryGlow = Color(0x992DD4BF),
        secondaryGlow = Color(0x880EA5E9),
        accentGlow = Color(0x7714B8A6),
        modifier = modifier,
      )
    }

    WallpaperPreset.NOCTURNE_SAPPHIRE -> {
      AtmosphericMeshCanvas(
        baseColors = listOf(Color(0xFF050A18), Color(0xFF0F2452), Color(0xFF091229)),
        primaryGlow = Color(0xAA3B82F6),
        secondaryGlow = Color(0x8838BDF8),
        accentGlow = Color(0x884F46E5),
        modifier = modifier,
      )
    }
  }
}

@Composable
private fun AtmosphericMeshCanvas(
  baseColors: List<Color>,
  primaryGlow: Color,
  secondaryGlow: Color,
  accentGlow: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    drawRect(
      brush = Brush.verticalGradient(colors = baseColors),
      size = size,
    )
    val maxDim = maxOf(size.width, size.height)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(primaryGlow, Color.Transparent),
        center = Offset(size.width * 0.22f, size.height * 0.24f),
        radius = maxDim * 0.62f,
      ),
      radius = maxDim * 0.62f,
      center = Offset(size.width * 0.22f, size.height * 0.24f),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(secondaryGlow, Color.Transparent),
        center = Offset(size.width * 0.85f, size.height * 0.42f),
        radius = maxDim * 0.56f,
      ),
      radius = maxDim * 0.56f,
      center = Offset(size.width * 0.85f, size.height * 0.42f),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(accentGlow, Color.Transparent),
        center = Offset(size.width * 0.45f, size.height * 0.88f),
        radius = maxDim * 0.65f,
      ),
      radius = maxDim * 0.65f,
      center = Offset(size.width * 0.45f, size.height * 0.88f),
    )
  }
}

@Composable
fun WallpaperPickerSheetContent(
  wallpaperState: WallpaperState,
  availableIconPacks: List<IconPackOption>,
  activeIconPackId: String,
  isImportingIconPack: Boolean,
  iconImportStatusMessage: String?,
  isIconImportError: Boolean,
  isThallyFolderDisbanded: Boolean,
  onSelectIconPack: (String) -> Unit,
  onImportIconPack: () -> Unit,
  onDeleteImportedIconPack: (String) -> Unit,
  onToggleThallyFolderDisbanded: () -> Unit,
  onSelectPreset: (WallpaperPreset) -> Unit,
  onChooseCustomPhoto: () -> Unit,
  onUseAsDefaultHome: () -> Unit,
) {
  val presetList = buildList {
    if (wallpaperState.customBitmap != null) {
      add(WallpaperPreset.CUSTOM_PHOTO to stringResource(id = R.string.wallpaper_preset_custom))
    }
    add(WallpaperPreset.ORBIT_NEBULA to stringResource(id = R.string.wallpaper_preset_orbit))
    add(WallpaperPreset.MIDNIGHT_AURORA to stringResource(id = R.string.wallpaper_preset_aurora))
    add(WallpaperPreset.OBSIDIAN_DUSK to stringResource(id = R.string.wallpaper_preset_dusk))
    add(WallpaperPreset.EMERALD_MIST to stringResource(id = R.string.wallpaper_preset_emerald))
    add(WallpaperPreset.NOCTURNE_SAPPHIRE to stringResource(id = R.string.wallpaper_preset_nocturne))
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .navigationBarsPadding()
      .padding(horizontal = 24.dp)
      .padding(bottom = 26.dp)
      .testTag("wallpaper_picker_sheet"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(id = R.string.customize_launcher),
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(id = R.string.wallpaper_menu_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.65f),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Surface(
        color = Color.White.copy(alpha = 0.12f),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier
          .clip(CircleShape)
          .clickable(onClick = onUseAsDefaultHome)
          .testTag("use_as_default_home_button"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Home,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(id = R.string.set_as_default_home),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Icon Packs section
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Palette,
        contentDescription = null,
        tint = Color(0xFF22D3EE),
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = stringResource(id = R.string.icon_packs_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = stringResource(id = R.string.icon_packs_subtitle),
      style = MaterialTheme.typography.bodySmall,
      color = Color.White.copy(alpha = 0.62f),
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .testTag("icon_pack_selector_row"),
    ) {
      availableIconPacks.forEach { pack ->
        val selected = pack.id == activeIconPackId
        val previewBrush = when (pack.id) {
          ExternalIconPackLoader.PACK_ID_ORBIT_FUSION -> Brush.linearGradient(
            colors = listOf(Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFFEC4899)),
          )
          ExternalIconPackLoader.PACK_ID_ORBIT_OREO -> Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
          )
          ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN -> Brush.linearGradient(
            colors = listOf(Color(0xFF070A11), Color(0xFF0D1423), Color(0xFF142034)),
          )
          ExternalIconPackLoader.PACK_ID_IPHONE -> Brush.verticalGradient(
            colors = listOf(Color(0xFF38C9FF), Color(0xFF007AFF)),
          )
          ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT -> Brush.linearGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF1E293B)),
          )
          else -> if (pack.isImported) {
            Brush.linearGradient(
              colors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFDC2626)),
            )
          } else {
            Brush.linearGradient(
              colors = listOf(Color(0xFF10B981), Color(0xFF0EA5E9), Color(0xFF6366F1)),
            )
          }
        }
        val previewIcon = when (pack.id) {
          ExternalIconPackLoader.PACK_ID_ORBIT_FUSION -> Icons.Rounded.AutoAwesome
          ExternalIconPackLoader.PACK_ID_ORBIT_OREO -> Icons.Rounded.Contrast
          ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN -> Icons.Rounded.DarkMode
          ExternalIconPackLoader.PACK_ID_IPHONE -> Icons.Rounded.PhoneIphone
          ExternalIconPackLoader.PACK_ID_SYSTEM_DEFAULT -> Icons.Rounded.Layers
          else -> if (pack.isImported) Icons.Rounded.Archive else Icons.Rounded.Palette
        }
        val previewTint = when (pack.id) {
          ExternalIconPackLoader.PACK_ID_ORBIT_OREO -> Color(0xFF0F172A)
          ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN -> Color(0xFF22D3EE)
          else -> Color.White
        }
        val badgeBorderColor = when (pack.id) {
          ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN -> Color(0xFF22D3EE).copy(alpha = 0.75f)
          ExternalIconPackLoader.PACK_ID_ORBIT_OREO -> Color(0xFF0EA5E9).copy(alpha = 0.55f)
          ExternalIconPackLoader.PACK_ID_IPHONE -> Color.White.copy(alpha = 0.55f)
          else -> Color.White.copy(alpha = 0.35f)
        }

        Box(
          modifier = Modifier
            .width(108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              if (selected) Color(0xFF22D3EE).copy(alpha = 0.14f)
              else Color.White.copy(alpha = 0.06f),
              RoundedCornerShape(18.dp),
            )
            .border(
              width = if (selected) 1.6.dp else 0.85.dp,
              color = if (selected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.16f),
              shape = RoundedCornerShape(18.dp),
            )
            .clickable { onSelectIconPack(pack.id) }
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .testTag("icon_pack_option_${pack.id}"),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(48.dp)
                .clip(DefaultAppleSquircleShape)
                .background(previewBrush, DefaultAppleSquircleShape)
                .border(
                  width = if (pack.id == ExternalIconPackLoader.PACK_ID_ORBIT_OBSIDIAN) 1.2.dp else 0.8.dp,
                  color = badgeBorderColor,
                  shape = DefaultAppleSquircleShape,
                ),
            ) {
              Icon(
                imageVector = previewIcon,
                contentDescription = pack.title,
                tint = previewTint,
                modifier = Modifier.size(24.dp),
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = pack.title,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
              ),
              color = if (selected) Color.White else Color.White.copy(alpha = 0.88f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (pack.isImported) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xCC0F172A))
                .clickable { onDeleteImportedIconPack(pack.id) }
                .testTag("delete_imported_pack_${pack.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(id = R.string.delete_imported_pack_desc, pack.title),
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(13.dp),
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
      onClick = onImportIconPack,
      enabled = !isImportingIconPack,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(
        width = 0.9.dp,
        color = Color(0xFF22D3EE).copy(alpha = 0.45f),
      ),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color(0xFF22D3EE).copy(alpha = 0.08f),
        contentColor = Color.White,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("import_icon_pack_button"),
    ) {
      if (isImportingIconPack) {
        CircularProgressIndicator(
          color = Color(0xFF22D3EE),
          strokeWidth = 2.dp,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(id = R.string.importing_icon_pack),
          style = MaterialTheme.typography.labelLarge,
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.FileUpload,
          contentDescription = null,
          tint = Color(0xFF22D3EE),
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(id = R.string.import_icon_pack_button),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }

    if (!iconImportStatusMessage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = iconImportStatusMessage,
        style = MaterialTheme.typography.bodySmall,
        color = if (isIconImportError) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
        modifier = Modifier.testTag("icon_import_status_text"),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Thally Ace folder disband toggle
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
        .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
        .clickable(onClick = onToggleThallyFolderDisbanded)
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .testTag("sheet_thally_disband_row"),
    ) {
      Icon(
        imageVector = if (isThallyFolderDisbanded) Icons.Rounded.GridView else Icons.Rounded.FolderSpecial,
        contentDescription = null,
        tint = Color(0xFFFBBF24),
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(id = R.string.thally_folder_setting_title),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = Color.White,
        )
        Text(
          text = stringResource(id = R.string.thally_folder_setting_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.65f),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Switch(
        checked = isThallyFolderDisbanded,
        onCheckedChange = { onToggleThallyFolderDisbanded() },
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color(0xFF06101B),
          checkedTrackColor = Color(0xFFFBBF24),
        ),
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = stringResource(id = R.string.wallpaper_section_title),
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = Color.White,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
    ) {
      presetList.forEach { (preset, label) ->
        val selected = wallpaperState.activePreset == preset
        val previewState = wallpaperState.copy(activePreset = preset)

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .width(82.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectPreset(preset) }
            .padding(4.dp)
            .testTag("wallpaper_preset_${preset.id}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(width = 74.dp, height = 114.dp)
              .clip(RoundedCornerShape(16.dp))
              .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(16.dp),
              ),
          ) {
            WallpaperLayerContent(
              wallpaperState = previewState,
              modifier = Modifier.fillMaxSize(),
            )
            if (selected) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF22D3EE)),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = null,
                  tint = Color(0xFF06101B),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Button(
      onClick = onChooseCustomPhoto,
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .testTag("wallpaper_choose_photo_button"),
    ) {
      Icon(
        imageVector = Icons.Rounded.PhotoLibrary,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(id = R.string.wallpaper_choose_photo),
        style = MaterialTheme.typography.titleMedium,
      )
    }
  }
}
