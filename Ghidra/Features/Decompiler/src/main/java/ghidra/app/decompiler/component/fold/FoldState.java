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

/**
 * Mutable per-decompilation fold state for a {@code ClangLayoutController}.
 *
 * <p>Holds the set of {@link FoldRegion}s discovered by {@link FoldRegionScanner} and the
 * subset of anchor indices that are currently collapsed. Hidden-row queries
 * ({@link #isHidden(int)}) compose nesting: an inner region's body is hidden whenever
 * <em>any</em> ancestor region is folded, even if the inner region itself is expanded.
 *
 * <p>Not thread-safe. All mutation and queries happen on the Swing EDT during decompiler
 * rendering, so no synchronization is needed.
 */
public final class FoldState {

	private final Map<Integer, FoldRegion> regions;
	private final Set<Integer> foldedAnchors = new HashSet<>();

	public FoldState() {
		this(Collections.emptyMap());
	}

	public FoldState(Map<Integer, FoldRegion> regions) {
		this.regions = new HashMap<>(regions);
	}

	/**
	 * Replace the discovered fold regions and drop any prior folded state.
	 */
	public void setRegions(Map<Integer, FoldRegion> newRegions) {
		regions.clear();
		regions.putAll(newRegions);
		foldedAnchors.clear();
	}

	/**
	 * Replace the discovered fold regions while preserving fold state at any anchor that
	 * still maps to an identical region. Used after re-decompiling the same function so
	 * fold state survives a refresh.
	 */
	public void setRegionsPreservingFoldedAnchors(Map<Integer, FoldRegion> newRegions) {
		Set<Integer> survivors = new HashSet<>();
		for (Integer anchor : foldedAnchors) {
			FoldRegion before = regions.get(anchor);
			FoldRegion after = newRegions.get(anchor);
			if (before != null && before.equals(after)) {
				survivors.add(anchor);
			}
		}
		regions.clear();
		regions.putAll(newRegions);
		foldedAnchors.clear();
		foldedAnchors.addAll(survivors);
	}

	public boolean isFoldable(int anchorIndex) {
		return regions.containsKey(anchorIndex);
	}

	public boolean isFolded(int anchorIndex) {
		return foldedAnchors.contains(anchorIndex);
	}

	/**
	 * @return true if the given layout index is in the body of <em>any</em> currently
	 *         folded region (i.e. should not be rendered)
	 */
	public boolean isHidden(int layoutIndex) {
		if (foldedAnchors.isEmpty()) {
			return false;
		}
		for (Integer anchor : foldedAnchors) {
			FoldRegion r = regions.get(anchor);
			if (r != null && r.containsBody(layoutIndex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Toggle fold state at the given anchor.
	 *
	 * @param anchorIndex layout index of a fold anchor
	 * @return true if the state changed (i.e. the index is a known anchor); false if the
	 *         anchor was unknown and nothing happened
	 */
	public boolean toggle(int anchorIndex) {
		if (!regions.containsKey(anchorIndex)) {
			return false;
		}
		if (!foldedAnchors.add(anchorIndex)) {
			foldedAnchors.remove(anchorIndex);
		}
		return true;
	}

	/**
	 * @return true if state changed
	 */
	public boolean foldAll() {
		if (foldedAnchors.size() == regions.size()) {
			return false;
		}
		foldedAnchors.addAll(regions.keySet());
		return true;
	}

	/**
	 * @return true if state changed
	 */
	public boolean unfoldAll() {
		if (foldedAnchors.isEmpty()) {
			return false;
		}
		foldedAnchors.clear();
		return true;
	}

	public FoldRegion getRegion(int anchorIndex) {
		return regions.get(anchorIndex);
	}

	public Collection<FoldRegion> getRegions() {
		return Collections.unmodifiableCollection(regions.values());
	}

	public Set<Integer> getFoldedAnchors() {
		return Collections.unmodifiableSet(foldedAnchors);
	}

	/**
	 * @param layoutIndex any layout index
	 * @return the innermost region whose body contains {@code layoutIndex}, or whose
	 *         anchor equals {@code layoutIndex} — useful for "fold at cursor" actions.
	 *         Returns null if no enclosing foldable region exists.
	 */
	public FoldRegion findEnclosing(int layoutIndex) {
		FoldRegion best = null;
		int bestSpan = Integer.MAX_VALUE;
		for (FoldRegion r : regions.values()) {
			boolean encloses = layoutIndex == r.getAnchorIndex() || r.containsBody(layoutIndex);
			if (!encloses) {
				continue;
			}
			int span = r.getEndIndex() - r.getAnchorIndex();
			if (span < bestSpan) {
				bestSpan = span;
				best = r;
			}
		}
		return best;
	}
}
