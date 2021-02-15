package com.example.myapplication;

import android.util.Log;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 서버 통신
 * OkHttpClient 사용
 * https://github.com/Syehunter/OkHttpManager
 */
public class HttpManager {
    private static final String TAG = "HttpManager";
    public static void get(String url, HttpManagerCallback callback){
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    //.addHeader("x-api-key", RestTestCommon.API_KEY)
                    .url(url)
                    .build(); //GET Request

            //동기 처리시 execute함수 사용
            Response response = client.newCall(request).execute();

            //출력
            String message = response.body().string();
            callback.responseBody(message);
        } catch (Exception e) {
            callback.onError(e.toString());
        }
    }

    public static void post(String url, String json, HttpManagerCallback callback){
        try{
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json, (MediaType.parse("application/json")))) //POST로 전달할 내용 설정
                    .build();


            Response response = client.newCall(request).execute();

            //출력
            String message = response.body().string();
            System.out.println(message);

        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
