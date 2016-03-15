package org.dieschnittstelle.mobile.android.apps.contenttagger;

import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Note;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.components.model.impl.EntityManager;
import org.dieschnittstelle.mobile.android.components.model.impl.LocalEntityCRUDOperationsImpl;

/**
 * Created by master on 14.03.16.
 */
// a local application class that instantiates the entity manager
public class ContentTaggerApplication extends com.orm.SugarApp {

    protected static String logger = "ContentTaggerApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Tag.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Tag.class, true);
        EntityManager.getInstance().addEntityCRUDOperationsImpl(Note.class, EntityManager.CRUDOperationsScope.LOCAL, new LocalEntityCRUDOperationsImpl());
        EntityManager.getInstance().setEntityCRUDAsync(Note.class, true);
    }
}
