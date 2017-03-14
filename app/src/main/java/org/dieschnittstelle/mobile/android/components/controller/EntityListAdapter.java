package org.dieschnittstelle.mobile.android.components.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.dieschnittstelle.mobile.android.components.model.Entity;

/**
 * Created by master on 12.03.16.
 * also here we have removed any dependencies to application specific classes
 */
public abstract class EntityListAdapter<E extends Entity,H extends EntityListAdapter.EntityViewHolder> extends RecyclerView.Adapter<EntityListAdapter.EntityViewHolder> {

    protected static String logger = "EntityListAdapter";

    private Activity controller;

    private List<E> entities = new ArrayList<E>();
    //private SortedList<?> entities = new SortedList(null,null);
    private int itemLayout;
    public int[] itemMenuActions;
    public int itemMenuLayout;

    // a list of sorting strategies
    private List<List<Comparator<? super E>>> sortingStrategies = new ArrayList<List<Comparator<? super E>>>();

    // we keep the state indicating which sorting strategy we are currently using
    private int currentSortingStrategy = -1;

    public View.OnTouchListener onTouchItemListener = new SimpleOnTouchListener(new SimpleOnSelectListItemListener(this));
    public View.OnTouchListener onTouchItemMenuListener = new SimpleOnTouchListener(new SimpleOnSelectListItemMenuListener(this));
    public View.OnTouchListener onTouchItemMenuActionListener = new SimpleOnTouchListener(new SimpleOnSelectListItemMenuActionListener(this));

    public EntityListAdapter(Activity controller, int itemLayout, int itemMenuLayout, int[] itemMenuMenuActions) {
        this.itemLayout = itemLayout;
        this.controller = controller;
        this.itemMenuLayout = itemMenuLayout;
        this.itemMenuActions = itemMenuMenuActions;
    }

    public Activity getController() {
        return this.controller;
    }

    /*
     * an alternative constructor passing a recycler view for doing default settings
     */
    public EntityListAdapter(Activity controller, RecyclerView recyclerView, int itemLayout, int itemMenuLayout, int[] itemMenuMenuActions) {
        this(controller, itemLayout, itemMenuLayout, itemMenuMenuActions);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(controller));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        // set the adapter on the view
        recyclerView.setAdapter(this);
    }


    @Override
    public EntityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        try {
            EntityViewHolder vh = onCreateEntityViewHolder(v, this);
            vh.setViewAndAdapter(v,this);
            return vh;
        }
        catch (Exception e) {
            String msg = "onCreateViewHolder(): got exception: " + e;
            Log.e(logger,msg,e);
            throw new RuntimeException(msg,e);
        }
    }

    @Override
    public void onBindViewHolder(EntityViewHolder holder, int position) {
        // only show the divider if we have not reached the last list item
        E entity = entities.get(position);
        holder.itemView.setTag(entity);
        holder.itemMenu.setTag(entity);
        onBindEntityViewHolder((H)holder,entity,position);
    }

    public abstract H onCreateEntityViewHolder(View view,EntityListAdapter adapter);

    public abstract void onBindEntityViewHolder(H holder,E entity,int position);


    @Override
    public int getItemCount() {
        return this.entities.size();
    }

    /************************************************************
     * the view holder
     ************************************************************/

    public abstract static class EntityViewHolder extends RecyclerView.ViewHolder {
        public View itemMenu;
        public View divider;

        public EntityViewHolder(final View itemView, EntityListAdapter adapter) {
            super(itemView);
            // assume we have a divider
            this.divider = itemView.findViewById(adapter.getController().getResources().getIdentifier("divider","id",adapter.getController().getApplicationContext().getPackageName())/*R.id.divider*/);
            this.itemMenu = itemView.findViewById(adapter.itemMenuLayout);
        }

        public void setViewAndAdapter(View itemView,EntityListAdapter adapter) {
            this.itemMenu = itemView.findViewById(adapter.getController().getResources().getIdentifier("listitem_menu","id",adapter.getController().getApplicationContext().getPackageName())/*R.id.listitem_menu*/);
            this.itemView.setOnTouchListener(adapter.onTouchItemListener);
            if (this.itemMenu != null) {
                this.itemMenu.setOnTouchListener(adapter.onTouchItemMenuListener);
            }
        }

    }

    /**********************************************************************
     * handling item / item menu / item menu action selection
     **********************************************************************/

    public void onSelectListItem(E entity) {
        Log.i(logger, "onSelectListItem(): " + entity);
        onSelectEntity(entity);
    }

    protected abstract void onSelectEntity(E entity);

    public void onSelectListItemMenu(E entity) {
        Log.i(logger, "onSelectListItemMenu(): " + entity);
        showItemMenuDialog(entity);
    }

    public void onSelectListItemMenuAction(int action, E entity) {
        Log.i(logger, "onSelectListItemMenuAction(): " + action + "@" + entity);
        onSelectEntityMenuAction(action,entity);
        hideItemMenuDialog();
    }

    protected abstract void onSelectEntityMenuAction(int action,E entity);

    /****************************************************************************************************************
     * handling feedback from crud operations - note that these methods do not need to invoked from the ui thread!!!
     ****************************************************************************************************************/

    public void addItem(E item) {
        this.entities.add(item);
        if (this.sortingStrategies.size() > 0) {
            this.sort();
        }
        else {
            this.notifyItemInserted(this.entities.size() - 1);
        }
    }

    public void addItems(List<E> items) {
        int sizeBefore = this.entities.size();
        this.entities.addAll(items);
        // we sort
        if (this.sortingStrategies.size() > 0) {
            this.sort();
        }
        else {
            this.notifyItemRangeInserted(sizeBefore, items.size());
        }
    }

    public void removeItem(E item) {
        int index = this.entities.indexOf(item);
        if (index > -1) {
            this.entities.remove(index);
            this.notifyItemRemoved(index);
        }
    }

    public void clear() {
        this.entities.clear();
    }

    public void updateItem(Entity item) {
        int index = this.entities.indexOf(item);
        if (index > -1) {
            try {
                this.notifyItemChanged(index);
            }
            catch (IllegalStateException ise) {
                Log.w(logger,"got illegal state exception on updateItem(): " + ise,ise);
            }
        }
    }

    /************************************************************
     * sorting
     ************************************************************/

    public void addSortingStrategy(List<Comparator<? super E>> strategy) {
        this.sortingStrategies.add(strategy);
    }

    public void sort() {
        if (currentSortingStrategy == -1) {
            currentSortingStrategy = 0;
        }
        if (sortingStrategies.size() > currentSortingStrategy) {
            sort(sortingStrategies.get(currentSortingStrategy));
        }
    }

    public void sortNext() {
        if (currentSortingStrategy == -1) {
            currentSortingStrategy = 0;
        }
        else if (currentSortingStrategy < (sortingStrategies.size() - 1)) {
            currentSortingStrategy++;
        }
        else {
            currentSortingStrategy = 0;
        }
        sort(sortingStrategies.get(currentSortingStrategy));
    }

    public void sort(List<Comparator<? super E>> comparators) {
        Log.i(logger, "sorting entities using comparators: " + comparators);

        for (Comparator<? super E> comp : comparators) {
            Log.i(logger,"applying " + comp);
            Collections.sort(entities, comp);
        }

        // this is a very brute-force solution as it results in recreating all list item views to be displayed
        this.getController().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EntityListAdapter.this.notifyDataSetChanged();
            }
        });
    }


    /************************************************************
     * listeners for handling interaction
     ************************************************************/

    public class SimpleOnTouchListener implements View.OnTouchListener {

        private View.OnClickListener onClickListener;

        public SimpleOnTouchListener() {

        }

        public SimpleOnTouchListener(View.OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case (MotionEvent.ACTION_DOWN):
                    Log.d(logger, "Action was DOWN");
                    view.setSelected(true);
                    return true;
                case (MotionEvent.ACTION_MOVE):
                    Log.d(logger, "Action was MOVE");
                    view.setSelected(false);
                    return true;
                case (MotionEvent.ACTION_UP):
                    Log.d(logger, "Action was UP");
                    view.setSelected(false);
                    if (this.onClickListener != null) {
                        this.onClickListener.onClick(view);
                    }
                    return true;
                case (MotionEvent.ACTION_CANCEL):
                    Log.d(logger, "Action was CANCEL");
                    view.setSelected(false);
                    return true;
                case (MotionEvent.ACTION_OUTSIDE):
                    view.setSelected(false);
                    Log.d(logger, "Movement occurred outside bounds " +
                            "of current screen element");
                    return true;
                default:
                    return false;
            }
        }
    }

    public class SimpleOnSelectListItemListener implements View.OnClickListener {

        private EntityListAdapter adapter;

        public SimpleOnSelectListItemListener(EntityListAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onClick(View v) {
            adapter.onSelectListItem((Entity) v.getTag());
        }
    }

    public class SimpleOnSelectListItemMenuListener implements View.OnClickListener {

        private EntityListAdapter adapter;

        public SimpleOnSelectListItemMenuListener(EntityListAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onClick(View v) {
            adapter.onSelectListItemMenu((Entity) v.getTag());
        }
    }

    public class SimpleOnSelectListItemMenuActionListener implements View.OnClickListener {

        private EntityListAdapter adapter;

        public SimpleOnSelectListItemMenuActionListener(EntityListAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onClick(View v) {
            adapter.onSelectListItemMenuAction(v.getId(), (Entity) v.getTag());
        }
    }

    /**********************************************************************
     * handling item menu dialog
     **********************************************************************/

    private boolean dialogAsFragment = false;

    private Dialog itemMenuDialog;
    private View itemMenuDialogView;

    private void showItemMenuDialog(Entity item) {
        if (this.dialogAsFragment) {
            // in contrast to the example we should not need to consider the case that there is already an existing instance of the dialog...
            FragmentTransaction ft = controller.getFragmentManager().beginTransaction();
            Fragment prev = controller.getFragmentManager().findFragmentByTag("menuItemDialog");
            if (prev != null) {
                ft.remove(prev);
            }

            // Create and show the dialog.
            DialogFragment dialog = newItemMenuDialogInstance(item, this);
            dialog.show(ft, "menuItemDialog");
        } else {
            if (this.itemMenuDialog == null) {
                this.itemMenuDialog = createItemMenuDialog();
            }
            if (this.itemMenuDialog.isShowing()) {
                this.itemMenuDialog.hide();
            }
            ((ItemMenuDialogViewHolder) this.itemMenuDialogView.findViewById(controller.getResources().getIdentifier("dialogRoot","id", getController().getApplicationContext().getPackageName()) /*R.id.dialogRoot*/).getTag()).setItem(item);

            this.itemMenuDialog.show();
        }
    }

    private void hideItemMenuDialog() {
        if (this.dialogAsFragment) {
            FragmentTransaction ft = controller.getFragmentManager().beginTransaction();
            Fragment prev = controller.getFragmentManager().findFragmentByTag("menuItemDialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.commit();
        } else {
            if (this.itemMenuDialog.isShowing()) {
                this.itemMenuDialog.hide();
            }
        }
    }

    /*
     * alternative for creating a dialog as a singleton
     */
    public Dialog createItemMenuDialog() {
        Log.i(logger, "createMenuItemDialog()");

        AlertDialog.Builder builder = new AlertDialog.Builder(controller);
        // Get the layout inflater
        LayoutInflater inflater = controller.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View dialogView = inflater.inflate(itemMenuLayout, null);
        // we create a view holder
        dialogView.findViewById(getController().getResources().getIdentifier("dialogRoot","id", getController().getApplicationContext().getPackageName())/*R.id.dialogRoot*/).setTag(new ItemMenuDialogViewHolder(dialogView, this));

        builder.setView(dialogView);
        // we set the view as an instance variable as findViewById() does not work on the dialog
        this.itemMenuDialogView = dialogView;

        return builder.create();
    }

    /*
     * callback for binding an entity to an itemMenu
     */
    public abstract void onBindEntityMenuDialog(ItemMenuDialogViewHolder holder,E item);

    /*
     * a EntityViewHolder for the itemmenu dialog that allows us to easily update the dialog for new items
     */
    public static class ItemMenuDialogViewHolder {

        public View heading;
        public List<View> actions = new ArrayList<View>();
        public EntityListAdapter adapter;

        public ItemMenuDialogViewHolder(View dialogView,EntityListAdapter adapter) {
            this.adapter = adapter;
            this.heading = (View) dialogView.findViewById(adapter.getController().getResources().getIdentifier("heading","id",adapter.getController().getApplicationContext().getPackageName())/*R.id.heading*/);
            for (int i = 0; i < adapter.itemMenuActions.length; i++) {
                View currentAction = dialogView.findViewById(adapter.itemMenuActions[i]);
                currentAction.setOnTouchListener(adapter.onTouchItemMenuActionListener);
                this.actions.add(currentAction);
            }
        }

        public void setItem(Entity item) {
            for (View view : this.actions) {
                view.setTag(item);
            }

            adapter.onBindEntityMenuDialog(this, item);
        }
    }

    /*
 * a dialog for selecting actions for a list item, following http://developer.android.com/reference/android/app/DialogFragment.html
 * alternative solution using a dialogfragment
 */
    private ListItemMenuDialogFragment newItemMenuDialogInstance(Entity item, EntityListAdapter adapter) {
        ListItemMenuDialogFragment instance = new ListItemMenuDialogFragment();
        instance.setItem(item);
        instance.setAdapter(adapter);

//            // take over argument passing via bundle rather than setting the object directly...
//            Bundle args = new Bundle();
//            args.putSerializable("item", item);
//            instance.setArguments(args);

        return instance;
    }


    public static class ListItemMenuDialogFragment extends DialogFragment {

        private String logger = "ListItemMenuDialogFragment";

        private Entity item;

        private EntityListAdapter adapter;

        public void setItem(Entity item) {
            this.item = item;
        }

        public void setAdapter(EntityListAdapter adapter) {
            this.adapter = adapter;
        }

        public void onCreate(Bundle savedInstanceState) {
            Log.i(logger, "onCreate()");
            super.onCreate(savedInstanceState);
//            this.item = (Entity) getArguments().getSerializable("item");
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.i(logger, "onCreateView()");

            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            ViewGroup dialogView = (ViewGroup) inflater.inflate(adapter.itemMenuLayout, container, false);
            View heading = (TextView) dialogView.findViewById(adapter.getController().getResources().getIdentifier("heading","id",adapter.getController().getApplicationContext().getPackageName())/*R.id.heading*/);

            this.adapter.onBindEntityMenuDialog(new ItemMenuDialogViewHolder(dialogView, adapter),item);

            // we iterate over the actions and set a listener
            for (int i = 0; i < adapter.itemMenuActions.length; i++) {
                View currentAction = dialogView.findViewById(adapter.itemMenuActions[i]);
                currentAction.setOnTouchListener(adapter.onTouchItemMenuActionListener);
                currentAction.setTag(this.item);
            }

            return dialogView;
        }

    }

}


