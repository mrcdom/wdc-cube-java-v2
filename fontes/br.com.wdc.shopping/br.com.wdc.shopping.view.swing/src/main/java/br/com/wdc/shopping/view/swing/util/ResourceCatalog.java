package br.com.wdc.shopping.view.swing.util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.domain.repositories.ProductRepository;

public class ResourceCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCatalog.class);

    private static final Map<String, ImageIcon> IMAGE_MAP = new HashMap<>();
    private static final ImageIcon NO_IMAGE_FOUND;

    static {
        putImage("images/big_logo.png");
        putImage("images/logo.png");
        putImage("images/carrinho.png");
        putImage("images/delet.png");
        NO_IMAGE_FOUND = putImage("images/no-image-found.png");
    }

    private ResourceCatalog() {
        super();
    }

    private static ImageIcon putImage(String uri) {
        var stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/" + uri);
        if (stream == null) {
            LOG.warn("Resource not found: /META-INF/resources/{}", uri);
            return null;
        }
        try {
            var image = new ImageIcon(ImageIO.read(stream));
            IMAGE_MAP.put(uri, image);
            return image;
        } catch (Exception e) {
            LOG.error("Failed to load image: {}", uri, e);
            return null;
        }
    }

    public static ImageIcon getImage(String resourceId) {
        var image = IMAGE_MAP.get(resourceId);
        if (image == null) {
            image = loadProductImage(resourceId);
        }
        if (image == null) {
            image = NO_IMAGE_FOUND;
        }
        return image;
    }

    public static ImageIcon getScaledImage(String resourceId, int width, int height) {
        var icon = getImage(resourceId);
        if (icon == null) {
            return null;
        }
        var scaled = icon.getImage().getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static ImageIcon loadProductImage(String url) {
        if (url != null && url.startsWith("image/product/") && url.endsWith(".png")) {
            var beforePngIdx = url.indexOf(".png");
            if (beforePngIdx == -1) {
                return null;
            }
            try {
                var lastSlashIdx = url.lastIndexOf('/');
                var productId = Long.parseLong(url.substring(lastSlashIdx + 1, beforePngIdx));
                var bytes = ProductRepository.BEAN.get().fetchImage(productId);
                if (bytes != null) {
                    var img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (img != null) {
                        var icon = new ImageIcon(img);
                        IMAGE_MAP.put(url, icon);
                        return icon;
                    }
                }
            } catch (Exception caught) {
                LOG.error("Loading product image from PATH {}", url, caught);
            }
        }
        return null;
    }
}
