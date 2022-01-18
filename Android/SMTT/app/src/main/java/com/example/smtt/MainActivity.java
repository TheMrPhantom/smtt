package com.example.smtt;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.TimeAnimator;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.web3j.tx.Contract;
import org.web3j.utils.Bytes;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jnr.ffi.annotations.In;

public class MainActivity extends AppCompatActivity {
    Button btnDeploy;
    Button btnStartSearchTask;
    Button btnFinish;
    Button btnAdvertisePosition;
    Button btnMySearchTask;
    Button btnMySightings;
    TextView txtTop;
    ListView listDevices;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bleScanner;
    ArrayList<String> test = new ArrayList<String>();
    ArrayList<String> ddd = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    BlockchainConnection blockchainConnection;


    private ScanCallback leScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            int idLength = 4;
            int nonceLength = 4;
            int secretLength = 16;
            Byte[] smttIdentifier = new Byte[]{(byte) 0xF0, (byte) 0x0D};
            Byte[] witnessIdentifier = new Byte[]{(byte) 0xD0, (byte) 0x0F};
            BluetoothDevice device = result.getDevice();
            Byte[] rawData = ArrayUtils.toObject(result.getScanRecord().getBytes());
            List<Byte> bytes = Arrays.asList(rawData);
            Iterator<Byte> it = bytes.iterator();
            ArrayList<Byte> manufacturerData = new ArrayList<Byte>();
            List<Byte> manufacturerID = new ArrayList<Byte>();
            Byte[] beaconIdentifier;
            Byte[] beaconSecret;
            Byte[] beaconNonce;
            String tmp = "";

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
            if (!ddd.contains(device.getAddress())) {
                if (manufacturerID.equals(Arrays.asList(smttIdentifier))) {
                    if (manufacturerData.size() != 26) {
                        return;
                    }
                    beaconIdentifier = manufacturerData.subList(2, 2 + idLength).toArray(new Byte[idLength]);
                    beaconNonce = manufacturerData.subList(2 + idLength, 2 + idLength + nonceLength).toArray(new Byte[nonceLength]);
                    beaconSecret = manufacturerData.subList(2 + idLength + nonceLength, 2 + idLength + nonceLength + secretLength).toArray(new Byte[secretLength]);
                    tmp = tmp + "SMTT Beacon:\n";
                    tmp = tmp + "Mac: " + device.getAddress() + "\n";
                    tmp = tmp + "ID: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(beaconIdentifier)) + "\n";
                    tmp = tmp + "Nonce: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(beaconNonce)) + "\n";
                    tmp = tmp + "Secret: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(beaconSecret));
                    test.add(0, tmp);
                    ddd.add(0, device.getAddress());
                } else if (manufacturerID.equals(Arrays.asList(witnessIdentifier))) {
                    beaconIdentifier = manufacturerData.subList(2, 2 + idLength).toArray(new Byte[idLength]);
                    beaconNonce = manufacturerData.subList(2 + idLength, 2 + idLength + nonceLength).toArray(new Byte[nonceLength]);
                    beaconSecret = manufacturerData.subList(2 + idLength + nonceLength, 2 + idLength + nonceLength + secretLength).toArray(new Byte[secretLength]);
                    tmp = tmp + "Witness Beacon:\n";
                    tmp = tmp + "Mac: " + device.getAddress() + "\n";
                    tmp = tmp + "ID: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(beaconIdentifier)) + "\n";
                    tmp = tmp + "Nonce: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(beaconNonce)) + "\n";
                    tmp = tmp + "Secret: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(beaconSecret)) + "";
                    test.add(0, tmp);
                    ddd.add(0, device.getAddress());
                } else {
                    tmp = tmp + "Other Device:\n";
                    tmp = tmp + "Mac: " + device.getAddress() + "\n";
                    tmp = tmp + "M-ID: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(manufacturerID.toArray(new Byte[manufacturerID.size()]))) + "\n";
                    tmp = tmp + "M-Data: " + BlockchainConnection.byteArrayToHex((byte[]) ArrayUtils.toPrimitive(manufacturerData.toArray(new Byte[manufacturerData.size()])));
                    test.add(tmp);
                    ddd.add(device.getAddress());
                }
                adapter.notifyDataSetChanged();

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED){
        } else {
            requestPermissions(
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH },
                    1);
        }

        ActivityManager manager = (ActivityManager) getSystemService(this.ACTIVITY_SERVICE);
        boolean serviceAlive = false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BluetoothScanService.class.getName().equals(service.service.getClassName())) {
                serviceAlive = true;
            }
        }
        if(!serviceAlive) {
            startService(new Intent(this, BluetoothScanService.class));
        } else{
            Log.i("SMTT INFO", "BLE Service aleady alive");
        }
        //SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthAccount = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userPublicKey = sharedPreferences.getString(getString(R.string.user_publicKey), "");
        String userPrivateKey = sharedPreferences.getString(getString(R.string.user_privateKey), "");
        String beaconID = sharedPreferences.getString(getString(R.string.beacon_id), "");
        Set<String> sightingAddresses = sharedPreferences.getStringSet("SightingAddresses", new HashSet<String>());
        if(userEthAccount.equals("")){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
            Intent intent = new Intent(this, RegisterActivity.class);
            intent.putExtra("deploy", false);
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);
        btnDeploy = (Button) findViewById(R.id.btnDeploy);
        btnStartSearchTask = (Button) findViewById(R.id.btnCreate);
        btnAdvertisePosition = (Button) findViewById(R.id.btnStartBeacon);
        btnMySearchTask = (Button) findViewById(R.id.btnMySearchTasks);
        btnMySightings = (Button) findViewById(R.id.btnMySightings);
        txtTop = (TextView) findViewById(R.id.txtTop);
        listDevices = (ListView) findViewById(R.id.listDevices);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        bleScanner.startScan(leScanCallback);
        adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_list_item_1, test);
        listDevices.setAdapter(adapter);
        try {
            blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);
            //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            //StrictMode.setThreadPolicy(policy);
            //System.out.println(blockchainConnection.deploySync().getContractAddress());
            //System.out.println(blockchainConnection.deploy().getContractAddress());
            //blockchainConnection.load("0x31a088017ec4d554f107e75bf0b2fd13684e4231");
            Contract smttContract = blockchainConnection.loadSmtt(contractAddress);
            Contract polContract = blockchainConnection.loadPOL();
            if (!blockchainConnection.isRegistered(userEthAccount)) {
                //blockchainConnection.register(BigInteger.valueOf(0x6A736A73));

                Intent intent = new Intent(this, RegisterActivity.class);
                intent.putExtra("deploy", false);
                startActivity(intent);
            }
            System.out.println(blockchainConnection.getAccount(userEthAccount).toString());
            blockchainConnection.startSearchTaskSubscription();
            blockchainConnection.startSightingReportSubscription();
            blockchainConnection.startSearchWitnessSubscription();
            blockchainConnection.startSightingReportedEventSubscriber();
            blockchainConnection.startSearchFinishedEventSubscriber();
            if(userEthAccount.equals("")) {
                txtTop.setText(String.format("Contract loaded successfully\nSMTT Address: %s\n POL Address: %s", smttContract.getContractAddress(), polContract.getContractAddress()));
            } else {
                txtTop.setText(String.format("ETH Account: %s\nContract loaded successfully\nSMTT Address: %s\n POL Address: %s", userEthAccount, smttContract.getContractAddress(), polContract.getContractAddress()));
            }
        } catch (Exception e){
            txtTop.setText("Blockchain connection failed!");
            e.printStackTrace();
            btnDeploy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    blockchainConnection.stopAll();
                    overridePendingTransition(0, 0);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(getIntent());
                }
            });

            btnDeploy.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    try {
                        blockchainConnection.stopAll();
                        Intent intent = new Intent(view.getContext(), RegisterActivity.class);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.commit();
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        Toast toast = Toast.makeText(view.getContext(), "Something went wrong!", Toast.LENGTH_SHORT);
                        toast.show();
                        return true;
                    }
                }
            });
            e.printStackTrace();

            return;
        }


        btnDeploy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               blockchainConnection.stopAll();
                btnDeploy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        blockchainConnection.stopAll();
                        overridePendingTransition(0, 0);
                        finish();
                        overridePendingTransition(0, 0);
                        startActivity(getIntent());
                    }
                });
            }
        });

        btnDeploy.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    blockchainConnection.stopAll();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.commit();
                    Intent intent = new Intent(view.getContext(), RegisterActivity.class);
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Toast toast = Toast.makeText(view.getContext(), "Something went wrong!", Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
            }
        });


        btnStartSearchTask.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), StartSearchTaskActivity.class);
                startActivity(intent);
           }
        });



        btnAdvertisePosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                boolean serviceAlive = false;
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (BluetoothAdvertiseService.class.getName().equals(service.service.getClassName())) {
                        serviceAlive = true;
                    }
                }
                if(!serviceAlive) {
                    startService(new Intent(v.getContext(), BluetoothAdvertiseService.class));
                    Toast toast = Toast.makeText(v.getContext(), "BLE Advertiser started!", Toast.LENGTH_SHORT);
                    toast.show();
                } else{
                    Toast toast = Toast.makeText(v.getContext(), "BLE Advertiser already started!", Toast.LENGTH_SHORT);
                    toast.show();
                }

            }
        });

        btnMySearchTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(blockchainConnection.getOngoingSearchOrders().containsKey(userEthAccount.toUpperCase())){
                    Intent intent = new Intent(v.getContext(), MySearchTasksActivity.class);
                    startActivity(intent);
                } else{
                    Toast toast = Toast.makeText(v.getContext(), "You have no search task!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        btnMySightings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                if(!sightingAddresses.isEmpty()){
                    Intent intent = new Intent(v.getContext(), MySightingsActivity.class);
                    startActivity(intent);
                } else{
                    Toast toast = Toast.makeText(v.getContext(), "You have no sightings!", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });






    }


    private void initiatePreferences(SharedPreferences sharedPreferences){
        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString(getString(R.string.node_url), "http://10.53.101.124:7545");
        editor.putString(getString(R.string.node_url), "http://192.168.6.2:7545");
        editor.putString(getString(R.string.user_eth_address), "0x7167e1A899B128e10Df9BE3a7D77aA2Db3123C53");
        editor.putString(getString(R.string.user_eth_private_key), "5192dd0788116b83360fe64b514842fe7a30c6ce5e8f0e5e41d6eb534d044d53");
        editor.putString(getString(R.string.user_publicKey), "nicht secret");
        editor.putString(getString(R.string.user_privateKey), "sehr secret");
        editor.apply();
    }

}


