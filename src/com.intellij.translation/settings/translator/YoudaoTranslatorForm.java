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

import com.intellij.ide.BrowserUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Created by zyuyou on 16/6/27.
 */
public class YoudaoTranslatorForm {
	private JPanel myPanel;
	private JTextField myKeyfromText;
	private JPasswordField myApiKeyText;
	private JTextPane myGenApiKeyText;

	private boolean myApiKeyModified;

	public YoudaoTranslatorForm() {
		myGenApiKeyText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			protected void hyperlinkActivated(HyperlinkEvent e) {
				BrowserUtil.browse(e.getURL());
			}
		});
		myGenApiKeyText.setText("<html>Do not have an api key at fanyi.youdao.com? <a href=\"http://fanyi.youdao.com/openapi?path=data-mode\">" + "Apply one" + "</a></html>");
		myGenApiKeyText.setBackground(myPanel.getBackground());
		myGenApiKeyText.setCursor(new Cursor(Cursor.HAND_CURSOR));

		myKeyfromText.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			protected void textChanged(DocumentEvent e) {
				if(!myApiKeyModified){
					eraseApiKey();
				}
			}
		});

		myApiKeyText.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			protected void textChanged(DocumentEvent e) {
				myApiKeyModified = true;
			}
		});
		myApiKeyText.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if(!myApiKeyModified && !getApiKey().isEmpty()){
					eraseApiKey();
				}
			}
			@Override
			public void focusLost(FocusEvent e) {
			}
		});

	}

	@Nullable
	public JComponent createComponent() {
		return myPanel;
	}

	private void eraseApiKey() {
		myApiKeyText.setText("");
		myApiKeyModified = true;
	}

	public void setApiKey(@NotNull String keyfrom, @NotNull String apikey){
		myKeyfromText.setText(keyfrom);
		myApiKeyText.setText(apikey);
	}

	public String getKeyfrom(){
		return myKeyfromText.getText();
	}

	public String getApiKey(){
		return String.valueOf(myApiKeyText.getPassword());
	}

	public boolean isModified(){
		return myApiKeyModified;
	}

	public void resetApiKeyModifed(){
		myApiKeyModified = false;
	}
}
