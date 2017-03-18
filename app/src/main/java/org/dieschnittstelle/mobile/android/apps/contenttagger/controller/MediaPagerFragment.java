package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mancj.slideup.SlideUp;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by master on 15.03.17.
 * <p>
 * this follows https://inducesmile.com/android/android-slideshow-using-viewpager-and-page-indicator-example/
 * <p>
 * DONE: there are still issues related to low memory and selecting pages from the buttons rather by paging - there is a critical number for ImageViews being active at the same time, i.e. problems are not caused by repeatedly loading images
 * solved this issue by only instantiating a single ImageView for the full size image, which will be set onItemSelected, thumbnails will be created for all items
 *
 * for creting sliding up panels that do not hide the background, see: https://android-arsenal.com/details/1/4929
 * TODO: add closing action button to sliding up panel, use custom button with smaller diameter instead of float button for opening
 */
public class MediaPagerFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "MediaPagerFragment";

    // arguments
    public static final String ARG_DISPLAY_MEDIA = "mediaIds";
    public static final String ARG_SELECTED_MEDIA_POS = "selectedMedia";

    public static final int FLAG_LOAD_THUMBNAIL = 1;
    public static final int FLAG_LOAD_IMAGE = 2;

    /*
    * the event dispatcher
    */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    private ViewGroup contentView;
    private CustomViewPagerAdapter adapter;
    private ViewPager viewPager;
    private int page = 0;
    private LayoutInflater inflater;
    private RadioGroup pagerControls;
    private TextView itemDescriptionView;

    // TODO: we could use this list for doing lazy loading rather than loading each single media element
    private List<Long> displayMediaIdList;
    private int selectedMediaPos = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // we expect to receive a list of ids for the media that shall be loaded and optionally a position which we will be displayed first
        displayMediaIdList = (List<Long>) getArguments().get(ARG_DISPLAY_MEDIA);
        if (getArguments().containsKey(ARG_SELECTED_MEDIA_POS)) {
            selectedMediaPos = getArguments().getInt(ARG_SELECTED_MEDIA_POS);
        }
        else {
            selectedMediaPos = 0;
        }

        // and set a listener that reacts to changes of an item
        // TODO: deletion needs to be handled, as well...
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                Log.i(logger,"onEvent(): updated: " + event.getData());
                // in case the updated media is the one currently displayed, we update the display
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = (ViewGroup)inflater.inflate(R.layout.media_pager, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.mediaPager);
        pagerControls = (RadioGroup) contentView.findViewById(R.id.radiogroup);

        createDescriptionSlider();

        // we load all media from the list
        new AsyncTask<Void, Void, List<Media>>() {

            @Override
            protected List<Media> doInBackground(Void... voids) {
                List<Media> loaded = new ArrayList<Media>();
                for (Long id : displayMediaIdList) {
                    loaded.add((Media) Media.readSync(Media.class, id));
                }
                return loaded;
            }

            @Override
            protected void onPostExecute(List<Media> media) {
                initialisePager(media);
                if (selectedMediaPos > -1) {
                    viewPager.setCurrentItem(selectedMediaPos);
                    // as the display of the current item will not be page change, we need to trigger item selection (and loading of the real image) by hand
                    // TODO: this does not work relably, as it might be that view creation will be delayed
                    adapter.onItemSelected(media.get(selectedMediaPos), selectedMediaPos);
                }
            }

        }.execute();

        return contentView;
    }

    public class CustomViewPagerAdapter extends PagerAdapter {
        private Context context;
        private List<Media> mediaList;

        // we use a single ImageView for showing the full images, which will be removed/set onItemSelected()
        private ImageView recycleableImageView;

        // we keep a map of items and views in order to update the actual image once a view is selected
        private Map<Media,View> mediaViewMap = new HashMap<Media,View>();

        public CustomViewPagerAdapter(Context context, List<Media> mediaList) {
            this.context = context;
            this.mediaList = mediaList;
        }

        @Override
        public int getCount() {
            return mediaList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == ((View) object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.i(logger, "destroyItem(): " + position);

            // well, we could reset the alias, but leave this up to later...

        }

        @Override
        public Object instantiateItem(final ViewGroup container, int position) {
            Log.i(logger, "instantiateItem(): " + position);
            final View view = inflater.inflate(R.layout.media_pager_itemview, container, false);
            final Media media = mediaList.get(position);
            final ViewGroup mediaContentContainer = (ViewGroup) view.findViewById(R.id.mediaContentContainer);

            final View mediaContent =  inflater.inflate(R.layout.media_pager_itemview_image, null);

            if (mediaContent != null) {
                mediaContentContainer.addView(mediaContent);

                loadMediaIntoImageView(getActivity(),media,view,mediaContent,FLAG_LOAD_THUMBNAIL);

                // add a listener that opens the detailview for the media
                mediaContent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((MainNavigationControllerActivity) getActivity()).showView(MediaEditviewFragment.class, MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_ID, media.getId()), true);
                    }
                });

            }

            // addView() needs to be run on the UI Thread
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    container.addView(view);
                }
            });

            mediaViewMap.put(media,view);

            return view;
        }

        public void onItemSelected(Media media, int position) {
            // we need to retrieve the imageView for the given media element
            View view = mediaViewMap.get(media);
            if (view == null) {
                Log.e(logger,"onItemSelected(): no view seems to exist for media at position: " + position);
                return;
            }
            // we read the media from the view
            View mediaContentContainer = view.findViewById(R.id.aliasableMediaContainer);
            if (mediaContentContainer == null) {
                Log.e(logger,"onItemSelected(): no mediaContent element found in view for media at position: " + position);
                return;
            }
            // check whether the recycleview has already been instantiated, otherwise read it out from the container
            if (recycleableImageView == null) {
                Log.i(logger,"onItemSelected(): instantiating the recycleable image view for position: " + position);
                recycleableImageView = (ImageView)inflater.inflate(R.layout.media_pager_itemview_image_content,null);
            }
            else {
                Log.i(logger,"onItemSelected(): recycling the recycleable image view for position: " + position);
                // we need to lookup the parent container in order to set the alias to visible
                ViewGroup currentContainer = (ViewGroup)findAncestorById(recycleableImageView,R.id.aliasableMediaContainer);
                if (currentContainer == null) {
                    Log.e(logger,"onItemSelected(): recycleable image view is not contained in aliasableMediaContainer! Cannot swap visibility on alias.");
                }
                else {
                    View mediaAlias = currentContainer.findViewById(R.id.mediaAlias);
                    if (mediaAlias == null) {
                        Log.e(logger,"onItemSelected(): could not find a mediaAlias for recycleable image view! Cannot swap visibility on alias.");
                    }
                    else {
                        Log.i(logger,"onItemSelected(): set visibility on the alias for the current holder of recycleable image view");
                        mediaAlias.setVisibility(View.VISIBLE);
                    }
                }

                // we now remove the view from its current parent
                if (recycleableImageView.getParent() != null) {
                    ((ViewGroup) recycleableImageView.getParent()).removeView(recycleableImageView);
                    Log.i(logger, "onItemSelected(): removed recycleableImageView from its current parent");
                }
                else {
                    Log.e(logger, "onItemSelected(): recycleableImageView cannot be removed from its current parent. No parent seems to exist");
                }

            }

            // and set it to the placeholde in the current mediaContainer
            ViewGroup mediaContentWrapper = (ViewGroup)mediaContentContainer.findViewById(R.id.mediaContentWrapper);
            mediaContentWrapper.addView(recycleableImageView);
            recycleableImageView.setVisibility(View.GONE);
            recycleableImageView.setImageResource(0);
            Log.i(logger,"onItemSelected(): loading real image for position: " + position + ", into: " + recycleableImageView);
            checkStatus();
            loadMediaIntoImageView(getActivity(),media,view,mediaContentContainer,FLAG_LOAD_IMAGE);
        }

        public Object getItem(int pos) {
            return mediaList.get(pos);
        }

        private View findAncestorById(View view,int id) {

            ViewParent parent = view.getParent();
            if (view.getId() == id) {
                return view;
            }
            else if (!(parent instanceof ViewGroup)) {
                return null;
            }
            else {
                return findAncestorById((View)parent,id);
            }

        }

        private void checkStatus() {
            for (Media key : mediaViewMap.keySet()) {
                View view = mediaViewMap.get(key);
                if (view != null) {
                    if (view.findViewById(R.id.mediaContent) != null) {
                        Log.d(logger,"checkStatus(): found mediaContent for item at position: " + mediaList.indexOf(key));
                    }
                }
            }
        }

    }

    public static void loadMediaIntoImageView(Context context,Media media,View mediaContainer,final View mediaContentContainer,int options) {
        boolean loadThumbnail = (options & FLAG_LOAD_THUMBNAIL) == FLAG_LOAD_THUMBNAIL;
        boolean loadImage = (options & FLAG_LOAD_IMAGE) == FLAG_LOAD_IMAGE;

        final View loadPlaceholder = mediaContainer.findViewById(R.id.loadPlaceholder);
        if (loadPlaceholder != null) {
            loadPlaceholder.setVisibility(View.VISIBLE);
        }

        ImageView mediaContent = null;
        ImageView mediaAlias = null;

        // if mediaContent is an imageView then we do not use an alias
        if (mediaContentContainer instanceof ImageView) {
            mediaContent = (ImageView)mediaContentContainer;
            mediaAlias = (ImageView)mediaContentContainer;
        }
        else {
            mediaAlias = (ImageView)mediaContentContainer.findViewById(R.id.mediaAlias);
            mediaContent = (ImageView)mediaContentContainer.findViewById(R.id.mediaContent);
        }

        final ImageView useMediaContent = mediaContent;
        final ImageView useMediaAlias = mediaAlias;

        // we load the alias and the real media element


        if (loadThumbnail) {
            if (useMediaAlias != useMediaContent) {
                useMediaAlias.setVisibility(View.VISIBLE);
                if (useMediaContent != null) {
                    useMediaContent.setVisibility(View.GONE);
                }
            }

            // set a thumbnail first
            media.loadThumbnail(context, new Media.OnImageLoadedHandler() {
                public void onImageLoaded(Bitmap image) {
                    Log.i(logger, "loadMediaIntoImageView(): setting thumbnail on image: " + ((image == null) ? "null" : "<bitmap>"));
                    useMediaAlias.setImageBitmap(image);
                }
            });

        }

        if (loadImage) {
            if (useMediaContent == null) {
                Log.e(logger,"loadMediaIntoImageView(): no media content element found!");
                if (loadPlaceholder != null) {
                    loadPlaceholder.setVisibility(View.GONE);
                }
            }
            else {
                // use a manually controlled placeholder animation as shown in http://stackoverflow.com/questions/24826459/animated-loading-image-in-picasso
                Picasso.with(context).load(Uri.parse(media.getContentUri())).into(useMediaContent, new Callback() {
                    @Override
                    public void onSuccess() {
                        if (loadPlaceholder != null) {
                            loadPlaceholder.setVisibility(View.GONE);
                            if (useMediaAlias != useMediaContent) {
                                useMediaAlias.setVisibility(View.GONE);
                                useMediaContent.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onError() {
                        if (loadPlaceholder != null) {
                            loadPlaceholder.setVisibility(View.GONE);
                        }
                    }

                });
            }
        }
        else if (loadThumbnail) {
            if (loadPlaceholder != null) {
                loadPlaceholder.setVisibility(View.GONE);
            }
        }

    }

    public void initialisePager(final List<Media> mediaList) {

        if (getActivity() != null) {

            // create the adapter
            adapter = new CustomViewPagerAdapter(getActivity(), mediaList);
            viewPager.setAdapter(adapter);
            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    Log.i(logger, "onPageSelected(): " + position);
                    // here, we select the radio buttons
                    updateRadioGroup(position);
                    // and we update the description
                    Media selectedItem = (Media)adapter.getItem(position);
                    if (selectedItem != null && selectedItem.getDescription() != null) {
                        if (itemDescriptionView != null) {
                            itemDescriptionView.setText(selectedItem.getDescription());
                        }
                        else {
                            Log.e(logger,"itemDescriptionView has not been instantiated!");
                        }
                    }

                    adapter.onItemSelected(selectedItem,position);
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

            // dynamically create the radio buttons
            getActivity().runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < mediaList.size(); i++) {
                                final RadioButton button = (RadioButton) inflater.inflate(R.layout.media_pager_radio, pagerControls, false);
                                if (i == 0) {
                                    button.setChecked(true);
                                }
                                button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                        if (b) {
                                            updateRadioGroup(pagerControls.indexOfChild(button));
                                        }
                                    }
                                });

                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        int pos = pagerControls.indexOfChild(button);
                                        Log.i(logger, "onClick(): " + pos);
                                        // we cannot animate here as animation will result in instantiating all intermediate views up to the selected one
                                        viewPager.setCurrentItem(pos, false);
                                    }
                                });

                                pagerControls.addView(button);
                            }
                        }

                    });
        }
    }

    // for some reason, automatic checking/unchecking does not work for these dynamically added radios
    public void updateRadioGroup(int selected) {
        for (int i = 0; i < pagerControls.getChildCount(); i++) {
            if (pagerControls.getChildAt(i) instanceof RadioButton) {
                if (i == selected) {
                    ((RadioButton) pagerControls.getChildAt(i)).setChecked(true);
                } else {
                    ((RadioButton) pagerControls.getChildAt(i)).setChecked(false);
                }
            }
        }
    }

    // handle the sliding panel that will display the images' description
    private void createDescriptionSlider() {
        Log.i(logger,"createDescriptionSlider()");
        final View slideViewContainer = inflater.inflate(R.layout.media_pager_itemview_description,null);
        final View slideView = slideViewContainer.findViewById(R.id.slider);
        // we set the instance attribute on which the description text will be displayed
        itemDescriptionView = (TextView)slideView.findViewById(R.id.itemDescription);
        Log.d(logger,"createDescriptionSlider(): itemDescriptionView is: " + itemDescriptionView);
        contentView.addView(slideViewContainer);
        final FloatingActionButton fab = (FloatingActionButton) slideViewContainer.findViewById(R.id.sliderControl);
        // we read out the description

       final SlideUp slideUp = new SlideUp.Builder(slideView)
                .withListeners(new SlideUp.Listener() {
                    @Override
                    public void onSlide(float percent) {
                        Log.i(logger,"onSlide(): " + percent);
                        slideView.setAlpha(1 - (percent / 100));
                    }

                    @Override
                    public void onVisibilityChanged(int visibility) {
                        Log.i(logger,"onVisibilityChanged(): " + visibility);
                        if (visibility == View.GONE) {
                            fab.setVisibility(View.VISIBLE);
                        }
                        else {
                            fab.setVisibility(View.GONE);
                        }
                    }
                })
               .withStartGravity(Gravity.BOTTOM)
               .withLoggingEnabled(true)
               .withGesturesEnabled(true)
               .withStartState(SlideUp.State.HIDDEN)
               .build();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slideUp.show();
                fab.setVisibility(View.GONE);
            }
        });

        Log.i(logger,"createDescriptionSlider(): created slider: " + slideUp);
    }

    @Override
    public void onResume() {
        super.onResume();
        LifecycleHandling.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        LifecycleHandling.onPause(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LifecycleHandling.onDestroy(this);
    }
}
