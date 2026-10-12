package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Soft Color System for CashLedger
// 5 Soft Color Schemes - not too dark, each strictly providing 5 distinct shades
// Cards are clearly and easily differentiated from the canvas background
// =========================================================================

// --- 1. Soft Sage Mint ---
// Shade 1: Primary Accent
val SoftSagePrimary = Color(0xFF10B981)          // Vibrant soft mint
// Shade 2: Container / Badge Tint
val SoftSageContainerLight = Color(0xFFD1FAE5)    // Gentle pastel mint
val SoftSageContainerDark = Color(0xFF1B4D3E)     // Soft forest container
// Shade 3: Card Surface (clearly differentiated from canvas background)
val SoftSageCardLight = Color(0xFFFFFFFF)         // Clean card white
val SoftSageCardDark = Color(0xFF2C3646)          // Distinct elevated card slate
// Shade 4: Elevated Container / Header
val SoftSageElevatedLight = Color(0xFFE8F5ED)     // Tonal elevated card
val SoftSageElevatedDark = Color(0xFF384456)      // High-tonal card surface
// Shade 5: Canvas Background (soft, soothing, never harsh pitch-black)
val SoftSageBgLight = Color(0xFFF1F5F2)           // Soft calming sage canvas
val SoftSageBgDark = Color(0xFF202732)            // Soft slate-graphite dark canvas

// --- 2. Soft Iris Indigo ---
// Shade 1: Primary Accent
val SoftIndigoPrimary = Color(0xFF6366F1)
// Shade 2: Container / Badge Tint
val SoftIndigoContainerLight = Color(0xFFE0E7FF)
val SoftIndigoContainerDark = Color(0xFF353468)
// Shade 3: Card Surface
val SoftIndigoCardLight = Color(0xFFFFFFFF)
val SoftIndigoCardDark = Color(0xFF2B2F48)
// Shade 4: Elevated Container / Header
val SoftIndigoElevatedLight = Color(0xFFEEF2FF)
val SoftIndigoElevatedDark = Color(0xFF373C5A)
// Shade 5: Canvas Background
val SoftIndigoBgLight = Color(0xFFF3F4FA)
val SoftIndigoBgDark = Color(0xFF202334)

// --- 3. Soft Coral Rose ---
// Shade 1: Primary Accent
val SoftCoralPrimary = Color(0xFFF43F5E)
// Shade 2: Container / Badge Tint
val SoftCoralContainerLight = Color(0xFFFFE4E6)
val SoftCoralContainerDark = Color(0xFF632535)
// Shade 3: Card Surface
val SoftCoralCardLight = Color(0xFFFFFFFF)
val SoftCoralCardDark = Color(0xFF352B31)
// Shade 4: Elevated Container / Header
val SoftCoralElevatedLight = Color(0xFFFFF0F2)
val SoftCoralElevatedDark = Color(0xFF44383F)
// Shade 5: Canvas Background
val SoftCoralBgLight = Color(0xFFFAF4F5)
val SoftCoralBgDark = Color(0xFF262024)

// --- 4. Soft Slate Blue ---
// Shade 1: Primary Accent
val SoftSlatePrimary = Color(0xFF0EA5E9)
// Shade 2: Container / Badge Tint
val SoftSlateContainerLight = Color(0xFFE0F2FE)
val SoftSlateContainerDark = Color(0xFF1B4E6B)
// Shade 3: Card Surface
val SoftSlateCardLight = Color(0xFFFFFFFF)
val SoftSlateCardDark = Color(0xFF2B3445)
// Shade 4: Elevated Container / Header
val SoftSlateElevatedLight = Color(0xFFEDF4FA)
val SoftSlateElevatedDark = Color(0xFF374257)
// Shade 5: Canvas Background
val SoftSlateBgLight = Color(0xFFF1F5F9)
val SoftSlateBgDark = Color(0xFF1F2633)

// --- 5. Soft Amber Honey ---
// Shade 1: Primary Accent
val SoftAmberPrimary = Color(0xFFD97706)
// Shade 2: Container / Badge Tint
val SoftAmberContainerLight = Color(0xFFFEF3C7)
val SoftAmberContainerDark = Color(0xFF5C3B18)
// Shade 3: Card Surface
val SoftAmberCardLight = Color(0xFFFFFFFF)
val SoftAmberCardDark = Color(0xFF332D24)
// Shade 4: Elevated Container / Header
val SoftAmberElevatedLight = Color(0xFFFFF9E8)
val SoftAmberElevatedDark = Color(0xFF423B30)
// Shade 5: Canvas Background
val SoftAmberBgLight = Color(0xFFF9F6EE)
val SoftAmberBgDark = Color(0xFF25221B)

// Neutral text & outline tokens
val SoftOutlineLight = Color(0xFFE2E8F0)
val SoftOutlineDark = Color(0xFF445064)

val SoftBorderLight = Color(0xFFCBD5E1)
val SoftBorderDark = Color(0xFF384355)

val SoftTextPrimaryLight = Color(0xFF0F172A)
val SoftTextPrimaryDark = Color(0xFFF8FAFC)

val SoftTextSecondaryLight = Color(0xFF475569)
val SoftTextSecondaryDark = Color(0xFF94A3B8)

// Cash Transaction Status Colors
val CashInGreen = Color(0xFF10B981)
val CashInGreenDark = Color(0xFF34D399)

val CashOutRed = Color(0xFFEF4444)
val CashOutRedDark = Color(0xFFF87171)

// Chart Specific Color Palette (Accessible, High-Contrast Harmony)
val ChartTeal = Color(0xFF0D9488)
val ChartAmber = Color(0xFFF59E0B)
val ChartIndigo = Color(0xFF6366F1)
val ChartPink = Color(0xFFEC4899)
val ChartEmerald = Color(0xFF10B981)
val ChartOrange = Color(0xFFF97316)
val ChartCyan = Color(0xFF06B6D4)
val ChartPurple = Color(0xFF8B5CF6)
val ChartBlue = Color(0xFF3B82F6)
val ChartRose = Color(0xFFF43F5E)

val CategoryPalette = listOf(
    ChartTeal,
    ChartAmber,
    ChartIndigo,
    ChartPink,
    ChartEmerald,
    ChartOrange,
    ChartCyan,
    ChartPurple,
    ChartBlue,
    ChartRose
)

val CategoryColors: List<Color> = CategoryPalette
