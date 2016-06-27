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
package com.intellij.translation;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.io.UnsupportedEncodingException;

public class TranslationBundle extends AbstractBundle{
	public static final String PATH_TO_BUNDLE = "i18n.TranslationBundle";
	private static final TranslationBundle BUNDLE = new TranslationBundle();

	public static String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params){
		return BUNDLE.getMessage(key, params);
	}

	public static String message2(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params){
		String msg = message(key, params);
		try{
			return new String(msg.getBytes("ISO8859-1"), "utf-8");
		}catch (UnsupportedEncodingException ignore){
		}
		return msg;
	}

	public TranslationBundle() {
		super(PATH_TO_BUNDLE);
	}
}
