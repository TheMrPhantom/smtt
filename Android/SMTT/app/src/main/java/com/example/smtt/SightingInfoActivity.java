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
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class SightingInfoActivity extends AppCompatActivity {
    TextView txtTitle;
    Button btnStartPOL;
    Button btnAbort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sighting_info);
        txtTitle = findViewById(R.id.txtSightingInfoTitle);
        btnStartPOL = findViewById(R.id.btnStartPOL);
        btnAbort = findViewById(R.id.btnSightingInfoAbort);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthAccount = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userPublicKey = sharedPreferences.getString(getString(R.string.user_publicKey), "");
        String userPrivateKey = sharedPreferences.getString(getString(R.string.user_privateKey), "");
        Set<String> sightingAddresses = sharedPreferences.getStringSet("SightingAddresses", new HashSet<String>());

        Bundle extras = getIntent().getExtras();
        String addressA = extras.getString("addressA");
        String addressW = extras.getString("addressW");
        BigInteger timestamp = (BigInteger) extras.get("timestamp");

        byte[] reportedSecret = Base64.decode(sharedPreferences.getString("Secret"+addressA, ""), Base64.DEFAULT);

        try {
            BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);
            BigInteger[] searchTask = blockchainConnection.getOngoingSearchOrders().get(addressA.toUpperCase());
            Tuple5<String, BigInteger, byte[], byte[], Boolean> sighting = null;
            boolean a= true;
            int i = 0;
            while(a) {
                sighting = blockchainConnection.getSighting(addressA, BigInteger.valueOf(i));
                System.out.println(sighting.toString());
                if(sighting.component1().equalsIgnoreCase(userEthAccount.toUpperCase())){
                    a = false;
                }
                i++;
            }
            boolean secretRevealed = !sighting.component4().equals(new byte[]{(byte) 0x30, (byte) 0x78, (byte) 0x30, (byte) 0x30});
            Tuple3<BigInteger, BigInteger, BigInteger> searchOrder = blockchainConnection.getSearchTask(addressA);
            String txt = "";
            txt += "Sighting Info:\n";
            txt += "Timestamp: " + timestamp.toString() + "\n";
            txt += "Nonce: " + sighting.component2().toString() + "\n";
            txt += "Hashed Sighting: " + BlockchainConnection.byteArrayToHex(sighting.component3()) + "\n";
            txt += "POL Started: " + sighting.component5().toString() + "\n";
            txt += "Required TF: " + searchOrder.component1() +"\n";
            txt += "Reported Secret: " + BlockchainConnection.byteArrayToHex(reportedSecret) + "\n";
            if(secretRevealed) {
                txt += "Secret: " + BlockchainConnection.byteArrayToHex(sighting.component4()) + "\n\n";
                if(searchOrder.component1().equals(BigInteger.ZERO)){
                    btnStartPOL.setText("Finish Search");
                    if(Arrays.equals(reportedSecret, sighting.component4())){
                        txt += "The secret reveled by the Task Creator matches the one reported from the beacon!\nYou can safely end the Search now";

                        btnStartPOL.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    BigDecimal lonBeacon = BigDecimal.valueOf(sharedPreferences.getInt("Long"+addressA, -1));
                                    BigDecimal latBeacon = BigDecimal.valueOf(sharedPreferences.getInt("Lat"+addressA, -1));
                                    BigInteger lat = latBeacon.toBigInteger();
                                    BigInteger lon = lonBeacon.toBigInteger();
                                    byte[] latitudeArray = new byte[32];
                                    System.arraycopy(lat.toByteArray(), 0, latitudeArray, 32-lat.toByteArray().length, lat.toByteArray().length);
                                    byte[] longitudeArray = new byte[32];
                                    System.arraycopy(lon.toByteArray(), 0, longitudeArray, 32-lon.toByteArray().length, lon.toByteArray().length);
                                    blockchainConnection.finishSearch(addressA, addressW, ArrayUtils.addAll(latitudeArray, longitudeArray), v.getContext());
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putStringSet("SightingAddresses", new HashSet<String>());
                                    editor.commit();

                                } catch(Exception e){
                                    Toast toast = Toast.makeText(v.getContext(), "Something went wrong", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });

                    } else {
                        txt += "The secret reveled by the task creator does not match the secret reported by the beacon!";
                        btnStartPOL.setText("Reload");
                        btnStartPOL.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                                startActivity(getIntent());
                            }
                        });
                    }
                } else {
                    if (sighting.component5().booleanValue()) {
                        txt += "Proof of Location has already been started";
                        btnStartPOL.setText("View POL");
                        btnStartPOL.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(v.getContext(), PolInfoActivity.class);
                                startActivity(intent);
                            }
                        });
                    } else if (Arrays.equals(reportedSecret, sighting.component4())) {
                        txt += "The secret reveled by the Task Creator matches the one reported from the beacon!\nYou can safely start the Proof of Location process now";

                        btnStartPOL.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    blockchainConnection.startPOL(addressA, addressW, BigInteger.valueOf(System.currentTimeMillis() / 1000L));
                                    Toast toast = Toast.makeText(v.getContext(), "Proof of Location started!", Toast.LENGTH_SHORT);
                                    toast.show();
                                } catch (Exception e) {
                                    Toast toast = Toast.makeText(v.getContext(), "Something went wrong", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });

                    } else {
                        txt += "The secret reveled by the task creator does not match the secret reported by the beacon!";
                        btnStartPOL.setText("Reload");
                        btnStartPOL.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                                startActivity(getIntent());
                            }
                        });
                    }
                }


            } else {
                txt += "Secret: Not Reveled";
                txtTitle.setText(txt);
            }

            txtTitle.setText(txt);
        } catch(Exception e){
            txtTitle.setText("An error occurred while talking to the Blockchain");
            e.printStackTrace();
        }

        btnAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }
}