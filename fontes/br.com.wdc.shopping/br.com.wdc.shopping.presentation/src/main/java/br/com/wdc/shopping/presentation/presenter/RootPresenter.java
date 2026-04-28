package br.com.wdc.shopping.presentation.presenter;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.WrongPlace;

public class RootPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Public Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(RootPresenter.class);

    public static Function<RootPresenter, CubeView> createView;

    // :: Public Instance Fields

    public final RootViewState state = new RootViewState();

    // :: Internal Instance Fields

    private final CubeViewSlot contentSlot;

    // :: Constructor

    public RootPresenter(ShoppingApplication app) {
        super(app);
        this.contentSlot = this::setContentView;
    }

    // :: Cube API

    @Override
    public void release() {
        this.state.contentView = null;
        super.release();
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            this.view = createView.apply(this);
        }

        if (deepest) {
            throw new WrongPlace();
        } else {
            // Does not accept changing user id at URL
            if (this.app.getSubject() != null) {
                intent.setParameter(PlaceParameters.USER_ID, this.app.getSubject().getId());
            }
            intent.setViewSlot(PlaceAttributes.SLOT_OWNER, this.contentSlot);
        }

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        if (this.app.getSubject() != null) {
            intent.setParameter(PlaceParameters.USER_ID, this.app.getSubject().getId());
        }
    }

    // :: Messages

    public void alertUnexpectedError(String message, Exception caught) {
        this.alertUnexpectedError(LOG, message, caught);
    }

    public void alertUnexpectedError(Logger logger, String message, Throwable caught) {
        if (StringUtils.isNotBlank(caught.getMessage())) {
            this.state.errorMessage = message;
        } else {
            this.state.errorMessage = message + ": " + caught.getMessage();
        }
        this.update();
        logger.error(this.state.errorMessage, caught);
    }

    // :: Slots

    private void setContentView(CubeView view) {
        if (this.state.contentView != view) {
            this.state.contentView = view;
            this.update();
        }
    }

}
