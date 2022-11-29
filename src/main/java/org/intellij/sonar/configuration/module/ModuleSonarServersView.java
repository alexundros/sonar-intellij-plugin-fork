package org.intellij.sonar.configuration.module;

import static org.intellij.sonar.persistence.SonarServers.NO_SONAR;
import static org.intellij.sonar.persistence.SonarServers.PROJECT;
import static org.intellij.sonar.util.UIUtil.makeObj;

import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import org.intellij.sonar.configuration.partials.SonarServersView;
import org.intellij.sonar.persistence.ProjectSettings;
import org.intellij.sonar.persistence.Settings;
import org.intellij.sonar.persistence.SonarServerConfig;
import org.intellij.sonar.persistence.SonarServers;

public class ModuleSonarServersView extends SonarServersView {

  public ModuleSonarServersView(
      JComboBox myServersComboBox,
      JButton myAddServerButton,
      JButton myEditServerButton,
      JButton myRemoveServerButton,
      Project myProject
  ) {
    super(myServersComboBox, myAddServerButton, myEditServerButton, myRemoveServerButton, myProject);
  }

  @Override
  public String getSelectedItem() {
    final String selectedItem = super.getSelectedItem();
    if (SonarServers.PROJECT.equals(selectedItem)) {
      final Settings settings = ProjectSettings.getInstance(myProject).getState();
      return null != settings
          ? settings.getServerName()
          : NO_SONAR;
    }
    return selectedItem;
  }

  @Override
  protected boolean editAndRemoveButtonsCanBeEnabled() {
    final boolean isNoSelected = NO_SONAR.equals(String.valueOf(myServersComboBox.getSelectedItem()));
    final boolean isProjectSelected = PROJECT.equals(String.valueOf(myServersComboBox.getSelectedItem()));
    return !isNoSelected && !isProjectSelected;
  }

  @Override
  protected void initServersComboBox() {
    Optional<Collection<SonarServerConfig>> sonarServerConfigurationBeans = SonarServers.getAll();
    if (sonarServerConfigurationBeans.isPresent()) {
      myServersComboBox.removeAllItems();
      myServersComboBox.addItem(makeObj(PROJECT));
      myServersComboBox.addItem(makeObj(NO_SONAR));
      for (SonarServerConfig sonarServerConfigBean : sonarServerConfigurationBeans.get()) {
        myServersComboBox.addItem(makeObj(sonarServerConfigBean.getName()));
      }
    }
  }
}
