package org.dieschnittstelle.mobile.android.components.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by master on 15.03.16.
 *
 * controls a reusable alert dialog, combines controller and view holder functionality (with public attributes for the elements)
 * one may specifiy an object type to the instances of which a dialog may be bound
 */
public abstract class CustomDialogController<T> {

    /*
     * the dialog object
     */
    private AlertDialog dialog;

    /*
     * the standard controls which might be used by a layout - this
     */
    public TextView title;
    public TextView message;
    public EditText input;
    public Button primaryButton;
    public Button secondaryButton;

    /*
     * the underlying acivity
     */
    protected Activity controller;

    /*
     * the data on which the dialog is running
     */
    protected T data;

    /*
     * specify whether we have already been shown
     */
    protected boolean shown;

    /*
     * the app package relative to which the resource ids will be resolved
     */
    private String appPackage;

    public CustomDialogController(Activity controller,int layoutid) {
        this.controller = controller;
        this.appPackage = controller.getApplicationContext().getPackageName();

        // inflate the layout
        View view = controller.getLayoutInflater().inflate(layoutid,null);
        // instantiate the holder attributes
        onCreateViewHolder(view);

        // pass the layout to the builder
        AlertDialog.Builder builder = new AlertDialog.Builder(controller);
        builder.setView(view);

        // create the object
        this.dialog = builder.create();
    }

    /*
     * this might be overridden by subclasses
     */
    protected void onCreateViewHolder(View view) {
        title = (TextView)view.findViewById(controller.getResources().getIdentifier("title","id",appPackage));
        message = (TextView)view.findViewById(controller.getResources().getIdentifier("message","id",appPackage));
        input = (EditText)view.findViewById(controller.getResources().getIdentifier("input","id",appPackage));
        primaryButton = (Button)view.findViewById(controller.getResources().getIdentifier("button_primary","id",appPackage));
        secondaryButton = (Button)view.findViewById(controller.getResources().getIdentifier("button_secondary","id",appPackage));
    }

    /*
     * this needs to be overridden by subclasses - binding will always be called on show, but we may specify whether the view has already been bound
     */
    protected abstract void onBindViewHolder(boolean bound);

    /*
     * show the dialog passing data
     */
    public void show(T data) {
        this.data = data;
        // bind the view holder to the data
        onBindViewHolder(shown);
        this.shown = true;
        this.dialog.show();
    }

    /*
     * hide the dialog
     */
    public void hide() {
        this.dialog.hide();
    }

}
