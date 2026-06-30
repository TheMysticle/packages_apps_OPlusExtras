package org.evolution.oplus.OPlusExtras.autohbm;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.evolution.oplus.OPlusExtras.Nodes;
import org.evolution.oplus.OPlusExtras.utils.Utils;

public class AutoHbmService extends Service {

    private static final String MAX_BRIGHTNESS = "1";
    private static final String FALLBACK_BRIGHTNESS = "0";
    private static boolean mAutoHbmActive = false;
    private ExecutorService mExecutorService;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;

    private SharedPreferences mSharedPrefs;

    public void activateLightSensorRead() {
        submit(() -> {
            mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mSensorManager.registerListener(mSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        });
    }

    public void deactivateLightSensorRead() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
        mAutoHbmActive = false;
        restoreBrightness();
    }

    private void setBrightnessDirectly(String brightness) {
        Utils.writeValue(Nodes.nodeHBM(getApplicationContext()), brightness);
    }

    private void restoreBrightness() {
        setBrightnessDirectly(FALLBACK_BRIGHTNESS);
    }

    private boolean isCurrentlyEnabled() {
        String fileValue = Utils.getFileValue(Nodes.nodeHBM(getApplicationContext()), "0");
        return fileValue.equals(MAX_BRIGHTNESS);
    }

    SensorEventListener mSensorEventListener = new SensorEventListener() {
        private boolean mCrossedThreshold = false;
        private long mCrossedThresholdTime = 0;
        private long mLastTriggerTime = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            float lux = event.values[0];
            KeyguardManager km = (KeyguardManager) getSystemService(getApplicationContext().KEYGUARD_SERVICE);
            boolean keyguardShowing = km.inKeyguardRestrictedInputMode();
            int luxThreshold = 20000;
            int timeToEnableHbm = 0;
            int timeToDisableHbm = 1;

            if (lux > luxThreshold) {
                if (!mCrossedThreshold) {
                    mCrossedThreshold = true;
                    mCrossedThresholdTime = System.currentTimeMillis();
                } else {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mCrossedThresholdTime >= timeToEnableHbm * 1000 && (!mAutoHbmActive || !isCurrentlyEnabled()) && !keyguardShowing) {
                        mAutoHbmActive = true;
                        setBrightnessDirectly(MAX_BRIGHTNESS);
                        mLastTriggerTime = currentTime;
                    }
                }
            } else {
                mCrossedThreshold = false;

                if (mAutoHbmActive) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - mLastTriggerTime >= timeToDisableHbm * 1000) {
                        mAutoHbmActive = false;
                        restoreBrightness();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                activateLightSensorRead();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                deactivateLightSensorRead();
            }
        }
    };

    @Override
    public void onCreate() {
        mExecutorService = Executors.newSingleThreadExecutor();
        IntentFilter screenStateFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            activateLightSensorRead();
        }
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        deactivateLightSensorRead();
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
