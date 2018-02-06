package cn.easyar.samples.helloarcloud.utils;

import android.content.Context;
import android.support.annotation.RawRes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cn.easyar.samples.helloarcloud.App;

/**
 * Created by shucc on 18/1/25.
 * cc@cchao.org
 */
public class RawUtils {

    private RawUtils() {
        // util
    }

    public static String loadRaw(@RawRes int resId) {
        StringBuilder builder = new StringBuilder();
        try {
            InputStream inputStream = App.getInstance().getResources().openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
