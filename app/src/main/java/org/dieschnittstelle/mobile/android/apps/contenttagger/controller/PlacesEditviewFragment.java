package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.app.FragmentManager;
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

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Place;
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
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

import org.osmdroid.events.MapEventsReceiver;

import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

/**
 * Created by master on 28.02.17.
 * <p>
 * for usage of the osmdroid library see https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
 */
public class PlacesEditviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    private static int ZOOM = 17;

    protected static String logger = "PlacesEditviewFragment";

    // we always allow editing here, as the mere READ of places will be done via the map overview
    // BUT: how to give access to tags from that level?
    public enum Mode {
        READ, EDIT, CREATE
    }

    ;

    /*
    * we expect that the id of the item to be displayed is passed to us, rather than the item itself...
    */
    public static final String ARG_PLACE_ID = TaggableOverviewFragment.OUTARG_SELECTED_ITEM_ID;
    public static final String ARG_MODE = "mode";

    private Mode mode = Mode.READ;

    // these are osmdroid constructs
    private MapView map;
    private IMapController mapController;

    private EditText title;
    private TagsbarController tagsbarController;

    protected Place place;

    // the iconsOverlay to which the items will be added
    private ItemizedIconOverlay<OverlayItem> iconsOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(logger, "onCreate()");
        super.onCreate(savedInstanceState);

        // once we read a place (to be edited) we will update the map
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READ, Place.class, this), true, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                place = event.getData();
                bindModelToView();
            }
        });

        // on edits/creation we return to the previous view
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.DELETED, Event.CRUD.UPDATED, Event.CRUD.CREATED), Place.class), false, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                if (place == event.getData()) {
                    FragmentManager mng = getFragmentManager();
                    if (mng != null) {
                        getFragmentManager().popBackStack();
                    } else {
                        Log.w(logger, "onEvent(): the fragment manager is null. I am: " + PlacesEditviewFragment.this);
                    }
                } else {
                    Log.i(logger, "onEvent(): got " + event.getType() + " event for link, but it involves a different object than the one being edited: " + event.getData() + ". Ignore...");
                }
            }
        });


        // we have an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(logger, "onCreateView()");
        View contentView = inflater.inflate(R.layout.places_editview, container, false);

        this.title = (EditText) contentView.findViewById(R.id.title);
        Log.i(logger, "got title: " + title);

        this.tagsbarController = new TagsbarController(this, (ViewGroup) contentView.findViewById(R.id.tagsbar), R.layout.tagsbar_itemview);

        this.map = (MapView) contentView.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // here, we set the view/edit mode depending on whether we receive arguments or not
        // we read out the id from the arguments
        long placeId = getArguments().getLong(ARG_PLACE_ID);
        if (placeId > -1) {
            Mode argsMode = (Mode) getArguments().getSerializable(ARG_MODE);
            Log.i(logger, "got mode from arguments: " + argsMode);
            if (argsMode != null) {
                this.mode = argsMode;
            }
            // we read the place
            Log.i(logger, "editing: " + placeId);
            Place.read(Place.class, placeId);
        } else {
            this.mode = Mode.CREATE;
        }

        return contentView;
    }


    @Override
    public void onResume() {
        Log.i(logger, "onResume()");
        super.onResume();
        // this should normally always return true as this view is a navigation terminal
        if (LifecycleHandling.onResume(this)) {
            Log.i(logger, "onResume(): first call");

            // on appearing we will focus the map either on some default location (which could be the current location) or on the location of the Place item
            this.mapController = this.map.getController();

            // create an iconsOverlay for displaying items
            this.iconsOverlay = new ItemizedIconOverlay<OverlayItem>(new ArrayList<OverlayItem>(), new DummyOnGestureListener(), getActivity());
            this.map.getOverlays().add(iconsOverlay);

            // ... and another one for receiving map events
            // the following classes encapsulate handling of tap/click events on the map, see https://github.com/osmdroid/osmdroid/issues/295
            MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    Log.d(logger, "setting coordinates from GeoPoint on Place entity: " + p);

                    // we only react to this event if we are not in read mode
                    if (mode != Mode.READ) {
                        place.setGeolat(p.getLatitude());
                        place.setGeolong(p.getLongitude());

                        updatePlaceDisplay(place);
                    }

                    return false;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };

            MapEventsOverlay overlay = new MapEventsOverlay(mReceive);
            map.getOverlays().add(overlay);

            // read the mode from the arguments
            if (this.mode == Mode.EDIT || this.mode == Mode.READ) {
                long placeId = getArguments().getLong(ARG_PLACE_ID);
                // read all notes - reaction will be dealt with by event handler
                Place.read(Place.class, placeId, this);
            } else {
                place = new Place();
                place.setGeolong(PlacesOverviewFragment.DEFAULT_LOCATION.getLongitude());
                place.setGeolat(PlacesOverviewFragment.DEFAULT_LOCATION.getLatitude());
                bindModelToView();
            }

            AddTagDialogController.getInstance().attach(getActivity());
        } else {
            Log.i(logger, "onResume(): repeated call");
        }
    }

    // here, we centralise all view filling logics that might need to be applied more than once in case the mode is switched
    public void bindModelToView() {
        title.setText(place.getTitle());
        if (mode == Mode.READ) {
            tagsbarController.setRemoveActive(false);
            title.setEnabled(false);
        } else {
            tagsbarController.setRemoveActive(true);
            title.setEnabled(true);
        }
        tagsbarController.bindTaggable(place);

        if (mode != Mode.CREATE) {
            ((ActionBarActivity) getActivity()).setTitle(place.getTitle());
        }
        else {
            ((ActionBarActivity) getActivity()).setTitle(R.string.title_create_place);
        }

        getActivity().invalidateOptionsMenu();

        updatePlaceDisplay(place);
    }

    protected void updatePlaceDisplay(Place place) {
        Log.i(logger, "updatePlaceDisplay() lat/long: " + place.getGeolat() + "/" + place.getGeolong());

        GeoPoint gp = PlacesOverviewFragment.getGeopointFromPlace(place);
        mapController.setZoom(ZOOM);
        mapController.setCenter(gp);

        // we remove all icons and add a new icon
        iconsOverlay.removeAllItems();
        iconsOverlay.addItem(new OverlayItem("", "", gp));
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mode == Mode.READ) {
            inflater.inflate(R.menu.menu_places_editview_read, menu);
        } else {
            inflater.inflate(R.menu.menu_places_editview, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // we check the known options
        switch (item.getItemId()) {
            case R.id.action_save:
                // bind the data from the input form to the item
                place.setTitle(this.title.getText().toString());
                if (place.created()) {
                    place.update();
                } else {
                    place.create();
                }
                return true;
            case R.id.action_delete:
                if (place.created()) {
                    //note.delete();
                    NotesEditviewFragment.confirmDeleteTaggable(this.getActivity(), this.place);
                }
                return true;
            case R.id.action_add_tag:
                AddTagDialogController.getInstance().show(place);
                break;
            case R.id.action_edit:
                this.mode = Mode.EDIT;
                bindModelToView();
                break;
        }

        return false;
    }

    @Override
    public void onPause() {
        Log.i(logger, "onPause()");
        super.onPause();
        LifecycleHandling.onPause(this);
    }

    @Override
    public void onDestroy() {
        Log.i(logger, "onDestroy()");
        super.onDestroy();
        LifecycleHandling.onDestroy(this);
    }

    /**
     * a dummy on gesture listener
     */
    private class DummyOnGestureListener implements ItemizedIconOverlay.OnItemGestureListener {

        @Override
        public boolean onItemSingleTapUp(int index, Object item) {
            return false;
        }

        @Override
        public boolean onItemLongPress(int index, Object item) {
            return false;
        }
    }

}
