package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RevealSecretActivity extends AppCompatActivity {
    TextView txtTitle;
    Button btnAbort;
    Button btnReveal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reveal_secret);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userEthAddress = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        //String psk = "AuSiyh8KvoTGKXYtsRRfx6nXWZubKraFS9JFhJHWW8XJItR4YiF2fwvunYqLNiNVsDbQtRf49GjtEjhHN63bE2QKOy5a1mU1YqRd0PwchW9vk9v7laYSjJZ8cltj3KwB";
        String psk = sharedPreferences.getString(getString(R.string.beacon_psk), "");
        String beaconId = sharedPreferences.getString(getString(R.string.beacon_id), "");
        BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);

        txtTitle = (TextView) findViewById(R.id.txtRevealTitle);
        btnReveal = (Button) findViewById(R.id.btnReveal);
        btnAbort = (Button) findViewById(R.id.btnAbort);
        Tuple5<String, BigInteger, byte[], byte[], Boolean> sighting;
        String txt;
        try {
            int index = getIntent().getIntExtra("index", 0);
            sighting = blockchainConnection.getSighting(userEthAddress, BigInteger.valueOf(index));
            BigInteger[] searchTask = blockchainConnection.getOngoingSearchOrders().get(userEthAddress.toUpperCase());
            BigInteger trustFactor = searchTask[0];
            String addressW = sighting.component1();
            txt = String.format("Nonce: %s\nHash: %s\nSecret: %s\nPOL: %s", sighting.component2().toString(), byteArrayToHex(sighting.component3()), byteArrayToHex(sighting.component4()), sighting.component5().toString());
            txt += "\nIndex: " + index;
            byte[] id = new byte[]{(byte)0x6A, (byte) 0x73, (byte) 0x6A, (byte) 0x73};
            byte[] nonce = sighting.component2().toByteArray();
            if(nonce.length < 4){
                byte[] tmp = new byte[4];
                System.arraycopy(nonce, 0, tmp, 4-nonce.length, nonce.length);;
                nonce = tmp;
            }
            byte[] payload = ArrayUtils.addAll(id, nonce);
            Mac sha256hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(psk.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256hmac.init(secretKey);
            byte[] hash = new byte[16];
            System.arraycopy(sha256hmac.doFinal(payload), 0, hash, 0, 16);
            txt += "\nC++ Hash : " + Arrays.toString(sighting.component3());
            txt += "\nJava Hash: " + Arrays.toString(hash);

            if(sighting.component3().equals(new byte[]{(byte) 0x30, (byte) 0x78, (byte) 0x30, (byte) 0x30})){
                txt += "\nReveal secret? This can not be undone!";
                btnReveal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            String psk = "AuSiyh8KvoTGKXYtsRRfx6nXWZubKraFS9JFhJHWW8XJItR4YiF2fwvunYqLNiNVsDbQtRf49GjtEjhHN63bE2QKOy5a1mU1YqRd0PwchW9vk9v7laYSjJZ8cltj3KwB";
                            Tuple5<String, BigInteger, byte[], byte[], Boolean> sighting = blockchainConnection.getSighting(userEthAddress, BigInteger.valueOf(index));
                            byte[] id = Base64.decode(beaconId.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                            byte[] nonce = sighting.component2().toByteArray();
                            if(nonce.length < 4){
                                byte[] tmp = new byte[4];
                                System.arraycopy(nonce, 0, tmp, 4-nonce.length, nonce.length);
                                nonce = tmp;
                            }
                            byte[] payload = ArrayUtils.addAll(id, nonce);
                            Mac sha256hmac = Mac.getInstance("HmacSHA256");
                            SecretKeySpec secretKey = new SecretKeySpec(psk.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                            sha256hmac.init(secretKey);
                            byte[] hash = new byte[16];
                            System.arraycopy(sha256hmac.doFinal(payload), 0, hash, 0, 16);
                            blockchainConnection.revealSecret(userEthAddress, hash);
                            Toast toast = Toast.makeText(v.getContext(), "Secret Reveled!", Toast.LENGTH_SHORT);
                            toast.show();
                            finish();
                        } catch (Exception e){
                            Toast toast = Toast.makeText(v.getContext(), "Something went Wrong!", Toast.LENGTH_SHORT);
                            toast.show();

                        }
                    }
                });
            } else {
                txt += "\nThe secret has already been revealed. you can reveal it again and override it, but this will cost gas";
                btnReveal.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            String psk = "AuSiyh8KvoTGKXYtsRRfx6nXWZubKraFS9JFhJHWW8XJItR4YiF2fwvunYqLNiNVsDbQtRf49GjtEjhHN63bE2QKOy5a1mU1YqRd0PwchW9vk9v7laYSjJZ8cltj3KwB";
                            Tuple5<String, BigInteger, byte[], byte[], Boolean> sighting = blockchainConnection.getSighting(userEthAddress, BigInteger.valueOf(index));
                            byte[] id = Base64.decode(beaconId.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                            //byte[] id = new byte[]{(byte)0x6A, (byte) 0x73, (byte) 0x6A, (byte) 0x73};
                            byte[] nonce = sighting.component2().toByteArray();
                            if(nonce.length < 4){
                                byte[] tmp = new byte[4];
                                System.arraycopy(nonce, 0, tmp, 4-nonce.length, nonce.length);;
                                nonce = tmp;
                            }
                            byte[] payload = ArrayUtils.addAll(id, nonce);
                            Mac sha256hmac = Mac.getInstance("HmacSHA256");
                            SecretKeySpec secretKey = new SecretKeySpec(psk.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                            sha256hmac.init(secretKey);
                            byte[] hash = new byte[16];
                            System.arraycopy(sha256hmac.doFinal(payload), 0, hash, 0, 16);
                            blockchainConnection.revealSecret(addressW, hash);
                            Toast toast = Toast.makeText(v.getContext(), "Secret Reveled!", Toast.LENGTH_SHORT);
                            toast.show();
                            finish();
                        } catch (Exception e){
                            Toast toast = Toast.makeText(v.getContext(), "Something went Wrong!", Toast.LENGTH_SHORT);
                            toast.show();
                            e.printStackTrace();
                        }
                    }
                });

            }
            txtTitle.setText(txt);





        } catch (Exception e){
            txtTitle.setText("An Error happened while talking with the Blockchain!");
            e.printStackTrace();
        }




        btnAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public static String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b));
        return sb.toString();
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