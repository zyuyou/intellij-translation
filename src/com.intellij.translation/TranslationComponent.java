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

import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.options.FontSize;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.translation.translator.Translator;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by zyuyou on 16/6/21.
 *
 * https://github.com/JetBrains/intellij-community/blob/master/platform/lang-impl/src/com/intellij/codeInsight/documentation/DocumentationComponent.java
 * @author JetBrains s.r.o.
 */
public class TranslationComponent extends JPanel implements Disposable, DataProvider {
	private static Logger LOGGER = Logger.getInstance(TranslationComponent.class);

	private static final int PREFERRED_WIDTH_EM = 37;
	private static final int PREFERRED_HEIGHT_MIN_EM = 7;
	private static final int PREFERRED_HEIGHT_MAX_EM = 20;

	private TranslationManager myManager;
	private String myQuery;

	private final ActionToolbar myToolBar;
	private volatile boolean myIsEmpty;
	private boolean myIsShown;
	private final JLabel myTranslatorLabel;
	private final MutableAttributeSet myFontSizeStyle = new SimpleAttributeSet();
	private JSlider myFontSizeSlider;
	private final JComponent mySettingsPanel;
	private final MyShowSettingsButton myShowSettingsButton;
	private boolean myIgnoreFontSizeSliderChange;
	private String myExternalUrl;
	private Translator myTranslator;

	private final JScrollPane myScrollPane;
	private final JEditorPane myEditorPane;
	private String myText;  // myEditorPane.getText() surprisingly crashes.., let's cache the text
	private final JPanel myControlPanel;
	private boolean myControlPanelVisible;
	private final ExternalTranslationAction myExternalTranslationAction;

	private JBPopup myHint;

	private final Map<KeyStroke, ActionListener> myKeyboardActions = new HashMap<KeyStroke, ActionListener>();

	public TranslationComponent(final TranslationManager manager){
		this(manager, null);
	}

	public TranslationComponent(final TranslationManager manager, final AnAction[] additionalActions) {
		myManager = manager;
		myIsEmpty = true;
		myIsShown = false;

		myEditorPane = new JEditorPane(UIUtil.HTML_MIME, ""){
			@Override
			public Dimension getPreferredScrollableViewportSize() {
				int em = myEditorPane.getFont().getSize();
				int prefWidth = PREFERRED_WIDTH_EM * em;
				int prefHeightMin = PREFERRED_HEIGHT_MIN_EM * em;
				int prefHeightMax = PREFERRED_HEIGHT_MAX_EM * em;

				if(getWidth() == 0 || getHeight() == 0){
					setSize(prefWidth, prefHeightMax);
				}

				Insets ins = myEditorPane.getInsets();
				View rootView = myEditorPane.getUI().getRootView(myEditorPane);
				rootView.setSize(prefWidth, prefHeightMax); // Necessary! Without this line, the size won't increase when the content does

				int prefHeight = (int)rootView.getPreferredSpan(View.Y_AXIS) + ins.bottom + ins.top +
					myScrollPane.getHorizontalScrollBar().getMaximumSize().height;

				prefHeight = Math.max(prefHeightMin, Math.min(prefHeightMax, prefHeight));
				return new Dimension(prefWidth, prefHeight);
			}

			{
				enableEvents(AWTEvent.KEY_EVENT_MASK);
			}

			@Override
			protected void processKeyEvent(KeyEvent e) {
				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				ActionListener listener = myKeyboardActions.get(keyStroke);
				if(listener != null){
					listener.actionPerformed(new ActionEvent(TranslationComponent.this, 0, ""));
					e.consume();
					return;
				}
				super.processKeyEvent(e);
			}

			@Override
			public void paintComponents(Graphics g) {
				GraphicsUtil.setupAntialiasing(g);
				super.paintComponents(g);
			}
		};

		myText = "";
		myEditorPane.setEditable(false);
		myEditorPane.setBackground(HintUtil.INFORMATION_COLOR);
		myEditorPane.setEditorKit(UIUtil.getHTMLEditorKit(false));

		myScrollPane = new JBScrollPane(myEditorPane){
			@Override
			protected void processMouseWheelEvent(MouseWheelEvent e) {
				if(!EditorSettingsExternalizable.getInstance().isWheelFontChangeEnabled() || !EditorUtil.isChangeFontSize(e)){
					super.processMouseWheelEvent(e);
					return;
				}

				int change = Math.abs(e.getWheelRotation());
				boolean increase = e.getWheelRotation() <= 0;
				EditorColorsManager colorsManager = EditorColorsManager.getInstance();
				EditorColorsScheme scheme = colorsManager.getGlobalScheme();
				FontSize newFontSize = scheme.getQuickDocFontSize();
				for(; change > 0; change --){
					if(increase){
						newFontSize = newFontSize.larger();
					}else{
						newFontSize = newFontSize.smaller();
					}
				}

				if(newFontSize == scheme.getQuickDocFontSize()){
					return;
				}

				scheme.setQuickDocFontSize(newFontSize);
				applyFontSize();
				setFontSizeSliderSize(newFontSize);
			}
		};

		myScrollPane.setBorder(null);

		final MouseListener mouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				myShowSettingsButton.hideSettings();
			}
		};
		myEditorPane.addMouseListener(mouseAdapter);

		Disposer.register(this, new Disposable() {
			@Override
			public void dispose() {
				myEditorPane.removeMouseListener(mouseAdapter);
			}
		});

		final FocusListener focusAdapter = new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				Component previouslyFocused = WindowManagerEx.getInstanceEx().getFocusedComponent(manager.getProject());

				if(previouslyFocused != myEditorPane){
					if(myHint != null && !myHint.isDisposed()){
						myHint.cancel();
					}
				}
			}
		};
		myEditorPane.addFocusListener(focusAdapter);

		Disposer.register(this, new Disposable() {
			@Override
			public void dispose() {
				myEditorPane.removeFocusListener(focusAdapter);
			}
		});

		setLayout(new BorderLayout());
		JLayeredPane layeredPane = new JBLayeredPane(){
			@Override
			public void doLayout() {
				final Rectangle r = getBounds();
				for(Component component: getComponents()){
					if(component instanceof JScrollPane){
						component.setBounds(0, 0, r.width, r.height);
					}else{
						int insets = 2;
						Dimension d = component.getPreferredSize();
						component.setBounds(r.width - d.width - insets, insets, d.width, d.height);
					}
				}
			}

			@Override
			public Dimension getPreferredSize() {
				Dimension editorPaneSize = myEditorPane.getPreferredScrollableViewportSize();
				Dimension controlPanelSize = myControlPanel.getPreferredSize();
				return getSize(editorPaneSize, controlPanelSize);
			}

			@Override
			public Dimension getMinimumSize() {
				Dimension editorPaneSize = new JBDimension(20, 20);
				Dimension controlPanelSize = myControlPanel.getMinimumSize();
				return getSize(editorPaneSize, controlPanelSize);
			}

			private Dimension getSize(Dimension editorPaneSize, Dimension controlPanelSize){
				return new Dimension(Math.max(editorPaneSize.width, controlPanelSize.width), editorPaneSize.height + controlPanelSize.height);
			}
		};
		layeredPane.add(myScrollPane);
		layeredPane.setLayer(myScrollPane, 0);

		mySettingsPanel = createSettingsPanel();
		layeredPane.add(mySettingsPanel);
		layeredPane.setLayer(mySettingsPanel, JLayeredPane.POPUP_LAYER);
		add(layeredPane, BorderLayout.CENTER);
		setOpaque(true);
		myScrollPane.setViewportBorder(JBScrollPane.createIndentBorder());

		final DefaultActionGroup actions = new DefaultActionGroup();
		actions.add(myExternalTranslationAction = new ExternalTranslationAction());

		myExternalTranslationAction.registerCustomShortcutSet(CustomShortcutSet.fromString("UP"), this);

		if(additionalActions != null){
			for(final AnAction action : additionalActions){
				actions.add(action);
				ShortcutSet shortcutSet = action.getShortcutSet();
				if(shortcutSet != null){
					action.registerCustomShortcutSet(shortcutSet, this);
				}
			}
		}

		myToolBar = ActionManager.getInstance().createActionToolbar(ActionPlaces.JAVADOC_TOOLBAR, actions, true);

		myControlPanel = new JPanel(new BorderLayout(5, 5));
		myControlPanel.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));

		myTranslatorLabel = new JLabel();
		myTranslatorLabel.setMinimumSize(new Dimension(100, 0)); // do not recalculate size according to the text

		myControlPanel.add(myToolBar.getComponent(), BorderLayout.WEST);
		myControlPanel.add(myTranslatorLabel, BorderLayout.CENTER);
		myControlPanel.add(myShowSettingsButton = new MyShowSettingsButton(), BorderLayout.EAST);
		myControlPanelVisible = false;

		registerActions();

		updateControlState();
	}

	@Override
	public void dispose() {
		myKeyboardActions.clear();
		myManager = null;
		myHint = null;
	}

	@Nullable
	@Override
	public Object getData(@NonNls String dataId) {
		if(TranslationManager.SELECTED_QUICK_TRANSLATION_TEXT.getName().equals(dataId)){
			// Javadocs often contain &nbsp; symbols (non-breakable white space). We don't want to copy them as is and replace
			// with raw white spaces. See IDEA-86633 for more details.
			String selectedText = myEditorPane.getSelectedText();
			return selectedText == null? null : selectedText.replace((char)160, ' ');
		}
		return null;
	}

	@Override
	public boolean requestFocusInWindow() {
		return myScrollPane.requestFocusInWindow();
	}

	@Override
	public void requestFocus() {
		myScrollPane.requestFocus();
	}

	private class MyShowSettingsButton extends ActionButton {
		private MyShowSettingsButton() {
			this(new MyShowSettingsAction(), new Presentation(), TranslationConstants.TRANSLATION_INPLACE_SETTINGS, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
		}

		private MyShowSettingsButton(AnAction action, Presentation presentation, String place, @NotNull Dimension minimumSize) {
			super(action, presentation, place, minimumSize);
			myPresentation.setIcon(AllIcons.General.SecondaryGroup);
		}

		private void hideSettings() {
			if (!mySettingsPanel.isVisible()) {
				return;
			}
			AnActionEvent event = AnActionEvent.createFromDataContext(myPlace, myPresentation, DataContext.EMPTY_CONTEXT);
			myAction.actionPerformed(event);
		}
	}

	private class MyShowSettingsAction extends ToggleAction {
		@Override
		public boolean isSelected(AnActionEvent e) {
			return mySettingsPanel.isVisible();
		}

		@Override
		public void setSelected(AnActionEvent e, boolean state) {
			if (!state) {
				mySettingsPanel.setVisible(false);
				return;
			}

			EditorColorsManager colorsManager = EditorColorsManager.getInstance();
			EditorColorsScheme scheme = colorsManager.getGlobalScheme();
			// use quick doc font size
			setFontSizeSliderSize(scheme.getQuickDocFontSize());
			mySettingsPanel.setVisible(true);
		}
	}

	private void applyFontSize(){
		Document document = myEditorPane.getDocument();
		if(!(document instanceof StyledDocument)){
			return;
		}

		final StyledDocument styledDocument = (StyledDocument)document;

		EditorColorsManager colorsManager = EditorColorsManager.getInstance();
		EditorColorsScheme scheme = colorsManager.getGlobalScheme();
		StyleConstants.setFontSize(myFontSizeStyle, JBUI.scale(scheme.getQuickDocFontSize().getSize()));
		if(Registry.is("documentation.component.editor.font")){
			StyleConstants.setFontFamily(myFontSizeStyle, scheme.getEditorFontName());
		}

		ApplicationManager.getApplication().executeOnPooledThread(
			new Runnable() {
				@Override
				public void run() {
					styledDocument.setCharacterAttributes(0, styledDocument.getLength(), myFontSizeStyle, false);
				}
			}
		);
	}

	private void setFontSizeSliderSize(FontSize fontSize) {
		myIgnoreFontSizeSliderChange = true;
		try {
			FontSize[] sizes = FontSize.values();
			for (int i = 0; i < sizes.length; i++) {
				if (fontSize == sizes[i]) {
					myFontSizeSlider.setValue(i);
					break;
				}
			}
		}
		finally {
			myIgnoreFontSizeSliderChange = false;
		}
	}

	private class ExternalTranslationAction extends AnAction implements HintManagerImpl.ActionToIgnore {
		private ExternalTranslationAction() {
			super("View External Dictionary", null, AllIcons.Actions.Browser_externalJavaDoc);
			registerCustomShortcutSet(ActionManager.getInstance().getAction(TranslationConstants.ACTION_EXTERNAL_TRANSLATION).getShortcutSet(), null);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			if (myQuery == null) {
				return;
			}
			com.intellij.translation.actions.ExternalTranslationAction.showExternalTranslation(myQuery, myTranslator.getExternalUrl(myQuery), e.getDataContext());
		}

		@Override
		public void update(AnActionEvent e) {
			final Presentation presentation = e.getPresentation();
			presentation.setEnabled(false);
			if(myQuery != null){
				presentation.setEnabled(true);
			}
		}
	}

	@Nullable
	public String getQuery(){
		return myQuery != null ? myQuery : null;
	}

	@Nullable
	private HTMLDocument.Iterator getLink(int n) {
		if (n >= 0) {
			HTMLDocument document = (HTMLDocument)myEditorPane.getDocument();
			int linkCount = 0;
			for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.A); it.isValid(); it.next()) {
				if (it.getAttributes().isDefined(HTML.Attribute.HREF) && linkCount++ == n) return it;
			}
		}
		return null;
	}

	private JComponent createSettingsPanel(){
		JPanel result = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		result.add(new JLabel(ApplicationBundle.message("label.font.size")));
		myFontSizeSlider = new JSlider(SwingConstants.HORIZONTAL, 0, FontSize.values().length - 1, 3);
		myFontSizeSlider.setMinorTickSpacing(1);
		myFontSizeSlider.setPaintTicks(true);
		myFontSizeSlider.setPaintTrack(true);
		myFontSizeSlider.setSnapToTicks(true);
		UIUtil.setSliderIsFilled(myFontSizeSlider, true);
		result.add(myFontSizeSlider);
		result.setBorder(BorderFactory.createLineBorder(JBColor.border(), 1));

		myFontSizeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if(myIgnoreFontSizeSliderChange){
					return;
				}

				EditorColorsManager colorsManager = EditorColorsManager.getInstance();
				EditorColorsScheme scheme = colorsManager.getGlobalScheme();
				scheme.setQuickDocFontSize(FontSize.values()[myFontSizeSlider.getValue()]);
				applyFontSize();
			}
		});

		String tooltipText = ApplicationBundle.message("quickdoc.tooltip.font.size.by.wheel");
		result.setToolTipText(tooltipText);
		myFontSizeSlider.setToolTipText(tooltipText);
		result.setVisible(false);
		result.setOpaque(true);
		myFontSizeSlider.setOpaque(true);
		return result;
	}

	private void updateControlState() {
		if(myTranslator == null){
			myTranslatorLabel.setText("");
			myTranslatorLabel.setIcon(null);
		}else{
			myTranslatorLabel.setText(myTranslator.getTitle());
			myTranslatorLabel.setIcon(myTranslator.getIcon());
		}

		myToolBar.updateActionsImmediately(); // update faster
		setControlPanelVisible(true);   //(!myBackStack.isEmpty() || !myForwardStack.isEmpty());
	}

	private void setControlPanelVisible(boolean visible) {
		if (visible == myControlPanelVisible) return;
		if (visible) {
			add(myControlPanel, BorderLayout.NORTH);
		}
		else {
			remove(myControlPanel);
		}
		myControlPanelVisible = visible;
	}

	private void registerActions(){
		myExternalTranslationAction.registerCustomShortcutSet(ActionManager.getInstance().getAction(TranslationConstants.ACTION_EXTERNAL_TRANSLATION).getShortcutSet(), myEditorPane);

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
				int value = scrollBar.getValue() - scrollBar.getUnitIncrement(-1);
				value = Math.max(value, 0);
				scrollBar.setValue(value);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
				int value = scrollBar.getValue() + scrollBar.getUnitIncrement(+1);
				value = Math.min(value, scrollBar.getMaximum());
				scrollBar.setValue(value);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
				int value = scrollBar.getValue() - scrollBar.getUnitIncrement(-1);
				value = Math.max(value, 0);
				scrollBar.setValue(value);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
				int value = scrollBar.getValue() + scrollBar.getUnitIncrement(+1);
				value = Math.min(value, scrollBar.getMaximum());
				scrollBar.setValue(value);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
				int value = scrollBar.getValue() - scrollBar.getBlockIncrement(-1);
				value = Math.max(value, 0);
				scrollBar.setValue(value);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
				int value = scrollBar.getValue() + scrollBar.getBlockIncrement(+1);
				value = Math.min(value, scrollBar.getMaximum());
				scrollBar.setValue(value);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
				scrollBar.setValue(0);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getHorizontalScrollBar();
				scrollBar.setValue(scrollBar.getMaximum());
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_MASK), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
				scrollBar.setValue(0);
			}
		});

		myKeyboardActions.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_MASK), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JScrollBar scrollBar = myScrollPane.getVerticalScrollBar();
				scrollBar.setValue(scrollBar.getMaximum());
			}
		});
	}

	public void setHint(JBPopup hint) {
		myHint = hint;
	}

	public boolean isEmpty() {
		return myIsEmpty;
	}

	public void startWait() {
		myIsEmpty = true;
	}

	public void setText(String text, String query, Translator translator){
		updateControlState();
		setData(query, text, translator);

		myIsEmpty = false;
	}

	public void setData(String query, String text, final Translator translator){
		myTranslator = translator;

		setQuery(query);

		myIsEmpty = false;
		updateControlState();
		setDataInternal(query, text, new Rectangle(0, 0));
	}

	private void setQuery(String query){
		myQuery = query;
	}

	private void setDataInternal(String query, String text, final Rectangle viewRect){
		setQuery(query);

		myEditorPane.setText(text);
		applyFontSize();

		if(!myIsShown && myHint != null && !ApplicationManager.getApplication().isUnitTestMode()){
			myManager.showHint(myHint);
			myIsShown = true;
		}
		myText = text;
		//noinspection SSBasedInspection
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				myEditorPane.scrollRectToVisible(viewRect); // if ref is defined but is not found in document, this provides a default location
//			if (ref != null) {
//				myEditorPane.scrollToReference(ref);
//			}
			}
		});
	}

	public String getText() {
		return myText;
	}
}
