package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
    private EntityListAdapter adapter;

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
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar, 0, 0) {

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


        // create an adapter for the recycler view
        this.adapter = new EntityListAdapter<Tag,TagViewHolder>(this, (RecyclerView) findViewById(R.id.taglist), R.layout.tags_overview_itemview, R.layout.itemmenu_tags_overview, new int[]{R.id.action_delete, R.id.action_edit}) {

            @Override
            public TagViewHolder onCreateEntityViewHolder(View view, EntityListAdapter adapter) {
                return new TagViewHolder(view,adapter);
            }

            @Override
            public void onBindEntityViewHolder(TagViewHolder holder, Tag entity, int position) {
                holder.name.setText(entity.getName());
            }

            @Override
            protected void onSelectEntity(Tag entity) {
                Log.i(logger, "onSelectEntity(): " + entity);
            }

            @Override
            protected void onSelectEntityMenuAction(int action, Tag entity) {
                Log.i(logger, "onSelectEntityMenuAction(): " + action + "@" + entity);
                if (action == R.id.action_delete) {
                    this.removeItem(entity);
                }
            }

            @Override
            public void onBindEntityMenuDialog(EntityListAdapter.ItemMenuDialogViewHolder holder, Tag item) {
                ((TextView)holder.heading).setText(item.getName());
            }
        };

        new AsyncTask<Void, Void, List<Tag>>() {

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

    private class TagViewHolder extends EntityListAdapter.EntityViewHolder {

        public TextView name;

        public TagViewHolder(final View itemView, EntityListAdapter adapter) {
            super(itemView,adapter);
            name = (TextView)itemView.findViewById(R.id.name);
        }

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
        if (id == R.id.action_add) {
            adapter.addItem(new Tag("New"));
        }

        return super.onOptionsItemSelected(item);
    }

}
