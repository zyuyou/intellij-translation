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
package com.intellij.translation.settings.translator;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by zyuyou on 16/6/27.
 */
public class GoogleTranslatorForm {
	private JPanel myPanel;
	private JPasswordField myApiKeyText;

	public GoogleTranslatorForm() {
		// disable for now.
		myApiKeyText.setEnabled(false);
	}

	@Nullable
	public JComponent createComponent() {
		return myPanel;
	}

	public void setApiKey(@Nullable String apikey){
		if(apikey != null){
			myApiKeyText.setText(apikey);
		}
	}
}
