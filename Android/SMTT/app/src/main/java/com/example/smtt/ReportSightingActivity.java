package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ReportSightingActivity extends AppCompatActivity {
    TextView txtMessage;
    Button btnReport;
    Button btnAbort;
    byte[] hash;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_sighting);
        //SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userEthAddress = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);
//        blockchainConnection.load(contractAddress);
        txtMessage = (TextView) findViewById(R.id.txtRevealTitle);
        btnReport = (Button) findViewById(R.id.btnReveal);
        btnAbort = (Button) findViewById(R.id.btnAbort);
        txtMessage = (TextView) findViewById(R.id.txtRevealTitle);
        Bundle extras = getIntent().getExtras();
        BigInteger identifier = new BigInteger(extras.getByteArray("beaconIdentifier"));
        byte[] secret = extras.getByteArray("beaconSecret");
        BigInteger nonce = new BigInteger(extras.getByteArray("beaconNonce"));
        double longitude = extras.getDouble("posLongitude");
        double latitude = extras.getDouble("posLatitude");
        System.out.println(latitude);
        int solLatitude;
        int solLongitude;
        int latLength = String.valueOf(latitude).length();
        int lonLength = String.valueOf(longitude).length();
        int offset = Math.max(latLength, lonLength) - 8;
        if(offset > 0){
            solLatitude = Integer.valueOf(String.valueOf(latitude).replace(".", "").substring(0, latLength - offset));
            solLongitude = Integer.valueOf(String.valueOf(longitude).replace(".", "").substring(0, latLength - offset));
        } else {
            solLatitude = Integer.valueOf(String.valueOf(latitude).replace(".", ""));
            solLongitude = Integer.valueOf(String.valueOf(longitude).replace(".", ""));
        }
        latLength = String.valueOf(solLatitude).length();
        lonLength = String.valueOf(solLongitude).length();

        if(String.valueOf(latitude).split("\\.")[0].length() < String.valueOf(longitude).split("\\.")[0].length()){
            solLatitude = Integer.valueOf(String.valueOf(solLatitude).substring(0, latLength - Math.abs(String.valueOf(latitude).split("\\.")[0].length() - String.valueOf(longitude).split("\\.")[0].length())));
        } else if(String.valueOf(longitude).split("\\.")[0].length() < String.valueOf(latitude).split("\\.")[0].length()){
            solLongitude = Integer.valueOf(String.valueOf(solLongitude).substring(0, lonLength - Math.abs(String.valueOf(latitude).split("\\.")[0].length() - String.valueOf(longitude).split("\\.")[0].length())));
        }

        BigInteger lat = BigInteger.valueOf(solLatitude);
        BigInteger lon = BigInteger.valueOf(solLongitude);


        byte[] latitudeArray = new byte[32];
        System.arraycopy(lat.toByteArray(), 0, latitudeArray, 32-lat.toByteArray().length, lat.toByteArray().length);
        byte[] longitudeArray = new byte[32];
        System.arraycopy(lon.toByteArray(), 0, longitudeArray, 32-lon.toByteArray().length, lon.toByteArray().length);

        try{
            hash = blockchainConnection.createReportHash(identifier, nonce, secret, latitudeArray, longitudeArray, userEthAddress);
        } catch(Exception e){
            e.printStackTrace();
            hash = new byte[32];
        }

        String deviceMac = extras.getString("beaconMac");
        String text = "";
        text += String.format("Longitude: %s, Latitude: %s", longitude, latitude) + "\n";
        text += "\nBeacon: " + deviceMac;
        text += "\nIdentifier: " + byteArrayToHex(identifier.toByteArray());
        text += "\nNonce: " + byteArrayToHex(nonce.toByteArray());
        text += "\nSecret: " + byteArrayToHex(secret);
        try {
            text += "\nHash: "+byteArrayToHex(hash);
        } catch (Exception e) {
            text += "\nHashing error";
            e.printStackTrace();
        }
        text +="\n\n";
        try {
            String aAddress = blockchainConnection.getAddressFromNonce(identifier);
            HashMap<String, BigInteger[]> searchOrders = blockchainConnection.getOngoingSearchOrders();
            if(searchOrders.containsKey(aAddress.toUpperCase())) {
                BigInteger trustfactor = searchOrders.get(aAddress.toUpperCase())[0];
                BigDecimal reward = Convert.fromWei(searchOrders.get(aAddress.toUpperCase())[1].toString(), Convert.Unit.ETHER);
                text +=String.format("Reward: %s ETH, Required trustfactor: %s\n\n Do you want to report this sighting? This will cost gas fees!", reward, trustfactor);
                int finalSolLatitude = solLatitude;
                int finalSolLongitude = solLongitude;
                btnReport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        blockchainConnection.reportSighting(aAddress, nonce, hash);
                        Set<String> sightingAddresses =sharedPreferences.getStringSet("SightingAddresses", new HashSet<String>());
                        sightingAddresses.add(aAddress);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("Lat"+aAddress, finalSolLatitude);
                        editor.putInt("Long"+aAddress, finalSolLongitude);
                        editor.putString("Secret"+aAddress, Base64.encodeToString(secret, Base64.DEFAULT));
                        editor.putStringSet("SightingAddresses",sightingAddresses);
                        editor.commit();
                        Intent intent = new Intent(v.getContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                    }
                });
            } else {
                text +="There is no search order for this beacon";
                btnReport.setText("Reload");
                btnReport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                        overridePendingTransition(0, 0);
                        startActivity(getIntent());
                        overridePendingTransition(0, 0);
                    }
                });            }

        } catch (Exception e) {
            text +="An error occurred while fetching data from the blockchain";
            btnReport.setText("Retry");
            btnReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(getIntent());
                    overridePendingTransition(0, 0);
                }
            });
            e.printStackTrace();
        }
        txtMessage.setText(text);


        btnAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });



    }

    public static byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

}