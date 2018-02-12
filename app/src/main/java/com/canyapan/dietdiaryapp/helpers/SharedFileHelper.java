package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

public class SharedFileHelper {

    /**
     * @param context  Context to get cache dir.
     * @param fileName File name.
     * @return File object.
     * @throws IOException If [cache]/shared/[fileName] path couldn't be created.
     */
    @NonNull
    public static File getSharedFile(Context context, String fileName) throws IOException {
        File folder = new File(context.getCacheDir(), "shared");
        if (!folder.exists()) {
            org.apache.commons.io.FileUtils.forceMkdir(folder);
        }

        return new File(folder, fileName);
    }

}
