package org.dieschnittstelle.mobile.android.components.model.impl;

import android.content.Context;
import android.util.Log;

import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.model.EntityCRUDOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by master on 14.03.16.
 * <p/>
 * TODO: locally manage instances without db access for read operations after initial read!
 */
public class EntityManager implements EntityCRUDOperations {

    protected static String logger = "EnityManager";

    private static EntityManager instance;

    public static EntityManager getInstance() {
        if (instance == null) {
            instance = new EntityManager();
        }
        return instance;
    }


    public static enum CRUDOperationsScope {
        LOCAL, REMOTE, SYNCED;
    }

    private EntityManager() {

    }

    /*
     * use the event dispatcher
     */
    private static EventDispatcher dispatcher = EventDispatcher.getInstance();

    /*
     * the current scope
     */
    private CRUDOperationsScope currentOperationsScope = CRUDOperationsScope.LOCAL;

    /*
     * a mapping of classes to crud operations implementations
     */
    private Map<Class<? extends Entity>, Map<CRUDOperationsScope, EntityCRUDOperations>> entityCRUDOperations = new HashMap<Class<? extends Entity>, Map<CRUDOperationsScope, EntityCRUDOperations>>();

    /*
     * a mapping of classes to entity instances
     */
    private Map<Class<? extends Entity>, Map<Long, Entity>> entityInstances = new HashMap<Class<? extends Entity>, Map<Long, Entity>>();

    /*
     * a mapping of classes to information on whether all instances have already read out or not
     */
    private Map<Class<? extends Entity>, Boolean> entityInstancesSynced = new HashMap<Class<? extends Entity>, Boolean>();

    /*
     * and yet another mapping of classes to setting whether the crudps are run asynchronously or not
     */
    private Map<Class<? extends Entity>, Boolean> entityCRUDAsync = new HashMap<Class<? extends Entity>, Boolean>();

    @Override
    public void create(Class<? extends Entity> entityClass, Entity e) {
        this.create(entityClass, e, null);
    }

    @Override
    public void update(Class<? extends Entity> entityClass, Entity e) {
        this.update(entityClass, e, null);
    }

    @Override
    public boolean delete(Class<? extends Entity> entityClass, Entity e) {
        return this.delete(entityClass, e, null);
    }

    @Override
    public Entity read(Class<? extends Entity> entityClass, long id) {
        return this.read(entityClass, id, null);
    }

    @Override
    public List<Entity> readAll(Class<? extends Entity> entityClass) {
        return this.readAll(entityClass, null);
    }

    /*
     * implement methods passing a caller context
     */
    public void create(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context) {
        create(entityClass, e, context, null);
    }

    public void create(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context, final CRUDCallback callback) {
        Log.d(logger, "create()");
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    createSync(entityClass, e);
                    if (callback != null) {
                        callback.onCRUDCompleted();
                    }
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.CREATED, entityClass, context, e));
                }
            }).start();
        } else {
            createSync(entityClass, e);
        }
    }

    public void createSync(Class<? extends Entity> entityClass, Entity e) {
        Log.d(logger, "createSync()");
        e.prePersist();
        entityCRUDOperations.get(entityClass).get(currentOperationsScope).create(entityClass, e);
        addLocalEntity(entityClass, e);
    }

    public void update(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context) {
        update(entityClass, e, context, null);
    }

    public void update(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context, final CRUDCallback callback) {
        Log.d(logger, "update()");
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    updateSync(entityClass, e);
                    if (callback != null) {
                        callback.onCRUDCompleted();
                    }
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.UPDATED, entityClass, context, e));
                }
            }).start();
        } else {
            updateSync(entityClass, e);
        }
    }

    public void updateSync(Class<? extends Entity> entityClass, Entity e) {
        Log.d(logger, "updateSync()");
        e.prePersist();
        entityCRUDOperations.get(entityClass).get(currentOperationsScope).update(entityClass, e);
        updateLocalEntity(entityClass, e);
    }

    public boolean delete(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context) {
        return delete(entityClass, e, context, null);
    }

    public boolean delete(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context, final CRUDCallback callback) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (deleteSync(entityClass, e)) {
                        if (callback != null) {
                            callback.onCRUDCompleted();
                        }
                        dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.DELETED, entityClass, context, e));
                    }
                }
            }).start();
            return true;
        } else {
            if (deleteSync(entityClass, e)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean deleteSync(final Class<? extends Entity> entityClass, final Entity e) {
        if (entityCRUDOperations.get(entityClass).get(currentOperationsScope).delete(entityClass, e)) {
            deleteLocalEntity(entityClass, e);
            return true;
        } else {
            return false;
        }
    }

    public Entity read(final Class<? extends Entity> entityClass, final long id, final EventGenerator context) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Entity e = readSync(entityClass, id);
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.READ, entityClass, context, e));
                }
            }).start();
            return null;
        } else {
            return readSync(entityClass, id);
        }
    }

    public Entity readSync(final Class<? extends Entity> entityClass, final long id) {
        // first try to read locally
        Entity e = readLocalEntity(entityClass, id);
        if (e == null) {
            Log.d(logger, "readSync(): local instance of entity class " + entityClass + " for id: " + id + " does not exist yet. Read from datasource...");
            e = entityCRUDOperations.get(entityClass).get(currentOperationsScope).read(entityClass, id);
            // we invoke post load which might result in loading associated entities!
            if (e != null) {
                addLocalEntity(entityClass, e);
                e.postLoad();
            }
        } else {
            Log.d(logger, "readSync(): read local instance of entity class " + entityClass + " for id: " + id);
        }
        return e;
    }


    public List<Entity> readAll(final Class<? extends Entity> entityClass, final EventGenerator context) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Entity> es = readAllSync(entityClass);
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.READALL, entityClass, context, es));
                }
            }).start();
            return null;
        } else {
            return readAllSync(entityClass);
        }
    }

    public List<Entity> readAllSync(final Class<? extends Entity> entityClass) {
        List<Entity> es = readAllLocalEntities(entityClass);
        if (es == null) {
            Log.d(logger, "readAllSync(): local instances of entity class " + entityClass + " have not yet been synchronised. Read from datasource...");
            es = entityCRUDOperations.get(entityClass).get(currentOperationsScope).readAll(entityClass);
            addLocalEntities(entityClass, es);
            // we need to invoke postLoad on the entities (we invoke it on all instances, potentially duplicated loading of associations will be prevented internally)
            Log.d(logger,"readAllSync(): run postLoad() on " + es.size() + " entities of class " + entityClass);
            for (Entity e : es) {
                e.postLoad();
            }
        } else {
            Log.d(logger, "readAllSync(): read " + es.size() + " local instances of entity class " + entityClass);
        }
        return es;
    }

    /*
     * managing the local entities
     */
    private void addLocalEntity(Class<? extends Entity> entityClass, Entity entity) {
        prepareLocalEntities(entityClass);
        if (entity != null) {
            Entity existingEntity = this.entityInstances.get(entityClass).get(entity.getId());
            if (existingEntity != null) {
                if (existingEntity == entity) {
                    Log.i(logger, "addLocalEntity(): entity of class " + entityClass + " and id " + entity.getId() + " already exists, and it IS  identical to the one to be added. This should not cause referential issues");
                } else {
                    Log.w(logger, "addLocalEntity(): entity of class " + entityClass + " and id " + entity.getId() + " already exists, but it is NOT identical to the one to be added. This might cause referential issues");
                }
            }
            this.entityInstances.get(entityClass).put(entity.getId(), entity);
        }
    }

    private void addLocalEntities(Class<? extends Entity> entityClass, List<Entity> entities) {
        prepareLocalEntities(entityClass);
        // there might exists instances in case we have loaded entities referred by some other entities without reading all instances of the given class!
        if (this.entityInstances.get(entityClass).size() > 0) {
            Log.i(logger, "addLocalEntities(): there already exist local instances of entity class " + entityClass + ". Will only add instances for ids which have not been loaded yet...");
        }
        for (Entity e : entities) {
            if (this.entityInstances.get(entityClass).containsKey(e.getId())) {
                Log.d(logger, "addLocalEntities(): entity of class " + entityClass + " with id " + e.getId() + " has already been loaded");
            } else {
                Log.d(logger, "addLocalEntities(): add entity of class " + entityClass + " with id " + e.getId());
                this.entityInstances.get(entityClass).put(e.getId(), e);
            }
        }
        this.entityInstancesSynced.put(entityClass, true);
    }

    private void updateLocalEntity(Class<? extends Entity> entityClass, Entity entity) {
        prepareLocalEntities(entityClass);
        // the entity to be updated should exist!
        Entity existingEntity = this.entityInstances.get(entityClass).get(entity.getId());
        if (existingEntity != null) {
            if (existingEntity == entity) {
                // this is the normal case, i.e. the entity in the map and the updated entity are identical
            } else {
                Log.w(logger, "updateLocalEntity(): entity of class " + entityClass + " and id " + entity.getId() + " already exists, but it is NOT identical to the one to be updated. It will be overriden by the update, which might cause referential problems");
                this.entityInstances.get(entityClass).put(entity.getId(), entity);
            }
        }

    }

    private void deleteLocalEntity(Class<? extends Entity> entityClass, Entity entity) {
        prepareLocalEntities(entityClass);
        // we just remove without checking
        this.entityInstances.get(entityClass).remove(entity.getId());
    }

    private Entity readLocalEntity(Class<? extends Entity> entityClass, Long id) {
        prepareLocalEntities(entityClass);
        return this.entityInstances.get(entityClass).get(id);
    }

    private List<Entity> readAllLocalEntities(Class<? extends Entity> entityClass) {
        prepareLocalEntities(entityClass);
        // if syncing has not been done, we return null!
        if (!this.entityInstancesSynced.containsKey(entityClass) || !this.entityInstancesSynced.get(entityClass)) {
            return null;
        }
        return new ArrayList<Entity>(this.entityInstances.get(entityClass).values());
    }

    private void prepareLocalEntities(Class<? extends Entity> entityClass) {
        if (!this.entityInstances.containsKey(entityClass)) {
            this.entityInstances.put(entityClass, new HashMap<Long, Entity>());
        }
    }

    /*
     * managing the crud implementations
     */

    public void addEntityCRUDOperationsImpl(Class<? extends Entity> entityClass, CRUDOperationsScope scope, EntityCRUDOperations impl) {
        // check whether we alreary have an operations map
        Map<CRUDOperationsScope, EntityCRUDOperations> crudops = entityCRUDOperations.get(entityClass);
        if (crudops == null) {
            crudops = new HashMap<CRUDOperationsScope, EntityCRUDOperations>();
            entityCRUDOperations.put(entityClass, crudops);
        }
        crudops.put(scope, impl);
    }

    public void setEntityCRUDAsync(Class<? extends Entity> entityClass, boolean asyncCRUD) {
        entityCRUDAsync.put(entityClass, asyncCRUD);
    }

    private boolean runEntityCRUDAsync(Class<? extends Entity> entityClass) {
        return entityCRUDAsync.containsKey(entityClass) && entityCRUDAsync.get(entityClass);
    }

    /*
     * a crud callback interface for directly receiving notifications on completed crud operations
     */
    public static interface CRUDCallback {

        public void onCRUDCompleted();

    }

}
