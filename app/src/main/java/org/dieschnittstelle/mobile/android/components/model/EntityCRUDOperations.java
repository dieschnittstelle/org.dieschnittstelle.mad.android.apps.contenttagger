package org.dieschnittstelle.mobile.android.components.model;

import java.util.List;

/**
 * Created by master on 14.03.16.
 */
public interface EntityCRUDOperations {

    /*
     * create and update are assumed to be implemented using side effects on the entity being passed!
     */
    public void create(Class<? extends Entity> entityClass, Entity e);

    public void update(Class<? extends Entity> entityClass, Entity e);

    public boolean delete(Class<? extends Entity> entityClass, Entity e);

    public Entity read(Class<? extends Entity> entityClass,long id);

    public List<Entity> readAll(Class<? extends Entity> entityClass);

}
