package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AlignmentSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Note_view_pager extends FragmentActivity //UilBaseFragment 
{
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    static ViewPager mPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    // DB
    DB mDb;
    Long mRowId;
    int mEntryPosition;
    int mCurrentPosition;
    int EDIT_CURRENT_VIEW = 5;
    int MAIL_CURRENT_VIEW = 6;
    int mStyle;
    
    SharedPreferences mPref_show_note_attribute;
    
    Button editButton;
    Button sendButton;
    Button backButton;
    
    String mAudioUriInDB;
    static TextView mAudioTextView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        System.out.println("Note_view_pager / onCreate");
        setContentView(R.layout.note_view_slide);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.view_note_title);
		
		// text view for audio
        mAudioTextView = (TextView) findViewById(R.id.view_audio);
        
		UilCommon.init();
        
        // DB
		String strFinalPageViewed_tableId = Util.getPref_lastTimeView_NotesTableId(Note_view_pager.this);
        DB.setNotes_TableId(strFinalPageViewed_tableId);
        mDb = new DB(Note_view_pager.this);
        
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        
        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        
        // set current selection
        mEntryPosition = getIntent().getExtras().getInt("POSITION");
        System.out.println("onCreate / mEntryPosition = " + mCurrentPosition);
        
        // init
   		if(savedInstanceState != null)
   			mCurrentPosition = savedInstanceState.getInt("currentPosition");
   		else if (savedInstanceState == null)
   	        mCurrentPosition = mEntryPosition;

        mPager.setCurrentItem(mCurrentPosition);
        
        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        mRowId = mDb.getNoteId(mCurrentPosition);
        mStyle = mDb.getTabStyle(TabsHostFragment.mCurrentTabIndex);
        mDb.doClose();
        
        // show audio name
        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		mAudioUriInDB = mDb.getNoteAudioUriById(mRowId);        
    	mDb.doClose();
   		showAudioName(); // on Create

        // Note: if mPager.getCurrentItem() is not equal to mEntryPosition, _onPageSelected will
        //       be called again after rotation
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() 
        {
            @Override
            public void onPageSelected(int nextPosition) 
            {
            	if((nextPosition == mCurrentPosition+1) || (nextPosition == mCurrentPosition-1))
            	{
                    mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
                    mRowId = mDb.getNoteId(nextPosition);
            		mAudioUriInDB = mDb.getNoteAudioUriById(mRowId);
            		mDb.doClose();
            		// show audio
           			showAudioName();
            		
            		// stop audio media player and audio handler when not Rotate
           			if(AudioPlayer.mIsOneTimeMode)
           				Util.stopAudioPlayer();
            		
	            	mCurrentPosition = mPager.getCurrentItem();
	            	
	            	if(AudioPlayer.mIsOneTimeMode)
	            		AudioPlayer.audioIndex = mCurrentPosition;//update Audio index
            		
	            	showImageControlButtons(mCurrentPosition);
	            	
            		// show web view
	            	String tagStr = "current"+nextPosition+"webView";
	            	CustomWebView customWebView = (CustomWebView) mPager.findViewWithTag(tagStr);
	            	if (customWebView != null )
	            	{
	                    int defaultScale = pref_web_view.getInt("KEY_WEB_VIEW_SCALE",0);

	                    customWebView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
	                	customWebView.getSettings().setLoadWithOverviewMode(true);
	                	customWebView.getSettings().setBuiltInZoomControls(true);
	                	customWebView.getSettings().setUseWideViewPort(false);
	                	customWebView.getSettings().setSupportZoom(true);
	                	customWebView.setInitialScale(defaultScale);
//	                	  customWebView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);//240dpi
//	                	  customWebView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
	                	
	                    // load empty data to fix double width issue
	                    customWebView.loadData("","text/html; charset=utf-8", "UTF-8");
	                	customWebView.loadData(getHTMLstring(nextPosition), "text/html; charset=utf-8", "UTF-8");
	            	}
            	}
            	
            	
            	// When changing pages, reset the action bar actions since they are dependent
                // on which page is currently active. An alternative approach is to have each
                // fragment expose actions itself (rather than the activity exposing actions),
                // but for simplicity, the activity provides the actions in this sample.
                invalidateOptionsMenu();//The onCreateOptionsMenu(Menu) method will be called the next time it needs to be displayed.
            }
        }); //mPager.setOnPageChangeListener
        
		// edit note button
        editButton = (Button) findViewById(R.id.view_edit);
        editButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
        editButton.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View view) 
            {
		        Intent intent = new Intent(Note_view_pager.this, Note_edit.class);
				mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		        intent.putExtra(DB.KEY_NOTE_ID, mRowId);
		        intent.putExtra(DB.KEY_NOTE_TITLE, mDb.getNoteTitleById(mRowId));
		        intent.putExtra(DB.KEY_NOTE_AUDIO_URI , mDb.getNoteAudioUriById(mRowId));
		        intent.putExtra(DB.KEY_NOTE_PICTURE_URI , mDb.getNotePictureUriById(mRowId));
		        intent.putExtra(DB.KEY_NOTE_BODY, mDb.getNoteBodyById(mRowId));
		        intent.putExtra(DB.KEY_NOTE_CREATED, mDb.getNoteCreatedTimeById(mRowId));
				mDb.doClose();
		        startActivityForResult(intent, EDIT_CURRENT_VIEW);
            }
        });        
        
        // send note button
        sendButton = (Button) findViewById(R.id.view_send);
        sendButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_send, 0, 0, 0);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                // set Sent string Id
				List<Long> rowArr = new ArrayList<Long>();
				rowArr.add(0,mRowId);
                // mail
				Intent intent = new Intent(Note_view_pager.this, SendMailAct.class);
		        String extraStr = Util.getStringWithXmlTag(rowArr);
		        extraStr = Util.addXmlTag(extraStr);
		        intent.putExtra("SentString", extraStr);
		        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		        String picFile = mDb.getNotePictureUriById(mRowId);
				System.out.println("-> picFile = " + picFile);
				mDb.doClose();
				if( (picFile != null) && 
				 	(picFile.length() > 0) )
				{
					String picFileArray[] = new String[]{picFile};
			        intent.putExtra("SentPictureFileNameArray", picFileArray);
				}
				startActivityForResult(intent, MAIL_CURRENT_VIEW);
            }
        });
        
        // back button
        backButton = (Button) findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
        backButton.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View view) {
//            	setIntentResultForAudioIndex();
        		if(AudioPlayer.mIsOneTimeMode)
        			Util.stopAudioPlayer();            	
                finish();
            }
        });

        //To show buttons or not depends on display mode
    	mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  			.equalsIgnoreCase("PICTURE_AND_TEXT"))
	  	{
//	  		showToast(R.string.view_note_mode_all);
	  		editButton.setVisibility(View.VISIBLE);
	  	    sendButton.setVisibility(View.VISIBLE);
	  	    backButton.setVisibility(View.VISIBLE);
	  	    mAudioTextView.setVisibility(View.VISIBLE);
	  	}
	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  			.equalsIgnoreCase("PICTURE_ONLY"))
	  	{	
//	  		showToast(R.string.view_note_mode_picture);
	  		editButton.setVisibility(View.GONE);
	  	    sendButton.setVisibility(View.GONE);
	  	    backButton.setVisibility(View.GONE);
	  	    mAudioTextView.setVisibility(View.GONE);
	  	}
	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  			.equalsIgnoreCase("TEXT_ONLY"))
	  	{
//	  		showToast(R.string.view_note_mode_text);
	  		editButton.setVisibility(View.VISIBLE);
	  	    sendButton.setVisibility(View.VISIBLE);
	  	    backButton.setVisibility(View.VISIBLE);
	  	    mAudioTextView.setVisibility(View.VISIBLE);
	  	}
        
    } //onCreate end
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		System.out.println("Note_view_pager / onActivityResult ");
        if((requestCode==EDIT_CURRENT_VIEW) || (requestCode==MAIL_CURRENT_VIEW))
        {
//        	setIntentResultForAudioIndex();
    		if(AudioPlayer.mIsOneTimeMode)
    			Util.stopAudioPlayer(); 
        	finish();
        }
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Note_view_pager / onDestroy");
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("currentPosition", mCurrentPosition);
	};
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	};
	
	// On Create Options Menu
    Menu mMenu;
    int mSubMenu0Id;
    public static MenuItem mMenuItemAudio;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
    	mMenu = menu;
        // update row Id
        mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        mRowId = mDb.getNoteId(mPager.getCurrentItem());
        mDb.doClose();

        // menu item: for audio play
		MenuItem itemAudio = menu.add(0, R.id.AUDIO_IN_VIEW , 0, "Audio" )
 								 .setIcon(R.drawable.ic_lock_ringer_on);
		itemAudio.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mMenuItemAudio = itemAudio;
		
        if(currentNoteHasAudioUri())
        	mMenuItemAudio.setVisible(true);
        else
        	mMenuItemAudio.setVisible(false);
		
		if((AudioPlayer.mediaPlayer != null)&&
		   (AudioPlayer.mIsOneTimeMode == true) &&
		    mMenuItemAudio.isVisible())
		{
			// show playing state
			if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
				mMenuItemAudio.setIcon(R.drawable.ic_audio_selected); //highlight
			else if( (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) ||
					 (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_STOP)    ) 
				mMenuItemAudio.setIcon(R.drawable.ic_lock_ringer_on);
		}
		
        // menu item: with sub menu for View note mode selection
        SubMenu subMenu0 = menu.addSubMenu(0, R.id.action_mode, 1, R.string.view_note_mode);
	    MenuItem subMenuItem0 = subMenu0.getItem();
	    mSubMenu0Id = subMenuItem0.getItemId();
	    // set icon
	    subMenuItem0.setIcon(android.R.drawable.ic_menu_view);
	    // set sub menu display
		subMenuItem0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

	    // sub menu item list
        // picture and text
	    MenuItem subItem = subMenu0.add(0, R.id.VIEW_ALL, 1, R.string.view_note_mode_all);
        subItem.setIcon(R.drawable.btn_check_on_holo_dark);
   		markCurrentSelected(subItem,"PICTURE_AND_TEXT");
        // picture only		
        subItem = subMenu0.add(0, R.id.VIEW_PICTURE, 2, R.string.view_note_mode_picture);
        markCurrentSelected(subItem,"PICTURE_ONLY");		
        // text only		
	    subItem = subMenu0.add(0, R.id.VIEW_TEXT, 3, R.string.view_note_mode_text);
	    markCurrentSelected(subItem,"TEXT_ONLY");
	    
	    // menu item: previous
		MenuItem itemPrev = menu.add(0, R.id.action_previous, 2, R.string.slide_action_previous )
				 				.setIcon(R.drawable.ic_media_previous);
	    itemPrev.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		itemPrev.setEnabled(mPager.getCurrentItem() > 0);
		itemPrev.getIcon().setAlpha(mPager.getCurrentItem() > 0?255:30);
		
		// menu item: next or finish
        // Add either a "next" or "finish" button to the action bar, depending on which page is currently selected.
		MenuItem itemNext = menu.add(0, R.id.action_next, 3, 
                					(mPager.getCurrentItem() == mPagerAdapter.getCount() - 1)
                							? R.string.slide_action_finish
                							: R.string.slide_action_next)
                				.setIcon(R.drawable.ic_media_next);
        itemNext.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        
        // set Gray for Last item
        if(mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1))
        	itemNext.setEnabled( false );

        itemNext.getIcon().setAlpha(mPager.getCurrentItem() == (mPagerAdapter.getCount() - 1)?30:255);
        //??? why affect image button, workaround: one uses local, one uses system
        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// called after _onCreateOptionsMenu
        return true;
    }  
    
    // Note: No need to keep AudioPlayer.audioIndex for NOT one-time-mode
//    void setIntentResultForAudioIndex()
//    {
//    	Intent intent = getIntent();
//    	System.out.println("audioIndexOriginal = " + getIntent().getExtras().getInt("audioIndexOriginal"));
//        // put Extra for list view audio index
//		intent.putExtra("audioIndexBack",getIntent().getExtras().getInt("audioIndexOriginal"));?
//		setResult(Activity.RESULT_OK, intent);
//    }
    
    // for menu buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
        		if(AudioPlayer.mIsOneTimeMode)
        			Util.stopAudioPlayer();            	
            	finish();
                return true;

            case R.id.AUDIO_IN_VIEW:
            	TabsHostFragment.setPlayingTab_WithHighlight(false);// in case playing audio in pager
            	playAudioInPager();
        		return true;                
                
            case R.id.VIEW_ALL:
        		mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_MODE","PICTURE_AND_TEXT").commit();
        		showSelectedView();
            	return true;
            	
            case R.id.VIEW_PICTURE:
        		mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_MODE","PICTURE_ONLY").commit();
        		showSelectedView();
            	return true;

            case R.id.VIEW_TEXT:
        		mPref_show_note_attribute.edit().putString("KEY_VIEW_NOTE_MODE","TEXT_ONLY").commit();
        		showSelectedView();
            	return true;
            	
            case R.id.action_previous:
                // Go to the previous step in the wizard. If there is no previous step,
                // setCurrentItem will do nothing.
            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
                return true;

            case R.id.action_next:
                // Advance to the next step in the wizard. If there is no next step, setCurrentItem
                // will do nothing.
            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    // on back pressed
    @Override
    public void onBackPressed() {
		if(AudioPlayer.mIsOneTimeMode)
			Util.stopAudioPlayer();    	
        finish();
    }
    
    
    // check if current note has audio Uri
    boolean currentNoteHasAudioUri()
    {
		boolean isAudioUri = false;
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		if(mDb.getNoteAudioUri(mCurrentPosition) != null)
			isAudioUri = ((mDb.getNoteAudioUri(mCurrentPosition).length() > 0))?true:false;
		mDb.doClose();
		return isAudioUri;
    }
    
    
    void playAudioInPager()
    {
		if(currentNoteHasAudioUri())
		{
    		AudioPlayer.audioIndex = mCurrentPosition;
    		// new instance
    		if(AudioPlayer.mediaPlayer == null)
    		{   
        		int lastTimeView_NotesTblId =  Integer.valueOf(Util.getPref_lastTimeView_NotesTableId(this));
    			DrawerActivity.mCurrentPlaying_NotesTblId = lastTimeView_NotesTblId;
        		AudioPlayer.mIsOneTimeMode = true; 
    		}
    		// If Audio player is NOT at One time mode and media exists
    		else if((AudioPlayer.mediaPlayer != null) && 
    				(AudioPlayer.mIsOneTimeMode == false))
    		{
        		AudioPlayer.mIsOneTimeMode = true;
       			Util.stopAudioPlayer();
    		}
    		
			Util.startAudioPlayer(this);
			
    		// update playing state
    		if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
    			mMenuItemAudio.setIcon(R.drawable.ic_audio_selected); //highlight
    		else if( (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) ||
    				 (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_STOP)    ) 
    			mMenuItemAudio.setIcon(R.drawable.ic_lock_ringer_on); // no highlight

    		// update playing state of picture mode
    		showImageControlButtons(mCurrentPosition);
		}    	
    }
    
    // Mark current selected 
    void markCurrentSelected(MenuItem subItem, String str)
    {
        if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT").equalsIgnoreCase(str))
        	subItem.setIcon(R.drawable.btn_radio_on_holo_dark);
	  	else
        	subItem.setIcon(R.drawable.btn_radio_off_holo_dark);
    }    
    
    // show audio name
    void showAudioName()
    {
        String audio_name = "";
    	if(!Util.isEmptyString(mAudioUriInDB))
		{
			Uri audioUri = Uri.parse(mAudioUriInDB);
			audio_name = getResources().getText(R.string.note_audio) +
						 ": " + 
						 Util.getDisplayNameByUri(audioUri,this);
		}        
		System.out.println("Note_view_pager / showAudioName() = " + audio_name);
		mAudioTextView.setText(audio_name);
    }
    
    // Show selected view
    void showSelectedView()
    {
    	finish();
	  	
		Intent intent;
		intent = new Intent( this, Note_view_pager.class);
        intent.putExtra("POSITION", mPager.getCurrentItem());
        startActivity(intent);
    }
    
//    public void showToast (int strId){
//    	String st = getResources().getText(strId).toString();
//        try{ toast.getView().isShown();     // true if visible
//            toast.setText(st);
//        } catch (Exception e) {         // invisible if exception
//            toast = Toast.makeText(Note_view_pager.this, st, Toast.LENGTH_SHORT);
//            }
//        toast.show();  //finally display it
//    }
    
    /**
     * A simple pager adapter 
     */
    CustomWebView customWebView;
    SharedPreferences pref_web_view;
    
    private class ScreenSlidePagerAdapter extends PagerAdapter 
    {
		private LayoutInflater inflater;
		 
        public ScreenSlidePagerAdapter(FragmentManager fm) 
        {
            inflater = getLayoutInflater();
        }
        
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
			object = null;
		}

        @Override
		public Object instantiateItem(ViewGroup container, final int position) 
        {
//        	System.out.println("Note_view_pager / instantiateItem / position = " + position);

        	// Inflate the layout containing 
        	// 1. text group: title, body, time 
        	// 2. image group: picture, control buttons
        	View pagerView = (ViewGroup) inflater.inflate(R.layout.note_view_slide_pager, container, false);
            pagerView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
            
        	// text group
            ViewGroup textGroup = (ViewGroup) pagerView.findViewById(R.id.textGroup);
            customWebView = new CustomWebView(Note_view_pager.this);
            customWebView = ((CustomWebView) textGroup.findViewById(R.id.textBody));

            // set tag for custom web view
            String tagStr = "current"+position+"webView";
            customWebView.setTag(tagStr);
            
            pref_web_view = getSharedPreferences("web_view", 0);
            customWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onScaleChanged(WebView web_view, float oldScale, float newScale) 
                {
                    super.onScaleChanged(web_view, oldScale, newScale);
                    int newDefaultScale = (int) (newScale*100);
                    pref_web_view.edit().putInt("KEY_WEB_VIEW_SCALE",newDefaultScale).commit();
//                    System.out.println("onScaleChanged / mPager.getCurrentItem() " + mPager.getCurrentItem());
                    //update current position
                    mCurrentPosition = mPager.getCurrentItem();
                }
            });
            
            int defaultScale = pref_web_view.getInt("KEY_WEB_VIEW_SCALE",0);
//          System.out.println("instantiateItem / defaultScale = " + defaultScale);
            customWebView.mPref_web_view = pref_web_view;
            
        	customWebView.setBackgroundColor(Util.mBG_ColorArray[mStyle]);
            customWebView.getSettings().setLoadWithOverviewMode(true);
        	customWebView.getSettings().setBuiltInZoomControls(true);
        	customWebView.getSettings().setUseWideViewPort(false);
        	customWebView.getSettings().setSupportZoom(true);
            customWebView.setInitialScale(defaultScale); 
//        	  webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);//240dpi        	
//            webView.getSettings().setRenderPriority(RenderPriority.HIGH);
//            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

            // load empty data to fix double width issue
            customWebView.loadData("","text/html; charset=utf-8", "UTF-8");
        	customWebView.loadData(getHTMLstring(position), "text/html; charset=utf-8", "UTF-8");
        	// image group
            ViewGroup imageGroup = (ViewGroup) pagerView.findViewById(R.id.imageContent);
            String tagImageStr = "current"+ position +"imageView";
            imageGroup.setTag(tagImageStr);
            
            Button imageViewBackButton = (Button) (imageGroup.findViewById(R.id.image_view_back));
            Button imageViewAudioButton = (Button) (imageGroup.findViewById(R.id.image_view_audio));
            Button imageViewModeButton = (Button) (imageGroup.findViewById(R.id.image_view_mode));
            Button imagePreviousButton = (Button) (imageGroup.findViewById(R.id.image_previous));
            Button imageNextButton = (Button) (imageGroup.findViewById(R.id.image_next));
        	
        	TouchImageView pictureView = new TouchImageView(container.getContext());
			pictureView = ((TouchImageView) pagerView.findViewById(R.id.img_picture));

			// spinner
			final ProgressBar spinner = (ProgressBar) pagerView.findViewById(R.id.loading);

			// get picture name
        	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        	String strPicture = mDb.getNotePictureUri(position);
        	mDb.doClose();

            // view mode 
    	  	if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
    	  			.equalsIgnoreCase("PICTURE_ONLY"))
    	  	{
    	  	    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
	    					 		 WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	  		getActionBar().hide();
    	  		
    			// picture only
    	  		textGroup.setVisibility(View.GONE);
    	  		imageGroup.setVisibility(View.VISIBLE);
    	  		
    			showPictureByTouchImageView(spinner, pictureView, strPicture);
    			
    			// image: view back
    			imageViewBackButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_revert, 0, 0, 0);
    			// click to finish Note_view_pager
    			imageViewBackButton.setOnClickListener(new View.OnClickListener() {
    	            public void onClick(View view) {
//    	            	setIntentResultForAudioIndex();
    	        		if(AudioPlayer.mIsOneTimeMode)
    	        			Util.stopAudioPlayer();    	            	
    	            	finish();
    	            }
    	        });   
    			
    			// click to play audio 
				imageViewAudioButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_ringer_on, 0, 0, 0);
    			imageViewAudioButton.setOnClickListener(new View.OnClickListener() {

    	            public void onClick(View view) {
    	            	TabsHostFragment.setPlayingTab_WithHighlight(false);// in case playing audio in pager
    	            	playAudioInPager();
    	            }
    	        });       			
    			
    			// image: view mode
    			imageViewModeButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_view, 0, 0, 0);
    			// click to select view mode 
    			imageViewModeButton.setOnClickListener(new View.OnClickListener() {

    	            public void onClick(View view) {
	            		mMenu.performIdentifierAction(R.id.action_mode, 0);
    	            }
    	        });       			
    			
    			// image: previous button
    	        imagePreviousButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_previous, 0, 0, 0);
    			// click to previous 
    	        imagePreviousButton.setOnClickListener(new View.OnClickListener() 
    	        {
    	            public void onClick(View view) {
    	            	mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    	            }
    	        });   
    	        
    			// image: next button
    	        imageNextButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_next, 0, 0, 0);
    			// click to next 
    	        imageNextButton.setOnClickListener(new View.OnClickListener()
    	        {
    	            public void onClick(View view) {
    	            	mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    	            }
    	        }); 
    	        
    	  	}
    	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
    	  			.equalsIgnoreCase("TEXT_ONLY"))
    	  	{
    	  		// text only
    	  		textGroup.setVisibility(View.VISIBLE);
    	  		imageGroup.setVisibility(View.GONE);
    	  	}
    	  	else if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
    	  			.equalsIgnoreCase("PICTURE_AND_TEXT"))
    	  	{
    	  		// picture and text
    	  		textGroup.setVisibility(View.VISIBLE);
    	  		imageGroup.setVisibility(View.VISIBLE);
    	  		imagePreviousButton.setVisibility(View.GONE);
    	  		imageNextButton.setVisibility(View.GONE);
    	  		imageViewAudioButton.setVisibility(View.GONE);
    	  		imageViewModeButton.setVisibility(View.GONE);
    	  		
    			showPictureByTouchImageView(spinner, pictureView, strPicture);
    	  	}
            
        	container.addView(pagerView, 0);
        	
        	// set here since pagerView is just added 
        	showImageControlButtons(position); 
        	
			return pagerView;			
        } //instantiateItem

        // show picture or not
        private void showPictureByTouchImageView(final View spinner, TouchImageView pictureView, String strPicture) 
        {
        	if(strPicture.isEmpty())
        	{
        		pictureView.setImageResource(mStyle%2 == 1 ?
		    			R.drawable.btn_radio_off_holo_light:
		    			R.drawable.btn_radio_off_holo_dark);//R.drawable.ic_empty);
        	}
        	else
        	{
        		Uri imageUri = Uri.parse(strPicture);
        		
        		if(imageUri.isAbsolute())
        			UilCommon.imageLoader.displayImage(imageUri.toString(), 
										 	 pictureView,
										 	UilCommon.optionsForFadeIn,
										 	 new SimpleImageLoadingListener()
        		{
					@Override
					public void onLoadingStarted(String imageUri, View view) 
					{

						// make spinner appears at center
						LinearLayout.LayoutParams paramsSpinner = (LinearLayout.LayoutParams) spinner.getLayoutParams();
//						paramsSpinner.weight = (float) 1.0;
						paramsSpinner.weight = (float) 1000.0; //??? still see garbage at left top corner
						spinner.setLayoutParams(paramsSpinner);
						spinner.setVisibility(View.VISIBLE);
						view.setVisibility(View.GONE);
					}
	
					@Override
					public void onLoadingFailed(String imageUri, View view, FailReason failReason) 
					{
						String message = null;
						switch (failReason.getType()) 
						{
							case IO_ERROR:
								message = "Input/Output error";
								break;
							case DECODING_ERROR:
								message = "Image can't be decoded";
								break;
							case NETWORK_DENIED:
								message = "Downloads are denied";
								break;
							case OUT_OF_MEMORY:
								message = "Out Of Memory error";
								break;
							case UNKNOWN:
								message = "Unknown error";
								break;
						}
						Toast.makeText(Note_view_pager.this, message, Toast.LENGTH_SHORT).show();
						spinner.setVisibility(View.GONE);
						view.setVisibility(View.GONE);
					}
	
					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
					{
						spinner.setVisibility(View.GONE);
						view.setVisibility(View.VISIBLE);
					}
				});
        	}
		}
        
		@Override
        public int getCount() 
        {
        	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        	int count = mDb.getNotesCount();
        	mDb.doClose();
        	return count;
        }

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}
    }
    
    boolean isPictureMode()
    {
    	mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
	  	if(mPref_show_note_attribute.getString("KEY_VIEW_NOTE_MODE", "PICTURE_AND_TEXT")
	  	   	  			.equalsIgnoreCase("PICTURE_ONLY"))
	  		return true;
	  	else
	  		return false;
    }
    
    
    static ViewGroup imageGroup;
    static Button mImageViewAudioButton;
    void showImageControlButtons(int position)
    {
		if(isPictureMode())
	  	{
	        String tagImageStr = "current"+ position +"imageView";
	
	        System.out.println("tagImageStr = " + tagImageStr);
	        imageGroup = (ViewGroup) mPager.findViewWithTag(tagImageStr);
	        
	        if(imageGroup != null)
	        {
		        Button imageViewBackButton = (Button) (imageGroup.findViewById(R.id.image_view_back));
		        mImageViewAudioButton = (Button) (imageGroup.findViewById(R.id.image_view_audio));
		        Button imageViewModeButton = (Button) (imageGroup.findViewById(R.id.image_view_mode));
		        Button imagePreviousButton = (Button) (imageGroup.findViewById(R.id.image_previous));
		        Button imageNextButton = (Button) (imageGroup.findViewById(R.id.image_next));
		
		        imageViewBackButton.setVisibility(View.VISIBLE);
		        
		        
		        // audio playing state for one time mode
		        if(AudioPlayer.mIsOneTimeMode && (position == AudioPlayer.audioIndex))
		        {
	        		if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
	        			mImageViewAudioButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_audio_selected, 0, 0, 0);
	        		else if((AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) ||
	        				(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_STOP)    )
	        			mImageViewAudioButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_ringer_on, 0, 0, 0);
		        }
		        
		        if(currentNoteHasAudioUri())
		        	mImageViewAudioButton.setVisibility(View.VISIBLE);
		        else
		        	mImageViewAudioButton.setVisibility(View.GONE);
		        
		    	imageViewModeButton.setVisibility(View.VISIBLE);
				imagePreviousButton.setVisibility(View.VISIBLE);
		        imageNextButton.setVisibility(View.VISIBLE);
	        
		        imagePreviousButton.setEnabled(position==0? false:true);
		        imagePreviousButton.setAlpha(position==0? 0.1f:1f);

		        imageNextButton.setEnabled(position == (mPagerAdapter.getCount()-1 )? false:true);
		        imageNextButton.setAlpha(position == (mPagerAdapter.getCount()-1 )? 0.1f:1f);
		        
		    	new Note_view_pager_buttons_controller(Note_view_pager.this, 
    		            System.currentTimeMillis() + 1000 * 10); // for 10 seconds
//	            System.currentTimeMillis() + 1000 * 3); // for 3 seconds (for test)
	        }
	  	}
	  	else 
	  		return;
    }
    
    String getHTMLstring(int position)
    {
    	mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	String strTitle = mDb.getNoteTitle(position);
    	String strBody = mDb.getNoteBody(position);
    	Long createTime = mDb.getNoteCreatedTime(position);
    	mDb.doClose();
    	String head = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"+
    		       	  "<html><head>"+
    		       	  "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />"+
    		       	  //"<meta name=\"viewport\" content=\"target-densitydpi=device-dpi,width=device-width\">"
     	  			  "<head>";

       	String seperatedLineTitle = (strTitle.length()!=0)?"<hr size=2 color=blue width=99% >":"";
       	String seperatedLineBody = (strBody.length()!=0)?"<hr size=1 color=black width=99% >":"";

       	// title
    	Spannable spanTitle = new SpannableString(strTitle);
    	Linkify.addLinks(spanTitle, Linkify.ALL);
    	spanTitle.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), 
    					  0,
    					  spanTitle.length(), 
    					  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    	strTitle = Html.toHtml(spanTitle);
    	
    	// body
    	Spannable spanBody = new SpannableString(strBody);
    	Linkify.addLinks(spanBody, Linkify.ALL);
    	strBody = Html.toHtml(spanBody);
    	
    	// set web view text color
    	String colorStr = Integer.toHexString(Util.mText_ColorArray[mStyle]);
    	colorStr = colorStr.substring(2);
    	
    	String bgColorStr = Integer.toHexString(Util.mBG_ColorArray[mStyle]);
    	bgColorStr = bgColorStr.substring(2);
    	
    	String content = head + "<body color=\"" + bgColorStr + "\">" +
		         "<p align=\"center\"><b>" + 
				 "<font color=\"" + colorStr + "\">" + strTitle + "</font>" + 
         		 "</b></p>" + seperatedLineTitle + 
		         "<p>" + 
				 "<font color=\"" + colorStr + "\">" + strBody + "</font>" +
				 "</p>" + seperatedLineBody + 
		         "<p align=\"right\">" + 
				 "<font color=\"" + colorStr + "\">"  + Util.getTimeString(createTime) + "</font>" +
		         "</p>" + 
		         "</body></html>";
		return content;
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {

	        case MotionEvent.ACTION_DOWN:
	        case MotionEvent.ACTION_POINTER_DOWN: 
	    	  		showImageControlButtons(mPager.getCurrentItem());
	          break;
	        case MotionEvent.ACTION_MOVE: 
	        case MotionEvent.ACTION_UP:
	        case MotionEvent.ACTION_POINTER_UP:
	        case MotionEvent.ACTION_CANCEL: 
	          break;
        }

        boolean ret = super.dispatchTouchEvent(event);
        return ret;
    }    
}
