package br.com.wdc.shopping.view.remote.shell.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
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

	/** Kept alive for the app lifetime — NSApplication retains it after setApplicationIconImage. */
	@SuppressWarnings("unused")
	private static Image dockIconImage;

	public static void main(String[] args) throws Exception {
		String serverUrl = args.length > 0 ? args[0] : DEFAULT_SERVER_URL;
		LOG.info("Starting SWT Remote Shell → {}", serverUrl);

		var display = new Display();
		try {
			// macOS: must be called AFTER new Display() — Display's constructor calls
			// NSApplicationLoad() which reinitialises NSApplication and overrides any
			// icon set via -Xdock:icon or apple.awt.application.icon.
			setDockIcon(display);
			var shell = new Shell(display, SWT.SHELL_TRIM);
			shell.setText("WDC Shopping (Remote)");
			shell.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
			shell.setLayout(new StackLayout());
			applyAppIcons(display, shell);

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

	private static void applyAppIcons(Display display, Shell shell) {
		try (var stream = ShoppingSwtRemoteMain.class.getResourceAsStream("/images/app-icon.png")) {
			if (stream == null) return;
			var base = new ImageData(stream);
			shell.setImages(new Image[]{
				new Image(display, base.scaledTo(16, 16)),
				new Image(display, base.scaledTo(32, 32)),
				new Image(display, base.scaledTo(48, 48)),
				new Image(display, base.scaledTo(256, 256)),
			});
		} catch (Exception e) {
			LOG.debug("Could not load app icon: {}", e.getMessage());
		}
	}

	/**
	 * Sets the macOS Dock icon via {@code NSApplication.setApplicationIconImage()}.
	 *
	 * <p>Must be called <em>after</em> {@code new Display()} — SWT's Display constructor calls
	 * {@code NSApplicationLoad()} which reinitialises {@code NSApplication} and resets any icon
	 * previously set through {@code -Xdock:icon} or {@code apple.awt.application.icon}.
	 *
	 * <p>Uses SWT's own Cocoa internal bindings ({@code org.eclipse.swt.internal.cocoa.*})
	 * via reflection so that no compile-time dependency on internal packages is required.
	 * The created {@code Image} is kept in {@link #dockIconImage} for the app lifetime;
	 * {@code NSApplication} retains the underlying {@code NSImage} after the call.
	 */
	@SuppressWarnings("java:S3011")
	private static void setDockIcon(Display display) {
		try (var stream = ShoppingSwtRemoteMain.class.getResourceAsStream("/images/app-icon.png")) {
			if (stream == null) {
				LOG.debug("app-icon.png not found in classpath");
				return;
			}
			// Keep a static reference — prevents SWT from disposing the underlying NSImage
			dockIconImage = new Image(display, new ImageData(stream).scaledTo(512, 512));

			// Wrap SWT's NSImage* handle in SWT's internal NSImage proxy
			var nsImageClass = Class.forName("org.eclipse.swt.internal.cocoa.NSImage");
			var handleField  = Image.class.getDeclaredField("handle");
			handleField.setAccessible(true);
			var nsImage = nsImageClass.getDeclaredConstructor(long.class)
					.newInstance(handleField.getLong(dockIconImage));

			// NSApplication.sharedApplication().setApplicationIconImage(nsImage)
			var nsAppClass = Class.forName("org.eclipse.swt.internal.cocoa.NSApplication");
			var sharedApp  = nsAppClass.getMethod("sharedApplication").invoke(null);
			nsAppClass.getMethod("setApplicationIconImage", nsImageClass).invoke(sharedApp, nsImage);
		} catch (Exception e) {
			LOG.debug("Could not set Dock icon via NSApplication: {}", e.getMessage());
		}
	}
}
