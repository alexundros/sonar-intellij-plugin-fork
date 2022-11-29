package org.intellij.sonar.configuration.project;

import static org.intellij.sonar.persistence.SonarServers.NO_SONAR;
import static org.intellij.sonar.util.UIUtil.makeObj;

import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import org.intellij.sonar.configuration.partials.SonarServersView;
import org.intellij.sonar.persistence.SonarServerConfig;
import org.intellij.sonar.persistence.SonarServers;

public class ProjectSonarServersView extends SonarServersView {

  public ProjectSonarServersView(
      JComboBox myServersComboBox,
      JButton myAddServerButton,
      JButton myEditServerButton,
      JButton myRemoveServerButton,
      Project myProject
  ) {
    super(myServersComboBox, myAddServerButton, myEditServerButton, myRemoveServerButton, myProject);
  }

  @Override
  protected boolean editAndRemoveButtonsCanBeEnabled() {
    return !NO_SONAR.equals(myServersComboBox.getSelectedItem().toString());
  }

  @Override
  protected void initServersComboBox() {
    Optional<Collection<SonarServerConfig>> sonarServerConfigurationBeans = SonarServers.getAll();
    if (sonarServerConfigurationBeans.isPresent()) {
      myServersComboBox.removeAllItems();
      myServersComboBox.addItem(makeObj(NO_SONAR));
      for (SonarServerConfig sonarServerConfigBean : sonarServerConfigurationBeans.get()) {
        myServersComboBox.addItem(makeObj(sonarServerConfigBean.getName()));
      }
    }
  }
}
