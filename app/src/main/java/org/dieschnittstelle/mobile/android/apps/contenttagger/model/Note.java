package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import com.orm.dsl.Table;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by master on 15.03.16.
 */
@Table
public class Note extends Entity implements Taggable, Serializable {

    private String title;

    private List<Tag> tags = new ArrayList<Tag>();

    private String content;

    private long lastmodified = System.currentTimeMillis();

    private Long id;

    public Note() {

    }

    public Note(String title,String content) {
        this.title = title;
        this.content = content;
    }

    @Override
    public List<Tag> getTags() {
        return this.tags;
    }

    @Override
    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    @Override
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
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

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(long lastmodified) {
        this.lastmodified = lastmodified;
    }

    // update last modified on update
    public void create()  {
        this.lastmodified = System.currentTimeMillis();
        super.create();
    }

    public void update()  {
        this.lastmodified = System.currentTimeMillis();
        super.update();
    }

}
