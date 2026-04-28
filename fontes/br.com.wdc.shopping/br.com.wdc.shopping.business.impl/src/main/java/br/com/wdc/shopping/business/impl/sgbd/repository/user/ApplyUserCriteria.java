package br.com.wdc.shopping.business.impl.sgbd.repository.user;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnUser;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseApplyCriteria;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.shared.criteria.UserCriteria;

public class ApplyUserCriteria extends BaseApplyCriteria {

    EnUser root;
    UserCriteria criteria;
    
    public ApplyUserCriteria(BaseCommand cmd) {
        super(cmd);
    }

    public void apply(SqlList sql) {
        if (criteria.userId() != null) {
            sql.ln(AND, root.id, EQUAL, param("userId", criteria.userId()));
        }

        if (criteria.userName() != null) {
            sql.ln(AND, root.userName, EQUAL, param("userName", criteria.userName()));
        }

        if (criteria.password() != null) {
            var hashedPassword = new BigInteger(md5().digest(criteria.password()
                    .getBytes(StandardCharsets.UTF_8)))
                            .toString(36);
            sql.ln(AND, root.password, EQUAL, param("password", hashedPassword));
        }
    }

    // :: Internal

    private MessageDigest md5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException caught) {
            throw ExceptionUtils.asRuntimeException(caught);
        }
    }
}
