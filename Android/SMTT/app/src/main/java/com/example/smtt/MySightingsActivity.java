package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.widget.TextView;

import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple3;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class MySightingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sightings);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthAccount = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userPublicKey = sharedPreferences.getString(getString(R.string.user_publicKey), "");
        String userPrivateKey = sharedPreferences.getString(getString(R.string.user_privateKey), "");
        Set<String> sightingAddresses = sharedPreferences.getStringSet("SightingAddresses", new HashSet<String>());

        TextView txtTitle = (TextView) findViewById(R.id.txtMySightingsTitle);
        ListView listSightings = (ListView) findViewById(R.id.listSightings);


        BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey,nodeUrl);


        if(sightingAddresses.isEmpty()){
            txtTitle.setText("You have no pending Sightings!");
        } else {
            HashMap<String, LinkedList<Tuple2<String, BigInteger>>> sightings = blockchainConnection.getReportedSightings();
            LinkedList<Tuple3<String, String, BigInteger>> list = new LinkedList();
            Iterator<String> iterator = sightingAddresses.iterator();
            while(iterator.hasNext()){
                String addressA = iterator.next();
                LinkedList<Tuple2<String, BigInteger>> sightingList = sightings.get(addressA.toUpperCase());
                for(Tuple2<String, BigInteger> entry: sightingList){
                    if(entry.component1().toUpperCase().equals(userEthAccount.toUpperCase())){
                        list.add(new Tuple3<>(addressA, userEthAccount, entry.component2()));
                    }
                }
            }
            MySightingsArrayAdapter adapter = new MySightingsArrayAdapter(this, list);
            listSightings.setAdapter(adapter);
        }


    }
}