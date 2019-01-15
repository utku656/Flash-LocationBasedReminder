package com.utkukayman.locationbasedreminderbyutku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GeofenceReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";
    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent  sent by Location Services. This Intent is provided to Location
     *                Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Enqueues a JobIntentService passing the context and intent as parameters
        GeofenceTransitionService.enqueueWork(context, intent);
        Log.d(TAG, "GeofenceReceiver : onReceive");
    }
}
//    /**
//     * Receives incoming intents.
//     *
//     * @param context the application context.
//     * @param intent  sent by Location Services. This Intent is provided to Location
//     *                Services (inside a PendingIntent) when addGeofences() is called.
//     */
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        // Enqueues a JobIntentService passing the context and intent as parameters
//        Log.i("Broadcast", "onReceive: ");
//        Intent serviceIntent  = new Intent(context , GeofenceTransitionService.class);
//        context.startService(serviceIntent);
//        GeofenceTransitionService.enqueueWork(context, intent);
//    }
//}
