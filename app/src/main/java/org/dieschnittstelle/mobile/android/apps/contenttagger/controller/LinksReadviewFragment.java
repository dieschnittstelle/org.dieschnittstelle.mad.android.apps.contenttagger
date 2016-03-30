package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Link;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.events.EventListener;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;
import org.dieschnittstelle.mobile.android.components.events.EventMatcher;

/**
 * Created by master on 17.03.16.
 *
 * in constrast to handling Notes, this is not realised as a subclass of the editview
 */
public class LinksReadviewFragment extends Fragment implements EventGenerator, EventListenerOwner {

    protected static String logger = "LinksReadviewFragment";

    protected TagsbarController tagsbarController;

    /*
     * the model object that we use
     */
    protected Link link;

    /*
     * the web view
     */
    protected WebView webview;

    /*
     * we need to handle obsoletion of readview (which is a subclass) here...
     */
    protected boolean obsolete;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addEventListeners();

        setHasOptionsMenu(true);
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View contentView = inflater.inflate(R.layout.links_readview,container,false);

        this.webview = (WebView)contentView.findViewById(R.id.webview);
        prepareWebview();

        // we need to set padding manually on tagsbar as the content view does not use padding
        ViewGroup tagsbar = (ViewGroup)contentView.findViewById(R.id.tagsbar);
        // we want to set dps, therefore we need to scale the value, see http://stackoverflow.com/questions/9685658/add-padding-on-view-programmatically
        float scale = getResources().getDisplayMetrics().density;
        tagsbar.setPadding((int) (scale * 10 + 0.5f), (int) (scale * 10 + 0.5f), (int) (scale * 10 + 0.5f), (int) (scale * 10 + 0.5f));

        this.tagsbarController = new TagsbarController(this,tagsbar,R.layout.tagsbar_itemview);
        this.tagsbarController.setRemoveActive(false);

        return contentView;
    }



    protected void addEventListeners() {
        // set listeners
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.DELETED), Link.class), false, new EventListener<Link>() {
            @Override
            public void onEvent(Event<Link> event) {
                if (link == event.getData()) {
                    // in case the note is deleted, we mark this as obsolete
                  Log.i(logger, "link has been deleted. mark myself as obsolete...");
                    obsolete = true;
                } else {
                    Log.i(logger, "onEvent(): got " + event.getType() + " event for link, but it involves a different object than the one being edited: " + event.getData() + ". Ignore...");
                }
            }
        });

        // also handle read events that are generated by ourselves
        EventDispatcher.getInstance().addEventListener(this, new EventMatcher(Event.CRUD.TYPE, Event.OR(Event.CRUD.READ, Event.CRUD.UPDATED), Link.class, this), true, new EventListener<Link>() {
            @Override
            public void onEvent(Event<Link> event) {
                link = event.getData();
                // display the title in the title bar
                ((ActionBarActivity) getActivity()).setTitle(link.getTitle());
                // show the content in the webview
                webview.loadUrl(link.getUrl());
                // create the tagbar controller, passing the link object
                tagsbarController.bindTaggable(link);
            }
        });

    }

    /*
     * see http://stackoverflow.com/questions/10097233/optimal-webview-settings-for-html5-support
     */
    private void prepareWebview() {
       webview.setFocusable(true);
       webview.setFocusableInTouchMode(true);
       webview.getSettings().setJavaScriptEnabled(true);
       webview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
       webview.getSettings().setDomStorageEnabled(true);
       webview.getSettings().setDatabaseEnabled(true);
       webview.getSettings().setAppCacheEnabled(true);
       webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
       webview.setWebViewClient(new WebViewClient());
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity)getActivity()).setTitle(R.string.title_read_link);
        if (obsolete) {
            Log.i(logger,"view is obsolete. Pop from backstack...");
            getFragmentManager().popBackStack();
        }

        long linkId = getArguments().getLong(LinksEditviewFragment.ARG_LINK_ID);
        if (linkId > -1) {
            Log.d(logger,"onResume(): we have been passed a non empty linkId: " + linkId);
            ((ActionBarActivity) getActivity()).setTitle(R.string.title_edit_link);
            // read all notes - reaction will be dealt with by event handler
            Link.read(Link.class, linkId, this);
        } else {
          Log.e(logger,"onResume(): no linkId seems to have been passed!");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            ((MainNavigationControllerActivity) getActivity()).showView(LinksEditviewFragment.class, MainNavigationControllerActivity.createArguments(LinksEditviewFragment.ARG_LINK_ID, link.getId()), true);
            return true;
        }
        else if (item.getItemId() == R.id.action_open) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link.getUrl())));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventDispatcher.getInstance().unbindController(this);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // we do NOT invoke super as this will the super actions...
        //super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_links_readview, menu);
    }

}
