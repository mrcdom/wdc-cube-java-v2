package br.com.wdc.shopping.domain.model;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class Product implements KeyedEntity {

    public Long id;
    public String name;
    public Double price;
    public String description;
    public byte[] image;

    @Override
    public Long key() {
        return id;
    }

}
