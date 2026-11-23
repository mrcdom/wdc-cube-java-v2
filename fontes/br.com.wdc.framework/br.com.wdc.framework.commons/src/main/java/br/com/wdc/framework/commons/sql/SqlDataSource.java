package br.com.wdc.framework.commons.sql;

import javax.sql.DataSource;

/**
 * Marks an application {@link DataSource}. There is no global holder: each module receives its {@code DataSource} by
 * injection in the composition root (backend/view/test), not through a shared static {@code BEAN}.
 */
public interface SqlDataSource extends DataSource {

}
