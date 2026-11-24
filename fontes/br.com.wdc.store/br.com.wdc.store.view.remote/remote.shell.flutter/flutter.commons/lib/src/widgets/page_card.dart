import 'package:flutter/material.dart';

import '../design_tokens.dart';

/// A page-level scrollable card layout used by detail views (cart, receipt, product).
///
/// Structure: colored background → scroll → centered ConstrainedBox → optional white card.
class PageCard extends StatelessWidget {
  final List<Widget> children;
  final double maxWidth;
  final CrossAxisAlignment crossAxisAlignment;
  final bool useCardDecoration;

  const PageCard({
    super.key,
    required this.children,
    this.maxWidth = 900,
    this.crossAxisAlignment = CrossAxisAlignment.stretch,
    this.useCardDecoration = true,
  });

  @override
  Widget build(BuildContext context) {
    final content = Column(
      crossAxisAlignment: crossAxisAlignment,
      children: children,
    );

    return Container(
      color: appBg,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Center(
          child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: maxWidth),
            child: useCardDecoration
                ? Container(
                    padding: const EdgeInsets.all(28),
                    decoration: cardDecoration,
                    child: content,
                  )
                : content,
          ),
        ),
      ),
    );
  }
}
