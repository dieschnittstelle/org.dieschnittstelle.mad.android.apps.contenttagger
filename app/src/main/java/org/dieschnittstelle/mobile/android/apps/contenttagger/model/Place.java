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

}
