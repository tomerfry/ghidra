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

/**
 * A foldable scope in the decompiler view, identified by the layout indices of its opening
 * and closing brace lines.
 *
 * <p>Indices are 0-based layout indices (i.e. row indices into the {@code FieldPanel}
 * model), <em>not</em> the 1-based {@link ghidra.app.decompiler.ClangLine} line numbers.
 *
 * <p>When a region is collapsed, layout indices in the body range
 * {@code (anchorIndex, endIndex]} are hidden (the anchor line itself remains visible and
 * shows a placeholder).
 */
public final class FoldRegion {

	private final int anchorIndex;
	private final int endIndex;

	/**
	 * @param anchorIndex layout index of the line containing the opening brace
	 * @param endIndex layout index of the line containing the matching closing brace
	 */
	public FoldRegion(int anchorIndex, int endIndex) {
		if (anchorIndex < 0 || endIndex <= anchorIndex) {
			throw new IllegalArgumentException(
				"Invalid fold range: anchor=" + anchorIndex + " end=" + endIndex);
		}
		this.anchorIndex = anchorIndex;
		this.endIndex = endIndex;
	}

	public int getAnchorIndex() {
		return anchorIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	/**
	 * @param index a layout index
	 * @return true if {@code index} is in this region's body (i.e. would be hidden when folded)
	 */
	public boolean containsBody(int index) {
		return index > anchorIndex && index <= endIndex;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FoldRegion)) {
			return false;
		}
		FoldRegion that = (FoldRegion) o;
		return anchorIndex == that.anchorIndex && endIndex == that.endIndex;
	}

	@Override
	public int hashCode() {
		return anchorIndex * 31 + endIndex;
	}

	@Override
	public String toString() {
		return "FoldRegion[" + anchorIndex + ".." + endIndex + "]";
	}
}
