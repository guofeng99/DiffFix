package benchmark.generators.tools.runners.gt;

import com.github.gumtreediff.matchers.CompositeMatchers;
import org.refactoringminer.astDiff.models.ASTDiff;

public class SimpleGumTreeASTDiffProviderMod extends BaseGTSASTDiffProviderMod {
    public SimpleGumTreeASTDiffProviderMod(ASTDiff input) {
        super(new CompositeMatchers.SimpleGumtree(), input);
    }
}
