package com.cwc.litenote;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

//public class AudioPlayer extends Fragment
public class AudioPlayer 
{
	private static final String TAG = "AUDIO_PLAYER"; // error logging tag
	private static final int DURATION_1S = 1000; // 1 seconds per slide
	private static AudioInfo audioInfo; // slide show being played
	static Handler audioHandler; // used to update the slide show
	static int audioIndex; // index of current media to play
	static int playbackTime; // time in miniSeconds from which media should play 
	private static int audioDuration; // media length
	public static MediaPlayer mediaPlayer; // plays the background music, if any
	static Activity mAct;
	static int mPlayerState;
	static int PLAYER_AT_STOP = 0;
	static int PLAYER_AT_PLAY = 1;
	static int PLAYER_AT_PAUSE = 2;
	static boolean mIsOneTimeMode;
   
	public AudioPlayer(FragmentActivity fa)  {
		System.out.println("AudioPlayer constructor");
		mAct = fa;
		audioHandler = new Handler();
		
		if(mPlayerState == PLAYER_AT_PLAY) 
			audioHandler.post(runPlayAudio); 	      
	};
   
	// Runnable: play audio 
	static Runnable runPlayAudio = new Runnable()
	{   @Override
		public void run()
		{
			// check audio Uri
			if (audioInfo.getAudioAt(audioIndex) != null)
	   	   	{  
				// for One-time Play mode 
	   		   	if(mIsOneTimeMode == true) 
	   		   	{ 
	   		   		//if media is null, try to create a MediaPlayer to play the music
	   		   		if(mediaPlayer == null)
	   		   		{
	   		   			String audioStr = audioInfo.getAudioAt(audioIndex);
	   		   			if(!Util.isEmptyString(audioStr))
	   		   			{
	   		   				mediaPlayer = new MediaPlayer(); 
	   		   				System.out.println("Runnable updateMediaPlay / new media player (mIsOneTimeMode == true)");
	   		   				mediaPlayer.reset();
		   					   
	   		   				try
	   		   				{
	   		   					Uri uri = Uri.parse(audioStr);
	   		   					mediaPlayer.setDataSource(mAct, uri);
	   		   					mediaPlayer.prepare(); // prepare the MediaPlayer to play
	   		   					mediaPlayer.start();	    	            
	   		   					audioDuration = mediaPlayer.getDuration();
	   		   					mediaPlayer.seekTo(playbackTime); // seek to mediaTime, after start() sounds better
	   		   					mPlayerState = PLAYER_AT_PLAY;
		   					   
	   		   					//Note: below
	   		   					//Set 1 second will cause Media player abnormal on Power key short click
	   		   					audioHandler.postDelayed(runPlayAudio,DURATION_1S * 2);
	   		   				}
	   		   				catch(Exception e)
	   		   				{
	   		   					Toast.makeText(mAct,"Could not open file.",Toast.LENGTH_SHORT).show();
	   		   					mediaPlayer.release();
	   		   					mediaPlayer = null;
	   		   					audioHandler.removeCallbacks(runPlayAudio); 
	   		   					mPlayerState = PLAYER_AT_STOP;
   							   
	   		   					if(Note_view_pager.mImageViewAudioButton != null)
	   		   						Note_view_pager.mImageViewAudioButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_ringer_on, 0, 0, 0);
							   
	   		   					if(Note_view_pager.mMenuItemAudio != null)
	   		   						Note_view_pager.mMenuItemAudio.setIcon(R.drawable.ic_lock_ringer_on);
	   		   				}
	   		   			}
	   		   		}
	   		   		else
	   		   		{
	   		   			// for Not at playing 
	   		   			if(!mediaPlayer.isPlaying())
	   		   			{
	   		   				if(isMediaEndWasMet())	 
	   		   				{
	   		   					mediaPlayer.release();
	   		   					mediaPlayer = null;
	   		   					audioHandler.removeCallbacks(runPlayAudio);
	   		   					AudioPlayer.mPlayerState = AudioPlayer.PLAYER_AT_STOP;   							   
	   		   				}
	   		   			}
	   		   			else
	   		   				audioHandler.postDelayed(runPlayAudio,DURATION_1S);
	   		   		}
	   		   	}
	   		   
	   		   	// for Continue Play mode 
	   		   	if(mIsOneTimeMode == false)
	   		   	{
	   		   		if( AudioInfo.getAudioMarking(audioIndex) == 1 )
	   		   		{ 
	   		   			// if media is null, try to create a MediaPlayer to play the music
	   		   			if(mediaPlayer == null)
	   		   			{
	   		   				try
	   		   				{
	   		   					String audioStr = audioInfo.getAudioAt(audioIndex);
	   		   					if(!Util.isEmptyString(audioStr))
	   		   					{
	   		   						mediaPlayer = new MediaPlayer(); 
	   		   						System.out.println("Runnable updateMediaPlay / new media player (mIsOneTimeMode == false)");
	   		   						mediaPlayer.reset();
	   		   						try
	   		   						{
	   		   							Uri uri = Uri.parse(audioStr);
	   		   							mediaPlayer.setDataSource(mAct, uri);
	   		   							mediaPlayer.prepare(); // prepare the MediaPlayer to play
	   		   							mediaPlayer.start();	    	            
	   		   							audioDuration = mediaPlayer.getDuration();
	   		   							mediaPlayer.seekTo(playbackTime); // seek to mediaTime, after start() sounds better
				   					   
	   		   							//Note: below, set 1 second will cause Media player abnormal on Power key short click
	   		   							audioHandler.postDelayed(runPlayAudio,DURATION_1S * 2);
				   					   
	   		   							// set highlight of playing list item
	   		   							NoteFragment.mItemAdapter.notifyDataSetChanged();
			   						   
	   		   							// set highlight of playing tab
	   		   							if(DrawerActivity.mCurrentPlaying_DrawerIndex == DrawerActivity.mCurrentDrawerIndex)
	   		   								TabsHostFragment.setPlayingTab_WithHighlight(true);
	   		   							else
	   		   								TabsHostFragment.setPlayingTab_WithHighlight(false);			   						   
	   		   						}
	   		   						catch(Exception e)
	   		   						{
	   		   							Toast.makeText(mAct,"Could not open file, try next one.",Toast.LENGTH_SHORT).show();
	   		   							mediaPlayer.release();
	   		   							mediaPlayer = null;
	   		   							playbackTime = 0;
			   						   
	   		   							audioIndex++;
	   		   							if(audioIndex >= AudioInfo.getAudioList().size())
	   		   								audioIndex = 0; //back to first index
			   			   			   
//	   		   							audioHandler.postDelayed(runPlayAudio,DURATION_1S);			   						   
	   		   							audioHandler.post(runPlayAudio);			   						   
	   		   						}
	   		   					}
	   		   				}
	   		   				catch (Exception e)
	   		   				{
	   		   					Log.v(TAG, e.toString());
	   		   				}
	   		   			}
	   		   			else if(mediaPlayer != null )
	   		   			{
	   		   				// set looping: media is not playing 
	   		   				if(!mediaPlayer.isPlaying())
	   		   				{
	   		   					// increase media index
	   		   					if(isMediaEndWasMet())	 
	   		   					{
	   		   						mediaPlayer.release();
	   		   						mediaPlayer = null;
	   		   						playbackTime = 0;
					        		 
	   		   						// get next index
	   		   						audioIndex++;
	   		   						if(audioIndex == AudioInfo.getAudioList().size())
	   		   							audioIndex = 0;	// back to first index
	   		   					}
	   		   					else
	   		   						mediaPlayer.start();
	   		   				}
	   		   				// endless loop, do not set post() here, it will affect slide show timing
	   		   				audioHandler.postDelayed(runPlayAudio,DURATION_1S);
	   		   			}
	   		   		}
	   		   		else
	   		   			// for no marking item
	   		   		{
	   		   			// get next index
	   		   			audioIndex++;
	   		   			if(audioIndex >= AudioInfo.getAudioList().size())
	   		   				audioIndex = 0; //back to first index
	   		   			audioHandler.postDelayed(runPlayAudio,DURATION_1S);
	   		   		}	
	   		   	}
	   	   	}
		} 
	};
	
	static boolean isMediaEndWasMet()
	{
		playbackTime = mediaPlayer.getCurrentPosition();
		audioDuration = mediaPlayer.getDuration();
//		 System.out.println("mediaTime / mdeiaDuration = " + (int)((mediaTime * 100.0f) /mdeiaDuration) + "%" );
//		 System.out.println("mediaTime - mdeiaDuration = " + Math.abs(mediaTime - mdeiaDuration) );
		return Math.abs(playbackTime - audioDuration) < 1500; // toleration
	}
   
	static void prepareAudioInfo(Context context)
	{
		audioInfo = new AudioInfo(); 
		audioInfo.updateAudioInfo(context);
	}   
	
}