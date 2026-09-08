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

import java.util.*;

import org.junit.Test;

import ghidra.app.decompiler.*;

public class DecompilerFoldingModelTest {

	private final DecompilerFoldingModel model = new DecompilerFoldingModel();

	@Test
	public void testNestedStateSurvivesParentToggle() {
		model.reset(lines("{", "{", "body", "}", "tail", "}"));
		model.toggle(1);
		model.toggle(0);
		assertEquals(5, model.nextVisible(0));
		assertEquals(0, model.previousVisible(5));
		model.toggle(0);
		assertTrue(model.isCollapsed(1));
		assertTrue(model.isHidden(2));
		assertFalse(model.isHidden(1));
		assertFalse(model.isHidden(3));
		assertEquals(3, model.nextVisible(1));
	}

	@Test
	public void testRevealOpensAllAncestorsButNotOtherBlocks() {
		model.reset(lines("{", "{", "body", "}", "{", "other", "}", "}"));
		model.setAllCollapsed(true);
		assertTrue(model.reveal(2));
		assertFalse(model.isCollapsed(0));
		assertFalse(model.isCollapsed(1));
		assertTrue(model.isCollapsed(4));
		assertFalse(model.isHidden(2));
		assertTrue(model.isHidden(5));
		assertFalse(model.reveal(2));
	}

	@Test
	public void testElseClosingLineRemainsVisible() {
		List<ClangLine> lines = lines("{", "if body", "}", "else body", "}");
		lines.get(2).addToken(new ClangSyntaxToken(null, " else "));
		lines.get(2).addToken(new ClangSyntaxToken(null, "{"));
		model.reset(lines);
		model.toggle(0);
		assertFalse(model.isHidden(2));
		assertTrue(model.isFoldable(2));
		model.toggle(2);
		assertTrue(model.isHidden(3));
		assertFalse(model.isHidden(4));
	}

	@Test
	public void testIgnoreNonSyntaxBracesAndEmptyScopes() {
		List<ClangLine> lines = lines("{", "text", "}", "{", "}", "{");
		lines.get(1).addToken(new ClangToken(null, "}"));
		lines.get(1).addToken(new ClangSyntaxToken(null, "\"{\""));
		model.reset(lines);
		assertTrue(model.isFoldable(0));
		assertFalse(model.isFoldable(3));
		assertFalse(model.isFoldable(5));
		model.toggle(0);
		assertTrue(model.isHidden(1));
		assertFalse(model.isHidden(2));
	}

	@Test
	public void testEnclosingVisibleScope() {
		model.reset(lines("{", "{", "body", "}", "tail", "}"));
		assertEquals(1, model.enclosingBlock(2));
		assertEquals(0, model.enclosingBlock(4));
		assertEquals(-1, model.enclosingBlock(-1));
		model.toggle(0);
		assertEquals(0, model.enclosingBlock(2));
	}

	@Test
	public void testExpandAllAndReset() {
		model.reset(lines("{", "body", "}"));
		model.setAllCollapsed(true);
		model.setAllCollapsed(false);
		assertFalse(model.isHidden(1));
		model.toggle(0);
		model.reset(lines("new function"));
		assertFalse(model.isFoldable(0));
		assertFalse(model.isCollapsed(0));
		assertFalse(model.isHidden(1));
		assertEquals(-1, model.previousVisible(0));
	}

	private List<ClangLine> lines(String... text) {
		List<ClangLine> lines = new ArrayList<>();
		for (String value : text) {
			ClangLine line = new ClangLine(lines.size() + 1, 0);
			line.addToken(new ClangSyntaxToken(null, value));
			lines.add(line);
		}
		return lines;
	}
}
