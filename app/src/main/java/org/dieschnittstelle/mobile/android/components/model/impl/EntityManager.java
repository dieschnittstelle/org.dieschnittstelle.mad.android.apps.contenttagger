package org.dieschnittstelle.mobile.android.components.model.impl;

import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.model.EntityCRUDOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by master on 14.03.16.
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

    /*
     * the current scope
     */
    private CRUDOperationsScope currentOperationsScope = CRUDOperationsScope.LOCAL;

    /*
     * a mapping of classes to crud operations implementations
     */
    private Map<Class<? extends Entity>,Map<CRUDOperationsScope,EntityCRUDOperations>> entityCRUDOperations = new HashMap<Class<? extends Entity>,Map<CRUDOperationsScope,EntityCRUDOperations>>();

    /*
     * a mapping of classes to entity instances
     */
    private Map<Class<Entity>,List<Entity>> entityInstances = new HashMap<Class<Entity>,List<Entity>>();


    @Override
    public Entity create(Class<? extends Entity> entityClass, Entity e) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).create(entityClass,e);
    }

    @Override
    public Entity update(Class<? extends Entity> entityClass, Entity e) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).update(entityClass, e);
    }

    @Override
    public boolean delete(Class<? extends Entity> entityClass, Entity e) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).delete(entityClass, e);
    }

    @Override
    public Entity read(Class<? extends Entity> entityClass, long id) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).read(entityClass, id);
    }

    @Override
    public List<Entity> readAll(Class<? extends Entity> entityClass) {
        return entityCRUDOperations.get(entityClass).get(currentOperationsScope).readAll(entityClass);
    }

    public void addEntityCRUDOperationsImpl(Class<? extends Entity> entityClass,CRUDOperationsScope scope,EntityCRUDOperations impl) {
        // check whether we alreary have an operations map
        Map<CRUDOperationsScope,EntityCRUDOperations> crudops = entityCRUDOperations.get(entityClass);
        if (crudops == null) {
            crudops = new HashMap<CRUDOperationsScope,EntityCRUDOperations>();
            entityCRUDOperations.put(entityClass,crudops);
        }
        crudops.put(scope,impl);
    }

}
