package br.com.wdc.framework.commons.sql;

import javax.sql.DataSource;

/**
 * Marca um {@link DataSource} da aplicação. O holder global foi removido: cada módulo recebe seu {@code DataSource} por
 * injeção no composition root (backend/view/teste), em vez de um {@code BEAN} estático compartilhado.
 */
public interface SqlDataSource extends DataSource {

}
