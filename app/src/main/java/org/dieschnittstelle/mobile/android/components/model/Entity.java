package org.dieschnittstelle.mobile.android.components.model;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.orm.dsl.Ignore;

import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.model.impl.EntityManager;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Created by master on 10.03.16.
 */
public abstract class Entity {

    public static Comparator<Entity> COMPARE_BY_ID = new Comparator<Entity>() {
        @Override
        public int compare(Entity lhs, Entity rhs) {
            return (lhs.getId() < rhs.getId() ? -1 : (lhs.getId() == rhs.getId()) ? 0 : 1);
        }
    };

    /*
     * constants for representing entity associations as string
     */
    protected static final String FIELD_SEPARATOR = ";";
    protected static final String FIELDVALUE_SEPARATOR = "=";
    protected static final String ENTITY_SEPARATOR = ",";
    protected static final String ID_SEPARATOR = "@";


    protected static String logger = "Entity";

    // obtain a reference to the event dispatcher
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

    // collect entities that are pending to be updated once this entity is being created or updated
    @Ignore
    private Set<Entity> pendingUpdates = new HashSet<Entity>();

    // sugar orm requires Long rather than long - this way one can distinguish between no id and the value 0
    // unfortunately, sugar does not consider properties of superclasses, i.e. we need to add the ids in subclasses
//    private Long id;
//
//    public Long getId() {
//        return this.id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }

    public abstract void setId(Long id);

    public abstract Long getId();

    public abstract void setAssociations(String assoc);

    public abstract String getAssociations();

    public void addPendingUpdate(Entity e) {
        if (!this.pendingUpdates.contains(e)) {
            this.pendingUpdates.add(e);
        }
    }

    public Entity() {

    }

    public void create() {
        create(null);
    }

    public void update() {
        update(null);
    }

    public void delete() {
        delete(null);
    }

    public static Entity read(Class<? extends Entity> entityClass,long id) {
        return read(entityClass, id, null);
    }

    public static List<? extends Entity> readAll(Class<? extends Entity> entityClass) {
        return readAll(entityClass, null);
    }

    /*
     * enforce snychronised access even though the global setting might be asynchronous...
     */
    public static Entity readSync(Class<? extends Entity> entityClass,long id) {
        return EntityManager.getInstance().readSync(entityClass, id);
    }

    public static List<? extends Entity> readAllSync(Class<? extends Entity> entityClass) {
        return EntityManager.getInstance().readAllSync(entityClass);
    }

    public  void createSync() {
        EntityManager.getInstance().createSync(this.getClass(), this);
    }

    public void updateSync() {
        EntityManager.getInstance().updateSync(this.getClass(), this);
    }

    public boolean deleteSync() {
        return EntityManager.getInstance().deleteSync(this.getClass(), this);
    }

    /*
     * methods with argument for the caller context
     */

    public void create(final EventGenerator context) {
        // creation needs to be done in three steps:
        // 1) create the entity
        // 2) handle pending updates (which might result in the creation of associated entities)
        // 3) update the entity

        // 1)
        EntityManager.getInstance().create(Entity.this.getClass(), Entity.this, context, new EntityManager.CRUDCallback() {
            @Override
            public void onCRUDCompleted() {
                // 2)
                Log.i(logger,"create(): creation done. Now handling pending updates...");
                handlePendingUpdates(context, new EntityManager.CRUDCallback() {
                    @Override
                    public void onCRUDCompleted() {
                        // 3)
                        Log.i(logger,"create(): pending updates dones. Now updating myself: " + Entity.this);
                        Entity.this.update(context);
                    }
                });
            }
        });
        // here, we need to pass a callback in order to avoid that pending updates are created before an id has been set
    }



    public void update(final EventGenerator context) {
        handlePendingUpdates(context, new EntityManager.CRUDCallback() {
            @Override
            public void onCRUDCompleted() {
                Log.i(logger,"update(): pending updates dones. Now updating myself: " + Entity.this);
                EntityManager.getInstance().update(Entity.this.getClass(), Entity.this, context);
            }
        });
    }

//    private void handlePendingUpdates(EventGenerator context) {
//        if (this.pendingUpdates.size() > 0) {
//        Log.i(logger,"handlePendingUpdates(): handling " + this.pendingUpdates.size() + " pending updates for entity: " + this);
//            for (Entity e : this.pendingUpdates) {
//                if (!e.created()) {
//                    Log.i(logger,"entity contained in pendingUpdates has not been created yet: " + e);
//                }
//                e.update(context);
//            }
//            this.pendingUpdates.clear();
//        }
//        else {
//            Log.d(logger,"handlePendingUpdates(): no pending updates exist for entity: " + this);
//        }
//    }

    private void handlePendingUpdates(final EventGenerator context, final EntityManager.CRUDCallback callback) {
        Log.i(logger,"handlePendingUpdates()");
        if (this.pendingUpdates.size() > 0) {
            // we run this in an asyncTask to avoid synchronisation issues if pending updates relate to entities that have not been created so far
            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... voids) {
                    Log.i(logger,"handlePendingUpdates(): handling " + Entity.this.pendingUpdates.size() + " pending updates for entity: " + this);
                    for (Entity e : Entity.this.pendingUpdates) {
                        // TODO: note that we basically support creation of associated entities, but not recursively... and also note that createSync() will not trigger event handlers!
                        if (!e.created()) {
                            Log.i(logger,"entity contained in pendingUpdates has not been created yet: " + e);
                            e.createSync();
                        }
                        else {
                            e.update(context);
                        }
                    }
                    Entity.this.pendingUpdates.clear();
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean aBoolean) {
                    callback.onCRUDCompleted();
                }
            }.execute();
        }
        else {
            Log.d(logger,"handlePendingUpdates(): no pending updates exist for entity: " + this);
            callback.onCRUDCompleted();
        }
    }

    /*
     * delete should also be handled automatically!
     */
    public void delete(EventGenerator context) {
        preDestroy();
        EntityManager.getInstance().delete(this.getClass(), this, context);
        handlePendingUpdates(context, new EntityManager.CRUDCallback() {
            @Override
            public void onCRUDCompleted() {
                Log.i(logger,"delete(): done for " + Entity.this);
            }
        });
    }

    public static Entity read(Class<? extends Entity> entityClass,long id, EventGenerator context) {
        return EntityManager.getInstance().read(entityClass, id, context);
    }

    public static List<? extends Entity> readAll(Class<? extends Entity> entityClass, EventGenerator context) {
        return EntityManager.getInstance().readAll(entityClass, context);
    }

    public boolean created() {
        return getId() != null;
    }

    // we need to process fields that have one2many or many2many relations to other entities as they might not be handled by the underlying local orm solution (e.g. sugar orm)
    public void prePersist() {
        Log.d(logger,"prePersist(): " + this);

        StringBuffer entityAssociations = new StringBuffer();

        // we collect the local and the inherited public fields
        Set<Field> fields = new HashSet<Field>();
        for (Field field : this.getClass().getDeclaredFields()) {
            fields.add(field);
        }
        for (Field field : this.getClass().getFields()) {
            fields.add(field);
        }

        // we iterate over the fields and check for Collection-Type attributes
        for (Field field : fields) {
            if (Collection.class.isAssignableFrom(field.getType()) /*&& field.getGenericType() != null*/) {
                Log.d(logger, "prePersist(): found collection typed field with generic collection type: " + field.getName());
                Type[] typeParameters = ((ParameterizedType)field.getGenericType()).getActualTypeArguments();
                if (typeParameters.length > 0 && Entity.class.isAssignableFrom((Class<?>)typeParameters[0])) {
                    try {
                        field.setAccessible(true);
                        String assocs = prePersistEntityAssociation((Collection<Entity>) field.get(this));
                        if (assocs.length() > 0) {
                            entityAssociations.append(field.getName());
                            entityAssociations.append(FIELDVALUE_SEPARATOR);
                            field.setAccessible(true);
                            Log.d(logger, "prePersist(): field contains entity values.");
                            entityAssociations.append(assocs);
                            entityAssociations.append(FIELD_SEPARATOR);
                        }
                    }
                    catch (Exception e) {
                        Log.e(logger,"prePersist(): cannot process collection field " + field.getName() + ". Got exception: " + e,e);
                    }
                }
            }
        }

        this.setAssociations(entityAssociations.toString());

        Log.d(logger,"prePersist(): created entityAssociations: " + entityAssociations);
    }

    public void postLoad() {
        // check whether the entity has persisted associations
        String entityAssociations = getAssociations();
        Log.d(logger,"postLoad():entity associations for entity of type " + this.getClass() + ": " + entityAssociations);
        // parse associations
        if (entityAssociations != null && !"".equals(entityAssociations)) {
            Log.d(logger,"postLoad(): resolving entity associations: " + entityAssociations);
            // we first separate the fields
            StringTokenizer tok = new StringTokenizer(entityAssociations,FIELD_SEPARATOR);
            while (tok.hasMoreTokens()) {
                String currentField = tok.nextToken();
                if (!"".equals(currentField.trim())) {
                    // separate in name and value
                    int index = currentField.indexOf(FIELDVALUE_SEPARATOR);
                    if (index > -1) {
                        String currentFieldName = currentField.substring(0,index);
                        String currentFieldValues = currentField.substring(index+FIELDVALUE_SEPARATOR.length());
                        // then we separate the field values
                        StringTokenizer valtok = new StringTokenizer(currentFieldValues,ENTITY_SEPARATOR);
                        List<Entity> currentEntities = new ArrayList<Entity>();
                        while (valtok.hasMoreTokens()) {
                            String currentEntityRef = valtok.nextToken();
                            if (!"".equals(currentEntityRef.trim())) {
                                Log.d(logger,"postLoad(): resolving entity reference in value for field " + currentFieldName + ": " + currentEntityRef);
                                int idindex = currentEntityRef.indexOf(ID_SEPARATOR);
                                Log.d(logger,"postLoad(): idindex: " + idindex);
                                String currentEntityClassname = currentEntityRef.substring(0, idindex);
                                Log.d(logger,"postLoad(): entityClassname: " + currentEntityClassname);
                                String currentEntityId = currentEntityRef.substring(idindex+ID_SEPARATOR.length());
                                Log.d(logger,"postLoad(): entityEntityId: " + currentEntityId);
                                try {
                                    currentEntities.add(this.readSync((Class<? extends Entity>)Class.forName(currentEntityClassname), Long.parseLong(currentEntityId)));
                                } catch (Exception e) {
                                    Log.e(logger, "postLoad(): got exception trying to read entity given reference " + currentEntityRef + ": " + e, e);
                                }
                            }
                        }
                        Field currentFieldObj = null;

                        try {
                            // now we need to set the field value - we use getDeclaredField() in order to access the private field of the concrete entity classes - however, inheritance will not work here!
                            currentFieldObj = this.getClass().getDeclaredField(currentFieldName);
                        }
                        // lookup the field in the parent classes - then it must be public, though...
                        // TODO: make this more sophisticated at some moment and consider getters of superclasses rather than requiring public fields
                        catch (NoSuchFieldException e1) {
                            try {
                                currentFieldObj = this.getClass().getField(currentFieldName);
                            }
                            catch (NoSuchFieldException e2) {

                            }
                        }
                        if (currentFieldObj != null) {
                            currentFieldObj.setAccessible(true);
                            try {
                                currentFieldObj.set(this, currentEntities);
                            } catch (Exception e) {
                                Log.e(logger, "postLoad(): got exception trying to set reference attribute " + currentFieldName + " to values: " + currentEntities + ". Exception is: " + e, e);
                            }
                        }
                        else {
                            Log.e(logger, "postLoad(): could not access field: " + currentFieldName + ". Fields either need to be declared on the concrete entity classes or as public fields in superclasses.");
                        }
                    }
                }
            }
            // we reset the entityAssociations in case postLoad is run more than once...
            this.setAssociations("");
            Log.d(logger, "postLoad(): done");
        }
        else {
            //Log.d(logger,"postLoad(): no entity associations specified.");
        }

    }

    // given metainformation about associations this could be done automatically, but allow to manually handle the case where associations need to be updated because some entity is being deleted
    public void preDestroy() {
        Log.i(logger,"preDestroy(): override for bidirectional associations!");
    }

    private String prePersistEntityAssociation(Collection<Entity> entities) {
        StringBuffer buf = new StringBuffer();
        // we create a comma-separated list of classname followed by id
        for (Entity e : entities) {
            buf.append(e.getClass().getName() + ID_SEPARATOR + e.getId());
            buf.append(ENTITY_SEPARATOR);
        }
        return buf.toString();
    }

}
