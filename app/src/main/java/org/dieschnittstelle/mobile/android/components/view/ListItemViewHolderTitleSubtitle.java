package org.dieschnittstelle.mobile.android.components.view;

import android.view.View;
import android.widget.TextView;

import org.dieschnittstelle.mobile.android.apps.contenttagger.R;
import org.dieschnittstelle.mobile.android.components.controller.EntityListAdapter;

/**
 * Created by master on 15.03.16.
 *
 * can be used for view with title or wih title and subtitle (subtitle is optional)
 */
public class ListItemViewHolderTitleSubtitle extends EntityListAdapter.EntityViewHolder {

    public TextView title;

    public TextView subtitle;

    public ListItemViewHolderTitleSubtitle(final View itemView, EntityListAdapter adapter) {
        super(itemView,adapter);
        title = (TextView)itemView.findViewById(R.id.title);
        subtitle = (TextView)itemView.findViewById(R.id.subtitle);
    }

}
