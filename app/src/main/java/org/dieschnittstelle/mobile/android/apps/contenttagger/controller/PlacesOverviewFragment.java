package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Place;
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by master on 14.03.16.
 */
public class PlacesOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner, MainNavigationControllerActivity.OnBackListener {

    protected static String logger = "PlacesOverviewFragment";

    private MapView map;
    private IMapController mapController;

    // we introduce different modes
    private static enum Mode {
        FOCUS, OVERVIEW
    }

    ;

    // the overlay to which the items will be added
    private ItemizedOverlayWithFocus<OverlayItem> overlay;

    // we need to track initialisation because onresume in overviews is called multiple times
    private boolean initialised;

    // we keep a local list of places which will be reused onresume;
    private List<Place> places;

    // seems we need to track subsequent tracks on an overlay item as the overlay does not distinguish between tap to focus and tap after being focused
    private Place focusedPlace;

    private Mode mode = Mode.OVERVIEW;

    // we extend the OverlayItem class in order to keep the model data and the overlay view together
    public static class PlaceOverlayItem extends OverlayItem {

        private Place place;

        public PlaceOverlayItem(Place place) {
            super(place.getTitle(), "", getGeopointFromPlace(place));
            this.place = place;
        }

        public Place getPlace() {
            return place;
        }

        public void setPlace(Place place) {
            this.place = place;
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected int findItemPosForPlace(Place place) {
        for (int i = 0; i < this.overlay.size(); i++) {
            if (((PlaceOverlayItem) this.overlay.getItem(i)).getPlace() == place) {
                return i;
            }
        }
        Log.w(logger, "could not find overlay item for place: " + place);
        return -1;
    }

    protected void showPlacesOnMap(List<Place> places) {
        for (Place place : places) {
            if (place.getGeolat() > 0 && place.getGeolong() > 0) {
                this.overlay.addItem(new PlaceOverlayItem(place));
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(logger, "onCreateView()");
        super.onCreateView(inflater, container, savedInstanceState);

        // it seems we need to re-create the view each time this fragment is displayed
        View view = inflater.inflate(R.layout.places_overview, container, false);

        this.map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        setHasOptionsMenu(true);

        // there will be problems if listeners are set in onCreate() and fragments are reinitialised (e.g. on hiding the app and restarting it)

        // we register an event listener that will display the places on the map
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Place.class), true, new EventListener<List<Place>>() {
            @Override
            public void onEvent(Event<List<Place>> event) {
                places = event.getData();
                Log.i(logger, "read places: " + places);
                showPlacesOnMap(places);
            }
        });

        // if a new item is created we will add it to the map
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Place.class), true, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                Place place = event.getData();
                Log.i(logger, "will add new place to map: " + place);
                overlay.addItem(new PlaceOverlayItem(place));
                places.add(place);
                mapController.setCenter(getGeopointFromPlace(place));
            }
        });

        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Place.class), true, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                Place place = event.getData();
                Log.i(logger, "will remove place from map: " + place);
                int itemPos = findItemPosForPlace(place);
                if (itemPos != -1) {
                    overlay.removeItem(itemPos);
                }
                places.remove(place);
            }
        });

        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Place.class), true, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                Log.i(logger, "will update place in map: " + event.getData());
                int itemPos = findItemPosForPlace(event.getData());
                if (itemPos != -1) {
                    overlay.removeItem(itemPos);
                }
                overlay.addItem(new PlaceOverlayItem(event.getData()));
                mapController.setCenter(getGeopointFromPlace(event.getData()));
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

    @Override
    public void onResume() {
        Log.i(logger, "onResume()");
        super.onResume();

        Log.i(logger, "onResume(): first call");

        // reset the focused place
        this.focusedPlace = null;

        // we need to re-initialise the map view on every return to this fragment...
        Log.d(logger, "onResume(): initialising map view");
        // set the title
        ((ActionBarActivity) getActivity()).setTitle(R.string.menuitem_places);

        // on appearing we will focus the map
        this.mapController = map.getController();
        mapController.setZoom(12);
        GeoPoint startPoint = new GeoPoint(52.545377, 13.351655);
        mapController.setCenter(startPoint);

        // create the overlay for displaying places - seems we cannot factor this out to onCreate() to do it only once and keep the overlay
        // it seems that the overlay needs to be recreated...

            ItemizedOverlayWithFocus.OnItemGestureListener listener = new ItemizedIconOverlay.OnItemGestureListener() {
                @Override
                public boolean onItemSingleTapUp(int index, Object item) {
                    if (((PlaceOverlayItem) item).getPlace() == focusedPlace) {
                        Log.d(logger, "onSingleTapUp(): focused place was tapped: " + focusedPlace);
                        // check in which mode we are
                        if (mode == Mode.FOCUS) {
                            // show the editview (focusedPlace will be re-set onresume)
                            Bundle args = MainNavigationControllerActivity.createArguments(PlacesEditviewFragment.ARG_PLACE_ID, focusedPlace.getId());
                            args.putSerializable(PlacesEditviewFragment.ARG_MODE, PlacesEditviewFragment.Mode.READ);
                            ((MainNavigationControllerActivity) getActivity()).showView(PlacesEditviewFragment.class, args, true);
                        } else {
                            mode = Mode.FOCUS;
                            updateView(focusedPlace);
                        }
                        // ???
                        focusedPlace = null;
                    } else {
                        focusedPlace = ((PlaceOverlayItem) item).getPlace();
                        Log.d(logger, "onSingleTapUp(): focused place has changed: " + focusedPlace);
                    }
                    return false;
                }

                @Override
                public boolean onItemLongPress(int index, Object item) {
                    Log.d(logger, "onItemLongPress(): " + item);
                    return false;
                }
            };

            //the overlay
            this.overlay = new ItemizedOverlayWithFocus<OverlayItem>(getActivity(), new ArrayList<OverlayItem>(), listener);
            this.overlay.setFocusItemsOnTap(true);

            this.map.getOverlays().add(this.overlay);


        Log.d(logger, "onResume(): " + this + ": activity is: " + this.getActivity());

        // we read out all places unless the places have already been read
        if (this.places == null) {
            Place.readAll(Place.class);
        } else {
            showPlacesOnMap(this.places);
        }

        LifecycleHandling.onResume(this);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_places_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            ((MainNavigationControllerActivity) getActivity()).showView(PlacesEditviewFragment.class, MainNavigationControllerActivity.createArguments(PlacesEditviewFragment.ARG_PLACE_ID, -1L), true);
        }

        return false;
    }

    private void updateView() {
        updateView(null);
    }

    private void updateView(Place place) {
        if (place != null) {
            this.mapController.setCenter(getGeopointFromPlace(place));
        }
        this.mapController.setZoom(getZoomForMode());
    }

    private int getZoomForMode() {
        switch (this.mode) {
            case FOCUS:
                return 17;
            case OVERVIEW:
                return 12;
            default:
                return 14;
        }
    }

    @Override
    public boolean onBackPressed() {
        if (this.mode == Mode.FOCUS) {
            this.overlay.unSetFocusedItem();
            this.focusedPlace = null;
            this.mode = Mode.OVERVIEW;
            updateView();
            return true;
        }
        return false;
    }

    public static GeoPoint getGeopointFromPlace(Place place) {
        return new GeoPoint(place.getGeolat(), place.getGeolong());
    }


}
