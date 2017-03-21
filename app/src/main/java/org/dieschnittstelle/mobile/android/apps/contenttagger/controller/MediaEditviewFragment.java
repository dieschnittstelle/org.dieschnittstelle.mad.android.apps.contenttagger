package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.databinding.BindingConversion;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.databinding.MediaEditviewBinding;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;

/**
 * Created by master on 17.03.16.
 *
 * TODO: allow to edit images in Photo-App onclick of the ImageView
 */
public class MediaEditviewFragment extends Fragment implements EventGenerator, EventListenerOwner, MainNavigationControllerActivity.OnBackListener {

    protected static String logger = "MediaEditviewFragment";

    // TODO: provide some generic component for supporting mode (at least defining enum and argument constants)
    public static enum Mode {READ, EDIT};

    /*
     * we expect that the id of the item to be displayed is passed to us, rather than the item itself...
     */
    public static final String ARG_MEDIA_ID = TaggableOverviewFragment.OUTARG_SELECTED_ITEM_ID;

    public static final int REQUEST_PICK_IMAGE = 1;

    /*
     * check whether we have a linkUrl, which is the case if we are called for some send actiokn
     */
    public static final String ARG_MEDIA_CONTENT_URI = "mediaContentUri";

    public static final String ARG_CALLED_FROM_SEND = "calledFromSend";

    public static final String ARG_MODE = "mode";

    /*
     * the ui elements
     */
    protected EditText title;
    protected EditText description;
    protected MediaEditviewBinding binding;
    protected ImageView mediaContent;
    protected TagsbarController tagsbarController;
    protected View contentView;

    /*
     * the model object that we use and its savepoint that represent the state when entering this view
     */
    protected Media media;
    public Media savepoint;

    /*
     * the mode of the view (edit mode is default)
     */
    protected Mode mode = Mode.EDIT;

    /*
     * whether we are called from handling a send actions
     */
    protected boolean calledFromSend;

    /*
     * we need to handle obsoletion of readview (which is a subclass) here...
     */
    protected boolean obsolete;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calledFromSend = getArguments().getBoolean(ARG_CALLED_FROM_SEND);
        if (getArguments().containsKey(ARG_MODE)) {
            this.mode = (Mode)getArguments().getSerializable(ARG_MODE);
        }

        addEventListeners();

        setHasOptionsMenu(true);
    }

    protected void addEventListeners() {
        // set listeners
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.DELETED,Event.CRUD.UPDATED,Event.CRUD.CREATED), Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                if (media == event.getData()) {
                    // we need to distinguish between being called from send and being called from somewhere else
                    if (calledFromSend) {
                        Log.i(logger,"onEvent(): we have been called from send. Show the links overview...");
                       // ((MainNavigationControllerActivity)getActivity()).showView(LinksOverviewFragment.class, null, true);
                    }
                    else {
                        Log.i(logger,"onEvent(): we have not been called from send. PopBackStack()...");
                        getFragmentManager().popBackStack();
                    }
                }
                else {
                    Log.i(logger, "onEvent(): got " + event.getType() + " event for link, but it involves a different object than the one being edited: " + event.getData() + ". Ignore...");
                }
            }
        });

        // also handle read events that are generated by ourselves
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READ, Media.class, this), true, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                media = event.getData();
                ((ActionBarActivity) getActivity()).setTitle(media.getTitle());
                binding.setMedia(media);
                savepoint = (Media)media.shallowClone();
//                title.setText(media.getTitle());
//                description.setText(media.getDescription());

                if (media.getContentUri() != null) {
                    MediaPagerFragment.loadMediaIntoImageView(getActivity(), media, contentView, mediaContent, MediaPagerFragment.FLAG_LOAD_THUMBNAIL | MediaPagerFragment.FLAG_LOAD_IMAGE);
                }

                tagsbarController.bindTaggable(media);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        if (obsolete) {
            Log.i(logger,"view is obsolete.");
        }
        else {
            Log.d(logger, "onResume(): " + this.getClass());
            Log.d(logger, "onResume(): media is: " + this.media);
            // we read out the id from the arguments
            long mediaId = -1;
            if (getArguments().containsKey(ARG_MEDIA_ID)) {
                mediaId = getArguments().getLong(ARG_MEDIA_ID);
            }
            if (mediaId > -1) {
                Log.d(logger,"onResume(): we have been passed a non empty mediaId: " + mediaId);
                // read all notes - reaction will be dealt with by event handler
                Media.read(Media.class, mediaId, this);
            } else if (this.media == null) {
                Log.d(logger,"onResume(): no mediaId has been passed. Create new media element.");
                ((ActionBarActivity) getActivity()).setTitle(R.string.title_create_media);
                media = new Media();
                binding.setMedia(media);
                // check whether we have been passed a url
                if (getArguments().containsKey(ARG_MEDIA_CONTENT_URI)) {
                    Log.d(logger, "onResume(): mediaContentUri has been sent. Set it...");
                    media.setContentUri(getArguments().getString(ARG_MEDIA_CONTENT_URI));
                    // we set the url view here rather than using event dispatching...
                    mediaContent.setImageURI(Uri.parse(getArguments().getString(ARG_MEDIA_CONTENT_URI)));
                }
                // this is required in order for tagging to be available for create
                tagsbarController.bindTaggable(media);
            }

            // we instantiate the reusable wrapper for the add tag dialogs - needs to be done for each onResume() as settings might have changed in the meantime
            AddTagDialogController.getInstance().attach(getActivity());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LifecycleHandling.onDestroy(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.binding = MediaEditviewBinding.inflate(inflater);
        this.contentView = binding.getRoot();//inflater.inflate(R.layout.media_editview,container,false);

        this.binding.setMode(this.mode);

//        this.title = (EditText)contentView.findViewById(R.id.title);
//        this.description = (EditText)contentView.findViewById(R.id.description);
        this.mediaContent = (ImageView)contentView.findViewById(R.id.mediaContent);
        this.tagsbarController = new TagsbarController(this,(ViewGroup)contentView.findViewById(R.id.tagsbar),R.layout.tagsbar_itemview);

        // set a listener on the media content element
        this.mediaContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPhotoInPhotoApp(Uri.parse(media.getContentUri()));
            }
        });

        return contentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (this.mode == Mode.READ) {
            inflater.inflate(R.menu.menu_media_editview_read,menu);
        }
        else {
            inflater.inflate(R.menu.menu_media_editview,menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // we check the known options
        switch (item.getItemId()) {
            // this action triggers switching of the mode
            case R.id.action_edit:
                switchMode(Mode.EDIT);
                return true;
            case R.id.action_save:
                // bind the data from the input form to the item
//                media.setTitle(this.title.getText().toString());
//                media.setDescription(this.description.getText().toString());
                if (media.created()) {
                    media.update();
                }
                else {
                    media.create();
                }
                return true;
            case R.id.action_delete:
                if (media.created()) {
                    media.delete();
                }
                return true;
            case R.id.action_pick:
                // use the storage access framework for obtaining permanent links to content - seems this cannot be applied to share-actions initiated on the content items themselves
                Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                openIntent.addCategory(Intent.CATEGORY_OPENABLE);
                openIntent.setType("image/*");
                startActivityForResult(openIntent, REQUEST_PICK_IMAGE);
                break;
            case R.id.action_add_tag:
                AddTagDialogController.getInstance().show(media);
                break;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            this.media.setContentUri(selectedImage.toString());
            this.media.setContentType(Media.ContentType.LOCALURI);
            this.media.loadThumbnail(this.getActivity(), new Media.OnImageLoadedHandler() {
                @Override
                public void onImageLoaded(Bitmap thumbnail) {
                    MediaEditviewFragment.this.mediaContent.setImageURI(Uri.parse(media.getContentUri()));
                }
            });
        }
    }

    private void showPhotoInPhotoApp(Uri photoUri){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(photoUri, "image/*");
        startActivity(intent);
    }

    // this is required for assiging color using data binding
    @BindingConversion
    public static ColorDrawable convertColorToDrawable(int color) {
        return new ColorDrawable(color);
    }

    // mode handling, including switching back
    private boolean modeSwitched;

    private void switchMode(Mode mode) {
        this.mode = mode;
        this.binding.setMode(this.mode);
        this.modeSwitched = true;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onBackPressed() {
       if (this.mode == Mode.EDIT) {
           // if back is pressed we will undo any potential edits
           if (savepoint != null) {
               media.restoreFrom(savepoint);
               binding.setMedia(media);
           }

           if (modeSwitched) {
               switchMode(Mode.READ);
               return true;
           }
       }

       return false;
    }



}
