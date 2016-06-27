/*
 * Copyright 2016 Yuyou Chow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.translation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.translation.TranslationManager;
import org.jetbrains.annotations.NotNull;

/**
 * Created by zyuyou on 16/6/21.
 *
 * https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/codeInsight/documentation/actions/ShowQuickDocInfoAction.java
 * @author JetBrains s.r.o.
 */
public class ShowQuickTranslateAction extends BaseCodeInsightAction implements HintManagerImpl.ActionToIgnore, DumbAware, PopupAction {
	public ShowQuickTranslateAction() {
		setEnabledInModalContext(true);
		setInjectedContext(true);
	}

	@NotNull
	@Override
	protected CodeInsightActionHandler getHandler() {
		return new CodeInsightActionHandler() {
			@Override
			public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
				TranslationManager.getInstance(project).showTranslation(editor);
			}
			@Override
			public boolean startInWriteAction() {
				return false;
			}
		};
	}

	@Override
	protected boolean isValidForLookup() {
		return true;
	}

	@Override
	public void update(AnActionEvent event) {
		Presentation presentation = event.getPresentation();
		DataContext dataContext = event.getDataContext();

		presentation.setEnabled(false);

		Project project = CommonDataKeys.PROJECT.getData(dataContext);
		if(project != null){
			Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
			if(editor != null){
				SelectionModel selectionModel = editor.getSelectionModel();
				if(selectionModel.getSelectedText() != null){
					presentation.setEnabled(true);
				}
			}
		}
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		DataContext dataContext = e.getDataContext();
		final Project project = CommonDataKeys.PROJECT.getData(dataContext);
		final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);

		if(project != null && editor != null){
			actionPerformedImpl(project, editor);
		}
	}
}
