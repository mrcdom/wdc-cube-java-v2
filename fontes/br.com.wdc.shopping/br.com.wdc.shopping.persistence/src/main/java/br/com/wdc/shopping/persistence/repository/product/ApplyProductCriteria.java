package br.com.wdc.shopping.persistence.repository.product;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.persistence.repository.BaseApplyCriteria;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.EnProduct;
import br.com.wdc.shopping.persistence.sql.SqlList;

public class ApplyProductCriteria extends BaseApplyCriteria {

    // :: Field Arguments
    EnProduct root;
    ProductCriteria criteria;

    public ApplyProductCriteria(BaseCommand cmd) {
        super(cmd);
    }

    @Override
    public void apply(SqlList sql) {
        if (criteria.productId() != null) {
            sql.ln(AND, root.id, EQUAL, param("productId", criteria.productId()));
        }
    }

}
