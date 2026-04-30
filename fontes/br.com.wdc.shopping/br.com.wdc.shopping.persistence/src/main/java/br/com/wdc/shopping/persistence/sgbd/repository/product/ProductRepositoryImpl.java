package br.com.wdc.shopping.persistence.sgbd.repository.product;

import java.util.List;

import br.com.wdc.framework.commons.util.TransactionContext;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseRepository;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public class ProductRepositoryImpl extends BaseRepository implements ProductRepository {

    @Override
    public boolean insert(Product product) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return InsertProductRowCmd.run(tx.connection(), product);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public boolean insertOrUpdate(Product product) {
        try (var tx = TransactionContext.begin(dataSource())) {
            var modified = false;
            if (product.id == null) {
                modified = InsertProductRowCmd.run(tx.connection(), product);
            } else {
                modified = UpdateProductRowCmd.run(tx.connection(), product);
            }
            return modified;
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public boolean update(Product newProduct, Product oldProduct) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return UpdateProductRowCmd.run(tx.connection(), newProduct, oldProduct);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }
    
    @Override
    public int delete(ProductCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return DeleteProductsCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public int count(ProductCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return CountProductsCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public List<Product> fetch(ProductCriteria criteia) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchProductsCmd.byCriteria(tx.connection(), criteia);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public Product fetchById(Long productId, Product projection) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchProductsCmd.byId(tx.connection(), productId, projection);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public byte[] fetchImage(Long productId) {
        try (var tx = TransactionContext.begin(dataSource())) {
            var projection = new Product();
            projection.image = ProjectionValues.INSTANCE.bin;

            var product = FetchProductsCmd.byId(tx.connection(), productId, projection);
            return product.image;
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

}
