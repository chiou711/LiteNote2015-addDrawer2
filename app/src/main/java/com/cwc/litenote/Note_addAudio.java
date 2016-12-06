package com.cwc.litenote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class Note_addAudio extends FragmentActivity { 

    static Long mRowId;
    static String mSelectedAudioUri;
    Note_common note_common;
    static boolean mEnSaveDb = true;
	static String mAudioUriInDB;
	private static DB mDb;
    boolean bUseSelectedFile;
	private static final int MEDIA_ADD = 1;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        System.out.println("Note_addMusic / onCreate");
        
        note_common = new Note_common(this);
        mAudioUriInDB = "";
        mSelectedAudioUri = "";
        bUseSelectedFile = false;
			
        // get row Id from saved instance
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(DB.KEY_NOTE_ID);
        
        // get audio Uri in DB if instance is not null
        mDb = new DB(Note_addAudio.this);
        if(savedInstanceState != null)
        {
	        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	        System.out.println("Note_addMusic / mRowId =  " + mRowId);
	        if(mRowId != null)
	        	mAudioUriInDB = mDb.getNoteAudioUriById(mRowId);
	        mDb.doClose();
        }
        
        // at the first beginning
        if(savedInstanceState == null)
        	chooseMusic();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
    }

    // for Rotate screen
    @Override
    protected void onPause() {
    	System.out.println("Note_addAudio / onPause");
        super.onPause();
    }

    // for Add new picture (stage 2)
    // for Rotate screen (stage 2)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
   	 	System.out.println("Note_addNew / onSaveInstanceState");
        outState.putSerializable(DB.KEY_NOTE_ID, mRowId);
    }
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        mEnSaveDb = false;
        finish();
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
	{
		System.out.println("Note_addAudio / onActivityResult");
		if (resultCode == Activity.RESULT_OK)
		{
			// for music
			if(requestCode == MEDIA_ADD)
			{
				Uri selectedUri = imageReturnedIntent.getData(); 
				
				String uriStr = selectedUri.toString();
	  		    mRowId = null; // set null for Insert
	        	mRowId = Note_common.insertAudioToDB(uriStr); 
	        	mSelectedAudioUri = uriStr;
	        	
	        	if( getIntent().getExtras().getString("extra_ADD_NEW_TO_TOP", "false").equalsIgnoreCase("true") &&
	        		(Note_common.getCount() > 0) )
	        		NoteFragment.swap();
	    			
	        	showSavedFileToast();
	        	chooseMusic();	
	        	AudioPlayer.prepareAudioInfo(this);
	        	NoteFragment.mItemAdapter.notifyDataSetChanged();
			}
		} 
		else if (resultCode == RESULT_CANCELED)
		{
			Toast.makeText(Note_addAudio.this, R.string.note_cancel_add_new, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return; // must add this
		}
	}

    void chooseMusic()
    {
	    mEnSaveDb = true;
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("audio/*");
		startActivityForResult(Intent.createChooser(intent, 
									   getResources().getText(R.string.slide_show_chooser_music)),
													MEDIA_ADD);     	
    }	
	
	// show audio file name
	void showSavedFileToast()
	{
        final String audioUri = mSelectedAudioUri;
        String audioName = Util.getDisplayNameByUri(Uri.parse(audioUri), Note_addAudio.this);
		Toast.makeText(Note_addAudio.this,
						audioName,
						Toast.LENGTH_SHORT)
						.show();
	}
}