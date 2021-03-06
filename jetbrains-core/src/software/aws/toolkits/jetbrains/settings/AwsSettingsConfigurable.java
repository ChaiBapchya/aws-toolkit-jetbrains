// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.settings;

import static software.aws.toolkits.resources.Localization.message;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.labels.LinkLabel;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AwsSettingsConfigurable implements SearchableConfigurable {
    private static final String SAM_HELP_LINK = message("lambda.sam.cli.install_url");

    private final Project project;
    private JPanel panel;
    private TextFieldWithBrowseButton samExecutablePath;
    private LinkLabel samHelp;
    private JBCheckBox showAllHandlerGutterIcons;

    public AwsSettingsConfigurable(Project project) {
        this.project = project;

        samExecutablePath.addBrowseFolderListener(
            message("aws.settings.sam.find.title"),
            message("aws.settings.sam.find.description"),
            project,
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return panel;
    }

    private void createUIComponents() {
        samHelp = LinkLabel.create(message("aws.settings.sam.help"), () -> BrowserUtil.browse(SAM_HELP_LINK));

        String autoDetectPath = new SamExecutableDetector().detect();
        JBTextField samExecutableTextField = new JBTextField();
        if(autoDetectPath != null) {
            samExecutableTextField.getEmptyText()
                                  .setText(message("aws.settings.sam.auto_detect", autoDetectPath));
        }
        samExecutablePath = new TextFieldWithBrowseButton(samExecutableTextField);
        samExecutablePath.addBrowseFolderListener(
            message("aws.settings.sam.find.title"),
            message("aws.settings.sam.find.description"),
            project,
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
        );
    }

    @NotNull
    @Override
    public String getId() {
        return "aws";
    }

    @Nls(capitalization = Capitalization.Title)
    @Override
    public String getDisplayName() {
        return message("aws.settings.title");
    }

    @Override
    public boolean isModified() {
        SamSettings samSettings = SamSettings.getInstance();
        LambdaSettings lambdaSettings = LambdaSettings.getInstance(project);
        return !Objects.equals(getSamExecutablePath(), samSettings.getSavedExecutablePath()) ||
               isModified(showAllHandlerGutterIcons, lambdaSettings.getShowAllHandlerGutterIcons());
    }

    @Override
    public void apply() {
        SamSettings samSettings = SamSettings.getInstance();
        LambdaSettings lambdaSettings = LambdaSettings.getInstance(project);

        samSettings.setSavedExecutablePath(getSamExecutablePath());
        lambdaSettings.setShowAllHandlerGutterIcons(showAllHandlerGutterIcons.isSelected());
    }

    @Nullable
    private String getSamExecutablePath() {
        return StringUtil.nullize(samExecutablePath.getText().trim());
    }

    @Override
    public void reset() {
        SamSettings samSettings = SamSettings.getInstance();
        LambdaSettings lambdaSettings = LambdaSettings.getInstance(project);
        samExecutablePath.setText(samSettings.getSavedExecutablePath());
        showAllHandlerGutterIcons.setSelected(lambdaSettings.getShowAllHandlerGutterIcons());
    }
}
