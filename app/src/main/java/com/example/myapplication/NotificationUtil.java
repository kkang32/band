package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationUtil {

    public static final int ID_HEARTBEATCHECK = 1;
    public static final int ID_ALERT = 22;

    public static NotificationCompat.Builder builder(String title, String content, Context context){
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);


        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "HI-U";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "HI-U",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT);

            ((android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)//"블루투스가 비활성화 되어있습니다. 여기를 눌러 블루투스를 활성화 하세요.")
                .setContentIntent(pendingIntent);

        return builder;
    }

    public static void notification(int id, String title, String content, Context context){
        NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder(title, content, context).build());
    }


}
