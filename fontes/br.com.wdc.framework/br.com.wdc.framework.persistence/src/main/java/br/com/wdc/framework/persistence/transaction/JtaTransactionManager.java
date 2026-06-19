package br.com.wdc.framework.persistence.transaction;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.transaction.TransactionManager;

/**
 * Holder estático do {@link TransactionManager} JTA usado pela aplicação.
 *
 * <p>
 * Inicializado pelo bootstrap de transações da aplicação (ex.: {@code JtaSupport}, no módulo backend) durante a subida.
 * Acessado por {@link TransactionScope} para demarcação de transações JTA.
 * </p>
 */
public final class JtaTransactionManager {

    public static final AtomicReference<TransactionManager> BEAN = new AtomicReference<>();

    private JtaTransactionManager() {
        // NOOP
    }

    /**
     * Retorna o {@link TransactionManager} configurado.
     *
     * @throws IllegalStateException se o bootstrap ainda não foi executado
     */
    public static TransactionManager get() {
        var tm = BEAN.get();
        if (tm == null) {
            throw new IllegalStateException(
                    "JtaTransactionManager não foi inicializado — verifique o bootstrap JTA (JtaSupport.init)");
        }
        return tm;
    }
}
