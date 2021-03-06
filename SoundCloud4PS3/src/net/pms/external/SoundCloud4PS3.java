package net.pms.external;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import soundcloud4ps3.Authorization;
import soundcloud4ps3.Cloud;
import soundcloud4ps3.ResourceFolder;
import soundcloud4ps3.Settings;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SoundCloud4PS3 implements AdditionalFolderAtRoot {

	private static final String PLUGIN_NAME = "SoundCloud4PS3";
	private static final String VERSION = "0.5";

	private final Authorization authorization = new Authorization();
	private Cloud cloud;

	private final ArrayList<Component> authorizationComponents = new ArrayList<Component>();
	private final ArrayList<Component> unauthorizationComponents = new ArrayList<Component>();
	private final JLabel authorizationStateLabel = new JLabel();
	private final JTextArea authorizationUrlArea = new JTextArea();

	private final ResourceFolder topFolder;

	public SoundCloud4PS3() {
		logMinimal("v%s", VERSION);
		
		topFolder = new ResourceFolder("SoundCloud", "me");
		
		onAuthorizationStateChanged();
	}

	@Override
	public JComponent config() {

		FormLayout layout = new FormLayout(
				"70dlu, 10dlu, 300dlu", //$NON-NLS-1$
				"p, 5dlu, p, 5dlu, p, 10dlu, 10dlu, p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p, 5dlu, p, 0:grow"); //$NON-NLS-1$
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();
		
		//
		// Authorization
		//
		int row = 1;
		JComponent cmp = builder.addSeparator("Authorization", cc.xyw(1, row, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		row += 2;

		// Authorization State
		builder.addLabel("Current State:", cc.xy(1, row));
		builder.add(authorizationStateLabel, cc.xy(3, row));
		row += 2;

		// Authorization Explanation
		authorizationComponents.add(builder.addLabel("You must authorize this plugin to retrieve your favorites. In order to do so, navigate to the authorization URL,", cc.xyw(1, row++, 3)));
		authorizationComponents.add(builder.addLabel("then enter the retrieved verification code and click 'Authorize'.", cc.xyw(1, row++, 3)));
		row += 1;

		// Authorization URL
		authorizationUrlArea.setEditable(false);
		authorizationComponents.add(builder.addLabel("Authorization URL:", cc.xy(1, row)));
		authorizationComponents.add(builder.add(authorizationUrlArea, cc.xy(3, row)));
		row += 2;

		// Verification Code
		final JTextField verificationCodeField = new JTextField();
		authorizationComponents.add(builder.addLabel("Verification Code:", cc.xy(1, row)));
		authorizationComponents.add(builder.add(verificationCodeField, cc.xy(3,	row)));
		row += 2;

		// Authorize Button
		JButton authorizeButton = new JButton("Authorize");
		authorizeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				authorization.authorize(verificationCodeField.getText());
				assert authorization.isAuthorized();
				onAuthorizationStateChanged();
			}
		});
		authorizationComponents.add(builder.add(authorizeButton, cc.xy(3, row)));//, 1, CellConstraints.LEFT, CellConstraints.CENTER)));
		URL iconUrl = authorization.getClass().getResource("images/icon.png");
		ImageIcon icon = new ImageIcon(iconUrl, "Icon");
		JLabel iconLabel = new JLabel(icon);
		unauthorizationComponents.add(builder.add(iconLabel, cc.xywh(1, row, 1, 3, CellConstraints.LEFT, CellConstraints.CENTER)));
		row += 2;

		// Unauthorize Button
		JButton unauthorizeButton = new JButton("Unauthorize");
		unauthorizeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				authorization.unauthorize();
				assert !authorization.isAuthorized();
				onAuthorizationStateChanged();
			}
		});
		unauthorizationComponents.add(builder.add(unauthorizeButton, cc.xy(3, row))); //, CellConstraints.LEFT, CellConstraints.CENTER)));
		row += 2;		
		
		//
		// Debugging
		//
		cmp = builder.addSeparator("Debugging", cc.xyw(1, row, 3));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
		row += 2;

		final JCheckBox debugCheckBox = new JCheckBox("Enabled");
		debugCheckBox.setSelected(Settings.isDebug());
		debugCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Settings.setDebug(debugCheckBox.isSelected());				
			}
		});
		builder.add(debugCheckBox, cc.xyw(1, row, 3));

		// enable/disable controls
		onAuthorizationStateChanged();

		return builder.getPanel();
	}

	@Override
	public String name() {
		return "SoundCloud for PS3 Media Server";
	}

	@Override
	public void shutdown() {
	}

	@Override
	public DLNAResource getChild() {
		return topFolder;
	}

	public static void logMinimal(String message, Object... args) {
		PMS.minimal(getLogMessage(message, args));
	}

	public static void logInfo(String message, Object... args) {
		PMS.info(getLogMessage(message, args));
	}


	public static void logDebug(String message, Object... args) {
		PMS.debug(getLogMessage(message, args));
	}

	private static String getLogMessage(String message, Object... args) {
		return PLUGIN_NAME + ": " + String.format(message, args);
	}

	private void onAuthorizationStateChanged() {
		authorizationStateLabel.setText(authorization.getState().toString());
		cloud = authorization.getCloud();
		boolean isAuthorized = authorization.isAuthorized();
		for (Component c : authorizationComponents) {
			c.setEnabled(!isAuthorized);
		}
		for (Component c : unauthorizationComponents) {
			c.setEnabled(isAuthorized);
		}
		if (!isAuthorized) {
			authorizationUrlArea.setText(authorization.getAuthorizationUrl());
		}
		topFolder.setCloud(cloud);
	}
}
