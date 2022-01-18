package com.example.smtt;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.ContentInfoCompat;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BluetoothScanService extends Service {
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bleScanner;
    HashMap<String, Long> recentDevices = new HashMap<>();
    double latitude;
    double longitude;

    private ScanCallback scanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (recentDevices.containsKey(device.getAddress())) {
                if ((System.currentTimeMillis() / 1000L) - recentDevices.get(device.getAddress()) < 10) {
                    return;
                } else {
                    recentDevices.replace(device.getAddress(), System.currentTimeMillis() / 1000L);
                }
            } else {
                recentDevices.put(device.getAddress(), System.currentTimeMillis() / 1000L);
            }
            int idLength = 4;
            int nonceLength = 4;
            int secretLength = 16;
            Byte[] smttIdentifier = new Byte[]{(byte) 0xF0, (byte) 0x0D};
            Byte[] witnessIdentifier = new Byte[]{(byte) 0xD0, (byte) 0x0F};
            Byte[] rawData = ArrayUtils.toObject(result.getScanRecord().getBytes());
            List<Byte> bytes = Arrays.asList(rawData);
            Iterator<Byte> it = bytes.iterator();
            ArrayList<Byte> manufacturerData = new ArrayList<Byte>();
            List<Byte> manufacturerID = new ArrayList<Byte>();
            Byte[] beaconIdentifier;
            Byte[] beaconSecret;
            Byte[] beaconNonce;
            while (it.hasNext()) {
                Byte length = it.next();
                if (Byte.toUnsignedLong(length) == 0) {
                    break;
                }
                Byte type = it.next();
                for (long x = Byte.toUnsignedInt(length) - 1; x > 0; x--) {
                    if (type.equals(Byte.valueOf((byte) 0xFF))) {
                        manufacturerData.add(it.next());
                    } else {
                        it.next();
                    }
                }
            }
            if (manufacturerData.size() != 0) {
                manufacturerID = manufacturerData.subList(0, 2);
            }
            if (manufacturerID.equals(Arrays.asList(smttIdentifier))) {
                if (manufacturerData.size() != 26) {
                    return;
                }
                beaconIdentifier = manufacturerData.subList(2, 2 + idLength).toArray(new Byte[idLength]);
                beaconNonce = manufacturerData.subList(2 + idLength, 2 + idLength + nonceLength).toArray(new Byte[nonceLength]);
                beaconSecret = manufacturerData.subList(2 + idLength+ nonceLength, 2 + idLength + nonceLength + secretLength).toArray(new Byte[secretLength]);
                sendBeaconNotification(beaconIdentifier, beaconSecret, beaconNonce, device.getAddress());
            } else if(manufacturerID.equals(Arrays.asList(witnessIdentifier))){
                beaconIdentifier = manufacturerData.subList(2, 2 + idLength).toArray(new Byte[idLength]);
                beaconNonce = manufacturerData.subList(2 + idLength, 2 + idLength + nonceLength).toArray(new Byte[nonceLength]);
                beaconSecret = manufacturerData.subList(2 + idLength+ nonceLength, 2 + idLength + nonceLength + secretLength).toArray(new Byte[secretLength]);
                sendWitnessNotification(beaconIdentifier, beaconSecret, beaconNonce, device.getAddress());
            }
        }
    };

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendBeaconNotification(Byte[] identifier, Byte[] secret, Byte[] nonce, String deviceMac) {
        Log.i("BLE Scan",String.format("Found SMTT Beacon! Address: %s, ID:%s, nonce: %s, secret: %s", deviceMac, byteArrayToHex(ArrayUtils.toPrimitive(identifier)),byteArrayToHex(ArrayUtils.toPrimitive(nonce)), byteArrayToHex(ArrayUtils.toPrimitive(secret))));
        Bundle extras = new Bundle();
        Intent intent = new Intent(this, ReportSightingActivity.class);
        extras.putByteArray("beaconIdentifier", ArrayUtils.toPrimitive(identifier));
        extras.putByteArray("beaconSecret", ArrayUtils.toPrimitive(secret));
        extras.putByteArray("beaconNonce", ArrayUtils.toPrimitive(nonce));
        extras.putString("beaconMac", deviceMac);
        LocationManager locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null){
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            if(longitude == 0.0 && latitude == 0.0){
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.example.smtt");
                builder.setSmallIcon(R.drawable.ic_launcher_foreground);
                builder.setContentTitle("GPS is ot available");
                builder.setContentText("To report a beacon sighting GPS has to be enabled and available");
                builder.setSilent(false);
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(0, builder.build());
                return;
            }
            extras.putDouble("posLongitude", longitude);
            extras.putDouble("posLatitude", latitude);
        } else {
            System.out.println("No Position available.. Abort!");
            return;
        }
        intent.putExtras(extras);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.example.smtt");
        builder.setSmallIcon(R.drawable.ic_launcher_foreground);
        builder.setContentTitle("Found SMTT Beacon");
        builder.setContentText("Open to view the details and report the sighting");
        builder.setSilent(false);
        builder.setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(deviceMac.getBytes()[0], builder.build());
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendWitnessNotification(Byte[] identifier, Byte[] secret, Byte[] nonce, String deviceMac) {
        Log.i("BLE Scan",String.format("Found Witness Beacon! Address: %s, ID:%s, nonce: %s, secret: %s", deviceMac, byteArrayToHex(ArrayUtils.toPrimitive(identifier)),byteArrayToHex(ArrayUtils.toPrimitive(nonce)), byteArrayToHex(ArrayUtils.toPrimitive(secret))));
        Bundle extras = new Bundle();
        Intent intent = new Intent(this, ReportWitnessSightingActivity.class);
        extras.putByteArray("beaconIdentifier", ArrayUtils.toPrimitive(identifier));
        extras.putByteArray("beaconSecret", ArrayUtils.toPrimitive(secret));
        extras.putByteArray("beaconNonce", ArrayUtils.toPrimitive(nonce));
        extras.putString("beaconMac", deviceMac);
        LocationManager locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location != null){
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            if(longitude == 0.0 && latitude == 0.0){
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.example.smtt");
                builder.setSmallIcon(R.drawable.ic_launcher_foreground);
                builder.setContentTitle("GPS is ot available");
                builder.setContentText("To report a beacon sighting GPS has to be enabled and available");
                builder.setSilent(false);
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(0, builder.build());
                return;
            }
            extras.putDouble("posLongitude", longitude);
            extras.putDouble("posLatitude", latitude);
        } else {
            System.out.println("No Position available.. Abort!");
            return;
        }
        intent.putExtras(extras);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.example.smtt");
        builder.setSmallIcon(R.drawable.ic_launcher_foreground);
        builder.setContentTitle("Found Witness Beacon");
        builder.setContentText("Open to view the details and report the sighting");
        builder.setSilent(false);
        builder.setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(deviceMac.getBytes()[0], builder.build());
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ScanSettings.Builder bleBuilder = new ScanSettings.Builder();
        bleBuilder.setScanMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
        bleBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        Log.i("BLE Scanner", "Service Started");
        bleScanner.startScan(null, bleBuilder.build(), scanCallback);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationChannel channel = new NotificationChannel("com.example.smtt", "SMTT BLE", NotificationManager.IMPORTANCE_HIGH);


        channel.setShowBadge(true);
        channel.setDescription("Muchentuchen");
        notificationManager.createNotificationChannel(channel);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new NotificationCompat.Builder(this, "com.example.smtt")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build());


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        bleScanner.stopScan(scanCallback);
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }


}
