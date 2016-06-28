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

import com.intellij.codeInsight.documentation.DockablePopupManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.BaseNavigateToSourceAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.gotoByName.ChooseByNameBase;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.translation.translator.Translator;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.content.Content;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.popup.PopupPositionManager;
import com.intellij.util.Alarm;
import com.intellij.util.BooleanFunction;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by zyuyou on 16/6/20.
 *
 * https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/codeInsight/documentation/DocumentationManager.java
 * @author JetBrains s.r.o.
 */
public class TranslationManager extends DockablePopupManager<TranslationComponent> {
	public static final ExtensionPointName<Translator> TRANSLATOR_EP = ExtensionPointName.create("com.intellij.translation.translator");

	@NonNls public static final String TRANSLATION_LOCATION_AND_SIZE = "com.intellij.translation.popup";
	public static final DataKey<String> SELECTED_QUICK_TRANSLATION_TEXT = DataKey.create("QUICK_TRANSLATION.SELECTED_TEXT");

	private static final Logger LOG = Logger.getInstance("#" + TranslationManager.class.getName());
	private static final String SHOW_TRANSLATION_IN_TOOL_WINDOW = "ShowTranslationInToolWindow";
	private static final String TRANSLATION_AUTO_UPDATE_ENABLED = "TranslationAutoUpdateEnabled";

	private Editor myEditor;
	private final Alarm myUpdateTranslationAlarm;
	private WeakReference<JBPopup> myTranslationHintRef;
	private Component myPreviouslyFocused;

	private final ActionManager myActionManager;

	private boolean myCloseOnSneeze;
	private ActionCallback myLastAction;
	private TranslationComponent myTestTranslationComponent;

	private AnAction myRestorePopupAction;

	public TranslationManager(final Project project, ActionManager manager) {
		super(project);

		myActionManager = manager;
		final AnActionListener actionListener = new AnActionListener() {
			@Override
			public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
				final JBPopup hint = getTranslationHint();
				if(hint != null){
					if(action instanceof HintManagerImpl.ActionToIgnore){
						((AbstractPopup)hint).focusPreferredComponent();
						return;
					}
					if(action instanceof ScrollingUtil.ListScrollAction) return;
					if(action == myActionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)) return;
					if(action == myActionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)) return;
					if(action == myActionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN)) return;
					if(action == myActionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP)) return;
					if(TranslationConstants.TRANSLATION_INPLACE_SETTINGS.equals(event.getPlace())) return;
					if(action instanceof BaseNavigateToSourceAction) return;
					closeTranslationHint();
				}
			}

			@Override
			public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
			}

			@Override
			public void beforeEditorTyping(char c, DataContext dataContext) {
				final JBPopup hint = getTranslationHint();
				// todo when typing
				if(hint != null){
					hint.cancel();
				}
			}
		};

		myActionManager.addAnActionListener(actionListener, project);
		myUpdateTranslationAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);
	}

	public static TranslationManager getInstance(Project project){
		return ServiceManager.getService(project, TranslationManager.class);
	}

	@Override
	protected String getShowInToolWindowProperty() {
		return SHOW_TRANSLATION_IN_TOOL_WINDOW;
	}

	@Override
	protected String getAutoUpdateEnabledProperty() {
		return TRANSLATION_AUTO_UPDATE_ENABLED;
	}

	@Override
	protected String getAutoUpdateTitle() {
		return "Auto-update";
	}

	@Override
	protected String getRestorePopupDescription() {
		return "Restore popup view mode";
	}

	@Override
	protected String getAutoUpdateDescription() {
		return "Refresh com.intellij.com.intellij.translation on selection change automatically";
	}

	@NotNull
	@Override
	protected AnAction createRestorePopupAction() {
		myRestorePopupAction = super.createRestorePopupAction();
		return myRestorePopupAction;
	}

	@Override
	protected void restorePopupBehavior() {
		if(myPreviouslyFocused != null){
			IdeFocusManager.getInstance(myProject).requestFocus(myPreviouslyFocused, true);
		}
		super.restorePopupBehavior();
		updateComponent();
	}

	@Override
	public void createToolWindow(PsiElement element, PsiElement originalElement) {
		super.createToolWindow(element, originalElement);

		// switch between toolWindow and popup
		if(myToolWindow != null){
			myToolWindow.getComponent().putClientProperty(ChooseByNameBase.TEMPORARILY_FOCUSABLE_COMPONENT_KEY, Boolean.TRUE);
			if(myRestorePopupAction != null){
				ShortcutSet quickTranslateShortCut = ActionManager.getInstance().getAction(TranslationConstants.ACTION_QUICK_TRANSLATE).getShortcutSet();
				myRestorePopupAction.registerCustomShortcutSet(quickTranslateShortCut, myToolWindow.getComponent());
				myRestorePopupAction = null;
			}
		}
	}



	@Override
	protected TranslationComponent createComponent() {
		return new TranslationComponent(this, createActions());
	}

	@Override
	protected void doUpdateComponent(PsiElement element, PsiElement originalElement, TranslationComponent component) {
		fetchTranslation(getDefaultCollector(myEditor.getSelectionModel().getSelectedText()), component);
	}

	@Override
	protected void doUpdateComponent(Editor editor, PsiFile psiFile) {
		showTranslation(editor, false, null);
	}

	@Override
	protected void doUpdateComponent(@NotNull PsiElement element) {
//		System.out.println("doUpdateComponent(@NotNull PsiElement element)");
	}

	@Override
	protected String getTitle(PsiElement element) {
		return getTitle(element, true);
	}

	static String getTitle(@Nullable final PsiElement element, final boolean _short) {
		return _short ? "": TranslationConstants.TOOL_WINDOW_ID;
	}

	@Override
	protected String getToolwindowId() {
		return TranslationConstants.TOOL_WINDOW_ID;
	}

	/**
	 * @return    <code>true</code> if quick doc control is configured to not prevent user-IDE interaction (e.g. should be closed if
	 *            the user presses a key);
	 *            <code>false</code> otherwise
	 */
	public boolean isMyCloseOnSneeze() {
		return myCloseOnSneeze;
	}

	@Nullable
	public JBPopup getTranslationHint() {
		if (myTranslationHintRef == null) return null;
		JBPopup hint = myTranslationHintRef.get();
		if (hint == null || !hint.isVisible() && !ApplicationManager.getApplication().isUnitTestMode()) {
			myTranslationHintRef = null;
			return null;
		}
		return hint;
	}

	private void closeTranslationHint() {
		JBPopup hint = getTranslationHint();
		if (hint == null) {
			return;
		}
		myCloseOnSneeze = false;
		hint.cancel();
		Component toFocus = myPreviouslyFocused;
		hint.cancel();
		if (toFocus != null) {
			IdeFocusManager.getInstance(myProject).requestFocus(toFocus, true);
		}
	}

	public void showTranslation(@NotNull Editor editor){
		showTranslation(editor, true, null);
	}

	public void showTranslation(@NotNull Editor editor, boolean requestFocus, @Nullable final Runnable closeCallback){
		myEditor = editor;

		SelectionModel selectionModel = editor.getSelectionModel();
		if(selectionModel.getSelectedText() != null){
			doShowTranslation(selectionModel.getSelectedText(), requestFocus, closeCallback);
		}
	}

	private void doShowTranslation(@NotNull String queryText, boolean requestFocus, @Nullable final Runnable closeCallback) {
		final Project project = myProject;
		if(!project.isOpen()) return;

		myPreviouslyFocused = WindowManagerEx.getInstanceEx().getFocusedComponent(project);

		JBPopup _oldHint = getTranslationHint();

		if(myToolWindow == null && PropertiesComponent.getInstance().isTrueValue(SHOW_TRANSLATION_IN_TOOL_WINDOW)){
			createToolWindow(null, null);
		}else if(myToolWindow != null){
			Content content = myToolWindow.getContentManager().getSelectedContent();
			if(content != null){
				TranslationComponent component = (TranslationComponent) content.getComponent();
				boolean samQuery = queryText.equals(component.getQuery());
				if(samQuery){
					JComponent preferredFocusableComponent = content.getPreferredFocusableComponent();
					// focus toolwindow on the second actionPerformed
					boolean focus = requestFocus || CommandProcessor.getInstance().getCurrentCommand() != null;
					if (preferredFocusableComponent != null && focus) {
						IdeFocusManager.getInstance(myProject).requestFocus(preferredFocusableComponent, true);
					}
				}else{
					content.setDisplayName(getTitle(null, true));
					fetchTranslation(getDefaultCollector(queryText), component, true);
				}
			}
			if(!myToolWindow.isVisible()){

					myToolWindow.show(null);
			}
		}else if(_oldHint != null && _oldHint.isVisible() && _oldHint instanceof AbstractPopup){
			TranslationComponent oldComponent = (TranslationComponent)((AbstractPopup) _oldHint).getComponent();
			fetchTranslation(getDefaultCollector(queryText), oldComponent);
		}else{
			showInPopup(queryText, requestFocus, closeCallback);
		}
	}

	private void showInPopup(@NotNull String queryText, boolean requestFocus, @Nullable final Runnable closeCallback){
		final TranslationComponent component = myTestTranslationComponent == null?
			new TranslationComponent(this) : myTestTranslationComponent;

		// todo NavigateCallback

		Processor<JBPopup> pinCallback = new Processor<JBPopup>() {
			@Override
			public boolean process(JBPopup popup) {
				TranslationManager.this.createToolWindow(null, null);
				myToolWindow.setAutoHide(false);
				popup.cancel();
				return false;
			}
		};

		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TranslationManager.this.createToolWindow(null, null);
				final JBPopup hint = TranslationManager.this.getTranslationHint();
				if (hint != null && hint.isVisible()) hint.cancel();
			}
		};

		List<Pair<ActionListener, KeyStroke>> actions = ContainerUtil.newSmartList();
		AnAction quickDocAction = ActionManager.getInstance().getAction(TranslationConstants.ACTION_QUICK_TRANSLATE);
		for (Shortcut shortcut : quickDocAction.getShortcutSet().getShortcuts()) {
			if (!(shortcut instanceof KeyboardShortcut)) continue;
			actions.add(Pair.create(actionListener, ((KeyboardShortcut)shortcut).getFirstKeyStroke()));
		}

		final JBPopup hint = JBPopupFactory.getInstance().createComponentPopupBuilder(component, component)
			.setProject(myProject)
			.setKeyboardActions(actions)
			.setDimensionServiceKey(myProject, TRANSLATION_LOCATION_AND_SIZE, false)
			.setResizable(true)
			.setMovable(true)
			.setRequestFocus(requestFocus)
			.setCancelOnClickOutside(true)
			.setTitle(getTitle(null, false))
			.setCouldPin(pinCallback)
			.setModalContext(false)
			.setCancelCallback(new Computable<Boolean>() {
				@Override
				public Boolean compute() {
					myCloseOnSneeze = false;
					if (closeCallback != null) {
						closeCallback.run();
					}
					Disposer.dispose(component);
					myEditor = null;
					myPreviouslyFocused = null;
					return Boolean.TRUE;
				}
			})
			.setKeyEventHandler(new BooleanFunction<KeyEvent>() {
				@Override
				public boolean fun(KeyEvent e) {
					if (myCloseOnSneeze) {
						TranslationManager.this.closeTranslationHint();
					}
					if (AbstractPopup.isCloseRequest(e) && TranslationManager.this.getTranslationHint() != null) {
						TranslationManager.this.closeTranslationHint();
						return true;
					}
					return false;
				}
			})
			.createPopup();

		component.setHint(hint);

		fetchTranslation(getDefaultCollector(queryText), component);

		myTranslationHintRef = new WeakReference<JBPopup>(hint);
	}

	public void fetchTranslation(final TranslationCollector provider, final TranslationComponent component){
		doFetchTranslation(component, provider, true, false);
	}

	public void fetchTranslation(final TranslationCollector provider, final TranslationComponent component, final boolean clearHistory){
		doFetchTranslation(component, provider, true, clearHistory);
	}

	private ActionCallback doFetchTranslation(final TranslationComponent component, final TranslationCollector provider, final boolean cancelRequests, final boolean clearHistory){
		final ActionCallback callback = new ActionCallback();
		myLastAction = callback;

		boolean wasEmpty = component.isEmpty();
		component.startWait();
		if(cancelRequests){
			myUpdateTranslationAlarm.cancelAllRequests();
		}
		if(wasEmpty){
			component.setText(TranslationBundle.message("translation.fetching.progress"), null, null);
			final AbstractPopup jbPopup = (AbstractPopup) getTranslationHint();
			if(jbPopup != null){
				jbPopup.setDimensionServiceKey(null);
			}
		}

		myUpdateTranslationAlarm.addRequest(new Runnable() {
			@Override
			public void run() {
				if(myProject.isDisposed()) return ;
				LOG.debug("Started fetching com.intellij.translation...");
				final Throwable[] ex = new Throwable[1];
				String text = null;
				try{
					text = provider.getTranslation();
				}catch (Throwable e){
					LOG.info(e);
					ex[0] = e;
				}
				if(ex[0] != null){
					//noinspection SSBasedInspection
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							String message = ex[0] instanceof IndexNotReadyException
								? "Translation is not available until indices are built."
								: TranslationBundle.message("translation.external.fetch.error.message");
							component.setText(message, null, null);
							callback.setDone();
						}
					});
					return;
				}

				LOG.debug("Translation fetched successfully:\n", text);

				final String translationText = text;

				//noinspection SSBasedInspection
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						PsiDocumentManager.getInstance(myProject).commitAllDocuments();

						if (translationText == null) {
							component.setText(TranslationBundle.message("translation.no.info.found"), provider.getQuery(), null);
						} else if (translationText.isEmpty()) {
							component.setText(component.getText(), provider.getQuery(), null);
						} else {
							component.setData(provider.getQuery(), translationText, provider.getTranslator());
						}

						final AbstractPopup jbPopup = (AbstractPopup) getTranslationHint();
						if (jbPopup == null) {
							callback.setDone();
							return;
						}

						jbPopup.setDimensionServiceKey(TRANSLATION_LOCATION_AND_SIZE);
						jbPopup.setCaption(getTitle(null, false));
						callback.setDone();
					}
				});
			}
		}, 10);

		return callback;
	}

	private TranslationCollector getDefaultCollector(final String queryText){
		return new DefaultTranslationCollector(queryText);
	}

	private interface TranslationCollector {
		@Nullable
		String getTranslation() throws Exception;
		@Nullable
		String getQuery();
		@Nullable
		String getExternalUrl();
		@Nullable
		Translator getTranslator();
	}

	private class DefaultTranslationCollector implements TranslationCollector {
		private final String myQuery;
		private String myExternalUrl;
		private Translator myTranslator;

		public DefaultTranslationCollector(String query) {
			this.myQuery = query;
		}

		@Nullable
		@Override
		public String getQuery() {
			return myQuery;
		}

		@Nullable
		@Override
		public String getExternalUrl() {
			return myExternalUrl;
		}

		@Nullable
		@Override
		public Translator getTranslator() {
			return myTranslator;
		}

		@Nullable
		@Override
		public String getTranslation() throws Exception {
			if(myQuery != null){
				for(Translator provider: TranslationManager.TRANSLATOR_EP.getExtensions()){
					final String translation = provider.fetchInfo(myQuery);
					if(translation != null){
						LOG.debug("Fetched translation from ", provider.getTitle());
						myExternalUrl = provider.getExternalUrl(myQuery);
						myTranslator = provider;
						return translation;
					}
				}
			}
			return null;
		}
	}

	void showHint(final JBPopup hint) {
		final Component focusOwner = IdeFocusManager.getInstance(myProject).getFocusOwner();
		DataContext dataContext = DataManager.getInstance().getDataContext(focusOwner);
		PopupPositionManager.positionPopupInBestPosition(hint, myEditor, dataContext);
	}

	public Project getProject() {
		return myProject;
	}

}
