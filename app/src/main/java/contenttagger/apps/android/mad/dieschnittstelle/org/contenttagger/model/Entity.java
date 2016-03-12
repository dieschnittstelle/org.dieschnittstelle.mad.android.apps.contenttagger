package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.model;

/**
 * Created by master on 10.03.16.
 */
public abstract class Entity {

    private long id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }


}
