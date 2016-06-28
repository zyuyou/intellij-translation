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

import com.intellij.openapi.components.*;
import com.intellij.translation.translator.Translator;
import com.intellij.translation.translator.YoudaoTranslator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("MethodMayStatic")
@State(name = "TranslationSettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/translation_settings.xml")})
public class TranslationSettings implements PersistentStateComponent<TranslationSettings.State> {
	private State myState = new State();

	@Nullable
	@Override
	public State getState() {
		return myState;
	}

	@Override
	public void loadState(State state) {
		myState = state;
	}

	public static TranslationSettings getInstance(){
		return ServiceManager.getService(TranslationSettings.class);
	}

	public static class State {
		public boolean enableSpecified = false;
		public Class<? extends Translator> specifiedTranslator = YoudaoTranslator.class;

		@NotNull
		public String youdaoKeyfrom = "IntellijTranslate";
		@NotNull
		public String youdaoApiKey = "1918103305";

		@Nullable
		public String googleApiKey;
	}

	public boolean isEnableSpecified(){
		return myState.enableSpecified;
	}
	public void setEnableSpecified(boolean bool){
		myState.enableSpecified = bool;
	}

	public Class<? extends Translator> getSpecifiedTranslator(){
		return myState.specifiedTranslator;
	}
	public void setSpecifiedTranslator(Class<? extends Translator> translator){
		myState.specifiedTranslator = translator;
	}

	public String getYoudaoKeyfrom(){
		return myState.youdaoKeyfrom;
	}
	public String getYoudaoApiKey(){
		return myState.youdaoApiKey;
	}
	public void setYoudaoKeyfromAndApiKey(String keyfrom, String apiKey){
		myState.youdaoKeyfrom = keyfrom;
		myState.youdaoApiKey = apiKey;
	}

	public String getGoogleApiKey(){
		return myState.googleApiKey;
	}
}
