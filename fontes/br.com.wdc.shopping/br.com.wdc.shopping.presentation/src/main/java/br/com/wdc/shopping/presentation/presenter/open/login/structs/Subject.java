package br.com.wdc.shopping.presentation.presenter.open.login.structs;

import java.io.Serializable;

import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public class Subject implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String nickName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String ninkName) {
        this.nickName = ninkName;
    }
    
    public static User projection() {
        var pv = ProjectionValues.INSTANCE;

        var prj = new User();
        prj.id = pv.i64;
        prj.name = pv.str;
        return prj;
    }

    public static Subject create(User src) {
        if (src == null) {
            return null;
        }

        Subject tgt = new Subject();
        tgt.id = src.id;
        tgt.nickName = src.name;
        return tgt;
    }

}
