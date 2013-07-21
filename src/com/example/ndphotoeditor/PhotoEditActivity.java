package com.example.ndphotoeditor;

import java.io.File;
import java.util.ArrayList;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoEditActivity extends Activity implements OnGesturePerformedListener {
	private GestureLibrary mLibrary;
	Bitmap pic;
	ImageView imgView;
	Uri imgUri;
	File mStoreFile;
	int mode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_edit);
		mode = 0;
		mStoreFile = new File(Environment.getExternalStorageDirectory(), "gestures");
		mLibrary = GestureLibraries.fromFile(mStoreFile);
        if (!mLibrary.load()) {
        	finish();
        }
        
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.addOnGesturePerformedListener(this);
		
		imgUri = getIntent().getData();
		imgView = (ImageView)findViewById(R.id.imgEditView);
		imgView.setImageURI(imgUri);
		pic = loadBitmapFromView(imgView);
		Button btnRotate = (Button)findViewById(R.id.btnRotate);
		Button btnCrop = (Button)findViewById(R.id.btnCrop);
		
		btnRotate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	pic = loadBitmapFromView(imgView);
                imgView.setImageBitmap(rotate(pic, 90));
            }
        });
		
		btnCrop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
			}
		});		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.photo_edit, menu);
		return true;
	}
	
	public static Bitmap rotate(Bitmap src, float degree) {
	    // create new matrix
	    Matrix matrix = new Matrix();
	    // setup rotation degree
	    matrix.postRotate(degree);
	 
	    // return new bitmap rotated using matrix
	    return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
	}
	
	public static Bitmap loadBitmapFromView(View v) {
	     Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);                
	     Canvas c = new Canvas(b);
	     v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
	     v.draw(c);
	     return b;
	}
	
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
		pic = loadBitmapFromView(imgView);
		if (predictions.size() > 0) {
			Prediction prediction = predictions.get(0);
			// We want at least some confidence in the result
			if (prediction.score > 1.0) {
				if(prediction.name.matches("back") && mode !=0)
				{
					mode = 0;
					Toast.makeText(getApplicationContext(), "Choose Mode", Toast.LENGTH_LONG).show();
					
				}else
					if(mode != 0){
						if(mode == 1){
							//Cropping Mode
							RectF rec = gesture.getBoundingBox();
							float height = rec.height();
							float width = rec.width();
							float cX = (float) ((rec.centerX()) - (0.5)*width);
							float cY = (float)((rec.centerY())-(0.5)*height);
							Bitmap cPic = Bitmap.createBitmap(loadBitmapFromView(imgView),(int)cX,(int)cY,(int)width,(int)height);
							imgView.setImageBitmap(cPic);
						}if(mode == 2){
							//Rotating Mode
							if (predictions.size() > 0) {
								//prediction = predictions.get(0);
								// We want at least some confidence in the result
								if (prediction.score > 1.0) {
									if(prediction.name.matches("rotateleft"))
									{
										imgView.setImageBitmap(rotate(pic,270));
									}
									if(prediction.name.matches("rotateright"))
									{
										imgView.setImageBitmap(rotate(pic,90));
									}
								}
							}
						}	
					}else if(mode == 0){
						// We want at least one prediction
						if (predictions.size() > 0) {
							//Prediction prediction = predictions.get(0);
							// We want at least some confidence in the result
							if (prediction.score > 1.0) {	
								//Toast.makeText(getApplicationContext(), prediction.name, Toast.LENGTH_LONG).show();
								if(prediction.name.matches("crop"))
								{
									mode = 1;
									Toast.makeText(getApplicationContext(), "Crop Mode Enabled", Toast.LENGTH_LONG).show();
								}else if(prediction.name.matches("rotate"))
								{
									mode =2;
									Toast.makeText(getApplicationContext(), "Rotate Mode Enabled", Toast.LENGTH_LONG).show();
								}
								
							}
						}
					}
				}
		}
	}
}
			
		


