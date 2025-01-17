package org.intellij.sonar.configuration;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.apache.commons.lang.StringUtils;
import org.intellij.sonar.persistence.SonarServerConfig;
import org.jetbrains.annotations.Nullable;

public class SonarServerConfigurable extends DialogWrapper {

  private JCheckBox myAnonymousCheckBox;
  private JCheckBox myShowPasswordCheckBox;
  private JTextField myNameTestField;
  private JTextField myHostUrlTextField;
  private JTextField myUserTextField;
  private JPasswordField myPasswordField;
  private JPanel myRootPanel;
  private JRadioButton tokenRadioButton;
  private JRadioButton credentialsRadioButton;
  private JTextField tokenText;
  private JTextField myOrganizationTextField;
  private JTextField extParamsTextField;
  private char[] oldPass;

  public SonarServerConfigurable(@Nullable Project project) {
    super(project);
    init();
  }

  @Override
  protected void doOKAction() {
    if (StringUtil.isEmptyOrSpaces(myNameTestField.getText())) {
      Messages.showErrorDialog("Please provide an unique name for the SonarQube server", "Empty Name");
    } else if (!isHostUrlSyntaxCorrect()) {
      Messages.showErrorDialog(
          "Host url syntax should be: http(s)://your.host(:1234)/possible/path",
          "Malformed Host Url"
      );
    } else if (!myAnonymousCheckBox.isSelected()) {
      if (credentialsRadioButton.isSelected() && StringUtils.isBlank(myUserTextField.getText())) {
        Messages.showErrorDialog("User may not be empty", "Empty User");
      }
      if (tokenRadioButton.isSelected() && StringUtils.isBlank(tokenText.getText())) {
        Messages.showErrorDialog("Access Token may not be empty", "Empty Token");
      }
      super.doOKAction();
    } else {
      if (myAnonymousCheckBox.isSelected()) {
        myUserTextField.setText("");
        myPasswordField.setText("");
        tokenText.setText("");
      }
      super.doOKAction();
    }
  }

  private boolean isHostUrlSyntaxCorrect() {
    try {
      new URL(myHostUrlTextField.getText());
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    initCheckboxes();
    myAnonymousCheckBox.addActionListener(event -> setEnabledForCredentialsTextFields());
    tokenRadioButton.addActionListener(event -> setEnabledForCredentialsTextFields());
    credentialsRadioButton.addActionListener(event -> setEnabledForCredentialsTextFields());
    myShowPasswordCheckBox.addActionListener(event -> setPasswordVisibility());
    return myRootPanel;
  }

  private void initCheckboxes() {
    setEnabledForCredentialsTextFields();
    setPasswordVisibility();
  }

  private void setPasswordVisibility() {
    if (myShowPasswordCheckBox.isSelected()) {
      makePasswordVisible();
    } else {
      makePasswordInvisible();
    }
  }

  private void setEnabledForCredentialsTextFields() {
    final boolean isAnonymousCheckBoxSelected = myAnonymousCheckBox.isSelected();
    tokenRadioButton.setEnabled(!isAnonymousCheckBoxSelected);
    final boolean useToken = tokenRadioButton.isSelected();
    credentialsRadioButton.setEnabled(!isAnonymousCheckBoxSelected);
    myUserTextField.setEnabled(!isAnonymousCheckBoxSelected && !useToken);
    myPasswordField.setEnabled(!isAnonymousCheckBoxSelected && !useToken);
    tokenText.setEnabled(!isAnonymousCheckBoxSelected && useToken);
    if (useToken) {
      myUserTextField.setText("");
      myPasswordField.setText("");
    } else {
      tokenText.setText("");
    }
  }

  private void makePasswordInvisible() {
    myPasswordField.setEchoChar('•');
  }

  private void makePasswordVisible() {
    myPasswordField.setEchoChar('\u0000');
  }

  public SonarServerConfig toServerConfigurationBean() {
    SonarServerConfig sonarServerConfig = SonarServerConfig.of(
        myNameTestField.getText(),
        myHostUrlTextField.getText(),
        myAnonymousCheckBox.isSelected(),
        myUserTextField.getText(),
        tokenText.getText(),
        myOrganizationTextField.getText(),
        extParamsTextField.getText()
    );
    if (credentialsRadioButton.isSelected()) {
      char[] pass = myPasswordField.getPassword();
      sonarServerConfig.setPassword(String.valueOf(pass));
      sonarServerConfig.setPasswordChanged(!Arrays.equals(oldPass, pass));
    }
    return sonarServerConfig;
  }

  public void setValuesFrom(SonarServerConfig bean) {
    this.myNameTestField.setText(bean.getName());
    this.myHostUrlTextField.setText(bean.getHostUrl());
    if (!bean.isAnonymous()) {
      if (StringUtils.isNotBlank(bean.loadToken())) {
        tokenText.setText(bean.loadToken());
        bean.clearToken();
        tokenRadioButton.setSelected(true);
      } else {
        this.myUserTextField.setText(bean.getUser());
        this.myPasswordField.setText(bean.loadPassword());
        this.oldPass = myPasswordField.getPassword();
        bean.clearPassword();
        credentialsRadioButton.setSelected(true);
      }
    }
    this.myAnonymousCheckBox.setSelected(bean.isAnonymous());
    this.myOrganizationTextField.setText(bean.getOrganization());
    this.extParamsTextField.setText(bean.getExtParams());
    initCheckboxes();
  }
}
