package benchmark.gui.drivers;

import benchmark.gui.web.BenchmarkWebDiff;
import benchmark.gui.web.BenchmarkWebDiffFactory;
import benchmark.utils.CaseInfo;
import benchmark.utils.Configuration.Configuration;
import benchmark.utils.Configuration.ConfigurationFactory;
import org.refactoringminer.astDiff.matchers.FixingMatcher;

public class CompareWithCaseInfo {
    public static void main(String[] args) {
        String casename="https://github.com/infinispan/infinispan.git/03573a655bcbb77f7a76d8e22d851cc22796b4f8";
        FixingMatcher.useFix = false;
//        FixingMatcher.testMoveOpt = true;
        FixingMatcher.setDbg(false);

        String repo = casename.substring(0, casename.lastIndexOf("/"));
        String commit = casename.substring(casename.lastIndexOf("/")+1);
        CaseInfo info = new CaseInfo(repo, commit);
        BenchmarkWebDiff benchmarkWebDiff = null;
        try {
            benchmarkWebDiff = new BenchmarkWebDiffFactory().withCaseInfo(info);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        benchmarkWebDiff.run();
    }
}