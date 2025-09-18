package benchmark.generators;

import benchmark.utils.Configuration.ConfigurationFactory;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.nio.file.Path;

public class MakeToolsOutput {

    private final static Logger logger = LoggerFactory.getLogger(MakeToolsOutput.class);

    public static void main(String[] args) throws Exception {
        FixingMatcher.useFix = true;
        FixingMatcher.setDbg(false);

//        for(int i=0;i<12;++i){
//            logger.info("Iteration: "+i);
//            new BenchmarkHumanReadableDiffGenerator(ConfigurationFactory.defects4j()).generateSingleThreaded();
//            new BenchmarkHumanReadableDiffGenerator(ConfigurationFactory.refOracle()).generateSingleThreaded();
//        }

        new BenchmarkHumanReadableDiffGenerator(ConfigurationFactory.defects4j()).generateSingleThreaded();
//        new BenchmarkHumanReadableDiffGenerator(ConfigurationFactory.refOracle()).generateSingleThreaded();
//        new BenchmarkHumanReadableDiffGenerator(ConfigurationFactory.defects4jTwoPointOne()).generateSingleThreaded();
//        new BenchmarkHumanReadableDiffGenerator(ConfigurationFactory.refOracleTwoPointOne()).generateSingleThreaded();

    }
}