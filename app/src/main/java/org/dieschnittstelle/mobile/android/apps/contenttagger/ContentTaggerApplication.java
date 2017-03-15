package org.dieschnittstelle.mobile.android.apps.contenttagger;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.dieschnittstelle.mobile.android.apps.contenttagger.controller.LinksEditviewFragment;
import org.dieschnittstelle.mobile.android.apps.contenttagger.controller.MediaEditviewFragment;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Link;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Media;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Place;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.controller.SendActionDispatcher;
import org.dieschnittstelle.mobile.android.components.controller.SendActionDispatcherPresenter;
import org.dieschnittstelle.mobile.android.components.model.impl.EntityManager;
import org.dieschnittstelle.mobile.android.components.model.impl.LocalEntityCRUDOperationsImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by master on 14.03.16.
 *
 * TODO: for database import see: http://www.javahelps.com/2015/04/import-and-use-external-database-in.html?m=1
 */
// a local application class that instantiates the entity manager
public class ContentTaggerApplication extends com.orm.SugarApp implements SendActionDispatcher {

    private static final String DBFILENAME = "contenttagger.db";
    protected static String logger = "ContentTaggerApplication";

//    private GoogleApiClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(logger, "in case application launch fails with a no-such-table error, deinstall and deactivate 'instant run' option");

        Log.i(logger, "database path is: " + getDatabasePath("contenttagger.db"));

        File file = getDatabasePath("contenttagger.db");
        Log.i(logger,"database file exists: " + file.exists());

        EntityManager.getInstance().addEntityCRUDOperationsImpl(Tag.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Tag.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Note.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Note.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Link.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Link.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Media.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Media.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Place.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Place.class, true);

        // TODO: skip this for the time being
        // we deal with accessing location here, rather than inside of the particular activities
        // Create an instance of GoogleAPIClient.
//        if (mGoogleApiClient == null) {
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(LocationServices.API)
//                    .build();
//        }
    }

    @Override
    public void handleSendActionForType(String type, Intent intent,SendActionDispatcherPresenter presenter) {

        Log.i(logger,"handleSendActionForType(): intent: " + intent);

        switch(type) {
            case "text/plain":
                String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                // check whether the string starts with http:// or https://
                if (text != null && URLUtil.isValidUrl(text)) {
                    handleSendActionForLink(text,presenter);
                }
                else {
                    Toast.makeText(this,"Content type " + type + " for send action is not supported for arbitrary texts. Got: " + text,Toast.LENGTH_LONG).show();
                }
                break;
            case "image/jpeg":
                handleSendActionForImage(intent,presenter);
                break;
            default:
                Toast.makeText(this,"Content type " + type + " for send action is not supported.",Toast.LENGTH_LONG).show();
                break;
        }

    }

    private void handleSendActionForLink(String url,SendActionDispatcherPresenter presenter) {
        Bundle args = MainNavigationControllerActivity.createArguments(LinksEditviewFragment.ARG_LINK_URL, url);
        args.putBoolean(LinksEditviewFragment.ARG_CALLED_FROM_SEND, true);
        presenter.showView(LinksEditviewFragment.class, args, false);
    }

    private void handleSendActionForImage(Intent intent,SendActionDispatcherPresenter presenter) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
            Log.i(logger,"received imageUri: " + imageUri);
            Bundle args = MainNavigationControllerActivity.createArguments(MediaEditviewFragment.ARG_MEDIA_CONTENT_URI, imageUri.toString());
            args.putBoolean(LinksEditviewFragment.ARG_CALLED_FROM_SEND, true);
            presenter.showView(MediaEditviewFragment.class, args, false);
        }
        else {
            String err = "no image uri has been received!";
            Log.e(logger,"handleSendActionForImage(): " + err);
            Toast.makeText(this,err,Toast.LENGTH_LONG).show();
        }
    }

}
