package br.com.wdc.store.persistence.rest.security;

import java.util.Set;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.policy.DataPolicy;
import br.com.wdc.framework.domain.security.policy.EntityDescriptor;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.product.ProductCriteria;
import br.com.wdc.store.domain.security.Role;

/**
 * Who may do what with the products through the REST API.
 *
 * <ul>
 * <li><b>Every role</b> reads the catalog — every product, every field.</li>
 * <li><b>ADMIN</b> and <b>MANAGER</b> create, update and delete products, image included.</li>
 * </ul>
 *
 * <p>
 * The image is not in the JSON: it is read through its own public endpoint, and written through another one, which
 * checks it as an update of the {@code image} field.
 * </p>
 */
public final class ProductDataPolicy {

    private static final ProjectionValues PV = ProjectionValues.INSTANCE;

    private ProductDataPolicy() {
    }

    // @formatter:off
    public static final EntityDescriptor<Product, ProductCriteria> PRODUCT = EntityDescriptor.<Product, ProductCriteria>builder("product")
            .entity(Product::new)
            .criteria(ProductCriteria::new)
            .projection(ProductCriteria::projection, ProductCriteria::withProjection)
            .anyOf(ProductCriteria::anyOf, ProductCriteria::withAnyOf)
            .key("id", Product::id, Product::withId, PV.i64)
            .field("name", Product::name, Product::withName, PV.str)
            .field("price", Product::price, Product::withPrice, PV.f64)
            .field("description", Product::description, Product::withDescription, PV.str)
            .field("image", Product::image, Product::withImage, PV.bin)
            .criterion("productId", "id")
            .orderings(ProductDataPolicy::orderingFields)
            .build();
    // @formatter:on

    public static final DataPolicy<Product, ProductCriteria> POLICY = DataPolicy.on(PRODUCT)
            .grant(Role.ADMIN.name(), g -> g
                    .read(catalog())
                    .create(editable())
                    .update(editable())
                    .delete())
            .grant(Role.MANAGER.name(), g -> g
                    .read(catalog())
                    .create(editable())
                    .update(editable())
                    .delete())
            .grant(Role.CUSTOMER.name(), g -> g
                    .read(catalog()))
            .build();

    private static Product catalog() {
        return new Product()
                .withName(PV.str)
                .withPrice(PV.f64)
                .withDescription(PV.str);
    }

    private static Product editable() {
        return catalog()
                .withImage(PV.bin);
    }

    /** The fields each provisioned ordering sorts by; the tie-breaker is the key, which is not checked. */
    private static Set<String> orderingFields(ProductCriteria criteria) {
        if (criteria.orderBy() == null) {
            return Set.of();
        }
        return switch (criteria.orderBy()) {
        case OLDEST_FIRST, NEWEST_FIRST -> Set.of();
        case NAME_A_TO_Z -> Set.of("name");
        case CHEAPEST_FIRST, MOST_EXPENSIVE_FIRST -> Set.of("price");
        };
    }
}
