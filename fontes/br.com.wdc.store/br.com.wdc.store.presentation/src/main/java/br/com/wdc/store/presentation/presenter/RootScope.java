package br.com.wdc.store.presentation.presenter;

import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.i18n.Message;

/** What the root draws ({@link RootPresenter}): the scope of the presenter below and the unexpected-error message. */
public class RootScope extends Scope {

    public static final String CLASS_ID = "f2d345c4a610";

    /** The scope of the presenter below (the home, the login): the view layer draws it. */
    public final Prop<Scope> content = prop();
    public final Prop<Message> errorMessage = prop();

}
