package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;

/**
 * Created by master on 14.03.16.
 */
public class PlacesOverviewFragment extends Fragment {

    private MapView map;
    private IMapController mapController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.places_overview,null);

        this.map = (MapView) view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // on appearing we will focus the map
        this.mapController = map.getController();
        mapController.setZoom(12);
        GeoPoint startPoint = new GeoPoint(52.545377, 13.351655);
        mapController.setCenter(startPoint);
    }
}
