package benchmark.generators;


import benchmark.generators.hrd.HumanReadableDiffGenerator;
import benchmark.generators.tools.ASTDiffTool;
import benchmark.generators.tools.runners.DATConf;
import benchmark.utils.CaseInfo;
import benchmark.utils.Configuration.Configuration;
import benchmark.utils.Configuration.ConfigurationFactory;
import benchmark.utils.Configuration.GenerationStrategy;
import benchmark.utils.Helpers;
import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.actions.model.MoveIn;
import org.refactoringminer.astDiff.actions.model.MoveOut;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static benchmark.utils.Helpers.runWhatever;

public class BenchmarkHumanReadableDiffGenerator {

    private final Configuration configuration;

    private final static Logger logger = LoggerFactory.getLogger(BenchmarkHumanReadableDiffGenerator.class);

    public BenchmarkHumanReadableDiffGenerator(Configuration current){
        this.configuration = current;
    }
    public void generateMultiThreaded(int numThreads) {
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(configuration.getAllCases().size());
        for (CaseInfo info : configuration.getAllCases()) {
            executorService.submit(() -> {
                try {
                    writeActiveTools(info, configuration.getOutputFolder());
                } catch (Exception e) {
                    System.out.println(info.getRepo()+"/"+info.getCommit()+" : "+e.getMessage());
                    System.exit(1); // Terminate execution with status code 1
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
//                while(retry){
//                    try {
//                        try_count++;
//                        writeActiveTools(info, configuration.getOutputFolder());
//                        retry=false;
//                        break;
//                    } catch (Exception e) {
////                        System.out.println(e.getMessage());
//                        System.out.println(info.getRepo()+"/"+info.getCommit()+" : "+e.getMessage());
//                        if(!e.getMessage().startsWith("Cannot invoke \"Object.hashCode()\"") || try_count==5){
//                            retry=false;
//                            System.exit(1); // Terminate execution with status code 1
//                            throw new RuntimeException(e);
//                        }
//                        else{
//                            retry=true;
//                        }
//                    } finally {
//                        if(!retry)
//                            latch.countDown();
//                    }
//                }
            });
        }

        executorService.shutdown();
        try {
            // Wait until all threads finish their work
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Finished generating human readable diffs...");
    }
    public void generateMultiThreaded(){
        generateMultiThreaded(Runtime.getRuntime().availableProcessors());
    }

    public void testCase(String casename) throws Exception{
        String repo = casename.substring(0, casename.lastIndexOf("/"));
        String commit = casename.substring(casename.lastIndexOf("/")+1);
        for (CaseInfo info : configuration.getAllCases()) {
            if (!(info.getRepo().equals(repo) && info.getCommit().equals(commit)))
                continue;
            writeActiveTools(info, configuration.getOutputFolder());
//            try {
//            } catch (Exception e) {
//                System.out.println(e.getMessage());
//                System.exit(1); // Terminate execution with status code 1
//                throw new RuntimeException(e);
//            }
        }
    }

    public void generateSingleThreaded() throws Exception {
        for (CaseInfo info : configuration.getAllCases()) {
            writeActiveTools(info, configuration.getOutputFolder());
        }
        System.out.println("Finished generating human readable diffs...");
    }

    private void writeActiveTools(CaseInfo info, String output_folder) throws Exception {
        String repo = info.getRepo();
        String commit = info.getCommit();
        logger.info("Started for {}/{}", repo, commit);

        ProjectASTDiff projectASTDiff = runWhatever(repo, commit);

        Set<ASTDiff> astDiffs = projectASTDiff.getDiffSet();

//        var totalsize=0;
//        for(var srcTree: projectASTDiff.getParentContextMap().values()){
//            totalsize+=srcTree.getRoot().getMetrics().size;
//        }
//
//        logger.info("Srcs size: {}",totalsize);

        for (ASTDiff astDiff : astDiffs) {

            // create each file pair for DAT
//            if(!dir.exists()){
//                dir.mkdir();
//            }
//            if(dir.exists()){
//                File file1 = new File(dirpath+"/src.java");
//                File file2 = new File(dirpath+"/dst.java");
//                var fileContent1 = (String) astDiff.src.getRoot().getMetadata("fileContent");
//                var fileContent2 = (String) astDiff.dst.getRoot().getMetadata("fileContent");
//                BufferedWriter bf = null;
//                try{
//                    if(fileContent1!=null){
//                        bf = new BufferedWriter(new FileWriter(file1));
//                        bf.write(fileContent1);
//                        bf.flush();
//                    }
//                    if(fileContent2!=null){
//                        bf = new BufferedWriter(new FileWriter(file2));
//                        bf.write(fileContent2);
//                        bf.flush();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            logger.info("file name: {}",astDiff.getSrcPath());

            ASTDiff oracle = ASTDiffTool.GOD.diff(projectASTDiff, astDiff, info, configuration);

            //----------------------------------\\
            for (ASTDiffTool tool : configuration.getActiveTools()) {

                if(tool.name().startsWith("DAT")){
                    DATConf.getConf(repo,commit,astDiff.getSrcPath(),astDiff.getDstPath());
                }
                String toolPath = tool.name(); //In case we later introduce a map from tool's name to tool's path
                ASTDiff generated = tool.diff(projectASTDiff, astDiff, info, configuration);
//                for(var action:generated.editScript){
//                    System.out.println(action);
//                }
            }
        }
        logger.info("Finished for {}/{}",repo,commit);
    }

    private char getSetRelation(Set<Tree> s1,Set<Tree> s2){
        if(s1.containsAll(s2) && s2.containsAll(s1)){
            return '=';
        }
        else if(s1.containsAll(s2)){
            return '>';
        }
        else if(s2.containsAll(s1)){
            return '<';
        }
        return '!';
    }

    private void logDetailErrors(ExtendedMultiMappingStore generatedMappings, ExtendedMultiMappingStore oracleMappings,String toolname) {
        int missingMappingCnt = 0, arbitraryMappingCnt = 0, wrongMappingCnt = 0;
        int missingMulMappingCnt = 0, arbitraryMulMappingCnt = 0, wrongMulMappingCnt = 0;
        int otherCase = 0;
        var visSrcs = new HashSet<Tree>();
        var visDsts = new HashSet<Tree>();
        for (var mapping : generatedMappings) {
            if(visSrcs.contains(mapping.first) || visDsts.contains(mapping.second)){
                continue;
            }
            if(generatedMappings.isSrcUnique(mapping.first) && generatedMappings.isDstUnique(mapping.second)){
                visSrcs.add(mapping.first);
                visDsts.add(mapping.second);

                if((!oracleMappings.isSrcMapped(mapping.first) || oracleMappings.isSrcUnique(mapping.first))
                        && (!oracleMappings.isDstMapped(mapping.second) || oracleMappings.isDstUnique(mapping.second))){
                    if (!oracleMappings.isSrcMapped(mapping.first) && !oracleMappings.isDstMapped(mapping.second)) {
                        arbitraryMappingCnt++;
                    }
                    else if(!(oracleMappings.isSrcMapped(mapping.first) && oracleMappings.getDsts(mapping.first).contains(mapping.second))){
                        wrongMappingCnt++;
                    }
                }
                else if(oracleMappings.isSrcMapped(mapping.first) && !oracleMappings.isSrcUnique(mapping.first)){
                    if(oracleMappings.getDsts(mapping.first).contains(mapping.second)){
                        missingMulMappingCnt++;
                    }
                    else{
                        wrongMulMappingCnt++;
                    }
                }
                else if(oracleMappings.isDstMapped(mapping.second) && !oracleMappings.isDstUnique(mapping.second)){
                    if(oracleMappings.getSrcs(mapping.second).contains(mapping.first)){
                        missingMulMappingCnt++;
                    }
                    else{
                        wrongMulMappingCnt++;
                    }
                }
                else{
                    otherCase++;
                }
            }
            else if((oracleMappings.isSrcMapped(mapping.first) && !oracleMappings.isSrcUnique(mapping.first)) &&
                oracleMappings.isDstMapped(mapping.second) && !oracleMappings.isDstUnique(mapping.second)) {
                otherCase++;
            }
            else if(!generatedMappings.isSrcUnique(mapping.first)){
                visSrcs.add(mapping.first);

                if(oracleMappings.isSrcMapped(mapping.first)){
                    switch (getSetRelation(generatedMappings.getDsts(mapping.first),oracleMappings.getDsts(mapping.first))){
                        case '=' -> {}
                        case '>' -> arbitraryMulMappingCnt++;
                        case '<' -> missingMulMappingCnt++;
                        case '!' -> wrongMulMappingCnt++;
                    }
                }
                else{
                    arbitraryMulMappingCnt++;
                }
            }
            else if(!generatedMappings.isDstUnique(mapping.second)){
                visDsts.add(mapping.second);

                if(oracleMappings.isDstMapped(mapping.second)){
                    switch (getSetRelation(generatedMappings.getSrcs(mapping.second),oracleMappings.getSrcs(mapping.second))){
                        case '=' -> {}
                        case '>' -> arbitraryMulMappingCnt++;
                        case '<' -> missingMulMappingCnt++;
                        case '!' -> wrongMulMappingCnt++;
                    }
                }
                else{
                    arbitraryMulMappingCnt++;
                }
            }
            else{
                otherCase++;
            }
        }

        for (var mapping : oracleMappings) {
            if(visSrcs.contains(mapping.first) || visDsts.contains(mapping.second)){
                continue;
            }
            if(oracleMappings.isSrcUnique(mapping.first) && oracleMappings.isDstUnique(mapping.second)){
                if (!generatedMappings.isSrcMapped(mapping.first) && !generatedMappings.isDstMapped(mapping.second)) {
                    missingMappingCnt++;
                }
            }
            else if(!oracleMappings.isSrcUnique(mapping.first)){
                visSrcs.add(mapping.first);
                if(!generatedMappings.isSrcMapped(mapping.first)){
                    missingMulMappingCnt++;
                }
            }
            else if(!oracleMappings.isDstUnique(mapping.second)){
                visDsts.add(mapping.second);
                if(!generatedMappings.isDstMapped(mapping.second)){
                    missingMulMappingCnt++;
                }
            }
        }

        logger.info("tool name: {}, missingMappingCnt: {}, arbitraryMappingCnt: {}, wrongMappingCnt: {}, missingMulMappingCnt: {}, arbitraryMulMappingCnt: {}, wrongMulMappingCnt: {}, otherCase: {}",
                toolname,missingMappingCnt,arbitraryMappingCnt,wrongMappingCnt,missingMulMappingCnt,arbitraryMulMappingCnt,wrongMulMappingCnt,otherCase);
    }

    private void logDetailActions(EditScript editScript,String toolname){
        int inscnt=0, delcnt=0, movcnt=0, repcnt=0, movincnt=0, movoutcnt=0,mulmovcnt=0;
        for(var action:editScript){
            if(action instanceof TreeInsert){
                inscnt += action.getNode().getMetrics().size;
            }
            else if(action instanceof TreeDelete){
                delcnt += action.getNode().getMetrics().size;
            }
            else if(action instanceof MultiMove){
                mulmovcnt++;
            }
            else if(action instanceof MoveOut){
                movoutcnt++;
            }
            else if(action instanceof MoveIn){
                movincnt++;
            }
            else if(action instanceof Insert){
                inscnt++;
            }
            else if(action instanceof Delete){
                delcnt++;
            }
            else if(action instanceof Move){
                movcnt++;
            }
            else if(action instanceof Update){
                repcnt++;
            }
        }

        logger.info("tool name: {}, es size: {}, inscnt: {}, delcnt: {}, repcnt: {}, movcnt: {}, mulmovcnt: {}, movoutcnt: {}, movincnt: {}",
                toolname,inscnt+delcnt+repcnt+movcnt+mulmovcnt+movincnt+movoutcnt,inscnt,delcnt,repcnt,movcnt,mulmovcnt,movoutcnt,movincnt);
    }

    private int getRawESSize(EditScript editScript){
        int inscnt=0, delcnt=0, movcnt=0, repcnt=0;
        for(var action:editScript){
            if(action instanceof MoveIn) continue;
            if(action instanceof TreeInsert){
                inscnt += action.getNode().getMetrics().size;
            }
            else if(action instanceof TreeDelete){
                delcnt += action.getNode().getMetrics().size;
            }
            else if(action instanceof MultiMove || action instanceof MoveOut){
                movcnt++;
            }
            else if(action instanceof Insert){
                inscnt++;
            }
            else if(action instanceof Delete){
                delcnt++;
            }
            else if(action instanceof Move){
                movcnt++;
            }
            else if(action instanceof Update){
                repcnt++;
            }
        }
        return inscnt+delcnt+repcnt+movcnt;
    }

    private int getRawESSi2e(ASTDiff diff){
        // error-prone in RMD
        MappingStore mappings = diff.getAllMappings().getMonoMappingStore();
        MappingStore mappings2 = new MappingStore(mappings.src,mappings.dst);
//        for(var mapping:mappings){
//            if(mapping.first.getParents().contains(mappings.src) && mapping.second.getParents().contains(mappings.dst)){
//                mappings2.addMapping(mapping.first,mapping.second);
//            }
//        }
        EditScript actions = new ChawatheScriptGenerator().computeActions(mappings);
        return actions.size();
    }
}