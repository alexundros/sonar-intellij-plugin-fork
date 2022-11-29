package org.intellij.sonar.configuration.module;

import static org.intellij.sonar.util.UIUtil.makeObj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
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
import org.intellij.sonar.persistence.ModuleSettings;
import org.intellij.sonar.persistence.Resource;
import org.intellij.sonar.persistence.Settings;
import org.intellij.sonar.util.LocalAnalysisScriptsUtil;
import org.intellij.sonar.util.SonarServersUtil;
import org.intellij.sonar.util.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

public class ModuleSettingsConfigurationEditor implements ModuleConfigurationEditor {

  private final ModuleLocalAnalysisScriptView myLocalAnalysisScriptView;
  private final SonarResourcesTableView myResourcesTableView;
  private final Module myModule;
  private final Project myProject;
  private final ModuleSonarServersView myServersView;
  private JPanel myRootJPanel;
  private JPanel myPanelForResources;
  private JComboBox myServersComboBox;
  private JButton myAddServerButton;
  private JButton myEditServerButton;
  private JButton myRemoveServerButton;
  private JComboBox myLocalAnalysisScriptComboBox;
  private JButton myAddLocalAnalysisScriptButton;
  private JButton myEditLocalAnalysisScriptButton;
  private JButton myRemoveLocalAnalysisScriptButton;
  private JComboBox myWorkingDirComboBox;
  private JCheckBox myUseAlternativeWorkingDirCheckBox;
  private TextFieldWithBrowseButton myAlternativeWorkingDirTextFieldWithBrowseButton;
  private JTextField rulesTextField;

  public ModuleSettingsConfigurationEditor(ModuleConfigurationState state) {
    this.myModule = state.getCurrentRootModel().getModule();
    this.myProject = state.getProject();
    this.myLocalAnalysisScriptView = new ModuleLocalAnalysisScriptView(
        myLocalAnalysisScriptComboBox,
        myAddLocalAnalysisScriptButton,
        myEditLocalAnalysisScriptButton,
        myRemoveLocalAnalysisScriptButton,
        myProject
    );
    this.myServersView = new ModuleSonarServersView(
        myServersComboBox,
        myAddServerButton,
        myEditServerButton,
        myRemoveServerButton,
        myProject
    );
    this.myResourcesTableView = new SonarResourcesTableView(myProject, myServersView);
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
    myWorkingDirComboBox.addItem(makeObj(WorkingDirs.MODULE));
    myWorkingDirComboBox.addItem(makeObj(WorkingDirs.PROJECT));
  }

  private void initAlternativeWorkingDir() {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(myModule).getContentRoots();
    final VirtualFile projectBaseDir = ProjectUtil.guessProjectDir(myProject);
    final VirtualFile dirToSelect = contentRoots.length > 0
        ? contentRoots[0]
        : projectBaseDir;
    myAlternativeWorkingDirTextFieldWithBrowseButton.addActionListener(
        new AlternativeWorkingDirActionListener(
            myProject, myAlternativeWorkingDirTextFieldWithBrowseButton, dirToSelect
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
    final ModuleSettings component = ModuleSettings.getInstance(myModule);
    if (null == component) {
      return false;
    }
    Settings state = component.getState();
    return null == state || !state.equals(this.toSettings());
  }

  @Override
  public void apply() {
    Settings settings = this.toSettings();
    ModuleSettings moduleSettings = ModuleSettings.getInstance(myModule);
    moduleSettings.loadState(settings);
  }

  @Override
  public void reset() {
    ModuleSettings moduleSettings = myModule.getService(ModuleSettings.class);
    if (moduleSettings != null && moduleSettings.getState() != null) {
      Settings persistedSettings = moduleSettings.getState();
      this.setValuesFromSettings(persistedSettings);
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
        rulesTextField.getText()
    );
  }

  public void setValuesFromSettings(Settings settings) {
    if (null == settings) {
      return;
    }
    final String serverName = SonarServersUtil.withDefaultForModule(settings.getServerName());
    UIUtil.selectComboBoxItem(myServersComboBox, serverName);
    final ArrayList<Resource> resources = Lists.newArrayList(settings.getResources());
    myResourcesTableView.setModel(resources);
    final String localAnalysisScripName = LocalAnalysisScriptsUtil.withDefaultForModule(settings
        .getLocalAnalysisScripName());
    UIUtil.selectComboBoxItem(myLocalAnalysisScriptComboBox, localAnalysisScripName);
    UIUtil.selectComboBoxItem(myWorkingDirComboBox, WorkingDirs.withDefaultForModule(settings.getWorkingDirSelection()));
    myAlternativeWorkingDirTextFieldWithBrowseButton.setText(settings.getAlternativeWorkingDirPath());
    myUseAlternativeWorkingDirCheckBox.setSelected(
        Optional.ofNullable(settings.getUseAlternativeWorkingDir()).orElse(false)
    );
    rulesTextField.setText(settings.getExtParams());
    processAlternativeDirSelections();
  }
}
