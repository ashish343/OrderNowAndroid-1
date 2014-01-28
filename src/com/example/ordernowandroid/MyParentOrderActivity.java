package com.example.ordernowandroid;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.data.menu.CustomerOrderWrapper;
import com.example.ordernowandroid.adapter.MyParentOrderAdapter;
import com.example.ordernowandroid.model.MyOrderItem;
import com.example.ordernowandroid.model.OrderNowConstants;

public class MyParentOrderActivity extends Activity {

	protected static final String CUSTOMER_ORDER_WRAPPER = "customerOrderList";
	protected static final String FOOD_MENU_CATEGORY_ID = "foodMenuCategoryId";
	protected static final String TABLE_ID = "TableId";
	protected static final String MY_ORDER = "MyOrder";
	protected static final String SUB_ORDER_LIST = "SubOrderList";

	private int categoryId;
	private String tableId;
	private ArrayList<MyOrderItem> myOrderItems;

	public static CustomerOrderWrapper customerOrderWrapper;
	private ArrayList<MyOrderItem> myOrderItemList = new ArrayList<MyOrderItem>();
	public static ArrayList<CustomerOrderWrapper> subOrdersFromDB;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.my_parent_order_summary);
		setTitle("Confirmed Order");

		Bundle b = getIntent().getExtras();
		customerOrderWrapper = (CustomerOrderWrapper) b.getSerializable(CUSTOMER_ORDER_WRAPPER);
		myOrderItems = (ArrayList<MyOrderItem>) b.getSerializable(MY_ORDER);	
		tableId = b.getString(TABLE_ID);
		categoryId = b.getInt(FOOD_MENU_CATEGORY_ID);
		
		/**
		 * Fetch Sub Orders from Database //FIXME
		 * Add Current Order to SubOrder List
		 * Calculate Order Total and Display List of Sub-Orders 
		 */

		if (subOrdersFromDB == null) {
			subOrdersFromDB = new ArrayList<CustomerOrderWrapper>();
		} else {
			subOrdersFromDB = new ArrayList<CustomerOrderWrapper>();
			subOrdersFromDB.addAll((ArrayList<CustomerOrderWrapper>) b.getSerializable(SUB_ORDER_LIST));
		}

		TextView totalAmount = (TextView) findViewById(R.id.parentTotalAmount);
		Float totalOrderAmount = (float) 0.00;
		
		if(customerOrderWrapper !=null) {
			subOrdersFromDB.add(customerOrderWrapper);
		}

		for (CustomerOrderWrapper subOrder: subOrdersFromDB){
			totalOrderAmount = totalOrderAmount + subOrder.getOrderTotal();
			for (MyOrderItem myOrderItem: subOrder.getMyOrderItemList()) {
				myOrderItemList.add(myOrderItem);
			}
		}		

		totalAmount.setText(OrderNowConstants.INDIAN_RUPEE_UNICODE + " " + Float.toString(totalOrderAmount));

		ListView myOrderListView = (ListView) findViewById(R.id.parentListMyOrder);
		MyParentOrderAdapter myParentOrderAdapter = new MyParentOrderAdapter(this, myOrderItemList);
		myOrderListView.setAdapter(myParentOrderAdapter);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(getApplicationContext(), FoodMenuActivity.class);
			intent.putExtra(TABLE_ID, tableId);
			intent.putExtra(FOOD_MENU_CATEGORY_ID, categoryId);
			intent.putExtra(MY_ORDER, myOrderItems);
			intent.putExtra(SUB_ORDER_LIST, subOrdersFromDB);
			startActivity(intent);

			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
