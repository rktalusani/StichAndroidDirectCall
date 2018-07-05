package com.nexmo.sdk.conversation.core.client;

import android.content.Context;
import android.text.TextUtils;

import com.nexmo.sdk.conversation.client.event.network.*;
import com.nexmo.sdk.conversation.config.Defaults;
import com.nexmo.sdk.conversation.core.MapList;
import com.nexmo.sdk.conversation.core.client.request.Request;
import com.nexmo.sdk.conversation.core.persistence.RequestQueueStorage;
import com.nexmo.sdk.conversation.core.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import io.socket.parser.Packet;

import static io.socket.client.Socket.EVENT_CONNECT;
import static io.socket.client.Socket.EVENT_CONNECT_ERROR;
import static io.socket.client.Socket.EVENT_CONNECT_TIMEOUT;
import static io.socket.client.Socket.EVENT_DISCONNECT;
import static io.socket.client.Socket.EVENT_RECONNECT;
import static io.socket.client.Socket.EVENT_RECONNECT_FAILED;

/**
 * Proxy over SocketIO client
 * Handles network state, connections and shutdown.
 *
 * Reconnect attempts 3 times: T1=10 sec, 20sec, 40, 80.. threshold 120s
 * Reconnect no network: wait until network connected and reset T1.
 *
 * ReconnectionDelay (Number) how long to initially wait before attempting a new
 * reconnection (10sec). Affected by +/- randomizationFactor (0.2)
 *
 * @hide
 */

public class Router extends Emitter {
    private static final String TAG = Router.class.getSimpleName();
    public static boolean DEBUG_INCOMING_DATA = false;
    private NetworkState connectionStatus = NetworkState.DISCONNECTED;

    private Map<String, CAPIInternalRequest> pendingRequests = new ConcurrentHashMap<>();
    private MapList<SubscriptionListener> subscriptions = new MapList<>();
    private Socket socket;

    private final NetworkingStateListener connectionEventListener;
    private final boolean persistRequests;
    private final Context context;

    public Router(Context context, NetworkingStateListener connectionEventListener, boolean persistRequests) {
        this.context = context.getApplicationContext();
        this.connectionEventListener = connectionEventListener;
        this.persistRequests = persistRequests;
    }

    public void connect(final String connectionUrl, String path, boolean autoReconnect) throws URISyntaxException {
        IO.Options options = new IO.Options();
        options.forceNew = true;
        options.path = path;
        options.transports = new String[] { WebSocket.NAME };

        if (autoReconnect) {
            options.reconnectionDelay = Defaults.RECONNECT_DELAY;
            options.randomizationFactor = Defaults.RECONNECT_RANDOMIZATION_FACTOR;
            options.reconnectionDelayMax = Defaults.RECONNECT_DELAY_THRESHOLD;
            options.timeout = 20000;
        }
        else
            options.reconnection = false;

        this.socket = IO.socket(connectionUrl, options);
        System.out.println("Socket instance " + socket);

        this.socket.on(EVENT_CONNECT, new NetworkStateHandler(NetworkState.CONNECTED));
        this.socket.on(EVENT_CONNECT_ERROR, new NetworkStateHandler(NetworkState.CONNECT_ERROR));
        this.socket.on(EVENT_CONNECT_TIMEOUT, new NetworkStateHandler(NetworkState.CONNECT_TIMEOUT));
        this.socket.on(EVENT_DISCONNECT, new NetworkStateHandler(NetworkState.DISCONNECTED));
        this.socket.on(EVENT_RECONNECT, new NetworkStateHandler(NetworkState.RECONNECT));
        this.socket.on(EVENT_RECONNECT_FAILED, new NetworkStateHandler(NetworkState.DISCONNECTED));

        if (DEBUG_INCOMING_DATA)
            this.socket.io().on(Manager.EVENT_PACKET, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Packet p = (Packet) args[0];
                    System.err.println(Thread.currentThread().toString() + " PACKET: " + p.data);
                }
            });

        socket.io().on(Manager.EVENT_PACKET, this.listener);

        Log.d(TAG, "Connecting to " + connectionUrl);
        this.socket.connect();
    }


    /**
     * Disconnects socketio, unregisters all listeners (except of network ones),
     * releases socketio. Should be called after proper logout procedure.
     */
    public void shutdown() {
        this.socket.emit(Socket.EVENT_DISCONNECT, "");
        this.socket.io().off();
        this.socket.off();
        this.socket.disconnect();
        off();
    }

    /**
     * Nothing should be emitted directly
     */
    @Deprecated
    @Override
    public Emitter emit(String event, Object... args) {
        return super.emit(event, args);
    }

    /**
     * all methods must switch to request-response model
     */
    @Deprecated
    @Override
    public Emitter once(final String event, final Listener fn) {
        throw new IllegalArgumentException("For request-response you have to use sendRequest method instead");
    }

    /**
     * Send one-off request using request-response model.
     * <br/>
     * Method emits {@link Request#getRequestName()} with data formed by {@link Request#toJson()}
     * and expects response on {@link Request#getTid()}.
     * <br/>
     * If server sent response with event name which is equals to {@link Request#getSuccessEventName()} then
     * {@link CAPIAwareListener#onRawUnprocessResponseData(JSONObject, String, String)} will be called, otherwise (in case of error) -
     * {@link CAPIAwareListener#onError(String, JSONObject, String, String)}
     * <br/>
     * Method guarantees that callback will be called once and only once unless disconnect method was called.
     * @param request
     * @param listener to be called in case success or error cases
     */
    public void sendRequest(Request request, CAPIAwareListener listener) {
        Log.i(TAG, "sendRequest " + request.getRequestName() + ": " + request.toString());
        this.pendingRequests.put(request.getTid(), new CAPIInternalRequest(request, listener));
        if (request.isPersistable())
            updatePersistentStorage();

        if (this.socket!= null)
            this.socket.emit(request.getRequestName(), request.toJson());
    }


    /**
     * Single point to persist commands if need.
     * Might better to make it work in separate thread
     */
    private void updatePersistentStorage() {
        if (!this.persistRequests) return;
        new RequestQueueStorage(this.context, this.pendingRequests.values()).save();
    }

    public void loadAndRemoveAndFlush(Router router, FlushFinishedListener readyToCallUserCallback) {
        if (!this.persistRequests) return;

        RequestQueueStorage storage = new RequestQueueStorage(this.context, this.pendingRequests.values());
        storage.loadAndRemoveAndFlush(router, readyToCallUserCallback);
    }


    public void on(String eventName, SubscriptionListener subscription) {
        this.subscriptions.addIfNotExists(eventName, subscription);
    }

    @Override
    public Emitter off() {
        this.subscriptions.clear();
        return super.off();
    }

    /**
     * Sink for all valid incoming events.
     * Method is responsible for routing
     * @param eventName
     * @param eventData
     */
    private void dispatch(String eventName, JSONObject eventData) {
        String rid = eventData.optString("rid");
        String cid = eventData.optString("cid");

        // Check if we have handler for rid we found
        if (!TextUtils.isEmpty(rid))
            dispatchResponse(eventName, rid, cid, eventData);

        for (SubscriptionListener subscription : subscriptions.getList(eventName)) {
            try {
                subscription.onData(eventName, eventData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (Listener l : listeners(eventName))
            l.call(eventData);
    }

    private void dispatchResponse(String eventName, String tid, String cid, JSONObject data) {
        CAPIInternalRequest request = this.pendingRequests.remove(tid);
        if (request == null) {
            Log.w(TAG, "Got event " + eventName + " for unknown tid: " + tid);
            return;
        }

        if (request.getListener() == null) return;

        if (!request.getRequest().getSuccessEventName().equals(eventName)) {
            // We got unexpected response, assume it's error
            try {
                request.getListener().onError(eventName, data, tid, cid);
            } catch (Throwable throwable) {
                Log.w(TAG, "Got error during error processing ");
                throwable.printStackTrace();
            }
        } else {
            // This is response we expected
            try {
                request.getListener().onRawUnprocessResponseData(data, tid, cid);
            } catch (Throwable throwable) {
                Log.w(TAG, "Got error during response processing ");
                throwable.printStackTrace();
                request.getListener().onError(eventName, data, tid, cid);
            }
        }

        if (request.getRequest().isPersistable())
            updatePersistentStorage();
    }

    private Emitter.Listener listener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String eventName = null;
            JSONObject eventData = null;
            try {
                Packet packet = (Packet) args[0];
                JSONArray data = (JSONArray) packet.data;
                if (data.length() != 2) return; // CAPI returns eventName and json data
                eventName = data.getString(0);
                eventData = data.getJSONObject(1);
            } catch (Throwable ignored) { }
            if (eventName == null || eventData == null) return;
            dispatch(eventName, eventData);
        }
    };


    public synchronized void updateConnectionStatus(NetworkState networkState) {
        this.connectionStatus = networkState;
    }

    public boolean connected() {
        return socket.connected();
    }

    public NetworkState getConnectionStatus() {
        return connectionStatus;
    }

    public Map<String, CAPIInternalRequest> getPendingRequests() {
        return pendingRequests;
    }

    public interface FlushFinishedListener {
        void onFlushFinished();
    }

    private class NetworkStateHandler implements Emitter.Listener {
        private NetworkState state;

        private NetworkStateHandler(NetworkState state) {
            this.state = state;
        }

        @Override
        final public void call(Object... args) {
            Log.d(TAG, "NetworkStateListener: new state: " + state);
            updateConnectionStatus(state);
            if (connectionEventListener != null)
                connectionEventListener.onNetworkingState(state);
        }
    }

}
