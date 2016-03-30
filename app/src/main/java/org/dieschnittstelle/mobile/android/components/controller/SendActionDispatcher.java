package org.dieschnittstelle.mobile.android.components.controller;

import android.app.Fragment;
import android.content.Intent;

/**
 * Created by master on 30.03.16.
 */
public interface SendActionDispatcher {

    public void handleSendActionForType(String type, Intent intent, SendActionDispatcherPresenter presenter);

}
