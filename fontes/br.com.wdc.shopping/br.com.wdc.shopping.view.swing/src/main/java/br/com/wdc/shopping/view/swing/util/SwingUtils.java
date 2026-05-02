package br.com.wdc.shopping.view.swing.util;

import java.awt.Component;
import java.awt.Container;

public class SwingUtils {
	
	private SwingUtils() {
	}
	
    public static void replaceContent(Container container, Component child) {
        container.removeAll();
        if (child != null) {
            container.add(child);
        }
        container.revalidate();
        container.repaint();
    }


}
