package mx.itesm.web2mexadl.plugin.popup.actions;

import java.io.File;

import mx.itesm.arch.mvc.MvcAnalyzer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.PluginAction;

/**
 * 
 * @author jccastrejon
 * 
 */
@SuppressWarnings("restriction")
public class MvcAction implements IObjectActionDelegate, Runnable {

    /**
     * Window that manages this action.
     */
    private Shell shell;

    /**
     * Reference to the current selected resource.
     */
    private IResource resource;

    /**
     * Flag that indicates whether or not the external dependencies are shown in
     * the output results.
     */
    private final static boolean INCLUDE_EXTERNAL_DEPENDENCIES = false;

    /**
     * Constructor for Action1.
     */
    public MvcAction() {
        super();
    }

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        this.resource = (((IResource) ((IStructuredSelection) ((PluginAction) action).getSelection()).getFirstElement()));
        new Thread(this).start();
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void run() {
        File imageFile;
        File resourceFile;
        String exceptionMessage;
        boolean classificationCompleted;

        exceptionMessage = null;
        classificationCompleted = false;
        if (this.resource.getLocation() != null) {
            resourceFile = new File(this.resource.getLocation().toOSString());
            imageFile = new File(resourceFile.getParentFile().getAbsolutePath() + "/" + this.resource.getName()
                    + ".svg");
            // Classify
            try {
                if (this.resource instanceof IProject) {
                    // Only generated classes in the bin directory
                    MvcAnalyzer.classifyClassesInDirectory(new File(resourceFile, "bin"),
                            MvcAction.INCLUDE_EXTERNAL_DEPENDENCIES, imageFile);
                } else if (this.resource instanceof IFile) {
                    MvcAnalyzer.classifyClassesinWar(resourceFile, MvcAction.INCLUDE_EXTERNAL_DEPENDENCIES, imageFile);
                }
            } catch (Exception e) {
                exceptionMessage = e.getMessage();
                classificationCompleted = false;
            }

            // Successful execution
            classificationCompleted = true;
        }

        // Display result of execution to the user
        final boolean classificationCompletedParam = classificationCompleted;
        final String exceptionMessageParam = exceptionMessage;
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                if (!classificationCompletedParam) {
                    MessageDialog
                            .openInformation(shell, "MVC Classifier",
                                    "An error occurred while generating the architecture description: "
                                            + exceptionMessageParam);
                } else {
                    this.refreshWorkspace();
                    MessageDialog.openInformation(shell, "Web2MexADL", "MVC architecture successfully generated!");
                }
            }

            /**
             * 
             */
            private void refreshWorkspace() {
                IProject[] projects;

                projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                for (IProject project : projects) {
                    try {
                        project.refreshLocal(IProject.DEPTH_INFINITE, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}
