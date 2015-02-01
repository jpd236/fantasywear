package com.jeffpdavidson.fantasywear.api;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import java.util.concurrent.ExecutionException;

public class Volley {
    private static volatile Volley sInstance;

    private final RequestQueue mRequestQueue;

    private Volley(Context context) {
        mRequestQueue = com.android.volley.toolbox.Volley.newRequestQueue(context);
    }

    public static Volley getInstance(Context context) {
        if (sInstance == null) {
            synchronized (Volley.class) {
                if (sInstance == null) {
                    sInstance = new Volley(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public static boolean isClientError(NetworkResponse response) {
        return response != null && (response.statusCode >= 400 || response.statusCode < 499);
    }

    public static <T> T makeBlockingRequest(Context context, Request<T> request,
            RequestFuture<T> future) throws VolleyError, InterruptedException {
        Volley.getInstance(context).getRequestQueue().add(request);
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw (VolleyError) e.getCause();
        }
    }
}
