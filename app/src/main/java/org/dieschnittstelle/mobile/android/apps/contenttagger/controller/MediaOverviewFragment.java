package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
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
import org.dieschnittstelle.mobile.android.components.controller.LifecycleHandling;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.view.EntityViewHolderWithBinding;
import org.dieschnittstelle.mobile.android.components.view.ListItemViewHolderTitleSubtitle;

import java.util.ArrayList;
import java.util.Comparator;
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
    private EntityListAdapter<Media, EntityViewHolderWithBinding> adapter;

    private View contentView;

    // check whether we show attachments or not
    private boolean showAttachments;

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
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.CREATED, Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                adapter.addItem(event.getData());
            }
        });
        eventDispatcher.addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.CRUD.UPDATED, Media.class), false, new EventListener<Media>() {
            @Override
            public void onEvent(Event<Media> event) {
                Log.i(logger, "onEvent(): updated: " + event.getData());
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
                List<Media> selected = new ArrayList<Media>();
                for (Media media : event.getData()) {
                    // we only display those media items that have not been created as attachments - TODO: rethink this at some moment
                    if (media.getAttachers().size() == 0 || showAttachments) {
                        selected.add(media);
                    }
                }
                adapter.clear();
                adapter.addItems(selected);
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

            this.adapter = new EntityListAdapter<Media, EntityViewHolderWithBinding>(this, (RecyclerView) contentView.findViewById(R.id.listview), R.layout.media_overview_itemview_binding, R.layout.media_overview_itemmenu, new int[]{R.id.action_delete, R.id.action_edit, R.id.action_add_tag}) {

                @Override
                public void onBindEntityViewHolder(final EntityViewHolderWithBinding holder, final Media entity, final int position) {
                    Log.d(logger, "onBindEntityViewHolder(): id is: " + entity.getId());
                    // we first apply the default binding, which apart from image setting will realise all databinding previously done manually
                    super.onBindEntityViewHolder(holder,entity,position);

                    // then load the image
                    if (entity.getContentUri() != null) {
                        entity.loadThumbnail(getActivity(), new Media.OnImageLoadedHandler() {
                            @Override
                            public void onImageLoaded(Bitmap thumbnail) {
                                ((ImageView) holder.getBinding().getRoot().findViewById(R.id.mediaContent)).setImageBitmap(thumbnail);
                            }
                        });
                    }
                }

                @Override
                protected void onSelectEntity(Media entity) {
                    // on select, we show the editview, passing the entity's id!
                    Bundle args = MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_ID, entity.getId());
                    args.putSerializable(MediaEditviewFragment.ARG_MODE, MediaEditviewFragment.Mode.READ);
                    ((MainNavigationControllerActivity) getActivity()).showView(MediaEditviewFragment.class, args, true);
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

            prepareSorting();
        }

        return this.contentView;
    }

    // this is a custom function called by an event handler declared on the view
    public void showImagePager(int position) {
        // we open the pager fragment
        Bundle args = MainNavigationControllerActivity.createArguments(MediaPagerFragment.ARG_SELECTED_MEDIA_POS, position);
        List<Long> mediaIds = new ArrayList<Long>();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            mediaIds.add(adapter.getItemAt(i).getId());
        }
        args.putSerializable(MediaPagerFragment.ARG_DISPLAY_MEDIA, (ArrayList) mediaIds);
        ((MainNavigationControllerActivity) getActivity()).showView(MediaPagerFragment.class, args, true);
    }

    private void prepareSorting() {
        List<Comparator<? super Media>> c1 = new ArrayList<Comparator<? super Media>>();
        c1.add(Media.COMPARE_BY_TITLE);
        List<Comparator<? super Media>> c2 = new ArrayList<Comparator<? super Media>>();
        c2.add(Media.COMPARE_BY_TITLE);
        c2.add(Media.COMPARE_BY_DATE);
        List<Comparator<? super Media>> c3 = new ArrayList<Comparator<? super Media>>();
        c3.add(Media.COMPARE_BY_TITLE);
        c3.add(Media.COMPARE_BY_NUM_OF_TAGS);
        adapter.addSortingStrategy(c1);
        adapter.addSortingStrategy(c2);
        adapter.addSortingStrategy(c3);
    }

    // we use an own listitem holder for representing the number of tags
    private class MediaListItemViewHolder extends ListItemViewHolderTitleSubtitle {

        public TextView numOfTags;
        public ImageView mediaContent;

        public MediaListItemViewHolder(View itemView, EntityListAdapter adapter) {
            super(itemView, adapter);
            this.numOfTags = (TextView) itemView.findViewById(R.id.numOfTags);
            this.mediaContent = (ImageView) itemView.findViewById(R.id.mediaContent);
        }

    }


    // track whether we already have read all entries
    private boolean readall;

    @Override
    public void onResume() {
        super.onResume();
        // set the title
        ((ActionBarActivity) getActivity()).setTitle(R.string.menuitem_media);
        // read all notes - updates will be dealt with by event listeners
        if (!readall) {
            Entity.readAll(Media.class, this);
            readall = true;
        }
        // we instantiate the reusable wrapper for the add tag dialogs and specify that on adding tags update shall be executed
        // as the component is resuable, this must be called for each onResume as settings might have been changed in the meantime
        AddTagDialogController.getInstance().attach(getActivity(), true);
        LifecycleHandling.onResume(this);
    }

    public void onDestroy() {
        super.onDestroy();
        // we remove the fragment as event listener
        LifecycleHandling.onDestroy(this);
    }

    public void onPause() {
        super.onPause();
        LifecycleHandling.onPause(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_media_overview, menu);
        MenuItem item = menu.findItem(R.id.action_toggle_attachments);
        if (item != null) {
            item.setTitle(this.showAttachments ? R.string.action_hide_attachments : R.string.action_show_attachments);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            ((MainNavigationControllerActivity) getActivity()).showView(MediaEditviewFragment.class, MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_ID, -1L), true);

            return true;
        } else if (item.getItemId() == R.id.action_sort) {
            this.adapter.sortNext();
        } else if (item.getItemId() == R.id.action_toggle_attachments) {
            Log.d(logger, "toggleAttachments(): showAttachments: " + this.showAttachments);
            this.showAttachments = !this.showAttachments;
            this.getActivity().invalidateOptionsMenu();
            Media.readAll(Media.class, this);
        }

        return false;
    }

    // we need binding adapter for being able to set the typeface attribute...
    @BindingAdapter("android:typeface")
    public static void setTypeface(TextView view, int style) {
        view.setTypeface(null, style);
    }

}
