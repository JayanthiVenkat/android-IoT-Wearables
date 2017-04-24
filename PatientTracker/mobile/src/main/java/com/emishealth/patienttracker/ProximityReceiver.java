package com.emishealth.patienttracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Jayanthi Venkat on 4/19/2017.
 */

public class ProximityReceiver extends BroadcastReceiver {
    String notificationTitle;
    String notificationContent;
    String tickerMessage;
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

        boolean proximity_entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, true);
        Log.d("TAG","--proximity entering "+proximity_entering) ;
        if(proximity_entering){
            Toast.makeText(context,"Entering the region"  , Toast.LENGTH_LONG).show();
            notificationTitle="Proximity - Entry";
            notificationContent="Entered the region";
            tickerMessage = "Entered the region";
        }else{
            Toast.makeText(context,"Exiting the region"  ,Toast.LENGTH_LONG).show();
            notificationTitle="Proximity - Exit";
            notificationContent="Exited the region";
            tickerMessage = "Exited the region";
        }

        Intent notificationIntent = new Intent(context,NotificationView.class);
        notificationIntent.putExtra("content", notificationContent );

        /** This is needed to make this intent different from its previous intents */
        notificationIntent.setData(Uri.parse("tel:/"+ (int)System.currentTimeMillis()));
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /** Creating different tasks for each notification. See the flag Intent.FLAG_ACTIVITY_NEW_TASK */
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        /** Getting the System service NotificationManager */
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /** Configuring notification builder to create a notification */
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setContentText(notificationContent)
                .setContentTitle(notificationTitle)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setTicker(tickerMessage)
                .setContentIntent(pendingIntent);

        /** Creating a notification from the notification builder */
        Notification notification = notificationBuilder.build();

        /** Sending the notification to system.
         * The first argument ensures that each notification is having a unique id
         * If two notifications share same notification id, then the last notification replaces the first notification
         * */
        nManager.notify((int)System.currentTimeMillis(), notification);

        /** Finishes the execution of this activity */
//        finish();
    }
}


