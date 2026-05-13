package br.com.wdc.shopping.view.gluon.util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import br.com.wdc.framework.commons.log.Log;

import br.com.wdc.shopping.domain.repositories.ProductRepository;
import javafx.scene.image.Image;

public class ResourceCatalog {

    private static final Log LOG = Log.getLogger(ResourceCatalog.class);

    private static final Map<String, Image> IMAGE_MAP = new HashMap<>();

    static {
        putLocalImage("images/big_logo.png");
        putLocalImage("images/logo.png");
    }

    private ResourceCatalog() {
        super();
    }

    private static void putLocalImage(String uri) {
        var stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/" + uri);
        if (stream == null) {
            LOG.warn("Resource not found: /META-INF/resources/{}", uri);
            return;
        }
        IMAGE_MAP.put(uri, new Image(stream));
    }

    public static Image getImage(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return null;
        }

        var image = IMAGE_MAP.get(resourceId);
        if (image == null) {
            image = loadProductImage(resourceId);
            if (image != null) {
                IMAGE_MAP.put(resourceId, image);
            }
        }
        return image;
    }

    private static Image loadProductImage(String url) {
        if (url != null && url.startsWith("image/product/") && url.endsWith(".png")) {
            try {
                var lastSlashIdx = url.lastIndexOf('/');
                var beforePngIdx = url.indexOf(".png");
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
