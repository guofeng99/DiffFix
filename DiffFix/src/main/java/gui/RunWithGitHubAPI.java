package gui;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import gui.webdiff.WebDiff;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/* Created by pourya on 2022-12-26 9:30 p.m. */
public class RunWithGitHubAPI {
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        String url = "https://github.com/infinispan/infinispan.git/03573a655bcbb77f7a76d8e22d851cc22796b4f8";

        String repo = url.substring(0, url.lastIndexOf("/"));
        String commit = url.substring(url.lastIndexOf("/")+1);
        FixingMatcher.setDbg(true);
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(repo, commit, new File("D:/codediff/RM-ASTDiff/src/test/resources/oracle/commits/"));
        new WebDiff(projectASTDiff,6790).run();
    }

    public static ProjectASTDiff diffAtCommitWithGitHubAPIRev(String cloneURL, String commitId, File rootFolder) {
        var gitMiner = new GitHistoryRefactoringMinerImpl();
        try {
            Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
            Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
            Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
            Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
            Map<String, String> renamedFilesHint = new ConcurrentHashMap<String, String>();
            GitHistoryRefactoringMinerImpl.ChangedFileInfo info = gitMiner.populateWithGitHubAPIAndSaveFiles(cloneURL, commitId,
                    fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent, rootFolder);
            Map<String, String> filesBefore = new LinkedHashMap<String, String>();
            Map<String, String> filesCurrent = new LinkedHashMap<String, String>();
            for(String fileName : info.getFilesBefore()) {
                if(fileContentsBefore.containsKey(fileName)) {
                    filesBefore.put(fileName, fileContentsBefore.get(fileName));
                }
            }
            for(String fileName : info.getFilesCurrent()) {
                if(fileContentsCurrent.containsKey(fileName)) {
                    filesCurrent.put(fileName, fileContentsCurrent.get(fileName));
                }
            }
            fileContentsBefore = filesCurrent; // mod
            fileContentsCurrent = filesBefore; // mod
            List<MoveSourceFolderRefactoring> moveSourceFolderRefactorings = gitMiner.processIdenticalFiles(fileContentsBefore, fileContentsCurrent, renamedFilesHint, true);
            UMLModel currentUMLModel = gitMiner.createModelForASTDiff(fileContentsCurrent, repositoryDirectoriesCurrent);
            UMLModel parentUMLModel = gitMiner.createModelForASTDiff(fileContentsBefore, repositoryDirectoriesBefore);
            UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
            ProjectASTDiffer differ = new ProjectASTDiffer(modelDiff, fileContentsBefore, fileContentsCurrent);
            return differ.getProjectASTDiff();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
