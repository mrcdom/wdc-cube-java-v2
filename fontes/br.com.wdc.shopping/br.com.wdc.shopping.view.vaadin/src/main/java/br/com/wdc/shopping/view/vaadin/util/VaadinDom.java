package br.com.wdc.shopping.view.vaadin.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

public class VaadinDom {

    private HasComponents currentParent;

    private VaadinDom(HasComponents root) {
        this.currentParent = root;
    }

    public static <T extends Component & HasComponents> void render(T root, BiConsumer<VaadinDom, T> renderer) {
        var dom = new VaadinDom(root);
        renderer.accept(dom, root);
    }

    public VerticalLayout verticalLayout(Consumer<VerticalLayout> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new VerticalLayout();
            elm.setPadding(false);
            elm.setSpacing(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public HorizontalLayout horizontalLayout(Consumer<HorizontalLayout> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new HorizontalLayout();
            elm.setPadding(false);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Div div(Consumer<Div> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Div();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Span span(Consumer<Span> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Span();
            this.currentParent = elm;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Image image(Consumer<Image> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Image();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
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
            oldParent.add(elm);
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
            oldParent.add(elm);
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
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public FlexLayout flexLayout(Consumer<FlexLayout> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new FlexLayout();
            elm.setFlexWrap(FlexLayout.FlexWrap.WRAP);
            this.currentParent = elm;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Div hSpacer() {
        var oldParent = this.currentParent;
        try {
            var elm = new Div();
            elm.getStyle().set("flex-grow", "1");
            this.currentParent = null;
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Div hSpacer(int widthPx) {
        var oldParent = this.currentParent;
        try {
            var elm = new Div();
            elm.setMinWidth(widthPx + "px");
            this.currentParent = null;
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Div vSpacer() {
        var oldParent = this.currentParent;
        try {
            var elm = new Div();
            elm.getStyle().set("flex-grow", "1");
            this.currentParent = null;
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Div vSpacer(int heightPx) {
        var oldParent = this.currentParent;
        try {
            var elm = new Div();
            elm.setMinHeight(heightPx + "px");
            this.currentParent = null;
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Div scrollable(Consumer<Div> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Div();
            elm.getStyle().set("overflow-y", "auto");
            elm.getStyle().set("flex-grow", "1");
            this.currentParent = elm;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Scroller scroller(Consumer<Scroller> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Scroller();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public H2 h2(Consumer<H2> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new H2();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public H3 h3(Consumer<H3> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new H3();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public H4 h4(Consumer<H4> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new H4();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Paragraph paragraph(Consumer<Paragraph> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Paragraph();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Hr hr() {
        var oldParent = this.currentParent;
        try {
            var elm = new Hr();
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Icon icon(VaadinIcon vaadinIcon, Consumer<Icon> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = vaadinIcon.create();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public Avatar avatar(Consumer<Avatar> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new Avatar();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }

    public IntegerField integerField(Consumer<IntegerField> fnUpdate) {
        var oldParent = this.currentParent;
        try {
            var elm = new IntegerField();
            this.currentParent = null;
            fnUpdate.accept(elm);
            oldParent.add(elm);
            return elm;
        } finally {
            this.currentParent = oldParent;
        }
    }
}
