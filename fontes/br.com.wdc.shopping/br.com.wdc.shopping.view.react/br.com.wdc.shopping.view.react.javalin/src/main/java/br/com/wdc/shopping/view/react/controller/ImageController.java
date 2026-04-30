package br.com.wdc.shopping.view.react.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.domain.repositories.ProductRepository;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class ImageController {

    private static final Logger LOG = LoggerFactory.getLogger(ImageController.class);

    public static void configure(JavalinConfig config) {
        var controller = new ImageController();
        config.routes.get("/image/product/{productId}.png", controller::handle);
    }

    protected void handle(Context ctx) {
        Long productId;
        try {
            productId = Long.parseLong(ctx.pathParam("productId"));
        } catch (Exception e) {
            LOG.error("Parsing productId from URL", e);
            ctx.status(400);
            return;
        }

        byte[] imageBytes;
        try {
            imageBytes = ProductRepository.BEAN.get().fetchImage(productId);
        } catch (Exception caught) {
            LOG.error("Processing image request", caught);
            ctx.status(500);
            return;
        }

        if (imageBytes == null) {
            ctx.status(204);
            return;
        }

        ctx.contentType("image/png");
        ctx.result(imageBytes);
    }

}
