package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import com.orm.dsl.Ignore;
import com.orm.dsl.Table;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by master on 15.03.16.
 */
@Table
public class Note extends Taggable implements Serializable {


    // Comparators
    public static Comparator<Note> COMPARE_BY_DATE = new Comparator<Note>() {
        @Override
        public int compare(Note lhs, Note rhs) {
            Long lhsdate = lhs.lastmodified;
            Long rhsdate = rhs.lastmodified;
            return lhsdate.compareTo(rhsdate);
        }
    };


    private String title;

    private String content;

    private long lastmodified = System.currentTimeMillis();

    private Long id;

    private String associations;

    public Note() {

    }

    public Note(String title,String content) {
        this.title = title;
        this.content = content;
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

    @Override
    public String toString() {
        return "Note{" +
                "title='" + title + '\'' +
                ", tags=" + getTags() +
                ", content='" + content + '\'' +
                ", lastmodified=" + lastmodified +
                ", id=" + id +
                '}';
    }

    public void update()  {
        this.lastmodified = System.currentTimeMillis();
        super.update();
    }

}
