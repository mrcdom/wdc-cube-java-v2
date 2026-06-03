import 'package:flutter/material.dart';

/// A cross-platform widget that renders simple HTML as styled text.
/// Supports: <b>, <strong>, <i>, <em>, <u>, <br>, <p>, <ul>, <li>, plain text.
class HtmlText extends StatelessWidget {
  final String html;
  final TextStyle? style;

  const HtmlText({super.key, required this.html, this.style});

  @override
  Widget build(BuildContext context) {
    final baseStyle = style ?? const TextStyle(fontSize: 14, height: 1.5, color: Color(0xFF333333));
    final spans = _parse(html);
    return Text.rich(
      TextSpan(children: spans, style: baseStyle),
    );
  }

  static List<InlineSpan> _parse(String html) {
    final spans = <InlineSpan>[];
    final buffer = StringBuffer();
    var bold = false;
    var italic = false;
    var underline = false;

    void flush() {
      if (buffer.isEmpty) return;
      final text = buffer.toString();
      buffer.clear();
      spans.add(TextSpan(
        text: text,
        style: TextStyle(
          fontWeight: bold ? FontWeight.bold : null,
          fontStyle: italic ? FontStyle.italic : null,
          decoration: underline ? TextDecoration.underline : null,
        ),
      ));
    }

    var i = 0;
    while (i < html.length) {
      if (html[i] == '<') {
        final closeIdx = html.indexOf('>', i);
        if (closeIdx == -1) {
          buffer.write(html[i]);
          i++;
          continue;
        }
        final tag = html.substring(i + 1, closeIdx).trim().toLowerCase();
        flush();

        if (tag == 'br' || tag == 'br/') {
          spans.add(const TextSpan(text: '\n'));
        } else if (tag == 'p') {
          if (spans.isNotEmpty) spans.add(const TextSpan(text: '\n\n'));
        } else if (tag == '/p') {
          // nothing
        } else if (tag == 'b' || tag == 'strong') {
          bold = true;
        } else if (tag == '/b' || tag == '/strong') {
          bold = false;
        } else if (tag == 'i' || tag == 'em') {
          italic = true;
        } else if (tag == '/i' || tag == '/em') {
          italic = false;
        } else if (tag == 'u') {
          underline = true;
        } else if (tag == '/u') {
          underline = false;
        } else if (tag == 'li') {
          if (spans.isNotEmpty) spans.add(const TextSpan(text: '\n'));
          spans.add(const TextSpan(text: '  \u2022 '));
        } else if (tag == '/li' || tag == 'ul' || tag == '/ul' || tag == 'ol' || tag == '/ol') {
          // skip
        }

        i = closeIdx + 1;
      } else if (html.startsWith('&amp;', i)) {
        buffer.write('&');
        i += 5;
      } else if (html.startsWith('&lt;', i)) {
        buffer.write('<');
        i += 4;
      } else if (html.startsWith('&gt;', i)) {
        buffer.write('>');
        i += 4;
      } else if (html.startsWith('&quot;', i)) {
        buffer.write('"');
        i += 6;
      } else if (html.startsWith('&nbsp;', i)) {
        buffer.write('\u00A0');
        i += 6;
      } else {
        buffer.write(html[i]);
        i++;
      }
    }
    flush();
    return spans;
  }
}
