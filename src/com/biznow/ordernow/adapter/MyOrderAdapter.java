package com.biznow.ordernow.adapter;

import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.biznow.ordernow.ApplicationState;
import com.biznow.ordernow.R;
import com.biznow.ordernow.model.MyOrderItem;
import com.biznow.ordernow.model.OrderNowConstants;

public class MyOrderAdapter extends ArrayAdapter<MyOrderItem> {

	private ApplicationState applicationState;
	private static final String TEXT_COMMENT = "TextComment";

	List<MyOrderItem> myOrderItemList ; 
	public MyOrderAdapter(Context context, List<MyOrderItem> myOrderItemList) {
		super(context, R.layout.my_order, myOrderItemList);
		applicationState = (ApplicationState) context.getApplicationContext();
		this.myOrderItemList = myOrderItemList;
	}

	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.my_order, null);
		}

		TextView itemName = (TextView) convertView.findViewById(R.id.itemName);
		TextView itemNote = (TextView) convertView.findViewById(R.id.itemNote);
		final TextView quantity = (TextView) convertView.findViewById(R.id.quantity);
		final TextView itemTotalPrice = (TextView) convertView.findViewById(R.id.itemTotalPrice);

		String orderItemName = myOrderItemList.get(position).getFoodMenuItem().getItemName();
		String orderItemNote = null;
		if (myOrderItemList.get(position).getMetaData() != null) {
			orderItemNote = myOrderItemList.get(position).getMetaData().get(TEXT_COMMENT);
		}
		Float orderItemQuantity = myOrderItemList.get(position).getQuantity();
		Float orderItemPrice = myOrderItemList.get(position).getFoodMenuItem().getItemPrice();		

		itemName.setText(orderItemName);
		if(orderItemNote != null) {
			itemNote.setText(orderItemNote);
		} else {
			itemNote.setVisibility(View.GONE);
			itemName.setGravity(Gravity.CENTER_VERTICAL);	
		}
		quantity.setText(Float.toString(orderItemQuantity));
		itemTotalPrice.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString(orderItemPrice * orderItemQuantity));

		final ImageButton decrementQtyBtn = (ImageButton) convertView.findViewById(R.id.decrementQtyButton);
		final ImageButton incrementQtyBtn = (ImageButton) convertView.findViewById(R.id.incrementQtyButton);

		decrementQtyBtn.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View v) {
				HashMap<String, MyOrderItem> foodMenuItemQuantityMap = ApplicationState.getFoodMenuItemQuantityMap(applicationState);
				Float qty = Float.parseFloat((String) quantity.getText());
				String orderItemPriceStr = (String) itemTotalPrice.getText();				

				if (orderItemPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) != -1){
					orderItemPriceStr = orderItemPriceStr.substring(orderItemPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) + 1).trim();
				}
				final Float orderItemPrice = Float.parseFloat(orderItemPriceStr);

				RelativeLayout r = (RelativeLayout) ((ViewGroup) parent).getParent(); // This is to get the Parent View of the List View
				final TextSwitcher orderTotalPriceView = (TextSwitcher) r.findViewById(R.id.relativeBtnLayout).findViewById(R.id.totalAmount);

				if (qty > 1) {
					itemTotalPrice.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString((orderItemPrice/qty) * (qty - 1)));
					quantity.setText(Float.toString(qty - 1));					

					String orderTotalPriceStr = (String) ((TextView)orderTotalPriceView.getCurrentView()).getText();
					if (orderTotalPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) != -1){
						orderTotalPriceStr = orderTotalPriceStr.substring(orderTotalPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) + 1).trim();
					}
					Float newOrderTotalPrice = Float.parseFloat(orderTotalPriceStr) - (orderItemPrice/qty);					
					orderTotalPriceView.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString(newOrderTotalPrice));					
					myOrderItemList.get(position).setQuantity(qty - 1);
					foodMenuItemQuantityMap.get(myOrderItemList.get(position).getFoodMenuItem().getItemName()).setQuantity(qty-1);
				} else if (qty == 1){
					//Show Dialog and Remove Item from ListView on Positive Button Action
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());            
					builder.setTitle("Remove Item");
					builder.setMessage("Are you sure you want to remove this item from the order?");
					builder.setPositiveButton(R.string.ok, new AlertDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							HashMap<String, MyOrderItem> foodMenuItemQuantityMap = ApplicationState.getFoodMenuItemQuantityMap(applicationState);
							foodMenuItemQuantityMap.remove(getItem(position).getFoodMenuItem().getItemName());
							remove(getItem(position));
							notifyDataSetChanged();
							String orderTotalPriceStr = (String) ((TextView)orderTotalPriceView.getCurrentView()).getText();
							if (orderTotalPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) != -1){
								orderTotalPriceStr = orderTotalPriceStr.substring(orderTotalPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) + 1).trim();
							}
							Float newOrderTotalPrice = Float.parseFloat(orderTotalPriceStr) - (orderItemPrice);					
							orderTotalPriceView.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString(newOrderTotalPrice));
						}
					});

					builder.setNegativeButton(R.string.cancel, null);	            	            
					AlertDialog alert = builder.create();
					alert.show();					
				} else {
					Toast.makeText(getContext(), "Quantity cannnot be decreased below zero", Toast.LENGTH_SHORT).show();
				}

			}
		});

		incrementQtyBtn.setOnClickListener(new OnClickListener() { 
			@Override
			public void onClick(View v) {				
				Float qty = Float.parseFloat((String) quantity.getText());
				String orderItemPriceStr = (String) itemTotalPrice.getText();				

				if (orderItemPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) != -1){
					orderItemPriceStr = orderItemPriceStr.substring(orderItemPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) + 1).trim();
				}
				Float orderItemPrice = Float.parseFloat(orderItemPriceStr);

				RelativeLayout r = (RelativeLayout) ((ViewGroup) parent).getParent(); // This is to get the Parent View of the List View
				final TextSwitcher orderTotalPriceView = (TextSwitcher) r.findViewById(R.id.relativeBtnLayout).findViewById(R.id.totalAmount);

				if (qty == 0){
					Toast.makeText(getContext(), "Cannot determine Unit Price if the Quantity is zero", Toast.LENGTH_SHORT).show();					
				} else {
					itemTotalPrice.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString((orderItemPrice/qty) * (qty + 1)));
					quantity.setText(Float.toString(qty + 1));					

					String orderTotalPriceStr = (String) ((TextView)orderTotalPriceView.getCurrentView()).getText();
					if (orderTotalPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) != -1){
						orderTotalPriceStr = orderTotalPriceStr.substring(orderTotalPriceStr.indexOf(OrderNowConstants.INDIAN_RUPEE_UNICODE) + 1).trim();
					}
					Float newOrderTotalPrice = Float.parseFloat(orderTotalPriceStr) + (orderItemPrice/qty);					
					orderTotalPriceView.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString(newOrderTotalPrice));
					myOrderItemList.get(position).setQuantity(qty + 1);
				}
			}
		});

		return convertView;
	}
}
