package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.controller;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.R;

public class MainActivity extends ActionBarActivity {

    protected static String logger = "TagsOverviewActvity";

    // the drawer layout
    private DrawerLayout drawerLayout;

    // the toggle control for the drawer
    private ActionBarDrawerToggle drawerToggle;

    // the main menu options displayed in the drawer view
    private ListView mainMenuOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // read out the toolbar from the layout and set it as actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        // the following statements allow to open the drawer menu on pressing the "home" icon and set a custom home icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);
        // this allows to react to opening/closing the drawer - not really necessary
        this.drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        this.drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0) {

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

        ((DrawerLayout) findViewById(R.id.main_drawer_layout)).setDrawerListener(drawerToggle);

        // initialise the drawer
        this.mainMenuOptions = (ListView)findViewById(R.id.main_menu);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.main_menu_itemview,new String[]{"Tags","Notes","Media","Locations"});
        this.mainMenuOptions.setAdapter(adapter);
        this.mainMenuOptions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String option = adapter.getItem(position);
                mainMenuOptions.setItemChecked(position, true);
                drawerLayout.closeDrawer(mainMenuOptions);

                Log.i(logger, "onItemClick(): " + option);
                //
                Fragment nextContentView = null;
                if ("Tags".equalsIgnoreCase(option)) {
                    switchContentview("Tags", new TagsOverviewFragment());
                } else if ("Notes".equalsIgnoreCase(option)){
                    switchContentview("Notes",new NotesOverviewFragment());
                }
            }

        });

        // create the start fragment
        switchContentview("Tags", new TagsOverviewFragment());
    }

    public void switchContentview(String title,Fragment contentViewFragment) {
        setTitle(title);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main_contentview, contentViewFragment);
        ft.commit();
    }


    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }


    /*
     * the following methods are taken over from the android developer doc
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }
}
