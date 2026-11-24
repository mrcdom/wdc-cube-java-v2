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
import br.com.wdc.store.domain.StoreConfig;

/**
 * Bootstrap support for <b>JTA transactions</b> (Narayana).
 *
 * <p>
 * Initializes (when requested in {@code application.toml}) the {@link JtaTransactionManager#BEAN} and exposes the mode
 * query ({@link #isJtaActive()}) used by the other persistence supports.
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

    /** @return {@code true} if JTA mode is active (Narayana TransactionManager initialized). */
    public boolean isJtaActive() {
        return JtaTransactionManager.BEAN.get() != null;
    }

    /**
     * Reads {@code database.transaction} and, if it is {@code "jta"}, initializes the Narayana TransactionManager.
     *
     * @return {@code true} if JTA mode was enabled.
     */
    public boolean init(Defer cleanUp) {
        if ("jta".equalsIgnoreCase(txMode)) {
            initJta(cleanUp);
            return true;
        }
        LOG.info("Transaction mode: non-jta (plain JDBC)");
        return false;
    }

    /**
     * Emits a prominent warning if the configuration requests JTA but the TM was not initialized. Indicates a silent
     * failure in the JTA bootstrap or a misconfiguration.
     */
    public void warnIfModeMismatch() {
        boolean jtaRequested = "jta".equalsIgnoreCase(this.txMode);
        if (jtaRequested && !isJtaActive()) {
            LOG.warn("╔════════════════════════════════════════════════════════╗");
            LOG.warn("║  WARNING — WRONG TRANSACTION MODE                      ║");
            LOG.warn("║                                                        ║");
            LOG.warn("║  application.toml:  database.transaction = \"jta\"       ║");
            LOG.warn("║  Active mode:       NON-JTA (plain JDBC)               ║");
            LOG.warn("║                                                        ║");
            LOG.warn("║  The Narayana TransactionManager was not initialized.  ║");
            LOG.warn("║  Transactions are NOT XA — consistency across          ║");
            LOG.warn("║  distributed resources is NOT guaranteed.              ║");
            LOG.warn("║                                                        ║");
            LOG.warn("║  Check the startup logs above for exceptions           ║");
            LOG.warn("║  in initTransactions/initJta.                          ║");
            LOG.warn("╚════════════════════════════════════════════════════════╝");
        }
    }

    private void initJta(Defer cleanUp) {
        // Points the ObjectStore (Narayana's transaction log) at work/data/tx-logs,
        // avoiding the creation of ./ObjectStore in the process's working directory.
        var txFolderName = (StringUtils.isBlank(prefix) ? "" : prefix + "-") + "tx-logs";
        var objectStoreDir = StoreConfig.getDataDir().resolve(txFolderName).toAbsolutePath().toString();
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot("defaultStore");

        // Narayana initializes itself automatically when the static factories are accessed.
        // jtaPropertyManager allows programmatic configuration before first use.
        jtaPropertyManager.getJTAEnvironmentBean()
                .setTransactionManagerClassName(TransactionManagerImple.class.getName());

        jtaPropertyManager.getJTAEnvironmentBean()
                .setUserTransactionClassName(UserTransactionImple.class.getName());

        JtaTransactionManager.BEAN.set(TransactionManager.transactionManager());

        cleanUp.push(() -> JtaTransactionManager.BEAN.set(null));

        var initJta = (StringUtils.isBlank(prefix) ? "" : prefix + ".") + "initJta";
        LOG.info(initJta + ": Narayana TransactionManager initialized (objectStoreDir={})", objectStoreDir);
    }
}
