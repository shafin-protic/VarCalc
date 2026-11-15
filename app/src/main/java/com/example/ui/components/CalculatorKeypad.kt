package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledKeyNumber
import com.example.ui.theme.OledKeyOperator
import com.example.ui.theme.OledKeyUtility
import com.example.ui.theme.OledTextPrimary
import com.example.ui.theme.OledTextSecondary
import com.example.ui.theme.SpaceGroteskFontFamily

/**
 * Borderless OLED calculator keypad containing the exact 22 requested keys:
 * 0-9, +, -, *, /, =, (, ), ., %, ANS, AC, delete.
 * Default Regular height is 30% taller (73.dp keys, 57.dp utility) for effortless ergonomics.
 */
@Composable
fun CalculatorKeypad(
  onInput: (String) -> Unit,
  onDelete: () -> Unit,
  onClearDisplay: () -> Unit,
  onLongPressAc: (() -> Unit)? = null,
  onLongPressDelete: (() -> Unit)? = null,
  onEquals: () -> Unit,
  onAns: (() -> Unit)? = null,
  ansEnabled: Boolean = true,
  hapticEnabled: Boolean = true,
  soundEnabled: Boolean = true,
  equalsLabel: String = "=",
  keyHeight: Dp = 73.dp,
  utilityHeight: Dp = 57.dp,
  modifier: Modifier = Modifier,
) {
  val spacing = 8.dp
  val numFontSize = if (keyHeight >= 66.dp) 25.sp else 22.sp
  val opFontSize = if (keyHeight >= 66.dp) 28.sp else 24.sp
  val utilFontSize = if (utilityHeight >= 50.dp) 20.sp else 18.sp

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(spacing),
  ) {
    // Top utility row: (  )  %  ANS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      KeyTile(
        label = "(",
        contentDesc = "Open parenthesis",
        tag = "key_lparen",
        bgColor = OledKeyUtility,
        textColor = OledTextSecondary,
        fontSize = utilFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(utilityHeight),
        onClick = { onInput("(") },
      )
      KeyTile(
        label = ")",
        contentDesc = "Close parenthesis",
        tag = "key_rparen",
        bgColor = OledKeyUtility,
        textColor = OledTextSecondary,
        fontSize = utilFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(utilityHeight),
        onClick = { onInput(")") },
      )
      KeyTile(
        label = "%",
        contentDesc = "Percent",
        tag = "key_percent",
        bgColor = OledKeyUtility,
        textColor = AccentAmber,
        fontSize = utilFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(utilityHeight),
        onClick = { onInput("%") },
      )
      KeyTile(
        label = "ANS",
        contentDesc = "Insert last answer",
        tag = "key_ans",
        bgColor = if (ansEnabled) AccentCyan.copy(alpha = 0.14f) else OledKeyUtility.copy(alpha = 0.45f),
        textColor = if (ansEnabled) AccentCyan else OledTextSecondary.copy(alpha = 0.35f),
        fontSize = 15.sp,
        isMono = false,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        enabled = ansEnabled,
        modifier = Modifier.weight(1f).height(utilityHeight),
        onClick = { onAns?.invoke() ?: onInput("ANS") },
      )
    }

    // Row 1: AC  delete  /  *
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      KeyTile(
        label = "AC",
        contentDesc = "Clear display, long press to clear all and save session",
        tag = "key_ac",
        bgColor = AccentRose.copy(alpha = 0.15f),
        textColor = AccentRose,
        fontSize = 20.sp,
        isMono = false,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(keyHeight),
        onClick = onClearDisplay,
        onLongClick = onLongPressAc,
      )
      DeleteKeyTile(
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(keyHeight),
        onClick = onDelete,
        onLongClick = onLongPressDelete,
      )
      KeyTile(
        label = "÷",
        contentDesc = "Divide",
        tag = "key_div",
        bgColor = OledKeyOperator,
        textColor = AccentCyan,
        fontSize = opFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(keyHeight),
        onClick = { onInput("/") },
      )
      KeyTile(
        label = "×",
        contentDesc = "Multiply",
        tag = "key_mul",
        bgColor = OledKeyOperator,
        textColor = AccentCyan,
        fontSize = opFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(keyHeight),
        onClick = { onInput("*") },
      )
    }

    // Row 2: 7  8  9  -
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      NumberKey("7", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
      NumberKey("8", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
      NumberKey("9", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
      KeyTile(
        label = "−",
        contentDesc = "Subtract",
        tag = "key_minus",
        bgColor = OledKeyOperator,
        textColor = AccentCyan,
        fontSize = opFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(keyHeight),
        onClick = { onInput("-") },
      )
    }

    // Row 3: 4  5  6  +
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      NumberKey("4", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
      NumberKey("5", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
      NumberKey("6", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
      KeyTile(
        label = "+",
        contentDesc = "Add",
        tag = "key_plus",
        bgColor = OledKeyOperator,
        textColor = AccentCyan,
        fontSize = opFontSize,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier.weight(1f).height(keyHeight),
        onClick = { onInput("+") },
      )
    }

    // Rows 4 & 5: (1 2 3 / 0 .) on left 3 cols, tall = on right 1 col
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min),
      horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
      Column(
        modifier = Modifier.weight(3f),
        verticalArrangement = Arrangement.spacedBy(spacing),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
          NumberKey("1", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
          NumberKey("2", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
          NumberKey("3", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(1f).height(keyHeight), onInput)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
          NumberKey("0", numFontSize, hapticEnabled, soundEnabled, Modifier.weight(2f).height(keyHeight), onInput)
          KeyTile(
            label = ".",
            contentDesc = "Decimal point",
            tag = "key_dot",
            bgColor = OledKeyNumber,
            textColor = OledTextPrimary,
            fontSize = opFontSize,
            hapticEnabled = hapticEnabled,
            soundEnabled = soundEnabled,
            modifier = Modifier.weight(1f).height(keyHeight),
            onClick = { onInput(".") },
          )
        }
      }

      KeyTile(
        label = equalsLabel,
        contentDesc = "Equals",
        tag = "key_eq",
        bgColor = AccentCyan,
        textColor = OledBlack,
        fontSize = 30.sp,
        isMono = true,
        hapticEnabled = hapticEnabled,
        soundEnabled = soundEnabled,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .heightIn(min = keyHeight * 2 + spacing),
        onClick = onEquals,
      )
    }
  }
}

@Composable
private fun NumberKey(
  digit: String,
  fontSize: TextUnit,
  hapticEnabled: Boolean,
  soundEnabled: Boolean,
  modifier: Modifier,
  onInput: (String) -> Unit,
) {
  KeyTile(
    label = digit,
    contentDesc = digit,
    tag = "key_$digit",
    bgColor = OledKeyNumber,
    textColor = OledTextPrimary,
    fontSize = fontSize,
    hapticEnabled = hapticEnabled,
    soundEnabled = soundEnabled,
    modifier = modifier,
    onClick = { onInput(digit) },
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyTile(
  label: String,
  contentDesc: String,
  tag: String,
  bgColor: Color,
  textColor: Color,
  fontSize: TextUnit,
  hapticEnabled: Boolean,
  soundEnabled: Boolean,
  modifier: Modifier = Modifier,
  isMono: Boolean = true,
  enabled: Boolean = true,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val soundPlayer = LocalSoundEffectPlayer.current
  val shape = RoundedCornerShape(18.dp)
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .clip(shape)
      .background(bgColor, shape)
      .then(
        if (enabled) {
          Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = textColor.copy(alpha = 0.25f)),
            role = Role.Button,
            onClick = {
              soundPlayer?.playClick(soundEnabled)
              if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onClick()
            },
            onLongClick = onLongClick?.let { longAction ->
              {
                soundPlayer?.playClick(soundEnabled)
                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                longAction()
              }
            },
          )
        } else {
          Modifier
        }
      )
      .semantics { this.contentDescription = contentDesc }
      .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = fontSize,
      fontWeight = FontWeight.SemiBold,
      fontFamily = if (isMono) JetBrainsMonoFontFamily else SpaceGroteskFontFamily,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeleteKeyTile(
  hapticEnabled: Boolean,
  soundEnabled: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val soundPlayer = LocalSoundEffectPlayer.current
  val shape = RoundedCornerShape(18.dp)
  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .clip(shape)
      .background(OledKeyUtility, shape)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, color = OledTextPrimary.copy(alpha = 0.2f)),
        role = Role.Button,
        onClick = {
          soundPlayer?.playClick(soundEnabled)
          if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
        },
        onLongClick = onLongClick?.let { longAction ->
          {
            soundPlayer?.playClick(soundEnabled)
            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            longAction()
          }
        },
      )
      .semantics { contentDescription = "Delete" }
      .testTag("key_delete"),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Outlined.Backspace,
      contentDescription = "Delete",
      tint = OledTextPrimary,
      modifier = Modifier.size(24.dp),
    )
  }
}
