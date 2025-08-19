package me.schnitzel.apkbridge.web;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

public class WebServer extends RouterNanoHTTPD {
    private Consumer<Void> callbackEnd;

    public WebServer() {
        super(8080);
    }

    public void start(Consumer<Void> callbackStart, Consumer<Void> callbackEnd) {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            addMappings();
            System.out.println("\nRunning! Point your browsers to http://localhost:8080/ \n");
            callbackStart.accept(null);
            this.callbackEnd = callbackEnd;
        } catch (IOException e) {
            callbackEnd.accept(null);
        }
    }

    @Override
    public void addMappings() {
        super.addMappings();
        addRoute("/", IndexHandler.class);
        addRoute("/dalvik", DalvikHandler.class);
        addRoute("/stop", new GeneralHandler() {
            @Override
            public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
                shutdown();
                return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, MIME_PLAINTEXT,
                        "Shutting down...");
            }
        }.getClass());
    }

    public void shutdown() {
        if (callbackEnd != null) {
            callbackEnd.accept(null);
        }
        stop();
    }
}
