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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by zyuyou on 16/6/28.
 */
public abstract class TranslatorEx implements Translator{
	@NotNull
	public abstract URI createUrl(String query) throws URISyntaxException;
	@Nullable
	public abstract String generateSuccess(HttpEntity entity) throws IOException;
	@Nullable
	public abstract String generateFail(HttpResponse response);
}
