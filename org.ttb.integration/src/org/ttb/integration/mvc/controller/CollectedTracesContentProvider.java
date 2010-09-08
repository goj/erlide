package org.ttb.integration.mvc.controller;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.ttb.integration.mvc.model.CollectedDataList;
import org.ttb.integration.mvc.model.treenodes.ITreeNode;

/**
 * Content provider for tree view of collected traces.
 * 
 * @author Piotr Dorobisz
 * 
 */
public class CollectedTracesContentProvider implements /* ILazyTreeContentProvider */ITreeContentProvider {

    private final TreeViewer treeViewer;
    private CollectedDataList dataList;

    public CollectedTracesContentProvider(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        dataList = (CollectedDataList) newInput;
    }

    public Object[] getChildren(Object element) {
        return ((ITreeNode) element).getChildren().toArray();
    }

    public Object[] getElements(Object element) {
        return dataList.getData().toArray();
    }

    public Object getParent(Object element) {
        return ((ITreeNode) element).getParent();
    }

    public boolean hasChildren(Object element) {
        // return ((ITreeNode) element).hasChildren();
        // for optimization purposes always return true
        return true;
    }

    public void updateElement(Object parent, int index) {
        ITreeNode childElement;
        if (parent instanceof CollectedDataList) {
            childElement = CollectedDataList.getInstance().get(index);
        } else {
            childElement = ((ITreeNode) parent).getChildren().get(index);
        }
        treeViewer.replace(parent, index, childElement);
        // treeViewer.setChildCount(childElement,
        // childElement.getChildren().size());
        treeViewer.setHasChildren(childElement, true);
    }

    public void updateChildCount(Object element, int currentChildCount) {
        int newChildCount;
        if (element instanceof CollectedDataList) {
            newChildCount = CollectedDataList.getInstance().size();
        } else {
            newChildCount = ((ITreeNode) element).getChildren().size();
        }
        if (currentChildCount != newChildCount)
            treeViewer.setChildCount(element, newChildCount);
    }
}