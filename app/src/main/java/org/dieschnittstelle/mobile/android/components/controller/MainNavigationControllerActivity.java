package org.dieschnittstelle.mobile.android.components.controller;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/*
 * removed dependencies to application specific resources by using getResources().getIdentifier(), see http://stackoverflow.com/questions/3476430/how-to-get-a-resource-id-with-a-known-resource-name
 */
public class MainNavigationControllerActivity extends ActionBarActivity {

    protected static String logger = "MainNavigationControllerActvity";

    // the drawer layout
    private DrawerLayout drawerLayout;

    // the toggle control for the drawer
    private ActionBarDrawerToggle drawerToggle;

    // the main menu options displayed in the drawer view
    private ListView mainMenu;

    private String[] mainMenuOptions;

    private String[] mainMenuOptionsControllerClassnames;

    private String appPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appPackage = getApplicationContext().getPackageName();
        setContentView(getResources().getIdentifier("main_activity", "layout", appPackage)/*R.layout.main_activity*/);

        /*
         * initialise the toolbar
         */
        Toolbar toolbar = (Toolbar) findViewById(getResources().getIdentifier("main_toolbar","id",appPackage) /*R.id.main_toolbar*/);
        setSupportActionBar(toolbar);
        // the following statements allow to open the drawer menu on pressing the "home" icon and set a custom home icon
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(getResources().getIdentifier("ic_drawer","drawable",appPackage)/*R.drawable.ic_drawer*/);

        /*
         * initialise the main menu
         */
        this.drawerLayout = (DrawerLayout) findViewById(getResources().getIdentifier("main_drawer_layout","id",appPackage) /*R.id.main_drawer_layout*/);
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

        ((DrawerLayout) findViewById(getResources().getIdentifier("main_drawer_layout","id",appPackage) /*R.id.main_drawer_layout*/)).setDrawerListener(drawerToggle);

        this.mainMenu = (ListView) findViewById(getResources().getIdentifier("main_menu","id",appPackage)); // (ListView) findViewById(R.id.main_menu);
        this.mainMenuOptions = getResources().getStringArray(getResources().getIdentifier("main_menu_items","array",appPackage)); // getResources().getStringArray(R.array.main_menu_items);
        this.mainMenuOptionsControllerClassnames = getResources().getStringArray(getResources().getIdentifier("main_menu_controllers","array",appPackage)); // getResources().getStringArray(R.array.main_menu_controllers);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, /*R.layout.main_menu_itemview*/getResources().getIdentifier("main_menu_itemview","layout",appPackage), mainMenuOptions) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View itemview = getLayoutInflater().inflate(/*R.layout.main_menu_itemview*/getResources().getIdentifier("main_menu_itemview","layout", appPackage), null);
                // read out the image view and the text view
                ImageView iconview = (ImageView) itemview.findViewById(/*R.id.itemicon*/ getResources().getIdentifier("itemicon","id",appPackage));
                TextView nameview = (TextView) itemview.findViewById(/*R.id.itemname*/ getResources().getIdentifier("itemname","id",appPackage));
                nameview.setText(mainMenuOptions[position]);
                iconview.setImageDrawable(getResources().getDrawable(getResources().obtainTypedArray(/*R.array.main_menu_icons*/ getResources().getIdentifier("main_menu_icons","array",appPackage)).getResourceId(position, -1)));

                return itemview;
            }

        };

        this.mainMenu.setAdapter(adapter);
        this.mainMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String option = adapter.getItem(position);
                mainMenu.setItemChecked(position, true);
                drawerLayout.closeDrawer(findViewById(getResources().getIdentifier("main_menu_container","id",appPackage)));
                Log.i(logger, "onItemClick(): " + option);
                showViewForOption(position);
            }

        });

        /*
         * initialise the clear-all action if it exists
         */
        View clearAllAction = findViewById(getResources().getIdentifier("action_clear_all_data","id",appPackage));
        if (clearAllAction != null) {
            Log.d(logger,"main view provides clear-all action: " + clearAllAction);
            clearAllAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // try to read out the metadata for the database - assuming we use sugar orm
                        ApplicationInfo ai = getPackageManager().getApplicationInfo(appPackage, PackageManager.GET_META_DATA);
                        Bundle metaData = ai.metaData;
                        if (metaData == null) {
                            Log.i(logger, "no metadata specified in manifest for application. Cannot reset database...");
                        } else {
                            String database = metaData.getString("DATABASE");
                            Log.i(logger,"about to delete database: " + database);
                            deleteDatabase(database);
                            MainNavigationControllerActivity.this.finish();
                        }
                    }
                    catch (Exception e) {
                        Log.e(logger,"got exception trying to access application metadata: " + e,e);
                    }
                }
            });
        }
        else {
            Log.d(logger, "not clear-all action provided.");
        }

        /*
         * show the initial view
         */
        showViewForOption(getResources().getInteger(getResources().getIdentifier("main_menu_initial_view","integer",appPackage) /*R.integer.main_menu_initial_view*/));
    }

    /*
     * this takes an integer for some option and reads out the resources to initialise the view
     */
    public void showViewForOption(int position) {
        // we try to option the controller class and option name for the given option
        String optionName = mainMenuOptions[position];
        String optionsControllerClassname = mainMenuOptionsControllerClassnames[position];

        if (optionsControllerClassname != null && !"".equals(optionsControllerClassname.trim())) {
            try {
                setTitle(optionName);
                showView((Class<Fragment>) Class.forName(optionsControllerClassname), null, false);
            }
            catch (ClassNotFoundException cnfe) {
                Log.e(logger, "controller class cannot be found: " + optionsControllerClassname,cnfe);
            }
        }
        else {
            Log.e(logger,"no controller classname specified for option " + optionName + ". Ignore.");
        }

    }

    public void showView(Class<?> viewclass, Bundle args, boolean addToBackstack) {
        try {
            Fragment view = (Fragment)viewclass.newInstance();
            if (args != null) {
                view.setArguments(args);
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(getResources().getIdentifier("main_contentview","id",appPackage), view);
            if (addToBackstack) {
                ft.addToBackStack(null);
            }
            ft.commit();
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    /*
     * some utility
     */
    public static Bundle createArguments(String key,Long value) {
        Bundle bun = new Bundle();
        bun.putLong(key,value);

        return bun;
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
        this.drawerToggle.onConfigurationChanged(newConfig);
    }

    /*
     * we need to override onBackPressed and pop from the fragment manager
     */
    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        }
        else {
            super.onBackPressed();
        }
    }
}
