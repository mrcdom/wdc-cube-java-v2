package br.com.wdc.shopping.persistence.sgbd.utils;

import javax.sql.DataSource;

import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.shopping.domain.exception.BusinessException;

public class BaseRepository {

    // :: Internal

    protected static DataSource dataSource() {
        return SqlDataSource.BEAN.get();
    }

    protected BusinessException readException(Exception caught) {
        return BusinessException.wrap("Fetching data from RDBMS", caught);
    }

    protected BusinessException writeException(Exception caught) {
        return BusinessException.wrap("Saving on RDBMS", caught);
    }

}
