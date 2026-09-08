package br.com.wdc.shopping.domain.model;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class User implements KeyedEntity {

    private Long id;

    public Long id() {
        return id;
    }

    public User withId(Long id) {
        this.id = id;
        return this;
    }

    private String userName;

    public String userName() {
        return userName;
    }

    public User withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    private String password;

    public String password() {
        return password;
    }

    public User withPassword(String password) {
        this.password = password;
        return this;
    }

    private String name;

    public String name() {
        return name;
    }

    public User withName(String name) {
        this.name = name;
        return this;
    }

    private String roles;

    public String roles() {
        return roles;
    }

    public User withRoles(String roles) {
        this.roles = roles;
        return this;
    }

    @Override
    public Long key() {
        return id;
    }

}
