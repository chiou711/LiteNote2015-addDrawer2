package com.cwc.litenote;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.FragmentActivity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * This example illustrates a common usage of the DrawerLayout widget
 * in the Android support library.
 * <p/>
 * <p>When a navigation (left) drawer is present, the host activity should detect presses of
 * the action bar's Up affordance as a signal to open and close the navigation drawer. The
 * ActionBarDrawerToggle facilitates this behavior.
 * Items within the drawer should fall into one of two categories:</p>
 * <p/>
 * <ul>
 * <li><strong>View switches</strong>. A view switch follows the same basic policies as
 * list or tab navigation in that a view switch does not create navigation history.
 * This pattern should only be used at the root activity of a task, leaving some form
 * of Up navigation active for activities further down the navigation hierarchy.</li>
 * <li><strong>Selective Up</strong>. The drawer allows the user to choose an alternate
 * parent for Up navigation. This allows a user to jump across an app's navigation
 * hierarchy at will. The application should treat this as it treats Up navigation from
 * a different task, replacing the current task stack using TaskStackBuilder or similar.
 * This is the only form of navigation drawer that should be used outside of the root
 * activity of a task.</li>
 * </ul>
 * <p/>
 * <p>Right side drawers should be used for actions, not navigation. This follows the pattern
 * established by the Action Bar that navigation should be to the left and actions to the right.
 * An action should be an operation performed on the current contents of the window,
 * for example enabling or disabling a data overlay on top of the current content.</p>
 */
public class DrawerActivity extends FragmentActivity 
{
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mAppTitle;
    private static Context mContext;
	static Config mConfigFragment;
	static boolean bEnableConfig;
    static Menu mMenu;
    static DB mDb;
    DrawerInfoAdapter drawerInfoAdapter;
    List<String> mDrawerTitles;
    public static int mCurrentDrawerIndex;
    
    private static SharedPreferences mPref_lastTimeView;
    SharedPreferences mPref_add_new_note_option;
    
	static NoisyAudioStreamReceiver noisyAudioStreamReceiver;
	static IntentFilter intentFilter;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_activity_main);
        
        if(Util.CODE_MODE == Util.RELEASE_MODE) 
        {
        	OutputStream nullDev = new OutputStream() 
            {
                public  void    close() {}
                public  void    flush() {}
                public  void    write(byte[] b) {}
                public  void    write(byte[] b, int off, int len) {}
                public  void    write(int b) {}
            }; 
            System.setOut( new PrintStream(nullDev));
        }
        
        //Log.d below can be disabled by applying proguard
        //1. enable proguard-android-optimize.txt in project.properties
        //2. be sure to use newest version to avoid build error
        //3. add the following in proguard-project.txt
        /*-assumenosideeffects class android.util.Log {
        public static boolean isLoggable(java.lang.String, int);
        public static int v(...);
        public static int i(...);
        public static int w(...);
        public static int d(...);
        public static int e(...);
    	}
        */
        Log.d("test log tag","start app");         
        
        System.out.println("================start application ==================");

        mAppTitle = getTitle();
        
        mDrawerTitles = new ArrayList<String>();

        // get last time drawer number, default drawer number: 1
        mPref_lastTimeView = getSharedPreferences("last_time_view", 0);
        int iDrawerNum = mPref_lastTimeView.getInt("KEY_LAST_TIME_VIEW_DRAWER_NUM", 1);
        System.out.println("iDrawerNum = " + iDrawerNum);

        // note: drawer Id starts from 1, current index starts from 0
        if (savedInstanceState == null)
        {
        	mCurrentDrawerIndex = iDrawerNum - 1;
        	AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;
        	mIsCalledWhilePlayingAudio = false;
        }

		Context context = getApplicationContext();
        mDb = new DB(context);  
        DB.setDrawer_Tabs_TableId(iDrawerNum);
        DB.setNotes_TableId(Util.getPref_lastTimeView_NotesTableId(this));// note: mCurrentDrawerIndex is needed 
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		
		if(mDb.getDrawersCount() == 0)
		{
			String drawerPrefix = "D";
	        for(int i=0;i< DB.DEFAULT_DRAWER_COUNT;i++)
	        {
	        	String drawerTitle = drawerPrefix.concat(String.valueOf(i+1));
	        	mDrawerTitles.add(drawerTitle);
	        	mDb.insertDrawer(i+1, drawerTitle ); 
	        }
		}
		else
		{
	        for(int i=0;i< DB.DEFAULT_DRAWER_COUNT;i++)
	        {
	        	mDrawerTitles.add(""); // init only
	        	mDrawerTitles.set(i, mDb.getDrawerTitle(i)); 
	        }
		}
		mDb.doClose();

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerListView = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        
        // set adapter
        drawerInfoAdapter = new DrawerInfoAdapter(this, mDrawerTitles);       
        mDrawerListView.setAdapter(drawerInfoAdapter);

        // set up click listener
        mDrawerListView.setOnItemClickListener(new DrawerItemClickListener());
        
        // set up long click listener
        mDrawerListView.setOnItemLongClickListener(new DrawerItemLongClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
	                this,                  /* host Activity */
	                mDrawerLayout,         /* DrawerLayout object */
	                R.drawable.ic_drawer,  /* navigation drawer image to replace 'Up' caret */
	                R.string.drawer_open,  /* "open drawer" description for accessibility */
	                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) 
        {
            public void onDrawerClosed(View view) 
            {
//        		System.out.println("mDrawerToggle onDrawerClosed ");
        		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        		mDrawerTitle = mDb.getDrawerTitle(mDrawerListView.getCheckedItemPosition());
        		mDb.doClose();  
                setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) 
            {
//        		System.out.println("mDrawerToggle onDrawerOpened ");
                setTitle(mAppTitle);
                drawerInfoAdapter.notifyDataSetChanged();
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mContext = getBaseContext();
        bEnableConfig = false;
        
        fragmentManager = getSupportFragmentManager();
        
        if(savedInstanceState == null)
        {
       	 	selectDrawerItem(mCurrentDrawerIndex); // create new TabsHostFragment inside
        }
        
        
//		// register an audio stream receiver
		if(noisyAudioStreamReceiver == null)
		{
			noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
			intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY); 
			registerReceiver(noisyAudioStreamReceiver, intentFilter);
		}
    }

    /*
     * Life cycle
     * 
     */
    // for Rotate screen
    @Override
    protected void onSaveInstanceState(Bundle outState) 
    {
//  	   System.out.println("DrawerActivity / onSaveInstanceState");
       outState.putInt("CurrentDrawerIndex",mCurrentDrawerIndex);
       outState.putInt("CurrentPlaying_TabIndex",mCurrentPlaying_TabIndex);
       outState.putInt("CurrentPlaying_DrawerIndex",mCurrentPlaying_DrawerIndex);
       outState.putInt("PlayerState",AudioPlayer.mPlayerState);
       outState.putBoolean("CalledWhilePlayingAudio", mIsCalledWhilePlayingAudio);
       super.onSaveInstanceState(outState);
    }
    
    // for After Rotate
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
    	super.onRestoreInstanceState(savedInstanceState);
//		System.out.println("DrawerActivity / _onRestoreInstanceState ");
    	if(savedInstanceState != null)
    	{
    		mCurrentDrawerIndex = savedInstanceState.getInt("CurrentDrawerIndex");
    		selectDrawerItem(mCurrentDrawerIndex);

    		mCurrentPlaying_TabIndex = savedInstanceState.getInt("CurrentPlaying_TabIndex");
    		mCurrentPlaying_DrawerIndex = savedInstanceState.getInt("CurrentPlaying_DrawerIndex");
    		AudioPlayer.mPlayerState = savedInstanceState.getInt("PlayerState");
//    		System.out.println("DrawerActivity / onRestoreInstanceState / AudioPlayer.mPlayerState = " + AudioPlayer.mPlayerState);
    		mIsCalledWhilePlayingAudio = savedInstanceState.getBoolean("CalledWhilePlayingAudio");
    	}    
    }

    @Override
    protected void onResume() 
    {
//    	System.out.println("DrawerActivity / _onResume"); 

      	// To Registers a listener object to receive notification when incoming call 
     	TelephonyManager telMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
     	if(telMgr != null) 
     	{
     		telMgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
     	}         	 	
        super.onResume();
    }
	
    @Override
    protected void onPostResume() {
//    	System.out.println("DrawerActivity / _onPostResume ");
    	super.onPostResume();
    }    
    
    @Override
    protected void onResumeFragments() {
//    	System.out.println("DrawerActivity / _onResumeFragments ");
    	super.onResumeFragments();
    }
    
	// for finish(), for Rotate screen
    @Override
    protected void onPause() {
        super.onPause();
// 	   System.out.println("DrawerActivity / onPause");
    }

    @Override
    protected void onStop() {
//  	    System.out.println("DrawerActivity / onStop");    
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() 
    {
//    	System.out.println("DrawerActivity / onDestroy");
    	
    	//unregister TelephonyManager listener 
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
		// unregister an audio stream receiver
		if(noisyAudioStreamReceiver != null)
		{
			unregisterReceiver(noisyAudioStreamReceiver);//??? unregister here? 
			noisyAudioStreamReceiver = null;
		}        
        
        super.onDestroy();
    }
    
    
    /*
     * Listeners
     * 
     */
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener 
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
        {
        	System.out.println("DrawerActivity / DrawerItemClickListener");
        	mCurrentDrawerIndex = position;
            selectDrawerItem(position);
            // note: drawer Id starts from 1, current index starts from 0
            mPref_lastTimeView.edit().putInt("KEY_LAST_TIME_VIEW_DRAWER_NUM",mCurrentDrawerIndex+1).commit();
        }
    }

    // select drawer item
    private void selectDrawerItem(int position) 
    {
    	System.out.println("DrawerActivity / _selectDrawerItem");
    	// update selected item and title, then close the drawer
        mDrawerListView.setItemChecked(position, true);
        
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		mDrawerTitle = mDb.getDrawerTitle(position);
		mDb.doClose();        
        mDrawerLayout.closeDrawer(mDrawerListView);    	
        setTitle(mDrawerTitle);
        
        DB.setDrawer_Tabs_TableId(position+1); // position: start from 0, table Id starts from 1
        
        // use Runnable to make sure only one drawer background is seen
        mDrawerLayout.post(new Runnable() 
        {
		        @Override
		        public void run() 
		        {
		                Fragment mTabsHostFragment = new TabsHostFragment();
		            	Bundle args = new Bundle();
		            	args.putInt(TabsHostFragment.DRAWER_NUMBER, mCurrentDrawerIndex);
		            	mTabsHostFragment.setArguments(args);
		            	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		                fragmentTransaction.replace(R.id.content_frame, mTabsHostFragment).commit();
		                fragmentManager.executePendingTransactions();
		        } 
	    });
    }
    
    
    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemLongClickListener implements ListView.OnItemLongClickListener 
    {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) 
        {
        	editDrawerItem(position);
			return true;
        }
    }
    
	void editDrawerItem(final int position)
	{
		// insert when table is empty, activated only for the first time 
		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		final String drawerName = mDb.getDrawerTitle(position);
		mDb.doClose();

		final EditText editText = new EditText(this);
	    editText.setText(drawerName);
	    editText.setSelection(drawerName.length()); // set edit text start position
	    //update tab info
	    Builder builder = new Builder(this);
	    builder.setTitle(R.string.edit_page_tab_title)
	    	.setMessage(R.string.edit_page_tab_message)
	    	.setView(editText)   
	    	.setNegativeButton(R.string.btn_Cancel, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{/*cancel*/}
	    	})
	    	.setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
	    	{   @Override
	    		public void onClick(DialogInterface dialog, int which)
	    		{
	    			// save
	    			mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
	    			int drawerId =  (int) mDb.getDrawerId(position);
	    			int drawerTabInfoTableId =  mDb.getDrawerTabsTableId(position);
					mDb.updateDrawer(drawerId,
							drawerTabInfoTableId,
							editText.getText().toString())
							;
                    mDb.doClose();
                    drawerInfoAdapter.notifyDataSetChanged();
                    setTitle(editText.getText().toString());
	            }
            })	
            .setIcon(android.R.drawable.ic_menu_edit);
	        
        AlertDialog d1 = builder.create();
        d1.show();
        // android.R.id.button1 for positive: save
        ((Button)d1.findViewById(android.R.id.button1))
        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
        
        // android.R.id.button2 for negative: color 
        ((Button)d1.findViewById(android.R.id.button2))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
        
	}
    

    @Override
    public void setTitle(CharSequence title) {
        if(title == null)
        {
        	title = mDrawerTitle;
        	
    		getSupportFragmentManager().popBackStack();
    		mConfigFragment = null;  
    		bEnableConfig = false;
            mDrawerLayout.closeDrawer(mDrawerListView);
			getActionBar().setDisplayShowHomeEnabled(true);
			getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getActionBar().setTitle(title);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        System.out.println("DrawerActivity / onPostCreate");
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        System.out.println("DrawerActivity / onConfigurationChanged");
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        System.out.println("DrawerActivity / onPrepareOptionsMenu");
        // If the navigation drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerListView);
        if(drawerOpen)
        	mMenu.setGroupVisible(0, false); 
        else
            setTitle(mDrawerTitle);
        return super.onPrepareOptionsMenu(menu);
    }

	private static class ViewHolder
	{
		TextView drawerTitle; // refers to ListView item's ImageView
	}
    
	class DrawerInfoAdapter extends ArrayAdapter<String>
	{
		private LayoutInflater inflater;
      
		public DrawerInfoAdapter(Context context, List<String> items)
		{
			super(context, -1, items); // -1 indicates we're customizing view
			inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder viewHolder; // holds references to current item's GUI
         
			// if convertView is null, inflate GUI and create ViewHolder;
			// otherwise, get existing ViewHolder
			if (convertView == null)
			{
				convertView = inflater.inflate(R.layout.drawer_list_item, parent, false);
				// set up ViewHolder for this ListView item
				viewHolder = new ViewHolder();
				viewHolder.drawerTitle = (TextView) convertView.findViewById(android.R.id.text1);
				convertView.setTag(viewHolder); // store as View's tag
			}
			else // get the ViewHolder from the convertView's tag
				viewHolder = (ViewHolder) convertView.getTag();

			// set highlight of selected drawer
            if((AudioPlayer.mediaPlayer != null) &&
           		(mCurrentPlaying_DrawerIndex == position) )
            	viewHolder.drawerTitle.setTextColor(Color.argb(0xff, 0xff, 0x80, 0x00));
            else
            	viewHolder.drawerTitle.setTextColor(Color.argb(0xff, 0xff, 0xff, 0xff));
			
			
			mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
			viewHolder.drawerTitle.setText(mDb.getDrawerTitle(position));
  		    mDb.doClose();

			return convertView;
		}
	}
    
    
	/******************************************************
	 * Menu
	 * 
	 */
    // Menu identifiers
	private static SharedPreferences mPref_show_note_attribute;

    static final int ADD_TEXT = R.id.ADD_TEXT;
    static final int ADD_CAMERA_PICTURE = R.id.ADD_NEW_PICTURE;
    static final int ADD_READY_PICTURE = R.id.ADD_OLD_PICTURE;
    static final int ADD_AUDIO = R.id.ADD_MUSIC;
    static final int OPEN_PLAY_SUBMENU = R.id.PLAY;
    static final int PLAY_OR_PAUSE_AUDIO = R.id.PLAY_OR_PAUSE_MUSIC;
    static final int STOP_AUDIO = R.id.STOP_MUSIC;
    static final int SLIDE_SHOW = R.id.SLIDE_SHOW;
    
    static final int CHECK_ALL = R.id.CHECK_ALL;
    static final int UNCHECK_ALL = R.id.UNCHECK_ALL;
    static final int MOVE_CHECKED_NOTE = R.id.MOVE_CHECKED_NOTE;
    static final int COPY_CHECKED_NOTE = R.id.COPY_CHECKED_NOTE;
    static final int MAIL_CHECKED_NOTE = R.id.MAIL_CHECKED_NOTE;
    static final int DELETE_CHECKED_NOTE = R.id.DELETE_CHECKED_NOTE;
    static final int SLIDE_SHOW_CHECKED_NOTE = R.id.SLIDE_SHOW_CHECKED_NOTE;
    static final int ADD_NEW_PAGE = R.id.ADD_NEW_PAGE;
    static final int CHANGE_PAGE_COLOR = R.id.CHANGE_PAGE_COLOR;
    static final int SHIFT_PAGE = R.id.SHIFT_PAGE;
    static final int SHOW_BODY = R.id.SHOW_BODY;
    static final int ENABLE_DRAGGABLE = R.id.ENABLE_DND;
    static final int SEND_PAGES = R.id.SEND_PAGES;
    static final int GALLERY = R.id.GALLERY;
	static final int CONFIG_PREFERENCE = R.id.CONFIG_PREF;    
	
	/*
	 * onCreate Options Menu
	 */
	static MenuItem mSubMenuItemAudio;
	MenuItem playOrPauseMusicButton;
	MenuItem stopMusicButton;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
//		System.out.println("DrawerActivity / onCreateOptionsMenu");
		mMenu = menu;
		//
		// set sub menu 0: add new note
		//
	    SubMenu subMenu0 = menu.addSubMenu(0, 0, 0, R.string.add_new_note);//order starts from 0
	    
	    // add item
	    subMenu0.add(0, ADD_TEXT, 1, R.string.note_text)
        		.setIcon(android.R.drawable.ic_menu_edit);
	    subMenu0.add(0, ADD_CAMERA_PICTURE, 2, R.string.note_camera_picture)
				.setIcon(android.R.drawable.ic_menu_camera);
	    subMenu0.add(0, ADD_READY_PICTURE, 3, R.string.note_ready_picture)
        		.setIcon(android.R.drawable.ic_menu_gallery);	    
	    subMenu0.add(0, ADD_AUDIO, 4, R.string.note_audio)
        		.setIcon(R.drawable.ic_lock_ringer_on);
	    
	    // icon
	    MenuItem subMenuItem0 = subMenu0.getItem();
	    subMenuItem0.setIcon(R.drawable.ic_input_add);
		
	    // set sub menu display
		subMenuItem0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);	 
		
		//
		// set sub menu 1: Play music & slide show
		//
	    SubMenu subMenu1 = menu.addSubMenu(0, OPEN_PLAY_SUBMENU, 1, R.string.menu_button_play);//order starts from 0
	    
	    // add item
	    subMenu1.add(0, PLAY_OR_PAUSE_AUDIO, 1, R.string.menu_button_play_audio)
   				.setIcon(R.drawable.ic_media_play);	    	
	    subMenu1.add(0, STOP_AUDIO, 2, R.string.menu_button_stop_audio)
				.setIcon(R.drawable.ic_media_stop);
	    playOrPauseMusicButton = subMenu1.getItem(0);
	    stopMusicButton = subMenu1.getItem(1);
		  
	    if(AudioPlayer.mediaPlayer ==  null)
	    	stopMusicButton.setVisible(false);
	    else
	    	stopMusicButton.setVisible(true);

	    subMenu1.add(0, SLIDE_SHOW, 3, R.string.menu_button_slide_show)
				.setIcon(R.drawable.ic_menu_play_clip);
	    
		//set highlight icon or not
	    mSubMenuItemAudio = subMenu1.getItem();
		if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_STOP)
			mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
		else
			mSubMenuItemAudio.setIcon(R.drawable.ic_menu_at_playing);
		
	    // set sub menu display
		mSubMenuItemAudio.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                     MenuItem.SHOW_AS_ACTION_WITH_TEXT);	 
		
	    //
		// set sub menu 2: handle checked note
	    //
	    SubMenu subMenu2 = menu.addSubMenu(0, 0, 2, R.string.checked_notes);//order starts from 0
	    
	    // add item
	    subMenu2.add(0, CHECK_ALL, 1, R.string.checked_notes_check_all)
        		.setIcon(R.drawable.btn_check_on_holo_dark);
	    subMenu2.add(0, UNCHECK_ALL, 2, R.string.checked_notes_uncheck_all)
				.setIcon(R.drawable.btn_check_off_holo_dark);
	    subMenu2.add(0, MOVE_CHECKED_NOTE, 3, R.string.checked_notes_move_to)
        		.setIcon(R.drawable.ic_menu_goto);	    
	    subMenu2.add(0, COPY_CHECKED_NOTE, 4, R.string.checked_notes_copy_to)
        		.setIcon(R.drawable.ic_menu_copy_holo_dark);
	    subMenu2.add(0, MAIL_CHECKED_NOTE, 5, R.string.mail_notes_btn)
        		.setIcon(android.R.drawable.ic_menu_send);
	    subMenu2.add(0, DELETE_CHECKED_NOTE, 6, R.string.checked_notes_delete)
        		.setIcon(R.drawable.ic_menu_clear_playlist);
	    // icon
	    MenuItem subMenuItem2 = subMenu2.getItem();
	    subMenuItem2.setIcon(R.drawable.ic_menu_mark);
	    
	    // set sub menu display
		subMenuItem2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		
		//
		// set sub menu 3: overflow
		//
	    SubMenu subMenu3 = menu.addSubMenu(0, 0, 3, R.string.options);//order starts from 0
	    // add item
	    subMenu3.add(0, ADD_NEW_PAGE, 1, R.string.add_new_page)
	            .setIcon(R.drawable.ic_menu_add_new_page);
	    
	    subMenu3.add(0, CHANGE_PAGE_COLOR, 2, R.string.change_page_color)
        	    .setIcon(R.drawable.ic_color_a);
	    
	    subMenu3.add(0, SHIFT_PAGE, 3, R.string.rearrange_page)
	            .setIcon(R.drawable.ic_dragger_h);
    	
	    // show body
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
    		subMenu3.add(0, SHOW_BODY, 4, R.string.preview_note_body_no)
     	   		    .setIcon(R.drawable.ic_media_group_collapse);
    	else
    		subMenu3.add(0, SHOW_BODY, 4, R.string.preview_note_body_yes)
        	        .setIcon(R.drawable.ic_media_group_expand);
    	
    	// show draggable
	    mPref_show_note_attribute = getSharedPreferences("show_note_attribute", 0);
    	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
    		subMenu3.add(0, ENABLE_DRAGGABLE, 5, getResources().getText(R.string.draggable_no))
		    				.setIcon(R.drawable.ic_dragger_off);
    	else
    		subMenu3.add(0, ENABLE_DRAGGABLE, 5, getResources().getText(R.string.draggable_yes))
    						.setIcon(R.drawable.ic_dragger_on);
    	
	    subMenu3.add(0, SEND_PAGES, 6, R.string.mail_notes_title)
 	   			.setIcon(android.R.drawable.ic_menu_send);

	    subMenu3.add(0, GALLERY, 7, R.string.gallery)
				.setIcon(android.R.drawable.ic_menu_gallery);	    
	    
	    subMenu3.add(0, CONFIG_PREFERENCE, 8, R.string.settings)
	    	   .setIcon(R.drawable.ic_menu_preferences);
	    
	    // set icon
	    MenuItem subMenuItem3 = subMenu3.getItem();
	    subMenuItem3.setIcon(R.drawable.ic_menu_moreoverflow);
	    
	    // set sub menu display
		subMenuItem3.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | 
				                    MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		return super.onCreateOptionsMenu(menu);
	}
	
	/*
	 * on options item selected
	 * 
	 */
	static SlideshowInfo slideshowInfo;
	FragmentManager fragmentManager = null;
	static int mCurrentPlaying_NotesTblId;
	static int mCurrentPlaying_TabIndex;
	static int mCurrentPlaying_DrawerIndex;
    @Override 
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		// Go back: check if Configure fragment now
    	if( (item.getItemId() == android.R.id.home) && bEnableConfig)
    	{
    		getSupportFragmentManager().popBackStack();
    		mConfigFragment = null;  
    		bEnableConfig = false;
    		mMenu.setGroupVisible(0, true);        		
            mDrawerLayout.closeDrawer(mDrawerListView);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			setTitle(mDrawerTitle);
            return true;
    	}
    	
    	// The action bar home/up action should open or close the drawer.
    	// ActionBarDrawerToggle will take care of this.
    	if (mDrawerToggle.onOptionsItemSelected(item))
    	{
    		System.out.println("mDrawerToggle.onOptionsItemSelected(item) / ActionBarDrawerToggle");
    		return true;
    	}
    	
    	final Intent intent;
        switch (item.getItemId()) 
        {
        	case ADD_TEXT:
				intent = new Intent(this, Note_addNewText.class);
				new Note_addNew_optional(this, intent);
				return true;

        	case ADD_CAMERA_PICTURE:
				intent = new Intent(this, Note_addCameraPicture.class);
				new Note_addNew_optional(this, intent);
	            return true;
	            
        	case ADD_READY_PICTURE:
				intent = new Intent(this, Note_addReadyPicture.class); 
				new Note_addNew_optional(this, intent);
				return true;

        	case ADD_AUDIO:
				intent = new Intent(this, Note_addAudio.class); 
				new Note_addNew_optional(this, intent);
				return true;
        	
        	case OPEN_PLAY_SUBMENU:
        		// new play instance: stop button is off
        	    if(AudioPlayer.mediaPlayer == null)
           	    {
    		    	playOrPauseMusicButton.setTitle(R.string.menu_button_play_audio);
            		playOrPauseMusicButton.setIcon(android.R.drawable.ic_media_play);	    	
            		stopMusicButton.setVisible(false);
           	    }         	    	
        	    else
        	    {
        		    if(AudioPlayer.mediaPlayer.isPlaying())
        		    {
        		    	// show Pause
               			playOrPauseMusicButton.setTitle(R.string.menu_button_pause_audio);
               			playOrPauseMusicButton.setIcon(android.R.drawable.ic_media_pause);
        		    }
        		    else
        		    {
        		    	// show Play (continue)
        			    playOrPauseMusicButton.setTitle(R.string.menu_button_continue_play_audio);
        			    playOrPauseMusicButton.setIcon(android.R.drawable.ic_media_play);	    	
        		    }
        		    // show Stop
       		    	stopMusicButton.setVisible(true);
        	    }
        		return true;
        	
        	case PLAY_OR_PAUSE_AUDIO:
        		// new instance
        		if(AudioPlayer.mediaPlayer == null)
        		{   
            		int lastTimeView_NotesTblId =  Integer.valueOf(Util.getPref_lastTimeView_NotesTableId(this));
        			mCurrentPlaying_NotesTblId = lastTimeView_NotesTblId;
            		AudioPlayer.audioIndex  = 0;
            		AudioPlayer.mIsOneTimeMode = false; 
            		mCurrentPlaying_TabIndex = TabsHostFragment.mCurrentTabIndex;
            		mCurrentPlaying_DrawerIndex = mCurrentDrawerIndex;
        		}
        		Util.startAudioPlayer(this);
        	    invalidateOptionsMenu(); // update audio play status
        		return true;

        	case STOP_AUDIO:
        		if(AudioPlayer.mediaPlayer != null)
        		{
					Util.stopAudioPlayer();
					AudioPlayer.audioIndex = 0;
					AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;
					TabsHostFragment.setPlayingTab_WithHighlight(false);
					NoteFragment.mItemAdapter.notifyDataSetChanged();
					
    		    	mSubMenuItemAudio.setIcon(R.drawable.ic_menu_slideshow);
					return true; // just stop playing, wait for user action
        		}
        		return true;
        		
        	case SLIDE_SHOW:
        		slideshowInfo = new SlideshowInfo();
        		// add images for slide show
        		mDb = new DB(this);
        		mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        		for(int i=0;i< mDb.getNotesCount() ;i++)
        		{
        			if(mDb.getNoteMarking(i) == 1)
        			{
        				String pictureUri = mDb.getNotePictureUri(i);
        				if(pictureUri.length() > 0) // skip empty
        					slideshowInfo.addImage(pictureUri);
        			}
        		}
        		mDb.doClose();
        		          		
        		if(slideshowInfo.imageSize() > 0)
        		{
					// create new Intent to launch the slideShow player Activity
					Intent playSlideshow = new Intent(this, SlideshowPlayer.class);
					startActivity(playSlideshow);  
        		}
        		else
        			Toast.makeText(mContext,R.string.file_not_found,Toast.LENGTH_SHORT).show();
        		return true;
				
            case ADD_NEW_PAGE:
                addNewPage(TabsHostFragment.mLastExist_TabId + 1);
                return true;
                
            case CHANGE_PAGE_COLOR:
            	changePageColor();
                return true;    
                
            case SHIFT_PAGE:
            	shiftPage();
                return true;  
                
            case SHOW_BODY:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_SHOW_BODY", "yes").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","no").commit();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_SHOW_BODY","yes").commit();
            	TabsHostFragment.updateChange(this);
                return true; 

            case ENABLE_DRAGGABLE:
            	mPref_show_note_attribute = mContext.getSharedPreferences("show_note_attribute", 0);
            	if(mPref_show_note_attribute.getString("KEY_ENABLE_DRAGGABLE", "yes").equalsIgnoreCase("yes"))
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","no").commit();
            	else
            		mPref_show_note_attribute.edit().putString("KEY_ENABLE_DRAGGABLE","yes").commit();
            	TabsHostFragment.updateChange(this);
                return true;                 
                
            case SEND_PAGES:
				Intent intentSend = new Intent(this, SendMailAct.class);
				startActivity(intentSend);
				TabsHostFragment.updateChange(this);
            	return true;

            case GALLERY:
				Intent i_browsePic = new Intent(this, GalleryGridAct.class);
				i_browsePic.putExtra("gallery", true);
				startActivity(i_browsePic);
            	return true; 	

            case CONFIG_PREFERENCE:
            	mMenu.setGroupVisible(0, false); //hide the menu
        		setTitle(R.string.settings);
        		bEnableConfig = true;
        		
            	mConfigFragment = new Config();
            	fragmentManager = getSupportFragmentManager();
            	FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.content_frame, mConfigFragment).addToBackStack("config").commit();
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    /*
     *  on Back button pressed
     *
     */
    @Override
    public void onBackPressed()
    {
        System.out.println("_onBackPressed");
        if(!bEnableConfig)
        {
        	super.onBackPressed();
            // stop audio player
            Util.stopAudioPlayer();        	
        }
        else
        {
    		getSupportFragmentManager().popBackStack();
    		mConfigFragment = null;  
    		bEnableConfig = false;
    		mMenu.setGroupVisible(0, true);
            mDrawerLayout.closeDrawer(mDrawerListView);
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		// Note: No need to keep AudioPlayer.audioIndex for NOT one-time-mode
//		//http://blog.shamanland.com/2014/01/nested-fragments-for-result.html
//        if( (requestCode & 0xffff /*to strip off the fragment index*/) 
//        	== Util.ACTIVITY_VIEW_NOTE ) 
//        {
//        	if (resultCode == Activity.RESULT_OK)
//        		AudioPlayer.audioIndex =  data.getIntExtra("audioIndexBack", AudioPlayer.audioIndex);
//        }  
	}	
    
    /**
     * Add new page
     * 
     */
	public  void addNewPage(final int newTabId) {
		// get tab name
		String tabName = "N".concat(String.valueOf(newTabId));
        
        final EditText editText1 = new EditText(getBaseContext());
        editText1.setText(tabName);
        editText1.setSelection(tabName.length()); // set edit text start position
        
        //update tab info
        Builder builder = new Builder(DrawerActivity.this);
        builder.setTitle(R.string.edit_page_tab_title)
                .setMessage(R.string.edit_page_tab_message)
                .setView(editText1)   
                .setNegativeButton(R.string.edit_page_button_ignore, new OnClickListener(){   
                	@Override
                    public void onClick(DialogInterface dialog, int which)
                    {/*nothing*/}
                })
                .setPositiveButton(R.string.edit_page_button_update, new OnClickListener()
                {   @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                		
	    	            final String[] items = new String[]{
	    	            		getResources().getText(R.string.add_new_page_leftmost).toString(),
	    	            		getResources().getText(R.string.add_new_page_rightmost).toString() };
	    	            
						AlertDialog.Builder builder = new AlertDialog.Builder(DrawerActivity.this);
						  
						builder.setTitle(R.string.add_new_page_select_position)
						.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which) {
						
							if(which ==0)
								insertTabLeftmost(newTabId, editText1.getText().toString());
							else
								insertTabRightmost(newTabId, editText1.getText().toString());
							//end
							dialog.dismiss();
						}})
						.setNegativeButton(R.string.btn_Cancel, null)
						.show();
                    }
                })	 
                .setIcon(android.R.drawable.ic_menu_edit);
        
	        final AlertDialog d = builder.create();
	        d.show();
	        // android.R.id.button1 for negative: cancel 
	        ((Button)d.findViewById(android.R.id.button1))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
	        // android.R.id.button2 for positive: save
	        ((Button)d.findViewById(android.R.id.button2))
	        .setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
	}
	
	/* 
	 * Insert Tab to Leftmost
	 * 
	 */
	void insertTabLeftmost(int newTabId,String tabName)
	{
 	    // insert tab name
		TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		int style = Util.getNewPageStyle(mContext);
		TabsHostFragment.mDb.insertTab(DB.getCurrentTabsTableName(),tabName, newTabId,tabName, newTabId,style );
		
		// insert table for new tab
		TabsHostFragment.mDb.insertNoteTable(newTabId);
		TabsHostFragment.mTabCount++;
		TabsHostFragment.mDb.doClose();
		
		//change to leftmost tab Id
		TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		int tabTotalCount = DB.getTabsCount();
		TabsHostFragment.mDb.doClose();
		for(int i=0;i <(tabTotalCount-1);i++)
		{
			int tabIndex = tabTotalCount -1 -i ;
			swapTabInfo(tabIndex,tabIndex-1);
			updateFinalPageViewed();
		}
		
        // set scroll X
		final int scrollX = 0; // leftmost
		
		// commit: scroll X
		TabsHostFragment.updateChange(this);
    	
		TabsHostFragment.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHostFragment.mHorScrollView.scrollTo(scrollX, 0);
	        	Util.setPref_lastTimeView_scrollX_byDrawerNumber(DrawerActivity.this, scrollX );
	        } 
	    });
		
		// update highlight tab
		DrawerActivity.mCurrentPlaying_TabIndex++;
	}
	
	/*
	 * Update Final page which was viewed last time
	 * 
	 */
	protected void updateFinalPageViewed()
	{
        // get final viewed table Id
        String tblId = Util.getPref_lastTimeView_NotesTableId(this);
		Context context = getApplicationContext();

		DB.setNotes_TableId(tblId);
		TabsHostFragment.mDb = new DB(context);
		
		TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		// get final view tab index of last time
		for(int i =0;i<DB.getTabsCount();i++)
		{
			if(Integer.valueOf(tblId) == DB.getTab_NotesTableId(i))
				TabsHostFragment.mFinalPageViewed_TabIndex = i;	// starts from 0
			
        	if(	TabsHostFragment.mDb.getTabId(i)== TabsHostFragment.mFirstExist_TabId)
        		Util.setPref_lastTimeView_NotesTableId(this, DB.getTab_NotesTableId(i) );
		}
		TabsHostFragment.mDb.doClose();
	}
	
	/*
	 * Insert Tab to Rightmost
	 * 
	 */
	void insertTabRightmost(int newTblId,String tabName)
	{
 	    // insert tab name
		TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		int style = Util.getNewPageStyle(mContext);
		TabsHostFragment.mDb.insertTab(DB.getCurrentTabsTableName(),tabName,newTblId,tabName,newTblId,style );
		
		// insert table for new tab
		TabsHostFragment.mDb.insertNoteTable(newTblId);
		TabsHostFragment.mTabCount++;
		TabsHostFragment.mDb.doClose();
		
		// commit: final page viewed
		Util.setPref_lastTimeView_NotesTableId(this, newTblId);
		
        // set scroll X
		final int scrollX = (TabsHostFragment.mTabCount) * 60 * 5; //over the last scroll X
		
		TabsHostFragment.updateChange(this);
    	
		TabsHostFragment.mHorScrollView.post(new Runnable() {
	        @Override
	        public void run() {
	        	TabsHostFragment.mHorScrollView.scrollTo(scrollX, 0);
	        	Util.setPref_lastTimeView_scrollX_byDrawerNumber(DrawerActivity.this, scrollX );
	        } 
	    });
	}
	
	/*
	 * Change Page Color
	 * 
	 */
	void changePageColor()
	{
		// set color
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_page_color_title)
	    	   .setPositiveButton(R.string.edit_page_button_ignore, new OnClickListener(){   
	            	@Override
	                public void onClick(DialogInterface dialog, int which)
	                {/*cancel*/}
	            	});
		// inflate select style layout
		LayoutInflater mInflator= (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = mInflator.inflate(R.layout.select_style, null);
		RadioGroup RG_view = (RadioGroup)view.findViewById(R.id.radioGroup1);
		
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio0),0);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio1),1);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio2),2);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio3),3);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio4),4);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio5),5);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio6),6);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio7),7);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio8),8);
		Util.setButtonColor((RadioButton)RG_view.findViewById(R.id.radio9),9);
		
		// set current selection
		for(int i=0;i< Util.getStyleCount();i++)
		{
			if(Util.getCurrentPageStyle(this) == i)
			{
				RadioButton buttton = (RadioButton) RG_view.getChildAt(i);
		    	if(i%2 == 0)
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_dark);
		    	else
		    		buttton.setButtonDrawable(R.drawable.btn_radio_on_holo_light);		    		
			}
		}
		
		builder.setView(view);
		
		RadioGroup radioGroup = (RadioGroup) RG_view.findViewById(R.id.radioGroup1);
		    
		final AlertDialog dlg = builder.create();
	    dlg.show();
	    
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup RG, int id) {
				TabsHostFragment.mStyle = RG.indexOfChild(RG.findViewById(id));
				TabsHostFragment.mDb = new DB(DrawerActivity.this);
				TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
				TabsHostFragment.mDb.updateTab(TabsHostFragment.mDb.getTabId(TabsHostFragment.mCurrentTabIndex),
	 							  DB.getTabTitle(TabsHostFragment.mCurrentTabIndex),
	 							  DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex),
	 							  TabsHostFragment.mStyle );
				TabsHostFragment.mDb.doClose();
	 			dlg.dismiss();
	 			TabsHostFragment.updateChange(DrawerActivity.this);
		}});
	}

	
	
    /**
     * shift page right or left
     * 
     */
    void shiftPage()
    {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.rearrange_page_title)
          	   .setMessage(null)
               .setNegativeButton(R.string.rearrange_page_left, null)
               .setNeutralButton(R.string.edit_note_button_back, null)
               .setPositiveButton(R.string.rearrange_page_right,null)
               .setIcon(R.drawable.ic_dragger_h);
        final AlertDialog d = builder.create();
        
        // disable dim background 
    	d.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    	d.show();
    	
    	
    	final int dividerWidth = getResources().getDrawable(R.drawable.ic_tab_divider).getMinimumWidth();
    	// To left
        d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
        {  @Override
           public void onClick(View v)
           {
        		//change to OK
        		Button mButton=(Button)d.findViewById(android.R.id.button3);
    	        mButton.setText(R.string.btn_Finish);
    	        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);

    	        int[] leftMargin = {0,0};
    	        if(TabsHostFragment.mCurrentTabIndex == 0)
    	        	TabsHostFragment.mTabHost.getTabWidget().getChildAt(0).getLocationInWindow(leftMargin);
    	        else
    	        	TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex-1).getLocationInWindow(leftMargin);

    			int curTabWidth,nextTabWidth;
    			curTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex).getWidth();
    			if(TabsHostFragment.mCurrentTabIndex == 0)
    				nextTabWidth = curTabWidth;
    			else
    				nextTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex-1).getWidth(); 

    			// when leftmost tab margin over window border
           		if(leftMargin[0] < 0) 
           			TabsHostFragment.mHorScrollView.scrollBy(- (nextTabWidth + dividerWidth) , 0);
				
        		d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        	    if(TabsHostFragment.mCurrentTabIndex == 0)
        	    {
        	    	Toast.makeText(TabsHostFragment.mTabHost.getContext(), R.string.toast_leftmost ,Toast.LENGTH_SHORT).show();
        	    	d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);//avoid long time toast
        	    }
        	    else
        	    {
        	    	TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
        	    	Util.setPref_lastTimeView_NotesTableId(DrawerActivity.this, DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex));
        	    	TabsHostFragment.mDb.doClose();
					swapTabInfo(TabsHostFragment.mCurrentTabIndex,TabsHostFragment.mCurrentTabIndex-1);
					TabsHostFragment.updateChange(DrawerActivity.this);
        	    }
           }
        });
        
        // done
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
        {   @Override
           public void onClick(View v)
           {
               d.dismiss();
           }
        });
        
        // To right
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {  @Override
           public void onClick(View v)
           {
        		d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
        		
        		// middle button text: change to OK
	    		Button mButton=(Button)d.findViewById(android.R.id.button3);
		        mButton.setText(R.string.btn_Finish);
		        mButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_finish , 0, 0, 0);
   	    		
		        TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
   	    		int count = DB.getTabsCount();
   	    		TabsHostFragment.mDb.doClose();
                
    			int[] rightMargin = {0,0};
    			if(TabsHostFragment.mCurrentTabIndex == (count-1))
    				TabsHostFragment.mTabHost.getTabWidget().getChildAt(count-1).getLocationInWindow(rightMargin);
    			else
    				TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex+1).getLocationInWindow(rightMargin);

    			int curTabWidth, nextTabWidth;
    			curTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex).getWidth();
    			if(TabsHostFragment.mCurrentTabIndex == (count-1))
    				nextTabWidth = curTabWidth;
    			else
    				nextTabWidth = TabsHostFragment.mTabHost.getTabWidget().getChildAt(TabsHostFragment.mCurrentTabIndex+1).getWidth();
    			
	    		// when rightmost tab margin plus its tab width over screen border 
    			int screenWidth = UtilImage.getScreenWidth(DrawerActivity.this);
	    		if( screenWidth <= rightMargin[0] + nextTabWidth )
	    			TabsHostFragment.mHorScrollView.scrollBy(nextTabWidth + dividerWidth, 0);	
				
	    		d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
   	    		
       	    	if(TabsHostFragment.mCurrentTabIndex == (count-1))
       	    	{
       	    		// end of the right side
       	    		Toast.makeText(TabsHostFragment.mTabHost.getContext(),R.string.toast_rightmost,Toast.LENGTH_SHORT).show();
       	    		d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);//avoid long time toast
       	    	}
       	    	else
       	    	{
        	    	Util.setPref_lastTimeView_NotesTableId(DrawerActivity.this, DB.getTab_NotesTableId(TabsHostFragment.mCurrentTabIndex));
					swapTabInfo(TabsHostFragment.mCurrentTabIndex,TabsHostFragment.mCurrentTabIndex+1);
					TabsHostFragment.updateChange(DrawerActivity.this);
       	    	}
           }
        });
        
        // android.R.id.button1 for positive: next 
        ((Button)d.findViewById(android.R.id.button1))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_forward, 0, 0, 0);
        // android.R.id.button2 for negative: previous
        ((Button)d.findViewById(android.R.id.button2))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);
        // android.R.id.button3 for neutral: cancel
        ((Button)d.findViewById(android.R.id.button3))
        .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_close_clear_cancel, 0, 0, 0);
    }
    
    /**
     * swap tab info
     * 
     */
    void swapTabInfo(int start, int end)
    {
    	TabsHostFragment.mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
    	TabsHostFragment.mDb.updateTab(TabsHostFragment.mDb.getTabId(end),
        		DB.getTabTitle(start),
        		DB.getTab_NotesTableId(start),
        		TabsHostFragment.mDb.getTabStyle(start));		        
		
        TabsHostFragment.mDb.updateTab(TabsHostFragment.mDb.getTabId(start),
				DB.getTabTitle(end),
				DB.getTab_NotesTableId(end),
				TabsHostFragment.mDb.getTabStyle(end));
		TabsHostFragment.mDb.doClose();
    }
    
    boolean mIsCalledWhilePlayingAudio;
    // for Pause audio player when incoming call
    // http://stackoverflow.com/questions/5610464/stopping-starting-music-on-incoming-calls
    PhoneStateListener phoneStateListener = new PhoneStateListener() 
    {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) 
        {
            if ( (state == TelephonyManager.CALL_STATE_RINGING) || 
                 (state == TelephonyManager.CALL_STATE_OFFHOOK )   ) 
            {
                //Incoming call or Call out: Pause music
            	System.out.println("Incoming call:");
            	if(AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PLAY)
            	{
            		Util.startAudioPlayer(DrawerActivity.this);
            		mIsCalledWhilePlayingAudio = true;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_IDLE) 
            {
                //Not in call: Play music
            	System.out.println("Not in call:");
            	if( (AudioPlayer.mPlayerState == AudioPlayer.PLAYER_AT_PAUSE) && 
            		mIsCalledWhilePlayingAudio )	
            	{
            		Util.startAudioPlayer(DrawerActivity.this); // pause => play
            		mIsCalledWhilePlayingAudio = false;
            	}
            } 
            else if(state == TelephonyManager.CALL_STATE_OFFHOOK) 
            {
                //A call is dialing, active or on hold
            	System.out.println("A call is dialing, active or on hold:");
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    
    
	private class NoisyAudioStreamReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				if(AudioPlayer.mediaPlayer.isPlaying())
				{
					System.out.println("NoisyAudioStreamReceiver / play -> pause");
					AudioPlayer.mediaPlayer.pause();
					AudioPlayer.audioHandler.removeCallbacks(AudioPlayer.runPlayAudio); 
					AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_PAUSE;
					
		    		// update playing state in note view pager
					if( Note_view_pager.mPager != null)
					{
						if(Note_view_pager.mImageViewAudioButton != null) 
							Note_view_pager.mImageViewAudioButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_ringer_on, 0, 0, 0);
						if(Note_view_pager.mMenuItemAudio.isVisible())
							Note_view_pager.mMenuItemAudio.setIcon(R.drawable.ic_lock_ringer_on);
					}
		    		
				}        	
	        }
	    }
	}
}