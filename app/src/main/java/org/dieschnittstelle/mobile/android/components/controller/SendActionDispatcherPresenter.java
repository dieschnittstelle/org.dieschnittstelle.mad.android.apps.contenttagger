package org.dieschnittstelle.mobile.android.components.controller;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by master on 30.03.16.
 */
public interface SendActionDispatcherPresenter {

    public void showView(Class<? extends Fragment> viewclass, Bundle args, boolean addToBackstack);

}
