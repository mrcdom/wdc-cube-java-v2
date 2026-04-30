package br.com.wdc.shopping.persistence.sgbd.repository.product;

import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnProduct;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseApplyCriteria;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;

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
