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

import java.util.HashMap;
import java.util.Iterator;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.timeline.TimelineManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
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
    
    int pic_count=0;
    
    UsbManager mUsbManager;
	PendingIntent mPermissionIntent;
	UsbDevice target_device=null;
	
	private static final String ACTION_USB_PERMISSION =
		    "com.android.example.USB_PERMISSION";
	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(device != null){
	                      //call method to set up device communication
	                    	System.out.println("GOT Permission!");
	                   }
	                }else {
	                	System.out.println("permission denied for device " + device);
	                }
	            }
	        }
	    }
	};
	

    
    @Override
    public void onCreate() {
        super.onCreate();
        mTimelineManager = TimelineManager.from(this);
        
     	IntentFilter intentFilter = new IntentFilter("android.intent.action.CAMERA_BUTTON");
     	camera_event_r = new Camera_event_Reciever();
        //---register the receiver---
        registerReceiver(camera_event_r, intentFilter);  
        
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	    
	    
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.i ("info", "STARTED!!!");
    	boolean toggle_light = false;
    	String capture_result = intent.getStringExtra("picture");
    	if (capture_result!=null && capture_result.equals("button")){
    		Log.i ("info", "this is a pic");
    		pic_count++;
    		toggle_light = true;
    	}
    	
        if (mLiveCard == null) {
            Log.i ("info", "Publishing LiveCard");
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_TAG);

            RemoteViews rv=new RemoteViews(this.getPackageName(),R.layout.livecard_detecting);
            rv.setTextViewText(R.id.livecard_content, "Ready to capture");
            mLiveCard.setViews(rv);

            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(PublishMode.REVEAL);
            Log.i ("info", "Done publishing LiveCard");
            
            search_usb_device();
        } else {
        	if (mLiveCard.isPublished()){
        		RemoteViews rv=new RemoteViews(this.getPackageName(),R.layout.livecard_detecting);
                rv.setTextViewText(R.id.livecard_content, "Captured "+ pic_count + " pics");
                mLiveCard.setViews(rv);
                if (target_device!=null){
                	Log.i ("info", "Found Flashlight");
                	if (mUsbManager.hasPermission (target_device)){
                    	Log.i ("info", "has permission");
                    	if (toggle_light){
                    		cmd_flashlight('T');
                        	handler.removeCallbacksAndMessages(null);  
                        	handler.postDelayed(new Runnable(){
                                @Override
                                public void run() {
                                	cmd_flashlight('F');
                                }
                            }, 2000);
                    		
                    	}
                    	
                    }
                }
                
        	}
            //  Jump to the LiveCard when API is available.
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	Log.i ("info", "Destroy!!!");
        if (mLiveCard != null && mLiveCard.isPublished()) {
        	Log.i ("info", "Unpublishing LiveCard");
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        unregisterReceiver(camera_event_r); 
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }
    
    public boolean search_usb_device(){
		boolean result=false;
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
		    UsbDevice device = deviceIterator.next();
		    Log.i ("info", "VID "+String.format("%04X", device.getVendorId())+" PID "+String.format("%04X", device.getProductId())+" NAME "+device.getDeviceName());
		    if (device.getVendorId()==0x4207 && device.getProductId()==0x20A0){
		    	target_device=device;
		    	break;
		    }
		}
		if (target_device!=null){
			Log.i ("info", "GOT DEVICE!!");
			mUsbManager.requestPermission(target_device, mPermissionIntent);
			result=true;
		}else{
			Log.i ("info", "NOT GOT DEVICE!!");
    	    result=false;
		}
		return result;
	}
    
    public void cmd_flashlight(char cmd){
		if (mUsbManager.hasPermission (target_device)){
			boolean forceClaim = true;
			UsbDeviceConnection usb_connection;
			
			UsbInterface intf = target_device.getInterface(0);
			
			usb_connection = mUsbManager.openDevice(target_device); 
			
			usb_connection.claimInterface(intf, forceClaim);
			
			UsbEndpoint out_endpoint = null;
			for (int i=0;i<intf.getEndpointCount();i++){
				UsbEndpoint endpoint = intf.getEndpoint(i);
				if (endpoint.getDirection()==UsbConstants.USB_DIR_OUT) {
					out_endpoint=endpoint;
				}
			}
			
			if (out_endpoint!=null){
				byte[] bytes={(byte) 'F',(byte) cmd};
				int TIMEOUT = 0;
				usb_connection.bulkTransfer (out_endpoint, bytes, 2, TIMEOUT);
				Log.i ("info", "sending: " + bytes[1]);
			}
		}else{
		}
	}

}
