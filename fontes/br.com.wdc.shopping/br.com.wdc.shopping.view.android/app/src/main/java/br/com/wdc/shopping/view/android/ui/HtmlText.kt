package br.com.wdc.shopping.view.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Renders simple HTML (ul/li, br, p, b, strong) as Compose Text with bullet points.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier
) {
    val annotated = remember(html) { parseSimpleHtml(html) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

private val BR_REGEX = Regex("<br\\s*/?>") 
private val P_REGEX = Regex("</?p>")
private val UL_REGEX = Regex("</?[uo]l>")
private val LI_REGEX = Regex("<li>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
private val TAG_REGEX = Regex("<[^>]+>")
private val MULTILINE_REGEX = Regex("\n{3,}")

private fun parseSimpleHtml(html: String): AnnotatedString {
    return buildAnnotatedString {
        var text = html
            .replace(BR_REGEX, "\n")
            .replace(P_REGEX, "\n")
            .replace(UL_REGEX, "")

        text = text.replace(LI_REGEX) { match ->
            "\u2022 ${match.groupValues[1].trim()}\n"
        }

        text = text.replace(TAG_REGEX, "")
        text = text.trim().replace(MULTILINE_REGEX, "\n\n")

        append(text)
    }
}
