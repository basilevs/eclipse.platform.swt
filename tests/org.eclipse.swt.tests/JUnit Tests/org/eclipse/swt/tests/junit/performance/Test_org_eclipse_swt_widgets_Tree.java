/*******************************************************************************
 * Copyright (c) 2023, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vasili Gulevich - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.tests.junit.performance;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.tests.junit.SwtTestUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class Test_org_eclipse_swt_widgets_Tree {
	enum Shape {
		STAR {
			@Override
			Tree buildTree(Composite parent, int size, Consumer<TreeItem> initializeItem, boolean virtual) {
				Tree tree = new Tree(parent, virtual ? SWT.VIRTUAL : SWT.NONE);
				if (virtual) {
					tree.addListener(SWT.SetData, event -> {
						TreeItem item = (TreeItem) event.item;
						initializeItem.accept(item);
						if (item.getParentItem() == null) item.setItemCount(size - 1);
					});
					tree.setItemCount(1);
				} else {
					tree.setItemCount(1);
					TreeItem root = tree.getItem(0);
					initializeItem.accept(root);

					root.setItemCount(size - 1);
					for (TreeItem item: root.getItems()) {
						initializeItem.accept(item);
					}
				}
				return tree;
			}

			@Override
			protected TreeItem lastItem(Tree tree) {
				TreeItem root = tree.getItem(0);
				return root.getItem(tree.getItemCount() - 1);
			}
		};

		abstract Tree buildTree(Composite parent, int size, Consumer<TreeItem> intializeItem, boolean virtual);

		protected abstract TreeItem lastItem(Tree tree);
	}

	@Rule
	public final TestName name = new TestName();
	private final boolean virtual;
	private final Shape shape;
	private final Shell shell = new Shell();
	private final Font font = new Font(shell.getDisplay(), "Arial", 5, 5);
	private final Color foreground = shell.getDisplay().getSystemColor(SWT.COLOR_GREEN);
	private final Color background = shell.getDisplay().getSystemColor(SWT.COLOR_BLACK);

    @Parameters(name = "Shape: {0}, virtual: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
        	{ Shape.STAR, false },
        	{ Shape.STAR, true},
        });
    }

	public Test_org_eclipse_swt_widgets_Tree(Shape shape, boolean virtual) {
		this.shape =  Objects.requireNonNull(shape);
		this.virtual = virtual;
	}

	@Before
	public void setUp() {
		shell.setSize(500, 500);
		shell.setLayout(new FillLayout());
		// Make tree visible to make GTK request updates
		SwtTestUtil.openShell(shell);
	}

	@After
	public void teardown() {
		font.dispose();
		shell.dispose();
	}

	@Test
	public void build() {
		assertMaximumDegree(1.3, n -> {
			return measureNanos(() -> buildSubject(n, this::initializeItem).requestLayout());
		});
	}

	@Test
	public void reveal() {
		assertMaximumDegree(1.3, n -> {
			Tree tree = buildSubject(n, this::initializeItem);
			return measureNanos(() -> tree.showItem(shape.lastItem(tree)));
		});
	}

	private Tree buildSubject(int size, Consumer<TreeItem> initialize) {
		Tree result = shape.buildTree(shell, size, initialize, virtual);
		return result;
	}

	/** Ensure that given function grows within acceptable polynomial degree */
	private void assertMaximumDegree(double maximumDegree, IntFunction<Double> function) {
		clearShell();
		int elementCount[] = new int[] { 10000, 100000 };
		function.apply(elementCount[0]); // warmup
		clearShell();
		double elapsed[] = new double[] { function.apply(elementCount[0]), 0 };
		clearShell();
		elapsed[1] = function.apply(elementCount[1]);
		double ratio = elapsed[1] / elementCount[1] / elapsed[0] * elementCount[0];
		double degree = Math.log(elapsed[1] / elapsed[0]) / Math.log(elementCount[1] / elementCount[0]);
		String error = String.format(
				"Execution time should grow as %f polynom. \nTime for %d elements: %f ns\nTime for %d elements: %f ns\nRatio: %f\nGrade: %f\n",
				maximumDegree, elementCount[0], elapsed[0], elementCount[1], elapsed[1], ratio, degree);
		System.out.println(name.getMethodName() + "\n" + error);
		assertTrue(error, (elapsed[1] <= 100 && elapsed[0] <= 100) || degree < maximumDegree);
	}

	private double measureNanos(Runnable runnable) {
		SwtTestUtil.processEvents();
		long start = System.nanoTime();
		runnable.run();
		SwtTestUtil.processEvents();
		long stop = System.nanoTime();
		return stop - start;
	}

	private final AtomicLong itemCount = new AtomicLong(0);
	private void initializeItem(TreeItem item) {
		item.setText(itemCount.getAndIncrement() + "");
		item.setForeground(foreground);
		item.setBackground(background);
		item.setFont(font);
	}

	private void clearShell() {
		for (Control child: shell.getChildren()) {
			child.dispose();
		}
		assert shell.getChildren().length == 0;
		itemCount.set(0);
	}

}
