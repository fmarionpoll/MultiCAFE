package plugins.fmp.multicafe.dlg.kymos;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.tools.ImageRegistration;
import plugins.fmp.multicafe.tools.ImageRegistrationFeatures;
import plugins.fmp.multicafe.tools.ImageRegistrationFeaturesGPU;
import plugins.fmp.multicafe.tools.ImageRegistrationGaspard;

public class Register extends JPanel {

	private static final long serialVersionUID = -1234567890L;

	private MultiCAFE parent0 = null;

	private JComboBox<String> typeCombo = new JComboBox<>(new String[] { "Gaspard Rigid", "Feature Tracking (CPU)", "Feature Tracking (GPU)" });
	private JComboBox<String> referenceCombo = new JComboBox<>(
			new String[] { "End (Last Frame)", "Start (First Frame)" });
	private JComboBox<String> directionCombo = new JComboBox<>(
			new String[] { "Backward (End -> Start)", "Forward (Start -> End)" });

	private JButton registerButton = new JButton("Register & Save");
	private JLabel statusLabel = new JLabel("Ready");

	void init(GridLayout capLayout, MultiCAFE parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		FlowLayout layoutLeft = new FlowLayout(FlowLayout.LEFT);

		JPanel panel0 = new JPanel(layoutLeft);
		panel0.add(new JLabel("Algorithm: "));
		panel0.add(typeCombo);
		add(panel0);

		JPanel panel1 = new JPanel(layoutLeft);
		panel1.add(new JLabel("Reference: "));
		panel1.add(referenceCombo);
		add(panel1);

		JPanel panel2 = new JPanel(layoutLeft);
		panel2.add(new JLabel("Direction: "));
		panel2.add(directionCombo);
		add(panel2);

		JPanel panel3 = new JPanel(layoutLeft);
		panel3.add(registerButton);
		panel3.add(statusLabel);
		add(panel3);

		defineActionListeners();
	}

	private void defineActionListeners() {
		registerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startRegistration();
			}
		});
	}

	private void startRegistration() {
		Experiment exp = (Experiment) parent0.expListCombo.getSelectedItem();
		if (exp == null)
			return;

		registerButton.setEnabled(false);
		statusLabel.setText("Processing...");

		new Thread(() -> {
			int startFrame = 0;
			int endFrame = exp.getSeqCamData().getnTotalFrames() - 1;

			boolean reverse = directionCombo.getSelectedIndex() == 0; // Backward
			int referenceFrame = (referenceCombo.getSelectedIndex() == 0) ? endFrame : startFrame;

			ImageRegistration reg = null;
			if (typeCombo.getSelectedIndex() == 0)
				reg = new ImageRegistrationGaspard();
			else if (typeCombo.getSelectedIndex() == 1)
				reg = new ImageRegistrationFeatures();
			else
				reg = new ImageRegistrationFeaturesGPU();

			boolean result = reg.runRegistration(exp, referenceFrame, startFrame, endFrame, reverse);

			SwingUtilities.invokeLater(() -> {
				statusLabel.setText(result ? "Done." : "Failed.");
				registerButton.setEnabled(true);
			});
		}).start();
	}

}

