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

import java.util.*;

import ghidra.app.decompiler.*;

/** Tracks folded block interiors using the original, zero-based C line indices. */
class DecompilerFoldingModel {

	private final NavigableMap<Integer, Integer> blocks = new TreeMap<>();
	private final Set<Integer> collapsed = new HashSet<>();
	private final BitSet hidden = new BitSet();

	void reset(List<ClangLine> lines) {
		blocks.clear();
		collapsed.clear();
		hidden.clear();
		Deque<Integer> openings = new ArrayDeque<>();
		for (int i = 0; i < lines.size(); i++) {
			for (ClangToken token : lines.get(i).getAllTokens()) {
				// Braces in comments and string constants are not scope delimiters.
				if (!(token instanceof ClangSyntaxToken)) {
					continue;
				}
				if ("{".equals(token.getText())) {
					openings.push(i);
				}
				else if ("}".equals(token.getText()) && !openings.isEmpty()) {
					int start = openings.pop();
					if (i > start + 1) {
						// Keep the closing line visible, including any "else" or "while".
						blocks.merge(start, i, Math::max);
					}
				}
			}
		}
	}

	boolean isFoldable(int line) {
		return blocks.containsKey(line);
	}

	boolean isCollapsed(int line) {
		return collapsed.contains(line);
	}

	boolean isHidden(int line) {
		return line >= 0 && hidden.get(line);
	}

	int nextVisible(int line) {
		return hidden.nextClearBit(Math.max(0, line + 1));
	}

	int previousVisible(int line) {
		return hidden.previousClearBit(line - 1);
	}

	int enclosingBlock(int line) {
		for (var entry : blocks.headMap(line, true).descendingMap().entrySet()) {
			if (line <= entry.getValue() && !isHidden(entry.getKey())) {
				return entry.getKey();
			}
		}
		return -1;
	}

	void toggle(int line) {
		if (isFoldable(line)) {
			if (!collapsed.remove(line)) {
				collapsed.add(line);
			}
			rebuildHidden();
		}
	}

	void setAllCollapsed(boolean collapse) {
		collapsed.clear();
		if (collapse) {
			collapsed.addAll(blocks.keySet());
		}
		rebuildHidden();
	}

	boolean reveal(int line) {
		if (!isHidden(line)) {
			return false;
		}
		collapsed.removeIf(start -> start < line && line < blocks.get(start));
		rebuildHidden();
		return true;
	}

	private void rebuildHidden() {
		hidden.clear();
		for (int start : collapsed) {
			hidden.set(start + 1, blocks.get(start));
		}
	}
}
