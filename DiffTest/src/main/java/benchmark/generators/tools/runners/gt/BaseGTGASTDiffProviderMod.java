package benchmark.generators.tools.runners.gt;

import benchmark.generators.tools.models.BaseASTDiffProvider;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseGTGASTDiffProviderMod extends BaseASTDiffProvider {

    protected final CompositeMatchers.CompositeMatcher matcher;

    private final static Logger logger = LoggerFactory.getLogger(BaseGTGASTDiffProviderMod.class);

    protected BaseGTGASTDiffProviderMod(CompositeMatchers.CompositeMatcher matcher, ASTDiff input) {
        super(input);
        this.matcher = matcher;
    }

    @Override
    public ASTDiff makeASTDiff() throws Exception {
        return makeASTDiffFromMatcher();
    }

    private ASTDiff makeASTDiffFromMatcher() {
        ASTDiff astDiff = input;
        Tree srcRoot = astDiff.src.getRoot();
        Tree dstRoot = astDiff.dst.getRoot();
        long GT_started = System.currentTimeMillis();
        MappingStore match = match(srcRoot, dstRoot, matcher);
        long GT_finished =  System.currentTimeMillis();
        logger.info("GumTree-greedy execution: " + (GT_finished - GT_started) + " milliseconds");
        return mappingStoreToASTDiffWithActions(srcRoot, dstRoot, match);
    }

    protected ASTDiff mappingStoreToASTDiffWithActions(Tree srcRoot, Tree dstRoot, MappingStore match) {
        ASTDiff astDiff = input;
        Tree srcParent = srcRoot.getParent();
        Tree dstParent = dstRoot.getParent();
        ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcRoot, dstRoot);
        mappingStore.add(match);
        long DFX_started = System.currentTimeMillis();
        new FixingMatcher().match(srcRoot, dstRoot, mappingStore);
        long DFX_finished =  System.currentTimeMillis();
        logger.info("GumTree-greedy Fixing execution: " + (DFX_finished - DFX_started) + " milliseconds");
        EditScript actions = new SimplifiedChawatheScriptGenerator().computeActions(mappingStore.getMonoMappingStore());
        ASTDiff diff = new ASTDiff(astDiff.getSrcPath(), astDiff.getDstPath(), astDiff.src, astDiff.dst, mappingStore, actions);
        if (diff.getAllMappings().size() != mappingStore.size())
            if (!astDiff.getSrcPath().equals("src_java_org_apache_commons_lang_math_NumberUtils.java"))
                throw new RuntimeException("Mapping has been lost!");
        srcRoot.setParent(srcParent);
        dstRoot.setParent(dstParent);
        return diff;
    }

    public static MappingStore match(Tree srcRoot, Tree dstRoot, Matcher matcher){
        return matcher.match(srcRoot, dstRoot);
    }

    public static void safeAdd(ExtendedMultiMappingStore extendedMultiMappingStore, Iterable<Mapping> match) {
        for (Mapping mapping : match) {
            safeAdd(extendedMultiMappingStore, mapping);
        }
    }

    protected static void safeAdd(ExtendedMultiMappingStore extendedMultiMappingStore, Mapping mapping) {
        if (!mapping.first.getType().name.equals(mapping.second.getType().name)) return;
        extendedMultiMappingStore.addMapping(mapping.first, mapping.second);
    }
}
