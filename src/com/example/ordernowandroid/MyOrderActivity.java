package com.example.ordernowandroid;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ordernowandroid.adapter.MyOrderAdapter;
import com.example.ordernowandroid.model.MyOrderItem;

public class MyOrderActivity extends Activity {
    public static final String RETURN_FROM_MY_ORDER = "ReturnFromMyOrder";
    private ArrayList<MyOrderItem> myOrders;
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("MyOrder");
        Bundle b = getIntent().getExtras();
        myOrders = (ArrayList<MyOrderItem>) b.getSerializable(FoodMenuActivity.MY_ORDER);
        setContentView(R.layout.my_order_summary);
        Button addMoreItemsBtn = (Button) findViewById(R.id.addMoreItemsButton);
        Button cancelOrderBtn = (Button) findViewById(R.id.cancelOrderButton);
        Button confirmOrderBtn = (Button) findViewById(R.id.confirmOrderButton);
        
        addMoreItemsBtn.setOnClickListener(new Button.OnClickListener() {           
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        
        cancelOrderBtn.setOnClickListener(new Button.OnClickListener() {            
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MyOrderActivity.this);            
                builder.setTitle("Cancel Order");
                builder.setMessage("Are you sure you want to cancel the order ?");
                builder.setPositiveButton(R.string.ok, new OnClickListener() {                  
                    @Override
                    public void onClick(DialogInterface dialog, int which) {                                                
                        Toast.makeText(getApplicationContext(), "Order has been canceled.", Toast.LENGTH_LONG).show();
                        //Clear the Selected Quantities and Start the Food Menu Activity again
                        Intent intent = new Intent(getApplicationContext(), FoodMenuActivity.class);
                        startActivity(intent);                                              
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);                           
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
        
        confirmOrderBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MyOrderActivity.this);            
                builder.setTitle("Confirm Order");
                builder.setMessage("Are you sure you want to confirm the order ?");
                builder.setPositiveButton(R.string.ok, new OnClickListener() {                  
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //TODO: Back End Integration, Make an API Call to let the Server know about Order Confirmation
                        Toast.makeText(getApplicationContext(), "Order has been confirmed.", Toast.LENGTH_LONG).show();
                        //TODO: Check how should the flow be
                        /*Intent intent = new Intent(getActivity(), FoodMenuActivity.class);
                        startActivity(intent);*/
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);                               
                AlertDialog alert = builder.create();
                alert.show();
            }
        }); 
                
        TextView totalAmount = (TextView) findViewById(R.id.totalAmount);
        Float totalOrderAmount = (float) 0.00;
        for (MyOrderItem myOrderItem: myOrders) {
            totalOrderAmount = totalOrderAmount + (myOrderItem.getQuantity() * myOrderItem.getFoodMenuItem().getItemPrice()); 
        }
        
        totalAmount.setText("\u20B9" + " " + Float.toString(totalOrderAmount)); //UniCode for Rupee Symbol. HardCoding it here for now, cannot get R.string.id to make it work
                
        ListView myOrderListView = (ListView) findViewById(R.id.listMyOrder);
        
        
        MyOrderAdapter myOrderAdapter = new MyOrderAdapter(MyOrderActivity.this, myOrders);
        myOrderListView.setAdapter(myOrderAdapter);
    }
    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra(RETURN_FROM_MY_ORDER, myOrders);
        // Activity finished ok, return the data
        setResult(RESULT_OK, data);
        super.finish();
    }
}