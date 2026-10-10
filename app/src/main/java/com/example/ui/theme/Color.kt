package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Modern Soft Color System for CashBook & Financial Management
// Strictly provides 5 distinct shades per palette for optimal UI distinction
// =========================================================================

// --- 1. Soft Sage Mint (Fintech Mint & Fresh Sage) ---
// Shade 1: Primary Accent
val SoftSagePrimary = Color(0xFF10B981)       // Vibrant soft mint
// Shade 2: Container / Badge Tint
val SoftSageContainerLight = Color(0xFFD1FAE5) // Gentle pastel mint
val SoftSageContainerDark = Color(0xFF064E3B)  // Deep emerald container
// Shade 3: Card Surface (distinct from canvas background)
val SoftSageCardLight = Color(0xFFFFFFFF)      // Pure crisp white card
val SoftSageCardDark = Color(0xFF242C39)       // Elevated graphite card (clearly distinct from canvas)
// Shade 4: Elevated Container / Header
val SoftSageElevatedLight = Color(0xFFE6F4EA)  // Soft sage elevated surface
val SoftSageElevatedDark = Color(0xFF2E3848)   // High-tonal surface
// Shade 5: Canvas Background (soft, never harsh pitch-black)
val SoftSageBgLight = Color(0xFFF4F7F5)        // Soft calming sage white
val SoftSageBgDark = Color(0xFF161B24)         // Soothing graphite dark canvas

// --- 2. Soft Iris Indigo (Modern Periwinkle & Electric Indigo) ---
// Shade 1: Primary Accent
val SoftIndigoPrimary = Color(0xFF6366F1)
// Shade 2: Container / Badge Tint
val SoftIndigoContainerLight = Color(0xFFE0E7FF)
val SoftIndigoContainerDark = Color(0xFF312E81)
// Shade 3: Card Surface
val SoftIndigoCardLight = Color(0xFFFFFFFF)
val SoftIndigoCardDark = Color(0xFF24283D)
// Shade 4: Elevated Container / Header
val SoftIndigoElevatedLight = Color(0xFFEEF2FF)
val SoftIndigoElevatedDark = Color(0xFF2E344F)
// Shade 5: Canvas Background
val SoftIndigoBgLight = Color(0xFFF5F6FB)
val SoftIndigoBgDark = Color(0xFF171A29)

// --- 3. Soft Coral Rose (Warm Peach & Velvet Rose) ---
// Shade 1: Primary Accent
val SoftCoralPrimary = Color(0xFFF43F5E)
// Shade 2: Container / Badge Tint
val SoftCoralContainerLight = Color(0xFFFFE4E6)
val SoftCoralContainerDark = Color(0xFF881337)
// Shade 3: Card Surface
val SoftCoralCardLight = Color(0xFFFFFFFF)
val SoftCoralCardDark = Color(0xFF2D2328)
// Shade 4: Elevated Container / Header
val SoftCoralElevatedLight = Color(0xFFFFF1F2)
val SoftCoralElevatedDark = Color(0xFF3B2D34)
// Shade 5: Canvas Background
val SoftCoralBgLight = Color(0xFFFAF5F6)
val SoftCoralBgDark = Color(0xFF1D171A)

// --- 4. Soft Slate Blue (Titanium Slate & Cool Sky) ---
// Shade 1: Primary Accent
val SoftSlatePrimary = Color(0xFF0EA5E9)
// Shade 2: Container / Badge Tint
val SoftSlateContainerLight = Color(0xFFE0F2FE)
val SoftSlateContainerDark = Color(0xFF0369A1)
// Shade 3: Card Surface
val SoftSlateCardLight = Color(0xFFFFFFFF)
val SoftSlateCardDark = Color(0xFF222938)
// Shade 4: Elevated Container / Header
val SoftSlateElevatedLight = Color(0xFFEDF2F7)
val SoftSlateElevatedDark = Color(0xFF2C3548)
// Shade 5: Canvas Background
val SoftSlateBgLight = Color(0xFFF3F6F9)
val SoftSlateBgDark = Color(0xFF161A24)

// --- 5. Soft Amber Honey (Warm Honey & Golden Sand) ---
// Shade 1: Primary Accent
val SoftAmberPrimary = Color(0xFFD97706)
// Shade 2: Container / Badge Tint
val SoftAmberContainerLight = Color(0xFFFEF3C7)
val SoftAmberContainerDark = Color(0xFF78350F)
// Shade 3: Card Surface (distinct from canvas background)
val SoftAmberCardLight = Color(0xFFFFFFFF)
val SoftAmberCardDark = Color(0xFF28251E)
// Shade 4: Elevated Container / Header
val SoftAmberElevatedLight = Color(0xFFFFFBEB)
val SoftAmberElevatedDark = Color(0xFF353026)
// Shade 5: Canvas Background
val SoftAmberBgLight = Color(0xFFFAF7F0)
val SoftAmberBgDark = Color(0xFF1B1813)

// Neutral text & outline tokens
val SoftOutlineLight = Color(0xFFE2E8F0)
val SoftOutlineDark = Color(0xFF384357)

val SoftBorderLight = Color(0xFFCBD5E1)
val SoftBorderDark = Color(0xFF2D3647)

val SoftTextPrimaryLight = Color(0xFF0F172A)
val SoftTextPrimaryDark = Color(0xFFF8FAFC)

val SoftTextSecondaryLight = Color(0xFF475569)
val SoftTextSecondaryDark = Color(0xFF94A3B8)

val SoftTextMutedLight = Color(0xFF94A3B8)
val SoftTextMutedDark = Color(0xFF64748B)

// Transaction semantic indicators (Income / Expense)
val CashInGreen = Color(0xFF10B981)       // Modern emerald mint
val CashInLight = Color(0xFFD1FAE5)       // Pastel mint
val CashInDark = Color(0xFF047857)
val CashInPill = Color(0xFF064E3B)

val CashOutRed = Color(0xFFF43F5E)        // Modern rose
val CashOutLight = Color(0xFFFFE4E6)      // Pastel rose
val CashOutDark = Color(0xFFBE123C)
val CashOutPill = Color(0xFF4C0519)

// Accent highlights for charts and categoricals
val ChartBlue = Color(0xFF0EA5E9)
val ChartAmber = Color(0xFFF59E0B)
val ChartPurple = Color(0xFF8B5CF6)
val ChartPink = Color(0xFFF43F5E)
val ChartTeal = Color(0xFF14B8A6)
val ChartIndigo = Color(0xFF6366F1)
val ChartOrange = Color(0xFFF97316)
val ChartCyan = Color(0xFF06B6D4)

val CategoryColors = listOf(
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFFF43F5E),
    Color(0xFF14B8A6),
    Color(0xFFF97316),
    Color(0xFF6366F1),
    Color(0xFF06B6D4),
    Color(0xFF84CC16),
    Color(0xFFD946EF),
    Color(0xFF64748B)
)
