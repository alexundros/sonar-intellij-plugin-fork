package org.intellij.sonar.configuration.partials;

import static org.intellij.sonar.util.UIUtil.makeObj;

import com.google.common.base.Throwables;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JComboBox;
import org.intellij.sonar.configuration.SonarServerConfigurable;
import org.intellij.sonar.console.ConsoleLogLevel;
import org.intellij.sonar.console.SonarConsole;
import org.intellij.sonar.persistence.SonarServerConfig;
import org.intellij.sonar.persistence.SonarServers;
import org.intellij.sonar.util.UIUtil;

public abstract class SonarServersView {

  protected final JComboBox myServersComboBox;
  protected final JButton myAddServerButton;
  protected final JButton myEditServerButton;
  protected final JButton myRemoveServerButton;
  protected final Project myProject;

  public SonarServersView(
      JComboBox myServersComboBox,
      JButton myAddServerButton,
      JButton myEditServerButton,
      JButton myRemoveServerButton,
      Project myProject
  ) {
    this.myServersComboBox = myServersComboBox;
    this.myAddServerButton = myAddServerButton;
    this.myEditServerButton = myEditServerButton;
    this.myRemoveServerButton = myRemoveServerButton;
    this.myProject = myProject;

    addActionListenersForButtons();
  }

  public void init() {
    initServersComboBox();
    disableEditAndRemoveButtonsIfPossible();
  }

  public String getSelectedItemFromComboBox() {
    return myServersComboBox.getSelectedItem().toString();
  }

  public String getSelectedItem() {
    return getSelectedItemFromComboBox();
  }

  protected abstract boolean editAndRemoveButtonsCanBeEnabled();

  protected abstract void initServersComboBox();

  protected void disableEditAndRemoveButtonsIfPossible() {
    final boolean enabled = editAndRemoveButtonsCanBeEnabled();
    myEditServerButton.setEnabled(enabled);
    myRemoveServerButton.setEnabled(enabled);
  }

  protected SonarServerConfigurable showServerConfigurableDialog() {
    return showServerConfigurableDialog(null);
  }

  protected SonarServerConfigurable showServerConfigurableDialog(SonarServerConfig oldServerConfigBean) {
    final SonarServerConfigurable dlg = new SonarServerConfigurable(myProject);
    if (null != oldServerConfigBean) {
      dlg.setValuesFrom(oldServerConfigBean);
    }
    dlg.show();
    return dlg;
  }

  protected final void addActionListenersForButtons() {
    addItemListenerForServersComboBox();
    addActionListenerForAddServerButton();
    addActionListenerForEditServerButton();
    addActionListenerForRemoveServerButton();
  }

  private void addItemListenerForServersComboBox() {
    myServersComboBox.addItemListener(
        itemEvent -> disableEditAndRemoveButtonsIfPossible()
    );
  }

  private void addActionListenerForAddServerButton() {
    myAddServerButton.addActionListener(
        actionEvent -> {
          final SonarServerConfigurable dlg = showServerConfigurableDialog();
          if (dlg.isOK()) {
            SonarServerConfig newConfigurationBean = dlg.toServerConfigurationBean();
            try {
              SonarServers.add(newConfigurationBean);
              myServersComboBox.addItem(makeObj(newConfigurationBean.getName()));
              UIUtil.selectComboBoxItem(myServersComboBox, newConfigurationBean.getName());
            } catch (IllegalArgumentException e) {
              Messages.showErrorDialog(newConfigurationBean.getName() + " already exists", "SonarQube Name Error");
              showServerConfigurableDialog(newConfigurationBean);
              SonarConsole.get(myProject).log(Throwables.getStackTraceAsString(e), ConsoleLogLevel.ERROR);
            }
          }
        }
    );
  }

  private void addActionListenerForEditServerButton() {
    myEditServerButton.addActionListener(
        actionEvent -> {
          final Object selectedServer = myServersComboBox.getSelectedItem();
          final Optional<SonarServerConfig> oldBean = SonarServers.get(selectedServer.toString());
          if (oldBean.isEmpty()) {
            Messages.showErrorDialog(selectedServer + " is not more preset", "Cannot Perform Edit");
          } else {
            final SonarServerConfigurable dlg = showServerConfigurableDialog(oldBean.get());
            if (dlg.isOK()) {
              performEdit(selectedServer, oldBean.get(), dlg);
            }
          }
        }
    );
  }

  private void performEdit(Object selectedServer, SonarServerConfig oldBean, SonarServerConfigurable dlg) {
    SonarServerConfig newConfigurationBean = dlg.toServerConfigurationBean();
    try {
      SonarServers.remove(oldBean.getName());
      SonarServers.add(newConfigurationBean);
      myServersComboBox.removeItem(selectedServer);
      myServersComboBox.addItem(makeObj(newConfigurationBean.getName()));
      UIUtil.selectComboBoxItem(myServersComboBox, newConfigurationBean.getName());
    } catch (IllegalArgumentException e) {
      final String trace = Throwables.getStackTraceAsString(e);
      Messages.showErrorDialog(
          selectedServer.toString() + " cannot be saved\n\n" + trace, "Cannot Perform Edit"
      );
    }
  }

  private void addActionListenerForRemoveServerButton() {
    myRemoveServerButton.addActionListener(
        actionEvent -> {
          final Object selectedServer = myServersComboBox.getSelectedItem();
          int rc = Messages.showOkCancelDialog(
              "Are you sure you want to remove " + selectedServer.toString() + " ?",
              "Remove SonarQube Server",
              "Yes, remove", "No",
              Messages.getQuestionIcon()
          );
          if (rc == Messages.OK) {
            SonarServers.remove(selectedServer.toString());
            myServersComboBox.removeItem(selectedServer);
            disableEditAndRemoveButtonsIfPossible();
          }
        }
    );
  }

  public JComboBox getServersComboBox() {
    return myServersComboBox;
  }

  public JButton getAddServerButton() {
    return myAddServerButton;
  }

  public JButton getEditServerButton() {
    return myEditServerButton;
  }

  public JButton getRemoveServerButton() {
    return myRemoveServerButton;
  }
}
