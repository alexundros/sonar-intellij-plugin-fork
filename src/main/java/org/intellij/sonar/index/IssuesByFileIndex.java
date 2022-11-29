package org.intellij.sonar.index;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.intellij.sonar.persistence.IssuesByFileIndexProjectService;

public class IssuesByFileIndex {

  public static Map<String, Set<SonarIssue>> getIndex(Project project) {
    final Optional<IssuesByFileIndexProjectService> indexService = IssuesByFileIndexProjectService.getInstance(
        project
    );
    if (!indexService.isPresent()) {
      return Maps.newConcurrentMap();
    } else {
      return indexService.get().getIndex();
    }
  }

  public static Set<SonarIssue> getIssuesForFile(PsiFile psiFile) {
    String fullPath = psiFile.getVirtualFile().getPath();
    Project project = psiFile.getProject();
    final Map<String, Set<SonarIssue>> index = getIndex(project);
    Set<SonarIssue> issues = index.get(fullPath);
    if (issues == null) {
      issues = Sets.newLinkedHashSet();
    }
    return issues;
  }

  public static void clearIndexFor(Collection<PsiFile> psiFiles) {
    for (PsiFile psiFile : psiFiles) {
      getIndex(psiFile.getProject()).remove(psiFile.getVirtualFile().getPath());
    }
  }
}
