package br.com.wdc.shopping.view.vaadin.util;

import java.io.ByteArrayInputStream;

import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import br.com.wdc.shopping.domain.product.ProductRepository;

public class ResourceCatalog {

    private static final String IMAGE_PNG = "image/png";

    private ResourceCatalog() {
        super();
    }

    public static DownloadHandler getImageResource(String resourceId) {
        if (resourceId != null && resourceId.startsWith("image/product/") && resourceId.endsWith(".png")) {
            return loadProductImageResource(resourceId);
        }
        return DownloadHandler.fromInputStream(event -> {
            var stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/" + resourceId);
            if (stream == null) {
                stream = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/images/no-image-found.png");
            }
            return new DownloadResponse(stream, resourceId, IMAGE_PNG, -1);
        }, resourceId);
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

    private static DownloadHandler loadProductImageResource(String url) {
        var beforePngIdx = url.indexOf(".png");
        if (beforePngIdx == -1) {
            return getImageResource("images/no-image-found.png");
        }
        var lastSlashIdx = url.lastIndexOf('/');
        try {
            var productId = Long.parseLong(url.substring(lastSlashIdx + 1, beforePngIdx));
            var fileName = "product-" + productId + ".png";
            return DownloadHandler.fromInputStream(event -> {
                var bytes = ProductRepository.BEAN.get().fetchImage(productId);
                if (bytes != null) {
                    return new DownloadResponse(new ByteArrayInputStream(bytes), fileName, IMAGE_PNG, bytes.length);
                }
                var fallback = ResourceCatalog.class.getResourceAsStream("/META-INF/resources/images/no-image-found.png");
                return new DownloadResponse(fallback, fileName, IMAGE_PNG, -1);
            }, fileName);
        } catch (Exception e) {
            return getImageResource("images/no-image-found.png");
        }
    }
}
