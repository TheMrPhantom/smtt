package com.example.smtt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;

public class StartSearchTaskActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_search_task);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String contractAddress = sharedPreferences.getString(getString(R.string.contract_address), "");
        String nodeUrl = sharedPreferences.getString(getString(R.string.node_url), "");
        String userEthAccount = sharedPreferences.getString(getString(R.string.user_eth_address), "");
        String userEthPrivateKey = sharedPreferences.getString(getString(R.string.user_eth_private_key), "");
        String userPublicKey = sharedPreferences.getString(getString(R.string.user_publicKey), "");
        String userPrivateKey = sharedPreferences.getString(getString(R.string.user_privateKey), "");
        Button btnAbort = (Button) findViewById(R.id.btnAbortSearchTaskCreate);
        Button btnStartSearchTask = (Button) findViewById(R.id.btnStartSearchTask);
        TextView txtTitle = (TextView) findViewById(R.id.txtStartSearchTitle);
        EditText inputReward = (EditText) findViewById(R.id.inputReward);
        EditText inputTrustFactor = (EditText) findViewById(R.id.inputTrustFactor);
        EditText inputMinNonce = (EditText) findViewById(R.id.edtMinNonce);

        inputMinNonce.setText("1");
        inputTrustFactor.setText("0");
        inputReward.setText("0.1");

        BlockchainConnection blockchainConnection = BlockchainConnection.getInstance(nodeUrl, userPrivateKey);
        HashMap<String, BigInteger[]> searchTasks = blockchainConnection.getOngoingSearchOrders();
        String txt = "";
        if(searchTasks.containsKey(userEthAccount.toUpperCase())){
            txt = "You already have an active Seach Task, you can only have one Search Task at once!\n";
        }
        txt += "Pleas submit the Reward, Trust Factor and the smallest acceptable Nonce";
        txtTitle.setText(txt);

        btnStartSearchTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    BigInteger reward = Convert.toWei(inputReward.getText().toString(), Convert.Unit.ETHER).toBigInteger();
                    BigInteger trustFactor = new BigInteger(inputTrustFactor.getText().toString());
                    BigInteger minNonce = new BigInteger(inputMinNonce.getText().toString());
                    BigInteger userBalance = blockchainConnection.getUserBalance(userEthAccount);

                    if(userBalance.compareTo(reward) == -1){
                        Toast toast = Toast.makeText(view.getContext(), "You dont have enought ETH!", Toast.LENGTH_SHORT);
                        toast.show();
                    } else{
                        blockchainConnection.startSerchTask(trustFactor, minNonce, reward);
                        Toast toast = Toast.makeText(view.getContext(), "Search Task Submitted!", Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                    }
                } catch (Exception e){
                    Toast toast = Toast.makeText(view.getContext(), "Error while talking to the Blockchain!", Toast.LENGTH_SHORT);
                    toast.show();
                    e.printStackTrace();
                }
            }
        });




        btnAbort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}