package org.intellij.sonar.configuration.partials;

import static org.intellij.sonar.configuration.SonarQualifier.MODULE;
import static org.intellij.sonar.configuration.SonarQualifier.PROJECT;
import static org.intellij.sonar.persistence.SonarServers.NO_SONAR;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.intellij.sonar.configuration.ResourcesSelectionConfigurable;
import org.intellij.sonar.persistence.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SonarResourcesTableView {

  private static final Map<String, String> TYPE_BY_QUALIFIER = ImmutableMap.<String, String>builder()
      .put(PROJECT.getQualifier(), "Project")
      .put(MODULE.getQualifier(), "Module")
      .build();
  private static final ColumnInfo<Resource, String> TYPE_COLUMN = new ColumnInfo<Resource, String>("Type") {
    @Nullable
    @Override
    public String valueOf(Resource sonarResource) {
      String qualifier = sonarResource.getQualifier();
      return Optional.ofNullable(TYPE_BY_QUALIFIER.get(qualifier)).orElse(qualifier);
    }
  };
  private static final ColumnInfo<Resource, String> NAME_COLUMN = new ColumnInfo<Resource, String>("Name") {
    @Nullable
    @Override
    public String valueOf(Resource sonarResource) {
      return sonarResource.getName();
    }
  };
  private static final ColumnInfo<Resource, String> KEY_COLUMN = new ColumnInfo<Resource, String>("Key") {
    @Nullable
    @Override
    public String valueOf(Resource sonarResource) {
      return sonarResource.getKey();
    }
  };

  private final TableView<Resource> myResourcesTable;
  private final JComponent myJComponent;
  private final SonarServersView myServersView;
  private final Project myProject;

  public SonarResourcesTableView(Project project, SonarServersView sonarServersView) {
    this.myServersView = sonarServersView;
    this.myResourcesTable = new TableView<>();
    this.myJComponent = createJComponent();
    this.myProject = project;
  }

  private JComponent createJComponent() {
    JPanel panelForTable = ToolbarDecorator.createDecorator(myResourcesTable, null).
        setAddAction(
            addAction()
        ).
        setRemoveAction(
            anActionButton -> TableUtil.removeSelectedItems(myResourcesTable)
        )
        .disableUpDownActions()
        .createPanel();
    panelForTable.setPreferredSize(new Dimension(-1, 100));
    return panelForTable;
  }

  @NotNull
  private AnActionButtonRunnable addAction() {
    return anActionButton -> {
      final String selectedServerName = Optional.ofNullable(myServersView.getSelectedItem())
          .orElse(NO_SONAR);
      if (!NO_SONAR.equals(selectedServerName)) {
        ResourcesSelectionConfigurable dlg = new ResourcesSelectionConfigurable(
            myProject,
            selectedServerName
        );
        dlg.show();
        if (dlg.isOK()) {
          setResourcesModel(dlg);
        }
      }
    };
  }

  private void setResourcesModel(ResourcesSelectionConfigurable dlg) {
    final List<Resource> selectedResources = dlg.getSelectedResources();
    final List<Resource> currentResources = myResourcesTable.getItems();
    Set<Resource> mergedResourcesAsSet = new TreeSet<>(
        Comparator.comparing(Resource::getKey)
    );
    mergedResourcesAsSet.addAll(currentResources);
    mergedResourcesAsSet.addAll(selectedResources);
    setModel(Lists.newArrayList(mergedResourcesAsSet));
  }

  public void setModel(List<Resource> sonarResources) {
    myResourcesTable.setModelAndUpdateColumns(
        new ListTableModel<>(
            new ColumnInfo[]{
                NAME_COLUMN,
                KEY_COLUMN,
                TYPE_COLUMN
            }, sonarResources, 0
        )
    );
  }

  public TableView<Resource> getTable() {
    return myResourcesTable;
  }

  public JComponent getComponent() {
    return myJComponent;
  }
}
