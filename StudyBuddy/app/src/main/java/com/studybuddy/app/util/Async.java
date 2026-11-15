package com.studybuddy.app.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny helper to run database / AI work off the main thread. */
public final class Async {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Async() {
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    /** Run work in the background, then deliver the result on the main thread. */
    public static <T> void load(Callable<T> work, Callback<T> callback) {
        IO.execute(() -> {
            T value = null;
            try {
                value = work.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            final T result = value;
            MAIN.post(() -> callback.onResult(result));
        });
    }

    /** Fire-and-forget background work. */
    public static void io(Runnable work) {
        IO.execute(() -> {
            try {
                work.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
