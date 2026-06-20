package br.com.wdc.shopping.view.remote.shell.cn1;

/**
 * Seletores (UIIDs) <b>globais</b> do tema — compartilhados entre telas, espelhando o
 * {@code _base.scss}. Cada pacote de view tem sua própria classe {@code *Sel} para os UIIDs
 * específicos (análogo ao {@code global-sel}/{@code *-sel} do shell TeaVM).
 */
public final class Sel {

    private Sel() {
        // NOOP
    }

    public static final String PRIMARY_BUTTON = "PrimaryButton";
    public static final String CARD_LEAD = "CardLead";

    // Cabeçalho de card (Widgets.cardHeader)
    public static final String CARD_HEADER_ROW = "CardHeaderRow";
    public static final String CARD_HEADER_ICON_BOX = "CardHeaderIconBox";
    public static final String CARD_HEADER_TITLE = "CardHeaderTitle";
    public static final String CARD_HEADER_SUB = "CardHeaderSub";

    public static final String BACK_BUTTON = "BackButton";
    // Stepper de quantidade (produto e carrinho)
    public static final String QTY_BTN = "QtyBtn";
    public static final String QTY_VALUE = "QtyValue";

    // Tela de conexão (splash)
    public static final String SPLASH_STATUS = "SplashStatus";
    public static final String SPLASH_RETRY = "SplashRetry";
}
