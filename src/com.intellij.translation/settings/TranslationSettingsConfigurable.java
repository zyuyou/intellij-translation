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
package com.intellij.translation.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.Comparing;
import com.intellij.translation.TranslationManager;
import com.intellij.translation.settings.translator.GoogleTranslatorForm;
import com.intellij.translation.settings.translator.YoudaoTranslatorForm;
import com.intellij.translation.translator.Translator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class TranslationSettingsConfigurable implements SearchableConfigurable, Configurable.NoScroll  {
	public static final String TRANSLATION_RELATED_TOOLS = "Translation";

	private TranslationSettings mySettings;

	private JPanel myPanel;
	private YoudaoTranslatorForm myYoudaoTranslatorForm;
	private JCheckBox myTranslatorCheckBox;
	private GoogleTranslatorForm myGoogleTranslatorForm;
	private JComboBox myTranslatorComboBox;

	public TranslationSettingsConfigurable() {
		mySettings = TranslationSettings.getInstance();

		//noinspection unchecked
		myTranslatorComboBox.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(value instanceof Translator){
					final Translator translator = (Translator)value;
					setText(translator.getTitle());
					setIcon(translator.getIcon());
				}else{
					setText((String)value);
				}
				return rendererComponent;
			}
		});

		for(Translator translator: TranslationManager.TRANSLATOR_EP.getExtensions()){
			//noinspection unchecked
			myTranslatorComboBox.addItem(translator);
		}

		if(mySettings.isEnableSpecified()){
			toggleEnableSpecified(true);

			if(mySettings.getSpecifiedTranslator() != null ){
				myTranslatorComboBox.setSelectedItem(mySettings.getSpecifiedTranslator());
			}
		}else{
			toggleEnableSpecified(false);
		}

		// youdao
		myYoudaoTranslatorForm.setApiKey(mySettings.getYoudaoKeyfrom(), mySettings.getYoudaoApiKey());

		// google
		myGoogleTranslatorForm.setApiKey(mySettings.getGoogleApiKey());
	}

	private void toggleEnableSpecified(boolean bool){
		myTranslatorCheckBox.setEnabled(bool);
		myTranslatorCheckBox.setSelected(bool);
		myTranslatorComboBox.setEnabled(bool);
	}

	@NotNull
	@Override
	public String getId() {
		return TRANSLATION_RELATED_TOOLS;
	}

	@Nullable
	@Override
	public Runnable enableSearch(String option) {
		return null;
	}

	@Nls
	@Override
	public String getDisplayName() {
		return TRANSLATION_RELATED_TOOLS;
	}

	@Nullable
	@Override
	public String getHelpTopic() {
		return null;
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		myYoudaoTranslatorForm.createComponent();
		myGoogleTranslatorForm.createComponent();
		return myPanel;
	}

	@Override
	public boolean isModified() {
		return !Comparing.equal(mySettings.isEnableSpecified(), myTranslatorCheckBox.isSelected()) ||
			!Comparing.equal(mySettings.getSpecifiedTranslator(), ((Translator)myTranslatorComboBox.getSelectedItem()).getClass()) ||
			myYoudaoTranslatorForm.isModified();
	}

	@Override
	public void apply() throws ConfigurationException {
		if(myYoudaoTranslatorForm.isModified()){
			mySettings.setYoudaoKeyfromAndApiKey(myYoudaoTranslatorForm.getKeyfrom(), myYoudaoTranslatorForm.getApiKey());
		}

		mySettings.setEnableSpecified(myTranslatorCheckBox.isSelected());
		mySettings.setSpecifiedTranslator(((Translator)myTranslatorComboBox.getSelectedItem()).getClass());

		myYoudaoTranslatorForm.resetApiKeyModifed();
	}

	@Override
	public void reset() {
		toggleEnableSpecified(false);

		myYoudaoTranslatorForm.setApiKey(mySettings.getYoudaoKeyfrom(), mySettings.getYoudaoApiKey());
		myYoudaoTranslatorForm.resetApiKeyModifed();

		myGoogleTranslatorForm.setApiKey(mySettings.getGoogleApiKey());
	}

	@Override
	public void disposeUIResources() {
	}
}
