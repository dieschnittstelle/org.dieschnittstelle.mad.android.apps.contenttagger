package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
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

import java.util.ArrayList;
import java.util.List;

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

/**
 * Created by master on 12.03.16.
 */
public class TagsOverviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "TagsOverviewFragment";

    /*
     * the event dispatcher
     */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    /*
     * the adapter for the listview
     */
    private EntityListAdapter<Tag,ListItemViewHolderTitleSubtitle> adapter;

    /*
     * the dialog for creating/editing tags
     */
    private CustomDialogController<Tag> editTagDialogController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * declare the listeners for the crud events in onCreate() rather than in onCreateView() in order to avoid duplicated additions
         */
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Tag.class), false, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                Log.d(logger,"onEvent(): tag created");
                adapter.addItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Tag.class), false, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                Log.d(logger,"onEvent(): tag updated");
                adapter.updateItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Tag.class), false, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                Log.d(logger,"onEvent(): tag deleted");
                adapter.removeItem(event.getData());
            }
        });
        // we only react to reading out all tags if we have generated the event ourselves
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.READALL, Tag.class, this), false, new EventListener<List<Tag>>() {
            @Override
            public void onEvent(Event<List<Tag>> event) {
                Log.d(logger, "onEvent(): tags read");
                adapter.addItems(event.getData());
            }
        });

        // declare that we use an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // the view - note that in order to obtain match_parent height, it needs to be instantiated as follows, see http://stackoverflow.com/questions/24503760/cardview-layout-width-match-parent-does-not-match-parent-recyclerview-width
        View contentView = inflater.inflate(R.layout.tags_overview_contentview, container, false);

        // create an adapter for the recycler view
        this.adapter = new EntityListAdapter<Tag,ListItemViewHolderTitleSubtitle>(this.getActivity(), (RecyclerView) contentView.findViewById(R.id.listview), R.layout.tags_overview_itemview, R.layout.tags_overview_itemmenu, new int[]{R.id.action_delete, R.id.action_edit}) {

            @Override
            public ListItemViewHolderTitleSubtitle onCreateEntityViewHolder(View view, EntityListAdapter adapter) {
                return new ListItemViewHolderTitleSubtitle(view,adapter);
            }

            @Override
            public void onBindEntityViewHolder(ListItemViewHolderTitleSubtitle holder, Tag entity, int position) {
                holder.title.setText(entity.getName() + "-" + entity.getId());
            }

            @Override
            protected void onSelectEntity(Tag entity) {
                Log.i(logger, "onSelectEntity(): " + entity);
            }

            @Override
            protected void onSelectEntityMenuAction(int action, Tag entity) {
                Log.i(logger, "onSelectEntityMenuAction(): " + action + "@" + entity);
                if (action == R.id.action_delete) {
                    entity.delete();
                }
                else if (action == R.id.action_edit) {
                    editTagDialogController.show(entity);
                }
            }

            @Override
            public void onBindEntityMenuDialog(EntityListAdapter.ItemMenuDialogViewHolder holder, Tag item) {
                ((TextView)holder.heading).setText(item.getName());
            }
        };

        // we initialise the dialog
        createEditTagDialogController();

        return contentView;
    }

    // we start populating the view onresumt
    @Override
    public void onResume() {
        super.onResume();
        Log.d(logger, "onResume()");

        Tag.readAll(Tag.class, this);
    }

    public void onDestroy() {
        super.onDestroy();
        // we remove the fragment as event listener
        eventDispatcher.unbindController(this);
    }

    public void onDetach() {
        super.onDestroy();
        Log.d(logger, "onDetach()");
    }

    public void createEditTagDialogController() {
        this.editTagDialogController = new CustomDialogController<Tag>(this.getActivity(),R.layout.dialog_inputtext) {
            @Override
            protected void onBindViewHolder(boolean bound) {
                // the listener for create/edit only needs to be set once
                this.title.setText(data.created() ? R.string.title_edit_tag : R.string.title_create_tag);
                if (this.data.created()) {
                    this.input.setText(this.data.getName());
                }
                else {
                    this.input.setText("");
                }
                this.primaryButton.setText(data.created() ? R.string.action_update : R.string.action_create);
                this.secondaryButton.setText(R.string.action_cancel);
                if (!bound) {

                    this.primaryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // we set the value of the input field on the tag (we ignore empty values here)
                            data.setName(input.getText().toString());
                            if (data.created()) {
                                data.update();
                            }
                            else {
                                data.create();
                            }
                            // we close the dialog
                            hide();
                        }
                    });
                }
            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_tags_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add) {
            this.editTagDialogController.show(new Tag());
        }

        return super.onOptionsItemSelected(item);
    }



}
