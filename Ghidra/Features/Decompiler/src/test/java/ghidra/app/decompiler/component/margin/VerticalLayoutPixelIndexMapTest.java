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
import java.util.List;

import org.junit.Test;

import docking.widgets.fieldpanel.support.AnchoredLayout;

public class VerticalLayoutPixelIndexMapTest {

	@Test
	public void testFoldedAndWrappedLines() {
		VerticalLayoutPixelIndexMap map = new VerticalLayoutPixelIndexMap();
		map.layoutsChanged(List.of(layout(2, -5), layout(20, 15), layout(21, 55)));
		assertEquals(BigInteger.valueOf(2), map.getIndex(0));
		assertEquals(BigInteger.valueOf(20), map.getIndex(15));
		assertEquals(BigInteger.valueOf(20), map.getIndex(54));
		assertEquals(BigInteger.valueOf(21), map.getIndex(55));
		assertEquals(55, map.getPixel(BigInteger.valueOf(21)));
		assertEquals(-Integer.MAX_VALUE, map.getPixel(BigInteger.TEN));
	}

	@Test
	public void testEmptyAndShortenedViewport() {
		VerticalLayoutPixelIndexMap map = new VerticalLayoutPixelIndexMap();
		assertEquals(BigInteger.ZERO, map.getIndex(0));
		assertEquals(-Integer.MAX_VALUE, map.getPixel(BigInteger.ZERO));
		map.layoutsChanged(List.of(layout(0, 0), layout(1, 20), layout(2, 40)));
		map.layoutsChanged(List.of(layout(50, 0)));
		assertEquals(BigInteger.valueOf(50), map.getIndex(1000));
		assertEquals(-Integer.MAX_VALUE, map.getPixel(BigInteger.ONE));
		map.layoutsChanged(List.of());
		assertEquals(BigInteger.ZERO, map.getIndex(0));
	}

	private AnchoredLayout layout(int index, int y) {
		return new AnchoredLayout(null, BigInteger.valueOf(index), y);
	}
}
