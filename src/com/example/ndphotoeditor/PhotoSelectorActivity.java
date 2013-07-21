package com.example.ndphotoeditor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class PhotoSelectorActivity extends Activity {
private static final String TAG = "IoGallery";
    
    // TODO: place LruCache into a Loader, onRetain(), or static to keep across config changes

    private static final int LOADER_CURSOR = 1;

    private ThumbnailCache mCache;
    private boolean mCacheEnabled;
    
    private PhotoAdapter mAdapter;
    private GridView mGridView;
    
    private class PhotoAdapter extends CursorAdapter {
    	        
        public PhotoAdapter(Context context) {
            super(context, null, false);
        }


        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final long photoId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));

            final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);

            // Cancel any pending thumbnail task, since this view is now bound
            // to new thumbnail
            final ThumbnailAsyncTask oldTask = (ThumbnailAsyncTask) imageView.getTag();
            if (oldTask != null) {
                oldTask.cancel(false);
            }

            if (mCacheEnabled) {
                // Cache enabled, try looking for cache hit
                final Bitmap cachedResult = mCache.get(photoId);
                if (cachedResult != null) {
                    imageView.setImageBitmap(cachedResult);
                    return;
                }
            }

            // If we arrived here, either cache is disabled or cache miss, so we
            // need to kick task to load manually
            final ThumbnailAsyncTask task = new ThumbnailAsyncTask(imageView);
            imageView.setImageBitmap(null);
            imageView.setTag(task);
            task.execute(photoId);
        }
    }

    public class ThumbnailAsyncTask extends AsyncTask<Long, Void, Bitmap> {
        private final ImageView mTarget;

        public ThumbnailAsyncTask(ImageView target) {
            mTarget = target;
        }

        @Override
        protected void onPreExecute() {
            mTarget.setTag(this);
        }

        @Override
        protected Bitmap doInBackground(Long... params) {
            final long id = params[0];

            final Bitmap result = MediaStore.Images.Thumbnails.getThumbnail(
                    getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND, null);

            // When cache enabled, keep reference to this bitmap
            if (mCacheEnabled) {
                mCache.put(id, result);
            }

            return result;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mTarget.getTag() == this) {
                mTarget.setImageBitmap(result);
                mTarget.setTag(null);
            }
        }        
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_photo_selector);

        // Pick cache size based on memory class of device
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
        mCache = new ThumbnailCache(memoryClassBytes / 2);

        mAdapter = new PhotoAdapter(this);

        mGridView = (GridView) findViewById(android.R.id.list);
        mGridView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setAdapter(mAdapter);

        mGridView.setRecyclerListener(new RecyclerListener() {
            
        	@Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
                imageView.setImageBitmap(null);
            }
        });

        mGridView.setOnItemClickListener(mPhotoClickListener);

        // Kick off loader for Cursor with list of photos
        getLoaderManager().initLoader(LOADER_CURSOR, null, mCursorCallbacks);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter.swapCursor(null);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.v(TAG, "onTrimMemory() with level=" + level);

        // Memory we can release here will help overall system performance, and
        // make us a smaller target as the system looks for memory

        if (level >= TRIM_MEMORY_MODERATE) { // 60
            // Nearing middle of list of cached background apps; evict our
            // entire thumbnail cache
            Log.v(TAG, "evicting entire thumbnail cache");
            mCache.evictAll();

        } else if (level >= TRIM_MEMORY_BACKGROUND) { // 40
            // Entering list of cached background apps; evict oldest half of our
            // thumbnail cache
            Log.v(TAG, "evicting oldest half of thumbnail cache");
            mCache.trimToSize(mCache.size() / 2);
        }
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.photo, menu);
		return true;
	}
	
	//@Override
    /*public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_cache: {
                mCacheEnabled = !item.isChecked();
                item.setChecked(mCacheEnabled);
                mCache.evictAll();
                mStats.setVisibility(mCacheEnabled ? View.VISIBLE : View.GONE);
                return true;
            }
            case R.id.menu_transaction: {
                mTransactionEnabled = !item.isChecked();
                item.setChecked(mTransactionEnabled);
                return true;
            }
        }
        return false;
    }*/

    private AdapterView.OnItemClickListener mPhotoClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // User clicked on photo, open our viewer
            final Intent intent = new Intent(PhotoSelectorActivity.this, PhotoEditActivity.class);
            final Uri data = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            intent.setData(data);
            startActivity(intent);
        }
    };

    private final LoaderCallbacks<Cursor> mCursorCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final String[] columns = { BaseColumns._ID }; 
            return new CursorLoader(PhotoSelectorActivity.this,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }

    };

    /**
     * Simple extension that uses {@link Bitmap} instances as keys, using their
     * memory footprint in bytes for sizing.
     */
    public static class ThumbnailCache extends LruCache<Long, Bitmap> {
        public ThumbnailCache(int maxSizeBytes) {
            super(maxSizeBytes);
        }
        
        @Override
        protected int sizeOf(Long key, Bitmap value) {
            return value.getByteCount();
        }
    }
  }



