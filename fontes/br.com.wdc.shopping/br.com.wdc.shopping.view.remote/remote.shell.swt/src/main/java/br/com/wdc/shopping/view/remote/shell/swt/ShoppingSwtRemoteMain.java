package br.com.wdc.shopping.view.remote.shell.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the SWT Remote Shell.
 * <p>
 * Connects to the WDC Shopping Host server and renders the application using SWT widgets from {@code swt.commons}, driven by remote state.
 *
 * <h3>Usage</h3>
 *
 * <pre>
 *   java -jar remote.shell.swt.jar [server-url]
 *   # default: http://localhost:8080
 * </pre>
 */
public class ShoppingSwtRemoteMain {

	private static final Logger LOG = LoggerFactory.getLogger(ShoppingSwtRemoteMain.class);

	private static final String DEFAULT_SERVER_URL = "http://localhost:8080";
	private static final int WINDOW_WIDTH = 800;
	private static final int WINDOW_HEIGHT = 820;

	public static void main(String[] args) throws Exception {
		String serverUrl = args.length > 0 ? args[0] : DEFAULT_SERVER_URL;
		LOG.info("Starting SWT Remote Shell → {}", serverUrl);

		var display = new Display();
		try {
			var shell = new Shell(display, SWT.SHELL_TRIM);
			shell.setText("WDC Shopping (Remote)");
			shell.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
			shell.setLayout(new StackLayout());

			// Centre on primary monitor
			var monitor = display.getPrimaryMonitor();
			var monitorBounds = monitor.getBounds();
			shell.setLocation(monitorBounds.x + (monitorBounds.width - WINDOW_WIDTH) / 2, monitorBounds.y + (monitorBounds.height - WINDOW_HEIGHT) / 2);

			var app = new ShoppingSwtRemoteApp(display, shell);

			shell.open();

			// Connect & await initial state push (blocks briefly on UI thread before loop)
			try {
				app.connect(serverUrl);
			} catch (Exception e) {
				LOG.error("Failed to connect to {}: {}", serverUrl, e.getMessage(), e);
				// Show empty shell — user will see a blank window
			}

			// Standard SWT event loop
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}

			app.stop();
		} finally {
			display.dispose();
		}

		LOG.info("SWT Remote Shell exited.");
	}
}
