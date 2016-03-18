package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;
import org.dieschnittstelle.mobile.android.components.controller.CustomDialogController;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by master on 16.03.16.
 */
public class AddTagDialogController implements EventListenerOwner {

    protected static String logger = "AddTagDialogController";

    private static AddTagDialogController instance = new AddTagDialogController();

    public static AddTagDialogController getInstance() {
        return instance;
    }

    /*
     * an activity as main controller
     */
    private Activity controller;

    /*
     * the alert dialog for adding a tag
     */
    private CustomDialogController<Taggable> addTagDialog;

    /*
     * the alert dialog for creating a new tag (which will be passed an array of a tag and a taggable
     */
    private CustomDialogController<Entity[]> confirmNewTagDialog;

    /*
     * the map of tags that will be offered for autocompletion
     */
    private Map<String, Tag> tagsMap = new HashMap<String, Tag>();

    /*
     * specify whether we update on add
     */
    private boolean updateOnAdd;


    private AddTagDialogController() {

    }

    public void show(Taggable entity) {
        addTagDialog.show(entity);
    }

    /*
     * on attachment, we instantiate the dialogs again, but check whether it is necessary
     */
    public void attach(Activity controller) {
        attach(controller,false);
    }

    public void attach(Activity controller, boolean updateOnAdd) {
        this.updateOnAdd = updateOnAdd;
        if (controller == this.controller) {
            return;
        }
        this.controller = controller;
        this.tagsMap.clear();
        initialiseAddTagDialog();
        initaliseConfirmNewTagDialog();

        // register for tag crud events
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Tag.class), false, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                Log.d(logger,"adding new tag: " + event.getData());
                tagsMap.put(event.getData().getName(), event.getData());
            }
        });
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Tag.class), false, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                Log.d(logger,"updating tag: " + event.getData());
                // well... we need to iterate
                for (String key : tagsMap.keySet()) {
                    if (tagsMap.get(key).getId() == event.getData().getId()) {
                        tagsMap.remove(key);
                        tagsMap.put(event.getData().getName(), event.getData());
                        break;
                    }
                }
            }
        });
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.DELETED, Tag.class), false, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                Log.d(logger,"removing tag: " + event.getData());
                tagsMap.remove(event.getData().getName());
            }
        });
    }

    private void initialiseAddTagDialog() {

        new AsyncTask<Void, Void, List<Tag>>() {

            @Override
            protected List<Tag> doInBackground(Void... params) {
                return (List<Tag>) Tag.readAllSync(Tag.class);
            }

            @Override
            protected void onPostExecute(List<Tag> tags) {
                // we add the tags
                for (Tag tag : tags) {
                    tagsMap.put(tag.getName(), tag);
                }
                // then instantiate the dialog
                addTagDialog = new CustomDialogController<Taggable>(controller, R.layout.dialog_inputtext_autocomplete) {
                    @Override
                    protected void onBindViewHolder(boolean bound) {
                        if (!bound) {
                            this.title.setText(R.string.action_select_tag);
                            this.input.setHint(R.string.hint_select_tag);
                            this.primaryButton.setText(R.string.action_select);
                            this.primaryButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // we check whether the text of the input field is the name of an exising tag or not
                                    Tag existingTag = tagsMap.get(input.getText().toString());
                                    if (existingTag != null) {
                                        Log.d(logger, "add existing tag to note...");
                                        data.addTag(existingTag);
                                        EventDispatcher.getInstance().notifyListeners(new Event<TagsbarController.TaggableTagBundle>(Event.UI.TYPE, TagsbarController.EVENT_TAG_ADDED, TagsbarController.TaggableTagBundle.class, new TagsbarController.TaggableTagBundle(data, existingTag)));
                                        if (updateOnAdd) {
                                            Log.d(logger, "run updateOnAdd...");
                                            ((Entity)data).update();
                                        }
                                    } else {
                                        confirmNewTagDialog.show(new Entity[]{new Tag(input.getText().toString()), (Entity) data});
                                    }
                                    hide();
                                }
                            });
                        }
                        Log.d(logger, "input element: " + this.input);
                        this.input.setText("");
                        // for each invocation instantiate the input field
                        ((AutoCompleteTextView) this.input).setAdapter(new ArrayAdapter<String>(controller, R.layout.dialog_inputtext_autocomplete_itemview, new ArrayList(tagsMap.keySet())));
                    }
                };
            }

        }.execute();

    }

    private void initaliseConfirmNewTagDialog() {
        this.confirmNewTagDialog = new CustomDialogController<Entity[]>(this.controller, R.layout.dialog_confirm) {
            @Override
            protected void onBindViewHolder(boolean bound) {
                if (!bound) {
                    this.title.setText(R.string.title_create_tag);
                    this.primaryButton.setText(R.string.action_confirm);
                    this.secondaryButton.setText(R.string.action_cancel);
                    this.primaryButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // create a new tag and assign it
                            new AsyncTask<Tag, Void, Tag>() {

                                @Override
                                protected Tag doInBackground(Tag... params) {
                                    params[0].createSync();
                                    return params[0];
                                }

                                @Override
                                protected void onPostExecute(Tag tag) {
                                    ((Taggable) data[1]).addTag(tag);
                                    EventDispatcher.getInstance().notifyListeners(new Event<TagsbarController.TaggableTagBundle>(Event.UI.TYPE,TagsbarController.EVENT_TAG_ADDED,TagsbarController.TaggableTagBundle.class,new TagsbarController.TaggableTagBundle( ((Taggable) data[1]),tag)));
                                    if (updateOnAdd) {
                                        Log.d(logger, "run updateOnAdd...");
                                        data[1].update();
                                    }
                                }

                            }.execute((Tag) data[0]);

                            hide();
                        }
                    });
                }
                this.message.setText(String.format(controller.getResources().getString(R.string.message_confirm_create_tag), ((Tag) data[0]).getName()));
            }
        };
    }

}
