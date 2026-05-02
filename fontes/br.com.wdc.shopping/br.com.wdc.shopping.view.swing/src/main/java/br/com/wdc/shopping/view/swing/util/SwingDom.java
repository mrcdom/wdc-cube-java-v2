package br.com.wdc.shopping.view.swing.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

public class SwingDom {

    private Container currentParent;
    private Object pendingConstraints;

    private SwingDom(Container root) {
        this.currentParent = root;
    }

    public static <T extends Container> void render(T root, BiConsumer<SwingDom, T> renderer) {
        var dom = new SwingDom(root);
        renderer.accept(dom, root);
    }

    public SwingDom constraints(Object constraints) {
        this.pendingConstraints = constraints;
        return this;
    }

    private void addChild(Container parent, Component child) {
        if (this.pendingConstraints != null) {
            parent.add(child, this.pendingConstraints);
            this.pendingConstraints = null;
        } else {
            parent.add(child);
        }
    }

    public JPanel vbox(Consumer<JPanel> fnUpdate) {
        var oldParent = this.currentParent;
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        try {
            var elm = new JPanel();
            elm.setLayout(new BoxLayout(elm, BoxLayout.Y_AXIS));
            elm.setOpaque(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.pendingConstraints = savedConstraints;
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public JPanel hbox(Consumer<JPanel> fnUpdate) {
        var oldParent = this.currentParent;
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        try {
            var elm = new JPanel();
            elm.setLayout(new BoxLayout(elm, BoxLayout.X_AXIS));
            elm.setOpaque(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.pendingConstraints = savedConstraints;
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public JPanel gridBagPane(Consumer<JPanel> fnUpdate) {
        var oldParent = this.currentParent;
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        try {
            var elm = new JPanel(new GridBagLayout());
            elm.setOpaque(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.pendingConstraints = savedConstraints;
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public JPanel borderPane(Consumer<JPanel> fnUpdate) {
        var oldParent = this.currentParent;
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        try {
            var elm = new JPanel(new BorderLayout());
            elm.setOpaque(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.pendingConstraints = savedConstraints;
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public JPanel stackPane(Consumer<JPanel> fnUpdate) {
        var oldParent = this.currentParent;
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        try {
            var elm = new StackPanel();
            elm.setOpaque(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.pendingConstraints = savedConstraints;
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public JLabel label(Consumer<JLabel> fnUpdate) {
        var elm = new JLabel();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public JLabel img(Consumer<JLabel> fnUpdate) {
        var elm = new JLabel();
        elm.setHorizontalAlignment(JLabel.CENTER);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public JTextField textField(Consumer<JTextField> fnUpdate) {
        var elm = new JTextField();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public JPasswordField passwordField(Consumer<JPasswordField> fnUpdate) {
        var elm = new JPasswordField();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public JButton button(Consumer<JButton> fnUpdate) {
        var elm = new JButton();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public JTextPane textPane(Consumer<JTextPane> fnUpdate) {
        var elm = new JTextPane();
        elm.setEditable(false);
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public Component hSpacer() {
        var elm = Box.createHorizontalGlue();
        addChild(this.currentParent, elm);
        return elm;
    }

    public Component hSpacer(int width) {
        var elm = Box.createRigidArea(new Dimension(width, 0));
        addChild(this.currentParent, elm);
        return elm;
    }

    public Component vSpacer() {
        var elm = Box.createVerticalGlue();
        addChild(this.currentParent, elm);
        return elm;
    }

    public Component vSpacer(int height) {
        var elm = Box.createRigidArea(new Dimension(0, height));
        addChild(this.currentParent, elm);
        return elm;
    }

    public WrapPanel flowPane(Consumer<WrapPanel> fnUpdate) {
        var oldParent = this.currentParent;
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        try {
            var elm = new WrapPanel();
            elm.setOpaque(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.pendingConstraints = savedConstraints;
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public JScrollPane scrollPane(Consumer<JScrollPane> fnUpdate) {
        var savedConstraints = this.pendingConstraints;
        this.pendingConstraints = null;
        var elm = new JScrollPane();
        elm.setBorder(null);
        elm.getViewport().setOpaque(false);
        elm.setOpaque(false);
        fnUpdate.accept(elm);
        this.pendingConstraints = savedConstraints;
        addChild(this.currentParent, elm);
        return elm;
    }
}
