package org.dieschnittstelle.mobile.android.components.view;

import android.databinding.ViewDataBinding;
import android.view.View;
import android.widget.TextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;

/**
 * Created by master on 15.03.16.
 *
 * can be used for view with title or wih title and subtitle (subtitle is optional)
 */
public class EntityViewHolderWithBinding extends EntityListAdapter.EntityViewHolder {

    public ViewDataBinding binding;

    public static final int VAR_ENTITY = 0;

    public EntityViewHolderWithBinding(final View itemView, EntityListAdapter adapter) {
        super(itemView,adapter);
    }

    public ViewDataBinding getBinding() {
        return binding;
    }

    public void setBinding(ViewDataBinding binding) {
        this.binding = binding;
    }
}
