package br.com.wdc.shopping.domain.model;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class User implements KeyedEntity {

    public Long id;
    public String userName;
    public String password;
    public String name;
    public String roles;

    @Override
    public Long key() {
        return id;
    }

}
