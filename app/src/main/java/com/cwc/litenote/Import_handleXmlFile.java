package com.cwc.litenote;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.Context;

public class Import_handleXmlFile {

   private String tabname,title,body,picture,audio;
   private static DB mDb;
   private Context mContext;
   
   FileInputStream fileInputStream = null;
   public volatile boolean parsingComplete = true;
   public String fileBody = ""; 
   String strSplitter;
   public boolean mEnableInsertDB = true;
   
   public Import_handleXmlFile(FileInputStream fileInputStream,Context context)
   {
      mDb = new DB(context);
      mContext = context;
      this.fileInputStream = fileInputStream;
   }
   
   public String getTitle()
   {
      return title;
   }
   
   public String getBody()
   {
      return body;
   }
   
   public String getPicture()
   {
      return picture;
   }   
   
   public String getAudio()
   {
      return audio;
   }   
   
   public String getPage()
   {
      return tabname;
   }
   
   public void parseXMLAndInsertDB(XmlPullParser myParser) 
   {
	  
      int event;
      String text=null;
      try 
      {
         event = myParser.getEventType();
         while (event != XmlPullParser.END_DOCUMENT) 
         {
        	 String name = myParser.getName(); //name: null, link, item, title, description
//        	 System.out.println("name = " + name);
        	 switch (event)
	         {
	            case XmlPullParser.START_TAG:
	            if(name.equals("note"))
                {
	            	strSplitter = "--- note ---";
                }	
		        break;
		        
	            case XmlPullParser.TEXT:
			       text = myParser.getText();
	            break;
	            
	            case XmlPullParser.END_TAG:
		           if(name.equals("tabname"))
		           {
	                  tabname = text.trim();
	                  
	                  if(mEnableInsertDB)
	                  {
			        	  mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
			        	  int style = Util.getNewPageStyle(mContext);
			        	  // style is not set in XML file, so insert default style instead
			        	  mDb.insertTab(DB.getCurrentTabsTableName(),
			        			  			tabname,
			        			  			TabsHostFragment.getLastTabId() + 1,
			        			  			tabname,
			        			  			TabsHostFragment.getLastTabId() + 1,
			        			  			style );
			        		
			        	  // insert table for new tab
			        	  mDb.insertNoteTable(TabsHostFragment.getLastTabId() + 1 );
			        	  // update last tab Id after Insert
			        	  TabsHostFragment.setLastTabId(TabsHostFragment.getLastTabId() + 1);
			        	  mDb.doClose();
	                  }
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "=== " + "Page:" + " " + tabname + " ===");
	               }
	               else if(name.equals("title"))
	               {
		              text = text.replace("[n]"," ");
		              text = text.replace("[s]"," ");
		              title = text.trim();
		           }
	               else if(name.equals("body"))
	               { 	
	            	  body = text.trim();
	               }
	               else if(name.equals("picture"))
	               { 	
	            	  picture = text.trim();
	               }		           
	               else if(name.equals("audio"))
	               { 	
	            	  audio = text.trim();
	            	  if(mEnableInsertDB)
	            	  {
		            	  DB.setNotes_TableId(String.valueOf(TabsHostFragment.getLastTabId()));  
		            	  mDb.doOpenByDrawerNum(DB.getDrawer_TabsTableId());
		            	  if(title.length() !=0 || body.length() != 0 || picture.length() !=0 || audio.length() !=0)
		            		  mDb.insertNote(title, picture, audio, "", body,0, (long) 0); //set picture null
		            	  mDb.doClose();
	            	  }
		              fileBody = fileBody.concat(Util.NEW_LINE + strSplitter);
		              fileBody = fileBody.concat(Util.NEW_LINE + "title:" + " " + title);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "body:" + " " + body);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "picture:" + " " + picture);
		        	  fileBody = fileBody.concat(Util.NEW_LINE + "audio:" + " " + audio);
	            	  fileBody = fileBody.concat(Util.NEW_LINE);
	               }
	               
	               break;
	         }		 
        	 event = myParser.next();
         }
         
         parsingComplete = false;
      } 
      catch (Exception e) 
      {
         e.printStackTrace();
      }
   }

   public void handleXML()
   {
	   Thread thread = new Thread(new Runnable()
	   {
		   @Override
		   public void run() 
		   {
		      try 
		      {
		         InputStream stream = fileInputStream;
		         XmlPullParser myparser = XmlPullParserFactory.newInstance().newPullParser();
		         myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		         myparser.setInput(stream, null);
		         parseXMLAndInsertDB(myparser);
		         stream.close();
		      } 
		      catch (Exception e) 
		      { }
		  }
	  });
	  thread.start(); 
   }
   
   public void enableInsertDb(boolean en)
   {
	   mEnableInsertDB = en;
   }
}