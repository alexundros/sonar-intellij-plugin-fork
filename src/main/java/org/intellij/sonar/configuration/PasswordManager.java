package org.intellij.sonar.configuration;

import com.google.common.base.Throwables;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;

public final class PasswordManager {

  private PasswordManager() {
  }

  private static CredentialAttributes createCredentialAttributes(String key) {
    return new CredentialAttributes(CredentialAttributesKt.generateServiceName(PasswordManager.class.getName(), key));
  }

  public static void storePassword(final String user, final String password) {
    UIUtil.invokeAndWaitIfNeeded(
        (Runnable) () -> {
          try {
            CredentialAttributes attrs = createCredentialAttributes(user);
            PasswordSafe.getInstance().set(attrs, new Credentials(user, password));
          } catch (Exception e) {
            Messages.showErrorDialog("Cannot store password\n" + Throwables.getStackTraceAsString(e), "Error");
          }
        }
    );
  }

  public static String loadPassword(final String user) {
    try {
      CredentialAttributes attrs = createCredentialAttributes(user);
      return PasswordSafe.getInstance().getPassword(attrs);
    } catch (Exception e) {
      Messages.showErrorDialog("Cannot load password\n" + Throwables.getStackTraceAsString(e), "Error");
    }
    return null;
  }
}
