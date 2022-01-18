package com.example.smtt;

import android.app.Activity;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.web3j.tuples.generated.Tuple2;

import java.math.BigInteger;
import java.util.List;

public class SightingArrayAdapter extends ArrayAdapter<Tuple2<String, BigInteger>> {

    private final Activity context;
    private final List<Tuple2<String, BigInteger>> data;

    public SightingArrayAdapter(Activity context, List<Tuple2<String, BigInteger>> data) {
        super(context, R.layout.sighting_list_item, data);
        System.out.println(data.toString());
        this.context=context;
        this.data=data;

    }

    @Override
    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.sighting_list_item, null,true);

        TextView txtAddress = (TextView) rowView.findViewById(R.id.txtAddressW);
        TextView txtTimestamp = (TextView) rowView.findViewById(R.id.txtTimestamp);
        Button btnView = (Button) rowView.findViewById(R.id.btnView);
        Tuple2<String, BigInteger> item = data.get(position);
        System.out.println(42);

        txtAddress.setText(item.component1());
        txtTimestamp.setText(item.component2().toString());
        btnView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(v.getContext(), RevealSecretActivity.class);
                intent.putExtra("addressW", item.component1());
                intent.putExtra("timestamp", item.component2());
                intent.putExtra("index", position);
                v.getContext().startActivity(intent);
            }
        });


        return rowView;

    };
}