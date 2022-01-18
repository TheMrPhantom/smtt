package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.web3j.tuples.generated.Tuple2;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class MySearchTasksActivity extends AppCompatActivity {
    TextView txtTitle;
    TextView txtReportedSightings;
    ListView listSightings;
    LinkedList<Tuple2<String, BigInteger>> list;
    SightingArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_search_tasks);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userEthAddress = sharedPreferences.getString(getString(R.string.user_eth_address), "lala");
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");

        txtTitle = (TextView) findViewById(R.id.txtTitle);
        txtReportedSightings = (TextView) findViewById(R.id.txtReportetSightings);
        listSightings = (ListView) findViewById(R.id.listSightings);
        list = new LinkedList<Tuple2<String, BigInteger>>();
        BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(userEthPrivateKey, nodeUrl);
        BigInteger[] searchTask = blockchainConnection.getOngoingSearchOrders().get(userEthAddress.toUpperCase());

        txtTitle.setText(String.format("Search Task:\nReward: %s ETH,\n Trust Factor: %s", Convert.fromWei(new BigDecimal(searchTask[1]), Convert.Unit.ETHER), searchTask[0]));
        HashMap<String,LinkedList<Tuple2<String, BigInteger>>> reportedSightings = blockchainConnection.getReportedSightings();
        if(reportedSightings.containsKey(userEthAddress.toUpperCase())) {
            list = reportedSightings.get(userEthAddress.toUpperCase());
            System.out.println(list.toString());
        }
        adapter = new SightingArrayAdapter(this, list);
        listSightings.setAdapter(adapter);
        listSightings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(adapter.getItem(position));
            }
        });

    }
}