package benchmark.generators.tools.runners.gt;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ASTDiff;

/* Created by pourya on 2024-05-02*/
public class SimpleGumTreeASTDiffProvider extends BaseGumTreeASTDiffProvider {
    public SimpleGumTreeASTDiffProvider(ASTDiff input) {
        super(new CompositeMatchers.SimpleGumtree(), input);
    }
}
