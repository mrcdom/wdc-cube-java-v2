package br.com.wdc.shopping.domain.purchase;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.wdc.framework.commons.serialization.KeyedEntity;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.user.User;

public class Purchase implements KeyedEntity {

    private Long id;

    public Long id() {
        return id;
    }

    public Purchase withId(Long id) {
        this.id = id;
        return this;
    }

    private OffsetDateTime buyDate;

    public OffsetDateTime buyDate() {
        return buyDate;
    }

    public Purchase withBuyDate(OffsetDateTime buyDate) {
        this.buyDate = buyDate;
        return this;
    }

    private User user;

    public User user() {
        return user;
    }

    public Purchase withUser(User user) {
        this.user = user;
        return this;
    }

    private List<PurchaseItem> items;

    public List<PurchaseItem> items() {
        return items;
    }

    public Purchase withItems(List<PurchaseItem> items) {
        this.items = items;
        return this;
    }

    @Override
    public Long key() {
        return id;
    }

    public Long userId() {
        return user != null ? user.id() : null;
    }

}
