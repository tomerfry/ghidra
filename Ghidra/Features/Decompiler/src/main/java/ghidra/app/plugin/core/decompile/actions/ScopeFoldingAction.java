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

import docking.action.MenuData;
import ghidra.app.decompiler.component.ClangLayoutController;
import ghidra.app.plugin.core.decompile.DecompilerActionContext;
import ghidra.app.util.HelpTopics;
import ghidra.util.HelpLocation;

/** Scope folding actions, also available for user-assigned key bindings. */
public class ScopeFoldingAction extends AbstractDecompilerAction {

	public enum Operation {
		TOGGLE("Toggle Scope"), COLLAPSE_ALL("Collapse All Scopes"), EXPAND_ALL("Expand All Scopes");

		private final String label;

		Operation(String label) {
			this.label = label;
		}
	}

	private final Operation operation;

	public ScopeFoldingAction(Operation operation) {
		super(operation.label);
		this.operation = operation;
		setPopupMenuData(new MenuData(new String[] { "Scope Folding", operation.label },
			"Decompiler Folding"));
		setHelpLocation(new HelpLocation(HelpTopics.DECOMPILER, "ScopeFolding"));
	}

	@Override
	protected boolean isEnabledForDecompilerContext(DecompilerActionContext context) {
		if (!context.hasRealFunction()) {
			return false;
		}
		return operation != Operation.TOGGLE || context.getDecompilerPanel().getLayoutController()
				.getEnclosingBlock(context.getLineNumber() - 1) >= 0;
	}

	@Override
	protected void decompilerActionPerformed(DecompilerActionContext context) {
		ClangLayoutController model = context.getDecompilerPanel().getLayoutController();
		switch (operation) {
			case TOGGLE -> model.toggleFold(model.getEnclosingBlock(context.getLineNumber() - 1));
			case COLLAPSE_ALL -> model.setAllCollapsed(true);
			case EXPAND_ALL -> model.setAllCollapsed(false);
		}
	}
}
