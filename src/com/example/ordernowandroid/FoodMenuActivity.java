package com.example.ordernowandroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.data.database.CustomDbAdapter;
import com.data.database.DishHelper;
import com.data.menu.Category;
import com.data.menu.CustomerOrderWrapper;
import com.data.menu.Dish;
import com.data.menu.FoodType;
import com.data.menu.Restaurant;
import com.dm.zbar.android.scanner.ZBarConstants;
import com.example.ordernowandroid.adapter.DownloadResturantMenu;
import com.example.ordernowandroid.adapter.NavDrawerListAdapter;
import com.example.ordernowandroid.fragments.AddNoteDialogFragment;
import com.example.ordernowandroid.fragments.AddNoteListener;
import com.example.ordernowandroid.fragments.IndividualMenuTabFragment;
import com.example.ordernowandroid.fragments.IndividualMenuTabFragment.numListener;
import com.example.ordernowandroid.model.CategoryNavDrawerItem;
import com.example.ordernowandroid.model.FoodMenuItem;
import com.example.ordernowandroid.model.MyOrderItem;
import com.util.Utilities;

public class FoodMenuActivity extends FragmentActivity implements numListener, AddNoteListener{

    public static final String TABLE_ID = "TableId";
    private String tableId;
    private static final int MY_ORDER_REQUEST_CODE = 1;
    //private static final int CONFIRMED_ORDER_REQUEST_CODE = 2;
    protected static final String MY_ORDER = "MyOrder";
	protected static final String FOOD_MENU_CATEGORY_ID = "foodMenuCategoryId";
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle; // nav drawer title
    private CharSequence mTitle; // used to store app title
    private ArrayList<CategoryNavDrawerItem> navDrawerItems;
    private NavDrawerListAdapter adapter;
    private Restaurant restaurant;
    private HashMap<String, MyOrderItem> foodMenuItemQuantityMap = new HashMap<String, MyOrderItem>();
    protected static final String SUB_ORDER_LIST = "SubOrderList";
    public static ArrayList<CustomerOrderWrapper> subOrdersFromDB;
    private static Map<String, Boolean> restaurantLoadedInDb = new HashMap<String, Boolean>();
    
    @SuppressWarnings("unchecked")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        tableId = b.getString(TABLE_ID);
        subOrdersFromDB = (ArrayList<CustomerOrderWrapper>) b.getSerializable(SUB_ORDER_LIST);
        
        setContentView(R.layout.food_menu);
        mTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navDrawerItems = new ArrayList<CategoryNavDrawerItem>();

        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        mDrawerList.setAdapter(adapter);

        // enabling action bar app icon and behaving it as toggle button
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 
                R.drawable.ic_drawer, // nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility 
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if(isSearchIntent(getIntent())) {
            Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);
            if (appData != null) {
                tableId = appData.getString(TABLE_ID);
                ArrayList<MyOrderItem> myOrders = (ArrayList<MyOrderItem>) appData.getSerializable(MY_ORDER);
                if(myOrders!=null){
                    foodMenuItemQuantityMap = new HashMap<String, MyOrderItem>();
                    for (MyOrderItem myOrderItem : myOrders) {
                        foodMenuItemQuantityMap.put(myOrderItem.getFoodMenuItem().getItemName(), myOrderItem);
                    }
                }
            }
        }

        restaurant = getResturant(tableId);
        if (restaurant == null){
            Toast.makeText(this, "null resturant ", Toast
                    .LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(FoodMenuActivity.this);            
            builder.setTitle("Invalid QR code");
            builder.setMessage("Please scan a valid QR code");
            builder.setPositiveButton(R.string.ok, new OnClickListener() {                  
                @Override
                public void onClick(DialogInterface dialog, int which) {                                                
                    //Clear the Selected Quantities and Start the Food Menu Activity again
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);                                              
                }
            });
           // builder.setNegativeButton(R.string.cancel, null);
            AlertDialog alert = builder.create();
            alert.show();
            return ;
        } else {
            mDrawerTitle = restaurant.getName();
            if (savedInstanceState == null) {
                // on first time display view for first menu item
                displayView(0);
            }
            mDrawerList.setOnItemClickListener(new SlideMenuClickListener());
            
            for (Category category : getCategories()) {
                CategoryNavDrawerItem categoryNavDrawerItem = new CategoryNavDrawerItem(category);
                navDrawerItems.add(categoryNavDrawerItem);
            }
        }
        
        boolean search = handleIntent(getIntent());
        if(!search) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
        
    }

    private List<Category> getCategories() {
        return restaurant.getMenu().getCategories();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
             
        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
               (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        SearchView searchView =
                (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        //searchMenuItem.collapseActionView();
        //searchView.setIconifiedByDefault(false);

        RelativeLayout food_cart_layout = (RelativeLayout)menu.findItem(R.id.action_cart).getActionView();
        TextView food_item_notification = (TextView)food_cart_layout.findViewById(R.id.food_cart_notifcation_textview);
        food_item_notification.setText(Integer.toString(foodMenuItemQuantityMap.keySet().size()));
        ImageView cart_image = (ImageView)food_cart_layout.findViewById(R.id.action_cart_image);
        
        RelativeLayout confirmed_order_layout = (RelativeLayout) menu.findItem(R.id.confirmed_order).getActionView();
        ImageView confirmed_order_image = (ImageView) confirmed_order_layout.findViewById(R.id.confirmed_order_image);
        
        final Context context = this;
        final ArrayList<MyOrderItem> orderItems = new ArrayList<MyOrderItem>();
        if (foodMenuItemQuantityMap != null) {
            orderItems.addAll(foodMenuItemQuantityMap.values());
        }
        
        cart_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
	            Intent intent = new Intent(context, MyOrderActivity.class);
	            intent.putExtra(MY_ORDER, orderItems);
	            Utilities.info("mDrawerList " + mDrawerList.getCheckedItemPosition());
	            intent.putExtra(FOOD_MENU_CATEGORY_ID, mDrawerList.getCheckedItemPosition());
	            intent.putExtra(TABLE_ID, tableId);
	            intent.putExtra(SUB_ORDER_LIST, subOrdersFromDB);
	            startActivityForResult(intent, MY_ORDER_REQUEST_CODE);		
			}
		});
        
        confirmed_order_image.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
	            Intent intent = new Intent(context, MyParentOrderActivity.class);
	            intent.putExtra(MY_ORDER, orderItems);
	            intent.putExtra(FOOD_MENU_CATEGORY_ID, mDrawerList.getCheckedItemPosition());
	            intent.putExtra(TABLE_ID, tableId);
	            intent.putExtra(SUB_ORDER_LIST, subOrdersFromDB);
	            startActivity(intent);
			}
		});
        
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        mDrawerLayout.closeDrawer(Gravity.LEFT);
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar actions click
        switch (item.getItemId()) {
        case R.id.action_cart :
            return true;
        case R.id.search:
            onSearchRequested();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);                
        switch (requestCode) {
        case MY_ORDER_REQUEST_CODE:
            if (resultCode == RESULT_OK) {            	    	
                Bundle bundleExtra = data.getExtras();
                @SuppressWarnings("unchecked")
                ArrayList<MyOrderItem> myOrders = (ArrayList<MyOrderItem>) bundleExtra.getSerializable(MyOrderActivity.RETURN_FROM_MY_ORDER);
                if(myOrders!=null){
                	foodMenuItemQuantityMap = new HashMap<String, MyOrderItem>();
                    for (MyOrderItem myOrderItem : myOrders) {
                        foodMenuItemQuantityMap.put(myOrderItem.getFoodMenuItem().getItemName(), myOrderItem);
                    }
                }
                displayView(bundleExtra.getInt(MyOrderActivity.FOOD_MENU_CATEGORY_ID));
            } else if(resultCode == RESULT_CANCELED && data != null) {
                String error = data.getStringExtra(ZBarConstants.ERROR_INFO);
                if(!TextUtils.isEmpty(error)) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
                Intent intent = new Intent(this, FoodMenuActivity.class);
                startActivity(intent);
            }
            break;
       /* case CONFIRMED_ORDER_REQUEST_CODE:
        	if (resultCode == RESULT_OK) {            	    	
                Bundle bundleExtra = data.getExtras();
                @SuppressWarnings("unchecked")
                ArrayList<MyOrderItem> myOrderItems = (ArrayList<MyOrderItem>) bundleExtra.getSerializable(MyParentOrderActivity.MY_ORDER);
                if(myOrderItems!=null){
                	foodMenuItemQuantityMap = new HashMap<String, MyOrderItem>();
                    for (MyOrderItem myOrderItem : myOrderItems) {
                        foodMenuItemQuantityMap.put(myOrderItem.getFoodMenuItem().getItemName(), myOrderItem);
                    }
                }
                displayView(bundleExtra.getInt(MyParentOrderActivity.FOOD_MENU_CATEGORY_ID));
            }
        	break;
*/        }
        invalidateOptionsMenu();
    }

    /***
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.search).collapseActionView();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Slide menu item click listener
     * */
    private class SlideMenuClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // display view for selected nav drawer item
            displayView(position);
        }
    }

    /**
     * Diplaying fragment view for selected nav drawer list item
     * */
    private void displayView(int position) { // update the main content by
                                             // replacing fragments
        Fragment fragment = null;
        Category category = getCategories().get(position);
        fragment = IndividualMenuTabFragment.newInstance(category.getName(), getFoodMenuItems(category.getDishes()));

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).addToBackStack(null).commit();
        } else {
            // error in creating fragment
            Log.e("FoodMenuActivity", "Error in creating fragment");
        }
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerList.setSelection(position);
        setTitle(category.getName());
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    private ArrayList<FoodMenuItem> getFoodMenuItems(List<Dish> dishes) {
        ArrayList<FoodMenuItem> foodMenuItem = new ArrayList<FoodMenuItem>();
        for (Dish dish : dishes) {
            foodMenuItem.add(new FoodMenuItem(dish));
        }
        return foodMenuItem;
    }
    
    @Override
	public float getQuantity(FoodMenuItem foodMenuItem) {
        final String itemName = foodMenuItem.getItemName();
        if (foodMenuItemQuantityMap.get(itemName) != null) {
            return foodMenuItemQuantityMap.get(itemName).getQuantity();
        }
        return 0;
    }

    @Override
    public void incrementQuantity(FoodMenuItem foodMenuItem) {
        final String itemName = foodMenuItem.getItemName();
        
        if (foodMenuItemQuantityMap.get(itemName) == null) {
            MyOrderItem myOrderItem = new MyOrderItem(foodMenuItem, 1);
            foodMenuItemQuantityMap.put(itemName, myOrderItem);
        } else {
            float quantity = foodMenuItemQuantityMap.get(itemName).getQuantity();
            foodMenuItemQuantityMap.get(itemName).setQuantity(++quantity);
        }
        //updateFoodCartNotificationText();
        invalidateOptionsMenu();
    }

    @Override
    public void decrementQuantity(FoodMenuItem foodMenuItem) {
        float quantity = 0;
        final String itemName = foodMenuItem.getItemName();
        if (foodMenuItemQuantityMap.get(itemName) != null) {
            quantity = foodMenuItemQuantityMap.get(itemName).getQuantity();
            quantity--;
            if (quantity == 0) {
                foodMenuItemQuantityMap.remove(itemName);
            } else {
                foodMenuItemQuantityMap.get(itemName).setQuantity(quantity);
            }
        }

        // updateFoodCartNotificationText();
        invalidateOptionsMenu();
    }

    public Restaurant getResturant(String tableId) {
        if(tableId == null || tableId.trim().length() == 0) {
            return null;
        }
        //http://ordernow.herokuapp.com/serveTable?tableId=T1
        Restaurant restaurant = null;
        try {
            restaurant = new DownloadRestaurantTask().execute("http://ordernow.herokuapp.com/serveTable",tableId).get();
            loadRestaurantDishes(restaurant);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        
        return restaurant;
    }

    private void loadRestaurantDishes(final Restaurant restaurant) {
        Utilities.info("restaurant load " + restaurant.getName() + restaurantLoadedInDb);
        if (!restaurantLoadedInDb.containsKey(restaurant.getName())) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        CustomDbAdapter dbManager = CustomDbAdapter
                                .getInstance(getBaseContext());
                        DishHelper dh = new DishHelper(dbManager);
                        for(Category category : restaurant.getMenu().getCategories()) {
                            for(Dish dish: category.getDishes()) {
                                dh.addDish(dish);
                            }
                        }
                        restaurantLoadedInDb.put(restaurant.getName(), true);
                       //for(Res)
                    } catch (Exception e) {
                        Utilities.error("Failed to load into DB " + e);
                    }
                }
            }).start();
        }
    }

    private Restaurant getResturantLocaly() {
        List<Integer> categoryItemName = new LinkedList<Integer>();
        categoryItemName.add(R.array.soups);
        categoryItemName.add(R.array.starters);
        categoryItemName.add(R.array.salads);
        categoryItemName.add(R.array.sizzlers);
        categoryItemName.add(R.array.favourites);

        List<Integer> categoryItemPrice = new LinkedList<Integer>();
        categoryItemPrice.add(R.array.soups_prices);
        categoryItemPrice.add(R.array.starters_prices);
        categoryItemPrice.add(R.array.salads_prices);
        categoryItemPrice.add(R.array.sizzlers_prices);
        categoryItemPrice.add(R.array.favourites_prices);

		List<Integer> categoryItemID = new LinkedList<Integer>();
		categoryItemID.add(R.array.soups_ids);
		categoryItemID.add(R.array.starters_ids);
		categoryItemID.add(R.array.salads_ids);
		categoryItemID.add(R.array.sizzlers_ids);
		categoryItemID.add(R.array.favourites_ids);
		
		List<Integer> imageId = new LinkedList<Integer>();
		imageId.add(R.array.soups_icons);
		imageId.add(R.array.starters_icons);
		imageId.add(R.array.salads_icons);
		imageId.add(R.array.sizzlers_icons);
		imageId.add(R.array.favourites_icons);

        String[] categoryNames = getResources().getStringArray(R.array.nav_drawer_items);
        List<Category> categories = new LinkedList<Category>();
        for (int i = 0; i < categoryNames.length; i++) {
            Category category = new Category();
			getCategory(categoryNames[i], categoryItemName.get(i),
					categoryItemPrice.get(i), categoryItemID.get(i), imageId.get(i), category);
            categories.add(category);
        }

        com.data.menu.Menu menu = new com.data.menu.Menu();
		menu.setCategories(categories);
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Eat 3");
        restaurant.setMenu(menu);
        return restaurant;
    }

	private void getCategory(String categoryName, int itemNameResource,
			int itemPriceResource, int itemDishIds, int itemImage , Category soupCategory) {
        soupCategory.setName(categoryName);
        List<Dish> dishes = new LinkedList<Dish>();
		getDishes(dishes, itemNameResource, itemPriceResource, itemDishIds, itemImage);
        soupCategory.setDishes(dishes);
    }

	private void getDishes(List<Dish> dishes, int itemNameResource, int itemPriceResource,
			int itemDishIds, int itemImage) {
        String[] itemNames = getResources().getStringArray(itemNameResource);
        int[] itemPrices = getResources().getIntArray(itemPriceResource);
		String[] itemids = getResources().getStringArray(itemDishIds);
		String[] itemImages = getResources().getStringArray(itemImage);

        for (int i = 0; itemNames != null && i < itemNames.length; i++) {
            Dish dish = new Dish();
            dish.setName(itemNames[i]);
            dish.setPrice(itemPrices[i]);
            dish.setImg(itemImages[i]);
            if (i % 2 == 0) {
                dish.setType(FoodType.Veg);
            } else {
                dish.setType(FoodType.NonVeg);
            }
            dish.setDescription("item description comes here");
			dish.setDishId(itemids[i]);
            dishes.add(dish);
            
        }
    }
	
    private class DownloadRestaurantTask extends AsyncTask<String, Integer, Restaurant> {
        @Override
        protected Restaurant doInBackground(String... params) {
            // "http://www.creativefreedom.co.uk/icon-designers-blog/wp-content/uploads/2013/03/00-android-4-0_icons.png"
            return DownloadResturantMenu.getInstance().getResturant(params[0], params[1]);
        }

    }


    @Override
    public void showNote(FoodMenuItem foodMenuItem) {
        AddNoteDialogFragment noteFragment = AddNoteDialogFragment.newInstance(foodMenuItem,
                foodMenuItemQuantityMap.get(foodMenuItem.getItemName()));
        noteFragment.show(getSupportFragmentManager(), "notes");
        invalidateOptionsMenu();

    }

    @Override
    public void saveNote(FoodMenuItem foodMenuItem, HashMap<String, String> metaData) {
        foodMenuItemQuantityMap.get(foodMenuItem.getItemName()).setNotes("");
        foodMenuItemQuantityMap.get(foodMenuItem.getItemName()).setMetaData(metaData);
        invalidateOptionsMenu();

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Utilities.info("on new intent");
        handleIntent(intent);
    }
    
    private boolean isSearchIntent(Intent intent) {
        return Intent.ACTION_SEARCH.equals(intent.getAction());
    }
    private boolean handleIntent(Intent intent) {
        
        if (isSearchIntent(intent)) {
            Utilities.info("search intent");
            CustomDbAdapter dbManager = CustomDbAdapter
                    .getInstance(getBaseContext());
            DishHelper dh = new DishHelper(dbManager);   
            String query = intent.getStringExtra(SearchManager.QUERY);
            if(dh!=null) {
                ArrayList<FoodMenuItem> searchDishList = (ArrayList<FoodMenuItem>) dh.searchDishes(query);
                //IndividualMenuTabFragment.newInstance("Search", searchDishList);
                Fragment fragment = IndividualMenuTabFragment.newInstance("Search", searchDishList);
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).addToBackStack(null).commit();
                return true;
            }
            
        }
        return false;
        
    }
    
    @Override
    public boolean onSearchRequested() {
         Bundle appData = new Bundle();
         appData.putString(TABLE_ID, tableId);
         ArrayList<MyOrderItem> orderItems = new ArrayList<MyOrderItem>();
         if (foodMenuItemQuantityMap != null) {
             orderItems.addAll(foodMenuItemQuantityMap.values());
         }
         appData.putSerializable(MY_ORDER, orderItems);
         startSearch(null, false, appData, false);
         return true;
     }

}
