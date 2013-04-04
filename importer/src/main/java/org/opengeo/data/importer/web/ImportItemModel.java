package org.opengeo.data.importer.web;

import org.apache.wicket.model.LoadableDetachableModel;
import org.opengeo.data.importer.ImportTask;

public class ImportItemModel extends LoadableDetachableModel<ImportTask> {

    long context;
    long task;
    
    public ImportItemModel(ImportTask task) {
        this(task.getContext().getId(), task.getId());
    }

    public ImportItemModel(long context, long task) {
        this.context = context;
        this.task = task;
    }

    @Override
    protected ImportTask load() {
        return ImporterWebUtils.importer().getContext(context).task(task);
    }

}
