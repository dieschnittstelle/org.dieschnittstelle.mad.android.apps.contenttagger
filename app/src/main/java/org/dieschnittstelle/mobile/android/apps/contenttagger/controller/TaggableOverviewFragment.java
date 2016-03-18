package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.app.Notification;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.components.controller.CustomDialogController;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.view.ListItemViewHolderTitleSubtitle;

import java.util.List;

/**
 * Created by master on 12.03.16.
 *
 * displays all taggable elements given some tag
 */
public class TaggableOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "TaggableOverviewFragment";

    /*
     * the id argument to be passed
     */
    public static final String ARG_TAG_ID = "tagId";

    /*
     * the event dispatcher
     */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // react to the tag being read
        eventDispatcher.addEventListener(this,new EventMatcher(Event.CRUD.TYPE,Event.CRUD.READ,Tag.class,this), true, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                ((ActionBarActivity)getActivity()).setTitle(String.format(getResources().getString(R.string.tag_lable),event.getData().getName()));
            }
        });

        // declare that we use an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        Tag.read(Tag.class,getArguments().getLong(ARG_TAG_ID), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }


}
