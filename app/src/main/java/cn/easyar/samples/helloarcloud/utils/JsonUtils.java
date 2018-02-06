package cn.easyar.samples.helloarcloud.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by shucc on 18/2/6.
 * cc@cchao.org
 */
public class JsonUtils {

    private static Gson gson = new Gson();

    private static GsonBuilder gb = new GsonBuilder();

    private JsonUtils() {
    }

    public static String toString(Object object) {
        gb.disableHtmlEscaping();
        return gb.create().toJson(object);
    }

    public static <T> T toObject(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}