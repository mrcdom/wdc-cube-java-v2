package br.com.wdc.shopping.view.gluon.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextFlow;

public class GluonDom {

    private Pane currentParent;

    private GluonDom(Pane root) {
        this.currentParent = root;
    }

    public static <T extends Pane> void render(T root, BiConsumer<GluonDom, T> renderer) {
        var dom = new GluonDom(root);
        renderer.accept(dom, root);
    }

    private void addChild(Pane parent, Node child) {
        parent.getChildren().add(child);
    }

    // ---- Container methods ----

    public VBox vbox(Consumer<VBox> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new VBox();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HBox hbox(Consumer<HBox> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new HBox();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public StackPane stackPane(Consumer<StackPane> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new StackPane();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public FlowPane flowPane(Consumer<FlowPane> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new FlowPane();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    // ---- Leaf / control methods ----

    public Label label(Consumer<Label> fnUpdate) {
        var elm = new Label();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public Button button(Consumer<Button> fnUpdate) {
        var elm = new Button();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public TextField textField(Consumer<TextField> fnUpdate) {
        var elm = new TextField();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public PasswordField passwordField(Consumer<PasswordField> fnUpdate) {
        var elm = new PasswordField();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public ImageView imageView(Consumer<ImageView> fnUpdate) {
        var elm = new ImageView();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public TextFlow textFlow(Consumer<TextFlow> fnUpdate) {
        var elm = new TextFlow();
        fnUpdate.accept(elm);
        addChild(this.currentParent, elm);
        return elm;
    }

    public ScrollPane scrollPane(Consumer<ScrollPane> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var content = new VBox();
            var elm = new ScrollPane(content);
            this.currentParent = content;
            fnUpdate.accept(elm);
            addChild(oldParent, elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    // ---- Spacer utilities ----

    public Region hSpacer() {
        var elm = new Region();
        HBox.setHgrow(elm, Priority.ALWAYS);
        addChild(this.currentParent, elm);
        return elm;
    }

    public Region vSpacer() {
        var elm = new Region();
        VBox.setVgrow(elm, Priority.ALWAYS);
        addChild(this.currentParent, elm);
        return elm;
    }

    public Region vSpacer(double height) {
        var elm = new Region();
        elm.setMinHeight(height);
        elm.setPrefHeight(height);
        elm.setMaxHeight(height);
        addChild(this.currentParent, elm);
        return elm;
    }

    public Region hSpacer(double width) {
        var elm = new Region();
        elm.setMinWidth(width);
        elm.setPrefWidth(width);
        elm.setMaxWidth(width);
        addChild(this.currentParent, elm);
        return elm;
    }

    // ---- Utility: add an existing node to current parent ----

    public <T extends Node> T node(T node) {
        addChild(this.currentParent, node);
        return node;
    }

    public SVGPath icon(SVGPath svgIcon) {
        addChild(this.currentParent, svgIcon);
        return svgIcon;
    }
}
