package org.opengeo.data.importer.rest;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.restlet.data.Status;

public abstract class BaseResource extends AbstractResource {

    protected Importer importer;

    protected BaseResource(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext context() {
        return context(false);
    }

    protected ImportContext context(boolean optional) {
        long i = Long.parseLong(getAttribute("import"));

        ImportContext context = importer.getContext(i);
        if (!optional && context == null) {
            throw new RestletException("No such import: " + i, Status.CLIENT_ERROR_NOT_FOUND);
        }
        return context;
    }

    protected ImportTask task() {
        return task(false);
    }

    protected ImportTask task(boolean optional) {
        ImportContext context = context();
        ImportTask task = null;

        String t = getAttribute("task");
        if (t != null) {
            int id = Integer.parseInt(t);
            if (id < context.getTasks().size()) {
                task = context.getTasks().get(id);
            }
        }

        if (task == null && !optional) {
            throw new RestletException("No such task: " + t + " for import: " + context.getId(),
                Status.CLIENT_ERROR_NOT_FOUND);
        }

        return task;
    }

    protected ImportItem item() {
        return item(false);
    }

    protected ImportItem item(boolean optional) {
        ImportTask task = task();
        ImportItem item = null;

        String i = getAttribute("item");
        
        if (i != null) {
            int id = Integer.parseInt(i);
            if (id < task.getItems().size()) {
                item = task.getItems().get(id);
            }
        }

        if (item == null && !optional) {
            throw new RestletException("No item specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }

        return item;
    }
}
