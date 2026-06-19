package br.com.wdc.cube.backend.supports;

import org.apache.commons.lang3.StringUtils;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.TransactionManager;
import com.arjuna.ats.jta.common.jtaPropertyManager;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.config.AppConfig;
import br.com.wdc.framework.persistence.transaction.JtaTransactionManager;
import br.com.wdc.shopping.domain.ShoppingConfig;

/**
 * Suporte de bootstrap das <b>transações JTA</b> (Narayana).
 *
 * <p>
 * Inicializa (quando solicitado em {@code application.toml}) o {@link JtaTransactionManager#BEAN} e expõe a consulta de
 * modo ({@link #isJtaActive()}) usada pelos demais suportes de persistência.
 * </p>
 */
public final class JtaSupport {

    private static final Log LOG = Log.getLogger(JtaSupport.class);

    private String prefix;
    private String txMode;

    public JtaSupport(String prefix, AppConfig config) {
        this.prefix = prefix;
        this.txMode = config.get(prefix + "database.transaction", "non-jta");
    }

    /** @return {@code true} se o modo JTA estiver ativo (TransactionManager Narayana inicializado). */
    public boolean isJtaActive() {
        return JtaTransactionManager.BEAN.get() != null;
    }

    /**
     * Lê {@code database.transaction} e, se for {@code "jta"}, inicializa o TransactionManager Narayana.
     *
     * @return {@code true} se o modo JTA foi ativado.
     */
    public boolean init(Defer cleanUp) {
        if ("jta".equalsIgnoreCase(txMode)) {
            initJta(cleanUp);
            return true;
        }
        LOG.info("Modo de transação: non-jta (JDBC direto)");
        return false;
    }

    /**
     * Emite aviso proeminente caso a configuração solicite JTA mas o TM não tenha sido inicializado. Indica falha
     * silenciosa no bootstrap JTA ou misconfiguration.
     */
    public void warnIfModeMismatch() {
        boolean jtaRequested = "jta".equalsIgnoreCase(this.txMode);
        if (jtaRequested && !isJtaActive()) {
            LOG.warn("╔════════════════════════════════════════════════════════╗");
            LOG.warn("║  ATENÇÃO — MODO DE TRANSAÇÃO INCORRETO                 ║");
            LOG.warn("║                                                        ║");
            LOG.warn("║  application.toml:  database.transaction = \"jta\"     ║");
            LOG.warn("║  Modo ativo:        NON-JTA (JDBC direto)              ║");
            LOG.warn("║                                                        ║");
            LOG.warn("║  O TransactionManager Narayana não foi inicializado.   ║");
            LOG.warn("║  Transações NÃO são XA — consistência entre recursos   ║");
            LOG.warn("║  distribuídos NÃO está garantida.                      ║");
            LOG.warn("║                                                        ║");
            LOG.warn("║  Verifique os logs de inicialização acima em busca de  ║");
            LOG.warn("║  exceções em initTransactions/initJta.                 ║");
            LOG.warn("╚════════════════════════════════════════════════════════╝");
        }
    }

    private void initJta(Defer cleanUp) {
        // Direciona o ObjectStore (log de transações do Narayana) para work/data/tx-logs,
        // evitando criar ./ObjectStore no diretório de trabalho do processo.
        var txFolderName = (StringUtils.isBlank(prefix) ? "" : prefix + "-") + "tx-logs";
        var objectStoreDir = ShoppingConfig.getDataDir().resolve(txFolderName).toAbsolutePath().toString();
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot("defaultStore");

        // Narayana inicializa-se automaticamente ao acessar as factories estáticas.
        // jtaPropertyManager permite configuração programática antes do primeiro uso.
        jtaPropertyManager.getJTAEnvironmentBean()
                .setTransactionManagerClassName(TransactionManagerImple.class.getName());

        jtaPropertyManager.getJTAEnvironmentBean()
                .setUserTransactionClassName(UserTransactionImple.class.getName());

        JtaTransactionManager.BEAN.set(TransactionManager.transactionManager());

        cleanUp.push(() -> JtaTransactionManager.BEAN.set(null));

        var initJta = (StringUtils.isBlank(prefix) ? "" : prefix + ".") + "initJta";
        LOG.info(initJta + ": TransactionManager Narayana inicializado (objectStoreDir={})", objectStoreDir);
    }
}
