package br.com.wdc.shopping.view.swt.impl;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.theme.Surface;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.SwtDom;
import static br.com.wdc.shopping.view.swt.util.GridDataUtils.*;

/**
 * Login view matching the TeaVM web design:
 * - Left panel: blue gradient with branding (WDC Shopping logo + features)
 * - Right panel: white card with login form
 */
public class LoginViewSwt extends AbstractViewSwt {

    public Supplier<LoginViewState> stateSupplier;
    public BiConsumer<String, String> onEnter;

    private boolean notRendered = true;
    private Text userNameField;
    private Text passwordField;
    private Canvas errorLabel;
    private Label errorSpacer;
    private String errorMessage;

    public LoginViewSwt(SwtApp app) {
        super("login", app, createRootComposite(app));
    }

    private static Composite createRootComposite(SwtApp app) {
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
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var s = this.stateSupplier != null ? this.stateSupplier.get() : null;
        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (s != null && s.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = s.errorMessage;
            s.errorCode = 0;
            s.errorMessage = null;
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
        SwtDom.render(this.element, (dom, root) -> {
            // :: Left panel — blue gradient with branding
            dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                canvas.setLayoutData(gdFill(new GridData()));
                canvas.addPaintListener(this::paintLeftPanel);
            });

            // :: Right panel — white form
            dom.col(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, col -> {
                col.setBackground(Theme.BG_WHITE);
                col.setBackgroundMode(SWT.INHERIT_FORCE);
                var colGd = new GridData();
                gdFillV(colGd);
                gdWidth(colGd, 420);
                col.setLayoutData(colGd);
                col.addPaintListener(e -> paintRightPanel(e.gc, col.getClientArea()));
                var layout = (GridLayout) col.getLayout();
                layout.marginWidth = 20;
                layout.verticalSpacing = 0;

                // Top spacer to push form toward vertical center
                dom.label(lbl -> {
                    lbl.setBackground(Theme.BG_WHITE);
                    lbl.setLayoutData(gdFill(new GridData()));
                });

                // Blue banner card
                dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                    var canvasGd = new GridData();
                    gdFillH(canvasGd);
                    gdHeight(canvasGd, 120);
                    canvas.setLayoutData(canvasGd);
                    canvas.addPaintListener(e -> paintBanner(e, canvas));
                });

                dom.spacer(24);

                // Inner form panel (indented relative to banner)
                dom.col(form -> {
                    form.setBackground(Theme.BG_WHITE);
                    form.setLayoutData(gdFillH(new GridData()));
                    var formLayout = (GridLayout) form.getLayout();
                    formLayout.marginWidth = 40;
                    formLayout.verticalSpacing = 0;

                    // "Bem-vindo" title
                    dom.label(lbl -> {
                        lbl.setText("Bem-vindo");
                        lbl.setFont(Theme.FONT_WELCOME);
                        lbl.setForeground(Theme.FG_TEXT_DARK);
                        lbl.setBackground(Theme.BG_WHITE);
                        var lblGd = new GridData();
                        gdLeft(lblGd);
                        gdGrabH(lblGd);
                        lbl.setLayoutData(lblGd);
                    });

                    dom.spacer(6);

                    // Subtitle
                    dom.label(lbl -> {
                        lbl.setText("Entre com suas credenciais para continuar");
                        lbl.setFont(Theme.FONT_SUBTITLE);
                        lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                        lbl.setBackground(Theme.BG_WHITE);
                        var lblGd = new GridData();
                        gdLeft(lblGd);
                        gdGrabH(lblGd);
                        lbl.setLayoutData(lblGd);
                    });

                    dom.spacer(28);

                    // Error alert box (hidden by default)
                    this.errorLabel = dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                        var gd = gdFillH(new GridData());
                        gd.heightHint = 40;
                        gd.verticalIndent = 12;
                        gd.exclude = true;
                        canvas.setLayoutData(gd);
                        canvas.setVisible(false);
                        canvas.addPaintListener(Surface.errorBox(canvas::getClientArea, () -> this.errorMessage));
                    });

                    // Spacer below error (also toggled with error)
                    this.errorSpacer = dom.label(lbl -> {
                        lbl.setBackground(Theme.BG_WHITE);
                        var gd = new GridData();
                        gdFillH(gd);
                        gdHeight(gd, 12);
                        gd.exclude = true;
                        lbl.setLayoutData(gd);
                        lbl.setVisible(false);
                    });

                    // "Usuário" label
                    dom.label(lbl -> {
                        lbl.setText("Usuário");
                        lbl.setFont(Theme.FONT_FIELD_LABEL);
                        lbl.setForeground(Theme.FG_TEXT_DARK);
                        lbl.setBackground(Theme.BG_WHITE);
                        var lblGd = new GridData();
                        gdLeft(lblGd);
                        gdGrabH(lblGd);
                        lbl.setLayoutData(lblGd);
                    });

                    dom.spacer(4);

                    // User text field
                    this.userNameField = createBorderedTextField(dom, SWT.SINGLE, "Digite seu usuário");

                    dom.spacer(16);

                    // "Senha" label
                    dom.label(lbl -> {
                        lbl.setText("Senha");
                        lbl.setFont(Theme.FONT_FIELD_LABEL);
                        lbl.setForeground(Theme.FG_TEXT_DARK);
                        lbl.setBackground(Theme.BG_WHITE);
                        var lblGd = new GridData();
                        gdLeft(lblGd);
                        gdGrabH(lblGd);
                        lbl.setLayoutData(lblGd);
                    });

                    dom.spacer(4);

                    // Password field
                    this.passwordField = createBorderedTextField(dom, SWT.SINGLE | SWT.PASSWORD, "Digite sua senha");
                    this.passwordField.addListener(SWT.DefaultSelection, _e -> emitEnter());

                    dom.spacer(24);

                    // Login button
                    dom.button(btn -> {
                        btn.setText("Entrar");
                        btn.setFont(Theme.FONT_BUTTON);
                        btn.setBackground(Theme.PRIMARY_BLUE);
                        btn.setForeground(Theme.FG_TEXT_WHITE);
                        var btnGd = new GridData();
                        gdFillH(btnGd);
                        gdHeight(btnGd, Theme.BUTTON_HEIGHT);
                        btn.setLayoutData(btnGd);
                        btn.addListener(SWT.Selection, _e -> emitEnter());
                    });

                    dom.spacer(16);

                    // Demo hint (gray box with border)
                    dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                        var canvasGd = new GridData();
                        gdFillH(canvasGd);
                        gdHeight(canvasGd, 40);
                        canvas.setLayoutData(canvasGd);
                        canvas.addPaintListener(ev -> paintDemoBox(ev.gc, canvas.getClientArea()));
                    });
                });

                // Bottom spacer for centering
                dom.label(lbl -> {
                    lbl.setBackground(Theme.BG_WHITE);
                    lbl.setLayoutData(gdFill(new GridData()));
                });
            });
        });

        // Show this view in the Shell's StackLayout
        var shell = this.element.getParent();
        if (shell.getLayout() instanceof org.eclipse.swt.custom.StackLayout stackLayout) {
            stackLayout.topControl = this.element;
            shell.layout(true, true);
        }
    }

    // ========== SURFACES ==========

    private void paintLeftPanel(PaintEvent e) {
        GC gc = e.gc;
        Rectangle bounds = ((Canvas) e.widget).getBounds();
        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);

        // Blue gradient background (matches web: #0d66d0 top → #4da6ff bottom)
        gc.setForeground(Theme.PRIMARY_BLUE);
        gc.setBackground(Theme.BG_LOGIN_LEFT);
        gc.fillGradientRectangle(0, 0, bounds.width, bounds.height, true);

        // Decorative circles (6% and 4% white, matching web)
        gc.setBackground(Theme.FG_TEXT_WHITE);
        gc.setAlpha(15);
        gc.fillOval(bounds.width - 120, -60, 240, 240);
        gc.setAlpha(10);
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

        // Feature list
        int featureY = titleY + titleSize.y + 60;
        String[][] features = {
                { Theme.ICON_SHIELD_CHECK, "  Compra segura" },
                { Theme.ICON_TRUCK, "  Entrega rápida" },
                { Theme.ICON_ARROW_REPEAT, "  Troca garantida" }
        };
        for (String[] feature : features) {
            gc.setFont(Theme.FONT_ICON);
            Point iSize = gc.textExtent(feature[0]);
            int featureX = (bounds.width - 200) / 2;
            gc.drawText(feature[0], featureX, featureY, true);
            gc.setFont(Theme.FONT_BODY);
            gc.drawText(feature[1], featureX + iSize.x, featureY, true);
            featureY += iSize.y + 16;
        }
    }

    private void paintBanner(PaintEvent e, Canvas banner) {
        GC gc = e.gc;
        Rectangle bounds = banner.getClientArea();
        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);

        // Blue gradient background (square corners)
        gc.setBackground(Theme.BG_LOGIN_LEFT);
        gc.setForeground(Theme.PRIMARY_BLUE_LIGHT);
        gc.fillRectangle(0, 0, bounds.width, bounds.height);

        // Decorative circles (6% and 4% white, matching web)
        gc.setBackground(Theme.FG_TEXT_WHITE);
        gc.setAlpha(15);
        gc.fillOval(bounds.width - 80, -40, 160, 160);
        gc.setAlpha(10);
        gc.fillOval(-60, bounds.height - 60, 120, 120);
        gc.setAlpha(255);

        // Icon box
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

    private void paintRightPanel(GC gc, Rectangle area) {
        gc.setBackground(Theme.BG_WHITE);
        gc.fillRectangle(area);
    }

    private void paintDemoBox(GC gc, Rectangle area) {
        Surface.drawOutlinedPanel(gc, area);
        gc.setForeground(Theme.FG_TEXT_SUBTLE);
        gc.setFont(Theme.FONT_SUBTITLE);
        String text = "Acesso demo: admin / admin";
        Point size = gc.textExtent(text);
        gc.drawText(text, (area.width - size.x) / 2, (area.height - size.y) / 2, true);
    }

    // ========== HELPERS ==========

    /**
     * Creates a Text field wrapped in a rounded border composite.
     */
    private Text createBorderedTextField(SwtDom dom, int textStyle, String placeholder) {
        var field = new Text[1];
        dom.col(SWT.DOUBLE_BUFFERED, borderComp -> {
            var borderCompGd = new GridData();
            gdFillH(borderCompGd);
            gdHeight(borderCompGd, 36);
            borderComp.setLayoutData(borderCompGd);
            borderComp.setBackground(Theme.BG_WHITE);
            var borderLayout = (GridLayout) borderComp.getLayout();
            borderLayout.marginWidth = 10;
            borderLayout.marginHeight = 0;
            borderLayout.horizontalSpacing = 0;
            borderLayout.verticalSpacing = 0;

            borderComp.addPaintListener(Surface.borderedField(borderComp::getClientArea));

            dom.text(textStyle, txt -> {
                if (Theme.IS_WIN32) {
                    txt.setData("org.eclipse.swt.internal.win32.use_WS_BORDER", Boolean.FALSE);
                }
                txt.setMessage(placeholder);
                txt.setBackground(Theme.BG_WHITE);
                txt.setForeground(Theme.FG_TEXT_DARK);
                txt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
                field[0] = txt;
            });
        });
        return field[0];
    }

    private void emitEnter() {
        var userName = this.userNameField.getText();
        var password = this.passwordField.getText();
        if (this.onEnter != null) {
            safeAction("login", () -> this.onEnter.accept(userName, password));
        }
    }
}
