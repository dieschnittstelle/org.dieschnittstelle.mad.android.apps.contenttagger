package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
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
import java.util.List;

/**
 * Created by master on 15.03.17.
 * <p>
 * this follows https://inducesmile.com/android/android-slideshow-using-viewpager-and-page-indicator-example/
 * <p>
 * TODO: there are still issues related to low memory and selecting pages from the buttons rather by paging - there is a critical number for ImageViews being active at the same time, i.e. problems are not caused by repeatedly loading images
 *
 * for creting sliding up panels that do not hide the background, see: https://android-arsenal.com/details/1/4929
 */
public class MediaPagerFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "MediaPagerFragment";

    // arguments
    public static final String ARG_DISPLAY_MEDIA = "mediaIds";
    public static final String ARG_SELECTED_MEDIA_POS = "selectedMedia";

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

    private static final int RECYCLER_CAPACITY_INITIAL = 3;

    // some class that wraps an image view and indicates whether it is used or not
    private class RecycleableImageViewHolder {

        public ImageView view;
        public int user = -1;
        public int id;

        public RecycleableImageViewHolder(int id, ImageView view) {
            this.view = view;
            view.setTag(R.string.tag_viewholder_id, id);
        }
    }

    private List<RecycleableImageViewHolder> recycleableImageViews = new ArrayList<RecycleableImageViewHolder>();

    private ImageView bindReycleableImageViewForPosition(int pos) {
        synchronized (recycleableImageViews) {

            RecycleableImageViewHolder useView = null;
            for (RecycleableImageViewHolder view : recycleableImageViews) {
                if (view.user == -1) {
                    useView = view;
                }
            }
            if (useView == null) {
                Log.e(logger, "bindRecycleableImageView(): capacity exceeded. Need to create new view!");
                useView = addRecycleaebleImageView();
            }
            useView.user = pos;

            return useView.view;
        }
//        return (ImageView) inflater.inflate(R.layout.media_pager_itemview_image, null);
    }

    private void releaseRecycleableImageView(ImageView view) {
        synchronized (recycleableImageViews) {
            if (view != null) {
                // remove the view from its parent, which should be done anyway, though
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent != null) {
                    parent.removeView(view);
                }

                Integer holderId = (Integer) view.getTag(R.string.tag_viewholder_id);
                if (holderId == null) {
                    Log.i(logger, "releaseRecycleableImageView(): cannot release. no holderId set on imageView: " + view);
                    return;
                }
                Log.e(logger, "releaseRecycleableImageView(): releasing: " + holderId);
                recycleableImageViews.get(holderId).user = -1;
                // see http://stackoverflow.com/questions/2859212/how-to-clear-an-imageview-in-android
                view.setImageResource(0);
            } else {
                Log.e(logger, "releaseRecycleableImageView(): no view specified. got null");
            }
        }
    }

    private RecycleableImageViewHolder addRecycleaebleImageView() {
        RecycleableImageViewHolder newView = new RecycleableImageViewHolder(recycleableImageViews.size(), (ImageView) inflater.inflate(R.layout.media_pager_itemview_image, null));
        recycleableImageViews.add(newView);

        return newView;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // we expect to receive a list of ids for the media that shall be loaded and optionally a position which we will be displayed first
        displayMediaIdList = (List<Long>) getArguments().get(ARG_DISPLAY_MEDIA);
        if (getArguments().containsKey(ARG_SELECTED_MEDIA_POS)) {
            selectedMediaPos = getArguments().getInt(ARG_SELECTED_MEDIA_POS);
        }

        // we load a couple of instances of the image layout
        for (int i = 0; i < RECYCLER_CAPACITY_INITIAL; i++) {
            addRecycleaebleImageView();
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
                }
            }

        }.execute();

        return contentView;
    }

    public class CustomViewPagerAdapter extends PagerAdapter {
        private Context context;
        private List<Media> mediaList;

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
//            mediaList.get(position).releaseImage();
            ViewGroup mediaContentContainer = (ViewGroup) container.findViewById(R.id.mediaContentContainer);
            ImageView mediaContent = (ImageView) container.findViewById(R.id.mediaContent);
            releaseRecycleableImageView(mediaContent);
            mediaContentContainer.removeView(mediaContent);
            container.removeView((View) object);
        }

        @Override
        public Object instantiateItem(final ViewGroup container, int position) {
            Log.i(logger, "instantiateItem(): " + position);
            final View view = inflater.inflate(R.layout.media_pager_itemview, container, false);
            final Media media = mediaList.get(position);
            final ViewGroup mediaContentContainer = (ViewGroup) view.findViewById(R.id.mediaContentContainer);

            final ImageView mediaContent = bindReycleableImageViewForPosition(position);

            if (mediaContent != null) {
                mediaContentContainer.addView(mediaContent);

                loadMediaIntoImageView(getActivity(),media,view,mediaContent);

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

            return view;
        }

        public Object getItem(int pos) {
            return mediaList.get(pos);
        }

    }

    public static void loadMediaIntoImageView(Context context,Media media,View mediaContainer,final ImageView mediaContent) {
        final View loadPlaceholder = mediaContainer.findViewById(R.id.loadPlaceholder);
        if (loadPlaceholder != null) {
            loadPlaceholder.setVisibility(View.VISIBLE);
        }

        // set a thumbnail first
        media.loadThumbnail(context, new Media.OnImageLoadedHandler() {
            public void onImageLoaded(Bitmap image) {
                Log.i(logger,"setting thumbnail on image: " + ((image == null ) ? "null" : "<bitmap>"));
                mediaContent.setImageBitmap(image);
            }
        });

        // use a manually controlled placeholder animation as shown in http://stackoverflow.com/questions/24826459/animated-loading-image-in-picasso
        Picasso.with(context).load(Uri.parse(media.getContentUri())).into(mediaContent, new Callback() {
            @Override
            public void onSuccess() {
                if (loadPlaceholder != null) {
                    loadPlaceholder.setVisibility(View.GONE);
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
