package com.amo.websocket.server;

import com.amo.websocket.HandshakeHandler;
import com.amo.websocket.HttpRequest;
import com.amo.websocket.HttpResponse;
import com.amo.websocket.RequestLine;
import com.amo.websocket.api.Endpoint;
import com.amo.websocket.api.Session;
import com.amo.websocket.exception.EndPointAlreadyRegister;

import javax.websocket.CloseReason;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by ayeminoo on 1/7/18.
 */
public class BasicContainer implements com.amo.websocket.api.Container {
    private Map<String, Endpoint> endpointMap = new HashMap<>();
    private Map<Long, Session> sessionMap = new HashMap<>();
    private int port = 80;
    private HandshakeHandler handshakeHandler = new BasicHandshakeHandler();

    @Override
    public void registerEndpoint(String uri, Endpoint endpoint) throws URISyntaxException {
        validateUri(uri);
        if (endpointMap.containsKey(uri)) throw new EndPointAlreadyRegister();
        endpointMap.put(uri, endpoint);
    }

    @Override
    public void unRegisterEndpoint(String uri) {
        endpointMap.remove(uri);
    }

    @Override
    public void registerHandShakeHandler(HandshakeHandler handshakeHandler) {
        this.handshakeHandler = handshakeHandler;
    }

    @Override
    public void close() {
        sessionMap.forEach((k, s) -> {
            if(!s.isClose()){
                s.close();
            }
        });
        endpointMap.forEach((k, e) -> {
            e.onClose(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Server going down"));
        });
    }

    @Override
    public void listen(int port) {
        this.port = port;
        listen();
    }

    @Override
    public void listen() {
        try {
            ServerSocket server = new ServerSocket(port);
            debug("websocket server is listening on port " + port);
            while(true){
                Socket socket = server.accept();

                new Thread(){
                    @Override
                    public void run(){
                        HttpRequest request = null;
                        try {
                            request = createHttpRequest(socket.getInputStream());
                            String uri = request.getRequestLine().getUri();
                            Optional<Endpoint> endpoint = getEndPoint(uri);
                            if(endpoint.isPresent() == false){
                                debug("A client connected but not endpoint registered on ." + uri);
                                return;
                            }
                            HttpResponse response = createHttpResponse(socket.getOutputStream());
                            handshakeHandler.doHandshake(request, response);
                            Optional<Session> session = createSession(socket, endpoint.get());
                            session.ifPresent(s -> {
                                sessionMap.put(s.getSessionId(), s);
                                endpoint.ifPresent((e -> {e.onConnect(s);}));
                            });
                        } catch (IOException e) {
                            e.printStackTrace(debugStream);
                        }
                    }
                }.start();
            }
        } catch (IOException e) {
            e.printStackTrace(debugStream);
        }
    }

    private Optional<Session> createSession(Socket socket, Endpoint endpoint) {
        //create new session
        Session session = null;
        try {
            session = new BasicSession(socket, endpoint);
            session.setWebsocketHandler(new BasicWebsocketHandler(session));
        } catch (IOException e) {
            e.printStackTrace(debugStream);
        }
        return Optional.ofNullable(session);
    }

    private Optional<Endpoint> getEndPoint(String uri) {
        return Optional.ofNullable(endpointMap.get(uri));
    }

    private HttpResponse createHttpResponse(OutputStream outputStream) {
        //todo:
        return new HttpResponse() {
            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }
        };
    }

    private HttpRequest createHttpRequest(InputStream inputStream) {
        //todo:
        return new HttpRequest() {
            @Override
            public RequestLine getRequestLine() {
                return new RequestLine() {
                    @Override
                    public String getMethod() {
                        return "GET";
                    }

                    @Override
                    public String getProtocolVersion() {
                        return "1.1";
                    }

                    @Override
                    public String getUri() {
                        return "/";
                    }
                };
            }

            @Override
            public InputStream getInputStream() {
                return inputStream;
            }
        };
    }

    private void validateUri(String uri) throws URISyntaxException {
        //todo: implement validation logic
    }

    // set this to a print stream if you want debug info
    // sent to it; otherwise, leave it null
    static private PrintStream debugStream = System.out;

    public static PrintStream getDebugStream(){
        return debugStream;
    }
    // we have two versions of this ...
    static public void setDebugStream( PrintStream ps ) {
        debugStream = ps;
    }

    // ... just for convenience
    static public void setDebugStream( OutputStream out ) {
        debugStream = new PrintStream( out );
    }

    // send debug info to the print stream, if there is one
    static public void debug( String s ) {
        if (debugStream != null)
            debugStream.println( s );
    }
}
