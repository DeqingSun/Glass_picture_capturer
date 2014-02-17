package com.deqing.glass_picture_capturer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Camera_event_Reciever extends BroadcastReceiver{
    @Override
       public void onReceive(Context context, Intent intent) { 
    	Log.i ("info", "Reciever!!!");
    	// use this to start and trigger a service
    	Intent i= new Intent(context, Camera_listener_service.class);
    	// potentially add data to the intent
    	i.putExtra("picture", "button");
    	context.startService(i); 
    }
}