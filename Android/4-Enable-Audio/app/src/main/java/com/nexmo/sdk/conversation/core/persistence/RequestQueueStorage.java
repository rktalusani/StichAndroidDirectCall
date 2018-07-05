package com.nexmo.sdk.conversation.core.persistence;

import android.content.Context;

import com.nexmo.sdk.conversation.client.event.network.CAPIInternalRequest;
import com.nexmo.sdk.conversation.core.util.Log;

import com.nexmo.sdk.conversation.client.event.network.CAPIAwareListener;
import com.nexmo.sdk.conversation.core.client.Router;
import com.nexmo.sdk.conversation.core.client.request.Request;
import com.nexmo.sdk.conversation.core.util.StreamUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

/**
 * @hide
 */

public class RequestQueueStorage {
    private static final String TAG = RequestQueueStorage.class.getSimpleName();
    private final ArrayList<Request> requests;

    private Context context;

    private static final String FILENAME = "capi-sdk-pending.json";

    public RequestQueueStorage(Context application, final Collection<CAPIInternalRequest> requests) {
        this.context = application.getApplicationContext();
        this.requests = new ArrayList<Request>(requests.size()) {{
            for (CAPIInternalRequest request : requests) add(request.getRequest());
        }};
    }


    public void save() {
        Log.d(TAG, "save called with request size = " + requests.size());
        try {
            FileOutputStream outputStream = this.context.openFileOutput(FILENAME, 0);
            serialize(requests, outputStream);
            outputStream.close();
        } catch (Exception e) {
            Log.w(TAG, "save: couldn't save items", e);
            e.printStackTrace();
        }
    }

    /**
     * Method reads persisted queue with commands and removes file.
     * Hence, this method will success only <b>once</b> per session
     * @return queue or empty queue
     */
    public Deque<RequestHolder> loadAndRemove() {
        Deque<RequestHolder> requestHolders = new LinkedList<>();
        try {
            FileInputStream inputStream = this.context.openFileInput(FILENAME);
            requestHolders = deserialize(inputStream);
            inputStream.close();
        } catch (FileNotFoundException ignoreAsItIsNormalToNotToHavePersistedQueue) {
        } catch (Exception e) {
            Log.w(TAG, "loadAndRemove: can't load anything from persistent storage", e);
        } finally {
            removeFile();
        }
        return requestHolders;
    }

    public void loadAndRemoveAndFlush(Router router, Router.FlushFinishedListener finishedListener) {
        flush(loadAndRemove(), router, finishedListener);
    }

    private void removeFile() {
        try {
            this.context.deleteFile(FILENAME);
        } catch (Exception ignore) {}
    }

    static String serialize(Collection<Request> opsRequests) {
        JSONArray root = new JSONArray();
        ArrayList<Request> requests = new ArrayList<>(opsRequests);
        for (Request request : requests) {
            if (!request.isPersistable()) continue;
            root.put(new RequestHolder(request).toJson());
        }

        return root.toString();
    }

    static Deque<RequestHolder> deserialize(String input) throws JSONException {
        JSONArray array = new JSONArray(input);
        LinkedList<RequestHolder> requestsQueue = new LinkedList<>();
        for (int i = 0, len = array.length(); i < len; i++) {
            RequestHolder requestHolder = RequestHolder.fromJson(array.getJSONObject(i));
            requestsQueue.add(requestHolder);
        }
        return requestsQueue;
    }

    private Deque<RequestHolder> deserialize(InputStream input) throws JSONException, IOException {
        return deserialize(StreamUtils.streamToString(input));
    }


    static void serialize(Collection<Request> requests, OutputStream outputStream) throws IOException {
        outputStream.write(serialize(requests).getBytes());
    }

    static void flush(final Deque<RequestHolder> requests, final Router router, final Router.FlushFinishedListener finishedListener) {
        if (requests.isEmpty()) {
            finishedListener.onFlushFinished();
            return;
        }

        final RequestHolder pop = requests.pop();

        router.sendRequest(pop.syntheticRequest(), new RequestDoneListener() {
            @Override
            public void onDone() {
                Log.d(TAG, "flush for request " + pop.requestName + " done, " + requests.size() + " remain in queue");
                flush(requests, router, finishedListener);
            }
        });
    }

    static class RequestHolder {
        private String requestName;
        private JSONObject networkData;

        RequestHolder(Request request) {
            this(request.getRequestName(), request.toJson());
        }

        private RequestHolder(String requestName, JSONObject networkData) {
            this.requestName = requestName;
            this.networkData = networkData;
        }

        JSONObject toJson() {
            try {
                return new JSONObject()
                        .put("n", requestName)
                        .put("d", networkData);
            } catch (JSONException canNeverHappensKeyNamesAreNotNull) {
                return null;
            }
        }

        JSONObject getNetworkData() {
            return networkData;
        }

        Request syntheticRequest() {
            return new Request(Request.TYPE.OTHER, networkData.optString("tid")) {
                @Override
                public Object parse(JSONObject jsonObject, JSONObject body) throws JSONException {
                    return new Object();
                }

                @Override
                protected JSONObject makeJson() throws JSONException {
                    return networkData;
                }

                @Override
                public String getRequestName() {
                    return requestName;
                }

                @Override
                public String getSuccessEventName() {
                    return "doesn't really matter";
                }
            };
        }

        static RequestHolder fromJson(JSONObject jsonObject) throws JSONException {
            return new RequestHolder(jsonObject.getString("n"), jsonObject.getJSONObject("d"));
        }
    }

    /**
     * Route any response to the done method.
     * To be used when we don't really care about result
     */
    private static abstract class RequestDoneListener implements CAPIAwareListener {
        public abstract void onDone();

        @Override
        public void onRawUnprocessResponseData(JSONObject data, String rid, String cid) throws JSONException {
            onDone();
        }

        @Override
        public void onError(String errorEventName, JSONObject data, String rid, String cid) {
            onDone();
        }
    }

}
