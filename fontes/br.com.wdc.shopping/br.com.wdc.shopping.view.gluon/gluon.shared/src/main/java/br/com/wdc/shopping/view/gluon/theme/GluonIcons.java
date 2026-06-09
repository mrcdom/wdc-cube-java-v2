package br.com.wdc.shopping.view.gluon.theme;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Ícones vetoriais (SVG) do design system.
 * Cada constante é um SVG path (viewBox 24x24, Material Design).
 */
public final class GluonIcons {

    private GluonIcons() {
    }

    // Material Design icon paths (viewBox 0 0 24 24)

    /** Seta voltar (arrow_back) */
    public static final String ARROW_BACK = "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z";

    /** Carrinho de compras (shopping_cart) */
    public static final String SHOPPING_CART = "M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z";

    /** Loja / produtos (store) */
    public static final String STORE = "M20 4H4v2h16V4zm1 10v-2l-1-5H4l-1 5v2h1v6h10v-6h4v6h2v-6h1zm-9 4H6v-4h6v4z";

    /** Histórico / lista (history / list) */
    public static final String HISTORY = "M13 3c-4.97 0-9 4.03-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42C8.27 19.99 10.51 21 13 21c4.97 0 9-4.03 9-9s-4.03-9-9-9zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z";

    /** Lixeira (delete) */
    public static final String DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    /** Chevron direita (chevron_right) */
    public static final String CHEVRON_RIGHT = "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z";

    /** Chevron esquerda (chevron_left) */
    public static final String CHEVRON_LEFT = "M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z";

    /** Check circle (sucesso) */
    public static final String CHECK_CIRCLE = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z";

    /** Menos (remove) */
    public static final String MINUS = "M19 13H5v-2h14v2z";

    /** Mais (add) */
    public static final String PLUS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";

    /** Sair / logout */
    public static final String LOGOUT = "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z";

    /** Sacola de compras (shopping_bag) */
    public static final String SHOPPING_BAG = "M18 6h-2c0-2.21-1.79-4-4-4S8 3.79 8 6H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6-2c1.1 0 2 .9 2 2h-4c0-1.1.9-2 2-2zm6 16H6V8h2v2c0 .55.45 1 1 1s1-.45 1-1V8h4v2c0 .55.45 1 1 1s1-.45 1-1V8h2v12z";

    // ---- Factory method ----

    /**
     * Cria um nó SVGPath com o path, tamanho e cor especificados.
     */
    public static SVGPath create(String svgPath, double size, String color) {
        var icon = new SVGPath();
        icon.setContent(svgPath);
        icon.setFill(Color.web(color));
        // Scale: Material icons are 24x24, scale to desired size
        double scale = size / 24.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        return icon;
    }

    /**
     * Cria um nó SVGPath com o path e tamanho, usando cor padrão (TEXT_DEFAULT).
     */
    public static SVGPath create(String svgPath, double size) {
        return create(svgPath, size, GluonColors.TEXT_DEFAULT);
    }
}
