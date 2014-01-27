package com.example.ordernowandroid.model;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.data.menu.Dish;
import com.data.menu.FoodType;
import com.example.ordernowandroid.adapter.ImageService;

/**
 * 
 * @author Rohit
 * 
 */

public class FoodMenuItem implements Serializable {

    private static final long serialVersionUID = 1L;
    private Dish dish;

    public FoodMenuItem(Dish dish) {
        this.dish = dish;
        populateImageCache(); // To populate image cache.
    }

    public String getItemName() {
        return dish.getName();
    }

    public Float getItemPrice() {
        return dish.getPrice();
    }

    public FoodType getFoodType() {
        return dish.getType();
    }
    
    public String getDescription() {
    	return dish.getDescription();
    }

    @Override
    public String toString() {
        return dish.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof FoodMenuItem) {
            FoodMenuItem fmi = (FoodMenuItem) o;
            return this.dish.equals(fmi.getDish());
        }
        return false;
    }

    private Dish getDish() {
        return this.dish;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

	public String getDishId() {
		return dish.getDishId();
	}
	
    public Bitmap getImage() {

        String image = dish.getImg();
        if (image == null || image.equals("")) {
            return null;
        }

        Bitmap bitmap = null;
        
        try {
            bitmap = new DownloadImageTask().execute(image).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public void populateImageCache() {

        String image = dish.getImg();
        if (image == null || image.equals("")) {
            return;
        }
        //not keeping the asynctask else it wont be serializable
        //also the underlying class ImageService is singleton so no problem
        new DownloadImageTask().execute(image);

    }
	
	private class DownloadImageTask extends AsyncTask<String, Integer, Bitmap> {
	    @Override
	    protected Bitmap doInBackground(String... params) {
	        //"http://www.creativefreedom.co.uk/icon-designers-blog/wp-content/uploads/2013/03/00-android-4-0_icons.png"
	        return ImageService.getInstance().getImageWithCache(params[0]);
	    }
	    
	}

}


