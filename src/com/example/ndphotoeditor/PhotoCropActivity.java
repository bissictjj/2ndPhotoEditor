package com.example.ndphotoeditor;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoCropActivity extends Activity {
	ImageView imgView;
	Bitmap cImg;
	Uri imgUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_crop);
		imgView = (ImageView)findViewById(R.id.imgCropView);
		imgUri = getIntent().getData();
		int width = getIntent().getIntExtra("width", 0);
		int height = getIntent().getIntExtra("height", 0);
		int cX = getIntent().getIntExtra("x", 0);
		int cY = getIntent().getIntExtra("y", 0);
		Button btnBack = (Button)findViewById(R.id.btnBack);
		try {
			cImg = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgUri);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Bitmap cPic = Bitmap.createBitmap(cImg,cX,cY,width,height);
		imgView.setImageBitmap(cPic);
		Log.d("Measurements", "w/h/cx/cy :"+ width+ "/"+height+"/"+cX+"/"+cY);
			
		
		
		btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	Intent i = new Intent(PhotoCropActivity.this,PhotoSelectorActivity.class);
            	startActivity(i);
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.photo_crop, menu);
		return true;
	}
	
	public static Bitmap loadBitmapFromView(View v) {
	     Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);                
	     Canvas c = new Canvas(b);
	     v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
	     v.draw(c);
	     return b;
	}

}
