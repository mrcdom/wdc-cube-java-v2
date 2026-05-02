package br.com.wdc.shopping.view.swing.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.io.Serial;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * A panel that stacks all children on top of each other, similar to JavaFX StackPane.
 * Each child fills the entire area. Implements Scrollable so that when placed inside
 * a JScrollPane, it tracks the viewport width (preventing horizontal overflow).
 */
public class StackPanel extends JPanel implements Scrollable {

    @Serial
    private static final long serialVersionUID = 1L;

    public StackPanel() {
        super(new StackLayout());
    }

    private static class StackLayout implements LayoutManager2 {

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // no-op
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            // no-op
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // no-op
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return computeSize(parent, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return computeSize(parent, false);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public void layoutContainer(Container parent) {
            var insets = parent.getInsets();
            int w = parent.getWidth() - insets.left - insets.right;
            int h = parent.getHeight() - insets.top - insets.bottom;
            for (var comp : parent.getComponents()) {
                comp.setBounds(insets.left, insets.top, w, h);
            }
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.5f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.5f;
        }

        @Override
        public void invalidateLayout(Container target) {
            // no-op
        }

        private static Dimension computeSize(Container parent, boolean preferred) {
            var insets = parent.getInsets();
            int maxW = 0, maxH = 0;
            for (var comp : parent.getComponents()) {
                var d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                maxW = Math.max(maxW, d.width);
                maxH = Math.max(maxH, d.height);
            }
            return new Dimension(maxW + insets.left + insets.right, maxH + insets.top + insets.bottom);
        }
    }

    // Scrollable implementation — track viewport width, scroll vertically
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? 20 : 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true; // always match viewport width — no horizontal scrolling
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false; // allow vertical scrolling
    }
}
