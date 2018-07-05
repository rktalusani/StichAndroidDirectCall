package com.nexmo.sdk.conversation.core;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rux on 02/02/17.
 *
 * @hide
 */

public class MapList<T> extends ConcurrentHashMap<String, List<T>> {
    public void addIfNotExists(String key, T value) {
        if (!this.containsKey(key))
            this.putIfAbsent(key, Collections.synchronizedList(new ArrayList<T>()));
        this.getList(key).add(value);
    }

    public void removeIfExists(String key, T value) {
        this.getList(key).remove(value);
    }

    /**
     * Method returns valid list associated with key
     * @param key key
     * @return empty list if key isn't exists or list for valid keys
     */
    @NonNull
    public List<T> getList(Object key) {
        List<T> list = super.get(key);
        if (list != null) return list;
        return Collections.emptyList();
    }

}
