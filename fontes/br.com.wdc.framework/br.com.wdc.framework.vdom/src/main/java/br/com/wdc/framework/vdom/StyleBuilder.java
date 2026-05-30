// @formatter:off
package br.com.wdc.framework.vdom;

/**
 * Builder fluente para construir strings de estilo CSS inline.
 * <p>
 * Uso:
 * <pre>{@code
 * import static br.com.wdc.framework.vdom.StyleBuilder.css;
 *
 * var style = css().display("flex").gap("8px").padding("16px").build();
 * // → "display:flex;gap:8px;padding:16px"
 *
 * el("div").style(css().flexRow().gap("12px").build())
 * }</pre>
 */
public class StyleBuilder {

    private final StringBuilder sb;

    private StyleBuilder() {
        this.sb = new StringBuilder(128);
    }

    public static StyleBuilder css() {
        return new StyleBuilder();
    }

    public String build() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return build();
    }

    // ---- Raw property ----

    public StyleBuilder prop(String name, String value) {
        if (value != null && !value.isEmpty()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(name).append(':').append(value);
        }
        return this;
    }

    // ---- Display ----

    public StyleBuilder display(String value) { return prop("display", value); }
    public StyleBuilder displayNone() { return prop("display", "none"); }
    public StyleBuilder displayFlex() { return prop("display", "flex"); }
    public StyleBuilder displayGrid() { return prop("display", "grid"); }
    public StyleBuilder displayBlock() { return prop("display", "block"); }
    public StyleBuilder displayInline() { return prop("display", "inline"); }
    public StyleBuilder displayInlineFlex() { return prop("display", "inline-flex"); }

    // ---- Flex shortcuts ----

    public StyleBuilder flexRow() { return displayFlex().prop("flex-direction", "row"); }
    public StyleBuilder flexCol() { return displayFlex().prop("flex-direction", "column"); }
    public StyleBuilder flexDirection(String value) { return prop("flex-direction", value); }
    public StyleBuilder flex(String value) { return prop("flex", value); }
    public StyleBuilder flexGrow(int value) { return prop("flex-grow", String.valueOf(value)); }
    public StyleBuilder flexShrink(int value) { return prop("flex-shrink", String.valueOf(value)); }
    public StyleBuilder flexWrap(String value) { return prop("flex-wrap", value); }
    public StyleBuilder gap(String value) { return prop("gap", value); }
    public StyleBuilder rowGap(String value) { return prop("row-gap", value); }
    public StyleBuilder columnGap(String value) { return prop("column-gap", value); }

    // ---- Alignment ----

    public StyleBuilder alignItems(String value) { return prop("align-items", value); }
    public StyleBuilder alignSelf(String value) { return prop("align-self", value); }
    public StyleBuilder justifyContent(String value) { return prop("justify-content", value); }
    public StyleBuilder justifySelf(String value) { return prop("justify-self", value); }
    public StyleBuilder center() { return alignItems("center").justifyContent("center"); }

    // ---- Box model ----

    public StyleBuilder width(String value) { return prop("width", value); }
    public StyleBuilder height(String value) { return prop("height", value); }
    public StyleBuilder minWidth(String value) { return prop("min-width", value); }
    public StyleBuilder minHeight(String value) { return prop("min-height", value); }
    public StyleBuilder maxWidth(String value) { return prop("max-width", value); }
    public StyleBuilder maxHeight(String value) { return prop("max-height", value); }

    public StyleBuilder margin(String value) { return prop("margin", value); }
    public StyleBuilder marginTop(String value) { return prop("margin-top", value); }
    public StyleBuilder marginBottom(String value) { return prop("margin-bottom", value); }
    public StyleBuilder marginLeft(String value) { return prop("margin-left", value); }
    public StyleBuilder marginRight(String value) { return prop("margin-right", value); }

    public StyleBuilder padding(String value) { return prop("padding", value); }
    public StyleBuilder paddingTop(String value) { return prop("padding-top", value); }
    public StyleBuilder paddingBottom(String value) { return prop("padding-bottom", value); }
    public StyleBuilder paddingLeft(String value) { return prop("padding-left", value); }
    public StyleBuilder paddingRight(String value) { return prop("padding-right", value); }

    // ---- Border & radius ----

    public StyleBuilder border(String value) { return prop("border", value); }
    public StyleBuilder borderTop(String value) { return prop("border-top", value); }
    public StyleBuilder borderBottom(String value) { return prop("border-bottom", value); }
    public StyleBuilder borderLeft(String value) { return prop("border-left", value); }
    public StyleBuilder borderRight(String value) { return prop("border-right", value); }
    public StyleBuilder borderRadius(String value) { return prop("border-radius", value); }

    // ---- Colors ----

    public StyleBuilder color(String value) { return prop("color", value); }
    public StyleBuilder background(String value) { return prop("background", value); }
    public StyleBuilder backgroundColor(String value) { return prop("background-color", value); }

    // ---- Typography ----

    public StyleBuilder fontSize(String value) { return prop("font-size", value); }
    public StyleBuilder fontWeight(String value) { return prop("font-weight", value); }
    public StyleBuilder fontFamily(String value) { return prop("font-family", value); }
    public StyleBuilder lineHeight(String value) { return prop("line-height", value); }
    public StyleBuilder textAlign(String value) { return prop("text-align", value); }
    public StyleBuilder textDecoration(String value) { return prop("text-decoration", value); }
    public StyleBuilder textOverflow(String value) { return prop("text-overflow", value); }
    public StyleBuilder whiteSpace(String value) { return prop("white-space", value); }

    // ---- Overflow ----

    public StyleBuilder overflow(String value) { return prop("overflow", value); }
    public StyleBuilder overflowX(String value) { return prop("overflow-x", value); }
    public StyleBuilder overflowY(String value) { return prop("overflow-y", value); }
    public StyleBuilder overflowHidden() { return prop("overflow", "hidden"); }

    // ---- Position ----

    public StyleBuilder position(String value) { return prop("position", value); }
    public StyleBuilder top(String value) { return prop("top", value); }
    public StyleBuilder bottom(String value) { return prop("bottom", value); }
    public StyleBuilder left(String value) { return prop("left", value); }
    public StyleBuilder right(String value) { return prop("right", value); }
    public StyleBuilder zIndex(int value) { return prop("z-index", String.valueOf(value)); }

    // ---- Visual ----

    public StyleBuilder opacity(String value) { return prop("opacity", value); }
    public StyleBuilder boxShadow(String value) { return prop("box-shadow", value); }
    public StyleBuilder cursor(String value) { return prop("cursor", value); }
    public StyleBuilder transition(String value) { return prop("transition", value); }
    public StyleBuilder transform(String value) { return prop("transform", value); }
    public StyleBuilder animation(String value) { return prop("animation", value); }
    public StyleBuilder visibility(String value) { return prop("visibility", value); }

    // ---- Grid ----

    public StyleBuilder gridTemplateColumns(String value) { return prop("grid-template-columns", value); }
    public StyleBuilder gridTemplateRows(String value) { return prop("grid-template-rows", value); }
    public StyleBuilder gridColumn(String value) { return prop("grid-column", value); }
    public StyleBuilder gridRow(String value) { return prop("grid-row", value); }

    // ---- Sizing shortcuts ----

    public StyleBuilder boxSizing(String value) { return prop("box-sizing", value); }
    public StyleBuilder aspectRatio(String value) { return prop("aspect-ratio", value); }
    public StyleBuilder objectFit(String value) { return prop("object-fit", value); }

    // ---- CSS Variables ----

    public StyleBuilder var(String varName, String value) { return prop(varName, value); }
}
