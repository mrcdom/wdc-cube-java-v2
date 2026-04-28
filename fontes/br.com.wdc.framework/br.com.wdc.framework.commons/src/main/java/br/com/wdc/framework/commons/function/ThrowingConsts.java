package br.com.wdc.framework.commons.function;

@SuppressWarnings("java:S1214")
interface ThrowingConsts {
    
    Registration NOOP_REGISTRATION = () -> {
        // NOOP
    };

    ThrowingRunnable NOOP_RUNNABLE = new ThrowingRunnable() {

        @Override
        public void run() {
            // NOOP
        }

        @Override
        public void runThrows() throws Exception {
            // NOOP
        }

    };

    ThrowingFunction<?, ?> NOOP_FUNCTION = new ThrowingFunction<Object, Object>() {

        @Override
        public Object apply(Object t) {
            // NOOP
            return null;
        }

        @Override
        public Object applyThrows(Object t) throws Exception {
            // NOOP
            return null;
        }

    };

    ThrowingBiFunction<?, ?, ?> NOOP_BIFUNCTION = new ThrowingBiFunction<Object, Object, Object>() {

        @Override
        public Object apply(Object t, Object u) {
            // NOOP
            return null;
        }

        @Override
        public Object applyThrows(Object t, Object u) throws Exception {
            // NOOP
            return null;
        }

    };

    ThrowingConsumer<?> NOOP_CONSUMER = new ThrowingConsumer<Object>() {

        @Override
        public void accept(Object t) {
            // NOOP
        }

        @Override
        public void acceptThrows(Object t) throws Exception {
            // NOOP
        }

    };

    ThrowingBiConsumer<?, ?> NOOP_BICONSUMER = new ThrowingBiConsumer<Object, Object>() {

        @Override
        public void accept(Object t, Object u) {
            // NOOP
        }

        @Override
        public void acceptThrows(Object t, Object u) throws Exception {
            // NOOP
        }

    };

    ThrowingSupplier<?> NOOP_SUPPLIER = new ThrowingSupplier<Object>() {

        @Override
        public Object get() {
            // NOOP
            return null;
        }

        @Override
        public Object getThrows() throws Exception {
            // NOOP
            return null;
        }

    };

    ThrowingUnaryOperator<?> NOOP_UNARY_OPERATOR = new ThrowingUnaryOperator<Object>() {

        @Override
        public Object apply(Object t) {
            // NOOP
            return null;
        }

        @Override
        public Object applyThrows(Object t) throws Exception {
            // NOOP
            return null;
        }

    };

}
