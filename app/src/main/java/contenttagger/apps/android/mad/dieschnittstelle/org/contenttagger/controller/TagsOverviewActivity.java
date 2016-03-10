package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.controller;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.R;
import contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.model.Entity;
import contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.model.Tag;

public class TagsOverviewActivity extends ActionBarActivity {

    protected static String logger = "TagsOverviewActvity";

    /*
     * the adapter for the listview
     */
    private TagListAdapter adapter = new TagListAdapter(new ArrayList<Tag>(), R.layout.tags_overview_itemview);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags_overview);

        // read out the toolbar from the layout and set it as actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_overview);
        setSupportActionBar(toolbar);
        // the following statements allow to open the drawer menu on pressing the "home" icon and set a custom home icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);
        // this allows to react to opening/closing the drawer - not really necessary
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,(DrawerLayout)findViewById(R.id.drawer_layout),toolbar,0,0) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.i(logger, "onDrawerOpened()");
            }

            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                Log.i(logger, "onDrawerClosed()");
            }
        };

        ((DrawerLayout) findViewById(R.id.drawer_layout)).setDrawerListener(toggle);


        // instantiate the listview adapter
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.taglist);
        // it does not seem to be straightforward to specify a separator...

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        new AsyncTask<Void,Void,List<Tag>>() {

            @Override
            protected List<Tag> doInBackground(Void... params) {
                return readAllTags();
            }

            @Override
            protected void onPostExecute(List<Tag> tags) {
                adapter.addItems(tags);
            }

        }.execute();

    }

    /*
     * read all tags
     */
    public List<Tag> readAllTags() {
        List<Tag> tags = new ArrayList();
        tags.add(new Tag("t1"));
        tags.add(new Tag("t2"));
        tags.add(new Tag("t3"));

        return tags;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tags_overview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * an inner class for handling the recyclerview, following http://antonioleiva.com/recyclerview/
     */
    public static class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {

        private List<Tag> tags;
        private int itemLayout;

        public TagListAdapter(List<Tag> tags, int itemLayout) {
            this.tags = tags;
            this.itemLayout = itemLayout;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            Tag tag = tags.get(position);
            holder.name.setText(tag.getName());
            holder.itemView.setTag(tag);
        }

        @Override public int getItemCount() {
            return this.tags.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name;

            public ViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
            }
        }

        public void addItem(Tag item) {
            this.tags.add(item);
            this.notifyItemInserted(this.tags.size()-1);
        }

        public void addItems(List<Tag> items) {
            int sizeBefore = this.tags.size();
            this.tags.addAll(items);
            this.notifyItemRangeInserted(sizeBefore,items.size());
        }

        public void removeItem(Tag item) {
            int index = this.tags.indexOf(item);
            if (index > -1) {
                this.tags.remove(index);
                this.notifyItemRemoved(index);
            }
        }

        public void updateItem(Tag item) {
            int index = this.tags.indexOf(item);
            if (index > -1) {
                this.notifyItemChanged(index);
            }
        }

    }
}
