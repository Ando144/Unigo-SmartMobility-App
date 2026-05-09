package com.example.unigo_das.network;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.simple.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NetworkWorker extends Worker {

    public NetworkWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String script = getInputData().getString("script");
        String responseStr = "";
        HttpURLConnection conn = null;

        try {
            if (script == null) return Result.failure();

            URL url = new URL("http://35.233.9.137/" + script);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            JSONObject jsonParam = new JSONObject();
            Map<String, Object> allData = getInputData().getKeyValueMap();
            for (Map.Entry<String, Object> entry : allData.entrySet()) {
                if (!entry.getKey().equals("script") && entry.getValue() != null) {
                    jsonParam.put(entry.getKey(), entry.getValue());
                }
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonParam.toJSONString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
                responseStr = sb.toString();
            }

        } catch (Exception e) {
            responseStr = "{\"error\": \"Error de conexión\"}";
        } finally {
            if (conn != null) conn.disconnect();
        }

        Data output = new Data.Builder()
                .putString("response", responseStr)
                .build();

        return Result.success(output);
    }
}
