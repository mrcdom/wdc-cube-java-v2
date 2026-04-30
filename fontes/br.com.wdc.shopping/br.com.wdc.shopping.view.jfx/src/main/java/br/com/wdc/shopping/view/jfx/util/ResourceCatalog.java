package br.com.wdc.shopping.view.jfx.util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.business.shared.repositories.ProductRepository;
import javafx.scene.image.Image;

public class ResourceCatalog {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceCatalog.class);

    private static final Map<String, Image> IMAGE_MAP = new HashMap<>();
    private static final Image NO_IMAGE_FOUND;

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

    private static Image putImage(String uri) {
        var stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/" + uri);
        if (stream == null) {
            LOG.warn("Resource not found: /META-INF/resources/{}", uri);
            return null;
        }
        var image = new Image(stream);
        IMAGE_MAP.put(uri, image);
        return image;
    }

    public static Image getImage(String resourceId) {
        var image = IMAGE_MAP.get(resourceId);
        if (image == null) {
            image = loadProductImage(resourceId);
        }
        if (image == null) {
            image = NO_IMAGE_FOUND;
        }
        return image;
    }

    private static Image loadProductImage(String url) {
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
                    return new Image(new ByteArrayInputStream(bytes));
                }
            } catch (Exception caught) {
                LOG.error("Loading product image from PATH {}", url, caught);
            }
        }
        return null;
    }
}
