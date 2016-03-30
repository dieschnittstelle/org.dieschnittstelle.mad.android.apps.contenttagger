package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public abstract class Taggable extends Entity {

    public abstract List<Tag> getTags();

    public abstract void addTag(Tag tag);

    public abstract void removeTag(Tag tag);

    public abstract String getTitle();

}
