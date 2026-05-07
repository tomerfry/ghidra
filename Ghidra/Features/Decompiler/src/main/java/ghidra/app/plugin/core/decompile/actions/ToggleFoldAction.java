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
package ghidra.app.plugin.core.decompile.actions;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import docking.action.KeyBindingData;
import docking.widgets.fieldpanel.support.FieldLocation;
import ghidra.app.decompiler.component.ClangLayoutController;
import ghidra.app.decompiler.component.DecompilerPanel;
import ghidra.app.decompiler.component.fold.FoldRegion;
import ghidra.app.decompiler.component.fold.FoldState;
import ghidra.app.plugin.core.decompile.DecompilerActionContext;
import ghidra.app.util.HelpTopics;
import ghidra.util.HelpLocation;

/**
 * Toggle the fold state of the scope enclosing the cursor in the Decompiler view.
 *
 * <p>If the cursor sits on a fold anchor or anywhere inside a foldable region's body,
 * that region collapses or expands. With nested folds, the innermost enclosing region
 * wins.
 */
public class ToggleFoldAction extends AbstractDecompilerAction {

	public ToggleFoldAction() {
		super("Toggle Fold");
		setHelpLocation(new HelpLocation(HelpTopics.DECOMPILER, "ToggleFold"));
		// Same chord IntelliJ/VSCode use for fold-toggle.
		setKeyBindingData(
			new KeyBindingData(KeyEvent.VK_PERIOD,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
	}

	@Override
	protected boolean isEnabledForDecompilerContext(DecompilerActionContext context) {
		return true;
	}

	@Override
	protected void decompilerActionPerformed(DecompilerActionContext context) {
		DecompilerPanel panel = context.getDecompilerPanel();
		ClangLayoutController controller = panel.getLayoutController();
		FoldState foldState = controller.getFoldState();

		FieldLocation cursor = panel.getCursorPosition();
		if (cursor == null) {
			return;
		}
		FoldRegion enclosing = foldState.findEnclosing(cursor.getIndex().intValue());
		if (enclosing == null) {
			return;
		}
		controller.toggleFold(enclosing.getAnchorIndex());
	}
}
