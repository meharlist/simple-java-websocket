package com.amo.websocket.api;

import com.amo.websocket.FrameReader;
import com.amo.websocket.FrameWriter;
import com.amo.websocket.server.BasicWebsocketHandler;

/**
 * Created by ayeminoo on 1/7/18.
 */
public interface Session {
    long getSessionId();
    void close();
    boolean isClose();
    Endpoint getEndpoint();
    FrameReader getFrameReader();
    void setWebsocketHandler(BasicWebsocketHandler basicWebsocketHandler);

    BasicWebsocketHandler getWebsocketHandler();

    void setMaxBufferSize(int length);
    FrameWriter getFrameWriter();
    int getMaxBufferSize();
}
