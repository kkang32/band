package com.example.myapplication;

public interface HttpManagerCallback {
    /**
     * 결과 수신
     * @param body
     */
    void responseBody(String body);

    /**
     * 에러 처리
     * @param error
     */
    void onError(String error);
}
