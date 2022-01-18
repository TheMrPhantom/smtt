package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings.Secure;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    TextView txtTitle;
    EditText edtNodeURL;
    EditText edtEthAccount;
    EditText edtEthPrivateKey;
    EditText edtPublicKey;
    EditText edtPrivateKey;
    EditText edtBeaconID;
    EditText edtBeaconPSK;
    Button btnRegister;
    Button btnDeploy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txtTitle = (TextView) findViewById(R.id.txtRegisterTitle);
        edtNodeURL = (EditText) findViewById(R.id.edtNodeURL);
        edtEthAccount = (EditText) findViewById(R.id.edtEthAccount);
        edtEthPrivateKey = (EditText) findViewById(R.id.edtEthPrivateKey);
        edtPublicKey = (EditText) findViewById(R.id.edtPublicKey);
        edtPrivateKey = (EditText) findViewById(R.id.edtPrivateKey);
        edtBeaconID = (EditText) findViewById(R.id.edtBeaconID);
        edtBeaconPSK = (EditText) findViewById(R.id.edtBeaconPSK);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        btnDeploy = (Button) findViewById(R.id.btnRegisterAndDeploy);

        txtTitle.setText("In order to participate in SMTT you need to register!\nThis will cost 0.02 ETH");

        edtNodeURL.setText("http://10.53.101.186:7545");
        System.out.println(Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID).toString());
        if(Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID).toString().equals("c772d5a4b0240a7f")) {
            edtEthAccount.setText("0x51648dc47c0715c29FbEa0193ee5AE01CAf2333C");
            edtEthPrivateKey.setText("147cc8b8bdec769a1822650e43021faba33ef8a9e4cbaf77e5e155bd38ad43b2");
            edtBeaconID.setText("0x6A736A73");
        } else if(Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID).toString().equals("8a98f0372f4b0727")){
            edtEthAccount.setText("0x6E5675F34d3a09efE40124C462E5D91014E5F6e1");
            edtEthPrivateKey.setText("e3c5a138d7611c8ba7f05aba463750c589ba770a42cfc54dfdc979d5ec187b19");
            edtBeaconID.setText("0x6A746A74");
        } else {
            edtBeaconID.setText("0x6A756A75");
            edtEthAccount.setText("0x056f303E5455d52221895977ea4618B8f5Dd31B1");
            edtEthPrivateKey.setText("1a6e289e202ee7a9b4d60692b3358abdef5352528050c5b41b9c84ad403676d3");

        }

        edtBeaconPSK.setText("AuSiyh8KvoTGKXYtsRRfx6nXWZubKraFS9JFhJHWW8XJItR4YiF2fwvunYqLNiNVsDbQtRf49GjtEjhHN63bE2QKOy5a1mU1YqRd0PwchW9vk9v7laYSjJZ8cltj3KwB");
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            edtPublicKey.setText(Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT));
            edtPrivateKey.setText(Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT));
            System.out.println("Public: " + edtPublicKey.getText().toString());
            System.out.println("Private: " + edtPrivateKey.getText().toString());

        } catch (Exception e){
            edtPublicKey.setText("Error");
            edtPrivateKey.setText("Error");
            e.printStackTrace();
        }


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nodeURL = edtNodeURL.getText().toString();
                String userEthAccount = edtEthAccount.getText().toString();
                String userEthPrivateKey = edtEthPrivateKey.getText().toString();
                String polPublicKey = edtPublicKey.getText().toString();
                String polPrivateKey = edtPrivateKey.getText().toString();
                String beaconPSK = edtBeaconPSK.getText().toString();
                byte[] beaconID = hexStringToByteArray(edtBeaconID.getText().toString());

                if(nodeURL.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply the Node URL!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(userEthPrivateKey.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply your ETH Private Key!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(polPublicKey.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply your POL Public Key!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(polPrivateKey.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply your POL Private KEy!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(edtBeaconID.getText().toString().equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply the Beacon ID!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(beaconPSK.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply the Beacon PSK!", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    try {
                        BlockchainConnection blockchainConnection = BlockchainConnection.resetInstance(userEthPrivateKey, nodeURL);
                        String contractAddress = "";
                        contractAddress = blockchainConnection.loadSmtt("").getContractAddress();
                        TimeUnit.SECONDS.sleep(1);
                        blockchainConnection.register(new BigInteger(beaconID), polPublicKey);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getString(R.string.node_url), nodeURL);
                        editor.putString(getString(R.string.user_eth_address), userEthAccount);
                        editor.putString(getString(R.string.user_eth_private_key), userEthPrivateKey);
                        editor.putString(getString(R.string.user_publicKey), polPublicKey);
                        editor.putString(getString(R.string.user_privateKey), polPrivateKey);
                        editor.putString(getString(R.string.beacon_id), Base64.encodeToString(beaconID, Base64.DEFAULT));
                        editor.putString(getString(R.string.beacon_psk), beaconPSK);
                        editor.putString(getString(R.string.contract_address), contractAddress);



                        editor.commit();
                        Toast toast = Toast.makeText(view.getContext(), "Registered successfully!", Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    } catch (Exception e){
                        Toast toast = Toast.makeText(view.getContext(), "Something went wrong! Check your inputs", Toast.LENGTH_SHORT);
                        toast.show();
                        e.printStackTrace();
                    }
                }
            }
        });

        btnDeploy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nodeURL = edtNodeURL.getText().toString();
                String userEthAccount = edtEthAccount.getText().toString();
                String userEthPrivateKey = edtEthPrivateKey.getText().toString();
                String polPublicKey = edtPublicKey.getText().toString();
                String polPrivateKey = edtPrivateKey.getText().toString();
                String beaconPSK = edtBeaconPSK.getText().toString();
                byte[] beaconID = hexStringToByteArray(edtBeaconID.getText().toString());

                if(nodeURL.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply the Node URL!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(userEthPrivateKey.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply your ETH Private Key!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(polPublicKey.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply your POL Public Key!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(polPrivateKey.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply your POL Private KEy!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(edtBeaconID.getText().toString().equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply the Beacon ID!", Toast.LENGTH_SHORT);
                    toast.show();
                } else if(beaconPSK.equals("")){
                    Toast toast = Toast.makeText(view.getContext(), "You need to supply the Beacon PSK!", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    try {
                        BlockchainConnection blockchainConnection = BlockchainConnection.resetInstance(userEthPrivateKey, nodeURL);
                        String contractAddress = "";
                        contractAddress = blockchainConnection.deploySmtt().getContractAddress();
                        TimeUnit.SECONDS.sleep(1);
                        blockchainConnection.register(new BigInteger(beaconID), polPublicKey);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getString(R.string.node_url), nodeURL);
                        editor.putString(getString(R.string.user_eth_address), userEthAccount);
                        editor.putString(getString(R.string.user_eth_private_key), userEthPrivateKey);
                        editor.putString(getString(R.string.user_publicKey), polPublicKey);
                        editor.putString(getString(R.string.user_privateKey), polPrivateKey);
                        editor.putString(getString(R.string.beacon_id), Base64.encodeToString(beaconID, Base64.DEFAULT));
                        editor.putString(getString(R.string.beacon_psk), beaconPSK);
                        editor.putString(getString(R.string.contract_address), contractAddress);

                        editor.commit();
                        Toast toast = Toast.makeText(view.getContext(), "Registered successfully!", Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    } catch (Exception e){
                        Toast toast = Toast.makeText(view.getContext(), "Something went wrong! Check your inputs", Toast.LENGTH_SHORT);
                        toast.show();
                        e.printStackTrace();
                    }
                }
            }
        });



    }
    private static byte[] hexStringToByteArray(String s) {
        s = s.replace("0x", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}