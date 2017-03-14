package org.dieschnittstelle.mobile.android.apps.contenttagger.model;

import com.orm.dsl.Ignore;

import org.dieschnittstelle.mobile.android.components.model.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public abstract class Taggable extends Entity {

    @Ignore
    // this attribute will be ignored when persisting / reading because it will be handled via the associations string in prePersist()/postLoad()
    public List<Tag> tags = new ArrayList<Tag>();

    @Ignore
    public List<Taggable> attachments = new ArrayList<Taggable>();

    @Ignore
    public List<Taggable> attachers = new ArrayList<Taggable>();


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

    public List<Tag> getTags() {
        return this.tags;
    }

    public void addTag(Tag tag) {
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
            // we directly access the inverse attribute in order to avoid loops
            tag.getTaggedItems().add(this);
            addPendingUpdate(tag);
        }
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getTaggedItems().remove(this);
        addPendingUpdate(tag);
    }

    @Override
    public void preDestroy() {
        // before a taggable is removed we remove all associations
        for (Tag tag : this.tags) {
            tag.getTaggedItems().remove(this);
            addPendingUpdate(tag);
        }
        for (Taggable attachment : this.attachments) {
            attachment.removeAttacher(this);
            addPendingUpdate(attachment);
        }
        for (Taggable attacher : this.attachers) {
            attacher.removeAttachment(this);
            addPendingUpdate(attacher);
        }
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public void addAttachment(Taggable attachment) {
        if (!this.attachments.contains(attachment)) {
            this.attachments.add(attachment);
            attachment.addAttacher(this);
            addPendingUpdate(attachment);
        }
    }

    public void removeAttachment(Taggable attachment) {
        this.attachments.remove(attachment);
        attachment.removeAttacher(this);
        addPendingUpdate(attachment);
    }

    public List<Taggable> getAttachments() {
        return this.attachments;
    }

    /*
     * bidirectionality attachment-attachers will be handled by methods for attachment (see above)
     */
    public void addAttacher(Taggable attacher) {
        this.attachers.add(attacher);
    }

    public void removeAttacher(Taggable attacher) {
        this.attachers.remove(attacher);
    }

    public List<Taggable> getAttachers() {
        return this.attachers;
    }

    public abstract String getTitle();

}
