import 'package:flutter/material.dart';

/// Design tokens aligned with the React app's CSS custom properties.
/// Single source of truth for colors, shadows, radii, and common styles.

// -- Colors -------------------------------------------------------------------

const Color appAccent = Color(0xFF0D66D0);
const Color appAccentLight = Color(0xFFE8F1FC);
const Color appAccentDark = Color(0xFF0A4F9E);
const Color appBg = Color(0xFFF4F6F9);
const Color appSurface = Color(0xFFFFFFFF);
const Color appText = Color(0xFF1D1D1F);
const Color appTextSecondary = Color(0xFF6E6E73);
const Color appBorder = Color(0xFFE5E5EA);
const Color appTextDisabled = Color(0xFFBBBBC0);
const Color appDanger = Color(0xFFEF5350);

// -- Gradients ----------------------------------------------------------------

const LinearGradient imageGradient = LinearGradient(
  begin: Alignment.topCenter,
  end: Alignment.bottomCenter,
  colors: [Color(0xFFF8FAFC), Color(0xFFEEF2F7)],
);

const LinearGradient headerGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [Color(0xFF0D66D0), Color(0xFF1A8CFF)],
);

const LinearGradient loginGradient = LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [Color(0xFF0D66D0), Color(0xFF1A8CFF), Color(0xFF4DA6FF)],
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

const List<BoxShadow> headerShadow = [
  BoxShadow(color: Color(0x4D0D66D0), blurRadius: 8, offset: Offset(0, 2)),
];

// -- Breakpoints --------------------------------------------------------------

const double breakpointSm = 576.0;
const double breakpointMd = 768.0;
const double breakpointLg = 900.0;

// -- Radii --------------------------------------------------------------------

const double radiusLg = 12.0;
const double radiusSm = 8.0;
const double radiusRound = 20.0;

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
  foregroundColor: Colors.white,
  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
  textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
);

// -- Shared widgets -----------------------------------------------------------

const Widget _defaultImageErrorPlaceholder = Icon(Icons.image_not_supported, size: 48, color: appTextDisabled);

/// Fallback widget shown when an image fails to load.
Widget imageErrorPlaceholder({double size = 48}) =>
    size == 48 ? _defaultImageErrorPlaceholder : Icon(Icons.image_not_supported, size: size, color: appTextDisabled);
