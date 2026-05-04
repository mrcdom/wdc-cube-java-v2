package br.com.wdc.shopping.view.robovm.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIImageView;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewContentMode;

/**
 * Declarative builder for UIKit view hierarchies, analogous to SwingDom.
 * <p>
 * Manages a current parent view and provides factory methods that:
 * <ul>
 *   <li>Create UIKit elements (UIView, UILabel, UIButton, etc.)</li>
 *   <li>Pass them to a callback for configuration</li>
 *   <li>Automatically add them to the current parent</li>
 * </ul>
 * <p>
 * Container methods (vbox, hbox, overlay) temporarily change the current parent,
 * enabling hierarchical composition through natural code indentation.
 * <p>
 * Layout strategy:
 * <ul>
 *   <li><b>vbox</b>: stacks children vertically; each child's y = sum of previous heights</li>
 *   <li><b>hbox</b>: stacks children horizontally; each child's x = sum of previous widths</li>
 *   <li><b>overlay</b>: all children share (0,0) origin (like StackPanel)</li>
 *   <li><b>absolute</b>: no automatic positioning, children use setFrame manually</li>
 * </ul>
 * After the callback completes, the container is sized to fit its children.
 */
public class UIKitDom {

    private UIView currentParent;
    private LayoutMode currentLayout;

    private UIKitDom(UIView root, LayoutMode layout) {
        this.currentParent = root;
        this.currentLayout = layout;
    }

    // ── Entry point ──────────────────────────────────────────────

    public static <T extends UIView> void render(T root, BiConsumer<UIKitDom, T> renderer) {
        render(root, LayoutMode.ABSOLUTE, renderer);
    }

    public static <T extends UIView> void render(T root, LayoutMode layout, BiConsumer<UIKitDom, T> renderer) {
        var dom = new UIKitDom(root, layout);
        renderer.accept(dom, root);
        if (layout == LayoutMode.VBOX || layout == LayoutMode.HBOX) {
            dom.fitParent(root, layout);
        }
    }

    // ── Layout modes ─────────────────────────────────────────────

    public enum LayoutMode {
        VBOX,
        HBOX,
        OVERLAY,
        ABSOLUTE
    }

    // ── Container methods ────────────────────────────────────────

    public UIView vbox(double width, Consumer<UIView> fnUpdate) {
        return container(LayoutMode.VBOX, width, 0, fnUpdate);
    }

    public UIView hbox(double width, Consumer<UIView> fnUpdate) {
        return container(LayoutMode.HBOX, width, 0, fnUpdate);
    }

    public UIView overlay(double width, double height, Consumer<UIView> fnUpdate) {
        return container(LayoutMode.OVERLAY, width, height, fnUpdate);
    }

    public UIView absolute(double width, double height, Consumer<UIView> fnUpdate) {
        return container(LayoutMode.ABSOLUTE, width, height, fnUpdate);
    }

    public UIScrollView scrollView(double width, double height, LayoutMode innerLayout, Consumer<UIScrollView> fnUpdate) {
        var oldParent = this.currentParent;
        var oldLayout = this.currentLayout;
        try {
            var elm = new UIScrollView(new CGRect(0, 0, width, height));
            this.currentParent = elm;
            this.currentLayout = innerLayout;
            fnUpdate.accept(elm);
            if (innerLayout == LayoutMode.VBOX || innerLayout == LayoutMode.HBOX) {
                var contentSize = computeContentSize(elm, innerLayout);
                elm.setContentSize(contentSize);
            }
            this.currentParent = oldParent;
            this.currentLayout = oldLayout;
            addChild(oldParent, oldLayout, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
            this.currentLayout = oldLayout;
        }
    }

    private UIView container(LayoutMode layout, double width, double height, Consumer<UIView> fnUpdate) {
        var oldParent = this.currentParent;
        var oldLayout = this.currentLayout;
        try {
            var elm = new UIView(new CGRect(0, 0, width, height));
            this.currentParent = elm;
            this.currentLayout = layout;
            fnUpdate.accept(elm);
            fitParent(elm, layout);
            this.currentParent = oldParent;
            this.currentLayout = oldLayout;
            addChild(oldParent, oldLayout, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
            this.currentLayout = oldLayout;
        }
    }

    // ── Leaf methods ─────────────────────────────────────────────

    public UILabel label(double width, double height, Consumer<UILabel> fnUpdate) {
        var elm = new UILabel(new CGRect(0, 0, width, height));
        fnUpdate.accept(elm);
        addChild(this.currentParent, this.currentLayout, elm);
        return elm;
    }

    public UITextField textField(double width, double height, Consumer<UITextField> fnUpdate) {
        var elm = new UITextField(new CGRect(0, 0, width, height));
        fnUpdate.accept(elm);
        addChild(this.currentParent, this.currentLayout, elm);
        return elm;
    }

    public UIButton button(UIButtonType type, double width, double height, Consumer<UIButton> fnUpdate) {
        var elm = new UIButton(type);
        elm.setFrame(new CGRect(0, 0, width, height));
        fnUpdate.accept(elm);
        addChild(this.currentParent, this.currentLayout, elm);
        return elm;
    }

    public UIButton button(double width, double height, Consumer<UIButton> fnUpdate) {
        return button(UIButtonType.System, width, height, fnUpdate);
    }

    public UIImageView imageView(double width, double height, Consumer<UIImageView> fnUpdate) {
        var elm = new UIImageView(new CGRect(0, 0, width, height));
        elm.setContentMode(UIViewContentMode.ScaleAspectFit);
        fnUpdate.accept(elm);
        addChild(this.currentParent, this.currentLayout, elm);
        return elm;
    }

    public UIView separator(double width, double thickness) {
        var elm = new UIView(new CGRect(0, 0, width, thickness));
        elm.setBackgroundColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
        addChild(this.currentParent, this.currentLayout, elm);
        return elm;
    }

    public UIView spacer(double size) {
        var elm = new UIView(new CGRect(0, 0, size, size));
        elm.setUserInteractionEnabled(false);
        addChild(this.currentParent, this.currentLayout, elm);
        return elm;
    }

    /**
     * Add a pre-built view (not created by UIKitDom) into the current parent.
     */
    public <T extends UIView> T embed(T view) {
        addChild(this.currentParent, this.currentLayout, view);
        return view;
    }

    // ── Layout engine ────────────────────────────────────────────

    private void addChild(UIView parent, LayoutMode layout, UIView child) {
        switch (layout) {
            case VBOX: {
                double yOffset = computeVOffset(parent);
                var frame = child.getFrame();
                child.setFrame(new CGRect(frame.getX(), yOffset, frame.getWidth(), frame.getHeight()));
                break;
            }
            case HBOX: {
                double xOffset = computeHOffset(parent);
                var frame = child.getFrame();
                child.setFrame(new CGRect(xOffset, frame.getY(), frame.getWidth(), frame.getHeight()));
                break;
            }
            case OVERLAY:
            case ABSOLUTE:
                // No repositioning
                break;
        }
        parent.addSubview(child);
    }

    private double computeVOffset(UIView parent) {
        double maxY = 0;
        var subviews = parent.getSubviews();
        if (subviews != null) {
            for (var sub : subviews) {
                double bottom = sub.getFrame().getY() + sub.getFrame().getHeight();
                if (bottom > maxY) maxY = bottom;
            }
        }
        return maxY;
    }

    private double computeHOffset(UIView parent) {
        double maxX = 0;
        var subviews = parent.getSubviews();
        if (subviews != null) {
            for (var sub : subviews) {
                double right = sub.getFrame().getX() + sub.getFrame().getWidth();
                if (right > maxX) maxX = right;
            }
        }
        return maxX;
    }

    private void fitParent(UIView parent, LayoutMode layout) {
        var frame = parent.getFrame();
        if (layout == LayoutMode.VBOX) {
            double contentHeight = computeVOffset(parent);
            if (contentHeight > 0 && frame.getHeight() < contentHeight) {
                parent.setFrame(new CGRect(frame.getX(), frame.getY(), frame.getWidth(), contentHeight));
            }
        } else if (layout == LayoutMode.HBOX) {
            double contentWidth = computeHOffset(parent);
            if (contentWidth > 0 && frame.getWidth() < contentWidth) {
                parent.setFrame(new CGRect(frame.getX(), frame.getY(), contentWidth, frame.getHeight()));
            }
        }
    }

    private CGSize computeContentSize(UIView parent, LayoutMode layout) {
        var frame = parent.getFrame();
        if (layout == LayoutMode.VBOX) {
            return new CGSize(frame.getWidth(), computeVOffset(parent));
        } else if (layout == LayoutMode.HBOX) {
            return new CGSize(computeHOffset(parent), frame.getHeight());
        }
        return new CGSize(frame.getWidth(), frame.getHeight());
    }
}
