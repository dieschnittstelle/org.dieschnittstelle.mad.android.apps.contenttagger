package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by master on 14.03.17.
 *
 * encapsulates control of an attachments panel which allows to associate taggables with taggables, starting with media
 * for using gridview see https://developer.android.com/guide/topics/ui/layout/gridview.html
 *
 */
public class AttachmentsPanelController {

    protected static String logger = "AttachmentsPanelController";

    private Taggable taggable;
    private GridView mediaGrid;
    private FloatingActionButton addMediaAction;
    private MediaAdapter mediaAdapter;
    private Context owner;

    public AttachmentsPanelController(Context owner,View parentView) {

        this.owner = owner;
        this.mediaGrid = (GridView)parentView.findViewById(R.id.mediaGrid);
        this.addMediaAction = (FloatingActionButton) parentView.findViewById(R.id.addMedia);

        // add a click listener on the action
        addMediaAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMedia();
                // well, let's see whether creation of attachent will be done automatically...
//                new AsyncTask<Taggable,Void,Taggable>() {
//
//                    @Override
//                    protected Taggable doInBackground(Taggable... taggables) {
//                        // well, we create a newMedia element
//                        newMedia.createSync();
//                        return newMedia;
//                    }
//
//                    @Override
//                    protected void onPostExecute(Taggable taggable) {
//                        AttachmentsPanelController.this.taggable.addAttachment(taggable);
//                    }
//
//                }.execute(newMedia);
            }
        });

        // populate the view
        mediaAdapter = new MediaAdapter(owner);
        mediaGrid.setAdapter(mediaAdapter);


    }

    public void setEditable(boolean editable) {
        addMediaAction.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void addMedia() {
        Log.i(logger,"addMedia()");
        // we create a new media element and add it to the taggable
        final Media newMedia = new Media();
        // some random stuff
        long currentTime = System.currentTimeMillis();
        newMedia.setTitle(String.valueOf(currentTime));

        String contentUri = "http://lorempixel.com/" + (400 / ((currentTime % 4)+1)) + "/" + (300 / ((currentTime % 3)+1));
        Log.i(logger,"setting randomised contentUri: " + contentUri);
        newMedia.setContentUri(contentUri);

        // we add the media as attachment
        AttachmentsPanelController.this.taggable.addAttachment(newMedia);

        // ... and show it in the grid
        mediaAdapter.addItem(newMedia);
    }

    public void bindTaggable(Taggable taggable) {
        this.taggable = taggable;
        Log.i(logger,"bindTaggable(): taggable has attachments: " + taggable.getAttachments());
        // we add the items to the adapter
        if (taggable.getAttachments().size() > 0) {
            mediaAdapter.addItems((List) taggable.getAttachments());
        }
    }

    /*
     * adapter class, taken from https://developer.android.com/guide/topics/ui/layout/gridview.html
     */
    public class MediaAdapter extends BaseAdapter {
        private Context mContext;

        private List<Media> items = new ArrayList<Media>();

        public MediaAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return items.size();
        }

        public Object getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i(logger,"getView(): position: " + position);

            final ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(285, 285));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            Media mediaItem = items.get(position);

            // check whether the item already has a thumbnail set
            Bitmap thumbnail = mediaItem.getThumbnail();
            if (thumbnail != null) {
                imageView.setImageBitmap(thumbnail);
            }
            // otherwise we load the img asynchronously
            else {
                new AsyncTask<Media,Void,Bitmap>() {

                    private Media mediaItem;

                    @Override
                    protected Bitmap doInBackground(Media... medias) {
                        this.mediaItem = medias[0];
                        Log.i(logger,"loading image data for media item with url: " + mediaItem.getContentUri());
                        try {
                            URL url = new URL(mediaItem.getContentUri());
                            return BitmapFactory.decodeStream(url.openConnection() .getInputStream());
                        }
                        catch (Exception e) {
                            Log.e(logger,"got exception trying to load media: " + e,e);
                            return null;
                        }
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null) {
                            mediaItem.setThumbnail(bitmap);
                            Log.i(logger,"setting image data on image view: " + imageView);
                            imageView.setImageBitmap(bitmap);
                        }
                    }
                }.execute(mediaItem);
            }

            return imageView;
        }

        public void addItem(Media media) {
            this.items.add(media);
            super.notifyDataSetChanged();
        }

        public void addItems(Collection<Media> media) {
            this.items.addAll(media);
            super.notifyDataSetChanged();
        }

    }
}
