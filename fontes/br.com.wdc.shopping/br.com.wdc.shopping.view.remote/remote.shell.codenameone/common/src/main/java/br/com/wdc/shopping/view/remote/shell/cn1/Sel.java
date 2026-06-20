package br.com.wdc.shopping.view.remote.shell.cn1;

/**
 * Seletores (UIIDs) <b>globais</b> do tema — compartilhados entre telas, espelhando o
 * {@code _base.scss}. É a base singleton dos seletores: cada pacote de view tem sua própria
 * subclasse {@code XxxSel extends Sel} com {@code INSTANCE} próprio, de modo que um único
 * {@code sel} expõe os globais (herdados) + os do pacote (análogo ao spread {@code ...GlobalSel}
 * dos {@code *-sel} do shell TeaVM).
 *
 * <p>Uso: {@code private static final XxxSel sel = XxxSel.INSTANCE;} e então {@code sel.FOO}.</p>
 */
public class Sel {

    public static final Sel INSTANCE = new Sel();

    protected Sel() {
        // singleton (instanciado pelas subclasses)
    }

    public final String PRIMARY_BUTTON = "PrimaryButton";
    public final String CARD_LEAD = "CardLead";

    // Cabeçalho de card (Widgets.cardHeader)
    public final String CARD_HEADER_ROW = "CardHeaderRow";
    public final String CARD_HEADER_ICON_BOX = "CardHeaderIconBox";
    public final String CARD_HEADER_TITLE = "CardHeaderTitle";
    public final String CARD_HEADER_SUB = "CardHeaderSub";

    public final String BACK_BUTTON = "BackButton";

    // Stepper de quantidade (produto e carrinho)
    public final String QTY_BTN = "QtyBtn";
    public final String QTY_VALUE = "QtyValue";

    // Marca (hero do login + splash de conexão)
    public final String HERO_TITLE = "HeroTitle";
    public final String HERO_SUBTITLE = "HeroSubtitle";

    // Tela de conexão (splash)
    public final String SPLASH_STATUS = "SplashStatus";
    public final String SPLASH_RETRY = "SplashRetry";
}
