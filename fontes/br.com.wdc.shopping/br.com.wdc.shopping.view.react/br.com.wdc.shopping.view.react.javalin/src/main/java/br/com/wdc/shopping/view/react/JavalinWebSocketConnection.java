package br.com.wdc.shopping.view.react;

import io.javalin.websocket.WsContext;

import br.com.wdc.shopping.view.react.skeleton.spi.WebSocketConnection;

final class JavalinWebSocketConnection implements WebSocketConnection {

    private final WsContext wsContext;

    JavalinWebSocketConnection(WsContext wsContext) {
        this.wsContext = wsContext;
    }

    @Override
    public void sendText(String text) {
        this.wsContext.send(text);
    }
}
