package com.google.appinventor.components.runtime;

public class AndroidNonvisibleComponent implements Component {
    protected final Form form;

    protected AndroidNonvisibleComponent(Form form) {
        this.form = form;
    }

    public Form getForm() {
        return form;
    }

    public void onDelete() {
        // Lifecycle hook for extensions; noop in stub.
    }
}
