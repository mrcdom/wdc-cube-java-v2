package br.com.wdc.shopping.view.vaadin.util;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.server.StreamResource;

import br.com.wdc.shopping.domain.repositories.ProductRepository;

public class ResourceCatalog {

    private ResourceCatalog() {
        super();
    }

    public static StreamResource getImageResource(String resourceId) {
        if (resourceId != null && resourceId.startsWith("image/product/") && resourceId.endsWith(".png")) {
            return loadProductImageResource(resourceId);
        }
        return new StreamResource(resourceId, () -> {
            var stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/" + resourceId);
            if (stream == null) {
                stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/images/no-image-found.png");
            }
            return stream;
        });
    }

    public static String getImageUrl(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return "images/no-image-found.png";
        }
        if (resourceId.startsWith("image/product/")) {
            return resourceId;
        }
        return resourceId;
    }

    private static StreamResource loadProductImageResource(String url) {
        var beforePngIdx = url.indexOf(".png");
        if (beforePngIdx == -1) {
            return getImageResource("images/no-image-found.png");
        }
        var lastSlashIdx = url.lastIndexOf('/');
        try {
            var productId = Long.parseLong(url.substring(lastSlashIdx + 1, beforePngIdx));
            return new StreamResource("product-" + productId + ".png", () -> {
                var bytes = ProductRepository.BEAN.get().fetchImage(productId);
                if (bytes != null) {
                    return new ByteArrayInputStream(bytes);
                }
                return ResourceCatalog.class.getResourceAsStream("/META-INF/resources/images/no-image-found.png");
            });
        } catch (Exception e) {
            return getImageResource("images/no-image-found.png");
        }
    }
}
