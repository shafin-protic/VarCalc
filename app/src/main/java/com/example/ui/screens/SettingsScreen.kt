package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CalculatorUiState
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledSurfaceHigh
import com.example.ui.theme.OledTextMuted
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary

@Composable
fun SettingsScreen(
  uiState: CalculatorUiState,
  onUpdateDecimalPlaces: (Int) -> Unit,
  onUpdateShowFractionFormat: (Boolean) -> Unit,
  onUpdateTrimTrailingZeros: (Boolean) -> Unit,
  onUpdateSmartPercentage: (Boolean) -> Unit,
  onUpdateHapticFeedback: (Boolean) -> Unit,
  onUpdateKeypressSound: (Boolean) -> Unit,
  onUpdateKeepScreenAwake: (Boolean) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)
  val ws = uiState.workspace

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OledBlack)
      .padding(horizontal = 16.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
          contentDescription = "Back",
          tint = OledTextPrimary,
        )
      }
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "Settings",
        color = OledTextPrimary,
        style = MaterialTheme.typography.headlineMedium,
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Decimal Precision Selector
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(OledSurface)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Decimal Places",
          color = OledTextPrimary,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = "Visible digits after decimal point for fractional results (default: 2).",
          color = OledTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf(0, 1, 2, 3, 4, 6).forEach { places ->
            val selected = ws.decimalPlaces == places
            Box(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) AccentCyan else OledSurfaceHigh)
                .clickable { onUpdateDecimalPlaces(places) },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = places.toString(),
                color = if (selected) OledBlack else OledTextPrimary,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              )
            }
          }
        }
      }

      // Display & Math Behavior
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(OledSurface)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Math & Display",
          color = AccentCyan,
          style = MaterialTheme.typography.titleSmall,
        )
        SettingsSwitchRow(
          title = "Stacked Fraction Display",
          subtitle = "Render divisions as numerator over denominator bars",
          checked = ws.showFractionFormat,
          onCheckedChange = onUpdateShowFractionFormat,
        )
        SettingsSwitchRow(
          title = "Trim Trailing Zeros",
          subtitle = "Show 2.5 instead of 2.50 on fractional outputs",
          checked = ws.trimTrailingZeros,
          onCheckedChange = onUpdateTrimTrailingZeros,
        )
        SettingsSwitchRow(
          title = "Smart Percentage (+ / −)",
          subtitle = "Evaluate 100 + 10% as 110 (multiplication always uses 0.10)",
          checked = ws.smartPercentage,
          onCheckedChange = onUpdateSmartPercentage,
        )
      }

      // Experience
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(OledSurface)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Experience",
          color = AccentCyan,
          style = MaterialTheme.typography.titleSmall,
        )
        SettingsSwitchRow(
          title = "Keypress Sound",
          subtitle = "Play a crisp tactile click sound on key presses",
          checked = ws.keypressSound,
          onCheckedChange = onUpdateKeypressSound,
        )
        SettingsSwitchRow(
          title = "Haptic Key Feedback",
          subtitle = "Vibrate subtly when tapping calculator keys",
          checked = ws.hapticFeedback,
          onCheckedChange = onUpdateHapticFeedback,
        )
        SettingsSwitchRow(
          title = "Keep Screen Awake",
          subtitle = "Prevent screen sleep during long calculation sessions",
          checked = ws.keepScreenAwake,
          onCheckedChange = onUpdateKeepScreenAwake,
        )
      }

      // Saved Data Summary
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(OledSurfaceElevated)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "Persistent Workspace",
          color = OledTextPrimary,
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = "Active variables: ${uiState.regularVariables.size} • History sessions: ${uiState.historySessions.size} • Function presets: ${uiState.functionPresets.size} • Compare bases: ${uiState.compareVariables.size}",
          color = OledTextMuted,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      // About Section
      val uriHandler = LocalUriHandler.current
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(OledSurface)
          .padding(16.dp)
          .testTag("settings_about_section"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "About",
          color = AccentCyan,
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = "</>Credit: Shafin Al Asad Protic </>",
          color = OledTextPrimary,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp,
        )
        Text(
          text = "www.facebook.com/shafin.protic",
          color = AccentCyan,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 13.sp,
          textDecoration = TextDecoration.Underline,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
              runCatching {
                uriHandler.openUri("https://www.facebook.com/shafin.protic")
              }
            }
            .padding(vertical = 2.dp),
        )
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onCheckedChange(!checked) }
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp),
    ) {
      Text(
        text = title,
        color = OledTextPrimary,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
      )
      Text(
        text = subtitle,
        color = OledTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = OledBlack,
        checkedTrackColor = AccentCyan,
        uncheckedThumbColor = OledTextSecondary,
        uncheckedTrackColor = OledSurfaceHigh,
      ),
    )
  }
}
