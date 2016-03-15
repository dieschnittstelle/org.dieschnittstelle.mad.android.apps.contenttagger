package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.components.controller.CustomDialogController;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.view.ListItemViewHolderTitleSubtitle;

import java.util.List;

/**
 * Created by master on 12.03.16.
 */
public class NotesOverviewFragment extends Fragment implements EventGenerator {

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
     * the alert dialog for adding a tag
     */
    private CustomDialogController<?> addTagDialog;

    /*
     * instantiate the adapter oncreateView
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
                        new AsyncTask<Void,Void,List<Tag>>() {

                            @Override
                            protected List<Tag> doInBackground(Void... params) {
                                return (List<Tag>)Tag.readAllSync(Tag.class);
                            }

                            @Override
                            protected void onPostExecute(List<Tag> tags) {
                                // we add the first element of the tags to the note
                                entity.addTag(tags.get(0));
                                // and then we try to update it!
                                entity.update();
                            }
                        }.execute();
                        break;
                }
            }

            @Override
            public void onBindEntityMenuDialog(ItemMenuDialogViewHolder holder, Note item) {
                ((TextView)holder.heading).setText(item.getTitle());
            }
        };

        // intialise the listeners for crud events
        eventDispatcher.addEventListener(new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Note.class), new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                adapter.addItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Note.class), new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                adapter.updateItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Note.class), new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                adapter.removeItem(event.getData());
            }
        });
        // we only react to reading out all tags if we have generated the event ourselves
        eventDispatcher.addEventListener(new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Note.class, this), new EventListener<List<Note>>() {
            @Override
            public void onEvent(Event<List<Note>> event) {
                for (Note note : event.getData()) {
                    Log.i(logger,"found tags on note: " + note.getTags());
                }
                adapter.addItems(event.getData());
            }
        });


        // we initialise the dialogs
        initialiseAddTagDialog();

        // we use an options menu
        setHasOptionsMenu(true);

        return contentView;
    }



    @Override
    public void onResume() {
        super.onResume();
        // read all notes
        Entity.readAll(Note.class, this);
    }


    private void initialiseAddTagDialog() {

        this.addTagDialog = new CustomDialogController<Object>(this.getActivity(),R.layout.dialog_select_tag) {
            @Override
            protected void onBindViewHolder(boolean bound) {
                this.title.setText(R.string.action_select_tag);
                this.primaryButton.setText(R.string.action_select);
            }
        };

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
