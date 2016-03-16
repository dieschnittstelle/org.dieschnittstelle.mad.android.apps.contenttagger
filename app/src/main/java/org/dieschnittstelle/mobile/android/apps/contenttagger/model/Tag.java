package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import com.orm.SugarRecord;
import com.orm.dsl.Table;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.io.Serializable;
import java.util.List;

/**
 * Created by master on 10.03.16.
 */
// we mark this as an entity in the sense of sugar orm
@Table
public class Tag extends Entity implements Serializable {

    // we need to declare the id locally...
    private Long id;

    public Long getId() {
        return this.id;
    }

    @Override
    public void setAssociations(String assoc) {

    }

    @Override
    public String getAssociations() {
        return null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    private List<Taggable> taggedItems;

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

    @Override
    public String toString() {
        return "Tag{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", taggedItems=" + taggedItems +
                '}';
    }

}
