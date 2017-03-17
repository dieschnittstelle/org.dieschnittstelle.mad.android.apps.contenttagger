package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.blahti.example.drag3.DragController;
import com.blahti.example.drag3.DragLayer;
import com.blahti.example.drag3.DragSource;
import com.blahti.example.drag3.DropTarget;
import com.blahti.example.drag3.ImageCell;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static android.content.res.Configuration.*;

/**
 * Created by master on 14.03.17.
 * <p>
 * encapsulates control of an attachments panel which allows to associate taggables with taggables, starting with media
 * for using gridview see https://developer.android.com/guide/topics/ui/layout/gridview.html
 * <p>
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
    private boolean editable = false;

    // D&D
    private DragController dragController;
    private DragLayer dragLayer;

    // the delete button, which will be displayed on d&d being activated
    private ControlElement deleteMediaControl;

    public AttachmentsPanelController(Fragment owner, View parentView, final boolean editable) {

        this.owner = owner;
        this.editable = editable;
        this.mediaGrid = (GridView) parentView.findViewById(R.id.mediaGrid);
        Log.i(logger, "mediaGrid: " + mediaGrid);
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

        mediaGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(logger, "onLongClick()");

                if (editable) {
                    startDrag(view);
                }

                return true;
            }
        });

        mediaGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                Bundle args = MainNavigationControllerActivity.createArguments(MediaPagerFragment.ARG_SELECTED_MEDIA_POS, pos);
                List<Long> mediaIds = new ArrayList<Long>();
                for (int j = 0; j < mediaAdapter.getCount(); j++) {
                    mediaIds.add(((Media) mediaAdapter.getItem(j)).getId());
                }
                args.putSerializable(MediaPagerFragment.ARG_DISPLAY_MEDIA, (ArrayList) mediaIds);
                ((MainNavigationControllerActivity) AttachmentsPanelController.this.owner.getActivity()).showView(MediaPagerFragment.class, args, true);
            }
        });


        // instantiate drag&drop handling
        if (this.editable) {
            dragController = new DragController(owner.getActivity());
            dragLayer = (DragLayer) attachmentsPanel.findViewById(R.id.drag_layer);
            dragLayer.setDragController(dragController);
            dragLayer.setGridView(mediaGrid);
            dragController.setDragListener(dragLayer);
            dragLayer.setOnDropListener(new DragLayer.OnDropListener() {

                @Override
                public void onDrop(View source, View target, boolean allowed) {
                    Log.i(logger, "onDrop(): " + source);

                    mediaAdapter.removeControlView(deleteMediaControl);

                    // check whether the target is the delete button
                    if (target == deleteMediaControl.controlView) {
                        int sourcepos = (int) source.getTag(R.string.tag_position);
                        mediaAdapter.removeItem(sourcepos);
                    } else {
                        // we obtain the positions of the two elements by reading out the tags
                        int sourcepos = (int) source.getTag(R.string.tag_position);
                        int targetpos = (int) target.getTag(R.string.tag_position);
                        if (targetpos > -1 && targetpos != sourcepos) {
                            // we will not swap positions, but insert the source *in front* of the target
                            mediaAdapter.insertItemBefore(sourcepos, targetpos);
                        }
                    }
                }

                @Override
                public void onDragEnd() {
                    Log.i(logger, "onDragEnd()");

                    mediaAdapter.removeControlView(deleteMediaControl);
                }

            });
        }

        deleteMediaControl = new ControlElement(this.attachmentsPanel.findViewById(R.id.deleteMedia));
        // remove it
        ((ViewGroup) deleteMediaControl.controlView.getParent()).removeView(deleteMediaControl.controlView);

        addMediaAction.setVisibility(this.editable ? View.VISIBLE : View.GONE);

    }


    private void addMedia() {
        Log.i(logger, "addMedia()");

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

    public int getColumnCountForAdapter() {
        int colcount = owner.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE ? 6 : 4;
        Log.d(logger, "getColumnCountForAdapter(): " + colcount);

        return colcount;
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
//            Log.d(logger,"getItem(): " + position + " from " + items);
            return items.get(position);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        // this is for dealing with d&d
        public void swapItemPositions(int from, int to) {
            Collections.swap(items, from, to);
            super.notifyDataSetChanged();
        }

        public void insertItemBefore(int from, int before) {
            Media move = items.get(from);
            items.remove(move);
            items.add(before, move);
            // we re-set the items on the media
            taggable.setAttachments((List) items);
            super.notifyDataSetChanged();
        }

        public void removeItem(int pos) {
            Media remove = items.get(pos);
            items.remove(remove);
            taggable.setAttachments((List) items);
            super.notifyDataSetChanged();
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(final int position, View convertView, ViewGroup parent) {
//            Log.i(logger, "getView(): position: " + position);

            Media mediaItem = items.get(position);

            final ImageView imageView;

            // if the element is a control element, we return its view
            if (mediaItem instanceof ControlElement) {
                Log.d(logger, "returning view for mediaItem: " + ((ControlElement) mediaItem).controlView);
                return ((ControlElement) mediaItem).controlView;
            }
            // do not recycle the control view
            else if (convertView == deleteMediaControl.controlView) {
                convertView = null;
            }

            if (convertView == null) {
                // if it's not recycled, initialize some attributes - depending on whether we are editable or not we will return ImageCell or normal ImageView objects
                if (editable) {
                    imageView = new ImageCell(mContext);
                    // we need to set the cell to false, otherwise dragging will not be started
                    ((ImageCell) imageView).mEmpty = false;
                    ((ImageCell) imageView).onDragEnteredColor = R.color.primary_action_selected;
                    ((ImageCell) imageView).onDragLeftColor = android.R.color.transparent;
                } else {
                    imageView = new ImageView(mContext);
                }

                imageView.setLayoutParams(new GridView.LayoutParams(285, 285));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageBitmap(null);
            mediaItem.loadThumbnail(owner.getActivity(), new Media.OnImageLoadedHandler() {
                @Override
                public void onImageLoaded(Bitmap thumbnail) {
                    imageView.setImageBitmap(thumbnail);
                }
            });

            imageView.setTag(R.string.tag_position, position);

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

        public void clear() {
            this.items.clear();
            super.notifyDataSetChanged();
        }

        public void addControlView(ControlElement view) {
            this.items.add(view);
            super.notifyDataSetChanged();
        }

        public void removeControlView(ControlElement view) {
            this.items.remove(view);
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

    public boolean startDrag(View v) {
        Log.d(logger, "startDrag()");
        DragSource dragSource = (DragSource) v;

        // we add the dragstart to the gridview

        // We are starting a drag. Let the DragController handle it.
        dragController.startDrag(v, dragSource, dragSource, DragController.DRAG_ACTION_MOVE);
        mediaAdapter.addControlView(deleteMediaControl);
        dragLayer.addDropTarget((DropTarget) deleteMediaControl.controlView);

        return true;
    }

    // as we use an adapter view, we need a solution for representing control elements for d&d
    public class ControlElement extends Media {

        public View controlView;

        public ControlElement(View controlView) {
            this.controlView = controlView;
            ((ImageCell) this.controlView).mEmpty = false;
            ((ImageCell) this.controlView).onDragEnteredColor = R.drawable.circle_secondary_filled;
            ((ImageCell) this.controlView).onDragLeftColor = R.drawable.circle_primary_filled;
        }

        // not draggable
        public boolean allowDrag() {
            return false;
        }

        public String toString() {
            return "{ControlElement}";
        }

    }

}
