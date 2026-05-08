package br.com.wdc.shopping.view.teavm.repo;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;

/**
 * Utilitários para converter JsonObject em objetos de domínio sem usar
 * Gson reflection (compatível com TeaVM).
 */
final class JsonModelParser {

    private JsonModelParser() {
    }

    static User parseUser(JsonObject json) {
        var user = new User();
        if (json.has("id") && !json.get("id").isJsonNull()) user.id = json.get("id").getAsLong();
        if (json.has("userName") && !json.get("userName").isJsonNull()) user.userName = json.get("userName").getAsString();
        if (json.has("name") && !json.get("name").isJsonNull()) user.name = json.get("name").getAsString();
        if (json.has("password") && !json.get("password").isJsonNull()) user.password = json.get("password").getAsString();
        if (json.has("roles") && !json.get("roles").isJsonNull()) user.roles = json.get("roles").getAsString();
        return user;
    }

    static List<User> parseUserList(JsonArray array) {
        var list = new ArrayList<User>(array.size());
        for (JsonElement el : array) {
            list.add(parseUser(el.getAsJsonObject()));
        }
        return list;
    }

    static Product parseProduct(JsonObject json) {
        var product = new Product();
        if (json.has("id") && !json.get("id").isJsonNull()) product.id = json.get("id").getAsLong();
        if (json.has("name") && !json.get("name").isJsonNull()) product.name = json.get("name").getAsString();
        if (json.has("price") && !json.get("price").isJsonNull()) product.price = json.get("price").getAsDouble();
        if (json.has("description") && !json.get("description").isJsonNull()) product.description = json.get("description").getAsString();
        return product;
    }

    static List<Product> parseProductList(JsonArray array) {
        var list = new ArrayList<Product>(array.size());
        for (JsonElement el : array) {
            list.add(parseProduct(el.getAsJsonObject()));
        }
        return list;
    }

    static Purchase parsePurchase(JsonObject json) {
        var purchase = new Purchase();
        if (json.has("id") && !json.get("id").isJsonNull()) purchase.id = json.get("id").getAsLong();
        if (json.has("buyDate") && !json.get("buyDate").isJsonNull()) {
            purchase.buyDate = OffsetDateTime.parse(json.get("buyDate").getAsString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (json.has("user") && !json.get("user").isJsonNull()) {
            purchase.user = parseUser(json.get("user").getAsJsonObject());
        }
        if (json.has("items") && !json.get("items").isJsonNull()) {
            purchase.items = parsePurchaseItemList(json.get("items").getAsJsonArray(), purchase);
        }
        return purchase;
    }

    static List<Purchase> parsePurchaseList(JsonArray array) {
        var list = new ArrayList<Purchase>(array.size());
        for (JsonElement el : array) {
            list.add(parsePurchase(el.getAsJsonObject()));
        }
        return list;
    }

    static PurchaseItem parsePurchaseItem(JsonObject json, Purchase parent) {
        var item = new PurchaseItem();
        if (json.has("id") && !json.get("id").isJsonNull()) item.id = json.get("id").getAsLong();
        if (json.has("amount") && !json.get("amount").isJsonNull()) item.amount = json.get("amount").getAsInt();
        if (json.has("price") && !json.get("price").isJsonNull()) item.price = json.get("price").getAsDouble();
        if (json.has("product") && !json.get("product").isJsonNull()) {
            item.product = parseProduct(json.get("product").getAsJsonObject());
        }
        item.purchase = parent;
        return item;
    }

    static List<PurchaseItem> parsePurchaseItemList(JsonArray array, Purchase parent) {
        var list = new ArrayList<PurchaseItem>(array.size());
        for (JsonElement el : array) {
            list.add(parsePurchaseItem(el.getAsJsonObject(), parent));
        }
        return list;
    }

    static JsonObject userToJson(User user) {
        var json = new JsonObject();
        if (user.id != null) json.addProperty("id", user.id);
        if (user.userName != null) json.addProperty("userName", user.userName);
        if (user.name != null) json.addProperty("name", user.name);
        if (user.password != null) json.addProperty("password", user.password);
        if (user.roles != null) json.addProperty("roles", user.roles);
        return json;
    }

    static JsonObject productToJson(Product product) {
        var json = new JsonObject();
        if (product.id != null) json.addProperty("id", product.id);
        if (product.name != null) json.addProperty("name", product.name);
        if (product.price != null) json.addProperty("price", product.price);
        if (product.description != null) json.addProperty("description", product.description);
        return json;
    }

    static JsonObject purchaseToJson(Purchase purchase) {
        var json = new JsonObject();
        if (purchase.id != null) json.addProperty("id", purchase.id);
        if (purchase.user != null) json.add("user", userToJson(purchase.user));
        if (purchase.buyDate != null) json.addProperty("buyDate", purchase.buyDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        if (purchase.items != null) {
            var arr = new JsonArray();
            for (var item : purchase.items) {
                arr.add(purchaseItemToJson(item));
            }
            json.add("items", arr);
        }
        return json;
    }

    static JsonObject purchaseItemToJson(PurchaseItem item) {
        var json = new JsonObject();
        if (item.id != null) json.addProperty("id", item.id);
        if (item.amount != null) json.addProperty("amount", item.amount);
        if (item.price != null) json.addProperty("price", item.price);
        if (item.product != null) json.add("product", productToJson(item.product));
        return json;
    }

    static JsonObject projectionToJson(Object projection) {
        if (projection == null) return null;
        if (projection instanceof User u) return userToJson(u);
        if (projection instanceof Product p) return productToJson(p);
        if (projection instanceof Purchase pu) return purchaseToJson(pu);
        return null;
    }

}
