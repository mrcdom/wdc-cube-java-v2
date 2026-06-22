package br.com.wdc.shopping.view.remote.shell.cn1.bridge;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;

/**
 * Container que mantém <b>uma única</b> view filha. Encapsula o padrão repetido de "acomodar a view
 * de um {@code xxxViewId} como filho único": resolve o vsid (criação lazy, via o app), re-monta só
 * quando a referência muda e limpa quando vazio.
 *
 * <p>
 * Centraliza o algoritmo (menos repetição e menos erro de programação) e evita
 * {@code removeAll()}/{@code revalidate()} quando nada mudou — só toca a árvore na transição.
 * </p>
 *
 * <p>
 * O guard interno ({@code mountedKey}) pressupõe "mesmo vsid ⇒ mesma instância de view", o que vale
 * dentro do tempo de vida do slot. Os slots aninhados vivem dentro das views e são recriados no
 * resize junto com elas; o slot da <b>raiz</b> vive no {@code form}, que sobrevive ao resize — por
 * isso o app chama {@link #reset()} nele para zerar o guard antes de remontar a árvore recriada.
 * </p>
 */
public final class ViewSlot extends Container {

    private final ShoppingCn1RemoteApp app;

    /** Chave do filho montado: um vsid, ou uma chave local arbitrária; vazio = slot limpo. */
    private String mountedKey = "";

    public ViewSlot(ShoppingCn1RemoteApp app) {
        super(new BorderLayout());
        this.app = app;
    }

    /**
     * Garante que a view de {@code vsid} seja o único filho. Vazio (ou {@code null}) limpa o slot;
     * re-monta só quando o vsid muda. A criação da view é lazy ({@link ShoppingCn1RemoteApp#viewFor}).
     */
    public void mount(String vsid) {
        if (vsid == null || vsid.isEmpty()) {
            setChild("", null);
            return;
        }
        if (vsid.equals(mountedKey)) {
            return;
        }
        AbstractCn1View view = app.viewFor(vsid);
        setChild(vsid, view != null ? view.getElement() : null);
    }

    /**
     * Garante que um componente <b>local</b> (não-vsid; ex.: o painel split da Home) seja o único
     * filho, identificado por {@code key}. Re-monta só quando a chave muda.
     */
    public void mountLocal(String key, Component component) {
        setChild(key, component);
    }

    /**
     * Esquece o filho montado e zera o guard — força o próximo {@link #mount}/{@link #mountLocal} a
     * remontar. Usado quando o app recria as views (resize) mas o slot sobrevive (vive no {@code form}).
     */
    public void reset() {
        mountedKey = "";
        removeAll();
    }

    private void setChild(String key, Component child) {
        if (key.equals(mountedKey)) {
            return;
        }
        mountedKey = key;
        removeAll();
        if (child != null) {
            add(BorderLayout.CENTER, child);
        }
        revalidate();
    }
}
