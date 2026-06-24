package br.com.wdc.shopping.view.remote.shell.cn1.views;

import java.util.function.Consumer;

import com.codename1.components.InfiniteProgress;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Decor;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;

/**
 * Tela de "marca" (splash) — gradiente azul + logo + título, com um <b>spinner</b> (conectando) ou um
 * ícone de erro + "Tentar novamente". View <b>local</b> (sem vsid/bridge), no padrão não-reativo: a
 * árvore é construída <b>uma vez</b> (spinner e botão sempre presentes) e o {@link #setState(State)}
 * muta logo/status e a <b>visibilidade</b> a partir do {@link State}.
 */
public final class SplashCn1View extends AbstractItemCn1View<SplashCn1View.State> {

    /** Parâmetros do splash: modo erro, mensagem de status e ação de "tentar novamente" (ou {@code null}). */
    public static final class State {
        public final boolean error;
        public final String status;
        public final Runnable onRetry;

        public State(boolean error, String status, Runnable onRetry) {
            this.error = error;
            this.status = status;
            this.onRetry = onRetry;
        }
    }

    private static final Sel sel = Sel.INSTANCE;
    /** Tamanho do glifo do logo (param do FontImage). */
    private static final float LOGO_ICON = 13f;

    private Label logo;
    private Consumer<String> status;
    private Component spinner;
    private Button retry;
    private Runnable currentRetry = () -> { };
    private char currentIcon;

    @Override
    protected Container build() {
        return Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, r) -> {
            Decor.blueWithCircles(r);
            dom.boxY(content -> {
                // logo (ícone branco) centralizado — o glifo é definido no doUpdate (conectando/erro)
                dom.container(new FlowLayout(Component.CENTER), null, box -> logo = dom.label(l -> {
                    l.getAllStyles().setFgColor(Colors.SURFACE);
                    l.getAllStyles().setBgTransparency(0); // Android: Label nativo é branco opaco
                }));

                dom.label(l -> {
                    l.setUIID(sel.HERO_TITLE);
                    l.setText("WDC Shopping");
                });
                dom.spanLabel(s -> {
                    s.setTextUIID(sel.HERO_SUBTITLE);
                    s.setText("Sua compra certa na internet.");
                });

                // spinner: sempre presente, visível só ao conectar (doUpdate)
                dom.container(new FlowLayout(Component.CENTER), null, box -> {
                    InfiniteProgress sp = new InfiniteProgress();
                    sp.setMaterialDesignMode(true);
                    sp.getAllStyles().setFgColor(Colors.SURFACE);
                    sp.getAllStyles().setBgTransparency(0); // Android: UIID nativo do progresso é branco
                    spinner = dom.add(sp, null);
                });

                dom.container(new FlowLayout(Component.CENTER), null,
                        box -> status = Guard.text(dom.label(l -> l.setUIID(sel.SPLASH_STATUS))));

                // botão "tentar novamente": sempre presente, visível só no modo erro (doUpdate)
                dom.container(new FlowLayout(Component.CENTER), null, box -> retry = dom.button(b -> {
                    b.setUIID(sel.SPLASH_RETRY);
                    b.setText("Tentar novamente");
                    b.addActionListener(e -> currentRetry.run());
                }));
            });
        });
    }

    @Override
    protected void doUpdate() {
        State s = data;
        setIcon(s.error ? FontImage.MATERIAL_CLOUD_OFF : FontImage.MATERIAL_SHOPPING_BAG);
        status.accept(s.status);
        Guard.visible(spinner, !s.error);
        currentRetry = s.onRetry != null ? s.onRetry : () -> { };
        Guard.visible(retry, s.onRetry != null);
    }

    /** Troca o glifo do logo só quando muda (re-baking de FontImage re-marca o componente). */
    private void setIcon(char icon) {
        if (icon != currentIcon) {
            currentIcon = icon;
            FontImage.setMaterialIcon(logo, icon, LOGO_ICON);
        }
    }
}
