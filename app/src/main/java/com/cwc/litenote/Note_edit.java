package com.cwc.litenote;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Note_edit extends Activity {

    private Long mRowId, mCreatedTime;
    private String mTitle, mPictureUri, mAudioUri, mCameraPictureUri, mBody;
    SharedPreferences mPref_style;
    SharedPreferences mPref_delete_warn;
    Note_common note_common;
    private boolean mEnSaveDb = true;
    boolean bUseCameraPicture;
    DB mDb;
    static TouchImageView mEnlargedImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.note_edit);
        setTitle(R.string.edit_note_title);// set title
    	
        System.out.println("Note_edit / onCreate");
        
		mEnlargedImage = (TouchImageView)findViewById(R.id.expanded_image);

		UilCommon.init();
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
        
    	Bundle extras = getIntent().getExtras();
    	mRowId = extras.getLong(DB.KEY_NOTE_ID);
    	mPictureUri = extras.getString(DB.KEY_NOTE_PICTURE_URI);
    	mAudioUri = extras.getString(DB.KEY_NOTE_AUDIO_URI);
    	mTitle = extras.getString(DB.KEY_NOTE_TITLE);
    	mBody = extras.getString(DB.KEY_NOTE_BODY);
    	mCreatedTime = extras.getLong(DB.KEY_NOTE_CREATED);
        
    	//initialization
        note_common = new Note_common(this, mRowId, mTitle, mPictureUri, mAudioUri, "", mBody, mCreatedTime);
        note_common.UI_init();
        mCameraPictureUri = "";
        bUseCameraPicture = false;
        
        // get picture Uri in DB if instance is not null
        mDb = new DB(Note_edit.this);
        if(savedInstanceState != null)
        {
	        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	        System.out.println("Note_edit / onCreate / mRowId =  " + mRowId);
	        if(mRowId != null)
	        {
	        	mPictureUri = mDb.getNotePictureUriById(mRowId);
	       		Note_common.mCurrentPictureUri = mPictureUri;
	        	mAudioUri = mDb.getNoteAudioUriById(mRowId);
	        	Note_common.mCurrentAudioUri = mAudioUri;
	        }
	        mDb.doClose();
        }
        
    	// show view
        Note_common.populateFields(mRowId);
		
		// OK button: edit OK, save
        Button okButton = (Button) findViewById(R.id.note_edit_ok);
        okButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
		// OK
        okButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_OK);
				if(Note_common.bRemovePictureUri)
				{
					mPictureUri = "";
				}
				if(Note_common.bRemoveAudioUri)
				{
					mAudioUri = "";
				}				
                mEnSaveDb = true;
                finish();
            }

        });
        
        // delete button: delete note
        Button delButton = (Button) findViewById(R.id.note_edit_delete);
        delButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        // delete
        delButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
				//warning :start
        		mPref_delete_warn = getSharedPreferences("delete_warn", 0);
            	if(mPref_delete_warn.getString("KEY_DELETE_WARN_MAIN","enable").equalsIgnoreCase("enable") &&
            	   mPref_delete_warn.getString("KEY_DELETE_NOTE_WARN","yes").equalsIgnoreCase("yes")) 
            	{
        			Util util = new Util(Note_edit.this);
    				util.vibrate();
            		
            		Builder builder1 = new Builder(Note_edit.this ); 
            		builder1.setTitle(R.string.confirm_dialog_title)
                        .setMessage(R.string.confirm_dialog_message)
                        .setNegativeButton(R.string.confirm_dialog_button_no, new OnClickListener()
                        {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                        	{/*nothing to do*/}
                        })
                        .setPositiveButton(R.string.confirm_dialog_button_yes, new OnClickListener()
                        {   @Override
                            public void onClick(DialogInterface dialog1, int which1)
                        	{
                        		note_common.deleteNote(mRowId);
                        		AudioPlayer.prepareAudioInfo(Note_edit.this);                        		
                            	finish();
                        	}
                        })
                        .show();//warning:end
            	}
            	else{
            	    //no warning:start
	                setResult(RESULT_CANCELED);
	                note_common.deleteNote(mRowId);
	                finish();
            	}
            }
        });
        
        // cancel button: leave, do not save current modification
        Button cancelButton = (Button) findViewById(R.id.note_edit_cancel);
        cancelButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        // cancel
        cancelButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                
                // check if note content is modified
               	if(note_common.isNoteModified())
            	{
               		// show confirmation dialog
            		confirmToUpdateDlg();
            	}
            	else
            	{
            		mEnSaveDb = false;
                    finish();
            	}
            }
        });
    }
    
    // confirm to update change or not
    void confirmToUpdateDlg()
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(Note_edit.this);
		builder.setTitle(R.string.confirm_dialog_title)
	           .setMessage(R.string.edit_note_confirm_update)
	           // Yes, to update
			   .setPositiveButton(R.string.confirm_dialog_button_yes, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						if(Note_common.bRemovePictureUri)
						{
							mPictureUri = "";
						}
						if(Note_common.bRemoveAudioUri)
						{
							mAudioUri = "";
						}						
					    mEnSaveDb = true;
					    finish();
					}})
			   // cancel
			   .setNeutralButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{   // do nothing
					}})
			   // no, roll back to original status		
			   .setNegativeButton(R.string.confirm_dialog_button_no, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Bundle extras = getIntent().getExtras();
						String originalPictureFileName = extras.getString(DB.KEY_NOTE_PICTURE_URI);

						if(originalPictureFileName.isEmpty())
						{   // no picture at first
							note_common.removePictureStringFromOriginalNote(mRowId);
		                    mEnSaveDb = false;
						}
						else
						{	// roll back existing picture
							Note_common.bRollBackData = true;
							mPictureUri = originalPictureFileName;
							mEnSaveDb = true;
						}	
						
						String originalAudioFileName = extras.getString(DB.KEY_NOTE_AUDIO_URI);

						if(originalAudioFileName.isEmpty())
						{   // no picture at first
							note_common.removeAudioStringFromOriginalNote(mRowId);
		                    mEnSaveDb = false;
						}
						else
						{	// roll back existing picture
							Note_common.bRollBackData = true;
							mAudioUri = originalAudioFileName;
							mEnSaveDb = true;
						}							
						
	                    finish();
					}})
			   .show();
    }
    

    // for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
        
        System.out.println("Note_edit / onPause / mEnSaveDb = " + mEnSaveDb);
        System.out.println("Note_edit / onPause / mPictureUri = " + mPictureUri);
        System.out.println("Note_edit / onPause / mAudioUri = " + mAudioUri);
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUri, mAudioUri, ""); 
    }

    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        System.out.println("Note_edit / onSaveInstanceState / mEnSaveDb = " + mEnSaveDb);
        System.out.println("Note_edit / onSaveInstanceState / bUseCameraPicture = " + bUseCameraPicture);
        System.out.println("Note_edit / onSaveInstanceState / mCameraPictureUri = " + mCameraPictureUri);
        
        
        if(Note_common.bRemovePictureUri)
    	    outState.putBoolean("removeOriginalPictureUri",true);

        if(Note_common.bRemoveAudioUri)
    	    outState.putBoolean("removeOriginalAudioUri",true);
        
        
        if(bUseCameraPicture)
        {
        	outState.putBoolean("UseCameraPicture",true);
        	outState.putString("showCameraPictureUri", mPictureUri);
        }
        else
        {
        	outState.putBoolean("UseCameraPicture",false);
        	outState.putString("showCameraPictureUri", "");
        }
        
        mRowId = Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUri, mAudioUri, ""); //??? 
        outState.putSerializable(DB.KEY_NOTE_ID, mRowId);
        
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    	if(savedInstanceState.getBoolean("UseCameraPicture"))
    		bUseCameraPicture = true;
    	else
    		bUseCameraPicture = false;
    	
    	mCameraPictureUri = savedInstanceState.getString("showCameraPictureUri");
    	
    	System.out.println("Note_edit / onRestoreInstanceState / savedInstanceState.getBoolean removeOriginalPictureUri =" +
    							savedInstanceState.getBoolean("removeOriginalPictureUri"));
        if(savedInstanceState.getBoolean("removeOriginalPictureUri"))
        {
        	mCameraPictureUri = "";
        	Note_common.mOriginalPictureUri="";
        	Note_common.mCurrentPictureUri="";
        	note_common.removePictureStringFromOriginalNote(mRowId);
        	Note_common.populateFields(mRowId);
        	Note_common.bRemovePictureUri = true;
        }
        if(savedInstanceState.getBoolean("removeOriginalAudioUri"))
        {
        	Note_common.mOriginalAudioUri="";
        	Note_common.mCurrentAudioUri="";
        	note_common.removeAudioStringFromOriginalNote(mRowId);
        	Note_common.populateFields(mRowId);
        	Note_common.bRemoveAudioUri = true;
        }        
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    public void onBackPressed() {
	    if(Note_common.bShowEnlargedImage == true)
	    {
	    	Note_common.closeEnlargedImage();
	    }
	    else
	    {
	    	if(note_common.isNoteModified())
	    	{
	    		confirmToUpdateDlg();
	    	}
	    	else
	    	{
	            mEnSaveDb = false;
	            finish();
	    	}
	    }
    }
    
    static final int TAKE_PICTURE = R.id.TAKE_PICTURE;
    static final int ADD_AUDIO = R.id.ADD_AUDIO;
	private Uri imageUri;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    menu.add(0, ADD_AUDIO, 0, R.string.note_audio )
	    .setIcon(R.drawable.ic_lock_ringer_on)
	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

	    menu.add(0, TAKE_PICTURE, 1, R.string.note_take_picture )
	    .setIcon(android.R.drawable.ic_menu_camera)
	    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

	    
	    
		return super.onCreateOptionsMenu(menu);
	}
    
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId()) 
        {
		    case android.R.id.home:
		    	if(note_common.isNoteModified())
		    	{
		    		confirmToUpdateDlg();
		    	}
		    	else
		    	{
		            mEnSaveDb = false;
		            finish();
		    	}
		        return true;
	        
            case TAKE_PICTURE:
            	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            	// new picture Uri with current time stamp
            	imageUri = UtilImage.getPictureUri(Util.getCurrentTimeString() + ".jpg",
						   						   Note_edit.this); 
            	mPictureUri = imageUri.toString();
			    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
			    startActivityForResult(intent, Util.TAKE_PICTURE_ACT); 
			    mEnSaveDb = true;
			    Note_common.bRemovePictureUri = false; // reset
			    
			    if(UtilImage.mExpandedImageView != null)
			    	UtilImage.closeExpandedImage();
		        
			    return true;
            
            case ADD_AUDIO:
            	Note_common.bRemoveAudioUri = false; // reset
            	setAudioSource();
			    return true;			    
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    void setAudioSource() 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_note_set_audio_dlg_title);
		// Cancel
		builder.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener()
		   	   {
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{// cancel
				}});
		// Set
		builder.setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) 
		{
		    mEnSaveDb = true;
    		Intent intentAudio = new Intent(Intent.ACTION_GET_CONTENT);
    		intentAudio.setType("audio/*");
    		startActivityForResult(Intent.createChooser(intentAudio, 
    									   getResources().getText(R.string.slide_show_chooser_music)),
    													Util.AUDIO_CHOOSER); 
		}});
		// None
		if(!mAudioUri.isEmpty())
		{
			builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						Note_common.bRemovePictureUri = true;
						Note_common.mOriginalAudioUri = "";
						mAudioUri = "";
						Note_common.removeAudioStringFromCurrentEditNote(mRowId);
						Note_common.populateFields(mRowId);
					}});		
		}
		
		Dialog dialog = builder.create();
		dialog.show();
    }
    
//    static String mSelectedAudioUri;
	protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) 
	{
		// take picture
		if (requestCode == Util.TAKE_PICTURE_ACT)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				imageUri = Uri.parse(Note_common.mCurrentPictureUri);
//				String str = getResources().getText(R.string.note_take_picture_OK ).toString();
//	            Toast.makeText(Note_edit.this, str + " " + imageUri.toString(), Toast.LENGTH_SHORT).show();
	            Note_common.populateFields(mRowId);
	            bUseCameraPicture = true;
	            mCameraPictureUri = Note_common.mCurrentPictureUri;
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				bUseCameraPicture = false;
				// to use captured picture or original picture
				if(!mCameraPictureUri.isEmpty())
				{
					// update
					Note_common.saveStateInDB(mRowId,mEnSaveDb,mCameraPictureUri, mAudioUri, "");//???  // replace with existing picture
					Note_common.populateFields(mRowId);
		            
					// set for Rotate any times
		            bUseCameraPicture = true;
		            mPictureUri = Note_common.mCurrentPictureUri; // for pause
		            mCameraPictureUri = Note_common.mCurrentPictureUri; // for save instance

				}
				else
				{
					// skip new Uri, roll back to original one
			    	Note_common.mCurrentPictureUri = Note_common.mOriginalPictureUri;
			    	mPictureUri = Note_common.mOriginalPictureUri;
					Toast.makeText(Note_edit.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
				}
				
				mEnSaveDb = true;
				Note_common.saveStateInDB(mRowId,mEnSaveDb,mPictureUri, mAudioUri, ""); //???
				Note_common.populateFields(mRowId);
			}
		}
		
		// choose picture
        if(requestCode == Util.PICTURE_CHOOSER && resultCode == Activity.RESULT_OK)
        {
			Uri selectedUri = returnedIntent.getData(); 
			System.out.println("selected Uri = " + selectedUri.toString());
			
			String pictureUri = selectedUri.toString();
        	System.out.println("check onActivityResult / uriStr = " + pictureUri);
        	
        	mRowId = Note_common.saveStateInDB(mRowId,true,pictureUri, mAudioUri, ""); 
        	
            Note_common.populateFields(mRowId);
			
            // set for Rotate any times
            bUseCameraPicture = true;
            mPictureUri = Note_common.mCurrentPictureUri; // for pause
            mCameraPictureUri = Note_common.mCurrentPictureUri; // for save instance

        }  
        
        // choose audio
		if(requestCode == Util.AUDIO_CHOOSER)
		{
			if (resultCode == Activity.RESULT_OK)
			{
				// for audio
				Uri auidoUri = returnedIntent.getData(); 
				
				String audioUriStr = auidoUri.toString();
//				System.out.println(" mPictureUri = " + mPictureUri);
	        	Note_common.saveStateInDB(mRowId,true,mPictureUri, audioUriStr, "");
	        	
	        	Note_common.populateFields(mRowId);
	        	mAudioUri = audioUriStr;
	    			
	        	showSavedFileToast(audioUriStr);
			} 
			else if (resultCode == RESULT_CANCELED)
			{
				Toast.makeText(Note_edit.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
	            setResult(RESULT_CANCELED, getIntent());
	            finish();
	            return; // must add this
			}
		}
	}
	
	// show audio file name
	void showSavedFileToast(String audioUri)
	{
        String audioName = Util.getDisplayNameByUri(Uri.parse(audioUri), Note_edit.this);
		Toast.makeText(Note_edit.this,
						audioName,
						Toast.LENGTH_SHORT)
						.show();
	}
	
}
