package com.cwc.litenote;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

public class Util 
{
    SharedPreferences mPref_vibration;
    Context mContext;
    Activity mActivity;
    String mEMailString;
    private static DB mDb;
    static String NEW_LINE = "\r" + System.getProperty("line.separator");

	static int STYLE_DEFAULT = 1;
    
	static int ACTIVITY_CREATE = 0;
    static int ACTIVITY_VIEW_NOTE = 1;
    static int ACTIVITY_EDIT_NOTE = 2;
    static int PICTURE_CHOOSER = 3;
    static int AUDIO_CHOOSER = 4;
    static int TAKE_PICTURE_ACT = 5;
    
    static boolean DEBUG_MODE = false; 
    static boolean RELEASE_MODE = !DEBUG_MODE;
    //set mode
//    static boolean CODE_MODE = RELEASE_MODE;
    static boolean CODE_MODE = DEBUG_MODE;
    
    int defaultBgClr;
    int defaultTextClr;

    // style
    // 0,2,4,6,8: dark background, 1,3,5,7,9: light background
	static int[] mBG_ColorArray = new int[]{Color.rgb(34,34,34), //#222222
											Color.rgb(255,255,255),
											Color.rgb(38,87,51), //#265733
											Color.rgb(186,249,142),
											Color.rgb(87,38,51),//#572633
											Color.rgb(249,186,142),
											Color.rgb(38,51,87),//#263357
											Color.rgb(142,186,249),
											Color.rgb(87,87,51),//#575733
											Color.rgb(249,249,140)};
	static int[] mText_ColorArray = new int[]{Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0),
											  Color.rgb(255,255,255),
											  Color.rgb(0,0,0)};

    
    public Util(){};
    
	public Util(FragmentActivity activity) {
		mContext = activity;
		mActivity = activity;
	}
	
	public Util(Context context) {
		mContext = context;
	}
	
	// set vibration time
	void vibrate()
	{
		mPref_vibration = mContext.getSharedPreferences("vibration", 0);
    	if(mPref_vibration.getString("KEY_ENABLE_VIBRATION","yes").equalsIgnoreCase("yes"))
    	{
			Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
			if(mPref_vibration.getString("KEY_VIBRATION_TIME","25") != "")
			{
				int vibLen = Integer.valueOf(mPref_vibration.getString("KEY_VIBRATION_TIME","25"));
				mVibrator.vibrate(vibLen); //length unit is milliseconds
				System.out.println("vibration len = " + vibLen);
			}
    	}
	}
	
	// export to SD card: for checked pages
	String exportToSdCard(String filename, List<Boolean> checkedArr,boolean enableToast)
	{   
		//first row text
		String data ="";
		//get data from DB
		if(checkedArr == null)
			data = queryDB(data,null);// all pages
		else
			data = queryDB(data,checkedArr);
		
		// sent data
		data = addXmlTag(data);
		mEMailString = data;
		
		exportToSdCardFile(data,filename);
		
		return mEMailString;
	}
	
	// save to SD card: for NoteView class
	String exportStringToSdCard(String filename, String curString)
	{   
		//sent data
		String data = "";
		data = data.concat(curString);
		
		mEMailString = data;
		
		exportToSdCardFile(data,filename);
		
		return mEMailString;
	}
	
	// Export data to be SD Card file
	void exportToSdCardFile(String data,String filename)
	{
	    // SD card path + "/" + directory path
	    String dirString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + 
	    		              Util.getAppName(mContext);
	    
		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();
		File file = new File(dir, filename);
		file.setReadOnly();
		
//		FileWriter fw = null;
//		try {
//			fw = new FileWriter(file);
//		} catch (IOException e1) {
//			System.out.println("_FileWriter error");
//			e1.printStackTrace();
//		}
//		BufferedWriter bw = new BufferedWriter(fw);
		
		BufferedWriter bw = null;
		OutputStreamWriter osw = null;
		
		int BUFFER_SIZE = 8192;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file.getPath()), "UTF-8");
			bw = new BufferedWriter(osw,BUFFER_SIZE);
			
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		try {
			bw.write(data);
			bw.flush();
			osw.close();
			bw.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
    /**
     * Query current data base
     * @param checkedArr 
     * 
     */
    String queryDB(String data, List<Boolean> checkedArr)
    {
    	String curData = data;
    	
		String strFinalPageViewed_tableId = Util.getPref_lastTimeView_NotesTableId((Activity) mContext);
        DB.setNotes_TableId(strFinalPageViewed_tableId);
    	
    	mDb = new DB(mContext);
    	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	int tabCount = DB.getTabsCount();
    	mDb.doClose();
    	for(int i=0;i<tabCount;i++)
    		
    	{
    		// null: all pages
        	if((checkedArr == null ) || ( checkedArr.get(i) == true  ))
    		{
	        	// set Sent string Id
				List<Long> rowArray = new ArrayList<Long>();
        		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
				DB.setNotes_TableId(String.valueOf(DB.getTab_NotesTableId(i)));
				mDb.doClose();
				
        		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	    		for(int k=0;k<mDb.getNotesCount();k++)
	    		{
    				rowArray.add(k,(long) mDb.getNoteId(k));
	    		}
	    		mDb.doClose();
	    		curData = curData.concat(getStringWithXmlTag(rowArray));
    		}
    	}
    	return curData;
    	
    }
    
    // get current time string
    static String getCurrentTimeString()
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONDAY)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		
//		int hour = cal.get(Calendar.HOUR);//12h 
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		int mSec = cal.get(Calendar.MILLISECOND);
		
		String strTime = year 
				+ "" + String.format(Locale.US,"%02d", month)
				+ "" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "_" + String.format(Locale.US,"%02d", hour)
				+ "" + String.format(Locale.US,"%02d", min)
				+ "" + String.format(Locale.US,"%02d", sec) 
				+ "_" + String.format(Locale.US,"%03d", mSec);
//		System.out.println("time = "+  strTime );
		return strTime;
    }
    
    // get time string
    static String getTimeString(Long time)
    {
		// set time
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
	
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONDAY)+ 1; //month starts from 0
		int date = cal.get(Calendar.DATE);
		int hour = cal.get(Calendar.HOUR_OF_DAY);//24h
//		int hour = cal.get(Calendar.HOUR);//12h 
//		String am_pm = (cal.get(Calendar.AM_PM)== 0) ?"AM":"PM"; // 0 AM, 1 PM
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		
		String strTime = year 
				+ "-" + String.format(Locale.US,"%02d", month)
				+ "-" + String.format(Locale.US,"%02d", date)
//				+ "_" + am_pm
				+ "    " + String.format(Locale.US,"%02d", hour)
				+ ":" + String.format(Locale.US,"%02d", min)
				+ ":" + String.format(Locale.US,"%02d", sec) ;
//		System.out.println("time = "+  strTime );
		
		return strTime;
    }
    
//    void deleteAttachment(String mAttachmentFileName)
//    {
//		// delete file after sending
//		String attachmentPath_FileName = Environment.getExternalStorageDirectory().getPath() + "/" +
//										 mAttachmentFileName;
//		File file = new File(attachmentPath_FileName);
//		boolean deleted = file.delete();
//		if(deleted)
//			System.out.println("delete file is OK");
//		else
//			System.out.println("delete file is NG");
//    }
    
    // add mark to current page
	void addMarkToCurrentPage(DialogInterface dialogInterface)
	{
		mDb = new DB(mActivity);
	    ListView listView = ((AlertDialog) dialogInterface).getListView();
	    final ListAdapter originalAdapter = listView.getAdapter();
	    final int style = Util.getCurrentPageStyle(mActivity);
        CheckedTextView textViewDefault = new CheckedTextView(mActivity) ;
        defaultBgClr = textViewDefault.getDrawingCacheBackgroundColor();
        defaultTextClr = textViewDefault.getCurrentTextColor();

	    listView.setAdapter(new ListAdapter()
	    {
	        @Override
	        public int getCount() {
	            return originalAdapter.getCount();
	        }
	
	        @Override
	        public Object getItem(int id) {
	            return originalAdapter.getItem(id);
	        }
	
	        @Override
	        public long getItemId(int id) {
	            return originalAdapter.getItemId(id);
	        }
	
	        @Override
	        public int getItemViewType(int id) {
	            return originalAdapter.getItemViewType(id);
	        }
	
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {
	            View view = originalAdapter.getView(position, convertView, parent);
	            //set CheckedTextView in order to change button color
	            CheckedTextView textView = (CheckedTextView)view;
	            mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	            if(DB.getTab_NotesTableId(position) == Integer.valueOf(DB.getNotesTableId()))
	            {
		            textView.setTypeface(null, Typeface.BOLD_ITALIC);
		            textView.setBackgroundColor(mBG_ColorArray[style]);
		            textView.setTextColor(mText_ColorArray[style]);
			        if(style%2 == 0)
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
			        else
			        	textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_light);
	            }
	            else
	            {
		        	textView.setTypeface(null, Typeface.NORMAL);
		            textView.setBackgroundColor(defaultBgClr);
		            textView.setTextColor(defaultTextClr);
		            textView.setCheckMarkDrawable(R.drawable.btn_radio_off_holo_dark);
	            }
				mDb.doClose();
	            return view;
	        }

	        @Override
	        public int getViewTypeCount() {
	            return originalAdapter.getViewTypeCount();
	        }

	        @Override
	        public boolean hasStableIds() {
	            return originalAdapter.hasStableIds();
	        }
	
	        @Override
	        public boolean isEmpty() {
	            return originalAdapter.isEmpty();
	        }

	        @Override
	        public void registerDataSetObserver(DataSetObserver observer) {
	            originalAdapter.registerDataSetObserver(observer);
	
	        }
	
	        @Override
	        public void unregisterDataSetObserver(DataSetObserver observer) {
	            originalAdapter.unregisterDataSetObserver(observer);
	
	        }
	
	        @Override
	        public boolean areAllItemsEnabled() {
	            return originalAdapter.areAllItemsEnabled();
	        }
	
	        @Override
	        public boolean isEnabled(int position) {
	            return originalAdapter.isEnabled(position);
	        }
	    });
	}
	
	// get App name
	static public String getAppName(Context context)
	{
		return context.getResources().getString(R.string.app_name);
	}
	
	// get style
	static public int getNewPageStyle(Context context)
	{
		SharedPreferences mPref_style;
		mPref_style = context.getSharedPreferences("style", 0);
		return mPref_style.getInt("KEY_STYLE",STYLE_DEFAULT);
	}
	
	
	// set button color
	static String[] mItemArray = new String[]{"1","2","3","4","5","6","7","8","9","10"};
    public static void setButtonColor(RadioButton rBtn,int iBtnId)
    {
    	if(iBtnId%2 == 0)
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_dark);
    	else
    		rBtn.setButtonDrawable(R.drawable.btn_radio_off_holo_light);
		rBtn.setBackgroundColor(Util.mBG_ColorArray[iBtnId]);
		rBtn.setText(mItemArray[iBtnId]);
		rBtn.setTextColor(Util.mText_ColorArray[iBtnId]);
    }
	
    // get current page style
	static public int getCurrentPageStyle(Context context)
	{
		int style = 0;
		mDb = new DB(context);
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		style = mDb.getTabStyle(TabsHostFragment.mCurrentTabIndex);
		mDb.doClose();
		
		return style;
	}

	// get style count
	static public int getStyleCount()
	{
		return mBG_ColorArray.length;
	}
	

	// set notes table id of last time view
	public static void setPref_lastTimeView_NotesTableId(Activity act, int notesTableId )
	{
	  SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
	  String keyPrefix = "KEY_DRAWER_NUMBER_";
	  String keyName = keyPrefix.concat(String.valueOf(DrawerActivity.mCurrentDrawerIndex+1));
      pref.edit().putInt(keyName, notesTableId).commit();
	}
	
	// get notes table id of last time view
	public static String getPref_lastTimeView_NotesTableId(Context context)
	{
		SharedPreferences pref = context.getSharedPreferences("last_time_view", 0);
		String keyPrefix = "KEY_DRAWER_NUMBER_";
		String keyName = keyPrefix.concat(String.valueOf(DrawerActivity.mCurrentDrawerIndex+1));
		int notesTableId = pref.getInt(keyName, 1); // notes table Id: default is 1
		return String.valueOf(notesTableId);
	}
	
	// set scroll X of drawer of last time view
	public static void setPref_lastTimeView_scrollX_byDrawerNumber(Activity act, int scrollX )
	{
	  SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
	  String keyPrefix = "KEY_DRAWER_NUMBER_";
	  String keyName = keyPrefix.concat(String.valueOf(DrawerActivity.mCurrentDrawerIndex+1));
	  keyName = keyName.concat("_SCROLL_X");
      pref.edit().putInt(keyName, scrollX).commit();
	}
	
	// get scroll X of drawer of last time view
	public static Integer getPref_lastTimeView_scrollX_byDrawerNumber(Activity act)
	{
		SharedPreferences pref = act.getSharedPreferences("last_time_view", 0);
		String keyPrefix = "KEY_DRAWER_NUMBER_";
		String keyName = keyPrefix.concat(String.valueOf(DrawerActivity.mCurrentDrawerIndex+1));
		keyName = keyName.concat("_SCROLL_X");
		int scrollX = pref.getInt(keyName, 0); // default scroll X is 0
		return scrollX;
	}	
	
	// get Send String with XML tag
	static String getStringWithXmlTag(List<Long> rowArray)
	{
        String PAGE_TAG_B = "<page>";
        String TAB_TAG_B = "<tabname>";
        String TAB_TAG_E = "</tabname>";
        String NOTEITEM_TAG_B = "<note>";
        String NOTEITEM_TAG_E = "</note>";
        String TITLE_TAG_B = "<title>";
        String TITLE_TAG_E = "</title>";
        String BODY_TAG_B = "<body>";
        String BODY_TAG_E = "</body>";
        String PICTURE_TAG_B = "<picture>";
        String PICTURE_TAG_E = "</picture>";
        String AUDIO_TAG_B = "<audio>";
        String AUDIO_TAG_E = "</audio>";
        String PAGE_TAG_E = "</page>";
        
        String sentString = NEW_LINE;

    	// when page has tab name only, no notes
    	if(rowArray.size() == 0)
    	{
        	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        	sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
	        sentString = sentString.concat(NEW_LINE + TAB_TAG_B + DB.getCurrentTabTitle() + TAB_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + NOTEITEM_TAG_B);
	    	sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + TITLE_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + BODY_TAG_B +  BODY_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + PICTURE_TAG_B + PICTURE_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + AUDIO_TAG_B + AUDIO_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + NOTEITEM_TAG_E);
	    	sentString = sentString.concat(NEW_LINE + PAGE_TAG_E );
    		sentString = sentString.concat(NEW_LINE);
    		mDb.doClose();
    	}
    	else
    	{
	        for(int i=0;i< rowArray.size();i++)
	        {
	        	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		    	Cursor cursorNote = mDb.queryNote(rowArray.get(i));
		        String strTitleEdit = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_TITLE));
		        strTitleEdit = replaceEscapeCharacter(strTitleEdit);
		        
		        String strBodyEdit = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_BODY));
		        strBodyEdit = replaceEscapeCharacter(strBodyEdit);

		        String strPictureUriStr = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_PICTURE_URI));
		        strPictureUriStr = replaceEscapeCharacter(strPictureUriStr);

		        String strAudioUriStr = cursorNote.getString(
		        		cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_AUDIO_URI));
		        strAudioUriStr = replaceEscapeCharacter(strAudioUriStr);
		        
		        int mark = cursorNote.getInt(cursorNote.getColumnIndexOrThrow(DB.KEY_NOTE_MARKING));
		        String srtMark = (mark == 1)? "[s]":"[n]";
		        
		        if(i==0)
		        {
		        	sentString = sentString.concat(NEW_LINE + PAGE_TAG_B );
		        	sentString = sentString.concat(NEW_LINE + TAB_TAG_B + DB.getCurrentTabTitle() + TAB_TAG_E );
		        }
		        
		        sentString = sentString.concat(NEW_LINE + NOTEITEM_TAG_B); 
		        sentString = sentString.concat(NEW_LINE + TITLE_TAG_B + srtMark + strTitleEdit + TITLE_TAG_E);
		        sentString = sentString.concat(NEW_LINE + BODY_TAG_B + strBodyEdit + BODY_TAG_E);
		        sentString = sentString.concat(NEW_LINE + PICTURE_TAG_B + strPictureUriStr + PICTURE_TAG_E);
		        sentString = sentString.concat(NEW_LINE + AUDIO_TAG_B + strAudioUriStr + AUDIO_TAG_E);
		        sentString = sentString.concat(NEW_LINE + NOTEITEM_TAG_E); 
		        sentString = sentString.concat(NEW_LINE);
		    	if(i==rowArray.size()-1)
		        	sentString = sentString.concat(NEW_LINE +  PAGE_TAG_E);
		    		
		    	mDb.doClose();
	        }
    	}
    	return sentString;
	}

    // replace special character (e.q. amp sign) for avoiding XML paring exception 
	//      &   &amp;
	//      >   &gt;
	//      <   &lt;
	//      '   &apos;
	//      "   &quot;
	static String replaceEscapeCharacter(String str)
	{
        str = str.replaceAll("&", "&amp;");
        str = str.replaceAll(">", "&gt;");
        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll("'", "&apos;");
        str = str.replaceAll("\"", "&quot;");
        return str;
	}
	
	// add XML tag
	public static String addXmlTag(String str)
	{
		String ENCODING = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String XML_TAG_B = NEW_LINE + "<LiteNote>";
        String XML_TAG_E = NEW_LINE + "</LiteNote>";
        
        String data = ENCODING + XML_TAG_B;
        
        data = data.concat(str);
		data = data.concat(XML_TAG_E);
		
		return data;
	}

	// trim XML tag
	public String trimXMLtag(String string) {
		string = string.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>","");
		string = string.replace("<LiteNote>","");
		string = string.replace("<page>","");
		string = string.replace("<tabname>","=== Page: ");
		string = string.replace("</tabname>"," ===");
		string = string.replace("<note>","--- note ---");
		string = string.replace("<title>","Title: ");
		string = string.replace("</title>","");
		string = string.replace("<body>","Body: ");
		string = string.replace("</body>","");
		string = string.replace("<picture>","Picture: ");
		string = string.replace("</picture>","");		
		string = string.replace("<audio>","Audio: ");
		string = string.replace("</audio>","");		
		string = string.replace("[s]","");
		string = string.replace("[n]","");
		string = string.replace("</note>","");
		string = string.replace("</page>"," ");
		string = string.replace("</LiteNote>","");
		string = string.trim();
		return string;
	}
	
	// get display name by URI
	static String getDisplayNameByUri(Uri uri, Activity activity)
	{
//		String audioID = null, title, artist;
		String display_name = "";

        String[] proj = { 
//        	      MediaStore.Audio.Media._ID, 
//                MediaStore.Audio.Media.ARTIST,
//                MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.DATA, 
                	MediaStore.Audio.Media.DISPLAY_NAME,
//                MediaStore.Audio.Media.DURATION
                };
        
                Cursor cursor = activity.getContentResolver().query(uri, proj, null, null, null);

                if((cursor != null) && cursor.moveToFirst()) //reset the cursor
                {
	                int col_index=-1;
	                do
	                {
	//                  col_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
	//                  title = cursor.getString(col_index);
	//                  col_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
	//                  artist = cursor.getString(col_index);
	                  col_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
	                  display_name = cursor.getString(col_index);
	                }while(cursor.moveToNext());
	                cursor.close();
                }
//                System.out.println("artist_name = " + title);
//                System.out.println("artist_band = " + artist);
//                System.out.println("display_name = " + display_name);
                return display_name;
	}
	
	// is URI existed for Activity
	static boolean isUriExisted(String uriString, Activity activity)
	{
		Uri imageUri = Uri.parse(uriString);
		boolean bFileExist = false;
		try
		{
			activity.getContentResolver().openInputStream(imageUri);
			bFileExist = true;
		}
		catch (Exception e) 
		{
	    }	
		
		try
		{
			if(Patterns.WEB_URL.matcher(uriString).matches())//??? URL can check URI string
				bFileExist = true;
		}
		catch (Exception e) 
		{
	    }
		
		return bFileExist;
	}
	
	// is URI existed for Context
	static boolean isUriExisted(String uriString, Context context)
	{
		Uri imageUri = Uri.parse(uriString);
		boolean bFileExist = false;
		try
		{
			context.getContentResolver().openInputStream(imageUri);
			bFileExist = true;
		}
		catch (Exception e) 
		{
	    }				
		
		return bFileExist;
	}
	
	// is Empty string
	static boolean isEmptyString(String str)
	{
		boolean empty = true;
		if( str != null )
		{
			if(str.length() > 0 )
				empty = false;
		}
		return empty;
	}
	
	static AudioPlayer mAudioPlayer;
	// Play audio
	static void startAudioPlayer(FragmentActivity fragAct)
	{
 	   	System.out.println("Util / startAudioPlayerFragment ");
 	   	// if media player is null, set new fragment
		if(AudioPlayer.mediaPlayer == null)
		{
			// prepare audio info when media player is null
			AudioPlayer.prepareAudioInfo(fragAct);
		 	// show toast if Audio file is not found or No selection of audio file
			if( (AudioPlayer.mediaPlayer == null) && 
				(AudioInfo.getAudioFilesSize() == 0) &&
				(AudioPlayer.mIsOneTimeMode == false)        )
			{
				Toast.makeText(fragAct,R.string.audio_file_not_found,Toast.LENGTH_SHORT).show();
			}
			else
			{
				AudioPlayer.playbackTime = 0;
				AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_PLAY;
				mAudioPlayer = new AudioPlayer(fragAct);
			}
		}
		else
		{
			// from play to pause
			if(AudioPlayer.mediaPlayer.isPlaying())
			{
				System.out.println("play -> pause");
				AudioPlayer.mediaPlayer.pause();
				AudioPlayer.audioHandler.removeCallbacks(AudioPlayer.runPlayAudio); 
				AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_PAUSE;
			}
			else // from pause to play
			{
				System.out.println("pause -> play");
				AudioPlayer.mediaPlayer.start();
				AudioPlayer.audioHandler.post(AudioPlayer.runPlayAudio);  
				AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_PLAY;
			}
		}
	} 
	
	// Stop audio media player and audio handler
    static void stopAudioPlayer()
    {
        if(AudioPlayer.mediaPlayer != null)
    	{
			if(AudioPlayer.mediaPlayer.isPlaying())
				AudioPlayer.mediaPlayer.pause();
    		AudioPlayer.mediaPlayer.release();
    		AudioPlayer.mediaPlayer = null;
    		AudioPlayer.audioHandler.removeCallbacks(AudioPlayer.runPlayAudio); 
    		AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;
    		DrawerActivity.mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
    	}
    }
    
    
    // picture gallery
    static File getPicturesDir(Context context)
    {
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DCIM);

        // check if App directory exists
        // SD card path + "/" + directory path
        File appPicturesDir = new File(storageDir + "/" + Util.getAppName(context));
        return appPicturesDir;
    }

}
