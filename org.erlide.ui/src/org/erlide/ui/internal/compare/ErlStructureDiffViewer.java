package org.erlide.ui.internal.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.StructureDiffViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.widgets.Composite;

public class ErlStructureDiffViewer extends StructureDiffViewer {

	private final ErlStructureCreator fStructureCreator;

	public ErlStructureDiffViewer(final Composite parent,
			final CompareConfiguration config) {
		super(parent, config);
		fStructureCreator = new ErlStructureCreator();
		setStructureCreator(fStructureCreator);
	}

	@Override
	public ViewerComparator getComparator() {
		return new ViewerComparator() {
			@Override
			public int compare(final Viewer viewer, final Object e1,
					final Object e2) {
				final ErlNode en1 = getErlNode(e1);
				final ErlNode en2 = getErlNode(e2);
				if (en1 != null && en2 != null) {
					return en1.getRange().getOffset()
							- en2.getRange().getOffset();
				}
				return super.compare(viewer, e1, e2);
			}

			private ErlNode getErlNode(final Object e) {
				if (e instanceof DiffNode) {
					final DiffNode d = (DiffNode) e;
					final ITypedElement left = d.getLeft();
					if (left instanceof ErlNode) {
						return (ErlNode) left;
					}
					final ITypedElement right = d.getRight();
					if (right instanceof ErlNode) {
						return (ErlNode) right;
					}
				}
				return null;
			}
		};
	}
}
