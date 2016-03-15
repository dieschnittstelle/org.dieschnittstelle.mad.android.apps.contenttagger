package org.dieschnittstelle.mobile.android.components.model.impl;

import android.content.Context;

import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.model.EntityCRUDOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by master on 14.03.16.
 * <p/>
 * TODO: locally manage instances without db access for read operations after initial read!
 */
public class EntityManager implements EntityCRUDOperations {

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
    private Map<Class<Entity>, List<Entity>> entityInstances = new HashMap<Class<Entity>, List<Entity>>();

    /*
     * a mapping of classes to setting whether the crudps are run asynchronously or not
     */
    private Map<Class<? extends Entity>, Boolean> entityCRUDAsync = new HashMap<Class<? extends Entity>, Boolean>();

    @Override
    public Entity create(Class<? extends Entity> entityClass, Entity e) {
        return this.create(entityClass, e, null);
    }

    @Override
    public Entity update(Class<? extends Entity> entityClass, Entity e) {
        return this.update(entityClass, e, null);
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
     * implement methods passing a caller contetx
     */
    public Entity create(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context) {

        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    entityCRUDOperations.get(entityClass).get(currentOperationsScope).create(entityClass, e);
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.CREATED, entityClass, context, e));
                }
            }).start();
            return null;
        } else {
            return createSync(entityClass, e);
        }
    }

    public Entity createSync(Class<? extends Entity> entityClass, Entity e) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).create(entityClass, e);
    }

    public Entity update(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    entityCRUDOperations.get(entityClass).get(currentOperationsScope).update(entityClass, e);
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.UPDATED, entityClass, context, e));
                }
            }).start();
            return null;
        } else {
            return updateSync(entityClass, e);
        }
    }

    public Entity updateSync(Class<? extends Entity> entityClass, Entity e) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).update(entityClass, e);
    }


    public boolean delete(final Class<? extends Entity> entityClass, final Entity e, final EventGenerator context) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (entityCRUDOperations.get(entityClass).get(currentOperationsScope).delete(entityClass, e)) {
                        dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.DELETED, entityClass, context, e));
                    }
                }
            }).start();
            return false;
        } else {
            if (deleteSync(entityClass, e)) {
                dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.DELETED, entityClass, context, e));
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean deleteSync(final Class<? extends Entity> entityClass, final Entity e) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).delete(entityClass, e);
    }

    public Entity read(final Class<? extends Entity> entityClass, final long id, final EventGenerator context) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Entity e = entityCRUDOperations.get(entityClass).get(currentOperationsScope).read(entityClass, id);
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.READ, entityClass, context, e));
                }
            }).start();
            return null;
        } else {
            return readSync(entityClass, id);
        }
    }

    public Entity readSync(final Class<? extends Entity> entityClass, final long id) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).read(entityClass, id);
    }


    public List<Entity> readAllSync(final Class<? extends Entity> entityClass) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).readAll(entityClass);
    }

    public List<Entity> readAll(final Class<? extends Entity> entityClass, final EventGenerator context) {
        if (runEntityCRUDAsync(entityClass)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Entity> es = entityCRUDOperations.get(entityClass).get(currentOperationsScope).readAll(entityClass);
                    dispatcher.notifyListeners(new Event(Event.CRUD.TYPE, Event.CRUD.READALL, entityClass, context, es));
                }
            }).start();
            return null;
        } else {
            return readAllSync(entityClass);
        }
    }

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

}
