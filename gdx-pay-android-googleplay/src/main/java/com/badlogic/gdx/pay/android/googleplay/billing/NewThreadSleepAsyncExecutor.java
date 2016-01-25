package com.badlogic.gdx.pay.android.googleplay.billing;

public class NewThreadSleepAsyncExecutor implements AsyncExecutor {

    @Override
    public void executeAsync(final Runnable runnable, final long delayInMs) {
        Runnable sleepingRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(delayInMs);
                } catch (InterruptedException e) {
                    return;
                }
                runnable.run();
            }
        };

        new Thread(sleepingRunnable)
                .start();
    }
}
