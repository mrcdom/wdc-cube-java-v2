package br.com.wdc.shopping.view.jfx.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class JfxDom {

    private Parent currentParent;

    private JfxDom(Parent root) {
        this.currentParent = root;
    }

    public static <T extends Parent> void render(T root, BiConsumer<JfxDom, T> renderer) {
        var dom = new JfxDom(root);
        renderer.accept(dom, root);
    }

    public VBox vbox(Consumer<VBox> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new VBox();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
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
            this.getChildren(oldParent).add(elm);
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
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public TextFlow textFlow(Consumer<TextFlow> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new TextFlow();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Text text(Consumer<Text> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Text();
            this.currentParent = null;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Label label(Consumer<Label> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Label();
            this.currentParent = null;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public ImageView img(Consumer<ImageView> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new ImageView();
            this.currentParent = null;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public TextField textField(Consumer<TextField> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new TextField();
            this.currentParent = null;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public PasswordField passwordField(Consumer<PasswordField> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new PasswordField();
            this.currentParent = null;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Button button(Consumer<Button> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Button();
            this.currentParent = null;
            fnUpdate.accept(elm);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Region hSpacer() {
        var oldParent = this.currentParent;
        try {
            var elm = new Region();
            this.currentParent = null;
            HBox.setHgrow(elm, Priority.ALWAYS);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Region hSpacer(int width) {
        var oldParent = this.currentParent;
        try {
            var elm = new Region();
            this.currentParent = null;
            elm.setMinWidth(width);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Region vSpacer() {
        var oldParent = this.currentParent;
        try {
            var elm = new Region();
            this.currentParent = null;
            VBox.setVgrow(elm, Priority.ALWAYS);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Region vSpacer(int height) {
        var oldParent = this.currentParent;
        try {
            var elm = new Region();
            this.currentParent = null;
            elm.setMinHeight(height);
            this.getChildren(oldParent).add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    private ObservableList<Node> getChildren(Parent parent) {
        if (parent instanceof Pane pane) {
            return pane.getChildren();
        } else if (parent instanceof Group group) {
            return group.getChildren();
        } else {
            throw new UnsupportedOperationException("Parent type not supported: " + parent.getClass().getName());
        }
    }
}
