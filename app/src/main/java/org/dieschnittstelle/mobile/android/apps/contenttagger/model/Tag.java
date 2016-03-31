package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import android.util.Log;

import com.orm.SugarRecord;
import com.orm.dsl.Table;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by master on 10.03.16.
 */
// we mark this as an entity in the sense of sugar orm
@Table
public class Tag extends Entity implements Serializable {

    protected static String logger = "Entity";

    // we need to declare the id locally...
    private Long id;

    public Long getId() {
        return this.id;
    }

    private String associations;

    @Override
    public void setAssociations(String assoc) {
        this.associations = assoc;
    }

    @Override
    public String getAssociations() {
        return this.associations;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private List<Taggable> taggedItems = new ArrayList<Taggable>();

    private String name;

    public Tag() {

    }

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Taggable> getTaggedItems() {
        return taggedItems;
    }

    public void setTaggedItems(List<Taggable> taggedItems) {
        this.taggedItems = taggedItems;
    }

    public void addTaggedItem(Taggable item) {
        if (!this.taggedItems.contains(item)) {
            this.taggedItems.add(item);
            item.getTags().add(this);
            addPendingUpdate(item);
        }
    }

    public void removeTaggedItem(Taggable item) {
        this.taggedItems.remove(item);
        item.getTags().remove(this);
        addPendingUpdate(item);
    }

    public Map<Class<Taggable>,List<Taggable>> getTaggedItemsAsGroups() {
        Map<Class<Taggable>,List<Taggable>> itemGroups = new HashMap<Class<Taggable>,List<Taggable>>();
        for (Taggable item : this.taggedItems) {
            List<Taggable> currentGroup = itemGroups.get(item.getClass());
            if (currentGroup == null) {
                currentGroup = new ArrayList<Taggable>();
                itemGroups.put((Class<Taggable>)item.getClass(),currentGroup);
            }
            currentGroup.add(item);
        }

        return itemGroups;
    }

    @Override
    public void preDestroy() {
        Log.i(logger, "preDestroy(): removing reference from " + this.taggedItems.size() + " tagged items");
        // before a link is removed, we need to remove it from any tags that are associated with it
        for (Taggable item : this.taggedItems) {
            Log.d(logger,"preDestroy(): removing reference from item: " + item);
            item.getTags().remove(this);
            Log.d(logger, "preDestroy(): after removal, item is: " + item);
            addPendingUpdate(item);
        }
    }

    @Override
    public String toString() {
        return "Tag{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", taggedItems=" + taggedItems.size() +
                '}';
    }

}
