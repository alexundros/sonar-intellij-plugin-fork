package org.intellij.sonar.persistence;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

@State(
    name = "sonarServers",
    storages = {
        @Storage("sonarSettings.xml")
    }
)
public class SonarServers implements PersistentStateComponent<SonarServers> {

  public static final String NO_SONAR = "<NO SONAR>";
  public static final String PROJECT = "<PROJECT>";
  public Collection<SonarServerConfig> beans = new ArrayList<>();

  @NotNull
  public static SonarServers getInstance() {
    return ServiceManager.getService(SonarServers.class);
  }

  public static void add(final SonarServerConfig newServerConfig) {
    final Collection<SonarServerConfig> serverConfigs = SonarServers.getInstance().getState().beans;
    final boolean alreadyExists = serverConfigs.stream().anyMatch(it -> it.equals(newServerConfig));
    if (alreadyExists) {
      throw new IllegalArgumentException("already exists");
    } else {
      serverConfigs.add(newServerConfig);
      if (newServerConfig.isPasswordChanged()) {
        newServerConfig.storePassword();
      }
      if (StringUtils.isNotBlank(newServerConfig.getToken())) {
        newServerConfig.storeToken();
      }
      newServerConfig.clearToken();
      newServerConfig.clearPassword();
    }
  }

  public static void remove(@NotNull final String serverName) {
    final Optional<SonarServerConfig> bean = get(serverName);
    Preconditions.checkArgument(bean.isPresent());
    getAll().ifPresent(serverConfigs -> getInstance().beans = serverConfigs.stream()
        .filter(serverConfigurationBean -> !bean.get().equals(serverConfigurationBean))
        .collect(Collectors.toCollection(LinkedList::new)));
  }

  public static Optional<SonarServerConfig> get(@NotNull final String sonarServerName) {
    Optional<SonarServerConfig> bean = Optional.empty();
    final Optional<Collection<SonarServerConfig>> allBeans = getAll();
    if (allBeans.isPresent()) {
      bean = allBeans.get().stream().filter(serverConfigBean -> sonarServerName.equals(serverConfigBean.getName())).findFirst();
    }
    return bean;
  }

  public static Optional<Collection<SonarServerConfig>> getAll() {
    return Optional.ofNullable(SonarServers.getInstance().getState().beans);
  }

  @NotNull
  @Override
  public SonarServers getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull SonarServers state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SonarServers that = (SonarServers) o;
    return Objects.equal(beans, that.beans);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(beans);
  }
}
