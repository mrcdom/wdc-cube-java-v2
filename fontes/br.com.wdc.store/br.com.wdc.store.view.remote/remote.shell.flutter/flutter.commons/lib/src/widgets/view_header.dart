import 'package:flutter/material.dart';

import '../design_tokens.dart';

/// Standard view header with a colored icon badge + title + subtitle.
class ViewHeader extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;

  const ViewHeader({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: appAccentLight,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: appAccent),
        ),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
            Text(subtitle, style: const TextStyle(fontSize: 12, color: appTextSecondary)),
          ],
        ),
      ],
    );
  }
}
