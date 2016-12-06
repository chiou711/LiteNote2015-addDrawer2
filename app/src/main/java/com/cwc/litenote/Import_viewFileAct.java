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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
public class Import_viewFileAct extends Activity {

    private TextView mTitleViewText;
    private TextView mBodyViewText;
    Bundle extras ;
    File file;
    FileInputStream fileInputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_file);
        
        mTitleViewText = (TextView) findViewById(R.id.view_title);
        mBodyViewText = (TextView) findViewById(R.id.view_body);
        
	    getActionBar().setDisplayShowHomeEnabled(false);
        
		insertDbWithFilledContent(false);

		int style = 2;
        //set title color
		mTitleViewText.setTextColor(Util.mText_ColorArray[style]);
		mTitleViewText.setBackgroundColor(Util.mBG_ColorArray[style]);
		//set body color 
		mBodyViewText.setTextColor(Util.mText_ColorArray[style]);
		mBodyViewText.setBackgroundColor(Util.mBG_ColorArray[style]);
		
        // back button
        Button backButton = (Button) findViewById(R.id.view_back);
        backButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_back, 0, 0, 0);

        // confirm button
        Button confirmButton = (Button) findViewById(R.id.view_confirm);
//        confirmButton.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_revert, 0, 0, 0);
        
        // do cancel
        backButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }

        });
        
		// do confirm 
        confirmButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) 
            {
                setResult(RESULT_OK);
                insertDbWithFilledContent(true); 
       		 	finish();
            }
        });
    }

    private void insertDbWithFilledContent(boolean enableInsertDb) 
    {
		 extras = getIntent().getExtras();
    	 file = new File(extras.getString("FILE_PATH"));
    	 
		 try {
				 fileInputStream = new FileInputStream(file);
		 } catch (FileNotFoundException e) {
		 	e.printStackTrace();
		 }
		 
		 // import data by HandleXmlByFile class
		 Import_handleXmlFile obj = new Import_handleXmlFile(fileInputStream,this);
	     obj.enableInsertDb(enableInsertDb);
	     obj.handleXML();
	     while(obj.parsingComplete);

	     // show Import content
	     mTitleViewText.setText(file.getName());
	     mBodyViewText.setText(obj.fileBody);
	     
	      if(enableInsertDb)
		  {
	    	  Toast.makeText(this,R.string.toast_import_finished,Toast.LENGTH_SHORT).show();
		  }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        insertDbWithFilledContent(false);
    }
}
