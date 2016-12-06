/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cwc.litenote;

import java.util.Date;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Note_common {

	static TextView mAudioTextView;
    static EditText mTitleEditText;
    static String mOriginalTitle;
    
    static ImageView mPicImageView;
    static String mPictureUriInDB;
    static String mAudioUriInDB;
    static String mOriginalPictureUri;
    static String mCurrentPictureUri;
    static String mCurrentAudioUri;

    static String mOriginalAudioUri;
    static String mOriginalDrawingUri;

    static EditText mBodyEditText;
    
    static String mOriginalBody;
    
    Long mRowId;
	static Long mOriginalCreatedTime;
	static Long mOriginalMarking;
    
    static boolean bRollBackData;
    static boolean bRemovePictureUri = false;
    static boolean bRemoveAudioUri = false;
    boolean bEditPicture = false;

    private static DB mDb;
    SharedPreferences mPref_style;
    SharedPreferences mPref_delete_warn;
    static Activity mAct;
    static int mStyle;
    
    public Note_common(Activity act,Long rowId,String strTitle, String pictureUri, String audioUri, String drawingUri, String strBody, Long createdTime)
    {
    	mAct = act;
    	mRowId = rowId;
    			
    	mOriginalTitle = strTitle;
	    mOriginalBody = strBody;
	    mOriginalPictureUri = pictureUri;
	    mOriginalAudioUri = audioUri;
	    mOriginalDrawingUri = drawingUri;
	    
	    mOriginalCreatedTime = createdTime;
	    mCurrentPictureUri = pictureUri;
	    mCurrentAudioUri = audioUri;
	    
    	DB.setNotes_TableId(DB.getNotesTableId()); 
        mDb = new DB(mAct);
    	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	    mOriginalMarking = mDb.getNoteMarkingById(rowId);
		mDb.doClose();

		bRollBackData = false;
		bEditPicture = true;
    }
    
    public Note_common(Activity act)
    {
    	mAct = act;
    	DB.setNotes_TableId(DB.getNotesTableId()); 
        mDb = new DB(mAct);
    }
    
    void UI_init()
    {
    	mAudioTextView = (TextView) mAct.findViewById(R.id.edit_audio);
        mTitleEditText = (EditText) mAct.findViewById(R.id.edit_title);
        mPicImageView = (ImageView) mAct.findViewById(R.id.edit_picture);
        mBodyEditText = (EditText) mAct.findViewById(R.id.edit_body);
        
        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		mStyle = mDb.getTabStyle(TabsHostFragment.mCurrentTabIndex);
		mDb.doClose();

		//set audio color
//		mAudioTextView.setTextColor(Util.mText_ColorArray[style]);
//		mAudioTextView.setBackgroundColor(Util.mBG_ColorArray[style]);
		
		//set title color
		mTitleEditText.setTextColor(Util.mText_ColorArray[mStyle]);
		mTitleEditText.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
		
		mPicImageView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
		
		//set body color 
		mBodyEditText.setTextColor(Util.mText_ColorArray[mStyle]);
		mBodyEditText.setBackgroundColor(Util.mBG_ColorArray[mStyle]);	
		
		// set thumb nail listener
        mPicImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	if(bShowEnlargedImage == true)
            		closeEnlargedImage();
            	else
                {
                	System.out.println("Note_common / mPictureUriInDB = " + mPictureUriInDB);
                	if(!Util.isEmptyString(mPictureUriInDB))
                	{
                		bRemovePictureUri = false;
                		System.out.println("mPicImageView.setOnClickListener / mPictureUriInDB = " + mPictureUriInDB);
                		
                		// check if pictureUri has scheme
                		if(Util.isUriExisted(mPictureUriInDB, mAct))
                		{
	                		if(Uri.parse(mPictureUriInDB).isAbsolute())
	                		{
//	                			UtilImage.zoomImageFromThumb(mPicImageView,
//	                										 mPictureUriInDB ,
//	                										 mAct);
	                			String uriString = Uri.parse(mPictureUriInDB).toString();
	        					UilCommon.imageLoader.displayImage(uriString, 
	        													   Note_edit.mEnlargedImage,
	        													   UilCommon.optionsForFadeIn,
		        					 new SimpleImageLoadingListener()
			   						 {
			   							 @Override
			   							 public void onLoadingStarted(String imageUri, View view) {
			   							 }
		
			   							 @Override
			   							 public void onLoadingFailed(String imageUri, View view,
			   									 FailReason failReason) {
			   							 }
		
			   							 @Override
			   							 public void onLoadingComplete(String imageUri, View view, 
			   									 Bitmap loadedImage) 
			   							 {
			 	                			Note_edit.mEnlargedImage.setVisibility(View.VISIBLE);
				                			bShowEnlargedImage = true;
			   							 }
		   						    });
	                		}
	                		else
	                		{
	                			System.out.println("mPictureUriInDB is not Uri format");
	                		}
                		}
                		else
                			Toast.makeText(mAct,R.string.file_not_found,Toast.LENGTH_SHORT).show();
                	}
                	else
            			Toast.makeText(mAct,R.string.file_is_not_created,Toast.LENGTH_SHORT).show();

				} 
            }
        });
        
		// set thumb nail long click listener
        mPicImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
            	if(bEditPicture)
            		setPictureImage();
                return false;
            }
        });
    }
    
    static boolean bShowEnlargedImage;
    public static void closeEnlargedImage()
    {
    	System.out.println("closeExpandImage");
		Note_edit.mEnlargedImage.setVisibility(View.GONE);
		bShowEnlargedImage = false;
    }
    
    void setPictureImage() 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(mAct);
		builder.setTitle(R.string.edit_note_set_picture_dlg_title)
			   .setNeutralButton(R.string.btn_Select, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						bRemovePictureUri = false; // reset
						// For selecting local gallery
//						Intent intent = new Intent(mAct, PictureGridAct.class);
//						intent.putExtra("gallery", false);
//						mAct.startActivityForResult(intent, Util.ACTIVITY_SELECT_PICTURE);
						
						// select global
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
						intent.setType("image/*");
						mAct.startActivityForResult(Intent.createChooser(intent, 
													   "Select image"),
													   Util.PICTURE_CHOOSER);
						
					}})					
			   .setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{// cancel
					}});

		if(!mPictureUriInDB.isEmpty())
		{
				builder.setPositiveButton(R.string.btn_None, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					//just delete picture file name
					mCurrentPictureUri = "";
					mOriginalPictureUri = "";
			    	removePictureStringFromCurrentEditNote(mRowId);
			    	populateFields(mRowId);
			    	bRemovePictureUri = true;
				}});
		}
		
		Dialog dialog = builder.create();
		dialog.show();
    }
    
    void deleteNote(Long rowId)
    {
    	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        // for Add new note (mRowId is null first), but decide to cancel 
        if(rowId != null)
        	mDb.deleteNote(rowId);
        mDb.close();
    }
    

    
    static void populateFields(Long rowId) 
    {
    	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	if (rowId != null) 
    	{
    		mPictureUriInDB = mDb.getNotePictureUriById(rowId);
			System.out.println("populateFields / mPictureFileNameInDB = " + mPictureUriInDB);

			mAudioUriInDB = mDb.getNoteAudioUriById(rowId);
    		
    		if(!mPictureUriInDB.isEmpty())
    		{
    			Uri imageUri = Uri.parse(mPictureUriInDB);
				System.out.println("populateFields / set image bitmap / imageUri = " + imageUri);
	    		// set picture
				try 
				{
					mPicImageView.setVisibility(View.VISIBLE );
					
					UilCommon.imageLoader.displayImage(imageUri.toString(), 
														mPicImageView,
														UilCommon.optionsForRounded,
														UilCommon.animateFirstListener);
					
					
					mPicImageView.setImageBitmap(UtilImage.decodeSampledBitmapFromUri(imageUri, 50, 50, mAct));
				} 
				catch (Exception e) 
				{
			        Log.w("Picture file name is not found", e.toString());
					mPicImageView.setImageResource(R.drawable.ic_cab_done_holo);//ic_dialog_focused_holo);
			    }
    		}
    		else
    		{
				mPicImageView.setImageResource(mStyle%2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty);
    		}
			
    		// audio
        	if(!Util.isEmptyString(mAudioUriInDB))
    		{
    			Uri audioUri = Uri.parse(mAudioUriInDB);
    			String audio_name = Util.getDisplayNameByUri(audioUri,mAct);
				System.out.println("populateFields / set audio name / audio_name = " + audio_name);
				mAudioTextView.setText(mAct.getResources().getText(R.string.note_audio) + ": " + audio_name);
    		}
        	else
				mAudioTextView.setText("");
        		
    		
			String strTitleEdit = mDb.getNoteTitleById(rowId);
            mTitleEditText.setText(strTitleEdit);
            mTitleEditText.setSelection(strTitleEdit.length());

            String strBodyEdit = mDb.getNoteBodyById(rowId);
            mBodyEditText.setText(strBodyEdit);
            mBodyEditText.setSelection(strBodyEdit.length());
        }
    	else
    	{
			String strTitleEdit = "";
            mTitleEditText.setText(strTitleEdit);
            mTitleEditText.setSelection(strTitleEdit.length());

            String strBodyEdit = "";
            mBodyEditText.setText(strBodyEdit);
            mBodyEditText.setSelection(strBodyEdit.length());
    	}
        mDb.close();
    }
    
    boolean isTitleModified()
    {
    	return !mOriginalTitle.equals(mTitleEditText.getText().toString());
    }
    
    boolean isPictureModified()
    {
    	return !mOriginalPictureUri.equals(mPictureUriInDB);
    }
    
    boolean isAudioModified()
    {
    	if(mOriginalAudioUri == null)
    		return false;
    	else
    		return !mOriginalAudioUri.equals(mAudioUriInDB);
    }    
    
    boolean isBodyModified()
    {
    	return !mOriginalBody.equals(mBodyEditText.getText().toString());
    }
    
    boolean isTimeCreatedModified()
    {
    	return false; 
    }
    
    boolean isNoteModified()
    {
    	boolean bModified = false;
    	if( isTitleModified() || isPictureModified() || isAudioModified() ||
    		isBodyModified() || bRemovePictureUri || bRemoveAudioUri)
    	{
    		bModified = true;
    	}
    	
    	return bModified;
    }
    
    boolean isTextEdited()
    {
    	boolean bEdit = false;
    	String curTitle = mTitleEditText.getText().toString();
    	String curBody = mBodyEditText.getText().toString();
    	if(!curTitle.isEmpty() || !curBody.isEmpty() || !(null ==  mPictureUriInDB))    		
    		bEdit = true;
    	
    	return bEdit;
    }

	public static Long saveStateInDB(Long rowId,boolean enSaveDb, String pictureUri, String audioUri, String drawingUri) 
	{
		boolean mEnSaveDb = enSaveDb;
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	String title = mTitleEditText.getText().toString();
    	String body = mBodyEditText.getText().toString();
    	
        if(mEnSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( (!title.isEmpty()) || (!body.isEmpty()) ||(!pictureUri.isEmpty()) || (!audioUri.isEmpty()))
	        	{
	        		// insert
	        		System.out.println("Note_common / saveState / insert");
	        		rowId = mDb.insertNote(title, pictureUri, audioUri, drawingUri, body, 0, (long) 0);// add new note, get return row Id
	        	}
        		mCurrentPictureUri = pictureUri; // update file name
        		mCurrentAudioUri = audioUri; // update file name
	        } 
	        else // for Edit
	        {
    	        Date now = new Date(); 
//	        	if( (!title.isEmpty()) || (!body.isEmpty()) || (!pictureUri.isEmpty()) || (!audioUri.isEmpty()) )
	        	if( !Util.isEmptyString(title) || !Util.isEmptyString(body) ||
	        		!Util.isEmptyString(pictureUri) || !Util.isEmptyString(audioUri) )
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_common / saveState / update: roll back");
	        			title = mOriginalTitle;
	        			body = mOriginalBody;
	        			Long time = mOriginalCreatedTime;
	        			mDb.updateNote(rowId, title, pictureUri, audioUri, drawingUri, body, mOriginalMarking, time);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_common / saveState / update new");
	        			mDb.updateNote(rowId, title, pictureUri, audioUri, drawingUri, body, mOriginalMarking, now.getTime()); // update note
	        		}
	        		mCurrentPictureUri = pictureUri; // update file name
	        		mCurrentAudioUri = audioUri; // update file name
	        	}
	        	else if( Util.isEmptyString(title) && Util.isEmptyString(body) &&
			        	 Util.isEmptyString(pictureUri) && Util.isEmptyString(audioUri) )
	        	{
	        		// delete
	        		System.out.println("Note_common / saveState / delete");
	        		mDb.deleteNote(rowId);
	        	}
	        }
        }
        mDb.close();
        
		return rowId;
	}

	public static Long savePictureStateInDB(Long rowId,boolean enSaveDb, String pictureUri, String audioUri, String drawingUri) 
	{
		boolean mEnSaveDb = enSaveDb;
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        if(mEnSaveDb)
        {
	        if (rowId == null) // for Add new
	        {
	        	if( !pictureUri.isEmpty())
	        	{
	        		// insert
	        		System.out.println("Note_common / saveState / insert");
	        		rowId = mDb.insertNote("", pictureUri, audioUri, drawingUri, "", 1, (long) 0);// add new note, get return row Id
	        	}
        		mCurrentPictureUri = pictureUri; // update file name
	        } 
	        else // for Edit
	        {
    	        Date now = new Date(); 
	        	if( !pictureUri.isEmpty())
	        	{
	        		// update
	        		if(bRollBackData) //roll back
	        		{
			        	System.out.println("Note_common / saveState / update: roll back");
	        			Long time = mOriginalCreatedTime;
	        			mDb.updateNote(rowId, "", pictureUri, audioUri, drawingUri, "", mOriginalMarking, time);
	        		}
	        		else // update new
	        		{
	        			System.out.println("Note_common / saveState / update new");
	        			mDb.updateNote(rowId, "", pictureUri, audioUri, drawingUri, "", 1, now.getTime()); // update note
	        		}
	        		mCurrentPictureUri = pictureUri; // update file name
	        	}
	        	else if(pictureUri.isEmpty())
	        	{
	        		// delete
	        		System.out.println("Note_common / saveState / delete");
	        		mDb.deleteNote(rowId);
	        	}
	        }
        }
        mDb.close();
        
		return rowId;
	}
	
	// for confirmation condition
	public void removePictureStringFromOriginalNote(Long rowId) {
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	mDb.updateNote(rowId, 
    				   mOriginalTitle,
    				   "", 
    				   mOriginalAudioUri,
    				   mOriginalDrawingUri,
    				   mOriginalBody,
    				   mOriginalMarking,
    				   mOriginalCreatedTime );
        mDb.close();
	}
	
	public void removePictureStringFromCurrentEditNote(Long rowId) {
        String title = mTitleEditText.getText().toString();
        String body = mBodyEditText.getText().toString();
        
        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	mDb.updateNote(rowId, 
    				   title,
    				   "", 
    				   mOriginalAudioUri,
    				   mOriginalDrawingUri,
    				   body,
    				   mOriginalMarking,
    				   mOriginalCreatedTime );
        mDb.close();
	}
	
	public void removeAudioStringFromOriginalNote(Long rowId) {
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	mDb.updateNote(rowId, 
    				   mOriginalTitle,
    				   mOriginalPictureUri, 
    				   "",
    				   mOriginalDrawingUri,
    				   mOriginalBody,
    				   mOriginalMarking,
    				   mOriginalCreatedTime );
        mDb.close();
	}	
	
	public static void removeAudioStringFromCurrentEditNote(Long rowId) {
        String title = mTitleEditText.getText().toString();
        String body = mBodyEditText.getText().toString();
        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        mDb.updateNote(rowId, 
    				   title,
    				   mOriginalPictureUri, 
    				   "",
    				   mOriginalDrawingUri,
    				   body,
    				   mOriginalMarking,
    				   mOriginalCreatedTime );
        mDb.close();
	}	
	
	static int getCount()
	{
    	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		int noteCount = mDb.getNotesCount();
		mDb.doClose();
		return noteCount;
	}
	
	// for audio
	public static Long insertAudioToDB(String audioUri) 
	{
		Long rowId = null;
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	if( !audioUri.isEmpty())
    	{
    		// insert
    		System.out.println("Note_common / insertAudioToDB / insert");
    		// set marking to 1 for default
    		rowId = mDb.insertNote("", "", audioUri, "", "", 1, (long) 0);// add new note, get return row Id
    	}
        mDb.close();
		return rowId;
	}
	
}