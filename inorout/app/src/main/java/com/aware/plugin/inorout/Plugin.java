package com.aware.plugin.inorout;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Light;
import com.aware.LinearAccelerometer;
import com.aware.Magnetometer;
import com.aware.Proximity;
import com.aware.providers.Battery_Provider;
import com.aware.providers.Light_Provider;
import com.aware.providers.Linear_Accelerometer_Provider;
import com.aware.providers.Locations_Provider;
import com.aware.providers.Magnetometer_Provider;
import com.aware.providers.Proximity_Provider;
import com.aware.providers.Telephony_Provider;
import com.aware.utils.Aware_Plugin;
import com.aware.plugin.inorout.Provider.InOrOut;

import java.util.Calendar;
import java.util.TimeZone;

public class Plugin extends Aware_Plugin {

//Variables used
    public static double CURRENT_BATTEMP, BATTEMP_avg;

    public static double CURRENT_SIGNAL1, CURRENT_SIGNAL2;

    public static double CURRENT_LIGHT, CURRENT_PROXI;

    public static double CURRENT_LATITUDE, CURRENT_LONGITUDE, CURRENT_SUNRISE, CURRENT_SUNSET;

    public static double CURRENT_IN, CURRENT_OUT, CURRENT_IN_temp, CURRENT_OUT_temp, CURRENT_IN_sum, CURRENT_OUT_sum;

    public static double elapsed_indoor=0, elapsed_outdoor=0;

    int counter;

    double signalweight1, signalweight2, signalweight3, lightweight=1.0,  batteryweight=2.0, battempdelta=0.0;

    public static final String EXTRA_ELAPSED_INDOOR = "elapsed_indoor";
    public static final String EXTRA_ELAPSED_OUTDOOR = "elapsed_outdoor";

//Get current hour of the day
    Calendar c = Calendar.getInstance();
    int hour = c.get(Calendar.HOUR_OF_DAY);

    public ContextReceiver dataReceiver = new ContextReceiver();

    private static Intent aware;
    private static ContextProducer sContext;

    @Override
    public void onCreate() {
        super.onCreate();

        aware = new Intent(this, Aware.class);
        startService(aware);

        TAG = "Big Brother";
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        if (DEBUG) Log.d(TAG, "InOrOut-plugin running");

//Set Sensors On
        if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_INOROUT).equals("true")) {
            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LIGHT, 500000);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, true);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_PROXIMITY, true);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_TELEPHONY, true);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_LOCATION_NETWORK, 3600);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, true);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_BATTERY,true);
            }
        else{
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_TELEPHONY, false);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, false);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_PROXIMITY, false);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, false);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_BATTERY,false);
            Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_LINEAR_ACCELEROMETER, false);
        }
        Intent refresh = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(refresh);

//Get sunrise and sunset times for light-sensor
        CURRENT_SUNRISE = getSunrise();
        CURRENT_SUNSET = getSunset();

        IntentFilter filter = new IntentFilter();

        filter.addAction(Light.ACTION_AWARE_LIGHT);
        filter.addAction(Proximity.ACTION_AWARE_PROXIMITY);

        registerReceiver(dataReceiver, filter);

//Shares this plugin's context to AWARE and applications
        sContext = new ContextProducer() {
            @Override
            public void onContext() {
                ContentValues context_data = new ContentValues();
                context_data.put(InOrOut.TIMESTAMP, System.currentTimeMillis());
                context_data.put(InOrOut.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                context_data.put(InOrOut.CURRENT_LIGHT, CURRENT_LIGHT);
                context_data.put(InOrOut.CURRENT_SIGNAL1, CURRENT_SIGNAL1);
                context_data.put(InOrOut.CURRENT_SIGNAL2, CURRENT_SIGNAL2);
                context_data.put(InOrOut.CURRENT_PROXI, CURRENT_PROXI);
                context_data.put(InOrOut.CURRENT_SUNRISE, CURRENT_SUNRISE);
                context_data.put(InOrOut.CURRENT_SUNSET, CURRENT_SUNSET);
                context_data.put(InOrOut.CURRENT_BATTEMP, CURRENT_BATTEMP);
                context_data.put(InOrOut.CURRENT_IN, CURRENT_IN);
                context_data.put(InOrOut.CURRENT_OUT, CURRENT_OUT);
                context_data.put(InOrOut.ELAPSED_INDOOR, elapsed_outdoor);
                context_data.put(InOrOut.ELAPSED_OUTDOOR, elapsed_indoor);
                if( DEBUG ) Log.d(TAG, context_data.toString());
//insert data to table
                getContentResolver().insert(InOrOut.CONTENT_URI, context_data);
                Intent sharedContext = new Intent("ACTION_AWARE_LOCATION_TYPE_INDOOR");
                Intent sharedContext2 = new Intent("ACTION_AWARE_LOCATION_TYPE_OUTDOOR");
                sharedContext.putExtra(EXTRA_ELAPSED_INDOOR, elapsed_outdoor);
                sharedContext.putExtra(EXTRA_ELAPSED_OUTDOOR, elapsed_indoor);
                sendBroadcast(sharedContext);
                sendBroadcast(sharedContext2);
            }
        };
        CONTEXT_PRODUCER = sContext;
//Our provider tables
        DATABASE_TABLES = Provider.DATABASE_TABLES;
//Our table fields
        TABLES_FIELDS = Provider.TABLES_FIELDS;
//Our provider URI
        CONTEXT_URIS = new Uri[]{InOrOut.CONTENT_URI };
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        //Set Sensors Off
        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_INOROUT, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_TELEPHONY, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LIGHT, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_PROXIMITY, false);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, false);
        Aware.setSetting(getApplicationContext(),Aware_Preferences.STATUS_BATTERY,false);

        Intent refresh = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(refresh);

        unregisterReceiver(dataReceiver);

        stopService(aware);

    }


//Method for getting data
    public class ContextReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

//Proximity-sensor
            ContentValues valuesProxi = (ContentValues) intent.getExtras().get(Proximity.EXTRA_DATA);
            if (valuesProxi.get(Proximity_Provider.Proximity_Data.PROXIMITY) != null) {
                CURRENT_PROXI = Double.parseDouble(valuesProxi.get(Proximity_Provider.Proximity_Data.PROXIMITY).toString());
            }

//Light-sensor
            ContentValues valuesLight = (ContentValues) intent.getExtras().get(Light.EXTRA_DATA);
            if (valuesLight.get(Light_Provider.Light_Data.LIGHT_LUX) != null) {
                CURRENT_LIGHT = Double.parseDouble(valuesLight.get(Light_Provider.Light_Data.LIGHT_LUX).toString());
            }

//GSM-signal & GSM-Neighbour-signal
            Cursor valuesSignal1 = getContentResolver().query(Telephony_Provider.GSM_Data.CONTENT_URI, null, null, null, Telephony_Provider.GSM_Data.TIMESTAMP + " DESC LIMIT 1");
            if(valuesSignal1 != null && valuesSignal1.moveToFirst() ) {
                CURRENT_SIGNAL1 = valuesSignal1.getDouble(valuesSignal1.getColumnIndex(Telephony_Provider.GSM_Data.SIGNAL_STRENGTH));
                CURRENT_SIGNAL1 = 2*CURRENT_SIGNAL1-113;
            }
            if(valuesSignal1 != null && ! valuesSignal1.isClosed()) valuesSignal1.close();
            Cursor valuesSignal2 = getContentResolver().query(Telephony_Provider.GSM_Neighbors_Data.CONTENT_URI, null, null, null, Telephony_Provider.GSM_Neighbors_Data.TIMESTAMP + " DESC LIMIT 1");
            if(valuesSignal2 != null && valuesSignal2.moveToFirst() ) {
                CURRENT_SIGNAL2 = valuesSignal2.getDouble(valuesSignal2.getColumnIndex(Telephony_Provider.GSM_Neighbors_Data.SIGNAL_STRENGTH));
            }
            if(valuesSignal2 != null && ! valuesSignal2.isClosed()) valuesSignal2.close();

//Location
            Cursor valuesLocation = getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP + " DESC LIMIT 1");
            if(valuesLocation != null && valuesLocation.moveToFirst() ) {
                CURRENT_LATITUDE = valuesLocation.getDouble(valuesLocation.getColumnIndex(Locations_Provider.Locations_Data.LATITUDE));
                CURRENT_LONGITUDE = valuesLocation.getDouble(valuesLocation.getColumnIndex(Locations_Provider.Locations_Data.LONGITUDE));
            }
            if(valuesLocation != null && ! valuesLocation.isClosed()) valuesLocation.close();

//Battery current temperature
            Cursor valuesBattery = getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, Battery_Provider.Battery_Data.TIMESTAMP + " DESC LIMIT 1");
            if(valuesBattery != null && valuesBattery.moveToFirst() ) {
                CURRENT_BATTEMP = valuesBattery.getDouble(valuesBattery.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE));
            }
            if(valuesBattery != null && ! valuesBattery.isClosed()) valuesBattery.close();

//Today's average for battery temperature
            Cursor valuesBatteryAVG = context.getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, Battery_Provider.Battery_Data.TIMESTAMP + ">" +(System.currentTimeMillis()-3*24*60*60*1000), null, Battery_Provider.Battery_Data.TIMESTAMP + " DESC");
            if( valuesBatteryAVG != null && valuesBatteryAVG.moveToFirst() ) {
                double temp_min = valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE));
                double temp_max = valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE));
                valuesBatteryAVG.moveToFirst();
                long avg=0;
                int counter2 = 1;
                while (!valuesBatteryAVG.isAfterLast()) {
                    long current = valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE));
                    avg  = avg + current;
                    if(valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE))<temp_min){
                        temp_min = valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE));
                    }
                    else if(valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE))>temp_max){
                        temp_max = valuesBatteryAVG.getLong(valuesBatteryAVG.getColumnIndex(Battery_Provider.Battery_Data.TEMPERATURE));
                    }
                    counter2++;
                    valuesBatteryAVG.moveToNext();
                }
                BATTEMP_avg = avg/counter2;
                Log.d(TAG, String.format("BATTEMP_avg: %.1f", BATTEMP_avg));
                Log.d(TAG, String.format("BATTEMPMAX: %.1f", temp_max));
                Log.d(TAG, String.format("BATTEMPMIN: %.1f", temp_min));
            }
            if(valuesBatteryAVG != null && ! valuesBatteryAVG.isClosed()) valuesBatteryAVG.close();



//Put weights on different components in different few situations:

//Signal-weights if no signal
            if(CURRENT_SIGNAL1 > 0){
                signalweight1 = 0.0;
                signalweight2 = 1.0;
            }
            else{
                signalweight1 = 2.0/3.0;
                signalweight2 = 1.0/3.0;
            }
            if(CURRENT_SIGNAL2 > 0){
                signalweight1 = 1.0;
                signalweight2 = 0.0;
            }
            else{
                signalweight1 = 3.0/4.0;
                signalweight2 = 1.0/4.0;
            }
            if(CURRENT_SIGNAL1 > 0 && CURRENT_SIGNAL2 > 0){
                signalweight3 = 0.0;
            }
            else{
                signalweight3 = 2.0;
            }

//Light-weights depending on sun and proximity
            if (CURRENT_PROXI < 50 || hour > CURRENT_SUNSET || hour < CURRENT_SUNRISE){
                lightweight = 0.0;
            }
            else if(CURRENT_LIGHT>1000){
                lightweight = 4.0;
            }
            else {
                lightweight = 3.0;
            }

//Calculate battery-temperature-probability and weight
            if(BATTEMP_avg-CURRENT_BATTEMP > 0){
                battempdelta = 1 - Math.exp(-Math.pow(BATTEMP_avg-CURRENT_BATTEMP,2)/(2*Math.pow(2,2)));
                batteryweight = 2.0;
            }
            else if(BATTEMP_avg-CURRENT_BATTEMP < 0){
                battempdelta = Math.exp(-Math.pow(BATTEMP_avg-CURRENT_BATTEMP,2)/(2*Math.pow(2,2)));
                batteryweight = 2.0;
            }
            else{
                batteryweight = 0.0;
            }


//Calculate indoor/outdoor probabilities(average of 10 values) with weights:
            CURRENT_OUT_temp = (Math.pow(((1 / (1 + (500 / CURRENT_LIGHT))) * lightweight),2) + Math.pow(((signalweight1*CURRENT_SIGNAL1+signalweight2*CURRENT_SIGNAL2+113)/62 * signalweight3),2) + Math.pow(battempdelta * batteryweight,2))/(lightweight+signalweight3+batteryweight);
            if(CURRENT_OUT_temp >1){
                CURRENT_OUT_temp = 100;
            }
            else{
                CURRENT_OUT_temp = CURRENT_OUT_temp*100;
            }
            CURRENT_IN_temp = 100-CURRENT_OUT_temp;
            if(counter < 5) {
                CURRENT_IN_sum = CURRENT_IN_sum + CURRENT_IN_temp;
                CURRENT_OUT_sum = CURRENT_OUT_sum + CURRENT_OUT_temp;
                counter++;
                Log.d(TAG, String.format("COUNTER: %d", counter));
            }
            else {
                CURRENT_IN = CURRENT_IN_sum/counter;
                CURRENT_OUT = CURRENT_OUT_sum/counter;
                CURRENT_IN_sum = 0;
                CURRENT_OUT_sum = 0;
                counter = 0;
            }

//Calculate times
            Cursor last_out = context.getContentResolver().query(InOrOut.CONTENT_URI, null, InOrOut.CURRENT_OUT + ">" +50, null, InOrOut.TIMESTAMP + " DESC LIMIT 1");
            if( last_out != null && last_out.moveToFirst() ) {
                elapsed_outdoor = (System.currentTimeMillis() - last_out.getDouble(last_out.getColumnIndex(InOrOut.TIMESTAMP)));
            }
            if( last_out != null && ! last_out.isClosed()) last_out.close();
            //Log.d(TAG, String.format("INDOORTIME: %.0f ms", elapsed_outdoor));

            Cursor last_in = context.getContentResolver().query(InOrOut.CONTENT_URI, null, InOrOut.CURRENT_IN + ">" +50, null, InOrOut.TIMESTAMP + " DESC LIMIT 1");
            if( last_out != null && last_in.moveToFirst() ) {
                elapsed_indoor = (System.currentTimeMillis() - last_in.getDouble(last_out.getColumnIndex(InOrOut.TIMESTAMP)));
            }
            if( last_in != null && ! last_in.isClosed()) last_in.close();
            //Log.d(TAG, String.format("OUTDOORTIME: %.0f ms", elapsed_indoor));

//Log for debugging
            Log.d(TAG, String.format("LIGHT: %.1f", ((1 / (1 + (500 / CURRENT_LIGHT))) * 100)));
            Log.d(TAG, String.format("BATTERY: %.1f", battempdelta * 100));
            Log.d(TAG, String.format("SIGNAALI: %.1f",((signalweight1*CURRENT_SIGNAL1+signalweight2*CURRENT_SIGNAL2+113)/62 * 100)));
            Log.d(TAG, String.format("CURRENT OUT: %.1f", CURRENT_OUT));

//Share context
            sContext.onContext();
        }

    }

//Algorithm for calculating sunrise/sunset times. Reference: http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
    public double getSunrise() {
        double lngHour = CURRENT_LONGITUDE/15;
        Calendar c = Calendar.getInstance();
        int gmtOffset = c.get(Calendar.ZONE_OFFSET);
        double day = c.get(Calendar.DAY_OF_YEAR);
        double t = day + ((6 - lngHour) / 24);
        double M = (0.9856 * t) - 3.289;
        double L = M + (1.916 * Math.sin((Math.PI/180)*M)) + (0.020 * Math.sin(2 * (Math.PI/180)*M)) + 282.634;
        if(L < 0){
            L=L+360;
        }
        if(L > 360){
            L=L-360;
        }
        double RA = (180/Math.PI)*Math.atan(0.91764 * Math.tan((Math.PI/180)*L));
        if(RA < 0){
            RA=RA+360;
        }
        if(RA > 360){
            RA=RA-360;
        }
        double Lquadrant  = (Math.floor(L/90)) * 90;
        double RAquadrant  = (Math.floor(RA/90)) * 90;
        RA = RA + (Lquadrant - RAquadrant);
        RA = RA / 15.0;
        double sinDec = 0.39782 * Math.sin((Math.PI/180)*L);
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (Math.cos((Math.PI/180)*90.833) - (sinDec *Math.sin((Math.PI/180)*CURRENT_LATITUDE))) / (cosDec * Math.cos((Math.PI/180)*CURRENT_LATITUDE));
        double H = 360 - (180/Math.PI)*Math.acos(cosH);
        H = H / 15.0;
        double T = H + RA - (0.06571 * t) - 6.622;
        double UT = T - lngHour;
        if(UT < 0){
            UT=UT+24;
        }
        if(UT > 24){
            UT=UT-24;
        }
        double localT = UT + gmtOffset*1000*60*60;

        return localT;
    }
    public double getSunset() {
        double lngHour = CURRENT_LONGITUDE/15;
        Calendar c = Calendar.getInstance();
        int gmtOffset = c.get(Calendar.ZONE_OFFSET);
        double day = c.get(Calendar.DAY_OF_YEAR);
        double t = day + ((18 - lngHour) / 24);
        double M = (0.9856 * t) - 3.289;
        double L = M + (1.916 * Math.sin((Math.PI/180)*M)) + (0.020 * Math.sin(2 * (Math.PI/180)*M)) + 282.634;
        if(L < 0){
            L=L+360;
        }
        if(L > 360){
            L=L-360;
        }
        double RA = (180/Math.PI)*Math.atan(0.91764 * Math.tan((Math.PI/180)*L));
        if(RA < 0){
            RA=RA+360;
        }
        if(RA > 360){
            RA=RA-360;
        }
        double Lquadrant  = (Math.floor(L/90)) * 90;
        double RAquadrant  = (Math.floor(RA/90)) * 90;
        RA = RA + (Lquadrant - RAquadrant);
        RA = RA / 15.0;
        double sinDec = 0.39782 * Math.sin((Math.PI/180)*L);
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (Math.cos((Math.PI/180)*90.833) - (sinDec *Math.sin((Math.PI/180)*CURRENT_LATITUDE))) / (cosDec * Math.cos((Math.PI/180)*CURRENT_LATITUDE));
        double H = (180/Math.PI)*Math.acos(cosH);
        H = H / 15.0;
        double T = H + RA - (0.06571 * t) - 6.622;
        double UT = T - lngHour;
        if(UT < 0){
            UT=UT+24;
        }
        if(UT > 24){
            UT=UT-24;
        }
        double localT = UT + gmtOffset*1000*60*60;

        return localT;
    }

}