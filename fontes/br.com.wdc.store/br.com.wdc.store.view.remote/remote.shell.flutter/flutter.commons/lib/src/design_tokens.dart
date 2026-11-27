import 'package:material_ui/material_ui.dart';

import 'generated/store_tokens.dart';

/// The shell's theme: colors and radii come from the store's design tokens ([StoreTokens]);
/// shadows, breakpoints and the shared styles are built on them.

// -- Colors -------------------------------------------------------------------

const Color appAccent = StoreTokens.primary;
const Color appAccentLight = StoreTokens.primaryLight;
const Color appAccentDark = StoreTokens.primaryDark;
const Color appOnPrimary = StoreTokens.onPrimary;
const Color appBg = StoreTokens.background;
const Color appSurface = StoreTokens.surface;
const Color appText = StoreTokens.text;
const Color appTextSecondary = StoreTokens.textSecondary;
const Color appBorder = StoreTokens.border;
const Color appTextDisabled = StoreTokens.textMuted;
const Color appDanger = StoreTokens.error;

// -- Gradients ----------------------------------------------------------------

const LinearGradient imageGradient = LinearGradient(
  begin: Alignment.topCenter,
  end: Alignment.bottomCenter,
  colors: [StoreTokens.surfaceMuted, StoreTokens.surfaceSubtle],
);

const LinearGradient headerGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [StoreTokens.primary, StoreTokens.accent],
);

const LinearGradient loginGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [StoreTokens.primary, StoreTokens.accent, StoreTokens.accentSoft],
  stops: [0.0, 0.4, 1.0],
);

// -- Shadows ------------------------------------------------------------------

const List<BoxShadow> cardShadowSm = [
  BoxShadow(color: Color(0x0F000000), blurRadius: 3, offset: Offset(0, 1)),
  BoxShadow(color: Color(0x0A000000), blurRadius: 2, offset: Offset(0, 1)),
];

const List<BoxShadow> cardShadowLg = [
  BoxShadow(color: Color(0x1A000000), blurRadius: 12, offset: Offset(0, 8)),
  BoxShadow(color: Color(0x0D000000), blurRadius: 4, offset: Offset(0, 2)),
];

/// The primary color at 30% opacity under the header.
final List<BoxShadow> headerShadow = [
  BoxShadow(color: StoreTokens.primary.withValues(alpha: 0.3), blurRadius: 8, offset: const Offset(0, 2)),
];

// -- Breakpoints --------------------------------------------------------------

const double breakpointSm = 576.0;
const double breakpointMd = 768.0;
const double breakpointLg = 900.0;

// -- Radii --------------------------------------------------------------------

/// Cards, panels.
const double radiusLg = StoreTokens.radiusMd;

/// Inputs, small buttons.
const double radiusSm = StoreTokens.radiusSm;

/// Pill buttons, large panels.
const double radiusRound = StoreTokens.radiusLg;

// -- Card decoration ----------------------------------------------------------

/// Standard page-level card decoration (white, bordered, rounded, shadow).
final BoxDecoration cardDecoration = BoxDecoration(
  color: appSurface,
  borderRadius: BorderRadius.circular(radiusLg),
  border: Border.all(color: appBorder),
  boxShadow: cardShadowSm,
);

// -- Button style -------------------------------------------------------------

/// Accent-colored pill button style used across views.
final ButtonStyle accentButtonStyle = FilledButton.styleFrom(
  backgroundColor: appAccent,
  foregroundColor: appOnPrimary,
  padding: const EdgeInsets.symmetric(horizontal: StoreTokens.spaceXl, vertical: 14),
  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusRound)),
  textStyle: const TextStyle(fontSize: 15, fontWeight: StoreTokens.fontWeightSemibold),
);

// -- Shared widgets -----------------------------------------------------------

const Widget _defaultImageErrorPlaceholder = Icon(Icons.image_not_supported, size: 48, color: appTextDisabled);

/// Fallback widget shown when an image fails to load.
Widget imageErrorPlaceholder({double size = 48}) =>
    size == 48 ? _defaultImageErrorPlaceholder : Icon(Icons.image_not_supported, size: size, color: appTextDisabled);
