package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
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
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Link;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.view.ListItemViewHolderTitleSubtitle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by master on 12.03.16.
 */
public class LinksOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "LinksOverviewFragment";

    /*
     * the event dispatcher
     */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    /**
     * the adapter for the listview
     */
    private EntityListAdapter<Link,LinksListItemViewHolder> adapter;

    private View contentView;


    /*
     * set event listeners in oncreate
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * intialise the listeners for crud events
         * note that adapter methods do not need to be run on the uithread!
         */
        eventDispatcher.addEventListener(this,new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Link.class), false, new EventListener<Link>() {
            @Override
            public void onEvent(Event<Link> event) {
                adapter.addItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Link.class), false, new EventListener<Link>() {
            @Override
            public void onEvent(Event<Link> event) {
                adapter.updateItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Link.class), false, new EventListener<Link>() {
            @Override
            public void onEvent(Event<Link> event) {
                adapter.removeItem(event.getData());
            }
        });
        // we only react to reading out all tags if we have generated the event ourselves
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Link.class, this), false, new EventListener<List<Link>>() {
            @Override
            public void onEvent(Event<List<Link>> event) {
                for (Link note : event.getData()) {
                    Log.i(logger, "found tags on note: " + note.getTags());
                }
                adapter.addItems(event.getData());
            }
        });

        // we use an options menu
        setHasOptionsMenu(true);
    }

    /*
         * instantiate the adapter in oncreateView
         */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // we create the view only once and reuse it afterwards
        if (this.contentView == null) {
            this.contentView = inflater.inflate(R.layout.links_overview_contentview, container, false);

            this.adapter = new EntityListAdapter<Link, LinksListItemViewHolder>(getActivity(), (RecyclerView) contentView.findViewById(R.id.listview), R.layout.links_overview_itemview, R.layout.links_overview_itemmenu, new int[]{R.id.action_delete, R.id.action_edit, R.id.action_add_tag}) {
                @Override
                public LinksListItemViewHolder onCreateEntityViewHolder(View view, EntityListAdapter adapter) {
                    return new LinksListItemViewHolder(view, adapter);
                }

                @Override
                public void onBindEntityViewHolder(LinksListItemViewHolder holder, Link entity, int position) {
                    Log.d(logger,"onBindEntityViewHolder(): id is: " + entity.getId());
                    holder.title.setText(entity.getTitle());
                    holder.subtitle.setText(entity.getUrl());
                    int numOfTags = entity.getTags() != null ? entity.getTags().size() : 0;
                    if (numOfTags == 0) {
                        holder.numOfTags.setVisibility(View.GONE);
                    }
                    else {
                        holder.numOfTags.setVisibility(View.VISIBLE);
                    }
                    holder.numOfTags.setText(String.valueOf(numOfTags));
                }

                @Override
                protected void onSelectEntity(Link entity) {
                    // on select, we show the editview, passing the entity's id!
                    ((MainNavigationControllerActivity) getActivity()).showView(LinksReadviewFragment.class, MainNavigationControllerActivity.createArguments(LinksEditviewFragment.ARG_LINK_ID, entity.getId()), true);
                }

                @Override
                protected void onSelectEntityMenuAction(int action, final Link entity) {
                    switch (action) {
                        case R.id.action_delete:
                            entity.delete();
                            break;
                        case R.id.action_edit:
                            ((MainNavigationControllerActivity) getActivity()).showView(LinksEditviewFragment.class, MainNavigationControllerActivity.createArguments(LinksEditviewFragment.ARG_LINK_ID, entity.getId()), true);
                            break;
                        case R.id.action_add_tag:
                            AddTagDialogController.getInstance().show(entity);
                            break;
                    }
                }

                @Override
                public void onBindEntityMenuDialog(ItemMenuDialogViewHolder holder, Link item) {
                    ((TextView) holder.heading).setText(item.getTitle());
                }
            };
        }

        prepareSorting();

        return this.contentView;
    }

    private void prepareSorting() {
        List<Comparator<? super Link>> c1 = new ArrayList<Comparator<? super Link>>();
        c1.add(Link.COMPARE_BY_TITLE);
        List<Comparator<? super Link>> c2 = new ArrayList<Comparator<? super Link>>();
        c2.add(Link.COMPARE_BY_TITLE);
        c2.add(Link.COMPARE_BY_DATE);
        List<Comparator<? super Link>> c3 = new ArrayList<Comparator<? super Link>>();
        c3.add(Link.COMPARE_BY_TITLE);
        c3.add(Link.COMPARE_BY_NUM_OF_TAGS);
        adapter.addSortingStrategy(c1);
        adapter.addSortingStrategy(c2);
        adapter.addSortingStrategy(c3);
    }

    // we use an own listitem holder for representing the number of tags
    private class LinksListItemViewHolder extends ListItemViewHolderTitleSubtitle {

        public TextView numOfTags;

        public LinksListItemViewHolder(View itemView, EntityListAdapter adapter) {
            super(itemView,adapter);
            this.numOfTags = (TextView)itemView.findViewById(R.id.numOfTags);
        }

    }


    // track whether we already have read all entries
    private boolean readall;

    @Override
    public void onResume() {
        super.onResume();
        // set the title
        ((ActionBarActivity)getActivity()).setTitle(R.string.menuitem_links);
        // read all notes - updates will be dealt with by event listeners
        if (!readall) {
            Entity.readAll(Link.class, this);
            readall = true;
        }
        // we instantiate the reusable wrapper for the add tag dialogs and specify that on adding tags update shall be executed
        // as the component is resuable, this must be called for each onResume as settings might have been changed in the meantime
        AddTagDialogController.getInstance().attach(getActivity(), true);
    }

    public void onDestroy() {
        super.onDestroy();
        // we remove the fragment as event listener
        eventDispatcher.unbindController(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_links_overview,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            ((MainNavigationControllerActivity)getActivity()).showView(LinksEditviewFragment.class,MainNavigationControllerActivity.createArguments(LinksEditviewFragment.ARG_LINK_ID,-1L),true);

            return true;
        }
        else if (item.getItemId() == R.id.action_sort) {
            this.adapter.sortNext();
        }

        return false;
    }
}
