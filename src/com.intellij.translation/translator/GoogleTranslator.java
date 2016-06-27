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
package com.intellij.translation.translator;

import com.intellij.translation.icons.TranslationIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GoogleTranslator implements Translator {
	@NotNull
	@Override
	public String getTitle() {
		return "Google Translation";
	}

	@NotNull
	@Override
	public Icon getIcon() {
		return TranslationIcons.GOOGLE_SMALL;
	}


	@Nullable
	@Override
	public String fetchInfo(String query) {
		return null;
	}

	@Nullable
	@Override
	public String getExternalUrl(String query) {
		try{
			String finalQuery = URLEncoder.encode(query, "UTF-8");
			return "https://translate.google.cn/#auto/zh-CN/" + finalQuery;
		} catch (UnsupportedEncodingException ignore) {
		}
		return null;
	}
}
