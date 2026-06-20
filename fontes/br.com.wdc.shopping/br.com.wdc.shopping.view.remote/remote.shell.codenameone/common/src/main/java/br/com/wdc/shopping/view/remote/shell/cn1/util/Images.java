package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.URLImage;

/** Imagens de produto via {@code /image/product/{id}.png} (download assíncrono + cache em Storage). */
public final class Images {

    private static String baseUrl = "";

    private Images() {
        // NOOP
    }

    public static void setBaseUrl(String url) {
        baseUrl = url != null ? url : "";
    }

    public static Image product(long id, int size) {
        EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(size, size, 0xffeeeeee), false);
        String url = baseUrl + "/image/product/" + id + ".png";
        String storage = "prod_" + id + "_" + size + ".png";
        return URLImage.createToStorage(placeholder, storage, url, URLImage.RESIZE_SCALE_TO_FILL);
    }
}
