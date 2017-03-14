package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
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
import android.widget.Toast;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;
import org.dieschnittstelle.mobile.android.components.controller.CustomDialogController;
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.model.Entity;

/**
 * Created by master on 17.03.16.
 *
 * use slidinguppanel: https://www.numetriclabz.com/implementation-of-sliding-up-panel-using-androidslidinguppanel-in-android-tutorial
 */
public class NotesEditviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "NotesEditviewFragment";

    /*
     * we expect that the id of the item to be displayed is passed to us, rather than the item itself...
     */
    public static final String ARG_NOTE_ID = TaggableOverviewFragment.OUTARG_SELECTED_ITEM_ID;

    /*
     * the ui elements
     */
    protected EditText title;
    protected EditText content;
    protected TagsbarController tagsbarController;
    protected AttachmentsPanelController attachmentsController;

    /*
     * the model object that we use
     */
    protected Note note;

    /*
     * we need to handle obsoletion of readview (which is a subclass) here...
     */
    protected boolean obsolete;

    protected boolean saved;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // we read out the id from the arguments
        long noteId = getArguments().getLong(ARG_NOTE_ID);

        addEventListeners();

        setHasOptionsMenu(true);
    }

    protected void addEventListeners() {
        // set listeners
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.DELETED,Event.CRUD.UPDATED,Event.CRUD.CREATED), Note.class), false, new EventListener<Note>() {
            @Override
            public void onEvent(Event<Note> event) {
                if (note == event.getData()) {
                    saved = true;
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
                Log.i(logger,"onEvent(): read(): " + note);
                ((ActionBarActivity) getActivity()).setTitle(note.getTitle());
                title.setText(note.getTitle());
                content.setText(note.getContent());
                tagsbarController.bindTaggable(note);
                attachmentsController.bindTaggable(note);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        LifecycleHandling.onResume(this);
        if (obsolete) {
            Log.i(logger,"view is obsolete.");
        }
        else {
            Log.d(logger, "onResume(): " + this.getClass());
            // we read out the id from the arguments
            long noteId = getArguments().getLong(ARG_NOTE_ID);
            if (noteId > -1) {
//                ((ActionBarActivity) getActivity()).setTitle(R.string.title_edit_note);
                // read all notes - reaction will be dealt with by event handler
                Note.read(Note.class, noteId, this);
            } else if (note == null) {
                ((ActionBarActivity) getActivity()).setTitle(R.string.title_create_note);
                note = new Note();
                // this is required in order for the tagbar to be available for new notes
                tagsbarController.bindTaggable(note);
                attachmentsController.bindTaggable(note);
            }

            // we instantiate the reusable wrapper for the add tag dialogs - needs to be done for each onResume() as settings might have changed in the meantime
            AddTagDialogController.getInstance().attach(getActivity());
        }
    }

    @Override
    /*
     * onPause cannot be interrupted, but at least we can perform a create/update *after* unbinding the listeners
     */
    public void onPause() {
        super.onPause();
        LifecycleHandling.onPause(this);

        Log.i(logger, "onPause(): saved: " + this.saved);
        if (!(this instanceof NotesReadviewFragment)) {
            if (!saved && pendingRequestCode == -1) {
                // set saved manually as the event handlers will not react
                saved = true;
                createOrUpdateNote();
            }
        }
    }

    private void createOrUpdateNote() {
        // this is duplicated from above
        note.setTitle(this.title.getText().toString());
        note.setContent(this.content.getText().toString());

        if (this.note.created()) {
            Log.i(logger,"createOrUpdateNote(): updating note");
            this.note.update();
        }
        else {
            Log.i(logger,"createOrUpdateNote(): creating note");
            this.note.create();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LifecycleHandling.onDestroy(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View contentView = inflater.inflate(R.layout.notes_editview,container,false);

        this.title = (EditText)contentView.findViewById(R.id.title);
        this.content = (EditText)contentView.findViewById(R.id.content);

        // TODO: tagsbarController could be refactored such that reading out the tagsbar will be encapsulated therein
        this.tagsbarController = new TagsbarController(this,(ViewGroup)contentView.findViewById(R.id.tagsbar),R.layout.tagsbar_itemview);
        this.attachmentsController = new AttachmentsPanelController(this,contentView);

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
                createOrUpdateNote();
                return true;
            case R.id.action_delete:
                if (note.created()) {
                    //note.delete();
                    confirmDeleteTaggable(this.getActivity(),this.note);
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

    // a dialog for reconfirming deletion
    public static void confirmDeleteTaggable(final Activity context, Taggable taggable) {
        (new CustomDialogController<Taggable>(context, R.layout.dialog_confirm) {
            @Override
            protected void onBindViewHolder(boolean bound) {
                if (!bound) {
                    this.title.setText(R.string.title_delete_taggable);
                    this.primaryButton.setText(R.string.action_confirm);
                    this.secondaryButton.setText(R.string.action_cancel);
                    this.primaryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // create a new tag and assign it
                            new AsyncTask<Taggable, Void, Taggable>() {

                                @Override
                                protected Taggable doInBackground(Taggable... params) {
                                    // TODO: sync operations do not trigger event listeners, this could be changed at some moment...
                                    if (params[0].created()) {
                                        params[0].delete();
//                                        params[0].deleteSync();
//                                        // but we can trigger an event ourselves...
//                                        EventDispatcher.getInstance().notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.DELETED, params[0].getClass(), null, params[0]));
                                        return params[0];
                                    }
                                    else {
                                        return null;
                                    }
                                }

                                @Override
                                protected void onPostExecute(Taggable taggable) {
                                    if (taggable != null) {
                                        Toast.makeText(context,String.format(context.getResources().getString(R.string.message_feedback_delete_taggable_ok),data.getTitle()),Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(context,String.format(context.getResources().getString(R.string.message_feedback_delete_taggable_nok),data.getTitle()),Toast.LENGTH_SHORT).show();
                                    }
                                }

                            }.execute(data);

                            hide();
                        }
                    });
                }
                this.message.setText(String.format(controller.getResources().getString(R.string.message_confirm_delete_taggable), (data.getTitle())));
            }
        }).show(taggable);
    }

    // a dialog for reconfirming deletion
    public static void confirmSaveTaggable(final Activity context, Taggable taggable) {
        Log.i(logger,"confirmSaveTaggable()");
        (new CustomDialogController<Taggable>(context, R.layout.dialog_confirm) {
            @Override
            protected void onBindViewHolder(boolean bound) {
                Log.i(logger,"onBindViewHolder()");
                if (!bound) {
                    this.title.setText(R.string.title_save_taggable
                    );
                    this.primaryButton.setText(R.string.action_confirm);
                    this.secondaryButton.setText(R.string.action_cancel);
                    this.primaryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // create a new tag and assign it
                            new AsyncTask<Taggable, Void, Taggable>() {

                                @Override
                                protected Taggable doInBackground(Taggable... params) {
                                    // TODO: sync operations do not trigger event listeners, this could be changed at some moment...
                                    if (params[0].created()) {
                                        params[0].updateSync();
                                        // but we can trigger an event ourselves...
                                        EventDispatcher.getInstance().notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.UPDATED, params[0].getClass(), null, params[0]));
                                        return params[0];
                                    }
                                    else {
                                        params[0].createSync();
                                        // but we can trigger an event ourselves...
                                        EventDispatcher.getInstance().notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.CREATED, params[0].getClass(), null, params[0]));
                                        return params[0];
                                    }
                                }

                                @Override
                                protected void onPostExecute(Taggable taggable) {
                                    if (taggable != null) {
                                        Toast.makeText(context,String.format(context.getResources().getString(R.string.message_feedback_save_taggable_ok),data.getTitle()),Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        Toast.makeText(context,String.format(context.getResources().getString(R.string.message_feedback_save_taggable_nok),data.getTitle()),Toast.LENGTH_SHORT).show();
                                    }
                                }

                            }.execute(data);

                            hide();
                        }
                    });
                }
                this.message.setText(String.format(controller.getResources().getString(R.string.message_confirm_delete_taggable), (data.getTitle())));
            }
        }).show(taggable);
    }

    // we need to track whether we are pending to receive a result in order to distinguish whether onPause() is forward-leaving or backward-leaving
    private int pendingRequestCode = -1;

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        this.pendingRequestCode = requestCode;
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.pendingRequestCode = -1;
        if (attachmentsController.handlesResult(requestCode)) {
            attachmentsController.onActivityResult(requestCode,resultCode,data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
