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
import android.widget.EditText;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Place;
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

import org.osmdroid.views.overlay.MapEventsOverlay;

/**
 * Created by master on 28.02.17.
 *
 * for usage of the osmdroid library see https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
 */
public class PlacesEditviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "PlacesEditviewFragment";

    // we use a single view for read and edit
    private enum Mode {EDIT, READ, CREATE};

    /*
    * we expect that the id of the item to be displayed is passed to us, rather than the item itself...
    */
    public static final String ARG_PLACE_ID = TaggableOverviewFragment.OUTARG_SELECTED_ITEM_ID;

    private Mode mode = Mode.EDIT;

    // these are osmdroid constructs
    private MapView map;
    private IMapController mapController;

    private EditText title;
    private TagsbarController tagsbarController;

    protected Place place;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // once we read a place (to be edited) we will update the map
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READ, Place.class, this), true, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                place = event.getData();
                title.setText(place.getTitle());
                tagsbarController.bindTaggable(place);
                updateDisplayedLocation(new GeoPoint(place.getGeolat(),place.getGeolong()),15);
            }
        });

        // on edits/creation we return to the previous view
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.DELETED,Event.CRUD.UPDATED,Event.CRUD.CREATED), Place.class), false, new EventListener<Place>() {
            @Override
            public void onEvent(Event<Place> event) {
                if (place == event.getData()) {
                    getFragmentManager().popBackStack();
                }
                else {
                    Log.i(logger, "onEvent(): got " + event.getType() + " event for link, but it involves a different object than the one being edited: " + event.getData() + ". Ignore...");
                }
            }
        });


        // we have an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.places_editview,container,false);

        this.title = (EditText)contentView.findViewById(R.id.title);
        Log.i(logger,"got title: " + title);

        this.tagsbarController = new TagsbarController(this,(ViewGroup)contentView.findViewById(R.id.tagsbar),R.layout.tagsbar_itemview);

        this.map = (MapView)contentView.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // here, we set the view/edit mode depending on whether we receive arguments or not
        // we read out the id from the arguments
        long noteId = getArguments().getLong(ARG_PLACE_ID);
        if (noteId > -1) {
            this.mode = Mode.READ;
            ((ActionBarActivity) getActivity()).setTitle(R.string.title_edit_place);
        }
        else {
            ((ActionBarActivity) getActivity()).setTitle(R.string.title_create_place);
            this.mode = Mode.CREATE;
        }

        return contentView;
    }

    // some utility method that is created both for edit and read mode
    public void updateDisplayedLocation(GeoPoint loc, int zoom) {
        mapController.setZoom(zoom);
        mapController.setCenter(loc);

        // well... also this could be synchronised with reading
        if (mode == Mode.EDIT || mode == Mode.CREATE) {
            // the following classes encapsulate handling of tap/click events on the map, see https://github.com/osmdroid/osmdroid/issues/295
            MapEventsReceiver mReceive = new MapEventsReceiver() {
                @Override
                public boolean singleTapConfirmedHelper(GeoPoint p) {
                    Log.d(logger, "setting coordinates from GeoPoint on Place entity: " + p);

                    place.setGeolat(p.getLatitude());
                    place.setGeolong(p.getLongitude());

                    mapController.setCenter(p);
                    return false;
                }

                @Override
                public boolean longPressHelper(GeoPoint p) {
                    return false;
                }
            };

            MapEventsOverlay overlay = new MapEventsOverlay(mReceive);
            map.getOverlays().add(overlay);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // on appearing we will focus the map either on some default location (which could be the current location) or on the location of the Place item
        this.mapController = map.getController();

        GeoPoint startPoint = null;

        // we read out the id from the arguments and
        if (this.mode == Mode.EDIT || this.mode == Mode.READ ) {
            long placeId = getArguments().getLong(ARG_PLACE_ID);
            // read all notes - reaction will be dealt with by event handler
            Place.read(Place.class, placeId, this);
        } else {
            place = new Place();
            // this is required in order for the tagbar to be available for new notes
            tagsbarController.bindTaggable(place);
            // we set a default start point (which should ideally be the user's location)
            updateDisplayedLocation(new GeoPoint(52.545377, 13.351655),12);
        }

        AddTagDialogController.getInstance().attach(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_places_editview,menu);
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
                }
                else {
                    place.create();
                }
                return true;
            case R.id.action_delete:
                if (place.created()) {
                    //note.delete();
                    NotesEditviewFragment.confirmDeleteTaggable(this.getActivity(),this.place);
                }
                return true;
            case R.id.action_add_tag:
                AddTagDialogController.getInstance().show(place);
                break;
        }

        return false;
    }

}
