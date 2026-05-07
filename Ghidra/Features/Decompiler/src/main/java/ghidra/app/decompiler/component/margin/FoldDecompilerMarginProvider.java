/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.decompiler.component.margin;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigInteger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import docking.widgets.fieldpanel.LayoutModel;
import docking.widgets.fieldpanel.listener.IndexMapper;
import docking.widgets.fieldpanel.listener.LayoutModelListener;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.component.ClangLayoutController;
import ghidra.app.decompiler.component.fold.FoldState;
import ghidra.program.model.listing.Program;

/**
 * Decompiler margin that paints a fold-toggle chevron next to every line that begins a
 * foldable scope. Clicking a chevron toggles the corresponding fold.
 */
public class FoldDecompilerMarginProvider extends JPanel
		implements DecompilerMarginProvider, LayoutModelListener {

	private static final int MARGIN_WIDTH = 12;
	private static final int CHEVRON_SIZE = 8;

	private LayoutPixelIndexMap pixmap;
	private ClangLayoutController controller;

	public FoldDecompilerMarginProvider() {
		setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		setPreferredSize(new Dimension(MARGIN_WIDTH, 0));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				onClick(e);
			}
		});
	}

	@Override
	public void setProgram(Program program, LayoutModel model, LayoutPixelIndexMap pixmap) {
		this.pixmap = pixmap;
		setLayoutModel(model);
		repaint();
	}

	private void setLayoutModel(LayoutModel model) {
		// We need access to FoldState, which only ClangLayoutController exposes. If the
		// model is something else (a unit test stand-in, say), the chevron simply won't
		// paint — no exception, no broken UI.
		ClangLayoutController newController =
			model instanceof ClangLayoutController ? (ClangLayoutController) model : null;
		if (this.controller == newController) {
			// Same model — don't re-register. setProgram is called re-entrantly during
			// modelChanged dispatch (FieldPanel -> layoutsChanged -> setProgram on every
			// margin), and mutating the listener list here while ClangLayoutController is
			// iterating it throws ConcurrentModificationException.
			return;
		}
		if (this.controller != null) {
			this.controller.removeLayoutModelListener(this);
		}
		this.controller = newController;
		if (this.controller != null) {
			this.controller.addLayoutModelListener(this);
		}
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public void setOptions(DecompileOptions options) {
		setFont(options.getDefaultFont());
		repaint();
	}

	@Override
	public void modelSizeChanged(IndexMapper indexMapper) {
		repaint();
	}

	@Override
	public void dataChanged(BigInteger start, BigInteger end) {
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (controller == null || pixmap == null) {
			return;
		}
		FoldState foldState = controller.getFoldState();

		Rectangle visible = getVisibleRect();
		BigInteger startIdx = pixmap.getIndex(visible.y);
		BigInteger endIdx = pixmap.getIndex(visible.y + visible.height);
		VerticalLayoutPixelIndexMap vmap =
			pixmap instanceof VerticalLayoutPixelIndexMap ? (VerticalLayoutPixelIndexMap) pixmap
					: null;

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(getForeground());

			for (BigInteger i = startIdx; i.compareTo(endIdx) <= 0; i = i.add(BigInteger.ONE)) {
				if (vmap != null && !vmap.isVisible(i)) {
					continue;
				}
				int idx = i.intValue();
				if (!foldState.isFoldable(idx)) {
					continue;
				}
				int y = pixmap.getPixel(i);
				paintChevron(g2, y, foldState.isFolded(idx));
			}
		}
		finally {
			g2.dispose();
		}
	}

	/**
	 * Draw a small filled triangle at the given line's y position.
	 * <p>Folded → right-pointing ▶ (collapsed).
	 * <p>Expanded → down-pointing ▼ (expanded; click to collapse).
	 */
	private void paintChevron(Graphics2D g, int lineTop, boolean folded) {
		int rowHeight = guessRowHeight();
		int cx = getWidth() / 2;
		int cy = lineTop + rowHeight / 2;
		int half = CHEVRON_SIZE / 2;

		Polygon p = new Polygon();
		if (folded) {
			// ▶
			p.addPoint(cx - half + 1, cy - half);
			p.addPoint(cx - half + 1, cy + half);
			p.addPoint(cx + half - 1, cy);
		}
		else {
			// ▼
			p.addPoint(cx - half, cy - half + 1);
			p.addPoint(cx + half, cy - half + 1);
			p.addPoint(cx, cy + half - 1);
		}
		g.fillPolygon(p);
	}

	private int guessRowHeight() {
		FontMetrics fm = getFontMetrics(getFont());
		return fm.getHeight();
	}

	private void onClick(MouseEvent e) {
		if (controller == null || pixmap == null) {
			return;
		}
		BigInteger idx = pixmap.getIndex(e.getY());
		if (idx == null) {
			return;
		}
		int anchor = idx.intValue();
		FoldState foldState = controller.getFoldState();
		if (!foldState.isFoldable(anchor)) {
			return;
		}
		if (foldState.isHidden(anchor)) {
			// Anchor is inside a collapsed outer region. Don't act on phantom clicks.
			return;
		}
		controller.toggleFold(anchor);
	}
}
