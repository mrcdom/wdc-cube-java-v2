package br.com.wdc.framework.commons.sql;

import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

public interface SqlDataSource extends DataSource {

	public AtomicReference<SqlDataSource> BEAN = new AtomicReference<>();

}
