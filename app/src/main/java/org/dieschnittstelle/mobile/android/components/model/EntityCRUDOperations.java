package org.dieschnittstelle.mobile.android.components.model;

import java.util.List;

/**
 * Created by master on 14.03.16.
 */
public interface EntityCRUDOperations {

    public Entity create(Class<? extends Entity> entityClass, Entity e);

    public Entity update(Class<? extends Entity> entityClass, Entity e);

    public boolean delete(Class<? extends Entity> entityClass, Entity e);

    public Entity read(Class<? extends Entity> entityClass,long id);

    public List<Entity> readAll(Class<? extends Entity> entityClass);

}
