package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public class Tag extends Entity implements Serializable {

    private String name;

    private List<Taggable> taggedItems;

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Taggable> getTaggedItems() {
        return taggedItems;
    }

    public void setTaggedItems(List<Taggable> taggedItems) {
        this.taggedItems = taggedItems;
    }


    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", taggedItems=" + taggedItems +
                '}';
    }
}