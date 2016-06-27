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

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.translation.TranslationManager;
import com.intellij.translation.translator.Translator;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ExternalTranslationAction extends AnAction {
	public ExternalTranslationAction() {
		setInjectedContext(true);
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
			SelectionModel selectionModel = editor.getSelectionModel();
			String query = selectionModel.getSelectedText();
			if(query == null) return;

			showExternalTranslation(query, null, dataContext);
		}
	}

	public static void showExternalTranslation(String query, String externalUrl, DataContext dataContext){
		final Component contextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);

		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			final List<String> urls = new ArrayList<String>();
			final List<Icon> icons = new ArrayList<Icon>();

			if(StringUtil.isEmptyOrSpaces(externalUrl)){
				for(Translator translator: TranslationManager.TRANSLATOR_EP.getExtensions()){
					String url = translator.getExternalUrl(query);
					if(url != null){
						urls.add(url);
						icons.add(translator.getIcon());
					}
				}
			}else{
				urls.add(externalUrl);
			}

			ApplicationManager.getApplication().invokeLater( () -> {
				if(ContainerUtil.isEmpty(urls)){
					// do nothing
				}else if(urls.size() == 1){
					BrowserUtil.browse(urls.get(0));
				}else{
					JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<String>("Choose external translation root",
						ArrayUtil.toStringArray(urls), ArrayUtil.toObjectArray(icons, Icon.class)) {
						@Override
						public PopupStep onChosen(final String selectedValue, final boolean finalChoice) {
							BrowserUtil.browse(selectedValue);
							return FINAL_CHOICE;
						}
					}).showInBestPositionFor(DataManager.getInstance().getDataContext(contextComponent));
				}

			}, ModalityState.NON_MODAL);
		});
	}


}
