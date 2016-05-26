package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.TextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
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

import java.util.List;

/**
 * Created by master on 12.03.16.
 */
public class MediaOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "MediaOverviewFragment";

    /*
     * the event dispatcher
     */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    /**
     * the adapter for the listview
     */
    private EntityListAdapter<Media,MediaListItemViewHolder> adapter;

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
        eventDispatcher.addEventListener(this,new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                adapter.addItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                adapter.updateItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                adapter.removeItem(event.getData());
            }
        });
        // we only react to reading out all tags if we have generated the event ourselves
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Media.class, this), false, new EventListener<List<Media>>() {
            @Override
            public void onEvent(Event<List<Media>> event) {
                for (Media media : event.getData()) {
                    Log.i(logger, "found tags on note: " + media.getTags());
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
            this.contentView = inflater.inflate(R.layout.media_overview_contentview, container, false);

            this.adapter = new EntityListAdapter<Media, MediaListItemViewHolder>(getActivity(), (RecyclerView) contentView.findViewById(R.id.listview), R.layout.media_overview_itemview, R.layout.media_overview_itemmenu, new int[]{R.id.action_delete, R.id.action_edit, R.id.action_add_tag}) {
                @Override
                public MediaListItemViewHolder onCreateEntityViewHolder(View view, EntityListAdapter adapter) {
                    return new MediaListItemViewHolder(view, adapter);
                }

                @Override
                public void onBindEntityViewHolder(MediaListItemViewHolder holder, Media entity, int position) {
                    Log.d(logger,"onBindEntityViewHolder(): id is: " + entity.getId());
                    holder.title.setText(entity.getTitle());
                    holder.subtitle.setText(String.valueOf(entity.getCreated()));
                    holder.mediaContent.setImageURI(Uri.parse(entity.getContentUri()));
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
                protected void onSelectEntity(Media entity) {
                    // on select, we show the editview, passing the entity's id!
                    ((MainNavigationControllerActivity) getActivity()).showView(MediaEditviewFragment.class, MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_ID, entity.getId()), true);
                }

                @Override
                protected void onSelectEntityMenuAction(int action, final Media entity) {
                    switch (action) {
                        case R.id.action_delete:
                            entity.delete();
                            break;
                        case R.id.action_edit:
                            ((MainNavigationControllerActivity) getActivity()).showView(MediaEditviewFragment.class, MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_ID, entity.getId()), true);
                            break;
                        case R.id.action_add_tag:
                            AddTagDialogController.getInstance().show(entity);
                            break;
                    }
                }

                @Override
                public void onBindEntityMenuDialog(ItemMenuDialogViewHolder holder, Media item) {
                    ((TextView) holder.heading).setText(item.getTitle());
                }
            };
        }

        return this.contentView;
    }

    // we use an own listitem holder for representing the number of tags
    private class MediaListItemViewHolder extends ListItemViewHolderTitleSubtitle {

        public TextView numOfTags;
        public ImageView mediaContent;

        public MediaListItemViewHolder(View itemView, EntityListAdapter adapter) {
            super(itemView,adapter);
            this.numOfTags = (TextView)itemView.findViewById(R.id.numOfTags);
            this.mediaContent = (ImageView)itemView.findViewById(R.id.mediaContent);
        }

    }


    // track whether we already have read all entries
    private boolean readall;

    @Override
    public void onResume() {
        super.onResume();
        // set the title
        ((ActionBarActivity)getActivity()).setTitle(R.string.menuitem_media);
        // read all notes - updates will be dealt with by event listeners
        if (!readall) {
            Entity.readAll(Media.class, this);
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
        inflater.inflate(R.menu.menu_media_overview,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            ((MainNavigationControllerActivity)getActivity()).showView(MediaEditviewFragment.class,MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_ID,-1L),true);

            return true;
        }
        return false;
    }
}
