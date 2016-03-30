package org.dieschnittstelle.mobile.android.apps.contenttagger;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.dieschnittstelle.mobile.android.apps.contenttagger.controller.LinksEditviewFragment;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Link;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.components.controller.MainNavigationControllerActivity;
import org.dieschnittstelle.mobile.android.components.controller.SendActionDispatcher;
import org.dieschnittstelle.mobile.android.components.controller.SendActionDispatcherPresenter;
import org.dieschnittstelle.mobile.android.components.model.impl.EntityManager;
import org.dieschnittstelle.mobile.android.components.model.impl.LocalEntityCRUDOperationsImpl;

/**
 * Created by master on 14.03.16.
 */
// a local application class that instantiates the entity manager
public class ContentTaggerApplication extends com.orm.SugarApp implements SendActionDispatcher {

    protected static String logger = "ContentTaggerApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Tag.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Tag.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Note.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Note.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Link.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Link.class, true);
    }

    @Override
    public void handleSendActionForType(String type, Intent intent,SendActionDispatcherPresenter presenter) {

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

}
