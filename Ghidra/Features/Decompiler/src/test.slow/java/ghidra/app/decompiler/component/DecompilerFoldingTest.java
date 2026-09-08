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
package ghidra.app.decompiler.component;

import static org.junit.Assert.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigInteger;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;

import docking.widgets.fieldpanel.FieldPanel;
import docking.widgets.fieldpanel.support.*;
import ghidra.app.decompiler.*;
import ghidra.app.decompiler.component.margin.FoldingDecompilerMarginProvider;
import ghidra.app.plugin.core.decompile.AbstractDecompilerTest;
import ghidra.app.plugin.core.decompile.DecompilerClipboardProvider;
import ghidra.program.database.ProgramBuilder;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;

public class DecompilerFoldingTest extends AbstractDecompilerTest {

	private DecompilerPanel panel;
	private ClangLayoutController model;

	@Override
	protected Program getProgram() throws Exception {
		ProgramBuilder builder = new ProgramBuilder("Folding", ProgramBuilder._X86, this);
		builder.createMemory("code", "1000", 16);
		builder.setBytes("1000", "31 c0 c3");
		builder.disassemble("1000", 3);
		builder.createFunction("1000");
		return builder.getProgram();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		decompile("1000");
		panel = provider.getDecompilerPanel();
		model = panel.getLayoutController();
		assertTrue("Native decompiler output should contain a foldable function body",
			panel.getLines().stream().anyMatch(line -> model.isFoldable(line.getLineNumber() - 1)));
		// Deterministic nested blocks, independent of native decompiler formatting choices.
		runSwing(() -> model.buildLayouts(null, document(), null, true));
		waitForSwing();
	}

	private ClangTokenGroup document() {
		ClangTokenGroup root = new ClangFunction(null, provider.getController().getHighFunction());
		for (String text : new String[] { "{", "{", "hidden_call();", "}", "tail();", "}" }) {
			root.AddTokenGroup(new ClangBreak(root, 0));
			root.AddTokenGroup(new ClangSyntaxToken(root, text));
		}
		return root;
	}

	@Test
	public void testFoldMovesCursorAndSkipsHiddenLayouts() {
		runSwing(() -> {
			panel.getFieldPanel().goTo(BigInteger.valueOf(2), 0, 0, 0, false);
			model.toggleFold(0);
			assertTrue(model.isCollapsed(0));
			assertNull(model.getLayout(BigInteger.valueOf(2)));
			assertEquals(BigInteger.ZERO, panel.getCursorPosition().getIndex());
			assertEquals(BigInteger.valueOf(5), model.getIndexAfter(BigInteger.ZERO));
			assertEquals(BigInteger.ZERO, model.getIndexBefore(BigInteger.valueOf(5)));
		});
		waitForSwing();
		assertEquals(6, panel.getLines().size());
	}

	@Test
	public void testNavigationAndSearchCursorRevealNestedCode() {
		runSwing(() -> {
			model.setAllCollapsed(true);
			panel.getFieldPanel().goTo(BigInteger.valueOf(2), 0, 0, 0, false);
			assertFalse(model.isCollapsed(0));
			assertFalse(model.isCollapsed(1));
			assertEquals(BigInteger.valueOf(2), panel.getCursorPosition().getIndex());
			model.setAllCollapsed(true);
			panel.setCursorPosition(new FieldLocation(2));
			assertNotNull(model.getLayout(BigInteger.valueOf(2)));
			assertEquals(BigInteger.valueOf(2), panel.getCursorPosition().getIndex());
		});
	}

	@Test
	public void testGutterClickCollapsesAndExpands() {
		FoldingDecompilerMarginProvider gutter =
			findComponent(panel, FoldingDecompilerMarginProvider.class);
		assertNotNull(gutter);
		runSwing(() -> {
			FieldPanel fields = panel.getFieldPanel();
			fields.goTo(BigInteger.ZERO, 0, 0, 0, false);
			Point point = fields.getPointForLocation(new FieldLocation(0));
			int y = fields.getVisibleStartLayout().getYPos() + 5;
			assertNotNull(point);
			gutter.dispatchEvent(new MouseEvent(gutter, MouseEvent.MOUSE_CLICKED,
				System.currentTimeMillis(), 0, 8, y, 1, false, MouseEvent.BUTTON1));
			assertTrue(model.isCollapsed(0));
			gutter.dispatchEvent(new MouseEvent(gutter, MouseEvent.MOUSE_CLICKED,
				System.currentTimeMillis(), 0, 8, y, 1, false, MouseEvent.BUTTON1));
			assertFalse(model.isCollapsed(0));
		});
	}

	@Test
	public void testCopySelectionIncludesHiddenCode() throws Exception {
		DecompilerClipboardProvider clipboard =
			(DecompilerClipboardProvider) getInstanceField("clipboard", panel);
		runSwing(() -> {
			model.toggleFold(0);
			FieldSelection selection = new FieldSelection();
			selection.addRange(BigInteger.ZERO, BigInteger.valueOf(6));
			clipboard.selectionChanged(selection);
		});
		String copied = (String) clipboard.copy(TaskMonitor.DUMMY)
				.getTransferData(DataFlavor.stringFlavor);
		assertTrue(copied.contains("hidden_call();"));
		assertTrue(model.isCollapsed(0));
	}

	@Test
	public void testNewDocumentClearsFolds() {
		runSwing(() -> {
			model.setAllCollapsed(true);
			model.buildLayouts(null, document(), null, true);
			assertFalse(model.isCollapsed(0));
			assertNotNull(model.getLayout(BigInteger.valueOf(2)));
		});
	}

	@Test
	public void testFoldedPanelPaints() throws Exception {
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(),
			BufferedImage.TYPE_INT_RGB);
		runSwing(() -> {
			model.toggleFold(1);
			Graphics2D graphics = image.createGraphics();
			try {
				panel.paint(graphics);
			}
			finally {
				graphics.dispose();
			}
		});
		ImageIO.write(image, "png", new File(System.getProperty("java.io.tmpdir"),
			"ghidra-scope-folding.png"));
	}
}
