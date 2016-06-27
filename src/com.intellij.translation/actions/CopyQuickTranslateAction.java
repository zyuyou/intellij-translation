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

import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.translation.TranslationManager;

import java.awt.datatransfer.StringSelection;

public class CopyQuickTranslateAction extends AnAction implements DumbAware, HintManagerImpl.ActionToIgnore {

  public CopyQuickTranslateAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    String selected = e.getData(TranslationManager.SELECTED_QUICK_TRANSLATION_TEXT);
    if (selected == null || selected.isEmpty()) {
      return;
    }

    CopyPasteManager.getInstance().setContents(new StringSelection(selected));
  }

  @Override
  public void update(AnActionEvent e) {
    String selected = e.getData(TranslationManager.SELECTED_QUICK_TRANSLATION_TEXT);
    e.getPresentation().setEnabled(selected != null && !selected.isEmpty());
  }
}
