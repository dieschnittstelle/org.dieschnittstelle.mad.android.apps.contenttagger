package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
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
 * <p>
 * encapsulates control of an attachments panel which allows to associate taggables with taggables, starting with media
 * for using gridview see https://developer.android.com/guide/topics/ui/layout/gridview.html
 *
 * for drag&drop handling, see: https://blahti.wordpress.com/2011/10/03/drag-drop-for-android-gridview/
 */
public class AttachmentsPanelController {

    protected static String logger = "AttachmentsPanelController";

    public static final int REQUEST_ATTACH_MEDIA = 10;

    private Taggable taggable;
    private GridView mediaGrid;
    private FloatingActionButton addMediaAction;
    private MediaAdapter mediaAdapter;
    private Fragment owner;
    private View attachmentsPanel;

    public AttachmentsPanelController(Fragment owner, View parentView) {

        this.owner = owner;
        this.mediaGrid = (GridView) parentView.findViewById(R.id.mediaGrid);
        this.addMediaAction = (FloatingActionButton) parentView.findViewById(R.id.addMedia);
        this.attachmentsPanel = parentView.findViewById(R.id.attachmentsPanel);

        // add a click listener on the action
        addMediaAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMedia();
            }
        });

        // populate the view
        mediaAdapter = new MediaAdapter(owner.getActivity());
        mediaGrid.setAdapter(mediaAdapter);


    }

    public void setEditable(boolean editable) {
        addMediaAction.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void addMedia() {
        Log.i(logger, "addMedia()");
//        // we create a new media element and add it to the taggable
//        final Media newMedia = new Media();
//        // some random stuff
//        long currentTime = System.currentTimeMillis();
//        newMedia.setTitle(String.valueOf(currentTime));
//
//        String contentUri = "http://lorempixel.com/" + (400 / ((currentTime % 4)+1)) + "/" + (300 / ((currentTime % 3)+1));
//        Log.i(logger,"setting randomised contentUri: " + contentUri);
//        newMedia.setContentUri(contentUri);
//
//        // we add the media as attachment
//        AttachmentsPanelController.this.taggable.addAttachment(newMedia);
//
//        // ... and show it in the grid
//        mediaAdapter.addItem(newMedia);

        Intent filePickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        filePickerIntent.setType("*/*");
        filePickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        owner.startActivityForResult(filePickerIntent, REQUEST_ATTACH_MEDIA);

    }

    public void addMediaItemWithUri(Uri uri, Media.ContentType contentType) {
        Media media = new Media();
        media.setContentUri(String.valueOf(uri));
        media.setContentType(contentType);

        this.taggable.addAttachment(media);
        mediaAdapter.addItem(media);
    }

    public void bindTaggable(Taggable taggable) {
        this.taggable = taggable;
        mediaAdapter.clear();
        Log.i(logger, "bindTaggable(): taggable has attachments: " + taggable.getAttachments());
        // we add the items to the adapter
        if (taggable.getAttachments().size() > 0) {
//            attachmentsPanel.setVisibility(View.VISIBLE);
            mediaAdapter.addItems((List) taggable.getAttachments());
        }
        // otherwise, we set ourselves to GONE
        else {
//            attachmentsPanel.setVisibility(View.GONE);
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
            Log.i(logger, "getView(): position: " + position);

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
            mediaItem.createThumbnail(owner.getActivity(), new Media.OnThumbnailCreatedHandler() {
                @Override
                public void onThumbnailCreated(Bitmap thumbnail) {
                    imageView.setImageBitmap(thumbnail);
                }
            });

            return imageView;
        }

        public void addItem(Media media) {
            this.items.add(media);
            super.notifyDataSetChanged();
        }

        public void addItems(Collection<Media> media) {
            if (media.size() > 0) {
                Log.i(logger,"addItems(): thumbnail on 1st element of media list is: " + media.iterator().next().getThumbnail());
            }
            this.items.addAll(media);
            super.notifyDataSetChanged();
        }

        public void clear() {
            this.items.clear();
            super.notifyDataSetChanged();
        }

    }

    // TODO: this is kindof adhoc, could be generalised
    public boolean handlesResult(int requestCode) {
        return (requestCode == REQUEST_ATTACH_MEDIA);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(logger, "onActivityResult()");

        if (requestCode == REQUEST_ATTACH_MEDIA) {
            if (data != null) {
                ClipData clipData = data.getClipData();
                // this is the multiple select case
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item path = clipData.getItemAt(i);
                        Log.i(logger, "selectedImage (multiple): " + String.valueOf(path.getUri()));
                        addMediaItemWithUri(path.getUri(), Media.ContentType.LOCALURI);
                    }
                } else {
                    Uri selectedImage = data.getData();
                    Log.i(logger, "selectedImage (single): " + selectedImage);
                    addMediaItemWithUri(selectedImage, Media.ContentType.LOCALURI);
                }
            }
        }
    }

}
