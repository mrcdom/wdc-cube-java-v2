package br.com.wdc.shopping.domain;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * SPI por módulo: holder do {@link TransactionService} da aplicação Shopping.
 *
 * <p>
 * Populado pelo backend (composition root) com uma instância ligada ao {@code DataSource} <b>deste</b> módulo. Assim
 * cada módulo tem seu próprio serviço de transação — e o fato de o backend compartilhar ou dedicar recursos é
 * transparente para o módulo, que apenas usa {@code ShoppingTransactions.BEAN.get()}.
 * </p>
 */
public final class ShoppingTransactions {

    public static final AtomicReference<TransactionService> BEAN = new AtomicReference<>();

    private ShoppingTransactions() {
        // NOOP
    }
}
