package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;

/**
 * Created by master on 17.03.16.
 */
public class TagsbarController {

    protected static String logger = "TagsbarController";

    // two custom events specifying that a tag has been been added/removed to/from some taggable
    public static String EVENT_TAG_ADDED = "tagAdded";
    public static String EVENT_TAG_REMOVED = "tagRemoved";

    // the flow layout, abstracted as ViewGroup, that contains the tags
    private ViewGroup tagsbarLayout;

    // the layout for the itemView
    private int tagItemViewLayoutId;

    // the parent controller (we assume this is a fragment)
    private Fragment parentController;

    // the taggable whose tags we show
    private Taggable taggable;

    // whether we allow to remove tags

    public TagsbarController(Fragment parentController,ViewGroup tagsbarLayout,int tagItemViewLayoutId) {
        this.tagsbarLayout = tagsbarLayout;
        this.tagItemViewLayoutId = tagItemViewLayoutId;
        this.parentController = parentController;

        // we set listeners that react to adding / removing tags
        EventDispatcher.getInstance().addEventListener((EventListenerOwner)this.parentController,new EventMatcher<TaggableTagBundle>(Event.UI.TYPE,EVENT_TAG_REMOVED,TaggableTagBundle.class),true,new EventListener<TaggableTagBundle>(){
            @Override
            public void onEvent(Event<TaggableTagBundle> event) {
                if (event.getData().getTaggable() == taggable) {
                    taggable.removeTag(event.getData().getTag());
                    removeTagItemView(event.getData().getTag());
                }
                else {
                    Log.i(logger,"got " + EVENT_TAG_REMOVED + " event, but it seems to be related to a different taggable than the one we have");
                }
            }
        });
    }

    private void removeTagItemView(Tag tag) {
        for (int i=0;i < this.tagsbarLayout.getChildCount();i++) {
            if (this.tagsbarLayout.getChildAt(i).getTag() == tag) {
                this.tagsbarLayout.removeViewAt(i);
                return;
            }
        }
        Log.w(logger,"removeTagItemView(): no view seems to exist for tag: " + tag);
    }

    // we take a taggable and add its tags
    public void bindTaggable(Taggable taggable) {
        Log.i(logger,"bindTaggable(): " + taggable);
        this.taggable = taggable;
        this.tagsbarLayout.removeAllViews();
        for (Tag tag : taggable.getTags()) {
            this.tagsbarLayout.addView(createTagItemView(tag));
        }
    }

    private View createTagItemView(final Tag tag) {
        View itemView = parentController.getActivity().getLayoutInflater().inflate(tagItemViewLayoutId,null);
        ((TextView)itemView.findViewById(R.id.tagname)).setText(tag.getName());

        // we set an onclick listener on the itemView
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(logger, "onClick(): tag is: " + tag);
                ((MainNavigationControllerActivity)parentController.getActivity()).showView(TaggableOverviewFragment.class, MainNavigationControllerActivity.createArguments(TaggableOverviewFragment.ARG_TAG_ID, tag.getId()), true);
            }
        });

        // we set a listener on the element for removing a tag
        itemView.findViewById(R.id.action_remove_tag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventDispatcher.getInstance().notifyListeners(new Event(Event.UI.TYPE,EVENT_TAG_REMOVED,TaggableTagBundle.class,new TaggableTagBundle(tag,taggable)));
            }
        });
        // :)
        itemView.setTag(tag);

        return itemView;
    }

    public static class TaggableTagBundle {

        private Tag tag;
        private Taggable taggable;

        public TaggableTagBundle(Tag tag, Taggable taggable) {
            this.tag = tag;
            this.taggable = taggable;
        }

        public Tag getTag() {
            return tag;
        }

        public Taggable getTaggable() {
            return taggable;
        }

    }

}
