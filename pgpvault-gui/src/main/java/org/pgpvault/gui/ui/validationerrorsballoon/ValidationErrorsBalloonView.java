package org.pgpvault.gui.ui.validationerrorsballoon;

import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.copied.colin.mummery.VerticalLayout;
import org.pgpvault.gui.app.EntryPoint;
import org.pgpvault.gui.ui.tools.UiUtils;
import org.summerb.approaches.i18n.I18nUtils;
import org.summerb.approaches.validation.ValidationError;

import com.google.common.base.Preconditions;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.RoundedBalloonStyle;
import ru.skarpushin.swingpm.base.View;
import ru.skarpushin.swingpm.bindings.Binding;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExEventListener;

public class ValidationErrorsBalloonView
		implements View<ListEx<ValidationError>>, ListExEventListener<ValidationError>, Binding {

	private static Color errorBackgroundColor = new Color(255, 230, 230);
	private RoundedBalloonStyle balloonStyle;

	private JComponent component;
	private Border originalBorder;
	private Border errorBorder;

	private BalloonTip balloon;
	private JPanel panelMessages;

	private ListEx<ValidationError> pm;

	private boolean contentsInitedAfterErrorsUpdate;

	public ValidationErrorsBalloonView() {
		panelMessages = new JPanel(new VerticalLayout(3, VerticalLayout.BOTH, VerticalLayout.TOP));
		panelMessages.setBackground(errorBackgroundColor);
		panelMessages.setOpaque(true);

		balloonStyle = new RoundedBalloonStyle(5, 5, errorBackgroundColor, new Color(255, 0, 0));
	}

	@Override
	public boolean isAttached() {
		return pm != null;
	}

	@Override
	public void detach() {
		setPm(null);
	}

	@Override
	public void setPm(ListEx<ValidationError> newPm) {
		// Unbind
		if (pm != null) {
			if (component != null) {
				component.setBorder(originalBorder);
			}
			hideBalloon();
			panelMessages.removeAll();
			pm.removeListExEventListener(this);
		}

		// Bind
		pm = newPm;
		if (pm != null) {
			pm.addListExEventListener(this);
			initiallyProcessExistingErrors();
		}
	}

	@Override
	public ListEx<ValidationError> getPm() {
		return pm;
	}

	@Override
	public void renderTo(Container target) {
		throw new IllegalStateException("Not supported operation");
	}

	@Override
	public void renderTo(Container target, Object constraints) {
		Preconditions.checkState(component == null);
		Preconditions.checkArgument(constraints != null);
		Preconditions.checkArgument(constraints instanceof JComponent);

		component = (JComponent) constraints;
		component.addMouseListener(toolTipMouseAdapter);
		originalBorder = component.getBorder();
		errorBorder = buildErrorBorder(component);

		balloon = new BalloonTip(component, panelMessages, balloonStyle, false);
		balloon.setPadding(0);
		hideBalloon();
	}

	@Override
	public void unrender() {
		hideBalloon();
		balloon = null;
		component.removeMouseListener(toolTipMouseAdapter);
		component.setBorder(originalBorder);
		component = null;
	}

	private static Border buildErrorBorder(JComponent component) {
		if (component.getBorder() == null) {
			return BorderFactory.createLineBorder(Color.RED, 1);
		} else {
			Insets borderInsets = component.getBorder().getBorderInsets(component);
			return BorderFactory.createMatteBorder(borderInsets.top, borderInsets.left, borderInsets.bottom,
					borderInsets.right, Color.RED);
		}
	}

	private MouseAdapter toolTipMouseAdapter = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent e) {
			showBalloonIfNeeded();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			hideBalloon();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			hideBalloon();
		}
	};

	protected void showBalloonIfNeeded() {
		if (balloon == null || balloon.isVisible() || pm == null || pm.isEmpty()) {
			return;
		}

		if (!contentsInitedAfterErrorsUpdate) {
			// For some reason BalloonTip refuses to rerender when we
			// change it's contents. Thats why we re-set it's contents here
			balloon.setContents(panelMessages);
			// Also we need to re-set the component in order to balloon tip will
			// re-consider situation is suitable for showing
			balloon.setAttachedComponent(component);
			contentsInitedAfterErrorsUpdate = true;
		}

		balloon.setVisible(true);
	}

	private void hideBalloon() {
		if (balloon != null) {
			balloon.setVisible(false);
		}
	}

	private void initiallyProcessExistingErrors() {
		for (ValidationError ve : pm) {
			onItemAdded(ve, 0);
		}
	}

	@Override
	public void onItemAdded(ValidationError item, int atIndex) {
		contentsInitedAfterErrorsUpdate = false;

		if (component != null) {
			component.setBorder(errorBorder);
		}

		String msg = getValidationErrorMessage(item);
		JComponent msgPresentation = buildMessagePresentation(msg);
		panelMessages.add(msgPresentation);
	}

	@Override
	public void onItemChanged(ValidationError item, int atIndex) {
		Preconditions.checkState(false, "This event is not expected on ValidationErrors list");
	}

	@Override
	public void onItemRemoved(ValidationError item, int wasAtIndex) {
		Preconditions.checkState(false, "This event is not expected on ValidationErrors list");
	}

	@Override
	public void onAllItemsRemoved(int sizeWas) {
		contentsInitedAfterErrorsUpdate = false;

		if (component != null) {
			component.setBorder(originalBorder);
		}

		panelMessages.removeAll();
		hideBalloon();
	}

	private JComponent buildMessagePresentation(String msg) {
		JLabel lbl = new JLabel(UiUtils.envelopeStringIntoHtml(msg));
		lbl.setBackground(new Color(255, 230, 230));
		lbl.setOpaque(true);
		return lbl;
	}

	private String getValidationErrorMessage(ValidationError ve) {
		return I18nUtils.buildMessage(ve, EntryPoint.INSTANCE.getApplicationContext());
	}

	@Override
	public boolean isBound() {
		return isAttached();
	}

	@Override
	public void unbind() {
		detach();
	}
}
