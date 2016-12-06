package com.cwc.litenote;

import java.io.File;
import java.io.IOException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class UtilImage 
{
    static boolean bShowExpandedImage = false;

    public UtilImage(){};

    public static int getScreenWidth(Context context)
	{
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.x;
		}
		else
		{
			return display.getWidth();
		}
	}
	
    public static int getScreenHeight(Context context)
	{
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	    Display display = wm.getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.y;
		}
		else
		{
			return display.getHeight();
		}
	}    
    
	public static int getScreenWidth(Activity activity)
	{
	    Display display = activity.getWindowManager().getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.x;
		}
		else
		{
			return display.getWidth();
		}
	}
	
	public static int getScreenHeight(Activity activity)
	{
	    Display display = activity.getWindowManager().getDefaultDisplay();
		if(Build.VERSION.SDK_INT >= 13)
		{
		    Point outSize = new Point();
	        display.getSize(outSize);
	        return outSize.y;
		}
		else
		{
			return display.getHeight();
		}
	}
	
	public static Uri getPictureUri(String pictureName, Activity act)
    {
		// main directory
	    String dirString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + Util.getAppName(act);
	    
		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();

		// picture directory
	    String picDirString = Environment.getExternalStorageDirectory().toString() + 
	              "/" + Util.getAppName(act) +"/picture";

		File picDir = new File(picDirString);
		if(!picDir.isDirectory())
			picDir.mkdir();
		

		File photo = new File(picDirString,  pictureName);
	    return Uri.fromFile(photo);
    }
	
	public static String getPicturePath(String pictureName, Activity act)
    {
	    String dirString = Environment.getExternalStorageDirectory().toString() + 
	    		              "/" + Util.getAppName(act) +"/picture";
	    
		File dir = new File(dirString);
		if(!dir.isDirectory())
			dir.mkdir();

		File photo = new File(dir,  pictureName);
	    return photo.getPath();
    }	
	
    /**
    * "Zooms" in a thumbnail view by assigning the high resolution image to a hidden "zoomed-in"
    * image view and animating its bounds to fit the entire activity content area. More
    * specifically:
    *
    * <ol>
    *   <li>Assign the high-res image to the hidden "zoomed-in" (expanded) image view.</li>
    *   <li>Calculate the starting and ending bounds for the expanded view.</li>
    *   <li>Animate each of four positioning/sizing properties (X, Y, SCALE_X, SCALE_Y)
    *       simultaneously, from the starting bounds to the ending bounds.</li>
    *   <li>Zoom back out by running the reverse animation on click.</li>
    * </ol>
    *
    * @param thumbView  The thumbnail view to zoom in.
    * @param imageResId The high-resolution version of the image represented by the thumbnail.
    * @throws IOException 
    */
	static Animator mCurrentAnimator;
	private static int mShortAnimationDuration;
	static ImageView mExpandedImageView;
	static View mThumbView;
	static Rect mStartBounds;
	static float mStartScaleFinal;
	static Dialog mDialog;
	static Activity mAddNewAct;
	static boolean bEnableContinue;
	
//	static void zoomImageFromThumb(final View thumbView,
//    		 						String pictureUri,
//    		 						final Activity mAct) throws IOException 
//    {
//        // Load the high-resolution "zoomed-in" image.
//        mExpandedImageView = (ImageView) mAct.findViewById(R.id.expanded_image);
//    	
//        mThumbView = thumbView;
//
//        // If there's an animation in progress, cancel it immediately and proceed with this one.
//        if (mCurrentAnimator != null) {
//            mCurrentAnimator.cancel();
//        }
//        
//        // Retrieve and cache the system's default "short" animation time.
//        mShortAnimationDuration = mAct.getResources().getInteger(android.R.integer.config_shortAnimTime);
//
//        Uri imageUri = Uri.parse(pictureUri);
//        System.out.println("zoomImageFromThumb / imageUri = " + imageUri);
//		
//		// for different scheme
//		String filePath = null;
//        if ((imageUri != null) && "content".equals(imageUri.getScheme())) 
//        {
//        	String[] projection = new String[] { android.provider.MediaStore.Images.ImageColumns.DATA };
//            Cursor cursor = mAct.getContentResolver().query(imageUri, projection, null, null, null);
//            cursor.moveToFirst();   
//            filePath = cursor.getString(0);
//            cursor.close();
//        }
//        else 
//        {
//            filePath = imageUri.getPath();
//        }
//        
////		System.out.println("filePath = " + filePath);
//		ExifInterface exif = new ExifInterface(filePath);
//		String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
//		String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
//		
////		System.out.println(String.format(Locale.US, "width = %s length = %s", width, length));
//		
//		// makes sure EXIF data
//		if( (Integer.valueOf(length) > 0 ) && (Integer.valueOf(length) > 0))
//		{		// set picture
//			mExpandedImageView.setVisibility(View.VISIBLE );
//			bShowExpandedImage = true;
//
//			mExpandedImageView.setImageBitmap(decodeSampledBitmapFromUri(imageUri,
//																		 Integer.valueOf(width)/5,
//																		 Integer.valueOf(length)/5,
//																		 mAct));
//		}
//		else 
//		{
//			// image is not seen
//			mExpandedImageView.setVisibility(View.INVISIBLE);
//			bShowExpandedImage = false;
//			return;
//		}
//									
//        // Calculate the starting and ending bounds for the zoomed-in image. This step
//        // involves lots of math. Yay, math.
//        mStartBounds = new Rect();
//        final Rect finalBounds = new Rect();
//        final Point globalOffset = new Point();
//
//        // The start bounds are the global visible rectangle of the thumbnail, and the
//        // final bounds are the global visible rectangle of the container view. Also
//        // set the container view's offset as the origin for the bounds, since that's
//        // the origin for the positioning animation properties (X, Y).
//        thumbView.getGlobalVisibleRect(mStartBounds);
//        mAct.findViewById(R.id.container).getGlobalVisibleRect(finalBounds, globalOffset);
//
//        mStartBounds.offset(-globalOffset.x, -globalOffset.y);
//        finalBounds.offset(-globalOffset.x, -globalOffset.y);
//
//        // Adjust the start bounds to be the same aspect ratio as the final bounds using the
//        // "center crop" technique. This prevents undesirable stretching during the animation.
//        // Also calculate the start scaling factor (the end scaling factor is always 1.0).
//        float startScale;
//        if ((float) finalBounds.width() / finalBounds.height()
//                > (float) mStartBounds.width() / mStartBounds.height()) {
//            // Extend start bounds horizontally
//            startScale = (float) mStartBounds.height() / finalBounds.height();
//            float startWidth = startScale * finalBounds.width();
//            float deltaWidth = (startWidth - mStartBounds.width()) / 2;
//            mStartBounds.left -= deltaWidth;
//            mStartBounds.right += deltaWidth;
//        } else {
//            // Extend start bounds vertically
//            startScale = (float) mStartBounds.width() / finalBounds.width();
//            float startHeight = startScale * finalBounds.height();
//            float deltaHeight = (startHeight - mStartBounds.height()) / 2;
//            mStartBounds.top -= deltaHeight;
//            mStartBounds.bottom += deltaHeight;
//        }
//
//        // Hide the thumbnail and show the zoomed-in view. When the animation begins,
//        // it will position the zoomed-in view in the place of the thumbnail.
//        thumbView.setAlpha(0f);
//        mExpandedImageView.setVisibility(View.VISIBLE);
//
//        // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
//        // the zoomed-in view (the default is the center of the view).
//        mExpandedImageView.setPivotX(0f);
//        mExpandedImageView.setPivotY(0f);
//
//        // Construct and run the parallel animation of the four translation and scale properties
//        // (X, Y, SCALE_X, and SCALE_Y).
//        
//        // Construct and run the parallel animation of the four translation and scale properties
//        // (X, Y, SCALE_X, and SCALE_Y).
//        AnimatorSet set = new AnimatorSet();
//        set.play(ObjectAnimator.ofFloat(mExpandedImageView, View.X, mStartBounds.left,finalBounds.left))
//           .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y, mStartBounds.top,finalBounds.top))
//           .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_X, startScale, 1f))
//           .with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_Y, startScale, 1f));
//        set.setDuration(mShortAnimationDuration);
//        set.setInterpolator(new DecelerateInterpolator());
//        set.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                mCurrentAnimator = null;
//            }
//
//            @Override
//            public void onAnimationCancel(Animator animation) {
//                mCurrentAnimator = null;
//            }
//        });
//        set.start();
//        mCurrentAnimator = set;
//
//        // Upon clicking the zoomed-in image, it should zoom back down to the original bounds
//        // and show the thumbnail instead of the expanded image.
//        mStartScaleFinal = startScale;
//        mExpandedImageView.setOnClickListener(new View.OnClickListener() 
//        {
//            @Override
//            public void onClick(View view) 
//            {
//            	closeExpandImage();
//            }
//        });
//        
//    }

		

	static void showImage(ImageView mPicImageView,
						  String pictureUri,
						  final Activity mAct) throws IOException 
   {
		// show dialog for confirmation Continue or not
		mAddNewAct = mAct;

		// retrieve display dimensions
		Rect displayRectangle = new Rect();
		Window window = mAct.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

		mExpandedImageView = mPicImageView;

		// If there's an animation in progress, cancel it immediately and
		// proceed with this one.
		if (mCurrentAnimator != null) {
			mCurrentAnimator.cancel();
		}

		// Retrieve and cache the system's default "short" animation time.
		mShortAnimationDuration = mAct.getResources().getInteger(
				android.R.integer.config_shortAnimTime);

		Uri imageUri = Uri.parse(pictureUri);
		System.out.println(" UtilImage / showImage / imageUri = " + imageUri);

		// for different scheme
		String filePath = null;
		if (imageUri != null && "content".equals(imageUri.getScheme())) 
		{
			String[] projection = new String[] { android.provider.MediaStore.Images.ImageColumns.DATA };
			Cursor cursor = mAct.getContentResolver().query(imageUri,
					projection, null, null, null);
			cursor.moveToFirst();
			filePath = cursor.getString(0);
			cursor.close();
		} else 
		{
			filePath = imageUri.getPath();
		}

		// System.out.println("filePath = " + filePath);
		ExifInterface exif = new ExifInterface(filePath);
		String length = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
		String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);

//		System.out.println(String.format(Locale.US, "width = %s length = %s",width, length));

		// makes sure EXIF data
		if( (Integer.valueOf(length) > 0 ) && (Integer.valueOf(length) > 0))
		{
			// set picture
			mExpandedImageView.setVisibility(View.VISIBLE);
			bShowExpandedImage = true;			
			mExpandedImageView.setImageBitmap(decodeSampledBitmapFromUri(imageUri,
																		 Integer.valueOf(width) / 5,
																		 Integer.valueOf(length) / 5,
																		 mAct));
		}
		else
		{
			mExpandedImageView.setVisibility(View.INVISIBLE);
			bShowExpandedImage = false;
			return;
		}

		// Calculate the starting and ending bounds for the zoomed-in image.
		// This step
		// involves lots of math. Yay, math.
		mStartBounds = new Rect();
		final Rect finalBounds = new Rect();
		final Point globalOffset = new Point();

		// The start bounds are the global visible rectangle of the thumbnail,
		// and the
		// final bounds are the global visible rectangle of the container view.
		// Also
		// set the container view's offset as the origin for the bounds, since
		// that's
		// the origin for the positioning animation properties (X, Y).

		mAct.findViewById(R.id.container).getGlobalVisibleRect(finalBounds,
				globalOffset);


		mStartBounds.offset(-globalOffset.x, -globalOffset.y);
		finalBounds.offset(-globalOffset.x, -globalOffset.y);

		// Adjust the start bounds to be the same aspect ratio as the final
		// bounds using the
		// "center crop" technique. This prevents undesirable stretching during
		// the animation.
		// Also calculate the start scaling factor (the end scaling factor is
		// always 1.0).
		float startScale;
		if ((float) finalBounds.width() / finalBounds.height() > (float) mStartBounds
				.width() / mStartBounds.height()) 
		{
			// Extend start bounds horizontally
			startScale = (float) mStartBounds.height() / finalBounds.height();
			float startWidth = startScale * finalBounds.width();
			float deltaWidth = (startWidth - mStartBounds.width()) / 2;
			mStartBounds.left -= deltaWidth;
			mStartBounds.right += deltaWidth;
		} else 
		{
			// Extend start bounds vertically
			startScale = (float) mStartBounds.width() / finalBounds.width();
			float startHeight = startScale * finalBounds.height();
			float deltaHeight = (startHeight - mStartBounds.height()) / 2;
			mStartBounds.top -= deltaHeight;
			mStartBounds.bottom += deltaHeight;
		}

		// Hide the thumbnail and show the zoomed-in view. When the animation
		// begins,
		// it will position the zoomed-in view in the place of the thumbnail.
//		thumbView.setAlpha(0f);
		mExpandedImageView.setVisibility(View.VISIBLE);

		// Set the pivot point for SCALE_X and SCALE_Y transformations to the
		// top-left corner of
		// the zoomed-in view (the default is the center of the view).
		mExpandedImageView.setPivotX(0f);
		mExpandedImageView.setPivotY(0f);

		// Construct and run the parallel animation of the four translation and
		// scale properties
		// (X, Y, SCALE_X, and SCALE_Y).

		// Construct and run the parallel animation of the four translation and
		// scale properties
		// (X, Y, SCALE_X, and SCALE_Y).
		AnimatorSet set = new AnimatorSet();
		set.play(
				ObjectAnimator.ofFloat(mExpandedImageView, View.X,
						mStartBounds.left, finalBounds.left))
				.with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y,
						mStartBounds.top, finalBounds.top))
				.with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_X,
						startScale, 1f))
				.with(ObjectAnimator.ofFloat(mExpandedImageView, View.SCALE_Y,
						startScale, 1f));
		set.setDuration(mShortAnimationDuration);
		set.setInterpolator(new DecelerateInterpolator());
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				mCurrentAnimator = null;
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				mCurrentAnimator = null;
			}
		});
		set.start();
		mCurrentAnimator = set;

		// Upon clicking the zoomed-in image, it should zoom back down to the
		// original bounds
		// and show the thumbnail instead of the expanded image.
		mStartScaleFinal = startScale;
		mExpandedImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				closeExpandedImage();
			}
		});

}


    public static void closeExpandedImage()
    {
    	System.out.println("closeExpandImage");
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Animate the four positioning/sizing properties in parallel, back to their
        // original values.
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(mExpandedImageView, View.X, mStartBounds.left))
                .with(ObjectAnimator.ofFloat(mExpandedImageView, View.Y, mStartBounds.top))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView, View.SCALE_X, mStartScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(mExpandedImageView, View.SCALE_Y, mStartScaleFinal));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            	if(mThumbView != null)
            		mThumbView.setAlpha(1f);
                mExpandedImageView.setVisibility(View.GONE);
            	bShowExpandedImage = false;
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if(mThumbView != null)
                	mThumbView.setAlpha(1f);
                mExpandedImageView.setVisibility(View.GONE);
            	bShowExpandedImage = false;
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }
    
    public static Bitmap decodeSampledBitmapFromUri(Uri uri, int reqWidth, int reqHeight, Activity mAct) throws IOException 
    {
    	Bitmap thumbNail;
    	ContentResolver cr = mAct.getContentResolver();
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeStream(cr.openInputStream(uri), null, options);//??? needed?

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
		thumbNail = BitmapFactory.decodeStream(cr.openInputStream(uri), null, options);
		
		if(thumbNail != null)
		{
			// scaled
			thumbNail = Bitmap.createScaledBitmap(thumbNail, reqWidth,reqHeight, true);
			// rotate bitmap
			thumbNail = Bitmap.createBitmap(thumbNail, 0, 0, reqWidth, reqHeight, getMatrix(uri), true);
		}

		return thumbNail;
    }
    
    public static Matrix getMatrix(Uri imageUri) throws IOException
    {
		Matrix matrix = new Matrix();

		ExifInterface exif = new ExifInterface(imageUri.getPath());
		int rotSetting = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
		int rotInDeg;
		
	    if (rotSetting == ExifInterface.ORIENTATION_ROTATE_90) 
	      rotInDeg = 90;  
	    else if(rotSetting == ExifInterface.ORIENTATION_ROTATE_180) 
	      rotInDeg = 180;  
	    else if (rotSetting == ExifInterface.ORIENTATION_ROTATE_270) 
	      rotInDeg = 270;
	    else
	      rotInDeg =0; 
		
	    System.out.println("rotInDeg = " + rotInDeg);
	    
		if (rotSetting != 0f) {matrix.preRotate(rotInDeg);}
    	
		return matrix;
    }
    
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int width = options.outWidth;
        final int height = options.outHeight;
        
//        System.out.println("bitmap height = " + height);
//        System.out.println("bitmap width = " + width);
        
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }
        
        return inSampleSize;
    }
}