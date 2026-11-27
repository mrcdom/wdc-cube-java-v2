package br.com.wdc.store.view.remote.shell.cn1.views.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.util.UITimer;

import br.com.wdc.store.view.remote.shell.cn1.StoreCn1RemoteApp;
import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.store.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.store.view.remote.shell.cn1.i18n.StoreMessages;
import br.com.wdc.store.view.remote.shell.cn1.i18n.Texts;
import br.com.wdc.store.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;
import br.com.wdc.store.view.remote.shell.cn1.util.Json;
import br.com.wdc.store.view.remote.shell.cn1.util.Px;

/**
 * Purchase history panel (classId {@value #CLASS_ID}) — header, paginated purchase list
 * (each item opens the receipt) and page navigation. The server only loads the data after the view
 * reports how many items fit in the available height ({@value #EVT_PAGE_SIZE}); that measurement is
 * redone on every resize and the notification is <b>debounced</b> so as not to flood the backend.
 */
public class PurchasesPanelCn1View extends AbstractCn1View {

    private static final HomeSel sel = HomeSel.INSTANCE;

    public static final String CLASS_ID = "b3c4d5e6f7a8";
    private static final int EVT_OPEN_RECEIPT = 1;
    private static final int EVT_PAGE_CHANGE = 2;
    private static final int EVT_PAGE_SIZE = 3;

    /** Estimated height (mm) of an item before measuring the first rendered one — density-independent. */
    private static final float DEFAULT_ITEM_H_MM = 15f;
    /** Debounce window of the capacity notification (ms). */
    private static final int DEBOUNCE_MS = 250;
    /** Max measurement retries while the panel has no layout yet (≈ 3s at 250ms). */
    private static final int MAX_CAPACITY_RETRIES = 12;

    /** Size (mm) and colors of the arrow icon (the color is "baked" when the icon is created). */
    private static final float CHEVRON_MM = 2.8f;
    private static final int CHEVRON_ENABLED = Colors.TEXT_SECONDARY;
    private static final int CHEVRON_DISABLED = Colors.GRAY_DISABLED;
    private static final int CHEVRON_PRESSED = Colors.ACCENT;

    private Container list;
    private Container pagination;
    private Container pill;
    private Label pageInfo;
    private Label prevBtn;
    private Label nextBtn;
    private final List<PurchaseItemCn1View> items = new ArrayList<>();

    private int totalPages = 1;
    private int lastCapacity = -1;
    private int capacityToken;
    private int capacityRetries;
    private boolean resizeHooked;
    /** Locked height of the arrows (= box height); re-applied after each icon re-bake. */
    private int chevronH;

    public PurchasesPanelCn1View(String vsid, BridgeSession session, StoreCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            r.setUIID(sel.PURCHASES_PANEL);
            // header (icon + title) + hint
            dom.boxY(BorderLayout.NORTH, head -> {
                dom.boxX(row -> {
                    dom.label(l -> {
                        l.setUIID(sel.PURCHASES_HEADER_ICON);
                        FontImage.setMaterialIcon(l, FontImage.MATERIAL_HISTORY, 4f);
                    });
                    dom.label(l -> {
                        l.setUIID(sel.PURCHASES_TITLE);
                        l.setText(Texts.text(StoreMessages.HOME_HISTORY));
                    });
                });
                dom.label(l -> {
                    l.setUIID(sel.PURCHASES_HINT);
                    l.setText(Texts.text(StoreMessages.HOME_HISTORY_HINT));
                });
            });

            // purchase list (scrollable, fills the remaining space)
            list = dom.boxY(BorderLayout.CENTER, l -> l.setScrollableY(true));

            // page navigation — gray pill with arrows and the "x / y" in a white box (like React)
            pagination = dom.container(new FlowLayout(Component.CENTER), BorderLayout.SOUTH, nav -> {
                nav.setUIID(sel.PURCHASE_PAGINATION);
                pill = dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, p -> {
                    p.setUIID(sel.PURCHASE_PAGE_PILL);
                    prevBtn = dom.label(l -> {
                        l.setUIID(sel.PURCHASE_PAGE_BTN);
                        l.addPointerPressedListener(e -> chevronIcon(l, FontImage.MATERIAL_CHEVRON_LEFT, CHEVRON_PRESSED));
                        l.addPointerReleasedListener(e -> {
                            chevronIcon(l, FontImage.MATERIAL_CHEVRON_LEFT, CHEVRON_ENABLED);
                            changePage(-1);
                        });
                    });
                    chevronIcon(prevBtn, FontImage.MATERIAL_CHEVRON_LEFT, CHEVRON_ENABLED);
                    pageInfo = dom.label(l -> l.setUIID(sel.PURCHASE_PAGE_INFO));
                    nextBtn = dom.label(l -> {
                        l.setUIID(sel.PURCHASE_PAGE_BTN);
                        l.addPointerPressedListener(e -> chevronIcon(l, FontImage.MATERIAL_CHEVRON_RIGHT, CHEVRON_PRESSED));
                        l.addPointerReleasedListener(e -> {
                            chevronIcon(l, FontImage.MATERIAL_CHEVRON_RIGHT, CHEVRON_ENABLED);
                            changePage(1);
                        });
                    });
                    chevronIcon(nextBtn, FontImage.MATERIAL_CHEVRON_RIGHT, CHEVRON_ENABLED);
                });
            });
        });
        stylePaginator();
        return root;
    }

    /**
     * Finishing of the "x / y" box that CN1's CSS does not cover: rounded corners + a <b>subtle</b>
     * shadow (box-shadow does not exist in CN1's CSS) via {@link RoundRectBorder}, so the box
     * "floats" on the pill as in React.
     */
    private void stylePaginator() {
        // subtle shadow on the "x / y" box
        RoundRectBorder infoBorder = RoundRectBorder.create()
                .cornerRadius(2.5f)
                .shadowOpacity(45)
                .shadowSpread(0f)
                .shadowBlur(0.8f)
                .shadowY(0.4f);
        pageInfo.getAllStyles().setBorder(infoBorder);
        pageInfo.getAllStyles().setBgColor(Colors.SURFACE);
        pageInfo.getAllStyles().setBgTransparency(255);

        // Rounded pill via Java's RoundRectBorder (a large CSS border-radius reserves extra vertical
        // space — leftover gray). Same mechanism as the box, which stays tight.
        pill.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(2.5f));
        pill.getAllStyles().setBgColor(Colors.BG);
        pill.getAllStyles().setBgTransparency(255);

        // Arrows with EXACTLY the box height: so the pill = box height (no leftover gray)
        // and the arrows are vertically centered with the indicator. The natural height
        // of a Label/Button is larger than the box's (font/touch reserve), so we lock
        // the height here, keeping the icon's natural width.
        chevronH = pageInfo.getPreferredH();
        lockChevronHeight(prevBtn);
        lockChevronHeight(nextBtn);
    }

    /** Locks the arrow height to the box height (setIcon marks it for recalculation, so we re-apply it). */
    private void lockChevronHeight(Label l) {
        if (chevronH > 0) {
            l.setPreferredSize(new Dimension(l.getPreferredW(), chevronH));
        }
    }

    /** (Re)generates the arrow icon with the given color — the color is "baked" when the FontImage is created. */
    private void chevronIcon(Label l, char icon, int color) {
        l.getAllStyles().setFgColor(color);
        FontImage.setMaterialIcon(l, icon, CHEVRON_MM);
        lockChevronHeight(l); // setIcon marked it for recalculation; keeps the height locked (no gray)
        l.repaint();
    }

    /** Enables/disables the arrow: light gray + no click when there is nowhere to go. */
    private void setChevronEnabled(Label l, char icon, boolean enabled) {
        l.setEnabled(enabled);
        chevronIcon(l, icon, enabled ? CHEVRON_ENABLED : CHEVRON_DISABLED);
    }

    @Override
    public void doUpdate() {
        Map<String, Object> scope = scope();
        List<Object> purchases = Json.asList(scope.get("purchases"));
        syncList(list, purchases, items, () -> new PurchaseItemCn1View(this::openReceipt));

        int page = Json.intOf(scope, "page");
        int pageSize = Math.max(1, Json.intOf(scope, "pageSize"));
        int totalCount = Json.intOf(scope, "totalCount");
        totalPages = Math.max(1, (totalCount + pageSize - 1) / pageSize);

        pageInfo.setText((page + 1) + " / " + totalPages);
        setChevronEnabled(prevBtn, FontImage.MATERIAL_CHEVRON_LEFT, page > 0);
        setChevronEnabled(nextBtn, FontImage.MATERIAL_CHEVRON_RIGHT, page < totalPages - 1);
        Guard.visible(pagination, totalCount > 0);

        capacityRetries = 0; // new update: renews the measurement retry budget
        hookResize();
        requestCapacityCompute();
    }

    /** Recomputes the capacity when the form changes size (registers the listener once). */
    private void hookResize() {
        if (resizeHooked) {
            return;
        }
        Form f = list.getComponentForm();
        if (f != null) {
            f.addSizeChangedListener(e -> requestCapacityCompute());
            resizeHooked = true;
        }
    }

    /** Schedules the measurement/notification with debounce — only the last one scheduled within the window runs. */
    private void requestCapacityCompute() {
        Form f = list.getComponentForm();
        if (f == null) {
            return;
        }
        final int my = ++capacityToken;
        new UITimer(() -> {
            if (my == capacityToken) {
                computeCapacityNow();
            }
        }).schedule(DEBOUNCE_MS, false, f);
    }

    /** How many items fit without scrolling — measures the list and the 1st item, like the React panel. */
    private void computeCapacityNow() {
        int h = list.getHeight();
        if (h <= 0) {
            // panel has no layout yet (common on device when opening the tab — slower than the debounce):
            // reschedules instead of giving up, with a cap so it does not loop if it stays invisible.
            if (capacityRetries++ < MAX_CAPACITY_RETRIES) {
                requestCapacityCompute();
            }
            return;
        }
        capacityRetries = 0;
        // actual stride between items (includes the margin) — avoids counting one item too many

        int itemH = Px.mm(DEFAULT_ITEM_H_MM);
        int n = list.getComponentCount();
        if (n >= 2) {
            int stride = list.getComponentAt(1).getY() - list.getComponentAt(0).getY();
            if (stride > 0) {
                itemH = stride;
            }
        } else if (n == 1) {
            int h0 = list.getComponentAt(0).getHeight();
            if (h0 > 0) {
                itemH = h0;
            }
        }
        int capacity = Math.max(1, h / itemH);
        if (capacity != lastCapacity) {
            lastCapacity = capacity;
            Map<String, Object> form = new HashMap<>();
            form.put("p.capacity", capacity);
            submit(EVT_PAGE_SIZE, form);
        }
    }

    private void changePage(int delta) {
        int page = Json.intOf(scope(), "page");
        int target = page + delta;
        if (target < 0 || target > totalPages - 1) {
            return;
        }
        Map<String, Object> form = new HashMap<>();
        form.put("p.page", target);
        submit(EVT_PAGE_CHANGE, form);
    }

    private void openReceipt(long purchaseId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.purchaseId", purchaseId);
        submit(EVT_OPEN_RECEIPT, form);
    }
}
