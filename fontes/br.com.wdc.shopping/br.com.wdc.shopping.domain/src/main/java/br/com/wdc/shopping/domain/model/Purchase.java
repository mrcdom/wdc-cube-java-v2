package br.com.wdc.shopping.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class Purchase implements KeyedEntity {

    public Long id;
    public OffsetDateTime buyDate;
    public User user;

    public List<PurchaseItem> items;

    @Override
    public Long key() {
        return id;
    }

    public Long userId() {
        return user != null ? user.id : null;
    }

}
