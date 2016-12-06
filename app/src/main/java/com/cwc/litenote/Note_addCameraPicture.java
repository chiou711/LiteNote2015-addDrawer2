package com.cwc.litenote;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Note: 
 * 	mCameraPictureUri: used to show in confirmation Continue dialog
 *  	Two conditions:
 *  	1. is got after taking picture
 *  	2. is kept during rotation
 * 
 *  UtilImage.bShowExpandedImage: used to control DB saving state
 * 
 *  Note_common: used to do DB operation
 */
public class Note_addCameraPicture extends Activity {

    static Long mRowId;
    static String mCameraPictureUri;
    Note_common note_common;
    static boolean mEnSaveDb;
	static String mPictureUriInDB;
	private static DB mDb;
    boolean bUseCameraPicture;
	static int TAKE_PICTURE_ACT = 1;    
	private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("Note_addCameraPicture / onCreate");
        
        note_common = new Note_common(this);
        mPictureUriInDB = "";
        mCameraPictureUri = "";
        bUseCameraPicture = false;
        mEnSaveDb = true;
        
        // get row Id from saved instance
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB.KEY_NOTE_ID);
        
        // get picture Uri in DB if instance is not null
        mDb = new DB(this);
        if(savedInstanceState != null)
        {
	        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	        System.out.println("Note_addCameraPicture / onCreate / mRowId =  " + mRowId);
	        if(mRowId != null)
	        	mPictureUriInDB = mDb.getNotePictureUriById(mRowId);
	        mDb.doClose();
        }
        
        // at the first beginning
        if(savedInstanceState == null)
        {
    	    mPictureUriInDB = takePicture();
        	if((UtilImage.mExpandedImageView != null) &&
               (UtilImage.mExpandedImageView.getVisibility() == View.VISIBLE) &&
               (UtilImage.bShowExpandedImage == true))
        	{
        		UtilImage.closeExpandedImage();
        	}
        }
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	if(savedInstanceState.getBoolean("UseCameraPicture"))
    		bUseCameraPicture = true;
    	else
    		bUseCameraPicture = false;
    	
    	mCameraPictureUri = savedInstanceState.getString("showCameraPictureUri");
    	
    	if(savedInstanceState.getBoolean("ShowConfirmContinueDialog"))
    	{
    		showContinueConfirmationDialog();
    		System.out.println("showContinueDialog again");
    	}
    	
    }

    // for Add new picture (stage 1)
    // for Rotate screen (stage 2)
    @Override
    protected void onPause() {
    	System.out.println("Note_addCameraPicture / onPause");
        super.onPause();
        
        if( UtilImage.bShowExpandedImage == false )
        	mRowId = Note_common.savePictureStateInDB(mRowId,mEnSaveDb,mPictureUriInDB, "", ""); 
    }

    // for Add new picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addCameraPicture / onSaveInstanceState");
   	 	
        if(bUseCameraPicture)
        {
        	outState.putBoolean("UseCameraPicture",true);
        	outState.putString("showCameraPictureUri", mCameraPictureUri);
        }
        else
        {
        	outState.putBoolean("UseCameraPicture",false);
        	outState.putString("showCameraPictureUri", "");
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
    	if((UtilImage.mExpandedImageView != null) &&
          	   (UtilImage.mExpandedImageView.getVisibility() == View.VISIBLE) &&
         	   (UtilImage.bShowExpandedImage == true))    	
    	{
	    	UtilImage.closeExpandedImage();
    	}
	        mEnSaveDb = false;
	        finish();
    }
    
    // Create image file
    private File createTempImageFile() throws IOException {
        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "IMG_" + Util.getCurrentTimeString();
		File appPicturesDir = Util.getPicturesDir(this);
		//  under DCIM, create a sub-directory named App name
		if(!appPicturesDir.isDirectory())
			appPicturesDir.mkdir();        
        
		
		// note: createTempFile will generate random number and a 0 bit file size instance first
//        File imageFile = File.createTempFile(imageFileName, /* prefix */
//								         ".jpg",         	/* suffix */
//								         appPicturesDir  	/* directory */);
		
        File imageFile = new File(appPicturesDir /* directory */,
        						  imageFileName  /* prefix */ +
        						  ".jpg" 		 /* suffix */);
        
        return imageFile;
    }
    

    private String takePicture() 
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) 
        {
            // Create temporary image File where the photo should go
            File photoFile = null;
            try 
            {
                photoFile = createTempImageFile();
            } 
            catch (IOException ex)
            {
                // Error occurred while creating the File
            }
            
            // Continue only if the File was successfully created
            if (photoFile != null) 
            {
            	mImageUri = Uri.fromFile(photoFile); // so far, file size is 0 
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri); // appoint Uri for captured image
                startActivityForResult(takePictureIntent, TAKE_PICTURE_ACT);
            }
        }
        return mImageUri.toString();
    }    
    
    // On Activity Result
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		System.out.println("Note_addCameraPicture / onActivityResult");
		if (requestCode == TAKE_PICTURE_ACT)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				// disable Rotate to avoid leak window
//				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);				
	            
				System.out.println("mPictureUriInDB = " + mPictureUriInDB);
		        
				if( UtilImage.bShowExpandedImage == false )
		        	mRowId = Note_common.savePictureStateInDB(mRowId,mEnSaveDb,mPictureUriInDB, "", ""); 
				
				// set for Rotate any times
		        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		        if(mRowId != null)
		        {
		        	mCameraPictureUri = mDb.getNotePictureUriById(mRowId);
		        }
		        mDb.doClose();
	            
	        	SharedPreferences pref_takePicture;
        		pref_takePicture = getSharedPreferences("takePicutre", 0);
        		
    			if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
    				(Note_common.getCount() > 0) )
		               NoteFragment.swap();
    			
    			Toast.makeText(this, R.string.toast_saved , Toast.LENGTH_SHORT).show();

				// check and delete duplicated image file in 100ANDRO (Sony)
				int lastContentId = getLastCapturedImageId(this);
				handleDuplictedPicture(this, lastContentId);
    			
    			
        		// show confirm Continue dialog
	        	if(pref_takePicture.getString("KEY_SHOW_CONFIRMATION_DIALOG","yes").equalsIgnoreCase("yes"))
	        	{
	    			 bUseCameraPicture = true; 
		            // set Continue Taking Picture dialog
	        		showContinueConfirmationDialog();
	        	}
	        	else
	        	// not show confirm Continue dialog
	        	{
	    			bUseCameraPicture = false; 
	        		
	        		// take picture without confirmation dialog 
		  		    mRowId = null; // set null for Insert
	    			mPictureUriInDB = takePicture();
	        	}

	            // enable Rotate 
//	            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				// hide action bar
				getActionBar().hide();
				
				// set action bar to transparent
//				getActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
//				getActionBar().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
//				setTitle("");
				
				// disable content view
//				findViewById(android.R.id.content).setVisibility(View.INVISIBLE);

				// set background to transparent
				getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
				
				Toast.makeText(this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
				
                note_common.deleteNote(mRowId);
                mEnSaveDb = false;
                
                // When auto time out of taking picture App happens, 
            	// Note_addCameraPicture activity will start from onCreate,
                // at this case, mImageUri is null
                if(mImageUri != null) 
                {
	           		File tempFile = new File(mImageUri.getPath());
	        		if(tempFile.isFile())
	        		{
	                    // delete 0 bit temporary file
	        			tempFile.delete();
	        			System.out.println("temp 0 bit file is deleted");
	        		}
                }
                finish();
                return; // must add this
			}
			
		}
	}

	public static void handleDuplictedPicture(Context context, int lastContentId) 
	{
		String path = null;
	    /*
	     * Checking for duplicate images
	     * This is necessary because some camera implementation not only save where you want them to save but also in their default location.
	     */
	    if (lastContentId == 0)
	        return;
	    
	    final String[] projection = {MediaStore.Images.ImageColumns.DATA,
	    							 MediaStore.Images.ImageColumns.DATE_TAKEN,
	    							 MediaStore.Images.ImageColumns.SIZE,
	    							 MediaStore.Images.ImageColumns._ID};
	    final String imageWhere = MediaStore.Images.Media._ID + "=?";
	    final String[] imageArguments = {Integer.toString(lastContentId)};
	    final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
	    
	    Cursor imageCursor = context.getContentResolver()
	    							.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	    								   projection,
	    								   imageWhere,
	    								   imageArguments,
	    								   imageOrderBy);

	    File gFile = null;
	    long dateTaken = 0;
    	Date now = new Date(); 
	    if (imageCursor.getCount() > 0) 
	    {
	        imageCursor.moveToFirst(); // newest one
	        path = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
	        dateTaken = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
	        System.out.println("date taken = " + Util.getTimeString(dateTaken) );
	        System.out.println("last Id point to file path: " + path);
	        gFile = new File(path);
	    }
	    else
	    	System.out.println("imageCursor.getCount() = " + imageCursor.getCount() ); 	

	    imageCursor.close();

	    // Check time for avoiding Delete existing file, since lastContentId could points to 
	    // wrong file by experiment
        System.out.println("current time = " + Util.getTimeString(now.getTime()) );
	    long elapsedTime = Math.abs(dateTaken - now.getTime() );

        if( (gFile != null) && (elapsedTime < 10000) ) // tolerance 10 seconds
	    {
    		// delete file
	        boolean bDelete = gFile.delete();
	        System.out.println("deleted file path = " + path);

	          if (bDelete) {
//	        	  String repPath =  path.replaceAll("'", "''"); //??? for what
	        	  String repPath =  path;
	        	  System.out.println("path after replace = " + repPath);
	        	  
	        	  // delete 
	        	  int deletedRows = context.getContentResolver().delete(
	        	            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	        	            MediaStore.Images.ImageColumns.DATA
	        	                    + "='"
	        	                    + repPath + "'", null);	        	  
	        	  
	        	  System.out.println("deleted thumbnail deletedRows = " + deletedRows);	  
	       }
	    }
	}

	public static int getLastCapturedImageId(Context context)
	{
	    final String[] imageColumns = { MediaStore.Images.Media._ID };
	    final String imageOrderBy = MediaStore.Images.Media._ID+" DESC";
	    final String imageWhere = null;
	    final String[] imageArguments = null;
	    Cursor imageCursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	    														imageColumns,
	    														imageWhere,
	    														imageArguments,
	    														imageOrderBy);
	    if(imageCursor.moveToFirst())
	    {
	        int id = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
	        imageCursor.close();
	        System.out.println("last captured image Id = " + id);
	        return id;
	    }else
	    {
	        return 0;
	    }
	}	

	
//	// get real path from Uri
//	public String getRealPathFromURI(Context context, Uri contentUri) {
//		  Cursor cursor = null;
//		  try { 
//		    String[] proj = { MediaStore.Images.Media.DATA };
//		    cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
//		    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//		    cursor.moveToFirst();
//		    return cursor.getString(column_index);
//		  } finally {
//		    if (cursor != null) {
//		      cursor.close();
//		    }
//		  }
//		}
	
	
	// show Continue dialog
	void showContinueConfirmationDialog()
	{
        setContentView(R.layout.note_add_camera_picture);
        setTitle(R.string.note_take_picture_continue_dlg_title); 
        
		// Continue button
        Button okButton = (Button) findViewById(R.id.note_add_new_picture_continue);
        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_camera, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
        		
            	// take picture without confirmation dialog 
	  		    mRowId = null; // set null for Insert
            	mPictureUriInDB = takePicture();
	  		    UtilImage.bShowExpandedImage = false; // set for getting new row Id
            }
        });
        
        // cancel button
        Button cancelButton = (Button) findViewById(R.id.note_add_new_picture_cancel);
        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        // cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
            	if((UtilImage.mExpandedImageView != null) &&
             	   (UtilImage.mExpandedImageView.getVisibility() == View.VISIBLE) &&
            	   (UtilImage.bShowExpandedImage == true))
            	{
        	    	UtilImage.closeExpandedImage();
            	}
            	
	            mEnSaveDb = false;
	            finish();
            }
        });
        
        final String pictureUri = mCameraPictureUri;//mPictureUriInDB;
        final ImageView imageView = (ImageView) findViewById(R.id.expanded_image_after_take);
        
	    	imageView.post(new Runnable() {
		        @Override
		        public void run() {
		        	try 
		        	{
						UtilImage.showImage(imageView, pictureUri , Note_addCameraPicture.this);
					} 
		        	catch (IOException e) 
		        	{
						e.printStackTrace();
						System.out.println("show image error");
					}
		        } 
		    });
	}
	
}
