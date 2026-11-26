package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Clickit – Crisp Commercial Light Palette (Royal Cobalt & Clean Slate Neutrals)
val ClickitBrandPrimary = Color(0xFF2563EB)    // Primary brand Royal Cobalt
val ClickitBrandDark = Color(0xFF1E40AF)       // Deep Royal Indigo for pressed/high-contrast states
val ClickitBrandTint = Color(0xFFEFF6FF)       // Soft ice-blue tint for selected states & chips

// Neutral Surfaces & Text
val ClickitLightBg = Color(0xFFF8FAFC)         // Clean alabaster app canvas
val ClickitLightSurface = Color(0xFFFFFFFF)    // Pure white cards, headers, bottom bars
val ClickitLightSurfaceVar = Color(0xFFF1F5F9) // Soft slate grey for inputs, chips & tiles
val ClickitTextPrimary = Color(0xFF0F172A)     // Rich slate-black readable text
val ClickitTextSecondary = Color(0xFF64748B)   // Balanced muted slate captions & metadata
val ClickitBorder = Color(0xFFE2E8F0)          // Crisp hairline borders
val ClickitBorderStrong = Color(0xFFCBD5E1)    // Subtle stronger dividers

// Accessible Semantic Status Colors
val ClickitSuccess = Color(0xFF16A34A)         // Accessible green (Free >5kg, verified, done)
val ClickitSuccessContainer = Color(0xFFF0FDF4)
val ClickitWarning = Color(0xFFD97706)         // Accessible amber (Quotes, Urgent, Delay)
val ClickitWarningContainer = Color(0xFFFFFBEB)
val ClickitCrimson = Color(0xFFDC2626)         // Accessible error / destructive
val ClickitDangerContainer = Color(0xFFFEF2F2)
val ClickitInfo = ClickitBrandPrimary
val ClickitInfoContainer = ClickitBrandTint

// Compatibility aliases mapped cleanly to the Royal Cobalt commercial palette
val ClickitBrandTeal = ClickitBrandPrimary
val ClickitBrandTealDark = ClickitBrandDark
val ClickitBrandTealTint = ClickitBrandTint
val ClickitMidnight = ClickitLightBg
val ClickitSlateDeep = ClickitLightSurface
val ClickitSlate = ClickitLightSurfaceVar
val ClickitPeach = ClickitBrandPrimary
val ClickitPeachSoft = ClickitBrandDark
val ClickitBurgundy = ClickitBrandTint
val ClickitIvory = ClickitTextPrimary
val ClickitMutedText = ClickitTextSecondary
val ClickitSubtleLine = ClickitBorder
