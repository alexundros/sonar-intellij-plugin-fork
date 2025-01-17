package org.intellij.sonar.persistence;

import com.google.common.base.Objects;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "sonarConsoleSettings",
    storages = {
        @Storage("sonarSettings.xml")
    }
)
public class SonarConsoleSettings implements PersistentStateComponent<SonarConsoleSettings> {

  private boolean showConsoleOnAnalysis = true;

  public static SonarConsoleSettings getInstance() {
    return ServiceManager.getService(SonarConsoleSettings.class);
  }

  @NotNull
  public static SonarConsoleSettings of(boolean showConsoleOnAnalysis) {
    final SonarConsoleSettings sonarConsoleSettings = new SonarConsoleSettings();
    sonarConsoleSettings.setShowConsoleOnAnalysis(showConsoleOnAnalysis);
    return sonarConsoleSettings;
  }

  @Nullable
  @Override
  public SonarConsoleSettings getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull SonarConsoleSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public boolean isShowConsoleOnAnalysis() {
    return showConsoleOnAnalysis;
  }

  public void setShowConsoleOnAnalysis(boolean showConsoleOnAnalysis) {
    this.showConsoleOnAnalysis = showConsoleOnAnalysis;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SonarConsoleSettings that = (SonarConsoleSettings) o;
    return showConsoleOnAnalysis == that.showConsoleOnAnalysis;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(showConsoleOnAnalysis);
  }
}
