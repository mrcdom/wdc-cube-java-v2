package br.com.wdc.shopping.backend;

import io.javalin.websocket.WsContext;

import br.com.wdc.shopping.view.remote.wiring.spi.WebSocketConnection;

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
