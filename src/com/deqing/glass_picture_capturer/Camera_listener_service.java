/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deqing.glass_picture_capturer;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Service owning the LiveCard living in the timeline.
 */
public class Camera_listener_service extends Service {

    private static final String LIVE_CARD_TAG = "Camera_listener";

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    
   
    
    Handler handler = new Handler();

    Camera_event_Reciever camera_event_r;
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        mTimelineManager = TimelineManager.from(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.i ("info", "STARTED!!!");
    	
     	IntentFilter intentFilter = new IntentFilter("android.intent.action.CAMERA_BUTTON");
     	camera_event_r = new Camera_event_Reciever();
        //---register the receiver---
        registerReceiver(camera_event_r, intentFilter);  
    	
        if (mLiveCard == null) {
            Log.i ("info", "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);

            mLiveCard.setViews(new RemoteViews(this.getPackageName(),R.layout.livecard_detecting));

            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(PublishMode.REVEAL);
            Log.i ("info", "Done publishing LiveCard");
        } else {
            //  Jump to the LiveCard when API is available.
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
        	Log.i ("info", "Unpublishing LiveCard");
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        unregisterReceiver(camera_event_r);  
        super.onDestroy();
    }
}
