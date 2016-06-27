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

import com.google.common.base.Joiner;

import java.util.List;
import java.util.Map;

/**
 * Created by zyuyou on 16/6/23.
 *
 * https://github.com/Skykai521/ECTranslation/blob/master/src/Translation.java
 * @author loucyin
 */
@SuppressWarnings("unchecked")
public class YoudaoTranslation {
	private final static String US_PHONETIC = "us-phonetic";
	private final static String UK_PHONETIC = "uk-phonetic";
	private final static String PHONETIC = "phonetic";
	private final static String EXPLAINS = "explains";

	private final static int SUCCESS = 0;
	private final static int QUERY_STRING_TOO_LONG = 20;
	private final static int CAN_NOT_TRANSLATE = 30;
	private final static int INVALID_LANGUAGE = 40;
	private final static int INVALID_KEY = 50;
	private final static int NO_RESULT = 60;

	private String[] translation;
	private String query;
	private int errorCode;
	private Map<String, Object> basic;
	private List<Map<String, Object>> web;

	private String getErrorMessage(){
		switch (errorCode){
			case SUCCESS:
				return "成功";
			case QUERY_STRING_TOO_LONG:
				return "要翻译的文本过长";
			case CAN_NOT_TRANSLATE:
				return "无法进行有效的翻译";
			case INVALID_LANGUAGE:
				return "不支持的语言类型";
			case INVALID_KEY:
				return "无效的key";
			case NO_RESULT:
				return "无词典结果";
			default:
				return "Unknown Error Code: " + errorCode;
		}
	}

	private String getPhonetic() {
		if (basic == null) {
			return null;
		}
		String result = null;
		String us_phonetic = (String) basic.get(US_PHONETIC);
		String uk_phonetic = (String) basic.get(UK_PHONETIC);
		if (us_phonetic == null && uk_phonetic == null) {
			String phonetic = (String) basic.get(PHONETIC);
			if(phonetic != null){
				result = "<span class=\"pronounce\"> 拼音：<span class=\"phonetic\">[" + (String) basic.get(PHONETIC) + "]</span></span>";
			}
		} else {
			if (us_phonetic != null) {
				result = "<span class=\"pronounce\"> 美式：<span class=\"phonetic\">[" + us_phonetic + "]</span></span>";
			}
			if (uk_phonetic != null) {
				if (result == null) {
					result = "";
				}
				result = result + "<span class=\"pronounce\"> 英式：<span class=\"phonetic\">[" + uk_phonetic + "]</span></span>";
			}
		}
		return result;
	}

	private String getExplains() {
		if (basic == null) {
			return null;
		}
		String result = null;
		List<String> explains = (List<String>) basic.get(EXPLAINS);
		if (explains.size() > 0) {
			result = "";
		}
		for (String explain : explains) {
			result += String.format("<li>%s</li>", explain);
		}
		return result;
	}

	private String getTranslationResult() {
		if (translation == null) {
			return null;
		}
		String result = null;
		if (translation.length > 0) {
			result = "";
		}
		for (String r : translation) {
			result += (r + ";");
		}
		return result;
	}

	private String getWebResult() {
		if (web == null) {
			return null;
		}
		String result = null;
		if (web.size() > 0) {
			result = "";
		}
		for (Map<String, Object> map : web) {
			String key = (String) map.get("key");
			List<String> value = (List<String>) map.get("value");
			result += String.format("<li>%s : %s</li>", key, Joiner.on(",").skipNulls().join(value));
		}
		return result;
	}

	@Override
	public String toString() {
		String result = null;
		if (errorCode != SUCCESS) {
			result = "错误代码：" + errorCode + "\n" + getErrorMessage();
		} else {
			String translation = getTranslationResult();
			if (translation != null) {
				translation = translation.substring(0, translation.length() - 1);
				result = String.format("<div class=\"trans\">%s : %s</div>", query, translation);

				if (getPhonetic() != null) {
					result += String.format("<div class=\"baav\">%s</div>", getPhonetic());
				}
				if (getExplains() != null) {
					result += String.format("<div class=\"trans-container\"><ul>%s</ul></div>", getExplains());;
				}
			}

			if (getWebResult() != null) {
				if (result == null) {
					result = "";
				}
				result += "<div class=\"web-trans\">网络释义</div>";

				result += String.format("<div class=\"trans-container\"><ul>%s</ul></div>", getWebResult());
			}
		}

		if (result == null) {
			result = "<h3>你选的内容：<span>" + query + "</span> 抱歉,翻译不了...</h3>";
		}
		return result;
	}
}
