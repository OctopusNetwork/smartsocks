package io.smarttangle.blockchain.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import io.smarttangle.blockchain.Utils.Base64Utils;

public class UserStorageUtils {
    private static final String CLEAN_PREFERENCES_NAME = "_cleanable_bucket";

    public static SharedPreferences getSharedPreferences(Context context, String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getPreferences(Context context) {
        return getSharedPreferences(context, context.getPackageName());
    }

    public static SharedPreferences getCleanupPreferencesOnLogout(Context context) {
        return getSharedPreferences(context, CLEAN_PREFERENCES_NAME);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getObject(Context context, String key) {
        if (context == null || TextUtils.isEmpty(key)) {
            return null;
        }
        T t = null;
        String fileName = Base64Utils.encodeToString(key.getBytes());
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = context.openFileInput(fileName);
            ois = new ObjectInputStream(fis);
            t = (T) ois.readObject();
        } catch (Exception e) {
            context.deleteFile(fileName);
        } finally {
            closeStream(ois, fis);
        }
        return t;
    }

    public static void removeObject(Context context, String key) {
        if (context == null || TextUtils.isEmpty(key)) {
            return;
        }
        String fileName = Base64Utils.encodeToString(key.getBytes());
        context.deleteFile(fileName);
    }

    public static void putObject(Context context, String key, Object object) {
        if (context == null || TextUtils.isEmpty(key)) {
            return;
        }
        if (object == null) {
            removeObject(context, key);
            return;
        }
        String fileName = Base64Utils.encodeToString(key.getBytes());
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
        } catch (IOException e) {
            context.deleteFile(fileName);
        } finally {
            closeStream(oos, fos);
        }
    }

    private static void closeStream(Closeable... streams) {
        if (streams != null) {
            for (Closeable stream : streams) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {

                    }
                }
            }
        }
    }

}
