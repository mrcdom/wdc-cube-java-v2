package br.com.wdc.store.test.mock.viewimpl;

import org.apache.commons.lang3.mutable.MutableInt;

import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

/** What the tests draw a scope with: it holds the scope, so a test reaches the scope's state and actions. */
public abstract class AbstractViewMock<S extends Scope> {

	public static final MutableInt INSTANCE_ID_GEN = new MutableInt();

	public final String instanceId;

	public final StoreApplicationMock app;

	public final S scope;

	protected AbstractViewMock(StoreApplicationMock app, S scope) {
		this.app = app;
		this.scope = scope;
		this.instanceId = String.valueOf(INSTANCE_ID_GEN.incrementAndGet());
	}

}
