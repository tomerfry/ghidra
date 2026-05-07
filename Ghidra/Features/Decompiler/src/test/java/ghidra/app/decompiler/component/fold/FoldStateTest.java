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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FoldStateTest {

	@Test
	public void empty_hidesNothing() {
		FoldState s = new FoldState();
		assertFalse(s.isHidden(0));
		assertFalse(s.isHidden(100));
		assertFalse(s.isFoldable(0));
		assertFalse(s.isFolded(0));
	}

	@Test
	public void toggle_returnsFalseForUnknownAnchor() {
		FoldState s = new FoldState(regions(new FoldRegion(0, 5)));
		assertFalse(s.toggle(7));
		assertFalse(s.isFolded(0));
	}

	@Test
	public void toggle_collapsesAndExpands() {
		FoldRegion r = new FoldRegion(0, 5);
		FoldState s = new FoldState(regions(r));

		assertTrue(s.toggle(0));
		assertTrue(s.isFolded(0));
		assertTrue(s.isHidden(1));
		assertTrue(s.isHidden(5));
		assertFalse(s.isHidden(0));
		assertFalse(s.isHidden(6));

		assertTrue(s.toggle(0));
		assertFalse(s.isFolded(0));
		assertFalse(s.isHidden(3));
	}

	@Test
	public void nestedFolds_outerFoldHidesInnerBody() {
		FoldRegion outer = new FoldRegion(0, 10);
		FoldRegion inner = new FoldRegion(2, 8);
		FoldState s = new FoldState(regions(outer, inner));

		s.toggle(0);

		// Inner region's body (3..8) is hidden via outer fold even though inner is expanded.
		assertTrue(s.isHidden(3));
		assertTrue(s.isHidden(8));
		assertFalse(s.isFolded(2));
	}

	@Test
	public void nestedFolds_innerFoldDoesNotHideOuterTail() {
		FoldRegion outer = new FoldRegion(0, 10);
		FoldRegion inner = new FoldRegion(2, 8);
		FoldState s = new FoldState(regions(outer, inner));

		s.toggle(2);

		assertTrue(s.isHidden(3));
		assertTrue(s.isHidden(8));
		assertFalse(s.isHidden(9));
		assertFalse(s.isHidden(10));
	}

	@Test
	public void foldAll_unfoldAll() {
		FoldRegion a = new FoldRegion(0, 3);
		FoldRegion b = new FoldRegion(5, 9);
		FoldState s = new FoldState(regions(a, b));

		assertTrue(s.foldAll());
		assertTrue(s.isFolded(0));
		assertTrue(s.isFolded(5));
		assertFalse("foldAll on already-folded state is a no-op", s.foldAll());

		assertTrue(s.unfoldAll());
		assertFalse(s.isFolded(0));
		assertFalse(s.isFolded(5));
		assertFalse(s.unfoldAll());
	}

	@Test
	public void findEnclosing_returnsInnermost() {
		FoldRegion outer = new FoldRegion(0, 10);
		FoldRegion inner = new FoldRegion(2, 8);
		FoldState s = new FoldState(regions(outer, inner));

		// Index 5 is inside both — innermost wins.
		assertEquals(inner, s.findEnclosing(5));
		// Index 9 is only inside the outer.
		assertEquals(outer, s.findEnclosing(9));
		// Anchor of inner — innermost wins.
		assertEquals(inner, s.findEnclosing(2));
		// Outside everything.
		assertNull(s.findEnclosing(11));
	}

	@Test
	public void setRegionsPreservingFoldedAnchors_keepsIdenticalAnchors() {
		FoldRegion a = new FoldRegion(0, 3);
		FoldRegion b = new FoldRegion(5, 9);
		FoldState s = new FoldState(regions(a, b));
		s.toggle(0);
		s.toggle(5);

		// Same anchor 0 with same range; anchor 5 with a *different* range -> dropped.
		Map<Integer, FoldRegion> next = regions(new FoldRegion(0, 3), new FoldRegion(5, 12));
		s.setRegionsPreservingFoldedAnchors(next);

		assertTrue("anchor 0 unchanged -> fold preserved", s.isFolded(0));
		assertFalse("anchor 5 range changed -> fold dropped", s.isFolded(5));
	}

	@Test
	public void setRegions_dropsAllFoldState() {
		FoldState s = new FoldState(regions(new FoldRegion(0, 3)));
		s.toggle(0);
		s.setRegions(regions(new FoldRegion(0, 3)));
		assertFalse(s.isFolded(0));
	}

	private static Map<Integer, FoldRegion> regions(FoldRegion... rs) {
		Map<Integer, FoldRegion> m = new HashMap<>();
		for (FoldRegion r : rs) {
			m.put(r.getAnchorIndex(), r);
		}
		return m;
	}
}
