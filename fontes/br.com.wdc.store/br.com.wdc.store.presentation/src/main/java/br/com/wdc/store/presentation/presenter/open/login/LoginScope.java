package br.com.wdc.store.presentation.presenter.open.login;

import br.com.wdc.framework.cube.ActionOf2;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.i18n.Message;

/** What the login draws ({@link LoginPresenter}): the user name typed, the outcome of the last try and the enter action. */
public class LoginScope extends Scope {

    public static final String CLASS_ID = "c677cda52d14";

    public final Prop<String> userName = prop();
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<Message> errorMessage = prop();
    public final Prop<Boolean> loading = prop(false);

    public ActionOf2<String, String> onEnter = ActionOf2.noop();

}
