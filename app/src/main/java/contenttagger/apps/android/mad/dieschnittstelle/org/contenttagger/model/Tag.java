package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.model;

import java.io.Serializable;
import java.util.List;

/**
 * Created by master on 10.03.16.
 */
public class Tag extends Entity implements Serializable {

    private List<Taggable> taggedItems;

    public Tag(String name) {
        super.setName(name);
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
                "id='" + getId() + '\'' +
                "name='" + getName() + '\'' +
                ", taggedItems=" + taggedItems +
                '}';
    }
}
