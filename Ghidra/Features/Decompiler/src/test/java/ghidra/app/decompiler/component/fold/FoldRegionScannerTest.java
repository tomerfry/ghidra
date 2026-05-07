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
package ghidra.app.decompiler.component.fold;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import ghidra.app.decompiler.*;
import ghidra.app.decompiler.component.DecompilerUtils;

public class FoldRegionScannerTest {

	@Test
	public void scan_returnsEmptyMap_whenRootIsNull() {
		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(null);
		assertNotNull(regions);
		assertTrue(regions.isEmpty());
	}

	@Test
	public void scan_returnsEmptyMap_whenNoBraces() {
		// "x = 1;" — no braces, no folds.
		ClangTokenGroup root = new ClangTokenGroup(null);
		appendText(root, "x = 1;");
		populateLines(root);

		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(root);

		assertTrue(regions.isEmpty());
	}

	@Test
	public void scan_skipsSingleLineBraceBlock() {
		// "if (x) { y; }" on a single line — no body lines to hide.
		ClangTokenGroup root = new ClangTokenGroup(null);
		appendText(root, "if (x) ");
		appendBrace(root, "{");
		appendText(root, " y; ");
		appendBrace(root, "}");
		populateLines(root);

		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(root);

		assertTrue("single-line {} should not produce a fold", regions.isEmpty());
	}

	@Test
	public void scan_findsSingleMultiLineBlock() {
		// 0: "if (x) {"
		// 1: "  y;"
		// 2: "}"
		ClangTokenGroup root = new ClangTokenGroup(null);
		appendText(root, "if (x) ");
		appendBrace(root, "{");
		appendBreak(root);
		appendText(root, "y;");
		appendBreak(root);
		appendBrace(root, "}");
		populateLines(root);

		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(root);

		assertEquals(1, regions.size());
		FoldRegion r = regions.get(0);
		assertNotNull("anchor at line 0 expected", r);
		assertEquals(0, r.getAnchorIndex());
		assertEquals(2, r.getEndIndex());
	}

	@Test
	public void scan_findsNestedBlocks() {
		// 0: "if (x) {"
		// 1: "  while (y) {"
		// 2: "    z;"
		// 3: "  }"
		// 4: "}"
		ClangTokenGroup root = new ClangTokenGroup(null);
		appendText(root, "if (x) ");
		appendBrace(root, "{");
		appendBreak(root);
		appendText(root, "while (y) ");
		appendBrace(root, "{");
		appendBreak(root);
		appendText(root, "z;");
		appendBreak(root);
		appendBrace(root, "}");
		appendBreak(root);
		appendBrace(root, "}");
		populateLines(root);

		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(root);

		assertEquals(2, regions.size());
		FoldRegion outer = regions.get(0);
		FoldRegion inner = regions.get(1);
		assertEquals(new FoldRegion(0, 4), outer);
		assertEquals(new FoldRegion(1, 3), inner);
	}

	@Test
	public void scan_findsIfElseAsTwoSeparateRegions() {
		// 0: "if (x) {"
		// 1: "  y;"
		// 2: "} else {"
		// 3: "  z;"
		// 4: "}"
		// The two `{` produce two regions; line 2 is anchor for the second.
		ClangTokenGroup root = new ClangTokenGroup(null);
		appendText(root, "if (x) ");
		appendBrace(root, "{");
		appendBreak(root);
		appendText(root, "y;");
		appendBreak(root);
		appendBrace(root, "}");
		appendText(root, " else ");
		appendBrace(root, "{");
		appendBreak(root);
		appendText(root, "z;");
		appendBreak(root);
		appendBrace(root, "}");
		populateLines(root);

		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(root);

		assertEquals(2, regions.size());
		assertEquals(new FoldRegion(0, 2), regions.get(0));
		assertEquals(new FoldRegion(2, 4), regions.get(2));
	}

	@Test
	public void scan_skipsUnmatchedOpenBrace() {
		// 0: "{"
		// 1: "y;"
		// (no closing brace) — getMatchingBrace returns null, so no region.
		ClangTokenGroup root = new ClangTokenGroup(null);
		appendBrace(root, "{");
		appendBreak(root);
		appendText(root, "y;");
		populateLines(root);

		Map<Integer, FoldRegion> regions = FoldRegionScanner.scan(root);

		assertTrue("unmatched '{' should not produce a fold", regions.isEmpty());
	}

	// --- tree-building helpers --------------------------------------------------------

	/**
	 * Append non-brace syntax tokens. We use a single ClangSyntaxToken per call; that's
	 * fine because the scanner only cares about literal {@code "{"} and {@code "}"}
	 * tokens.
	 */
	private static void appendText(ClangTokenGroup parent, String text) {
		parent.AddTokenGroup(new ClangSyntaxToken(parent, text));
	}

	private static void appendBrace(ClangTokenGroup parent, String brace) {
		parent.AddTokenGroup(new ClangSyntaxToken(parent, brace));
	}

	private static void appendBreak(ClangTokenGroup parent) {
		parent.AddTokenGroup(new ClangBreak(parent, 0));
	}

	/**
	 * Run the same line-flattening that {@link ghidra.app.decompiler.PrettyPrinter} runs,
	 * which assigns each token a {@link ClangLine} parent (with 1-based line numbers).
	 * The scanner relies on these parents.
	 */
	private static void populateLines(ClangTokenGroup root) {
		DecompilerUtils.toLines(root);
	}
}
