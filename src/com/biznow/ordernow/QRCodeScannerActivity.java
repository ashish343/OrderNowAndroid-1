package com.biznow.ordernow;

import java.util.ArrayList;

import net.sourceforge.zbar.Symbol;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.biznow.ordernow.R;
import com.biznow.ordernow.model.OrderNowConstants;
import com.data.menu.CustomerOrderWrapper;
import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.facebook.Session;
import com.facebook.widget.ProfilePictureView;
import com.util.OrderNowUtilities;
import com.util.Utilities;

public class QRCodeScannerActivity extends Activity {

	private static final String SAMPLE_REST_ID = "R3";
    private static final String SAMPLE_TABLE_ID = "T20";
    private static final int ZBAR_QR_SCANNER_REQUEST = 1;
	private ProfilePictureView profilePictureView;
	private TextView welcome;
	private TextView custName;
	private Button qrCodeButton;
	private Button openRestMenuButton;

	private String activeTableId;
	private String activeRestId;
	private String activeRestName;
	private ArrayList<CustomerOrderWrapper> activeSubOrderList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qr_code_scanner);

		welcome = (TextView) findViewById(R.id.welcome_text);
		custName = (TextView) findViewById(R.id.selection_profile_name);
		qrCodeButton = (Button) findViewById(R.id.qrscan_btn);
		openRestMenuButton = (Button) findViewById(R.id.open_res_menu);

		ApplicationState applicationContext = (ApplicationState) getApplicationContext();
		if(applicationContext.getUserName() != null && applicationContext.getUserName().trim() != "") {
			custName.setText(applicationContext.getUserName());
		}

		if(applicationContext.getProfilePictureId() != null) {
			profilePictureView = (ProfilePictureView) findViewById(R.id.selection_profile_pic);
			profilePictureView.setProfileId(applicationContext.getProfilePictureId());
		}

		checkForActiveSessionAndUpdateUI();	
	}

	private void checkForActiveSessionAndUpdateUI() {
		if(activeSessionPresent()) {
			welcome.setText("You are currently logged in " + activeRestName + " restaurant.");
			qrCodeButton.setVisibility(View.GONE);
			openRestMenuButton.setVisibility(View.VISIBLE);
		} else {
			welcome.setText(welcome.getText());
			qrCodeButton.setVisibility(View.VISIBLE);
			if (OrderNowConstants.IS_PRODUCTION_SAMPLE_MODE) {
				openRestMenuButton.setVisibility(View.VISIBLE);            
				openRestMenuButton.setText("Open Sample Menu");
            } else {
            	openRestMenuButton.setVisibility(View.GONE);
            }
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.qr_page_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.allhistorybutton:
			Intent historyintent = new Intent(getApplicationContext(), AllCustomerHistoryActivity.class);
			startActivity(historyintent);
			return true;
		case R.id.signOut :
			Session session = Session.getActiveSession();
			if (session != null && session.isOpened()) {
				session.closeAndClearTokenInformation();
			}
			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			startActivity(intent);
			finish();
			Toast.makeText(this, "Signed Out Successfully", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.sendAppFeedback :
			Intent Email = new Intent(Intent.ACTION_SEND);
	        Email.setType("text/email");
	        Email.putExtra(Intent.EXTRA_EMAIL, new String[] { "ordernowinfo@gmail.com" });
	        Email.putExtra(Intent.EXTRA_SUBJECT, "Feedback For OrderNow Android");
	        Email.putExtra(Intent.EXTRA_TEXT, "Hi ,\n" + "");
	        startActivity(Intent.createChooser(Email, "Choose an Email Client "));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	private boolean activeSessionPresent() {
	    if(OrderNowConstants.IS_PRODUCTION_SAMPLE_MODE) {
	        return false;
	    }
		activeTableId = OrderNowUtilities.getKeyFromSharedPreferences(getApplicationContext(), OrderNowConstants.KEY_ACTIVE_TABLE_ID);
		if(activeTableId != null && activeTableId.trim() != "") {
			activeRestId = OrderNowUtilities.getKeyFromSharedPreferences(getApplicationContext(), OrderNowConstants.KEY_ACTIVE_RESTAURANT_ID);
			activeRestName = OrderNowUtilities.getKeyFromSharedPreferences(getApplicationContext(), OrderNowConstants.KEY_ACTIVE_RESTAURANT_NAME);
			activeSubOrderList = OrderNowUtilities.getObjectFromSharedPreferences(getApplicationContext(), OrderNowConstants.KEY_ACTIVE_SUB_ORDER_LIST);
			return true;
		}
		return false;
		
	}

	public void launchQRScanner(View v) {
		if (isCameraAvailable()) {
			Intent intent = new Intent(this, ZBarScannerActivity.class);
			intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE });
			startActivityForResult(intent, ZBAR_QR_SCANNER_REQUEST);
		} else {
			Toast.makeText(this, "Rear Facing Camera Unavailable", Toast.LENGTH_SHORT).show();
		}
	}

	public void openRestaurantMenu(View v) {
		if (OrderNowConstants.IS_PRODUCTION_SAMPLE_MODE) {
			loadRestaurantTable((ApplicationState) getApplicationContext(), SAMPLE_TABLE_ID, SAMPLE_REST_ID);
		} else if(activeTableId != null && activeTableId.trim() != "") {
			ApplicationState applicationContext = (ApplicationState) getApplicationContext();
			ApplicationState.setOpenCategoryDrawer(applicationContext, true);
			Toast.makeText(this, "Session Table Id = " + activeTableId + " Rest Id = " + activeRestId, Toast.LENGTH_SHORT).show();

			Intent intent = new Intent(this, FoodMenuActivity.class);
			startActivity(intent);				
		} 
	}

	public boolean isCameraAvailable() {
		PackageManager pm = getPackageManager();
		return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);    
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ApplicationState applicationContext = (ApplicationState) getApplicationContext();
		switch (requestCode) {
		case ZBAR_QR_SCANNER_REQUEST:
			if (resultCode == RESULT_OK) {
				if (OrderNowConstants.IS_PRODUCTION_SAMPLE_MODE) {
					loadRestaurantTable((ApplicationState) getApplicationContext(), SAMPLE_TABLE_ID, SAMPLE_REST_ID);
				} else {
					String tableId_restID = data.getStringExtra(ZBarConstants.SCAN_RESULT);
					String[] scanResultStrings = tableId_restID.split(" ");
					String tableId = scanResultStrings[0];
					String restId = (scanResultStrings.length > 1)? scanResultStrings[1] : "";
					//Toast.makeText(this, "Table Id = " + tableId + " Rest Id = " + restId, Toast.LENGTH_SHORT).show();
					loadRestaurantTable(applicationContext, tableId, restId);
				}

			} else if(resultCode == RESULT_CANCELED && data != null) {
				String error = data.getStringExtra(ZBarConstants.ERROR_INFO);
				Utilities.error("Error Message: " + error);
				Toast.makeText(this, "To be implemented", Toast.LENGTH_SHORT).show();
			}
			break;
		}
	}

    private void loadRestaurantTable(ApplicationState applicationContext, String tableId, String restId) {

        //clean order stuff if present
        ApplicationState.cleanFoodMenuItemQuantityMap(applicationContext);
        //clean any previous session if present and start fresh
        OrderNowUtilities.sessionClean(applicationContext);

        OrderNowUtilities.putKeyToSharedPreferences(getApplicationContext(), OrderNowConstants.KEY_ACTIVE_TABLE_ID, tableId);
        OrderNowUtilities.putKeyToSharedPreferences(getApplicationContext(), OrderNowConstants.KEY_ACTIVE_RESTAURANT_ID, restId);
        ApplicationState.setOpenCategoryDrawer(applicationContext, true);

        Intent intent = new Intent(this, FoodMenuActivity.class);
        startActivity(intent);
    }

	@Override
	protected void onResume() {
		checkForActiveSessionAndUpdateUI();	
		super.onResume();
	}
}
