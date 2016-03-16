package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.os.Bundle;
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
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;
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
public class NotesOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "NotesOverviewFragment";

    /*
     * the event dispatcher
     */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    /**
     * the adapter for the listview
     */
    private EntityListAdapter<Note,ListItemViewHolderTitleSubtitle> adapter;


    /*
     * set event listeners in oncreate
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // intialise the listeners for crud events
        eventDispatcher.addEventListener(this,new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Note.class), false, new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                adapter.addItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Note.class), false, new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                adapter.updateItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Note.class), false, new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                adapter.removeItem(event.getData());
            }
        });
        // we only react to reading out all tags if we have generated the event ourselves
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Note.class, this), false, new EventListener<List<Note>>() {
            @Override
            public void onEvent(Event<List<Note>> event) {
                for (Note note : event.getData()) {
                    Log.i(logger, "found tags on note: " + note.getTags());
                }
                adapter.addItems(event.getData());
            }
        });

        // we instantiate the reusable wrapper for the add tag dialogs and specify that on adding tags update shall be executed
        AddTagDialogController.getInstance().attach(getActivity(), true);

        // we use an options menu
        setHasOptionsMenu(true);
    }

    /*
         * instantiate the adapter in oncreateView
         */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View contentView = inflater.inflate(R.layout.notes_overview_contentview,container,false);

        this.adapter = new EntityListAdapter<Note, ListItemViewHolderTitleSubtitle>(getActivity(),(RecyclerView)contentView.findViewById(R.id.listview),R.layout.notes_overview_itemview,R.layout.notes_overview_itemmenu,new int[]{R.id.action_delete, R.id.action_edit, R.id.action_add_tag}) {
            @Override
            public ListItemViewHolderTitleSubtitle onCreateEntityViewHolder(View view, EntityListAdapter adapter) {
                return new ListItemViewHolderTitleSubtitle(view, adapter);
            }

            @Override
            public void onBindEntityViewHolder(ListItemViewHolderTitleSubtitle holder, Note entity, int position) {
                holder.title.setText(entity.getTitle());
                holder.subtitle.setText(String.valueOf(entity.getLastmodified()));
            }

            @Override
            protected void onSelectEntity(Note entity) {

            }

            @Override
            protected void onSelectEntityMenuAction(int action, final Note entity) {
                switch (action) {
                    case R.id.action_delete:
                        entity.delete();
                        break;
                    case R.id.action_add_tag:
                        AddTagDialogController.getInstance().show(entity);
                        break;
                }
            }

            @Override
            public void onBindEntityMenuDialog(ItemMenuDialogViewHolder holder, Note item) {
                ((TextView)holder.heading).setText(item.getTitle());
            }
        };

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // read all notes
        Entity.readAll(Note.class, this);
    }

    public void onDestroy() {
        super.onDestroy();
        // we remove the fragment as event listener
        eventDispatcher.unbindController(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notes_overview,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            Note note = new Note("lorem","ipsum dolor sit amet");
            note.create();
            return true;
        }
        return false;
    }
}
