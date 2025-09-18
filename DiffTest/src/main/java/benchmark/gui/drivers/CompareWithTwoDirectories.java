package benchmark.gui.drivers;

import benchmark.generators.MakeToolsOutput;
import benchmark.generators.tools.ASTDiffTool;
import benchmark.gui.conf.GuiConf;
import benchmark.gui.web.BenchmarkWebDiff;
import benchmark.gui.web.BenchmarkWebDiffFactory;
import benchmark.utils.CaseInfo;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class CompareWithTwoDirectories {
    private final static Logger logger = LoggerFactory.getLogger(CompareWithTwoDirectories.class);
    private final static String abspath = "D:/recent_commits/";
    private static Set<String> passCommitSet = null;
    private static ArrayList<String> checkCommits = null;
    public static void main(String[] args) throws Exception {
//        String abspath = "D:/codediff/";
//        String folder1 = abspath+"defects4j/before/Cli/13/";
//        String folder2 = abspath+"defects4j/after/Cli/13/";
        FixingMatcher.useFix = true;
        FixingMatcher.setDbg(true);

        BufferedReader br = new BufferedReader(new FileReader("D:/diffs/recent_diffs/random_commits.txt"));
        checkCommits = new ArrayList<String>();
        String line;
        while ((line = br.readLine()) != null) {
            checkCommits.add(line);
        }

//        int i=0;
//        for(var commit: checkCommits){
//            File verified_results = new File("D:/diffs/recent_diffs/verified_results/", (++i)+"."+commit.replace("/","_")+".txt");
//            if(verified_results.exists() && verified_results.length()!=0) continue;
//            verified_results.createNewFile();
//            System.out.println("Running for ("+i+") "+commit);
//            compare(commit);
//            break;
//        }

//        new BenchmarkWebDiffFactory().withTwoDirectories(folder1,folder2).run();
//        testRepo("airbnb_lottie-android");
//        String casename = "airbnb_lottie-android/0ca5227776c9";
//        compare("airbnb_lottie-android/0ca5227776c9");

//        BufferedReader br = new BufferedReader(new FileReader(abspath+"pass_commits.txt"));
//        passCommitSet = new HashSet<>();
//        String line;
//        while ((line = br.readLine()) != null) {
//            passCommitSet.add(line);
//        }
//        for(int i=0;i<12;i++)
//            testAll();
//        System.out.println(count);
    }

    public static int count = 0;

    public static void compare(String casename){
        String repo = casename.substring(0, casename.lastIndexOf("/"));
        String commit = casename.substring(casename.lastIndexOf("/")+1);
        Map<ASTDiffTool, Set<ASTDiff>> diffs = new LinkedHashMap<>();
        FixingMatcher.useFix = true;
        ProjectASTDiff projectASTDiff = testOne(repo,commit);
        diffs.put(ASTDiffTool.RMD_Mod,projectASTDiff.getDiffSet());
        FixingMatcher.useFix = false;
        ProjectASTDiff projectASTDiff2 = testOne(repo,commit);
        diffs.put(ASTDiffTool.RMD,projectASTDiff2.getDiffSet());
        new BenchmarkWebDiff(projectASTDiff,diffs,GuiConf.defaultConf()).run();
    }

    public static void testAll() {
        File repoPath = new File(abspath);
        if (!repoPath.exists() || !repoPath.isDirectory()) {
            logger.info("Noexisted: " + repoPath.getAbsolutePath());
            return;
        }

        File[] repos = repoPath.listFiles(File::isDirectory);
        if (repos == null) {
            logger.info("Failed: " + repoPath.getAbsolutePath());
            return;
        }

        for (File repo : repos) {
            String repoName = repo.getName();
//            logger.info("Started for " + repoName);
            System.out.println("Started for " + repoName);
            testRepo(repoName);
//            logger.info("Finished for " + repoName);
        }
    }

    public static void testRepo(String repo) {
        File repoPath = new File(abspath, repo);
        if (!repoPath.exists()) {
            logger.info("Noexisted: " + repoPath.getAbsolutePath());
            return;
        }
        if (!repoPath.isDirectory()){
            return;
        }

        File[] commits = repoPath.listFiles(File::isDirectory);
        if (commits == null) {
            logger.info("Failed: " + repoPath.getAbsolutePath());
            return;
        }

        for (File commit : commits) {
            String commitName = commit.getName();
            if(passCommitSet.contains(repo+"/"+commitName)) {
//                System.out.println("Pass: " + repo + "/" + commitName);
                continue;
            }
            logger.info("Started for " + repo + "/" + commitName);
            System.out.println("Started for " + repo + "/" + commitName);
            testOne(repo,commitName);
            count+=1;
            logger.info("Finished for " + repo + "/" + commitName);
        }
    }

    public static ProjectASTDiff testOne(String repo,String commit){
        String folder1 = abspath+repo+"/"+commit+"/before";
        String folder2 = abspath+repo+"/"+commit+"/after";

        ProjectASTDiff projectASTDiff = null;
        // used to filter timeout commits
//        CompletableFuture<ProjectASTDiff> future = CompletableFuture.supplyAsync(() -> {
//            return new GitHistoryRefactoringMinerImpl().diffAtDirectories(Path.of(folder1), Path.of(folder2));
//        });
//        try {
//            projectASTDiff = future.get(60, TimeUnit.SECONDS);
//        } catch (TimeoutException e) {
//            logger.info("Timeout: "+commit);
//            System.out.println("Timeout: "+commit);
//            future.cancel(true);
//        } catch (InterruptedException | ExecutionException e) {
//            logger.info("Exception: "+commit);
//            System.out.println("Exception: "+commit);
//            future.cancel(true);
//        }
//

        projectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtDirectories(Path.of(folder1), Path.of(folder2));
        return projectASTDiff;
    }
}
