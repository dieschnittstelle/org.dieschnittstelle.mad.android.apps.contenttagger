package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import com.orm.dsl.Ignore;
import com.orm.dsl.Table;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by master on 15.03.16.
 */
@Table
public class Link extends Taggable implements Serializable {

    private String title;

    @Ignore
    // this attribute will be ignored when persisting / reading because it will be handled via the associations string in prePersist()/postLoad()
    private List<Tag> tags = new ArrayList<Tag>();

    private long created;

    private String url;

    private Long id;

    private String associations;

    public Link() {

    }

    public Link(String title, String url) {
        this.title = title;
        this.url = url;
    }

    @Override
    public List<Tag> getTags() {
        return this.tags;
    }

    @Override
    public void addTag(Tag tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
            tag.getTaggedItems().add(this);
            addPendingUpdate(tag);
        }
    }

    @Override
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getTaggedItems().remove(this);
        addPendingUpdate(tag);
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setAssociations(String assoc) {
        this.associations = assoc;
    }

    @Override
    public String getAssociations() {
        return this.associations;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void preDestroy() {
        // before a link is removed, we need to remove it from any tags that are associated with it
        for (Tag tag : this.tags) {
            tag.getTaggedItems().remove(this);
            addPendingUpdate(tag);
        }
    }

    // update last modified on update
    public void create()  {
        this.created = System.currentTimeMillis();
        super.create();
    }

    @Override
    public String toString() {
        return "Link{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", created=" + created +
                ", id=" + id +
                ", tags=" + tags +
                '}';
    }

    public void update()  {
        this.created = System.currentTimeMillis();
        super.update();
    }


}
