import 'package:flutter/material.dart';

import '../design_tokens.dart';

/// A card container that elevates and shifts up on hover.
///
/// Wraps any child with a consistent hover animation matching the React
/// `translateY(-3px) + box-shadow` pattern for clickable cards.
class HoverCard extends StatefulWidget {
  final Widget child;
  final VoidCallback? onTap;
  final BorderRadius borderRadius;

  const HoverCard({
    super.key,
    required this.child,
    this.onTap,
    this.borderRadius = const BorderRadius.all(Radius.circular(radiusLg)),
  });

  @override
  State<HoverCard> createState() => _HoverCardState();
}

class _HoverCardState extends State<HoverCard> {
  static final _identityMatrix = Matrix4.identity();
  static final _hoverMatrix = Matrix4.identity()
    ..translateByDouble(0.0, -3.0, 0.0, 1.0);

  bool _hovered = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      cursor: SystemMouseCursors.click,
      onEnter: (_) => setState(() => _hovered = true),
      onExit: (_) => setState(() => _hovered = false),
      child: GestureDetector(
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOut,
          transform: _hovered ? _hoverMatrix : _identityMatrix,
          decoration: BoxDecoration(
            color: appSurface,
            borderRadius: widget.borderRadius,
            border: Border.all(color: appBorder),
            boxShadow: _hovered ? cardShadowLg : cardShadowSm,
          ),
          clipBehavior: Clip.antiAlias,
          child: widget.child,
        ),
      ),
    );
  }
}
