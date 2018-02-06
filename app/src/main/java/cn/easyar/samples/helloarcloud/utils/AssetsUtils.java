package cn.easyar.samples.helloarcloud.utils;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cn.easyar.samples.helloarcloud.App;

/**
 * Created by shucc on 18/2/6.
 * cc@cchao.org
 */
public class AssetsUtils {

    private AssetsUtils() {}

    public static String loadAssets(String fileName) {
        StringBuilder stringBuilder = new StringBuilder("");
        AssetManager assetManager = App.getInstance().getAssets();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(fileName), "utf-8"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
