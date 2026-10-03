package com.example.mdfsoc.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final SessionTokenProvider tokenProvider;

    public interface SessionTokenProvider {
        String getToken();
    }

    public AuthInterceptor(SessionTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenProvider.getToken();
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }
        Request authenticated = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(authenticated);
    }
}