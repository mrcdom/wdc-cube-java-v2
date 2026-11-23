package br.com.wdc.shopping.test.mock.viewimpl;

import org.apache.commons.lang3.mutable.MutableInt;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public abstract class AbstractViewMock<S extends Scope> implements CubeView {

	public static final MutableInt INSTANCE_ID_GEN = new MutableInt();

	/*
	 * Fields
	 */

	public String instanceId;

	public boolean released;

	public final ShoppingApplicationMock app;

	public final S state;

	/*
	 * Constructor
	 */

	protected AbstractViewMock(ShoppingApplicationMock app, S state) {
		this.app = app;
		this.state = state;
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
