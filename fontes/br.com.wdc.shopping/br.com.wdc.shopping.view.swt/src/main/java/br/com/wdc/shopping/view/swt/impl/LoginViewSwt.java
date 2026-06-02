package br.com.wdc.shopping.view.swt.impl;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * Login view matching the TeaVM web design:
 * - Left panel: blue gradient with branding (WDC Shopping logo + features)
 * - Right panel: white card with login form
 */
public class LoginViewSwt extends AbstractViewSwt<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private Text userNameField;
    private Text passwordField;
    private Canvas errorLabel;
    private Label errorSpacer;
    private String errorMessage;

    public LoginViewSwt(LoginPresenter presenter) {
        super("login", (ShoppingSwtApplication) presenter.app, presenter,
                createRootComposite((ShoppingSwtApplication) presenter.app));
        this.state = presenter.state;
    }

    private static Composite createRootComposite(ShoppingSwtApplication app) {
        var root = new Composite(app.getOffscreen(), SWT.NONE);
        var layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        root.setLayout(layout);
        root.setBackground(Theme.BG_LOGIN_LEFT);
        return root;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.userNameField = null;
        this.passwordField = null;
        this.errorLabel = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (this.state.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }

        if (!Objects.equals(this.errorMessage, newErrorMessage)) {
            this.errorMessage = newErrorMessage;
            this.errorLabel.redraw();
        }
        if (this.errorLabel.getVisible() != newErrorDisplay) {
            this.errorLabel.setVisible(newErrorDisplay);
            ((GridData) this.errorLabel.getLayoutData()).exclude = !newErrorDisplay;
            this.errorSpacer.setVisible(newErrorDisplay);
            ((GridData) this.errorSpacer.getLayoutData()).exclude = !newErrorDisplay;
            this.element.layout(true, true);
        }
    }

    private void initialRender() {
        // :: Left panel — blue gradient with branding
        createLeftPanel();

        // :: Right panel — white form
        createRightPanel();

        // Show this view in the Shell's StackLayout
        var shell = this.element.getParent();
        if (shell.getLayout() instanceof org.eclipse.swt.custom.StackLayout stackLayout) {
            stackLayout.topControl = this.element;
            shell.layout(true, true);
        }
    }

    // ========== LEFT PANEL (Branding) ==========

    private void createLeftPanel() {
        var leftPanel = new Canvas(this.element, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        leftPanel.setLayoutData(gd);
        leftPanel.addPaintListener(this::paintLeftPanel);
    }

    private void paintLeftPanel(PaintEvent e) {
        GC gc = e.gc;
        Rectangle bounds = ((Canvas) e.widget).getBounds();
        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);

        // Blue gradient background
        gc.setBackground(Theme.BG_LOGIN_LEFT);
        gc.setForeground(Theme.PRIMARY_BLUE);
        gc.fillGradientRectangle(0, 0, bounds.width, bounds.height, true);

        // Decorative circles (matching the web design)
        gc.setAlpha(30);
        gc.setBackground(Theme.FG_TEXT_WHITE);
        gc.fillOval(bounds.width - 120, -60, 240, 240);
        gc.fillOval(-80, bounds.height - 200, 200, 200);
        gc.setAlpha(255);

        // Bag icon (logo)
        gc.setForeground(Theme.FG_TEXT_WHITE);
        gc.setFont(Theme.FONT_ICON_LARGE);
        Point iconSize = gc.textExtent(Theme.ICON_BAG_CHECK);
        int iconY = bounds.height / 4 - iconSize.y / 2;
        gc.drawText(Theme.ICON_BAG_CHECK, (bounds.width - iconSize.x) / 2, iconY, true);

        // Title text
        gc.setFont(Theme.FONT_TITLE);
        String title = "WDC Shopping";
        Point titleSize = gc.textExtent(title);
        int titleY = iconY + iconSize.y + 16;
        gc.drawText(title, (bounds.width - titleSize.x) / 2, titleY, true);

        // Subtitle
        gc.setFont(Theme.FONT_SUBTITLE);
        String subtitle = "Sua compra certa na internet.";
        Point subtitleSize = gc.textExtent(subtitle);
        gc.drawText(subtitle, (bounds.width - subtitleSize.x) / 2, titleY + titleSize.y + 8, true);

        // Feature list with Bootstrap Icons
        int featureY = titleY + titleSize.y + 60;
        String[][] features = {
                { Theme.ICON_SHIELD_CHECK, "  Compra segura" },
                { Theme.ICON_TRUCK, "  Entrega rápida" },
                { Theme.ICON_ARROW_REPEAT, "  Troca garantida" }
        };
        for (String[] feature : features) {
            // Draw icon
            gc.setFont(Theme.FONT_ICON);
            Point iSize = gc.textExtent(feature[0]);
            int featureX = (bounds.width - 200) / 2;
            gc.drawText(feature[0], featureX, featureY, true);

            // Draw text
            gc.setFont(Theme.FONT_BODY);
            gc.drawText(feature[1], featureX + iSize.x, featureY, true);

            featureY += iSize.y + 16;
        }
    }

    // ========== RIGHT PANEL (Form) ==========

    private void createRightPanel() {
        var rightPanel = new Composite(this.element, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        rightPanel.setBackground(Theme.BG_WHITE);
        rightPanel.setBackgroundMode(SWT.INHERIT_FORCE);
        var gd = new GridData(SWT.FILL, SWT.FILL, false, true);
        gd.widthHint = 420;
        rightPanel.setLayoutData(gd);

        // Custom white background painting (macOS Dark Mode bypassed via NO_BACKGROUND)
        rightPanel.addPaintListener(e -> {
            e.gc.setBackground(Theme.BG_WHITE);
            e.gc.fillRectangle(rightPanel.getClientArea());
        });

        var layout = new GridLayout(1, false);
        layout.marginWidth = 20;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        rightPanel.setLayout(layout);

        // Top spacer to push form toward vertical center
        var topSpacer = new Label(rightPanel, SWT.NONE);
        topSpacer.setBackground(Theme.BG_WHITE);
        topSpacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Blue banner card (matching web "login-mobile-logo")
        createBannerCard(rightPanel);

        createSpacer(rightPanel, 24);

        // Inner form panel (indented relative to banner)
        var formPanel = new Composite(rightPanel, SWT.NONE);
        formPanel.setBackground(Theme.BG_WHITE);
        var formGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        formPanel.setLayoutData(formGd);
        var formLayout = new GridLayout(1, false);
        formLayout.marginWidth = 40;
        formLayout.marginHeight = 0;
        formLayout.verticalSpacing = 0;
        formPanel.setLayout(formLayout);

        // "Bem-vindo" title (larger, bolder)
        var title = new Label(formPanel, SWT.NONE);
        title.setText("Bem-vindo");
        title.setFont(Theme.FONT_WELCOME);
        title.setForeground(Theme.FG_TEXT_DARK);
        title.setBackground(Theme.BG_WHITE);
        title.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        createSpacer(formPanel, 6);

        // Subtitle
        var subtitle = new Label(formPanel, SWT.NONE);
        subtitle.setText("Entre com suas credenciais para continuar");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.FG_TEXT_SUBTLE);
        subtitle.setBackground(Theme.BG_WHITE);
        subtitle.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        createSpacer(formPanel, 28);

        // Error alert box (hidden by default) — between subtitle and fields
        this.errorLabel = new Canvas(formPanel, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var errorGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        errorGd.heightHint = 40;
        errorGd.verticalIndent = 12;
        errorGd.exclude = true;
        this.errorLabel.setLayoutData(errorGd);
        this.errorLabel.setVisible(false);
        this.errorLabel.addPaintListener(ev -> {
            var gc = ev.gc;
            var area = this.errorLabel.getClientArea();
            gc.setAntialias(SWT.ON);
            // Red-tinted background
            gc.setBackground(Theme.BG_ERROR);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 8, 8);
            // Red border
            gc.setForeground(Theme.BORDER_ERROR_BOX);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 8, 8);
            // Icon
            gc.setForeground(Theme.FG_ERROR);
            gc.setFont(Theme.FONT_ICON);
            Point iconSz = gc.textExtent(Theme.ICON_EXCLAMATION_CIRCLE);
            int iconX = 12;
            int iconY = 12;
            gc.drawText(Theme.ICON_EXCLAMATION_CIRCLE, iconX, iconY, true);
            // Error text with wrapping via TextLayout
            String msg = this.errorMessage != null ? this.errorMessage : "";
            int textX = iconX + iconSz.x + 10;
            var tl = new TextLayout(ev.display);
            tl.setText(msg);
            tl.setFont(Theme.FONT_BODY);
            tl.setWidth(area.width - textX - 12);
            gc.setForeground(Theme.FG_ERROR);
            tl.draw(gc, textX, 12);
            // Resize height if needed
            int neededHeight = tl.getBounds().height + 24;
            tl.dispose();
            if (neededHeight > area.height) {
                ((GridData) this.errorLabel.getLayoutData()).heightHint = neededHeight;
                this.errorLabel.getParent().layout(true, true);
            }
        });

        // Spacer below error (also toggled with error)
        this.errorSpacer = new Label(formPanel, SWT.NONE);
        this.errorSpacer.setBackground(Theme.BG_WHITE);
        var errorSpacerGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        errorSpacerGd.heightHint = 12;
        errorSpacerGd.exclude = true;
        this.errorSpacer.setLayoutData(errorSpacerGd);
        this.errorSpacer.setVisible(false);

        // "Usuário" label
        var userLabel = new Label(formPanel, SWT.NONE);
        userLabel.setText("Usuário");
        userLabel.setFont(Theme.FONT_FIELD_LABEL);
        userLabel.setForeground(Theme.FG_TEXT_DARK);
        userLabel.setBackground(Theme.BG_WHITE);
        userLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        createSpacer(formPanel, 4);

        // User text field (wrapped in border composite)
        this.userNameField = createBorderedTextField(formPanel, SWT.SINGLE, "Digite seu usuário");

        createSpacer(formPanel, 16);

        // "Senha" label
        var passLabel = new Label(formPanel, SWT.NONE);
        passLabel.setText("Senha");
        passLabel.setFont(Theme.FONT_FIELD_LABEL);
        passLabel.setForeground(Theme.FG_TEXT_DARK);
        passLabel.setBackground(Theme.BG_WHITE);
        passLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        createSpacer(formPanel, 4);

        // Password field (wrapped in border composite)
        this.passwordField = createBorderedTextField(formPanel, SWT.SINGLE | SWT.PASSWORD, "Digite sua senha");
        this.passwordField.addListener(SWT.DefaultSelection, _e -> emitEnter());

        createSpacer(formPanel, 24);

        // Login button
        var loginBtn = new Button(formPanel, SWT.PUSH);
        loginBtn.setText("Entrar");
        loginBtn.setFont(Theme.FONT_BUTTON);
        loginBtn.setBackground(Theme.PRIMARY_BLUE);
        loginBtn.setForeground(Theme.FG_TEXT_WHITE);
        var btnGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        btnGd.heightHint = Theme.BUTTON_HEIGHT;
        loginBtn.setLayoutData(btnGd);
        loginBtn.addListener(SWT.Selection, _e -> emitEnter());

        createSpacer(formPanel, 16);

        // Demo hint (gray box with border)
        var demoBox = new Canvas(formPanel, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var demoGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        demoGd.heightHint = 40;
        demoBox.setLayoutData(demoGd);
        demoBox.addPaintListener(ev -> {
            var gc = ev.gc;
            var area = demoBox.getClientArea();
            gc.setAntialias(SWT.ON);
            // Light gray background
            gc.setBackground(Theme.BG_PAGE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 8, 8);
            // Slightly darker border
            gc.setForeground(Theme.BORDER_LIGHT);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 8, 8);
            // Text centered
            gc.setForeground(Theme.FG_TEXT_SUBTLE);
            gc.setFont(Theme.FONT_SUBTITLE);
            String text = "Acesso demo: admin / admin";
            Point size = gc.textExtent(text);
            gc.drawText(text, (area.width - size.x) / 2, (area.height - size.y) / 2, true);
        });

        // Bottom spacer for centering
        var bottomSpacer = new Label(rightPanel, SWT.NONE);
        bottomSpacer.setBackground(Theme.BG_WHITE);
        bottomSpacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    // ========== BANNER CARD ==========

    private void createBannerCard(Composite parent) {
        var banner = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = 120;
        banner.setLayoutData(gd);
        banner.addPaintListener(e -> paintBanner(e, banner));
    }

    private void paintBanner(PaintEvent e, Canvas banner) {
        GC gc = e.gc;
        Rectangle bounds = banner.getClientArea();
        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);

        // Blue gradient background (rounded corners)
        gc.setBackground(Theme.BG_LOGIN_LEFT);
        gc.setForeground(Theme.PRIMARY_BLUE_LIGHT);
        gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 16, 16);

        // Decorative circles
        gc.setAlpha(25);
        gc.setBackground(Theme.FG_TEXT_WHITE);
        gc.fillOval(bounds.width - 80, -40, 160, 160);
        gc.fillOval(-60, bounds.height - 60, 120, 120);
        gc.setAlpha(255);

        // Icon box (rounded square with translucent bg)
        gc.setForeground(Theme.FG_TEXT_WHITE);
        int boxSize = 44;
        int boxX = (bounds.width - boxSize) / 2;
        int boxY = 20;
        gc.setAlpha(30);
        gc.setBackground(Theme.FG_TEXT_WHITE);
        gc.fillRoundRectangle(boxX, boxY, boxSize, boxSize, 10, 10);
        gc.setAlpha(60);
        gc.drawRoundRectangle(boxX, boxY, boxSize, boxSize, 10, 10);
        gc.setAlpha(255);

        // Bag icon inside box
        gc.setForeground(Theme.FG_TEXT_WHITE);
        gc.setFont(Theme.FONT_ICON);
        Point iconSize = gc.textExtent(Theme.ICON_BAG_CHECK);
        gc.drawText(Theme.ICON_BAG_CHECK, boxX + (boxSize - iconSize.x) / 2, boxY + (boxSize - iconSize.y) / 2, true);

        // Title "WDC Shopping"
        gc.setFont(Theme.FONT_BANNER_TITLE);
        String title = "WDC Shopping";
        Point titleSize = gc.textExtent(title);
        int titleY = boxY + boxSize + 10;
        gc.drawText(title, (bounds.width - titleSize.x) / 2, titleY, true);

        // Subtitle
        gc.setAlpha(180);
        gc.setFont(Theme.FONT_BANNER_SUBTITLE);
        String sub = "Sua compra certa na internet.";
        Point subSize = gc.textExtent(sub);
        gc.drawText(sub, (bounds.width - subSize.x) / 2, titleY + titleSize.y + 4, true);
        gc.setAlpha(255);
    }

    // ========== HELPERS ==========

    private void createSpacer(Composite parent, int height) {
        var spacer = new Label(parent, SWT.NONE);
        spacer.setBackground(parent.getBackground());
        var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = height;
        spacer.setLayoutData(gd);
    }

    /**
     * Creates a Text field wrapped in a rounded border composite.
     */
    private Text createBorderedTextField(Composite parent, int textStyle, String placeholder) {
        // Outer composite with custom-painted rounded border
        var borderComp = new Composite(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var borderGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        borderGd.heightHint = 36;
        borderComp.setLayoutData(borderGd);
        var borderLayout = new GridLayout(1, false);
        borderLayout.marginWidth = 10;
        borderLayout.marginHeight = 6;
        borderLayout.horizontalSpacing = 0;
        borderLayout.verticalSpacing = 0;
        borderComp.setLayout(borderLayout);

        // Paint rounded border
        borderComp.addPaintListener(e -> {
            var bounds = borderComp.getClientArea();
            e.gc.setAntialias(SWT.ON);
            // Fill white background
            e.gc.setBackground(Theme.BG_WHITE);
            e.gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 8, 8);
            // Draw border
            e.gc.setForeground(Theme.BORDER_FIELD);
            e.gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, 8, 8);
        });

        // Inner text field (no SWT.BORDER — the rounded wrapper provides the border)
        var field = new Text(borderComp, textStyle);
        field.setMessage(placeholder);
        field.setBackground(Theme.BG_WHITE);
        field.setForeground(Theme.FG_TEXT_DARK);
        field.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        return field;
    }

    private void emitEnter() {
        var userName = this.userNameField.getText();
        var password = this.passwordField.getText();
        safeAction("login", () -> {
            this.presenter.onEnter(userName, password);
        });
    }
}
