package br.com.wdc.store.test.mock.viewimpl;

import org.apache.commons.lang3.mutable.MutableInt;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public abstract class AbstractViewMock<S extends Scope> implements CubeView {

	public static final MutableInt INSTANCE_ID_GEN = new MutableInt();

	/*
	 * Fields
	 */

	public String instanceId;

	public boolean released;

	public final StoreApplicationMock app;

	public final S scope;

	/*
	 * Constructor
	 */

	protected AbstractViewMock(StoreApplicationMock app, S scope) {
		this.app = app;
		this.scope = scope;
		this.instanceId = String.valueOf(INSTANCE_ID_GEN.incrementAndGet());
	}

	/*
	 * API
	 */

	@Override
	public void release() {
		this.released = true;
	}

	@Override
	public void update() {
		// NOOP
	}

	@Override
	public String instanceId() {
		return this.instanceId;
	}

}
