package com.example.background.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.background.MainActivity;
import com.example.background.api.APIClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import io.flutter.Log;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RequiresApi(api = Build.VERSION_CODES.M)
public class Location extends Service {
    FusedLocationProviderClient client;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        client = LocationServices.getFusedLocationProviderClient(getApplicationContext());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //onTaskRemoved(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "background";
            String description = "Channel for run service in background";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(name, name, importance);
            mChannel.setDescription(description);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(mChannel);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getForegroundService(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, "background")
                    .setContentTitle("Foreground Service")
                    .setContentText("input")
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);
        } else {
            startForeground(1, new Notification());
        }
        start();
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
//        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
//        restartServiceIntent.setPackage(getPackageName());
//        Log.i("TASK_REMOVED", "YES");
//        startService(restartServiceIntent);

        Intent restartServiceIntent = new Intent(getApplicationContext(), Restarter.class);
        restartServiceIntent.setPackage(getPackageName());
        Log.i("TASK_REMOVED", "YES");
        sendBroadcast(restartServiceIntent);

        super.onTaskRemoved(rootIntent);
    }

    @SuppressLint("WakelockTimeout")
    public void start() {
        PowerManager mgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationWakeLock");
        wakeLock.acquire();
        Handler handler = new Handler();
        int delay = 5000; //milliseconds
        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                client.getLastLocation().addOnCompleteListener(task -> {
                    double lat = Objects.requireNonNull(task.getResult()).getLatitude(), lng = task.getResult().getLongitude();
                    //Log.i("LAT_LNG", lat+", "+lng);
                    APIClient apiClient = new APIClient();
                    Call<ResponseBody> call = apiClient.sendCoords(lat, lng);
                    call.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
                            try {
                                if (response.isSuccessful()) {
                                    assert response.body() != null;
                                    Log.i("RESPONSE", response.body().string());
                                } else {
                                    assert response.errorBody() != null;
                                    Log.i("RESPONSE", response.errorBody().string());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
                            call.cancel();
                            t.printStackTrace();
                        }
                    });
                });

                handler.postDelayed(this, delay);
            }
        }, delay);
    }
}
