package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public interface Taggable {

    public List<Tag> getTags();

    public void addTag(Tag tag);

    public void removeTag(Tag tag);

    public String getTitle();

}
