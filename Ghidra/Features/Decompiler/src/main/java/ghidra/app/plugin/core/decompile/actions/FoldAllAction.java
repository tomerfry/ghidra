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
import ghidra.app.plugin.core.decompile.DecompilerActionContext;
import ghidra.app.util.HelpTopics;
import ghidra.util.HelpLocation;

/**
 * Collapse every foldable scope in the Decompiler view.
 */
public class FoldAllAction extends AbstractDecompilerAction {

	public FoldAllAction() {
		super("Fold All");
		setHelpLocation(new HelpLocation(HelpTopics.DECOMPILER, "ToggleFold"));
		setKeyBindingData(
			new KeyBindingData(KeyEvent.VK_MINUS,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
	}

	@Override
	protected boolean isEnabledForDecompilerContext(DecompilerActionContext context) {
		return true;
	}

	@Override
	protected void decompilerActionPerformed(DecompilerActionContext context) {
		context.getDecompilerPanel().getLayoutController().foldAll();
	}
}
