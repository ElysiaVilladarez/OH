package com.oohana.database;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by elysi on 5/11/2017.
 */

public class RequestQueSingleton {
    private static RequestQueSingleton requestQueSingletonInstance;
    private RequestQueue requestQueue;
    private static Context c;
    private RequestQueSingleton(Context c){
        this.c = c;
        requestQueue = getRequestQueue();
    }

    public static synchronized RequestQueSingleton getInstance(Context c){
        if(requestQueSingletonInstance == null){
            requestQueSingletonInstance = new RequestQueSingleton(c);

        }
        return requestQueSingletonInstance;
    }
    public RequestQueue getRequestQueue(){
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(c);
        }
        return requestQueue;
    }
}
