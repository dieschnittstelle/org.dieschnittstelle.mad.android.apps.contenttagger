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
import android.widget.ImageView;
import android.widget.TextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;
import org.dieschnittstelle.mobile.android.components.controller.CustomDialogController;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;
import org.dieschnittstelle.mobile.android.components.view.ListItemViewHolderTitleSubtitle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
     * the id to identify a selected item - this needs to be supported by the readview controllers
     */
    public static final String OUTARG_SELECTED_ITEM_ID = "taggableId";

    /*
     * the event dispatcher
     */
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    /*
     * the listview which displays all content items associated with the tag, grouped by their type
     * - we do not use a ListView or RecyclerView class, but create the view manually
     */
    private ViewGroup listview;

    /*
     * the selection listener - we only need one instance which will be passed to all items
     */
    private View.OnClickListener selectionListener = new TaggableSelectedListener();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // react to the tag being read
        eventDispatcher.addEventListener(this,new EventMatcher(Event.CRUD.TYPE,Event.CRUD.READ,Tag.class,this), true, new EventListener<Tag>() {
            @Override
            public void onEvent(Event<Tag> event) {
                ((ActionBarActivity)getActivity()).setTitle(String.format(getResources().getString(R.string.tag_lable),event.getData().getName()));
                showTaggedItemsForTag(event.getData());
            }
        });

        // declare that we use an options menu
        setHasOptionsMenu(true);
    }

    /*
     * we currently create the listview on any apperance of this fragment without any reuse...
     */
    private void showTaggedItemsForTag(Tag tag) {
        listview.removeAllViews();

        Map<Class<Taggable>,List<Taggable>> itemGroups = tag.getTaggedItemsAsGroups();
        for (Class<Taggable> group : itemGroups.keySet()) {
            addTaggedItemGroupHeaderForGroup(group);
            for (Taggable item : itemGroups.get(group)) {
                addTaggedItemForGroup(group,item);
            }
        }
    }

    private void addTaggedItemGroupHeaderForGroup(Class<Taggable> group) {
        // inflate the layout
        View headerLayout = getActivity().getLayoutInflater().inflate(R.layout.taggable_overview_itemgroup_header,listview,false);
        // from the navigation configuration, we can read out both the title and the icon for the given class
        int pos = Arrays.asList(getResources().getStringArray(R.array.main_menu_modelclasses)).indexOf(group.getName());
        if (pos == -1) {
            Log.e(logger,"could not find index for taggable class, cannot determine navigation settings: " + group.getName());
        }
        else {
            ((TextView) headerLayout.findViewById(R.id.itemname)).setText(getResources().obtainTypedArray(R.array.main_menu_items).getResourceId(pos,-1));
            ((ImageView) headerLayout.findViewById(R.id.itemicon)).setImageDrawable(getResources().getDrawable(getResources().obtainTypedArray(R.array.main_menu_icons).getResourceId(pos,-1)));
        }
        listview.addView(headerLayout);
    }

    private void addTaggedItemForGroup(Class<Taggable>group, Taggable item) {
        // we inflate the layout
        View itemLayout = getActivity().getLayoutInflater().inflate(R.layout.taggable_overview_itemview_default,listview,false);
        // populate the view
        ((TextView)itemLayout.findViewById(R.id.title)).setText(item.getTitle());
        TextView numOfTags = (TextView)itemLayout.findViewById(R.id.numOfTags);
        // we only display items that have more tags in addition to the one displayed here
        if (item.getTags() != null && item.getTags().size() > 1) {
            numOfTags.setText("+"+String.valueOf(item.getTags().size()-1));
            numOfTags.setVisibility(View.VISIBLE);
        }
        else {
            numOfTags.setVisibility(View.GONE);
        }
        itemLayout.setTag(item);
        itemLayout.setOnClickListener(this.selectionListener);
        // add the layout to the listview
        listview.addView(itemLayout);
    }

    @Override
    public void onResume() {
        super.onResume();
        Tag.read(Tag.class, getArguments().getLong(ARG_TAG_ID), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View contentView = inflater.inflate(R.layout.taggable_overview_contentview,container,false);
        listview = (ViewGroup)contentView.findViewById(R.id.listview);

        return contentView;
    }

    private void showDetailviewForItem(Taggable item) {
        Log.d(logger,"showDetailviewForItem(): " + item);
        int pos = Arrays.asList(getResources().getStringArray(R.array.main_menu_modelclasses)).indexOf(item.getClass().getName());
        if (pos != -1) {
            try {
                Class<Fragment> controller = (Class<Fragment>)Class.forName(getResources().getStringArray(R.array.main_menu_detailviews)[pos]);
                ((MainNavigationControllerActivity)getActivity()).showView(controller,MainNavigationControllerActivity.createArguments(OUTARG_SELECTED_ITEM_ID,item.getId()),true);
            }
            catch (Exception e) {
                Log.e(logger,"showDetailviewForItem(): got exception trying to load class for detailview for item " + item + ": " + e,e);
            }
        }
        else {
            Log.e(logger,"cannot determine the navigation settings for item of class " + item.getClass());
        }
    }

    /*
     * a listener for item selection
     */
    private class TaggableSelectedListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // obtain the item
            Taggable item = (Taggable)v.getTag();
            if (item != null) {
                showDetailviewForItem(item);
            }
            else {
                Log.e(logger,"no item has been set on view: " + v + ". cannot handle click event...");
            }
        }

    }


}
