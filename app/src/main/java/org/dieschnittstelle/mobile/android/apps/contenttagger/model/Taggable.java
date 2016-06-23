package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.util.Comparator;
import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public abstract class Taggable extends Entity {

    // Comparators
    public static Comparator<Taggable> COMPARE_BY_NUM_OF_TAGS = new Comparator<Taggable>() {
        @Override
        public int compare(Taggable lhs, Taggable rhs) {
            Integer lhstags = lhs.getTags().size();
            Integer rhstags = rhs.getTags().size();

            return (-1)*lhstags.compareTo(rhstags);
        }
    };

    // Comparators
    public static Comparator<Taggable> COMPARE_BY_TITLE = new Comparator<Taggable>() {
        @Override
        public int compare(Taggable lhs, Taggable rhs) {
            String lhstit = lhs.getTitle();
            String rhstit = rhs.getTitle();
            if (lhstit == null || rhstit == null) {
                return 0;
            }
            return lhstit.compareTo(rhstit);
        }
    };


    public abstract List<Tag> getTags();

    public abstract void addTag(Tag tag);

    public abstract void removeTag(Tag tag);

    public abstract String getTitle();

}
