package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistorySessionEntity
import com.example.data.JsonCodec
import com.example.engine.MathEngine
import com.example.ui.components.MathExpressionView
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledSurface
import com.example.ui.theme.OledSurfaceElevated
import com.example.ui.theme.OledTextMuted
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary
import com.example.ui.theme.variableColorAt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  sessions: List<HistorySessionEntity>,
  decimalPlaces: Int,
  trimTrailingZeros: Boolean,
  showFractionFormat: Boolean,
  onRestoreSession: (HistorySessionEntity) -> Unit,
  onDeleteSession: (Long) -> Unit,
  onClearAllHistory: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

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
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = OledTextPrimary,
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Session History",
          color = OledTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
      }

      if (sessions.isNotEmpty()) {
        TextButton(onClick = onClearAllHistory) {
          Text("Clear All", color = AccentRose)
        }
      }
    }

    if (sessions.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(32.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = OledTextMuted,
            modifier = Modifier.size(48.dp),
          )
          Text(
            text = "No Saved Sessions Yet",
            color = OledTextPrimary,
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Long-press AC in Regular Mode to archive your current variables with date and time.",
            color = OledTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(sessions, key = { it.id }) { session ->
          HistorySessionCard(
            session = session,
            decimalPlaces = decimalPlaces,
            trimTrailingZeros = trimTrailingZeros,
            showFractionFormat = showFractionFormat,
            onRestore = { onRestoreSession(session) },
            onDelete = { onDeleteSession(session.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun HistorySessionCard(
  session: HistorySessionEntity,
  decimalPlaces: Int,
  trimTrailingZeros: Boolean,
  showFractionFormat: Boolean,
  onRestore: () -> Unit,
  onDelete: () -> Unit,
) {
  val items = remember(session.itemsJson) { JsonCodec.decodeHistoryItems(session.itemsJson) }
  val varColors = remember(items) {
    items.associate { it.name to variableColorAt(it.colorIndex) }
  }
  val dateStr = remember(session.timestamp) {
    SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(session.timestamp))
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(OledSurface)
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = dateStr,
          color = AccentCyan,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "${items.size} variable${if (items.size == 1) "" else "s"} • ANS = ${
            MathEngine.formatNumber(session.ansValue, decimalPlaces, trimTrailingZeros)
          }",
          color = OledTextMuted,
          fontSize = 11.sp,
          fontFamily = JetBrainsMonoFontFamily,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AccentCyan.copy(alpha = 0.16f))
            .clickable(onClick = onRestore)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Restore,
            contentDescription = "Restore session",
            tint = AccentCyan,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Restore",
            color = AccentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = "Delete session",
            tint = AccentRose,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    items.forEach { item ->
      val accent = variableColorAt(item.colorIndex)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(OledSurfaceElevated)
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
          Text(
            text = item.name,
            color = OledBlack,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
          )
        }

        Box(modifier = Modifier.weight(1f)) {
          MathExpressionView(
            expression = item.expression,
            showFractionFormat = showFractionFormat,
            variableColors = varColors,
            maxFontSize = 15.sp,
            minFontSize = 9.sp,
            alignEnd = false,
            modifier = Modifier.heightIn(min = 22.dp, max = 44.dp),
          )
        }

        Text(
          text = "= ${MathEngine.formatNumber(item.value, decimalPlaces, trimTrailingZeros)}",
          color = accent,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = JetBrainsMonoFontFamily,
        )
      }
    }
  }
}
