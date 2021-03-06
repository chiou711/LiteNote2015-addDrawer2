package com.cwc.litenote;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

public class AudioInfo
{
	private static List<String> audioList; // this slideshow's medium
	private static List<Integer> audioMarkingList; // this slideshow's medium marking
   
   // constructor 
   public AudioInfo()
   {
      audioList = new ArrayList<String>(); 
      audioMarkingList = new ArrayList<Integer>(); 
   }

   public static List<String> getAudioList()
   {
      return audioList;
   }
   
   public static int getAudioFilesSize()
   {
	   int size = 0; 
	   if(getAudioList()!= null)
	   {
		  for(int i=0;i< getAudioList().size();i++)
		  {
			  if( !Util.isEmptyString(audioList.get(i)) && (getAudioMarking(i) == 1) )
				  size++;
		  }
	   }
//	   System.out.println( " AudioInfo / getAudioFilesSize = " + size);
	   return size;
   }
   
   public static void addAudio(String path)
   {
      audioList.add(path);
   }
   
   public void setAudio(int i, String path)
   {
      audioList.set(i,path);
   }   
   
   public static void addAudioMarking(int i)
   {
	   audioMarkingList.add(i);
   }   
   
   public static void setAudioMarking(int idx, int marking)
   {
	   audioMarkingList.set(idx,marking);
   }

   public static int getAudioMarking(int idx)
   {
	   return  audioMarkingList.get(idx);
   }
   
   public int getFirstAudioMarking()
   {
	   int first = 0;
	   for(int i = 0;i < audioMarkingList.size() ; i++ )
	   {
		   if( audioMarkingList.get(i) == 1)
			   first = Math.min(first,i);
	   }
	   return first;
   }   
   
   // return String at position index
   public String getAudioAt(int index)
   {
      if (index >= 0 && index < audioList.size())
         return audioList.get(index);
      else
         return null;
   }
   
	// Update audio info
	void updateAudioInfo(Context context)
	{
		DB dB = null;
	 	// set DB table
	 	DB.setNotes_TableId(Util.getPref_lastTimeView_NotesTableId(context));
	 	dB = new DB(context);
		
	 	dB.doOpenByDrawerNum(DB.getDrawer_TabsTableId());

	 	// update media info 
	 	for(int i=0;i< dB.getNotesCount();i++)
	 	{
	 		String audioUri = dB.getNoteAudioUri(i);
	 		
	 		// initialize
	 		addAudio(audioUri);
	 		addAudioMarking(i);

	 		// set playable
	 		if( !Util.isEmptyString(audioUri)  && (dB.getNoteMarking(i) == 1))
		 		setAudioMarking(i,1);
	 		else
	 			setAudioMarking(i,0);
	 	}
	 	dB.doClose();
	}
	
}