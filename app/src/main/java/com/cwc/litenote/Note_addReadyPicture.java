package com.cwc.litenote;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

/*
 * Note: 
 * 	mSelectedPictureUri: used to show in confirmation Continue dialog
 *  	Two conditions:
 *  	1. is got after selecting picture
 *  	2. is kept during rotation
 * 
 * UtilImage.bShowExpandedImage: used to control DB saving state
 * 
 * Note_common: used to do DB operation
 */
public class Note_addReadyPicture extends Activity {

    static Long mRowId;
    static String mSelectedPictureUri;
    Note_common note_common;
    static boolean mEnSaveDb;
	boolean bUseSelectedPicture;
	static int TAKE_PICTURE_ACT = 1;  
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("Note_addOkPicture / onCreate");
		
        note_common = new Note_common(this);
        mSelectedPictureUri = "";
        bUseSelectedPicture = false;
        mEnSaveDb = true;
	
        // get row Id from saved instance
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB.KEY_NOTE_ID);
        
        // at the first beginning
        if(savedInstanceState == null)
        	addPicture();
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	if(savedInstanceState.getBoolean("UseSelectedPicture"))
    		bUseSelectedPicture = true;
    	else
    		bUseSelectedPicture = false;
    	
    	mSelectedPictureUri = savedInstanceState.getString("showSelectedPictureUri");
    	
    	if(savedInstanceState.getBoolean("ShowConfirmContinueDialog"))
    	{
    		showContinueConfirmationDialog();
    		System.out.println("showContinueDialog again");
    	}
    	
    }

    // for Rotate screen
    @Override
    protected void onPause() {
    	System.out.println("Note_addOkPicture / onPause");
        super.onPause();
    }

    // for Add Ok picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addOkPicture / onSaveInstanceState");
   	 	
        if(bUseSelectedPicture)
        {
        	outState.putBoolean("UseSelectedPicture",true);
        	outState.putString("showSelectedPictureUri", mSelectedPictureUri);
        }
        else
        {
        	outState.putBoolean("UseSelectedPicture",false);
        	outState.putString("showSelectedPictureUri", "");
        }
        
        // if confirmation dialog still shows?
        if(UtilImage.bShowExpandedImage == true)
        {
        	outState.putBoolean("ShowConfirmContinueDialog",true);
        }
        else
        	outState.putBoolean("ShowConfirmContinueDialog",false);
        
        outState.putSerializable(DB.KEY_NOTE_ID, mRowId);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
    	if(UtilImage.bShowExpandedImage == true)
    	{
	    	UtilImage.closeExpandedImage();
	        mEnSaveDb = false;
	        finish();
	    }		
    }
    
    void addPicture()
    {
		Intent intentAdd = new Intent(Intent.ACTION_GET_CONTENT);
		intentAdd.setType("image/*");
		this.startActivityForResult(Intent.createChooser(intentAdd, 
									   "Select image"),
									   Util.PICTURE_CHOOSER);	    
	    mEnSaveDb = true;
	    
	    if(UtilImage.mExpandedImageView != null)
	    	UtilImage.closeExpandedImage();
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		System.out.println("Note_addOkPicture / onActivityResult");
		if (resultCode == Activity.RESULT_OK)
		{
			
			// for ready picture
			if(requestCode == Util.PICTURE_CHOOSER)
			{
				Uri selectedUri = imageReturnedIntent.getData(); 
				String uriStr = selectedUri.toString();
				//??? why DB static value is changed
	        	mRowId = Note_common.savePictureStateInDB(mRowId,mEnSaveDb,uriStr, "", ""); 
	        	mSelectedPictureUri = uriStr;
            
	        	SharedPreferences pref_takePicture;
	    		pref_takePicture = getSharedPreferences("takePicutre", 0);
	    		
    			if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
    				(Note_common.getCount() > 0) )
		               NoteFragment.swap();
    			
    			Toast.makeText(this, R.string.toast_saved , Toast.LENGTH_SHORT).show();
    			
	    		// show confirm Continue dialog
	        	if(pref_takePicture.getString("KEY_SHOW_CONFIRMATION_DIALOG","yes").equalsIgnoreCase("yes"))
	        	{
	    			bUseSelectedPicture = true; 
		            // set Continue Taking Picture dialog
	        		showContinueConfirmationDialog();
	        	}
	        	else
	        	// not show confirm Continue dialog
	        	{
	    			bUseSelectedPicture = false; 
	        		
	    			Intent intentAdd = new Intent(Intent.ACTION_GET_CONTENT);
	    			intentAdd.setType("image/*");
		  		    mRowId = null; // set null for Insert
	    			this.startActivityForResult(Intent.createChooser(intentAdd, 
	    										   "Select image"),
	    										   Util.PICTURE_CHOOSER);		    			
	        	}
			}
		} 
		else if (resultCode == RESULT_CANCELED)
		{
	        // hide action bar
			getActionBar().hide();
			// set background to transparent
			getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
			
			Toast.makeText(Note_addReadyPicture.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
            note_common.deleteNote(mRowId);
            mEnSaveDb = false;
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return; // must add this
		}
	}
	
	// show Continue dialog
	void showContinueConfirmationDialog()
	{
        setContentView(R.layout.note_add_camera_picture);
        setTitle(R.string.note_reday_picture_continue_dlg_title);// set title

		// Continue button
        Button okButton = (Button) findViewById(R.id.note_add_new_picture_continue);
        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
    			Intent intentAdd = new Intent(Intent.ACTION_GET_CONTENT);
    			intentAdd.setType("image/*");
	  		    mRowId = null; // set null for Insert
	  		    UtilImage.bShowExpandedImage = false; // set for getting new row Id
    			startActivityForResult(Intent.createChooser(intentAdd, 
    										   "Select image"),
    										   Util.PICTURE_CHOOSER);	            	
            }
        });
        
        // cancel button
        Button cancelButton = (Button) findViewById(R.id.note_add_new_picture_cancel);
        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        // cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
            	if(UtilImage.bShowExpandedImage == true)
        	    	UtilImage.closeExpandedImage();
	            mEnSaveDb = false;
	            finish();
            }
        });
        
        final String pictureUri = mSelectedPictureUri;//mPictureUriInDB;
        final ImageView imageView = (ImageView) findViewById(R.id.expanded_image_after_take);
        
	    	imageView.post(new Runnable() {
		        @Override
		        public void run() {
		        	try {
						UtilImage.showImage(imageView, pictureUri , Note_addReadyPicture.this);
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("show image error");
					}
		        } 
		    });
	}
}