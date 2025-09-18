package gui;

import gui.webdiff.WebDiff;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.IOException;
import java.nio.file.Path;

public class RunWithTwoDirectories {
    public static void main(String[] args) throws IOException {
//        FixingMatcher.useFix=false; // only RM-ASTDiff
        FixingMatcher.useFix=true; // RM-ASTDiff +DiffFix
        FixingMatcher.setDbg(true);
        testCase("Time/23");
    }

    public static void testCase(String casename) throws IOException {
        System.out.println("Running for "+casename);
        String abspath = System.getProperty("user.dir")+"/src/test/resources/oracle/commits/defects4j/";
        String folder1 = abspath+"before/"+casename+"/";
        String folder2 = abspath+"after/"+casename+"/";
        System.out.println("Folder1: "+folder1);
        System.out.println("-----------------------------------------------");
        ProjectASTDiff projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtDirectories(Path.of(folder1), Path.of(folder2));
        new WebDiff(projectASTDiff).run();
    }
}