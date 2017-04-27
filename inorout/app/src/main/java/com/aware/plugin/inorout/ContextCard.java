package com.aware.plugin.inorout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;


import com.aware.ui.Stream_UI;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.IContextCard;

import java.util.Calendar;

public class ContextCard implements IContextCard {

    //Set how often your card needs to refresh if the stream is visible (in milliseconds)
    private int refresh_interval = 1 * 1000; //1 second = 1000 milliseconds

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {

            if (card != null) {

//Get values from database and put in strings:
                //private double CURRENT_LIGHT, CURRENT_PROXI, CURRENT_SIGNAL1, CURRENT_SIGNAL2, CURRENT_MAGNET, CURRENT_BATTEMP, CURRENT_IN, CURRENT_OUT, IN_TIME = 0.0, OUT_TIME = 0.0, inminute = 0.0, outminute = 0.0;

                Cursor values = sContext.getContentResolver().query(Provider.InOrOut.CONTENT_URI, null, null, null, Provider.InOrOut.TIMESTAMP + " DESC LIMIT 1");
                if (values != null && values.moveToFirst()) {

                    signalText1.setText("GSM-signal: " + String.format("%.1f", values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_SIGNAL1))) + " db");
                    signalText2.setText("GSM-neighbourtower-signal: " + String.format("%.1f", values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_SIGNAL2))) + " db");
                    battText.setText("Battery temperature: " + String.format("%.1f", values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_BATTEMP))) + " \u2103");
                    lightText.setText("Illuminance: " + String.format("%.1f", values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_LIGHT))) + " lux");
                    if (values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_PROXI)) > 50) {
                        proxiText.setText("Proximity: " + "Far");
                    } else {
                        proxiText.setText("Proximity: " + "Near");
                    }
                    inText.setText("Indoor probability: " + String.format("%.1f", values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_IN))) + " %");
                    outText.setText("Outdoor probability: " + String.format("%.1f", values.getDouble(values.getColumnIndex(Provider.InOrOut.CURRENT_OUT))) + " %");

//Puts in/out times in more fancy format
                    double IN_TIME = values.getDouble(values.getColumnIndex(Provider.InOrOut.ELAPSED_INDOOR))/1000;
                    double OUT_TIME = values.getDouble(values.getColumnIndex(Provider.InOrOut.ELAPSED_OUTDOOR))/1000;
                    double inminute = Math.floor(values.getDouble(values.getColumnIndex(Provider.InOrOut.ELAPSED_INDOOR))/1000 / 60);
                    double outminute = Math.floor(values.getDouble(values.getColumnIndex(Provider.InOrOut.ELAPSED_OUTDOOR))/1000 / 60);
                    if (IN_TIME < 60 && OUT_TIME < 5 && outminute  == 0) {
                        intimeText.setText("Indoor time: " + String.format("%.0f s", IN_TIME));
                    } else if (OUT_TIME > 5 || outminute > 0) {
                        intimeText.setText("Indoor time: -");
                    } else {
                        IN_TIME = IN_TIME - inminute * 60;
                        intimeText.setText("Indoor time: " + String.format("%.0f min ", inminute) + String.format("%.0f s", IN_TIME));
                    }
                    if (OUT_TIME < 60 && IN_TIME < 5 && inminute == 0) {
                        outtimeText.setText("Outdoor time: " + String.format("%.0f s", OUT_TIME));
                    } else if (IN_TIME > 5 || inminute > 0) {
                        outtimeText.setText("Outdoor time: -");
                    } else {
                        OUT_TIME = OUT_TIME - outminute * 60;
                        outtimeText.setText("Outdoor time: " + String.format("%.0f min ", outminute) + String.format("%.0f s", OUT_TIME));
                    }
                }
                if (values != null && !values.isClosed()) values.close();
            }
//Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };


//Empty constructor used to instantiate this card
    public ContextCard() {}

//You may use sContext on uiChanger to do queries to databases, etc.
    private Context sContext;

//Declare here all the UI elements you'll be accessing
    private View card;

//Used to load your context card
    private LayoutInflater sInflater;

    private TextView signalText1, signalText2, lightText, battText, proxiText, inText, outText, intimeText, outtimeText;

//Some variables used

    @Override
    public View getContextCard(Context context) {

        sContext = context;

//Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

//Inflate and return your card's layout. See LayoutInflater documentation.
        sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.activity_settings, null);

        signalText1 = (TextView) card.findViewById(R.id.signalText1);
        signalText2 = (TextView) card.findViewById(R.id.signalText2);
        battText = (TextView) card.findViewById(R.id.battText);
        lightText = (TextView) card.findViewById(R.id.lightText);
        proxiText = (TextView) card.findViewById(R.id.proxiText);
        inText = (TextView) card.findViewById(R.id.inText);
        outText = (TextView) card.findViewById(R.id.outText);
        intimeText = (TextView) card.findViewById(R.id.intimeText);
        outtimeText = (TextView) card.findViewById(R.id.outtimeText);

        uiRefresher.post(uiChanger);
        
        return card;
    }


    private StreamObs streamObs = new StreamObs();

    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN)) {

//start refreshing when user enters the stream
                uiRefresher.postDelayed(uiChanger, refresh_interval);
            }
            if (intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED)) {

//stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
}
