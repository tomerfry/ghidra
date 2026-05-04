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

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import docking.widgets.fieldpanel.support.AnchoredLayout;

public class VerticalLayoutPixelIndexMapTest {

	@Test
	public void empty_returnsZeroAndIsVisibleFalse() {
		VerticalLayoutPixelIndexMap m = new VerticalLayoutPixelIndexMap();
		assertEquals(0, m.getPixel(BigInteger.ZERO));
		assertEquals(BigInteger.ZERO, m.getIndex(50));
		assertFalse(m.isVisible(BigInteger.ZERO));
	}

	@Test
	public void contiguousLayouts_resolveBothDirections() {
		VerticalLayoutPixelIndexMap m = new VerticalLayoutPixelIndexMap();
		m.layoutsChanged(layouts(0, 0, 1, 10, 2, 20, 3, 30));

		assertEquals(0, m.getPixel(BigInteger.valueOf(0)));
		assertEquals(20, m.getPixel(BigInteger.valueOf(2)));

		assertEquals(BigInteger.valueOf(0), m.getIndex(0));
		assertEquals(BigInteger.valueOf(0), m.getIndex(5));
		assertEquals(BigInteger.valueOf(2), m.getIndex(20));
		assertEquals(BigInteger.valueOf(2), m.getIndex(25));
		assertEquals(BigInteger.valueOf(3), m.getIndex(999));
	}

	@Test
	public void nonContiguousLayouts_resolveCorrectly() {
		// indices 0, 1, 2 are visible; 3, 4, 5 hidden; 6 visible.
		VerticalLayoutPixelIndexMap m = new VerticalLayoutPixelIndexMap();
		m.layoutsChanged(layouts(0, 0, 1, 10, 2, 20, 6, 30));

		assertEquals(0, m.getPixel(BigInteger.valueOf(0)));
		assertEquals(20, m.getPixel(BigInteger.valueOf(2)));
		assertEquals(30, m.getPixel(BigInteger.valueOf(6)));

		assertTrue(m.isVisible(BigInteger.valueOf(0)));
		assertTrue(m.isVisible(BigInteger.valueOf(6)));
		assertFalse("hidden index", m.isVisible(BigInteger.valueOf(3)));
		assertFalse("hidden index", m.isVisible(BigInteger.valueOf(5)));

		// getIndex should return the layout containing the pixel; pixels in the gap
		// (between y=20 and y=30) belong to the layout at y=20 (index 2).
		assertEquals(BigInteger.valueOf(2), m.getIndex(25));
		assertEquals(BigInteger.valueOf(6), m.getIndex(30));
		assertEquals(BigInteger.valueOf(6), m.getIndex(100));
	}

	@Test
	public void layoutsChanged_canShrink() {
		VerticalLayoutPixelIndexMap m = new VerticalLayoutPixelIndexMap();
		m.layoutsChanged(layouts(0, 0, 1, 10, 2, 20, 3, 30));
		m.layoutsChanged(layouts(0, 0, 5, 10));

		assertTrue(m.isVisible(BigInteger.valueOf(0)));
		assertTrue(m.isVisible(BigInteger.valueOf(5)));
		assertFalse(m.isVisible(BigInteger.valueOf(2)));
	}

	private static List<AnchoredLayout> layouts(int... indexAndYPairs) {
		assertEquals("pairs", 0, indexAndYPairs.length % 2);
		AnchoredLayout[] result = new AnchoredLayout[indexAndYPairs.length / 2];
		for (int i = 0; i < result.length; i++) {
			int idx = indexAndYPairs[i * 2];
			int y = indexAndYPairs[i * 2 + 1];
			result[i] = new AnchoredLayout(null, BigInteger.valueOf(idx), y);
		}
		return Arrays.asList(result);
	}
}
