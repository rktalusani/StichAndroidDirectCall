package com.nexmo.sdk.conversation.core.persistence;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

//import com.nexmo.sdk.conversation.R;
import com.nexmo.sdk.conversation.client.Image;
import com.nexmo.sdk.conversation.client.ImageRepresentation;
import com.nexmo.sdk.conversation.config.Defaults;
import com.nexmo.sdk.conversation.core.util.DateUtil;
import com.nexmo.sdk.conversation.core.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Image storage helper to save files on disk.
 * All 3 associated image representations will be stored.
 *
 * Path: "storage/emulated/0/AppPackageName/Media/Images/IMG-imageTimestamp-imageRepresentationID-imageRepresentationType.jpg"
 * Where imageTimestamp is {@link Image#getTimestamp()} ,
 * imageRepresentationID is {@link ImageRepresentation#id} and imageRepresentationType is {@link ImageRepresentation#type}
 *
 * @author emma tresanszki.
 * @hide
 */
public class ImageStorage {
    private static final String TAG = ImageStorage.class.getSimpleName();

    /***   Save all 3 image representations, one by one   ***/
    public static String saveFileToDisk(Context context, Image image, ImageRepresentation.TYPE type) {
        Log.d(TAG, "saveFilesToDisk " + image.toString());
        String root = Environment.getExternalStorageDirectory() + "/" + "nexmo_audio" + "/Media/Images/";

        File rootFile = new File(root);
        if (!rootFile.exists())
            if (!rootFile.mkdirs()) return null;

        return fileGenerator(rootFile, image.getTimestamp(), image.getImageRepresentationByType(type));
    }

    private static String fileNameGenerator(final Date timestamp, final String id, final String type) {
        return "IMG-" + DateUtil.formatImageNamingDateString(timestamp) + "-" + id + "-" + type + ".jpg";
    }

    private static String fileGenerator(File rootFile, final Date imageTimestamp, ImageRepresentation imageRepresentation)  {
        File imageRepresentationFile = new File(rootFile, fileNameGenerator(imageTimestamp, imageRepresentation.getId(), imageRepresentation.type.toString()));
        Log.d(TAG, "saveFilesToDisk: fileGenerator " + imageRepresentationFile.getPath());

        FileOutputStream out;
        try {
            out = new FileOutputStream(imageRepresentationFile);
            imageRepresentation.getBitmap().compress(Bitmap.CompressFormat.JPEG, Defaults.BITMAP_COMPRESS_QUALITY, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return imageRepresentationFile.getPath();
    }

    /***  Remove all 3 image representation files  ***/
    public static void deleteFilesFromDisk(Context context, Image image) {
        String root = Environment.getExternalStorageDirectory() + "/" + "nexmo_audio" + "/Media/Images/";
        File rootFile = new File(root);
        if (!rootFile.exists()) return;

        deleteFile(image.getOriginal());
        deleteFile(image.getThumbnail());
        deleteFile(image.getMedium());
    }

    /** Remove one image representation file **/
    static void deleteFile(ImageRepresentation imageRepresentation) {
        File fileToDelete = new File(imageRepresentation.getLocalFilePath());
        if (fileToDelete.exists())
            fileToDelete.delete();
    }
}
