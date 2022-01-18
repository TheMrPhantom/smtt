package com.example.smtt;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.apache.commons.lang3.ArrayUtils;
import org.web3j.tuples.generated.Tuple4;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class BluetoothAdvertiseService extends Service {
    AdvertisingSet currentAdvertisingSet;
    byte[] currentAdvertisingData;
    BigInteger currentNonce;
    byte[] beaconPSK;
    byte[] id;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeAdvertiser advertiser = adapter.getBluetoothLeAdvertiser();
        AdvertisingSetParameters parameters = (new AdvertisingSetParameters.Builder())
                .setConnectable(false)
                .setLegacyMode(true)
                .setInterval(500)
                .build();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        id = Base64.decode(sharedPreferences.getString(getString(R.string.beacon_id), ""), Base64.DEFAULT);
        String beaconPskString = sharedPreferences.getString("witnessKey", "");

        if(beaconPskString.equals("")){
            try {
                SecretKey psk = KeyGenerator.getInstance("HmacSHA256").generateKey();
                beaconPskString = Base64.encodeToString(psk.getEncoded(), Base64.DEFAULT);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("witnessKey", beaconPskString);
                editor.commit();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        this.beaconPSK = Base64.decode(beaconPskString, Base64.DEFAULT);

        AdvertisingSetCallback callback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                Log.i("BLE Advertiser", "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status + advertisingSet);
                currentAdvertisingSet = advertisingSet;
            }

            @Override
            public void onPeriodicAdvertisingDataSet (AdvertisingSet advertisingSet, int status){
                Log.i("BLE Advertiser", "onAdvertisingSetStarted(): status: "
                        + status);
            }

            @Override
            public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                Log.i("BLE Advertiser", "onAdvertisingSetStopped():");
            }
        };


        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        currentAdvertisingData = new byte[24];
        dataBuilder.addManufacturerData(0x0FD0, currentAdvertisingData);

        advertiser.startAdvertisingSet(parameters, dataBuilder.build(), null, null, dataBuilder.build(), callback);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentAdvertisingData = generateManufacturerData();
                AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
                dataBuilder.addManufacturerData(0x0FD0, currentAdvertisingData);
                currentAdvertisingSet.setAdvertisingData(dataBuilder.build());
                System.out.println("New Nonce: " + currentNonce.toString() + " New Manufacturer Data: " + BlockchainConnection.byteArrayToHex(currentAdvertisingData));
            }
        }, 100, 5000);//wait 0 ms before doing the action and do it evry 1000ms (1second)

        currentNonce = BigInteger.valueOf((System.currentTimeMillis()/1000L)/5);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationChannel channel = new NotificationChannel("com.example.smtt", "SMTT BLE", NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(true);
        channel.setDescription("Muchentuchen");
        notificationManager.createNotificationChannel(channel);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Log.i("BLE Advertiser", "Service Started");
        startForeground(1, new NotificationCompat.Builder(this, "com.example.smtt")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("You are advertising vie Bluetooth Low Energy")
                .setContentIntent(pendingIntent)
                .build());


        return super.onStartCommand(intent, flags, startId);
    }

    private byte[] generateManufacturerData(){
        try {
            BigInteger newNonce = currentNonce.add(BigInteger.valueOf(1));
            currentNonce = newNonce;

            String psk = "AuSiyh8KvoTGKXYtsRRfx6nXWZubKraFS9JFhJHWW8XJItR4YiF2fwvunYqLNiNVsDbQtRf49GjtEjhHN63bE2QKOy5a1mU1YqRd0PwchW9vk9v7laYSjJZ8cltj3KwB";
            byte[] nonce = newNonce.toByteArray();
            if (nonce.length < 4) {
                byte[] tmp = new byte[4];
                System.arraycopy(nonce, 0, tmp, 4 - nonce.length, nonce.length);
                nonce = tmp;
            }
            byte[] payload = ArrayUtils.addAll(id, nonce);
            Mac sha256hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(beaconPSK, "HmacSHA256");
            sha256hmac.init(secretKey);
            byte[] hash = new byte[16];
            System.arraycopy(sha256hmac.doFinal(payload), 0, hash, 0, 16);
            byte[] manufacturerData = new byte[24];
            System.arraycopy(id,0, manufacturerData, 0, 4);
            System.arraycopy(nonce,0, manufacturerData, 4, 4);
            System.arraycopy(hash,0, manufacturerData, 8, 16);
            return manufacturerData;
        } catch (Exception e){
            e.printStackTrace();
            return new byte[24];
        }

    }


}
