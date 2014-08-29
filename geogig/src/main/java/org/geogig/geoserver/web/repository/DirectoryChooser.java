/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.browser.FileBreadcrumbs;
import org.geoserver.web.wicket.browser.FileProvider;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

import com.google.common.collect.Lists;

/**
 * A geogig directory file chooser compoenent
 * <p>
 * Adapted from {@link GeoServerFileChooser}
 */
public class DirectoryChooser extends Panel {

    private static final long serialVersionUID = -5587014542924822012L;

    private final IModel<DirectoryFilter> fileFilter = new Model<DirectoryFilter>(
            new DirectoryFilter());

    static File USER_HOME = null;
    static {
        // try to safely determine the user home location
        try {
            File hf = null;
            String home = System.getProperty("user.home");
            if (home != null) {
                hf = new File(home);
            }
            if (hf != null && hf.exists()) {
                USER_HOME = hf;
            }
        } catch (Throwable t) {
            // that's ok, we might not be able to get the user home
        }
    }

    private FileBreadcrumbs breadcrumbs;

    private DirectoryDataView directoryListingTable;

    private IModel<File> directory;

    private final boolean makeRepositoriesSelectable;

    private AjaxLink accepdDirectoryLink;

    public DirectoryChooser(String contentId, IModel<File> directory) {
        this(contentId, directory, true);
    }

    public DirectoryChooser(final String contentId, IModel<File> initialDirectory,
            final boolean makeRepositoriesSelectable) {
        super(contentId, initialDirectory);

        this.directory = initialDirectory;
        this.makeRepositoriesSelectable = makeRepositoriesSelectable;

        // build the roots
        ArrayList<File> roots = Lists.newArrayList(File.listRoots());
        Collections.sort(roots);

        // TODO: find a better way to deal with the data dir
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File dataDirectory = loader.getBaseDirectory();

        roots.add(0, dataDirectory);

        // add the home directory as well if it was possible to determine it at all
        if (USER_HOME != null) {
            roots.add(1, USER_HOME);
        }

        // find under which root the selection should be placed
        File selection = initialDirectory.getObject();

        // first check if the file is a relative reference into the data dir
        if (selection != null) {
            File relativeToDataDir = loader.url(selection.getPath());
            if (relativeToDataDir != null) {
                selection = relativeToDataDir;
            }
        }

        // select the proper root
        File selectionRoot = null;
        if (selection != null && selection.exists()) {
            for (File root : roots) {
                if (isSubfile(root, selection.getAbsoluteFile())) {
                    selectionRoot = root;
                    break;
                }
            }

            // if the file is not part of the known search paths, give up
            // and switch back to the data directory
            if (selectionRoot == null) {
                selectionRoot = dataDirectory;
                initialDirectory = new Model<File>(selectionRoot);
            } else {
                if (!selection.isDirectory()) {
                    initialDirectory = new Model<File>(selection.getParentFile());
                } else {
                    initialDirectory = new Model<File>(selection);
                }
            }
        } else {
            selectionRoot = dataDirectory;
            initialDirectory = new Model<File>(selectionRoot);
        }
        this.directory = initialDirectory;
        setDefaultModel(initialDirectory);

        // the root chooser
        final DropDownChoice<File> choice = new DropDownChoice<File>("roots", new Model<File>(
                selectionRoot), new Model<ArrayList<File>>(roots), new FileRootsRenderer());
        choice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = -1113141016446727615L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                File selection = (File) choice.getModelObject();
                breadcrumbs.setRootFile(selection);
                updateFileBrowser(selection, target);
            }

        });
        choice.setOutputMarkupId(true);
        add(choice);

        // the breadcrumbs
        breadcrumbs = new FileBreadcrumbs("breadcrumbs", new Model<File>(selectionRoot),
                initialDirectory) {
            private static final long serialVersionUID = 3637173832581301482L;

            @Override
            protected void pathItemClicked(File file, AjaxRequestTarget target) {
                updateFileBrowser(file, target);
            }

        };
        breadcrumbs.setOutputMarkupId(true);
        add(breadcrumbs);

        // the file tables
        directoryListingTable = new DirectoryDataView("fileTable", new FileProvider(
                initialDirectory), this.makeRepositoriesSelectable) {

            private static final long serialVersionUID = -1559299096797421815L;

            @Override
            protected void linkNameClicked(File file, AjaxRequestTarget target) {
                updateFileBrowser(file, target);
            }

        };
        directoryListingTable.setOutputMarkupId(true);
        directoryListingTable.setFileFilter(fileFilter);
        add(directoryListingTable);

        accepdDirectoryLink = new AjaxLink<File>("ok", DirectoryChooser.this.directory) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // must of been set by #directoryClicked()
                File dir = getModelObject();
                directorySelected(dir, target);
            }
        };
        add(accepdDirectoryLink);
        accepdDirectoryLink.setVisible(!this.makeRepositoriesSelectable);
    }

    void updateFileBrowser(File file, AjaxRequestTarget target) {
        if (RepositoryManager.isGeogigDirectory(file)) {
            geogigDirectoryClicked(file, target);
        } else {
            directoryClicked(file, target);
        }
    }

    /**
     * Called when a file name is clicked. By default it does nothing
     */
    protected void geogigDirectoryClicked(File file, AjaxRequestTarget target) {
        // do nothing, subclasses will override
    }

    /**
     * Action undertaken as a directory is clicked. Default behavior is to drill down into the
     * directory.
     * 
     * @param file
     * @param target
     */
    protected void directoryClicked(File file, AjaxRequestTarget target) {
        // explicitly change the root model, inform the other components the model has changed
        DirectoryChooser.this.directory.setObject(file);
        directoryListingTable.setDirectory(new Model<File>(file));
        breadcrumbs.setSelection(file);

        target.addComponent(directoryListingTable);
        target.addComponent(breadcrumbs);
    }

    protected void directorySelected(File file, AjaxRequestTarget target) {
        // to be overriden
    }

    private boolean isSubfile(File root, File selection) {
        if (selection == null || "".equals(selection.getPath()))
            return false;
        if (selection.equals(root))
            return true;

        return isSubfile(root, selection.getParentFile());
    }

    /**
     * Set the file table fixed height. Set it to null if you don't want fixed height with overflow,
     * and to a valid CSS measure if you want it instead. Default value is "25em"
     * 
     * @param height
     */
    public void setFileTableHeight(String height) {
        directoryListingTable.setTableHeight(height);
    }

    class FileRootsRenderer implements IChoiceRenderer<File> {
        private static final long serialVersionUID = -5804668199121599078L;

        public Object getDisplayValue(File f) {
            if (f == USER_HOME) {
                return new ParamResourceModel("userHome", DirectoryChooser.this).getString();
            } else {
                GeoServerResourceLoader loader = GeoServerExtensions
                        .bean(GeoServerResourceLoader.class);

                if (f.equals(loader.getBaseDirectory())) {
                    return new ParamResourceModel("dataDirectory", DirectoryChooser.this)
                            .getString();
                }
            }

            try {
                final String displayName = FileSystemView.getFileSystemView().getSystemDisplayName(
                        f);
                if (displayName != null && displayName.length() > 0) {
                    return displayName;
                }
                return FilenameUtils.getPrefix(f.getAbsolutePath());
            } catch (Exception e) {
                // on windows we can get the occasional NPE due to
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6973685
            }
            return f.getName();
        }

        public String getIdValue(File f, int count) {
            return "" + count;
        }
    }

    private static final class DirectoryFilter implements FileFilter, Serializable {
        private static final long serialVersionUID = -2280505390702552L;

        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }
}
