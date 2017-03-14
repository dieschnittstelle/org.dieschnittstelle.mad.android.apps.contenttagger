package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

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
public class Place extends Taggable implements Serializable {

    private String title;

    private Long id;

    private String associations;

    private double geolat;

    private double geolong;

    public Place() {

    }

    public Place(String title) {
        this.title = title;
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

    // update last modified on update
    public void create()  {
        super.create();
    }

    @Override
    public String toString() {
        return "Place{" +
                "title='" + title + '\'' +
                ", id=" + id +
                ", geolat=" + geolat +
                ", geolong=" + geolong +
                '}';
    }

    public double getGeolat() {
        return geolat;
    }

    public void setGeolat(double geolat) {
        this.geolat = geolat;
    }

    public double getGeolong() {
        return geolong;
    }

    public void setGeolong(double geolong) {
        this.geolong = geolong;
    }


    public void update()  {
        super.update();
    }

//    /*
//     * this is for attachment handling
//     *
//     * TODO: clarify whether this can be placed in the abstract superclass
//     */
//    @Ignore
//    private List<Taggable> attachments = new ArrayList<Taggable>();
//
//    @Ignore
//    private List<Taggable> attachers = new ArrayList<Taggable>();
//
//    public void addAttachment(Taggable attachment) {
//        this.attachments.add(attachment);
//    }
//
//    public void removeAttachment(Taggable attachment) {
//        this.attachments.remove(attachment);
//    }
//
//    public List<Taggable> getAttachments() {
//        return this.attachments;
//    }
//
//    public void addAttacher(Taggable attacher) {
//        this.attachers.add(attacher);
//    }
//
//    public void removeAttacher(Taggable attacher) {
//        this.attachers.remove(attacher);
//    }
//
//    public List<Taggable> getAttachers() {
//        return this.attachers;
//    }

}
