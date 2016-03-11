package contenttagger.apps.android.mad.dieschnittstelle.org.contenttagger.controller;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LauncherActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.Serializable;
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
    private TagListAdapter adapter;

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
        this.adapter = new TagListAdapter(this,new ArrayList<Tag>(), R.layout.tags_overview_itemview,R.layout.itemmenu_tags_overview,new int[]{R.id.action_delete,R.id.action_edit});

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
        if (id == R.id.action_add) {
            adapter.addItem(new Tag("New"));
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * an inner class for handling the recyclerview, following http://antonioleiva.com/recyclerview/
     */
    public static class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.ViewHolder> {

        private Activity controller;

        private List<Tag> tags;
        private int itemLayout;
        public int[] itemMenuActions;
        public int itemMenuLayout;

        public View.OnTouchListener onTouchItemListener = new SimpleOnTouchListener(new SimpleOnSelectListItemListener(this));
        public View.OnTouchListener onTouchItemMenuListener = new SimpleOnTouchListener(new SimpleOnSelectListItemMenuListener(this));
        public View.OnTouchListener onTouchItemMenuActionListener = new SimpleOnTouchListener(new SimpleOnSelectListItemMenuActionListener(this));

        public TagListAdapter(Activity controller,List<Tag> tags, int itemLayout, int itemMenuLayout, int[] itemMenuMenuActions) {
            this.tags = tags;
            this.itemLayout = itemLayout;
            this.controller = controller;
            this.itemMenuLayout = itemMenuLayout;
            this.itemMenuActions = itemMenuMenuActions;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
            return new ViewHolder(v,this);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            // only show the divider if we have not reached the last list item
            Tag tag = tags.get(position);
            holder.name.setText(tag.getName());
            holder.itemView.setTag(tag);
            holder.itemMenu.setTag(tag);
        }


        @Override public int getItemCount() {
            return this.tags.size();
        }

        public class SimpleOnTouchListener implements View.OnTouchListener {

            private View.OnClickListener onClickListener;

            public SimpleOnTouchListener() {

            }

            public SimpleOnTouchListener(View.OnClickListener onClickListener) {
                this.onClickListener = onClickListener;
            }

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch(event.getAction()) {
                    case (MotionEvent.ACTION_DOWN) :
                        Log.d(logger,"Action was DOWN");
                        view.setSelected(true);
                        return true;
                    case (MotionEvent.ACTION_MOVE) :
                        Log.d(logger,"Action was MOVE");
                        view.setSelected(false);
                        return true;
                    case (MotionEvent.ACTION_UP) :
                        Log.d(logger,"Action was UP");
                        view.setSelected(false);
                        if (this.onClickListener != null) {
                            this.onClickListener.onClick(view);
                        }
                        return true;
                    case (MotionEvent.ACTION_CANCEL) :
                        Log.d(logger,"Action was CANCEL");
                        view.setSelected(false);
                        return true;
                    case (MotionEvent.ACTION_OUTSIDE) :
                        view.setSelected(false);
                        Log.d(logger,"Movement occurred outside bounds " +
                                "of current screen element");
                        return true;
                    default :
                        return false;
                }
            }
        }

        public class SimpleOnSelectListItemListener implements View.OnClickListener {

            private TagListAdapter adapter;

            public SimpleOnSelectListItemListener(TagListAdapter adapter) {
                this.adapter = adapter;
            }

            @Override
            public void onClick(View v) {
                adapter.onSelectListItem((Tag)v.getTag());
            }
        }

        public class SimpleOnSelectListItemMenuListener implements View.OnClickListener {

            private TagListAdapter adapter;

            public SimpleOnSelectListItemMenuListener(TagListAdapter adapter) {
                this.adapter = adapter;
            }

            @Override
            public void onClick(View v) {
                adapter.onSelectListItemMenu((Tag) v.getTag());
            }
        }

        public class SimpleOnSelectListItemMenuActionListener implements View.OnClickListener {

            private TagListAdapter adapter;

            public SimpleOnSelectListItemMenuActionListener(TagListAdapter adapter) {
                this.adapter = adapter;
            }

            @Override
            public void onClick(View v) {
                adapter.onSelectListItemMenuAction(v.getId(),(Tag) v.getTag());
            }
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name;
            public View itemMenu;
            public View divider;

            public ViewHolder(final View itemView,TagListAdapter adapter) {
                super(itemView);
                this.name = (TextView) itemView.findViewById(R.id.name);
                this.divider = itemView.findViewById(R.id.divider);
                this.itemMenu = itemView.findViewById(R.id.listitem_menu);

                // set listeners for visual (touch) and functional (click) feedback
                this.itemView.setOnTouchListener(adapter.onTouchItemListener);
                this.itemMenu.setOnTouchListener(adapter.onTouchItemMenuListener);
            }

        }

        public void onSelectListItem(Tag tag) {
            Log.i(logger,"onSelectListItem(): " + tag);
        }

        public void onSelectListItemMenu(Tag tag) {
            Log.i(logger,"onSelectListItemMenu(): " + tag);
            showMenuItemDialog(tag);
        }

        public void onSelectListItemMenuAction(int action,Tag tag) {
            Log.i(logger, "onSelectListItemMenuAction(): " + action + "@" + tag);
            if (action == R.id.action_delete) {
                removeItem(tag);
            }
            hideMenuItemDialog();
        }


        public void addItem(Tag item) {
            this.tags.add(item);
            this.notifyItemInserted(this.tags.size() - 1);
        }

        public void addItems(List<Tag> items) {
            int sizeBefore = this.tags.size();
            this.tags.addAll(items);
            this.notifyItemRangeInserted(sizeBefore, items.size());
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

        private void showMenuItemDialog(Tag item) {
            // in contrast to the example we should not need to consider the case that there is already an existing instance of the dialog...
            FragmentTransaction ft = controller.getFragmentManager().beginTransaction();
            Fragment prev = controller.getFragmentManager().findFragmentByTag("menuItemDialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog.
            DialogFragment dialog = ListItemMenuDialogFragment.newInstance(item, this);
            dialog.show(ft, "menuItemDialog");
        }

        private void hideMenuItemDialog() {
            FragmentTransaction ft = controller.getFragmentManager().beginTransaction();
            Fragment prev = controller.getFragmentManager().findFragmentByTag("menuItemDialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.commit();
        }

    }

    /*
     * a dialog for selecting actions for a list item, following http://developer.android.com/reference/android/app/DialogFragment.html
     */
    public static class ListItemMenuDialogFragment extends DialogFragment {

        private static String logger = "ListItemMenuDialogFragment";

        private Tag item;

        private TagListAdapter adapter;

        static ListItemMenuDialogFragment newInstance(Tag item,TagListAdapter adapter) {
            ListItemMenuDialogFragment instance = new ListItemMenuDialogFragment();
            instance.setItem(item);
            instance.setAdapter(adapter);

//            // take over argument passing via bundle rather than setting the object directly...
//            Bundle args = new Bundle();
//            args.putSerializable("item", item);
//            instance.setArguments(args);

            return instance;
        }

        public void setItem(Tag item) {
            this.item = item;
        }

        public void setAdapter(TagListAdapter adapter) {
            this.adapter = adapter;
        }

        public void onCreate(Bundle savedInstanceState) {
            Log.i(logger, "onCreate()");
            super.onCreate(savedInstanceState);
//            this.item = (Tag) getArguments().getSerializable("item");
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.i(logger, "onCreateView()");

            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            ViewGroup dialogView = (ViewGroup)inflater.inflate(adapter.itemMenuLayout, container, false);
                TextView heading = (TextView)dialogView.findViewById(R.id.heading);
            heading.setText(item.getName());

            // we iterate over the actions and set a listener
            for (int i=0;i<adapter.itemMenuActions.length;i++) {
                View currentAction = dialogView.findViewById(adapter.itemMenuActions[i]);
                currentAction.setOnTouchListener(adapter.onTouchItemMenuActionListener);
                currentAction.setTag(this.item);
            }

            return dialogView;
        }

    }

}
