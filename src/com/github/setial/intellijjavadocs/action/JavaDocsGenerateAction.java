package com.github.setial.intellijjavadocs.action;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * The type Java docs generate action.
 *
 * @author Sergey Timofiychuk
 */
public class JavaDocsGenerateAction extends JavaDocGenerateAction implements DumbAware {

    /**
     * Instantiates a new Java docs generate action.
     */
    public JavaDocsGenerateAction() {
        super(new JavaDocHandler());
    }

    /**
     * Action performed.
     *
     * @param e the Event
     */
    @Override
    public void actionPerformed(AnActionEvent e) {
        final PsiFile file = DataKeys.PSI_FILE.getData(e.getDataContext());
        DataContext dataContext = e.getDataContext();

        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

        if (editor != null && file != null) {
            processFile(file);
        } else if (project != null && files != null) {
            processFiles(files, project);
        } else {
            // TODO show message
        }

    }

    private void processFiles(VirtualFile[] files, Project project) {
        for (VirtualFile virtualFile : files) {
            if (virtualFile.isDirectory()) {
                processFiles(virtualFile.getChildren(), project);
            } else {
                PsiFile file = convertToPsiFile(virtualFile, project);
                processFile(file);
            }
        }
    }

    private PsiFile convertToPsiFile(VirtualFile file, Project project) {
        PsiManager manager = PsiManager.getInstance(project);
        return manager.findFile(file);
    }

    private void processFile(PsiFile file) {
        List<PsiElement> elements = new LinkedList<PsiElement>();
        // Find all class elements
        List<PsiClass> classElements = getClasses(file);
        elements.addAll(classElements);
        for (PsiClass classElement : classElements) {
            elements.addAll(PsiTreeUtil.getChildrenOfTypeAsList(classElement, PsiMethod.class));
            elements.addAll(PsiTreeUtil.getChildrenOfTypeAsList(classElement, PsiField.class));
        }
        for (PsiElement element : elements) {
            processElement(element);
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(false);
        DataContext dataContext = event.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null){
            presentation.setEnabled(false);
            return;
        }
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

        final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file != null && JavaFileType.INSTANCE.equals(file.getFileType())) {
                presentation.setEnabled(true);
                return;
            } else if (file != null && file.isDirectory()) {
                presentation.setEnabled(true);
                return;
            }
        } if (files!= null && containsJavaFiles(files)) {
            presentation.setEnabled(true);
            return;
        }
    }

    private boolean containsJavaFiles(VirtualFile[] files) {
        if (files == null) {
            return false;
        }
        if (files.length < 1) {
            return false;
        }
        boolean result = false;
        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                result = result || containsJavaFiles(file.getChildren());
            } else if (JavaFileType.INSTANCE.equals(file.getFileType())) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Gets the classes.
     *
     * @param element the Element
     * @return the Classes
     */
    private List<PsiClass> getClasses(PsiElement element) {
        List<PsiClass> elements = new LinkedList<PsiClass>();
        List<PsiClass> classElements = PsiTreeUtil.getChildrenOfTypeAsList(element, PsiClass.class);
        elements.addAll(classElements);
        for (PsiClass classElement : classElements) {
            elements.addAll(getClasses(classElement));
        }
        return elements;
    }

}
