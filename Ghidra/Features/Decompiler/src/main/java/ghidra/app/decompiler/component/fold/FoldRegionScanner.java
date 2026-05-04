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

import java.util.*;

import ghidra.app.decompiler.*;
import ghidra.app.decompiler.component.DecompilerUtils;

/**
 * Discovers brace-bounded foldable scopes in a decompiled function.
 *
 * <p>The scan is purely syntactic: every {@code '{'} {@link ClangSyntaxToken} whose matching
 * {@code '}'} lives on a different line becomes a {@link FoldRegion} keyed by the anchor
 * line's layout index. This naturally captures function bodies, if/else, while, for,
 * do-while, and switch blocks, since they all share the brace pattern.
 *
 * <p>Single-line constructs (e.g. {@code if (x) { y; }}) are skipped — there is nothing
 * to hide.
 */
public final class FoldRegionScanner {

	private FoldRegionScanner() {
	}

	/**
	 * Scan the token group of a decompiled function for foldable scopes.
	 *
	 * @param root the root {@link ClangTokenGroup} (typically obtained from
	 *            {@link DecompileResults#getCCodeMarkup()})
	 * @return regions keyed by anchor layout index; never null
	 */
	public static Map<Integer, FoldRegion> scan(ClangTokenGroup root) {
		Map<Integer, FoldRegion> result = new HashMap<>();
		if (root == null) {
			return result;
		}

		Iterator<ClangToken> it = root.tokenIterator(true);
		while (it.hasNext()) {
			ClangToken tok = it.next();
			if (!(tok instanceof ClangSyntaxToken)) {
				continue;
			}
			ClangSyntaxToken syn = (ClangSyntaxToken) tok;
			if (!"{".equals(syn.getText())) {
				continue;
			}
			ClangSyntaxToken close = DecompilerUtils.getMatchingBrace(syn);
			if (close == null) {
				continue;
			}

			int anchor = lineIndex(syn);
			int end = lineIndex(close);
			if (anchor < 0 || end < 0 || end <= anchor) {
				continue;
			}

			// Defensive: if two open braces somehow share an anchor line, keep the one
			// covering more lines (outermost), since folding the inner one alone is a no-op
			// for the user (the outer fold subsumes it).
			FoldRegion existing = result.get(anchor);
			if (existing == null || existing.getEndIndex() < end) {
				result.put(anchor, new FoldRegion(anchor, end));
			}
		}
		return result;
	}

	private static int lineIndex(ClangToken token) {
		ClangLine line = token.getLineParent();
		if (line == null) {
			return -1;
		}
		// ClangLine numbers are 1-based (see DecompilerUtils.toLines); layout indices are 0-based.
		return line.getLineNumber() - 1;
	}
}
