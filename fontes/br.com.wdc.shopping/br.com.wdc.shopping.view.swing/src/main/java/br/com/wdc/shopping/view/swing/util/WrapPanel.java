package br.com.wdc.shopping.view.swing.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.Serial;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * A panel that wraps children to the next row when they exceed the available width,
 * similar to JavaFX FlowPane.
 */
public class WrapPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    public WrapPanel() {
        super(new WrapLayout(FlowLayout.LEFT, 12, 12));
    }

    public void setHgap(int hgap) {
        ((WrapLayout) getLayout()).setHgap(hgap);
    }

    public void setVgap(int vgap) {
        ((WrapLayout) getLayout()).setVgap(vgap);
    }

    private static class WrapLayout extends FlowLayout {

        @Serial
        private static final long serialVersionUID = 1L;

        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return computeSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return computeSize(target, false);
        }

        private Dimension computeSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right;
                int x = 0, y = 0, rowHeight = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component c = target.getComponent(i);
                    if (!c.isVisible()) continue;
                    Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();

                    if (x > 0 && x + d.width > maxWidth) {
                        y += rowHeight + getVgap();
                        x = 0;
                        rowHeight = 0;
                    }

                    x += d.width + getHgap();
                    rowHeight = Math.max(rowHeight, d.height);
                }

                y += rowHeight + getVgap();
                return new Dimension(maxWidth + insets.left + insets.right, y + insets.top + insets.bottom);
            }
        }
    }
}
