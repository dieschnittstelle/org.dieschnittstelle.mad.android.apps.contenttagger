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
public class PlacesOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "PlacesOverviewFragment";

    private MapView map;
    private IMapController mapController;

    // the overlay to which the items will be added
    private ItemizedOverlayWithFocus<OverlayItem> overlay;

    // we need to track initialisation because onresume in overviews is called multiple times
    private boolean initialised;

    // we keep a local list of places which will be reused onresume;
    private List<Place> places;

    // seems we need to track subsequent tracks on an overlay item as the overlay does not distinguish between tap to focus and tap after being focused
    private Place focusedPlace;

    // we extend the OverlayItem class in order to keep the model data and the overlay view together
    public static class PlaceOverlayItem extends OverlayItem {

        private Place place;

        public PlaceOverlayItem(Place place) {
            super(place.getTitle(), "", new GeoPoint(place.getGeolat(), place.getGeolong()));
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
                Log.i(logger, "will add new place to map: " + event.getData());
                places.add(event.getData());
            }
        });

    }

    protected OverlayItem findItemForPlace(Place place) {
        for (int i = 0; i < this.overlay.size(); i++) {
            if (((PlaceOverlayItem) this.overlay.getItem(i)).getPlace() == place) {
                return this.overlay.getItem(i);
            }
        }
        Log.w(logger, "could not find overlay item for place: " + place);
        return null;
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

        return view;
    }

    @Override
    public void onResume() {
        Log.i(logger, "onResume()");
        super.onResume();

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
        ItemizedOverlayWithFocus.OnItemGestureListener listener = new ItemizedIconOverlay.OnItemGestureListener() {
            @Override
            public boolean onItemSingleTapUp(int index, Object item) {
                if (((PlaceOverlayItem)item).getPlace() == focusedPlace) {
                    Log.d(logger,"onSingleTapUp(): focused place was tapped: " + focusedPlace);
                    // show the editview (focusedPlace will be re-set onresume)
                    ((MainNavigationControllerActivity) getActivity()).showView(PlacesEditviewFragment.class, MainNavigationControllerActivity.createArguments(PlacesEditviewFragment.ARG_PLACE_ID, focusedPlace.getId()), true);
                }
                else {
                    focusedPlace = ((PlaceOverlayItem)item).getPlace();
                    Log.d(logger,"onSingleTapUp(): focused place has changed: " + focusedPlace);
                }
                return false;
            }

            @Override
            public boolean onItemLongPress(int index, Object item) {
                Log.d(logger,"onItemLongPress(): " + item);
                return false;
            }
        };

        //the overlay
        this.overlay = new ItemizedOverlayWithFocus<OverlayItem>(getActivity(), new ArrayList<OverlayItem>(), listener);
        this.overlay.setFocusItemsOnTap(true);

        this.map.getOverlays().add(this.overlay);

        // we read out all places unless the places have already been read
        if (this.places == null) {
            Place.readAll(Place.class);
        }
        else {
            showPlacesOnMap(this.places);
        }

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

}
