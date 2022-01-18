package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.web3j.crypto.Hash;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple3;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.crypto.Cipher;

public class PolInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pol_info);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthAccount = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userPublicKey = sharedPreferences.getString(getString(R.string.user_publicKey), "");
        String userPrivateKey = sharedPreferences.getString(getString(R.string.user_privateKey), "");
        Set<String> sightingAddresses = sharedPreferences.getStringSet("SightingAddresses", new HashSet<String>());

        Button btnFinish = (Button) findViewById(R.id.btnPolFinish);
        Button btnAbort = (Button) findViewById(R.id.btnPolAbort);

        TextView txtTitle = (TextView) findViewById(R.id.txtPolInfoTitle);
        BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);
        System.out.println(BlockchainConnection.byteArrayToHex(Base64.decode(userPrivateKey, Base64.DEFAULT)));
        try{
            String txt = "";
            Tuple3<BigInteger, String, BigInteger> polStatus = blockchainConnection.getProofOfLocationStatus(userEthAccount);
            BigInteger stake = blockchainConnection.getAccount(userEthAccount).component3();
            BigInteger polTimestamp = polStatus.component1();
            String addressA = polStatus.component2();
            BigDecimal lonBeacon = BigDecimal.valueOf(sharedPreferences.getInt("Long"+addressA, -1));
            BigDecimal latBeacon = BigDecimal.valueOf(sharedPreferences.getInt("Lat"+addressA, -1));
            LinkedList<Tuple2<byte[], byte[]>> sightngs = blockchainConnection.getPolSightigs(userEthAccount);
            txt += "Count: " + sightngs.size() + "\n";
            txt += "Beacon Pos: " + latBeacon.divide(BigDecimal.valueOf(1000000L)) + " " + lonBeacon.divide(BigDecimal.valueOf(1000000L)) + "\n\n";
            int i = 0;
            BigDecimal totalTF = BigDecimal.ZERO;
            ArrayList sightingList = new ArrayList();
            ArrayList indexes = new ArrayList();
            for(Tuple2<byte[], byte[]> t : sightngs){
                txt += "---- Sighting " + i + " ----\n";
                byte[] sighting;
                try {
                    Cipher rsa = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(userPrivateKey, Base64.DEFAULT));
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    PrivateKey key = keyFactory.generatePrivate(spec);
                    rsa.init(Cipher.DECRYPT_MODE, key);
                    sighting = rsa.doFinal(t.component1());
                } catch(Exception e){
                    txt += "Unable to decrypt\n";
                    e.printStackTrace();
                    continue;
                }
                if(!BlockchainConnection.byteArrayToHex(Hash.sha3(sighting)).equals(BlockchainConnection.byteArrayToHex(t.component2()))){
                    txt += "Invalide\n";
                    continue;
                }
                byte[] latArray = new byte[32];
                byte[] lonArray = new byte[32];
                byte[] timestampArray = new byte[32];
                byte[] idArray = new byte[4];
                System.arraycopy(sighting, 0, latArray, 0 ,32);
                System.arraycopy(sighting, 32, lonArray, 0 ,32);
                System.arraycopy(sighting, 64, timestampArray, 0 ,32);
                System.arraycopy(sighting, 96, idArray, 0 ,4);

                BigDecimal lat = new BigDecimal(new BigInteger(latArray));
                BigDecimal lon = new BigDecimal(new BigInteger(lonArray));
                BigInteger timestamp = new BigInteger(timestampArray);
                BigInteger id = new BigInteger(idArray);
                BigInteger dS = blockchainConnection.distanceInMeter(latBeacon.toBigInteger().multiply(BigInteger.TEN), lonBeacon.toBigInteger().multiply(BigInteger.TEN), lat.toBigInteger(), lon.toBigInteger());
                BigInteger dT = timestamp.subtract(polTimestamp);
                //BigDecimal TF = calculateTrustFactor(dS, dT, stake);
                BigDecimal TF = new BigDecimal(blockchainConnection.calculateTrustfactor(dT.abs(), dS.abs(), stake));
                totalTF = totalTF.add(TF);

                //txt += t.toString() + "\n";
                //txt += "S: "+Arrays.toString(sighting) + "\n";
                txt += "Lat: " + lat.divide(BigDecimal.valueOf(10000000)) + "  ";
                txt += "Lon: " + lon.divide(BigDecimal.valueOf(10000000)) + "\n";
                txt += "ID: " + id + "  ";
                txt += "Time: "+ timestamp + "\n";
                txt += "dT: " + dT + ", ";
                txt += "dS: " + dS + ", ";
                txt += "TF: " + TF + "\n";
                if(TF.intValue() > 0) {
                    sightingList.add(sighting);
                    indexes.add(BigInteger.valueOf(i));
                }
                i++;
            }
            BigInteger[] searchTask = blockchainConnection.getOngoingSearchOrders().get(addressA.toUpperCase());
            BigInteger requiredTrustfactor = searchTask[0];
            txt += "\n\nRequired TrustFactor: " + requiredTrustfactor + "\n";
            txt += "Total TrustFactor: " + totalTF + "\n";

            BigInteger lat = latBeacon.toBigInteger();
            BigInteger lon = lonBeacon.toBigInteger();
            byte[] latitudeArray = new byte[32];
            System.arraycopy(lat.toByteArray(), 0, latitudeArray, 32-lat.toByteArray().length, lat.toByteArray().length);
            byte[] longitudeArray = new byte[32];
            System.arraycopy(lon.toByteArray(), 0, longitudeArray, 32-lon.toByteArray().length, lon.toByteArray().length);


            if(requiredTrustfactor.compareTo(totalTF.toBigInteger()) == -1){
                txt += "The required Trustfactor has been reached, you can end the search.";
                btnFinish.setText("Finish Search");
                btnFinish.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ArrayList a = new ArrayList();
                            a.add(new byte[100]);
                            blockchainConnection.endPOL(sightingList, indexes, ArrayUtils.addAll(latitudeArray, longitudeArray));
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putStringSet("SightingAddresses", new HashSet<String>());
                            editor.commit();
                            //blockchainConnection.endPOL(a, ArrayUtils.addAll(latitudeArray, longitudeArray));
                        } catch (Exception e) {
                            Toast toast = Toast.makeText(v.getContext(), "Error while talking to the Blockchain!", Toast.LENGTH_SHORT);
                            toast.show();
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                btnFinish.setText("Reload");
                btnFinish.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        overridePendingTransition(0, 0);
                        finish();
                        overridePendingTransition(0, 0);
                        startActivity(getIntent());
                    }
                });
            }

            txtTitle.setText(txt);

        } catch (Exception e){
            txtTitle.setText("An error occurred while talking to the Blockchain");
            e.printStackTrace();
        }

    }

    private BigDecimal calculateTrustFactor(BigInteger deltaS, BigInteger deltaT, BigInteger stakeAnchor){
        BigDecimal dS = new BigDecimal(deltaS);
        BigDecimal dT = new BigDecimal(deltaT).divide(BigDecimal.valueOf(60*60), 4, RoundingMode.HALF_UP);
        BigDecimal stake = new BigDecimal(stakeAnchor);

        if(dT.equals(BigInteger.ZERO)){
            return stake;
        }
        BigDecimal a = stake.divide(dT.multiply(BigDecimal.valueOf(50)).add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        BigDecimal b = dT.multiply(BigDecimal.valueOf(50)).subtract(dS).divide(dT.multiply(BigDecimal.valueOf(50)), 2, RoundingMode.HALF_UP);
        BigDecimal val = a.multiply(b);
        System.out.println(a + " " + b + " " + val);
        if(val.compareTo(BigDecimal.ZERO) == 1){
            return val;
        } else {
            return BigDecimal.ZERO;
        }
    }


}