/*
 * Copyright 2015 Lithium Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lithium.flow.access.prompts;

import static com.google.common.base.Preconditions.checkNotNull;

import com.lithium.flow.access.Prompt;
import com.lithium.flow.util.Sleep;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * @author Matt Ayres
 */
public class DialogPrompt implements Prompt {
	private final String title;

	public DialogPrompt(@Nonnull String title) {
		this.title = checkNotNull(title);
	}

	@Override
	@Nonnull
	public String prompt(@Nonnull String name, @Nonnull String message, boolean mask, boolean retry) {
		Label promptLabel = new Label(message);
		JTextField promptField = mask ? new JPasswordField(20) : new JTextField(20);

		JDialog dialog = new JDialog();
		dialog.setTitle(title);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		Container content = dialog.getContentPane();
		content.setLayout(new FlowLayout());
		content.add(promptLabel);
		content.add(promptField);
		dialog.pack();

		Dimension screen = dialog.getToolkit().getScreenSize();
		Rectangle bounds = dialog.getBounds();
		dialog.setLocation((screen.width - bounds.width) / 2, (screen.height - bounds.height) / 2);
		dialog.setVisible(true);

		CountDownLatch latch = new CountDownLatch(1);
		promptField.addActionListener(event -> latch.countDown());
		promptField.requestFocus();

		Sleep.softly(latch::await);

		dialog.dispose();

		return promptField.getText();
	}
}
