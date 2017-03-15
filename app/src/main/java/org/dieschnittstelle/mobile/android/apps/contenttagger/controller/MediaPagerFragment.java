package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by master on 15.03.17.
 * <p>
 * this follows https://inducesmile.com/android/android-slideshow-using-viewpager-and-page-indicator-example/
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

    private CustomViewPagerAdapter adapter;
    private ViewPager viewPager;
    private int page = 0;
    private LayoutInflater inflater;
    private RadioGroup pagerControls;

    // TODO: we could use this list for doing lazy loading rather than loading each single media element
    private List<Long> displayMediaIdList;
    private int selectedMediaPos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

//        // we only react to reading out all tags if we have generated the event ourselves
//        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Media.class, this), false, new EventListener<List<Media>>() {
//            @Override
//            public void onEvent(Event<List<Media>> event) {
//                initialisePager(event.getData());
//            }
//        });

        displayMediaIdList = (List<Long>)getArguments().get(ARG_DISPLAY_MEDIA);
        selectedMediaPos = getArguments().getInt(ARG_SELECTED_MEDIA_POS);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.media_pager, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.mediaPager);
        pagerControls = (RadioGroup) contentView.findViewById(R.id.radiogroup);

        // we load all media from the list
        new AsyncTask<Void,Void,List<Media>>() {

            @Override
            protected List<Media> doInBackground(Void... voids) {
                List<Media> loaded = new ArrayList<Media>();
                for (Long id : displayMediaIdList) {
                    loaded.add((Media)Media.readSync(Media.class,id));
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

//        Media.readAll(Media.class, this);

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
            mediaList.get(position).releaseImage();
            ImageView mediaContent = (ImageView) container.findViewById(R.id.mediaContent);
        }

        @Override
        public Object instantiateItem(final ViewGroup container, int position) {
            Log.i(logger, "instantiateItem(): " + position);
            final View view = inflater.inflate(R.layout.media_pager_itemview, container, false);
            Media media = mediaList.get(position);
            final ImageView mediaContent = (ImageView) view.findViewById(R.id.mediaContent);

            media.loadThumbnail(context, new Media.OnImageLoadedHandler() {
                @Override
                public void onImageLoaded(Bitmap image) {
                    mediaContent.setImageBitmap(image);
                }
            });

            // addView() needs to be run on the UI Thread
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    container.addView(view);
                }
            });

            return view;
        }

    }

    public void initialisePager(final List<Media> mediaList) {

        // create the adapter
        adapter = new CustomViewPagerAdapter(getActivity(), mediaList);
        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // here, we select the radio buttons
                updateRadioGroup(position);
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
                            if (i==0) {
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
                                    Log.i(logger,"onClick()");
                                    viewPager.setCurrentItem(pagerControls.indexOfChild(button), true);
                                }
                            });

                            pagerControls.addView(button);
                        }
                    }

                });
    }

    // for some reason, automatic checking/unchecking does not work for these dynamically added radios
    public void updateRadioGroup(int selected) {
        for (int i=0;i<pagerControls.getChildCount();i++) {
            if (pagerControls.getChildAt(i) instanceof RadioButton) {
                if (i == selected) {
                    ((RadioButton)pagerControls.getChildAt(i)).setChecked(true);
                }
                else {
                    ((RadioButton)pagerControls.getChildAt(i)).setChecked(false);
                }
            }
        }
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
