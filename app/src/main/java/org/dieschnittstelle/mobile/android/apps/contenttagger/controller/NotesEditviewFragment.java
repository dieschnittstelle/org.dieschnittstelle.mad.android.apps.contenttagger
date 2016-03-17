package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;

/**
 * Created by master on 17.03.16.
 */
public class NotesEditviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "NotesEditviewFragment";

    /*
     * we expect that the id of the item to be displayed is passed to us, rather than the item itself...
     */
    public static final String ARG_NOTE_ID = "noteId";

    /*
     * the ui elements
     */
    private EditText title;
    private EditText content;
    private TagsbarController tagsbarController;

    /*
     * the model object that we use
     */
    protected Note note;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // we read out the id from the arguments
        long noteId = getArguments().getLong(ARG_NOTE_ID);

        // set listeners
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.DELETED,Event.CRUD.UPDATED,Event.CRUD.CREATED), Note.class), false, new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                if (note == event.getData()) {
                    getFragmentManager().popBackStack();
                }
                else {
                    Log.i(logger, "onEvent(): got " + event.getType() + " event for note, but it involves a different object than the one being edited: " + event.getData() + ". Ignore...");
                }
            }
        });

        // also handle read events that are generated by ourselves
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READ, Note.class, this), true, new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                note = event.getData();
                title.setText(note.getTitle());
                content.setText(note.getContent());
                tagsbarController.bindTaggable(note);
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // we read out the id from the arguments
        long noteId = getArguments().getLong(ARG_NOTE_ID);
        if (noteId > -1) {
            ((ActionBarActivity)getActivity()).setTitle(R.string.title_edit_note);
            // read all notes - reaction will be dealt with by event handler
            Note.read(Note.class, noteId, this);
        }
        else {
            ((ActionBarActivity)getActivity()).setTitle(R.string.title_create_note);
            note = new Note();
        }

        // we instantiate the reusable wrapper for the add tag dialogs - needs to be done for each onResume() as settings might have changed in the meantime
        AddTagDialogController.getInstance().attach(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventDispatcher.getInstance().unbindController(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View contentView = inflater.inflate(R.layout.notes_editview,container,false);

        this.title = (EditText)contentView.findViewById(R.id.title);
        this.content = (EditText)contentView.findViewById(R.id.content);

        this.tagsbarController = new TagsbarController(this,(ViewGroup)contentView.findViewById(R.id.tagsbar),R.layout.tagsbar_itemview);

        return contentView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_notes_editview,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // we check the known options
        switch (item.getItemId()) {
            case R.id.action_save:
                // bind the data from the input form to the item
                note.setTitle(this.title.getText().toString());
                note.setContent(this.content.getText().toString());
                if (note.created()) {
                    note.update();
                }
                else {
                    note.create();
                }
                return true;
            case R.id.action_delete:
                if (note.created()) {
                    note.delete();
                }
                return true;
            case R.id.action_add_tag:
                AddTagDialogController.getInstance().show(note);
                break;
            case R.id.action_paste_default:
                content.setText(content.getText() + "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
                return true;
        }

        return false;
    }

}