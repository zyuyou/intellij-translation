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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;

import static com.intellij.translation.TranslationConstants.REQUEST_TIMEOUT;

public class TranslatorUtil {
	public static CloseableHttpClient createClient(){
		HttpClientBuilder builder = HttpClients.custom();

		return builder.setDefaultRequestConfig(
			RequestConfig.custom().
				setSocketTimeout(REQUEST_TIMEOUT).
				setConnectTimeout(REQUEST_TIMEOUT).
				setConnectionRequestTimeout(REQUEST_TIMEOUT).
				build()
		).build();
	}

	public static String fetchInfo(String query, TranslatorEx translator){
		final CloseableHttpClient client = TranslatorUtil.createClient();
		try{
			final URI queryUrl = translator.createUrl(query);
			HttpGet httpGet = new HttpGet(queryUrl);
			HttpResponse response = client.execute(httpGet);
			int status = response.getStatusLine().getStatusCode();
			if(status >= 200 && status < 300){
				HttpEntity resEntity = response.getEntity();
				return translator.generateSuccess(resEntity);
			}else{
				return translator.generateFail(response);
			}
		}catch (Exception ignore){
		}finally {
			try{
				client.close();
			}catch (IOException ignore){
			}
		}
		return null;
	}
}
