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

/**
 * Created by master on 17.03.16.
 */
public class TagsbarController {

    protected static String logger = "TagsbarController";

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
    }

    // we take a taggable and add its tags
    public void bindTaggable(Taggable taggable) {
        Log.i(logger,"bindTaggable(): " + taggable);
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
            }
        });

        return itemView;
    }

}
