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
import java.awt.event.*;
import java.math.BigInteger;

import javax.swing.*;

import docking.widgets.fieldpanel.LayoutModel;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.component.ClangLayoutController;
import ghidra.program.model.listing.Program;

/** Clickable scope-folding controls beside the original C line numbers. */
public class FoldingDecompilerMarginProvider extends JPanel implements DecompilerMarginProvider {

	private ClangLayoutController model;
	private LayoutPixelIndexMap pixmap;
	private int iconSize = 10;

	public FoldingDecompilerMarginProvider() {
		setName("Decompiler Scope Folding");
		getAccessibleContext().setAccessibleName("Decompiler Scope Folding");
		setToolTipText("Expand or collapse scope");
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
					int line = getFoldLine(e);
					if (line >= 0) {
						model.toggleFold(line);
					}
				}
			}
		});
	}

	@Override
	public void setProgram(Program program, LayoutModel layout, LayoutPixelIndexMap map) {
		model = (ClangLayoutController) layout;
		pixmap = map;
		repaint();
	}

	@Override
	public void setOptions(DecompileOptions options) {
		setFont(options.getDefaultFont());
		setForeground(options.getDefaultColor());
		setBackground(options.getBackgroundColor());
		iconSize = Math.max(10, getFontMetrics(getFont()).getHeight() - 4);
		setPreferredSize(new Dimension(iconSize + 8, 0));
		revalidate();
		repaint();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	private int getFoldLine(MouseEvent e) {
		if (model == null || pixmap == null) {
			return -1;
		}
		BigInteger index = pixmap.getIndex(e.getY());
		int y = pixmap.getPixel(index) + 2;
		if (e.getX() < 4 || e.getX() > iconSize + 4 ||
			e.getY() < y || e.getY() > y + iconSize) {
			return -1;
		}
		int line = index.intValue();
		return model.isFoldable(line) ? line : -1;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		int line = getFoldLine(e);
		return line < 0 ? null : model.isCollapsed(line) ? "Expand scope" : "Collapse scope";
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (model == null || pixmap == null) {
			return;
		}
		Rectangle visible = getVisibleRect();
		BigInteger end = pixmap.getIndex(visible.y + visible.height);
		g.setColor(getForeground());
		for (BigInteger i = pixmap.getIndex(visible.y); i != null && i.compareTo(end) <= 0;
				i = model.getIndexAfter(i)) {
			if (!model.isFoldable(i.intValue()) || model.getLayout(i) == null) {
				continue;
			}
			int y = pixmap.getPixel(i) + 2;
			int mid = iconSize / 2;
			g.drawRect(4, y, iconSize, iconSize);
			g.drawLine(6, y + mid, iconSize + 2, y + mid);
			if (model.isCollapsed(i.intValue())) {
				g.drawLine(4 + mid, y + 2, 4 + mid, y + iconSize - 2);
			}
		}
	}
}
