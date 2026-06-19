package br.com.wdc.shopping.persistence.rest;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;

/**
 * Holder <b>server-side</b> do {@link RemoteTransactionCoordinator} usado pela camada REST.
 *
 * <p>
 * Diferente de {@code ShoppingTransactions} (domínio — guarda o {@code TransactionService} de demarcação que a
 * aplicação usa), este holder pertence ao <b>host REST</b>: o coordenador é um SPI puramente server-side de
 * persistência (mantém transações físicas vivas entre requisições HTTP) e não deve ser visível à camada de
 * apresentação. Por isso vive aqui, e não no domínio.
 * </p>
 *
 * <p>
 * Populado pelo composition root (backend) com uma instância ligada ao {@code DataSource} do módulo; {@code null}
 * fora do servidor (ex.: views locais, testes que não exercitam o coordenador).
 * </p>
 */
public final class RemoteTransactions {

    public static final AtomicReference<RemoteTransactionCoordinator> COORDINATOR = new AtomicReference<>();

    private RemoteTransactions() {
        // NOOP
    }
}
