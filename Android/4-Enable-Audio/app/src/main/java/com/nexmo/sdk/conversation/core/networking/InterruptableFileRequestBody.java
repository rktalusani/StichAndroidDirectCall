package com.nexmo.sdk.conversation.core.networking;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by rux on 10/03/17.
 *
 * @hide
 */

public class InterruptableFileRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048;

    private final File file;
    private final String contentType;
    private volatile long progress = 0;
    private volatile boolean isCanceled = false;

    public InterruptableFileRequestBody(File file, String contentType) {
        this.file = file;
        this.contentType = contentType;
    }

    @Override
    public long contentLength() {
        return file.length();
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            this.progress = 0;
            long read;

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                if (isCanceled) throw new UploadCanceledByUser();
                progress += read;
                sink.flush();
            }
        } finally {
            Util.closeQuietly(source);
        }
    }

    public long getProgress() {
        return progress;
    }

    public boolean isFinished() {
        return getProgress() == contentLength();
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public synchronized void cancel() {
        isCanceled = true;
    }


    public static class UploadCanceledByUser extends IOException {
    }

}
