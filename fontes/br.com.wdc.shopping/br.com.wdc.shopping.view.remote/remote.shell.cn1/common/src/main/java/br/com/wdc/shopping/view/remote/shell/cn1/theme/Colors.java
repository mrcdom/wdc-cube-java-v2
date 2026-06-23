package br.com.wdc.shopping.view.remote.shell.cn1.theme;

/**
 * Paleta de cores usada no código Java (pintura custom, ícones "assados", estados de clique) — onde
 * o tema CSS não alcança. <b>Espelha</b> os tokens de {@code scss/_tokens.scss}: mantenha os dois em
 * sincronia (o CSS do CN1 não expõe as variáveis Sass ao Java). Valores em RGB {@code 0xRRGGBB},
 * exceto onde anotado.
 */
public final class Colors {

    private Colors() {
        // NOOP
    }

    // -- Espelham tokens do _tokens.scss --
    public static final int ACCENT = 0x0d66d0;         // $accent
    public static final int ACCENT_3 = 0x4da6ff;       // $accent-3 (fim do gradiente azul)
    public static final int ACCENT_LIGHT = 0xe8f1fc;   // $accent-light (hover, ícone-box, borda)
    public static final int SURFACE = 0xffffff;        // $surface (branco)
    public static final int BG = 0xf4f6f9;             // $bg
    public static final int TEXT_SECONDARY = 0x6e6e73; // $text-secondary

    // -- Cores sem token correspondente (só no código) --
    public static final int BADGE = 0xff3b30;          // vermelho do badge do carrinho (iOS systemRed)
    public static final int ERROR = 0xcc0000;          // texto de erro do login
    public static final int GRAY_DISABLED = 0xc7c7cc;  // seta desabilitada (paginação)
    public static final int NEUTRAL = 0xe5e7eb;        // fundo de clique do botão "voltar"

    /** Placeholder da imagem de produto — <b>ARGB</b> ({@code 0xAARRGGBB}, opaco) p/ {@code Image.createImage}. */
    public static final int IMAGE_PLACEHOLDER = 0xffeeeeee;
}
