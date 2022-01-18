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

import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.tuples.generated.Tuple3;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ReportWitnessSightingActivity extends AppCompatActivity {
    TextView txtTitle;
    Button btnAbort;
    Button btnReportSighting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_witness_sighting);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");

        txtTitle = (TextView) findViewById(R.id.txtReportWitnessTitle);
        btnAbort = (Button) findViewById(R.id.btnReportWitnessAbort);
        btnReportSighting = (Button) findViewById(R.id.btnReportWitnessSighting);

        Bundle extras = getIntent().getExtras();
        byte[] beaconId = extras.getByteArray("beaconIdentifier");
        byte[] beaconNonce = extras.getByteArray("beaconNonce");
        byte[] beaconSecret = extras.getByteArray("beaconSecret");
        String beaconMac = extras.getString("beaconMac");
        double latitude = extras.getDouble("posLatitude");
        double longitude = extras.getDouble("posLongitude");

        try {
            BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);
            String witnessAddress = blockchainConnection.getAddressFromNonce(new BigInteger(beaconId));
            Tuple3<BigInteger, String, BigInteger> polStatus = blockchainConnection.getProofOfLocationStatus(witnessAddress);
            BigInteger timestamp = polStatus.component1();
            String addressA = polStatus.component2();
            BigInteger rechedTF = polStatus.component3();
            byte[] witnessPublicKey = Base64.decode(blockchainConnection.getAccount(witnessAddress).component1(), Base64.DEFAULT);
            String txt = "";


            BigInteger lat = BigInteger.valueOf(new Double(latitude*10000000).longValue());
            BigInteger lon = BigInteger.valueOf(new Double(longitude*10000000).longValue());
            byte[] latitudeArray = new byte[32];
            System.arraycopy(lat.toByteArray(), 0, latitudeArray, 32-lat.toByteArray().length, lat.toByteArray().length);
            byte[] longitudeArray = new byte[32];
            System.arraycopy(lon.toByteArray(), 0, longitudeArray, 32-lon.toByteArray().length, lon.toByteArray().length);
            byte[] hash = blockchainConnection.createReportHash(BigInteger.valueOf(0x6A736A73), BigInteger.valueOf(1), beaconSecret, latitudeArray, longitudeArray, "0x019d52E0145525a1Da2A4c2ec3d9aB002865f76E");
            byte[] sighting = BlockchainConnection.createPolSighting(latitudeArray, longitudeArray, BigInteger.valueOf(System.currentTimeMillis() / 1000L), new BigInteger(beaconId));

            txt += "POL: " + polStatus.toString() + "\n";
            txt += "BeaconID: " + BlockchainConnection.byteArrayToHex(beaconId) + "\n";
            txt += "BeaconNonce: " + BlockchainConnection.byteArrayToHex(beaconNonce) + "\n";
            txt += "beaconSecret: " + BlockchainConnection.byteArrayToHex(beaconSecret) + "\n";
            txt += "beaconMac: " + beaconMac + "\n";
            txt += "Witness AddresA: " + addressA + "\n";
            txt += "Witness AddresW: " + witnessAddress + "\n";
//            txt += "Witnrss Public Key: " + BlockchainConnection.byteArrayToHex(witnessPublicKey) + "\n";
            txt += "Lat: " + lat.toString() + "\n";
            txt += "Long: " + lon.toString() + "\n";
            txt += "Lat Array: " + BlockchainConnection.byteArrayToHex(latitudeArray) + "\n";
            txt += "Long Array: " + BlockchainConnection.byteArrayToHex(longitudeArray) + "\n";
            txt += "Sighting: " + BlockchainConnection.byteArrayToHex(sighting) + "\n";
            txt += "Sighting Hash: " + BlockchainConnection.byteArrayToHex(hash) + "\n";

            if(!timestamp.equals(BigInteger.valueOf(0))) {
                txtTitle.setText(txt);
                btnReportSighting.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            blockchainConnection.reportWitness(witnessAddress, sighting, beaconSecret, witnessPublicKey);
                            //blockchainConnection.reportWitness(witnessAddress, latitude, longitude, beaconNonce, beaconSecret, witnessPublicKey);
                            Toast toast = Toast.makeText(v.getContext(), "Sighting reported!", Toast.LENGTH_SHORT);
                            toast.show();
                            finish();
                        } catch (Exception e) {
                            Toast toast = Toast.makeText(v.getContext(), "Something went wrong!", Toast.LENGTH_SHORT);
                            toast.show();
                            e.printStackTrace();
                            Log.e("asd", e.getClass().toString());
                        }
                    }
                });
            } else {
                txt += "There is no Proof of Location Proccess for this beacon";
                txtTitle.setText(txt);
            }

        } catch (Exception e) {
            txtTitle.setText("Something went wrong!");
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