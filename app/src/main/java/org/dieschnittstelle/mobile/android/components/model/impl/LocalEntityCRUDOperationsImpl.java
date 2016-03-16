package org.dieschnittstelle.mobile.android.components.model.impl;

import android.util.Log;

import com.orm.SugarRecord;

import org.dieschnittstelle.mobile.android.components.model.Entity;
import org.dieschnittstelle.mobile.android.components.model.EntityCRUDOperations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by master on 14.03.16.
 */
public class LocalEntityCRUDOperationsImpl implements EntityCRUDOperations {

    protected static String logger = "LocalEntityCRUDOperationsImpl";

    @Override
    public void create(Class<? extends Entity> entityClass, Entity e) {
        Log.d(logger, "create():" + e);
        Long id = SugarRecord.save(e);
        Log.d(logger, "create(): id: " + id);
        e.setId(id);
    }

    @Override
    public void update(Class<? extends Entity> entityClass, Entity e) {
        Log.d(logger, "update():" + e);
        SugarRecord.save(e);
    }

    @Override
    public boolean delete(Class<? extends Entity> entityClass, Entity e) {
        Log.d(logger, "delete():" + e);
        return SugarRecord.delete(e);
    }

    @Override
    public Entity read(Class<? extends Entity> entityClass, long id) {
        Log.d(logger, "read():" + id);
        return SugarRecord.findById(entityClass,id);
    }

    @Override
    public List<Entity> readAll(Class<? extends Entity> entityClass) {
        Log.d(logger, "readAll()");
        List<Entity> entities = new ArrayList<Entity>();
        Iterator<? extends Entity> it = SugarRecord.findAll(entityClass);
        while (it.hasNext()) {
            entities.add(it.next());
        }

        return entities;
    }
}
