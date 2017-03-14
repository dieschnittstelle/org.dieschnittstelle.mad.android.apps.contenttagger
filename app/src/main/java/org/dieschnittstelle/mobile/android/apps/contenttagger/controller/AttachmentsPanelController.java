package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.animation.GridLayoutAnimationController;
import android.widget.GridView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Taggable;

/**
 * Created by master on 14.03.17.
 *
 * encapsulates control of an attachments panel which allows to associate taggables with taggables, starting with media
 * for using gridview see https://developer.android.com/guide/topics/ui/layout/gridview.html
 */
public class AttachmentsPanelController {

    protected static String logger = "AttachmentsPanelController";

    private Taggable taggable;
    private GridView mediaGrid;
    private FloatingActionButton addMediaAction;

    public AttachmentsPanelController(View parentView) {

        this.mediaGrid = (GridView)parentView.findViewById(R.id.mediaGrid);
        this.addMediaAction = (FloatingActionButton) parentView.findViewById(R.id.addMedia);

        // add a click listener on the action
        addMediaAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMedia();
            }
        });

    }

    private void addMedia() {
        Log.i(logger,"addMedia()");
    }

    public void bindTaggable(Taggable taggable) {
        this.taggable = taggable;
    }
}
