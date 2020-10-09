package com.kingja.loadsir.core;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.kingja.loadsir.BuildConfig;
import com.kingja.loadsir.LoadSirUtil;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.callback.SuccessCallback;

import java.util.HashMap;
import java.util.Map;

/**
 * Description:TODO
 * Create Time:2017/9/2 17:02
 * Author:KingJA
 * Email:kingjavip@gmail.com
 */

public class LoadLayout extends FrameLayout {
    private final String TAG = getClass().getSimpleName();
    private Map<Class<? extends Callback>, Callback> callbacks = new HashMap<>();
    private Context context;
    private Callback.OnReloadListener onReloadListener;
    private Class<? extends Callback> preCallback;
    private Class<? extends Callback> curCallback;
    private static final int CALLBACK_CUSTOM_INDEX = 1;

    public LoadLayout(@NonNull Context context) {
        super(context);
    }

    public LoadLayout(@NonNull Context context, Callback.OnReloadListener onReloadListener) {
        this(context);
        this.context = context;
        this.onReloadListener = onReloadListener;
    }

    public void setupSuccessLayout(Callback callback) {
        addCallback(callback);
        View successView = callback.getRootView();
        successView.setVisibility(View.INVISIBLE);
        addView(successView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        curCallback = SuccessCallback.class;
    }

    public void setupCallback(Callback callback) {
        Callback cloneCallback = callback.copy();
        cloneCallback.setCallback(context, onReloadListener);
        addCallback(cloneCallback);
    }

    public void addCallback(Callback callback) {
        if (!callbacks.containsKey(callback.getClass())) {
            callbacks.put(callback.getClass(), callback);
        }
    }

    public void showCallback(final Class<? extends Callback> callback) {
        showCallback(callback, null);
    }

//    public void showCallback(Callback callback) {
//        if (callbacks.containsKey(callback.getClass())) {
//            removeView(callback.getRootView());
//            callbacks.remove(callback.getClass());
//        }
//        setupCallback(callback.setCallback(getContext(), onReloadListener));
//        if (LoadSirUtil.isMainThread()) {
//            showCallbackView(callback.getClass());
//        } else {
//            postToMainThread(callback.getClass());
//        }
//    }

    public Class<? extends Callback> getCurrentCallback() {
        return curCallback;
    }

    private void postToMainThread(final Class<? extends Callback> status) {
        post(new Runnable() {
            @Override
            public void run() {
                showCallbackView(status);
            }
        });
    }

    private void showCallbackView(Class<? extends Callback> status) {
        if (preCallback != null) {
            if (preCallback == status) {
                return;
            }
            callbacks.get(preCallback).onDetach();
        }
        if (getChildCount() > 1) {
            removeViewAt(CALLBACK_CUSTOM_INDEX);
        }
        for (Class key : callbacks.keySet()) {
            if (key == status) {
                SuccessCallback successCallback = (SuccessCallback) callbacks.get(SuccessCallback.class);
                if (key == SuccessCallback.class) {
                    successCallback.show();
                } else {
                    successCallback.showWithCallback(callbacks.get(key).getSuccessVisible());
                    View rootView = callbacks.get(key).getRootView();
                    if (indexOfChild(rootView) == -1) {
                        addView(rootView);
                        callbacks.get(key).onAttach(context, rootView);
                    }
                    rootView.setVisibility(View.VISIBLE);
                }
                preCallback = status;
            }
        }
        curCallback = status;
    }

    public void setCallBack(Class<? extends Callback> callback, Transport transport) {
        if (transport == null) {
            return;
        }
        checkCallbackExist(callback);
        transport.order(context, callbacks.get(callback).obtainRootView());
    }

    public void showCallback(final Class<? extends Callback> callback, Transport transport) {
        checkCallbackExist(callback);

        if (transport != null) {
            transport.order(context, callbacks.get(callback).obtainRootView());
        }

        if (LoadSirUtil.isMainThread()) {
            showCallbackView(callback);
        } else {
            postToMainThread(callback);
        }
        if(BuildConfig.DEBUG){
            Log.d("xzwzz", "showCallback: " + callback);
        }
    }

    private void checkCallbackExist(Class<? extends Callback> callback) {
        if (!callbacks.containsKey(callback)) {
            try {
                Callback resultCallback = callback.newInstance().setCallback(getContext(), onReloadListener);
                addCallback(resultCallback);
                if(BuildConfig.DEBUG){
                    Log.d("xzwzz", "addCallback: " + resultCallback);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
