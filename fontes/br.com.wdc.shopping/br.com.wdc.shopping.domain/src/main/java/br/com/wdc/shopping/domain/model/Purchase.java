package br.com.wdc.shopping.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

public class Purchase {

    public Long id;
    public OffsetDateTime buyDate;
    public User user;

    public List<PurchaseItem> items;

}
