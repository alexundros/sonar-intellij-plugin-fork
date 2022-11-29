package org.intellij.sonar.persistence;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "sonarModuleSettings",
    storages = {
        @Storage("$MODULE_FILE$")
    }
)
public class ModuleSettings implements PersistentStateComponent<Settings> {

  protected Settings settings = new Settings();

  public static ModuleSettings getInstance(Module module) {
    return module.getService(ModuleSettings.class);
  }

  @Nullable
  @Override
  public Settings getState() {
    if (settings != null) {
      if (StringUtil.isEmpty(settings.getServerName())) {
        settings.setServerName(SonarServers.PROJECT);
      }
      if (StringUtil.isEmpty(settings.getLocalAnalysisScripName())) {
        settings.setLocalAnalysisScripName(LocalAnalysisScripts.PROJECT);
      }
    }
    return settings;
  }

  @Override
  public void loadState(@NotNull Settings settings) {
    this.settings = settings;
  }

}
