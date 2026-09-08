package br.com.wdc.shopping.domain.model;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class Product implements KeyedEntity {

    private Long id;

    public Long id() {
        return id;
    }

    public Product withId(Long id) {
        this.id = id;
        return this;
    }

    private String name;

    public String name() {
        return name;
    }

    public Product withName(String name) {
        this.name = name;
        return this;
    }

    private Double price;

    public Double price() {
        return price;
    }

    public Product withPrice(Double price) {
        this.price = price;
        return this;
    }

    private String description;

    public String description() {
        return description;
    }

    public Product withDescription(String description) {
        this.description = description;
        return this;
    }

    private byte[] image;

    public byte[] image() {
        return image;
    }

    public Product withImage(byte[] image) {
        this.image = image;
        return this;
    }

    @Override
    public Long key() {
        return id;
    }

}
