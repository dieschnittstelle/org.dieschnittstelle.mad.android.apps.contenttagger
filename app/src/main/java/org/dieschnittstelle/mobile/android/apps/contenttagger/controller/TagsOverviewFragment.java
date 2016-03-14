package org.dieschnittstelle.mobile.android.apps.contenttagger.controller;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.apps.contenttagger.model.Tag;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;

/**
 * Created by master on 12.03.16.
 */
public class TagsOverviewFragment extends Fragment {

    /*
     * the adapter for the listview
     */
    private EntityListAdapter<Tag,TagViewHolder> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // declare that we use an options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // the view
        View contentView = inflater.inflate(R.layout.tags_overview_contentview, null);

        // create an adapter for the recycler view
        this.adapter = new EntityListAdapter<Tag,TagViewHolder>(this.getActivity(), (RecyclerView) contentView.findViewById(R.id.listview), R.layout.tags_overview_itemview, R.layout.tags_overview_itemmenu, new int[]{R.id.action_delete, R.id.action_edit}) {

            @Override
            public TagViewHolder onCreateEntityViewHolder(View view, EntityListAdapter adapter) {
                return new TagViewHolder(view,adapter);
            }

            @Override
            public void onBindEntityViewHolder(TagViewHolder holder, Tag entity, int position) {
                holder.name.setText(entity.getName() + "-" + entity.getId());
            }

            @Override
            protected void onSelectEntity(Tag entity) {
                Log.i(logger, "onSelectEntity(): " + entity);
            }

            @Override
            protected void onSelectEntityMenuAction(int action, Tag entity) {
                Log.i(logger, "onSelectEntityMenuAction(): " + action + "@" + entity);
                if (action == R.id.action_delete) {
                    deleteTag(entity);
                }
            }

            @Override
            public void onBindEntityMenuDialog(EntityListAdapter.ItemMenuDialogViewHolder holder, Tag item) {
                ((TextView)holder.heading).setText(item.getName());
            }
        };

        return contentView;
    }

    // we start populating the view onresumt
    @Override
    public void onResume() {
        super.onResume();
        readAllTags();
    }

    private class TagViewHolder extends EntityListAdapter.EntityViewHolder {

        public TextView name;

        public TagViewHolder(final View itemView, EntityListAdapter adapter) {
            super(itemView,adapter);
            name = (TextView)itemView.findViewById(R.id.name);
        }

    }

    /*
     * usage of crud operations
     */
    public void readAllTags() {

        new AsyncTask<Void, Void, List<Tag>>() {

            @Override
            protected List<Tag> doInBackground(Void... params) {
                return (List<Tag>)Tag.readAll(Tag.class);
            }

            @Override
            protected void onPostExecute(List<Tag> tags) {
                adapter.addItems(tags);
            }

        }.execute();

    }

    public void createTag(Tag tag) {
        new AsyncTask<Tag, Void, Tag>() {

            @Override
            protected Tag doInBackground(Tag... params) {
                params[0].create();
                return params[0];
            }

            @Override
            protected void onPostExecute(Tag tag) {
                adapter.addItem(tag);
            }

        }.execute(tag);
    }

    public void deleteTag(Tag tag) {
        new AsyncTask<Tag,Void,Tag>() {

            @Override
            protected Tag doInBackground(Tag... params) {
                params[0].delete();
                return params[0];
            }

            @Override
            protected void onPostExecute(Tag tag) {
                adapter.removeItem(tag);
            }

        }.execute(tag);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_tags_overview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add) {
            createTag(new Tag("new"));
        }

        return super.onOptionsItemSelected(item);
    }



}
