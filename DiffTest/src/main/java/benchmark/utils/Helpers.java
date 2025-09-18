package benchmark.utils;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.astDiff.matchers.ProjectASTDiffer;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static benchmark.utils.Configuration.ConfigurationFactory.ORACLE_DIR;
import static benchmark.utils.PathResolver.getAfterDir;
import static benchmark.utils.PathResolver.getBeforeDir;


public class Helpers {

    public static ProjectASTDiff runWhatever(String repo, String commit) {
        ProjectASTDiff projectASTDiff;
        if (repo.contains("github")) {
            projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(repo, commit, new File(ORACLE_DIR));
        }
        else{
            Path beforePath = Path.of(getBeforeDir(repo, commit));
            Path afterPath = Path.of(getAfterDir(repo, commit));
            projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtDirectories(
                beforePath, afterPath);
        }
        return projectASTDiff;
    }
    public static ProjectASTDiff runWhatever(CaseInfo info) {
        return runWhatever(info.repo, info.commit);
    }

    public static rm2.refactoringminer.astDiff.actions.ProjectASTDiff runWhateverForRM2(CaseInfo caseInfo) {
        String repo = caseInfo.repo;
        String commit = caseInfo.commit;
        if (repo == null){
            if (caseInfo.srcPath != null && caseInfo.dstPath != null)
                return new rm2.refactoringminer.rm1.GitHistoryRefactoringMinerImpl().diffAtDirectories(new File(caseInfo.srcPath), new File(caseInfo.dstPath));
        }
        rm2.refactoringminer.astDiff.actions.ProjectASTDiff projectASTDiff;
        if (repo.contains("github")) {
            projectASTDiff = new rm2.refactoringminer.rm1.GitHistoryRefactoringMinerImpl().diffAtCommitWithGitHubAPI(repo, commit, new File(ORACLE_DIR));
        }
        else{
            Path beforePath = Path.of(getBeforeDir(repo, commit));
            Path afterPath = Path.of(getAfterDir(repo, commit));
            projectASTDiff = new rm2.refactoringminer.rm1.GitHistoryRefactoringMinerImpl().diffAtDirectories(
                    beforePath, afterPath);
        }
        return projectASTDiff;
    }
}