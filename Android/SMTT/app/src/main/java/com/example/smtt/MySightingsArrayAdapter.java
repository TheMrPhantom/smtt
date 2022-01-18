package com.example.smtt;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.web3j.tuples.generated.Tuple3;

import java.math.BigInteger;
import java.util.List;

public class MySightingsArrayAdapter extends ArrayAdapter<Tuple3<String, String, BigInteger>> {

    private final Activity context;
    private final List<Tuple3<String, String, BigInteger>> data;

    public MySightingsArrayAdapter(Activity context, List<Tuple3<String, String, BigInteger>> data) {
        super(context, R.layout.sighting_list_item, data);
        System.out.println(data.toString());
        this.context=context;
        this.data=data;

    }

    @Override
    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.my_sightings_list_item, null,true);

        TextView txtAddressA = (TextView) rowView.findViewById(R.id.txtAddressA);
        TextView txtAddressW = (TextView) rowView.findViewById(R.id.txtAddressW);
        TextView txtTimestamp = (TextView) rowView.findViewById(R.id.txtTimestamp);
        Button btnView = (Button) rowView.findViewById(R.id.btnView);
        Tuple3<String, String, BigInteger> item = data.get(position);
        System.out.println(42);

        txtAddressA.setText(item.component1());
        txtAddressW.setText(item.component2());
        txtTimestamp.setText(item.component3().toString());
        btnView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(v.getContext(), SightingInfoActivity.class);
                intent.putExtra("addressA", item.component1());
                intent.putExtra("addressW", item.component2());
                intent.putExtra("timestamp", item.component3());
                intent.putExtra("index", position);
                v.getContext().startActivity(intent);
            }
        });


        return rowView;

    };
}