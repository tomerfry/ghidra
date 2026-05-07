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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import docking.widgets.fieldpanel.support.AnchoredLayout;

/**
 * An implementation of {@link LayoutPixelIndexMap} for vertical coordinates.
 *
 * <p>The visible layouts may have non-contiguous indices (e.g. when scope folding hides
 * intermediate rows), so the map stores a sparse parallel pair of arrays: a sorted array
 * of visible layout indices and a sorted array of their y positions.
 *
 * <p>Both {@link #getPixel(BigInteger)} and {@link #getIndex(int)} run in O(log n).
 */
public class VerticalLayoutPixelIndexMap implements LayoutPixelIndexMap {

	private int[] indices = new int[0];
	private int[] yPositions = new int[0];
	private int size;

	@Override
	public int getPixel(BigInteger index) {
		if (size == 0) {
			return 0;
		}
		int idx = index.intValueExact();
		int pos = Arrays.binarySearch(indices, 0, size, idx);
		if (pos < 0) {
			// The index is not currently visible (hidden by a fold, or out of range).
			// Caller iterating a range should skip; return the position of the next
			// visible layout for safety, clamped within bounds.
			int insertPoint = -pos - 1;
			if (insertPoint >= size) {
				insertPoint = size - 1;
			}
			return yPositions[insertPoint];
		}
		return yPositions[pos];
	}

	@Override
	public BigInteger getIndex(int pixel) {
		if (size == 0) {
			return BigInteger.ZERO;
		}
		int pos = Arrays.binarySearch(yPositions, 0, size, pixel);
		if (pos < 0) {
			// pos = -insertionPoint - 1; want the layout *containing* pixel, i.e.
			// the largest layout whose y <= pixel.
			pos = -pos - 2;
			if (pos < 0) {
				pos = 0;
			}
		}
		return BigInteger.valueOf(indices[pos]);
	}

	/**
	 * Returns true if the given layout index is currently visible (not hidden).
	 *
	 * @param index a layout index
	 * @return true if this map has a y position for {@code index}
	 */
	public boolean isVisible(BigInteger index) {
		if (size == 0) {
			return false;
		}
		return Arrays.binarySearch(indices, 0, size, index.intValueExact()) >= 0;
	}

	public void layoutsChanged(List<AnchoredLayout> layouts) {
		size = layouts.size();
		if (indices.length < size) {
			indices = new int[size];
			yPositions = new int[size];
		}
		for (int i = 0; i < size; i++) {
			AnchoredLayout l = layouts.get(i);
			indices[i] = l.getIndex().intValueExact();
			yPositions[i] = l.getYPos();
		}
	}
}
