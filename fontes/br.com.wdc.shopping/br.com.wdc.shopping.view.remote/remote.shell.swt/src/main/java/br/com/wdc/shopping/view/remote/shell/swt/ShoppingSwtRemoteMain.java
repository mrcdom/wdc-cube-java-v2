package br.com.wdc.shopping.view.remote.shell.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

		// macOS: AWT must be initialized BEFORE SWT's Display, because Display's constructor
		// calls NSApplicationLoad() and takes over the Cocoa event loop. If AWT is initialized
		// after that, Taskbar.setIconImage() has no effect on the Dock icon.
		applyDockIcon();

		var display = new Display();
		try {
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
	 * Sets the macOS Dock icon.
	 *
	 * <p>SWT on macOS requires {@code -XstartOnFirstThread}, which prevents AWT from starting its
	 * own EventDispatchThread on the main thread. Because of this, {@code java.awt.Taskbar} alone
	 * is unreliable — AWT and SWT fight over the Cocoa main thread.
	 *
	 * <p>The reliable approach is to:
	 * <ol>
	 *   <li>Extract the icon PNG to a temp file.</li>
	 *   <li>Set {@code apple.awt.application.icon} <em>before</em> {@code new Display()} — the
	 *       property is read by {@code NSApplicationLoad()} when SWT's Display constructor
	 *       initialises {@code NSApplication}.</li>
	 *   <li>Also attempt {@code Taskbar.setIconImage()} as a secondary fallback.</li>
	 * </ol>
	 */
	private static void applyDockIcon() {
		try {
			var url = ShoppingSwtRemoteMain.class.getResource("/images/app-icon.png");
			if (url == null) return;

			// Extract to a temp file so the native layer can load it by path
			var tempFile = Files.createTempFile("wdc-shopping-icon-", ".png");
			tempFile.toFile().deleteOnExit();
			try (var in = url.openStream()) {
				Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
			}

			// Primary: apple.awt.application.icon is read by NSApplicationLoad()
			// when Display's constructor initialises NSApplication — set it BEFORE new Display()
			System.setProperty("apple.awt.application.icon", tempFile.toAbsolutePath().toString());

			// Secondary: java.awt.Taskbar (calls NSApplication.setApplicationIconImage())
			try {
				var taskbar = java.awt.Taskbar.getTaskbar();
				taskbar.setIconImage(ImageIO.read(url));
			} catch (UnsupportedOperationException ignored) {
				// Platform does not support Taskbar icon
			}
		} catch (Exception e) {
			LOG.debug("Could not set dock icon: {}", e.getMessage());
		}
	}
}
