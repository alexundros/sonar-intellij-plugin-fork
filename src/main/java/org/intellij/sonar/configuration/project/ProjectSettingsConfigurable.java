package org.intellij.sonar.configuration.project;

import static org.intellij.sonar.util.UIUtil.makeObj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.intellij.sonar.configuration.WorkingDirs;
import org.intellij.sonar.configuration.partials.AlternativeWorkingDirActionListener;
import org.intellij.sonar.configuration.partials.SonarResourcesTableView;
import org.intellij.sonar.persistence.ProjectSettings;
import org.intellij.sonar.persistence.Resource;
import org.intellij.sonar.persistence.Settings;
import org.intellij.sonar.persistence.SonarConsoleSettings;
import org.intellij.sonar.util.LocalAnalysisScriptsUtil;
import org.intellij.sonar.util.SonarServersUtil;
import org.intellij.sonar.util.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class ProjectSettingsConfigurable implements Configurable {

  private final ProjectSettings myProjectSettings;
  private final SonarConsoleSettings myConsoleSettings;
  private final ProjectLocalAnalysisScriptView myLocalAnalysisScriptView;
  private final SonarResourcesTableView myResourcesTableView;
  private final ProjectSonarServersView myServersView;
  private Project myProject;
  private JPanel myRootJPanel;
  private JPanel myPanelForResources;
  private JComboBox myServersComboBox;
  private JButton myAddServerButton;
  private JButton myEditServerButton;
  private JButton myRemoveServerButton;
  private JButton myAddLocalAnalysisScriptButton;
  private JButton myEditLocalAnalysisScriptButton;
  private JButton myRemoveLocalAnalysisScriptButton;
  private JComboBox myLocalAnalysisScriptComboBox;
  private JCheckBox myUseAlternativeWorkingDirCheckBox;
  private JComboBox myWorkingDirComboBox;
  private TextFieldWithBrowseButton myAlternativeWorkingDirTextFieldWithBrowseButton;
  private JCheckBox myShowQubeToolWindowCheckBox;
  private JTextField extParamsTextField;

  public ProjectSettingsConfigurable(Project project) {
    this.myProject = project;
    this.myProjectSettings = ProjectSettings.getInstance(project);
    this.myConsoleSettings = SonarConsoleSettings.getInstance();
    this.myServersView = new ProjectSonarServersView(
        myServersComboBox,
        myAddServerButton,
        myEditServerButton,
        myRemoveServerButton,
        project
    );
    this.myLocalAnalysisScriptView = new ProjectLocalAnalysisScriptView(
        myLocalAnalysisScriptComboBox,
        myAddLocalAnalysisScriptButton,
        myEditLocalAnalysisScriptButton,
        myRemoveLocalAnalysisScriptButton,
        project
    );
    this.myResourcesTableView = new SonarResourcesTableView(project, myServersView);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "SonarQube Fork";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    myPanelForResources.setLayout(new BorderLayout());
    myPanelForResources.add(myResourcesTableView.getComponent(), BorderLayout.CENTER);
    myServersView.init();
    myLocalAnalysisScriptView.init();
    initWorkingDir();
    initAlternativeWorkingDir();
    return myRootJPanel;
  }

  private void initWorkingDir() {
    myWorkingDirComboBox.removeAllItems();
    myWorkingDirComboBox.addItem(makeObj(WorkingDirs.PROJECT));
    myWorkingDirComboBox.addItem(makeObj(WorkingDirs.MODULE));
  }

  private void initAlternativeWorkingDir() {
    myAlternativeWorkingDirTextFieldWithBrowseButton.addActionListener(
        new AlternativeWorkingDirActionListener(
            myProject,
            myAlternativeWorkingDirTextFieldWithBrowseButton,
            ProjectUtil.guessProjectDir(myProject)
        )
    );
    processAlternativeDirSelections();
    myUseAlternativeWorkingDirCheckBox.addActionListener(
        e -> processAlternativeDirSelections()
    );
  }

  private void processAlternativeDirSelections() {
    myAlternativeWorkingDirTextFieldWithBrowseButton.setEnabled(myUseAlternativeWorkingDirCheckBox.isSelected());
    myWorkingDirComboBox.setEnabled(!myUseAlternativeWorkingDirCheckBox.isSelected());
  }

  @Override
  public boolean isModified() {
    return isProjectSettingsModified() || isConsoleSettings();
  }

  private boolean isProjectSettingsModified() {
    if (null == myProjectSettings) {
      return false;
    }
    Settings state = myProjectSettings.getState();
    return null == state || !state.equals(this.toSettings());
  }

  private boolean isConsoleSettings() {
    if (null == myConsoleSettings) {
      return false;
    }
    SonarConsoleSettings state = myConsoleSettings.getState();
    return null == state || !state.equals(this.toConsoleSettings());
  }

  @Override
  public void apply() {
    myProjectSettings.loadState(this.toSettings());
    myConsoleSettings.loadState(this.toConsoleSettings());
  }

  @Override
  public void reset() {
    if (myProjectSettings != null && myProjectSettings.getState() != null) {
      Settings persistedSettings = myProjectSettings.getState();
      this.setValuesFromSettings(persistedSettings);
    }
    if (myConsoleSettings != null && myConsoleSettings.getState() != null) {
      final SonarConsoleSettings persistedSettings = myConsoleSettings.getState();
      this.setValuesFromConsoleSettings(persistedSettings);
    }
  }

  @Override
  public void disposeUIResources() {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  public Settings toSettings() {
    return Settings.of(
        myServersComboBox.getSelectedItem().toString(),
        ImmutableList.copyOf(myResourcesTableView.getTable().getItems()),
        myLocalAnalysisScriptComboBox.getSelectedItem().toString(),
        myWorkingDirComboBox.getSelectedItem().toString(),
        myAlternativeWorkingDirTextFieldWithBrowseButton.getText(),
        myUseAlternativeWorkingDirCheckBox.isSelected(),
        extParamsTextField.getText()
    );
  }

  public void setValuesFromSettings(Settings settings) {
    if (null == settings) {
      return;
    }
    final String serverName = SonarServersUtil.withDefaultForProject(settings.getServerName());
    UIUtil.selectComboBoxItem(myServersComboBox, serverName);
    final ArrayList<Resource> resources = Lists.newArrayList(settings.getResources());
    myResourcesTableView.setModel(resources);
    final String localAnalysisScripName = LocalAnalysisScriptsUtil.withDefaultForProject(settings
        .getLocalAnalysisScripName());
    UIUtil.selectComboBoxItem(myLocalAnalysisScriptComboBox, localAnalysisScripName);
    UIUtil.selectComboBoxItem(
        myWorkingDirComboBox,
        WorkingDirs.withDefaultForProject(settings.getWorkingDirSelection())
    );
    myAlternativeWorkingDirTextFieldWithBrowseButton.setText(settings.getAlternativeWorkingDirPath());
    myUseAlternativeWorkingDirCheckBox.setSelected(
        Optional.ofNullable(settings.getUseAlternativeWorkingDir()).orElse(false)
    );
    extParamsTextField.setText(settings.getExtParams());
    processAlternativeDirSelections();
  }

  public SonarConsoleSettings toConsoleSettings() {
    return SonarConsoleSettings.of(myShowQubeToolWindowCheckBox.isSelected());
  }

  public void setValuesFromConsoleSettings(SonarConsoleSettings settings) {
    if (null == settings) {
      return;
    }
    myShowQubeToolWindowCheckBox.setSelected(settings.isShowConsoleOnAnalysis());
  }
}
