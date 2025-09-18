package benchmark.gui.drivers;

import benchmark.gui.web.BenchmarkWebDiff;
import benchmark.gui.web.BenchmarkWebDiffFactory;

public class CompareWithGitHubAPI {
    public static void main(String[] args) throws Exception {
        String url = "https://github.com/apache/pig.git/92dce401344a28ff966ad4cf3dd969a676852315";
        new BenchmarkWebDiffFactory().withURL(url).run();
    }
}
