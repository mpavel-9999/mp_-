package ru.mpavel.osmmap.core.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import ru.mpavel.osmmap.data.Scheme
import ru.mpavel.osmmap.data.Template

/** Цвета палитры сверх ролей Material: зона, статусы, карта, второстепенный текст. */
@Immutable
data class MpColors(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val text: Color,
    val textMuted: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val background: Color,
    val border: Color,
    val zone: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val dark: Boolean,
)

/** Палитра A_LIGHT - пока конфигурация не загружена (экран входа). */
private val DefaultPalette = mapOf(
    "accent" to "#0F766E", "onAccent" to "#FFFFFF", "accentSoft" to "#F0F7F5", "text" to "#12201D",
    "textMuted" to "#4B5A56", "surface" to "#FFFFFF", "surfaceAlt" to "#F4F6F5", "background" to "#EEF1EC",
    "border" to "#E3E8E6", "zone" to "#0F766E", "success" to "#166534", "warning" to "#B45309", "danger" to "#B91C1C",
)

fun mpColors(scheme: Scheme?): MpColors {
    val palette = scheme?.colors.orEmpty()
    fun color(key: String) = Color(parseHexColor(palette[key]) ?: parseHexColor(DefaultPalette.getValue(key))!!)
    return MpColors(
        accent = color("accent"), onAccent = color("onAccent"), accentSoft = color("accentSoft"),
        text = color("text"), textMuted = color("textMuted"), surface = color("surface"), surfaceAlt = color("surfaceAlt"),
        background = color("background"), border = color("border"), zone = color("zone"),
        success = color("success"), warning = color("warning"), danger = color("danger"),
        dark = scheme?.dark ?: false,
    )
}

/** Формы шаблона (template.shape): чипы, кнопки, карточки и шторки. */
@Immutable
data class MpShapes(val chip: RoundedCornerShape, val button: RoundedCornerShape, val card: RoundedCornerShape)

/** Шрифты шаблона: основной и моноширинный для цифр (шаблон C). */
@Immutable
data class MpFonts(val main: FontFamily, val mono: FontFamily)

val LocalMpColors = staticCompositionLocalOf { mpColors(null) }
val LocalMpShapes = staticCompositionLocalOf {
    MpShapes(RoundedCornerShape(16.dp), RoundedCornerShape(20.dp), RoundedCornerShape(16.dp))
}
val LocalMpFonts = staticCompositionLocalOf { MpFonts(FontFamily.Default, FontFamily.Monospace) }
val LocalTemplate = staticCompositionLocalOf { Template() }

object Mp {
    val colors: MpColors @Composable get() = LocalMpColors.current
    val shapes: MpShapes @Composable get() = LocalMpShapes.current
    val fonts: MpFonts @Composable get() = LocalMpFonts.current
    val template: Template @Composable get() = LocalTemplate.current
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = androidx.compose.ui.text.googlefonts.R.array.com_google_android_gms_fonts_certs,
)

/** Загружаемый шрифт Google; без Google Play - системный (fallback). */
private fun googleFamily(name: String?, fallback: FontFamily): FontFamily {
    if (name.isNullOrBlank()) {
        return fallback
    }
    val font = GoogleFont(name)
    return FontFamily(
        Font(font, fontProvider, FontWeight.Normal),
        Font(font, fontProvider, FontWeight.Medium),
        Font(font, fontProvider, FontWeight.SemiBold),
        Font(font, fontProvider, FontWeight.Bold),
    )
}

private fun Typography.withFamily(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

/**
 * Тема из конфигурации: палитра схемы -> роли Material 3, шрифт и формы шаблона.
 * Смена схемы или шаблона меняет параметры - интерфейс перестраивается без перезапуска.
 */
@Composable
fun MpTheme(scheme: Scheme?, template: Template?, content: @Composable () -> Unit) {
    val t = template ?: Template()
    val colors = remember(scheme) { mpColors(scheme) }
    val fonts = remember(t.font) {
        MpFonts(googleFamily(t.font.family, FontFamily.Default), googleFamily(t.font.mono ?: t.font.family, FontFamily.Monospace))
    }
    val shapes = remember(t.shape) {
        MpShapes(
            chip = RoundedCornerShape(t.shape.chip.dp),
            button = RoundedCornerShape(t.shape.button.dp),
            card = RoundedCornerShape((t.shape.corner / 2).coerceAtLeast(4).dp),
        )
    }
    val colorScheme = if (colors.dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, primaryContainer = colors.accentSoft, onPrimaryContainer = colors.text,
            secondary = colors.accent, onSecondary = colors.onAccent, secondaryContainer = colors.accentSoft, onSecondaryContainer = colors.text,
            background = colors.background, onBackground = colors.text, surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceAlt, onSurfaceVariant = colors.textMuted, surfaceContainer = colors.surfaceAlt,
            surfaceContainerHigh = colors.surfaceAlt, surfaceContainerLow = colors.surface, outline = colors.border,
            outlineVariant = colors.border, error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, primaryContainer = colors.accentSoft, onPrimaryContainer = colors.text,
            secondary = colors.accent, onSecondary = colors.onAccent, secondaryContainer = colors.accentSoft, onSecondaryContainer = colors.text,
            background = colors.background, onBackground = colors.text, surface = colors.surface, onSurface = colors.text,
            surfaceVariant = colors.surfaceAlt, onSurfaceVariant = colors.textMuted, surfaceContainer = colors.surfaceAlt,
            surfaceContainerHigh = colors.surfaceAlt, surfaceContainerLow = colors.surface, outline = colors.border,
            outlineVariant = colors.border, error = colors.danger,
        )
    }
    // Значки строки состояния и навигации - светлые на тёмной схеме, тёмные на светлой
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !colors.dark
                isAppearanceLightNavigationBars = !colors.dark
            }
        }
    }
    val typography = remember(fonts) { Typography().withFamily(fonts.main) }
    val materialShapes = remember(shapes) {
        Shapes(small = shapes.chip, medium = shapes.card, large = shapes.card, extraLarge = RoundedCornerShape(t.shape.corner.dp))
    }
    CompositionLocalProvider(
        LocalMpColors provides colors,
        LocalMpShapes provides shapes,
        LocalMpFonts provides fonts,
        LocalTemplate provides t,
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = materialShapes, content = content)
    }
}
