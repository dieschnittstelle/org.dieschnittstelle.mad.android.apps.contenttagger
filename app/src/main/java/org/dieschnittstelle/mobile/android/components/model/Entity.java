package org.dieschnittstelle.mobile.android.components.model;

import org.dieschnittstelle.mobile.android.components.model.impl.EntityManager;

import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public abstract class Entity {

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

    public Entity() {

    }

    public void create() {
        EntityManager.getInstance().create(this.getClass(),this);
    }

    public void update() {
        EntityManager.getInstance().update(this.getClass(), this);
    }

    public boolean delete() {
        return EntityManager.getInstance().delete(this.getClass(), this);
    }

    public static Entity read(Class<? extends Entity> entityClass,long id) {
        return EntityManager.getInstance().read(entityClass,id);
    }

    public static List<? extends Entity> readAll(Class<? extends Entity> entityClass) {
        return EntityManager.getInstance().readAll(entityClass);
    }

}
