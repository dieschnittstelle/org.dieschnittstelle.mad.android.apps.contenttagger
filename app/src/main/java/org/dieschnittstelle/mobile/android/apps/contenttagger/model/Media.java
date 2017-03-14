package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import android.graphics.Bitmap;

import com.orm.dsl.Ignore;
import com.orm.dsl.Table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by master on 15.03.16.
 */
@Table
public class Media extends Taggable implements Serializable {

    // Comparators
    public static Comparator<Media> COMPARE_BY_DATE = new Comparator<Media>() {
        @Override
        public int compare(Media lhs, Media rhs) {
            Long lhsdate = lhs.created;
            Long rhsdate = rhs.created;
            return lhsdate.compareTo(rhsdate);
        }
    };


    private String title;

    private long created;

    private String contentUri;

    private Long id;

    private String associations;

    @Ignore
    private Bitmap thumbnail;

    public Media() {

    }

    public Media(String title, String contentUri) {
        this.title = title;
        this.contentUri = contentUri;
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

    public String getContentUri() {
        return contentUri;
    }

    public void setContentUri(String contentUri) {
        this.contentUri = contentUri;
    }

//    @Override
//    public void preDestroy() {
//        // before a link is removed, we need to remove it from any tags that are associated with it
//        for (Tag tag : this.getTags()) {
//            tag.getTaggedItems().remove(this);
//            addPendingUpdate(tag);
//        }
//    }

    // update last modified on update
    public void create()  {
        this.created = System.currentTimeMillis();
        super.create();
    }

    public long getCreated() {
        return this.created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "Media{" +
                "title='" + title + '\'' +
                ", contentUri='" + contentUri + '\'' +
                ", created=" + created +
                ", id=" + id +
                ", tags=" + getTags() +
                '}';
    }

    public void update()  {
        this.created = System.currentTimeMillis();
        super.update();
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }
}
