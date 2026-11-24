package br.com.wdc.store.view.remote.shell.cn1;

/**
 * <b>Global</b> theme selectors (UIIDs) — shared across screens, mirroring
 * {@code _base.scss}. It is the singleton base of the selectors: each view package has its own
 * {@code XxxSel extends Sel} subclass with its own {@code INSTANCE}, so that a single
 * {@code sel} exposes the global ones (inherited) + the package's (analogous to the {@code ...GlobalSel}
 * spread of the TeaVM shell's {@code *-sel}).
 *
 * <p>Usage: {@code private static final XxxSel sel = XxxSel.INSTANCE;} and then {@code sel.FOO}.</p>
 */
public class Sel {

    public static final Sel INSTANCE = new Sel();

    protected Sel() {
        // singleton (instantiated by the subclasses)
    }

    public final String PRIMARY_BUTTON = "PrimaryButton";
    public final String CARD_LEAD = "CardLead";

    // Card header (widgets.CardHeader)
    public final String CARD_HEADER_ROW = "CardHeaderRow";
    public final String CARD_HEADER_ICON_BOX = "CardHeaderIconBox";
    public final String CARD_HEADER_TITLE = "CardHeaderTitle";
    public final String CARD_HEADER_SUB = "CardHeaderSub";

    public final String BACK_BUTTON = "BackButton";

    // Quantity stepper (product and cart)
    public final String QTY_BTN = "QtyBtn";
    public final String QTY_VALUE = "QtyValue";

    // Brand (login hero + connection splash)
    public final String HERO_TITLE = "HeroTitle";
    public final String HERO_SUBTITLE = "HeroSubtitle";

    // Connection screen (splash)

    public final String SPLASH_STATUS = "SplashStatus";
    public final String SPLASH_RETRY = "SplashRetry";
}
