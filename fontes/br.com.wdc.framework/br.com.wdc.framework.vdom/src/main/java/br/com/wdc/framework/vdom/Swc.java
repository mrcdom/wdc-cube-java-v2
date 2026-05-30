// @formatter:off
package br.com.wdc.framework.vdom;

import static br.com.wdc.framework.vdom.VNode.*;

/**
 * Factory methods para Spectrum Web Components (SWC).
 * <p>
 * Catálogo completo dos componentes disponíveis no bundle:
 * {@code @spectrum-web-components/bundle/elements.js}
 * <p>
 * Uso:
 * <pre>{@code
 * import static br.com.wdc.framework.vdom.Swc.*;
 *
 * spButton("accent", "l")
 *     .on("click", e -> doSomething())
 *     .children(textNode("OK"));
 *
 * spActionButton()
 *     .children(span().cls("bi bi-arrow-left"), span().text("Voltar"))
 *     .on("click", e -> goBack());
 *
 * spTextField("Digite seu nome")
 *     .attr("id", "name")
 *     .boolAttr("disabled", loading)
 *     .ref(el -> nameField = el);
 * }</pre>
 */
public final class Swc {

    private Swc() {
    }

    // ========================
    // Layout & Structure
    // ========================

    /**
     * {@code <sp-theme color="..." scale="medium">} — Container de design tokens.
     */
    public static VNode spTheme(String color) {
        return el("sp-theme").attr("color", color).attr("scale", "medium");
    }

    /**
     * {@code <sp-theme color="..." scale="..." system="...">}
     */
    public static VNode spTheme(String color, String scale, String system) {
        return el("sp-theme").attr("color", color).attr("scale", scale).attr("system", system);
    }

    /**
     * {@code <sp-divider size="...">} — Separador horizontal/vertical.
     * <p>Sizes: "s", "m", "l"
     */
    public static VNode spDivider(String size) {
        return el("sp-divider").attr("size", size);
    }

    /**
     * {@code <sp-split-view>} — Painel dividido redimensionável.
     */
    public static VNode spSplitView() {
        return el("sp-split-view");
    }

    // ========================
    // Buttons & Actions
    // ========================

    /**
     * {@code <sp-button variant="...">} — Botão primário.
     * <p>Variants: "accent", "primary", "secondary", "negative", "white", "black"
     */
    public static VNode spButton(String variant) {
        return el("sp-button").attr("variant", variant);
    }

    /**
     * {@code <sp-button variant="..." size="...">}
     * <p>Sizes: "s", "m", "l", "xl"
     */
    public static VNode spButton(String variant, String size) {
        return el("sp-button").attr("variant", variant).attr("size", size);
    }

    /**
     * {@code <sp-action-button quiet>} — Botão de ação discreto.
     */
    public static VNode spActionButton() {
        return el("sp-action-button").boolAttr("quiet", true);
    }

    /**
     * {@code <sp-action-button quiet size="...">}
     * <p>Sizes: "xs", "s", "m", "l", "xl"
     */
    public static VNode spActionButton(String size) {
        return el("sp-action-button").boolAttr("quiet", true).attr("size", size);
    }

    /**
     * {@code <sp-action-group>} — Agrupa action-buttons.
     * <p>Atributos comuns: compact, quiet, vertical, size
     */
    public static VNode spActionGroup() {
        return el("sp-action-group");
    }

    /**
     * {@code <sp-button-group>} — Agrupa botões com espaçamento padrão.
     */
    public static VNode spButtonGroup() {
        return el("sp-button-group");
    }

    /**
     * {@code <sp-close-button>} — Botão de fechar (×).
     */
    public static VNode spCloseButton() {
        return el("sp-close-button");
    }

    /**
     * {@code <sp-clear-button>} — Botão de limpar campo.
     */
    public static VNode spClearButton() {
        return el("sp-clear-button");
    }

    // ========================
    // Forms & Inputs
    // ========================

    /**
     * {@code <sp-textfield placeholder="...">} — Campo de texto single-line.
     */
    public static VNode spTextField(String placeholder) {
        return el("sp-textfield").attr("placeholder", placeholder);
    }

    /**
     * {@code <sp-textfield placeholder="..." type="...">}
     * <p>Types: "text", "password", "email", "url", "tel", "number"
     */
    public static VNode spTextField(String placeholder, String type) {
        return el("sp-textfield").attr("placeholder", placeholder).attr("type", type);
    }

    /**
     * {@code <sp-textarea placeholder="...">} — Campo de texto multi-line.
     */
    public static VNode spTextArea(String placeholder) {
        return el("sp-textarea").attr("placeholder", placeholder);
    }

    /**
     * {@code <sp-number-field>} — Campo numérico com stepper.
     */
    public static VNode spNumberField() {
        return el("sp-number-field");
    }

    /**
     * {@code <sp-search placeholder="...">} — Campo de busca com ícone e clear.
     */
    public static VNode spSearch(String placeholder) {
        return el("sp-search").attr("placeholder", placeholder);
    }

    /**
     * {@code <sp-field-label for="...">texto</sp-field-label>} — Label de campo.
     */
    public static VNode spFieldLabel(String forId, String label) {
        return el("sp-field-label").attr("for", forId).children(textNode(label));
    }

    /**
     * {@code <sp-field-group>} — Agrupa label + campo + help-text.
     */
    public static VNode spFieldGroup() {
        return el("sp-field-group");
    }

    /**
     * {@code <sp-help-text>} — Texto de ajuda/erro sob um campo.
     * <p>Variants: "neutral" (default), "negative"
     */
    public static VNode spHelpText(String text) {
        return el("sp-help-text").children(textNode(text));
    }

    /**
     * {@code <sp-help-text variant="negative">} — Mensagem de erro de campo.
     */
    public static VNode spHelpTextError(String text) {
        return el("sp-help-text").attr("variant", "negative").children(textNode(text));
    }

    /**
     * {@code <sp-checkbox>texto</sp-checkbox>}
     */
    public static VNode spCheckbox(String label) {
        return el("sp-checkbox").children(textNode(label));
    }

    /**
     * {@code <sp-switch>texto</sp-switch>} — Toggle on/off.
     */
    public static VNode spSwitch(String label) {
        return el("sp-switch").children(textNode(label));
    }

    /**
     * {@code <sp-radio value="...">texto</sp-radio>}
     */
    public static VNode spRadio(String value, String label) {
        return el("sp-radio").attr("value", value).children(textNode(label));
    }

    /**
     * {@code <sp-radio-group>} — Grupo de radio buttons.
     * <p>Atributos comuns: label, horizontal
     */
    public static VNode spRadioGroup() {
        return el("sp-radio-group");
    }

    /**
     * {@code <sp-picker label="...">} — Dropdown select.
     */
    public static VNode spPicker(String label) {
        return el("sp-picker").attr("label", label);
    }

    /**
     * {@code <sp-combobox>} — Combobox com autocomplete.
     */
    public static VNode spCombobox() {
        return el("sp-combobox");
    }

    /**
     * {@code <sp-slider>} — Controle deslizante.
     * <p>Atributos comuns: min, max, value, step, label
     */
    public static VNode spSlider() {
        return el("sp-slider");
    }

    // ========================
    // Menu
    // ========================

    /**
     * {@code <sp-menu>} — Container de menu (usado dentro de picker, action-menu, popover).
     */
    public static VNode spMenu() {
        return el("sp-menu");
    }

    /**
     * {@code <sp-menu-item value="...">texto</sp-menu-item>}
     */
    public static VNode spMenuItem(String value, String label) {
        return el("sp-menu-item").attr("value", value).children(textNode(label));
    }

    /**
     * {@code <sp-menu-divider>} — Separador dentro de menu.
     */
    public static VNode spMenuDivider() {
        return el("sp-menu-divider");
    }

    /**
     * {@code <sp-menu-group>} — Grupo com header dentro de menu.
     */
    public static VNode spMenuGroup() {
        return el("sp-menu-group");
    }

    /**
     * {@code <sp-action-menu>} — Botão que abre menu dropdown.
     */
    public static VNode spActionMenu() {
        return el("sp-action-menu");
    }

    // ========================
    // Navigation
    // ========================

    /**
     * {@code <sp-tabs>} — Container de abas.
     * <p>Atributos comuns: selected, direction ("horizontal", "vertical")
     */
    public static VNode spTabs() {
        return el("sp-tabs");
    }

    /**
     * {@code <sp-tab value="..." label="...">} — Aba individual.
     */
    public static VNode spTab(String value, String label) {
        return el("sp-tab").attr("value", value).attr("label", label);
    }

    /**
     * {@code <sp-tab-panel value="...">} — Conteúdo de uma aba.
     */
    public static VNode spTabPanel(String value) {
        return el("sp-tab-panel").attr("value", value);
    }

    /**
     * {@code <sp-sidenav>} — Navegação lateral.
     */
    public static VNode spSidenav() {
        return el("sp-sidenav");
    }

    /**
     * {@code <sp-sidenav-item value="..." label="...">}
     */
    public static VNode spSidenavItem(String value, String label) {
        return el("sp-sidenav-item").attr("value", value).attr("label", label);
    }

    /**
     * {@code <sp-top-nav>} — Navegação horizontal superior.
     */
    public static VNode spTopNav() {
        return el("sp-top-nav");
    }

    /**
     * {@code <sp-top-nav-item href="...">texto</sp-top-nav-item>}
     */
    public static VNode spTopNavItem(String href, String label) {
        return el("sp-top-nav-item").attr("href", href).children(textNode(label));
    }

    // ========================
    // Feedback & Status
    // ========================

    /**
     * {@code <sp-toast variant="...">} — Notificação temporária.
     * <p>Variants: "positive", "negative", "info"
     */
    public static VNode spToast(String variant) {
        return el("sp-toast").attr("variant", variant);
    }

    /**
     * {@code <sp-toast open variant="...">msg</sp-toast>}
     */
    public static VNode spToast(String variant, String message) {
        return el("sp-toast").attr("variant", variant).boolAttr("open", true).children(textNode(message));
    }

    /**
     * {@code <sp-progress-bar>} — Barra de progresso.
     * <p>Atributos: label, progress (0-100), indeterminate
     */
    public static VNode spProgressBar() {
        return el("sp-progress-bar");
    }

    /**
     * {@code <sp-progress-bar label="..." progress="...">}
     */
    public static VNode spProgressBar(String label, int progress) {
        return el("sp-progress-bar").attr("label", label).attr("progress", String.valueOf(progress));
    }

    /**
     * {@code <sp-progress-circle>} — Spinner circular.
     * <p>Atributos: size ("s","m","l"), indeterminate
     */
    public static VNode spProgressCircle() {
        return el("sp-progress-circle");
    }

    /**
     * {@code <sp-progress-circle size="..." indeterminate>} — Loading spinner.
     */
    public static VNode spProgressCircle(String size) {
        return el("sp-progress-circle").attr("size", size).boolAttr("indeterminate", true);
    }

    /**
     * {@code <sp-status-light variant="...">texto</sp-status-light>}
     * <p>Variants: "positive", "negative", "notice", "info", "neutral",
     *            "celery", "chartreuse", "cyan", "fuchsia", "indigo",
     *            "magenta", "purple", "seafoam", "yellow"
     */
    public static VNode spStatusLight(String variant, String label) {
        return el("sp-status-light").attr("variant", variant).children(textNode(label));
    }

    /**
     * {@code <sp-badge variant="...">texto</sp-badge>}
     * <p>Variants: "informative", "positive", "negative", "neutral"
     */
    public static VNode spBadge(String variant, String label) {
        return el("sp-badge").attr("variant", variant).children(textNode(label));
    }

    /**
     * {@code <sp-meter>} — Medidor com fill colorido.
     * <p>Atributos: label, progress, variant ("positive","notice","negative")
     */
    public static VNode spMeter(String label, int progress) {
        return el("sp-meter").attr("label", label).attr("progress", String.valueOf(progress));
    }

    // ========================
    // Overlays & Dialogs
    // ========================

    /**
     * {@code <sp-dialog>} — Diálogo modal/non-modal.
     * <p>Atributos comuns: size ("s","m","l"), dismissable
     */
    public static VNode spDialog() {
        return el("sp-dialog");
    }

    /**
     * {@code <sp-dialog-wrapper headline="...">} — Diálogo auto-contido com header.
     */
    public static VNode spDialogWrapper(String headline) {
        return el("sp-dialog-wrapper").attr("headline", headline);
    }

    /**
     * {@code <sp-alert-dialog title="..." variant="...">} — Diálogo de confirmação/alerta.
     * <p>Variants: "confirmation", "information", "warning", "error", "destructive"
     */
    public static VNode spAlertDialog(String title, String variant) {
        return el("sp-alert-dialog").attr("title", title).attr("variant", variant);
    }

    /**
     * {@code <overlay-trigger>} — Gatilho que abre overlay via interação.
     * <p>Atributos: type ("modal","replace","inline"), placement
     */
    public static VNode overlayTrigger() {
        return el("overlay-trigger");
    }

    /**
     * {@code <sp-popover>} — Conteúdo flutuante posicionado.
     */
    public static VNode spPopover() {
        return el("sp-popover");
    }

    /**
     * {@code <sp-tooltip>} — Tooltip informativo.
     * <p>Atributos: placement ("top","bottom","left","right"), self-managed
     */
    public static VNode spTooltip(String text) {
        return el("sp-tooltip").children(textNode(text));
    }

    /**
     * {@code <sp-tooltip placement="..." self-managed>}
     */
    public static VNode spTooltip(String text, String placement) {
        return el("sp-tooltip").attr("placement", placement).boolAttr("self-managed", true).children(textNode(text));
    }

    /**
     * {@code <sp-tray>} — Painel deslizante (mobile-first).
     */
    public static VNode spTray() {
        return el("sp-tray");
    }

    // ========================
    // Content & Display
    // ========================

    /**
     * {@code <sp-card heading="...">} — Card com header, preview e footer.
     */
    public static VNode spCard(String heading) {
        return el("sp-card").attr("heading", heading);
    }

    /**
     * {@code <sp-card heading="..." variant="...">}
     * <p>Variants: "standard" (default), "quiet"
     */
    public static VNode spCard(String heading, String variant) {
        return el("sp-card").attr("heading", heading).attr("variant", variant);
    }

    /**
     * {@code <sp-avatar label="..." src="...">} — Avatar circular.
     */
    public static VNode spAvatar(String label, String src) {
        return el("sp-avatar").attr("label", label).attr("src", src);
    }

    /**
     * {@code <sp-avatar label="...">} — Avatar sem imagem (initials).
     */
    public static VNode spAvatar(String label) {
        return el("sp-avatar").attr("label", label);
    }

    /**
     * {@code <sp-link href="...">texto</sp-link>} — Link estilizado.
     */
    public static VNode spLink(String href, String text) {
        return el("sp-link").attr("href", href).children(textNode(text));
    }

    /**
     * {@code <sp-icon name="...">} — Ícone do Spectrum.
     * <p>Ex: {@code spIcon("ui:Checkmark100")}
     */
    public static VNode spIcon(String name) {
        return el("sp-icon").attr("name", name);
    }

    /**
     * {@code <sp-icon name="..." size="...">}
     */
    public static VNode spIcon(String name, String size) {
        return el("sp-icon").attr("name", name).attr("size", size);
    }

    /**
     * {@code <sp-thumbnail>} — Miniatura de imagem/asset.
     */
    public static VNode spThumbnail() {
        return el("sp-thumbnail");
    }

    /**
     * {@code <sp-illustrated-message heading="...">} — Estado vazio com ilustração.
     */
    public static VNode spIllustratedMessage(String heading) {
        return el("sp-illustrated-message").attr("heading", heading);
    }

    /**
     * {@code <sp-tag>texto</sp-tag>} — Tag/chip de conteúdo.
     */
    public static VNode spTag(String label) {
        return el("sp-tag").children(textNode(label));
    }

    /**
     * {@code <sp-tags>} — Container de tags.
     */
    public static VNode spTags() {
        return el("sp-tags");
    }

    /**
     * {@code <sp-banner>} — Banner informativo de topo.
     */
    public static VNode spBanner() {
        return el("sp-banner");
    }

    // ========================
    // Table
    // ========================

    /**
     * {@code <sp-table>} — Tabela de dados.
     * <p>Atributos: selects ("single","multiple"), size ("s","m")
     */
    public static VNode spTable() {
        return el("sp-table");
    }

    /**
     * {@code <sp-table-head>} — Cabeçalho da tabela.
     */
    public static VNode spTableHead() {
        return el("sp-table-head");
    }

    /**
     * {@code <sp-table-head-cell>texto</sp-table-head-cell>}
     */
    public static VNode spTableHeadCell(String label) {
        return el("sp-table-head-cell").children(textNode(label));
    }

    /**
     * {@code <sp-table-body>} — Corpo da tabela.
     */
    public static VNode spTableBody() {
        return el("sp-table-body");
    }

    /**
     * {@code <sp-table-row>} — Linha da tabela.
     * <p>Atributos: value (para seleção)
     */
    public static VNode spTableRow() {
        return el("sp-table-row");
    }

    /**
     * {@code <sp-table-row value="...">}
     */
    public static VNode spTableRow(String value) {
        return el("sp-table-row").attr("value", value);
    }

    /**
     * {@code <sp-table-cell>texto</sp-table-cell>}
     */
    public static VNode spTableCell(String text) {
        return el("sp-table-cell").children(textNode(text));
    }

    /**
     * {@code <sp-table-cell>} — Célula vazia para conteúdo complexo.
     */
    public static VNode spTableCell() {
        return el("sp-table-cell");
    }

    // ========================
    // Action Patterns
    // ========================

    /**
     * {@code <sp-action-bar>} — Barra de ações contextuais (aparece ao selecionar itens).
     * <p>Atributos: open, emphasized
     */
    public static VNode spActionBar() {
        return el("sp-action-bar");
    }
}
