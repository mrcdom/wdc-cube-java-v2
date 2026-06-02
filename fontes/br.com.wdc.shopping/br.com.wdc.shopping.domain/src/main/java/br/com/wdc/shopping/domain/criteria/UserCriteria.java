package br.com.wdc.shopping.domain.criteria;

import br.com.wdc.shopping.domain.model.User;

public class UserCriteria {

    // :: Projection

    private User projection;

    public User projection() {
        return projection;
    }

    public UserCriteria withProjection(User projection) {
        this.projection = projection;
        return this;
    }

    // :: Criteria

    private Long userId;

    public Long userId() {
        return userId;
    }

    public UserCriteria withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    private String username;

    public String userName() {
        return username;
    }

    public UserCriteria withUserName(String username) {
        this.username = username;
        return this;
    }

    private String password;

    public String password() {
        return password;
    }

    public UserCriteria withPassword(String password) {
        this.password = password;
        return this;
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public UserCriteria withOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public enum OrderBy {
        ASCENDING,
        DESCENDING
    }

}
