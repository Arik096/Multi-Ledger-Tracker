package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.MathExpressionEvaluator

/**
 * Calculator Entry Field that transforms any numeric / amount entry field into an active calculator.
 *
 * Features:
 * - Real-time expression evaluation (e.g. typing or entering "1500 + 350 * 2" computes live preview "= 2200").
 * - Interactive Built-in Modern Calculator Keypad (toggleable or docked), containing numbers 0-9,
 *   operations (+, −, ×, ÷, %, .), Backspace, Clear, and "=" (evaluate & apply).
 * - Live evaluation preview badge right under/inside the field.
 * - Allows direct keyboard typing as well as touch calculator keypad entry.
 * - Auto-evaluates on blur, enter, or "=" button.
 */
@Composable
fun CalculatorEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    placeholder: String = "0.00",
    prefix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    testTag: String = "calculator_entry_field",
    showKeypadInitially: Boolean = false,
    onDone: (() -> Unit)? = null
) {
    var isKeypadVisible by remember { mutableStateOf(showKeypadInitially) }
    val focusManager = LocalFocusManager.current

    // Live calculation evaluation
    val liveResult by remember(value) {
        derivedStateOf {
            if (value.contains("+") || value.contains("-") || value.contains("×") ||
                value.contains("*") || value.contains("÷") || value.contains("/") ||
                value.contains("%")
            ) {
                MathExpressionEvaluator.evaluate(value)?.let {
                    MathExpressionEvaluator.formatResult(it)
                }
            } else {
                null
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                // Allow digits, math symbols, spaces and dots
                val filtered = input.filter { c ->
                    c.isDigit() || c in "+-*/×÷%(). " || c == '-'
                }
                onValueChange(filtered)
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "CALC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            },
            placeholder = { Text(placeholder) },
            prefix = prefix,
            isError = isError,
            supportingText = {
                if (supportingText != null) {
                    supportingText()
                } else if (liveResult != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Live Result: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "= $liveResult",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // If an expression is present, show instant evaluate button
                    if (liveResult != null) {
                        IconButton(
                            onClick = {
                                onValueChange(liveResult ?: value)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("${testTag}_eval_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "=",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Keypad toggle button
                    IconButton(
                        onClick = { isKeypadVisible = !isKeypadVisible },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("${testTag}_calc_toggle")
                    ) {
                        Icon(
                            imageVector = if (isKeypadVisible) Icons.Default.KeyboardHide else Icons.Default.Calculate,
                            contentDescription = if (isKeypadVisible) "Hide Calculator Keypad" else "Open Calculator Keypad",
                            tint = if (isKeypadVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        // Integrated interactive Calculator Keypad
        AnimatedVisibility(
            visible = isKeypadVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            CalculatorKeypad(
                currentExpression = value,
                liveResult = liveResult,
                onAppend = { char ->
                    onValueChange(value + char)
                },
                onBackspace = {
                    if (value.isNotEmpty()) {
                        onValueChange(value.dropLast(1))
                    }
                },
                onClear = {
                    onValueChange("")
                },
                onEquals = {
                    val evaluated = liveResult ?: MathExpressionEvaluator.evaluate(value)?.let {
                        MathExpressionEvaluator.formatResult(it)
                    }
                    if (evaluated != null) {
                        onValueChange(evaluated)
                    }
                },
                onDone = {
                    val evaluated = liveResult ?: MathExpressionEvaluator.evaluate(value)?.let {
                        MathExpressionEvaluator.formatResult(it)
                    }
                    if (evaluated != null) {
                        onValueChange(evaluated)
                    }
                    isKeypadVisible = false
                    focusManager.clearFocus()
                    onDone?.invoke()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

/**
 * Modern tactile Calculator Keypad designed specifically for financial and amount entry.
 */
@Composable
fun CalculatorKeypad(
    currentExpression: String,
    liveResult: String?,
    onAppend: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("calculator_keypad_surface"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Expression & Live computation monitor row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentExpression.isBlank()) "0" else currentExpression,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (liveResult != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "= $liveResult",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Row 1: C (Clear), ( ), % (Percent), ÷ (Divide)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorKey(
                    label = "C",
                    onClick = onClear,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                CalculatorKey(
                    label = "( )",
                    onClick = {
                        val openCount = currentExpression.count { it == '(' }
                        val closeCount = currentExpression.count { it == ')' }
                        if (openCount > closeCount && currentExpression.lastOrNull()?.isDigit() == true) {
                            onAppend(")")
                        } else {
                            onAppend("(")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                CalculatorKey(
                    label = "%",
                    onClick = { onAppend("%") },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                CalculatorKey(
                    label = "÷",
                    onClick = { onAppend(" / ") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    isOperator = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: 7, 8, 9, × (Multiply)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorKey(label = "7", onClick = { onAppend("7") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = "8", onClick = { onAppend("8") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = "9", onClick = { onAppend("9") }, modifier = Modifier.weight(1f))
                CalculatorKey(
                    label = "×",
                    onClick = { onAppend(" * ") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    isOperator = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: 4, 5, 6, − (Minus)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorKey(label = "4", onClick = { onAppend("4") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = "5", onClick = { onAppend("5") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = "6", onClick = { onAppend("6") }, modifier = Modifier.weight(1f))
                CalculatorKey(
                    label = "−",
                    onClick = { onAppend(" - ") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    isOperator = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: 1, 2, 3, + (Plus)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorKey(label = "1", onClick = { onAppend("1") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = "2", onClick = { onAppend("2") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = "3", onClick = { onAppend("3") }, modifier = Modifier.weight(1f))
                CalculatorKey(
                    label = "+",
                    onClick = { onAppend(" + ") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    isOperator = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 5: 0, . (Dot), ⌫ (Backspace), = / Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalculatorKey(label = "0", onClick = { onAppend("0") }, modifier = Modifier.weight(1f))
                CalculatorKey(label = ".", onClick = { onAppend(".") }, modifier = Modifier.weight(1f))
                CalculatorKey(
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    onClick = onBackspace,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                // Evaluate and Apply / Done Button
                CalculatorKey(
                    label = "=",
                    onClick = {
                        onEquals()
                        onDone()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    isOperator = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CalculatorKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isOperator: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = if (isOperator) 2.dp else 0.5.dp,
        modifier = modifier.height(44.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (label != null) {
                Text(
                    text = label,
                    fontSize = if (isOperator) 18.sp else 16.sp,
                    fontWeight = if (isOperator) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
