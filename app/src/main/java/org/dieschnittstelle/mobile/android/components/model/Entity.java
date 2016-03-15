package org.dieschnittstelle.mobile.android.components.model;

import android.content.Context;

import org.dieschnittstelle.mobile.android.components.events.Event;
import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventGenerator;
import org.dieschnittstelle.mobile.android.components.model.impl.EntityManager;

import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public abstract class Entity {

    // obtain a reference to the event dispatcher
    private static EventDispatcher eventDispatcher = EventDispatcher.getInstance();

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
        create(null);
    }

    public void update() {
        update(null);
    }

    public void delete() {
        delete(null);
    }

    public static Entity read(Class<? extends Entity> entityClass,long id) {
        return read(entityClass,id,null);
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

    public static Entity createSync(Class<? extends Entity> entityClass,Entity e) {
        return EntityManager.getInstance().createSync(entityClass, e);
    }

    public static Entity updateSync(Class<? extends Entity> entityClass,Entity e) {
        return EntityManager.getInstance().updateSync(entityClass, e);
    }

    public static boolean deleteSync(Class<? extends Entity> entityClass,Entity e) {
        return EntityManager.getInstance().deleteSync(entityClass, e);
    }

    /*
     * methods with argument for the caller context
     */

    public void create(EventGenerator context) {
        EntityManager.getInstance().create(this.getClass(), this, context);
    }

    public void update(EventGenerator context) {
        EntityManager.getInstance().update(this.getClass(), this, context);
    }

    public void delete(EventGenerator context) {
        EntityManager.getInstance().delete(this.getClass(), this, context);
    }

    public static Entity read(Class<? extends Entity> entityClass,long id, EventGenerator context) {
        return EntityManager.getInstance().read(entityClass,id,context);
    }

    public static List<? extends Entity> readAll(Class<? extends Entity> entityClass, EventGenerator context) {
        return EntityManager.getInstance().readAll(entityClass,context);
    }

    public boolean created() {
        return getId() != null;
    }

}
