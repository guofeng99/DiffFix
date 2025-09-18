package benchmark.generators.tools.runners;

import benchmark.generators.tools.models.ASTDiffProviderFromProjectASTDiff;
import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import iast.com.github.gumtreediff.matchers.Mapping;
import iast.com.github.gumtreediff.tree.ITree;
import org.refactoringminer.astDiff.matchers.FixingMatcher;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static benchmark.generators.tools.runners.Utils.getTreesExactPosition;
import static benchmark.generators.tools.runners.shaded.AbstractASTDiffProviderFromIncompatibleTree.diffToASTDiffWithActions;

public class IASTMapperMod extends ASTDiffProviderFromProjectASTDiff {
    private final String srcContents;
    private final String dstContents;

    private final static Logger logger = LoggerFactory.getLogger(IASTMapperMod.class);


    public IASTMapperMod(ProjectASTDiff projectASTDiff, ASTDiff input) {
        super(projectASTDiff, input);
        this.srcContents = projectASTDiff.getFileContentsBefore().get(input.getSrcPath());
        this.dstContents = projectASTDiff.getFileContentsAfter().get(input.getDstPath());
    }

    public ASTDiff makeASTDiff() throws Exception {
        return diffToASTDiffWithActions(diff(), input.getSrcPath(), input.getDstPath());
    }
    public Diff diff() throws Exception {
        cs.model.algorithm.iASTMapper m = new cs.model.algorithm.iASTMapper(srcContents, dstContents);
        long IAM_started = System.currentTimeMillis();
        m.buildMappingsOuterLoop();
        long IAM_finished =  System.currentTimeMillis();
        logger.info("iASTMapper execution: " + (IAM_finished - IAM_started) + " milliseconds");
//        Tree srcMirror = mirrorTree(m.getSrc());
//        Tree dstMirror = mirrorTree(m.getDst());
//
//        TreeContext srcTC = new TreeContext();
//        srcTC.setRoot(srcMirror);
//        TreeContext dstTC = new TreeContext();
//        dstTC.setRoot(dstMirror);
        Tree srcMirror = input.src.getRoot();
        Tree dstMirror = input.dst.getRoot();
        MappingStore mappingStore = new MappingStore(srcMirror,dstMirror);
        ExtendedMultiMappingStore mappingStore2 = new ExtendedMultiMappingStore(srcMirror, dstMirror);
        EditScript editScript = new EditScript();
        try {
            for (Mapping mapping : m.getTreeMappings()) {
                Tree firstMirror = findMirror(mapping.first, srcMirror);
                Tree secondMirror = findMirror(mapping.second, dstMirror);
                if(firstMirror==null || secondMirror==null){
                    continue;
                }
//                assert firstMirror != null;
//                assert secondMirror != null;
                if (!firstMirror.getType().name.equals(secondMirror.getType().name)) {
                    logger.info("Types are not equal: " +
                            firstMirror + " " + secondMirror);
//                    continue;
                }
                else
                    mappingStore.addMapping(firstMirror, secondMirror);
            }
            fixJavadoc(mappingStore);
//            editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappingStore);
            mappingStore2.add(mappingStore);
            long DFX_started = System.currentTimeMillis();
            new FixingMatcher().match(srcMirror, dstMirror, mappingStore2);
            long DFX_finished =  System.currentTimeMillis();
            logger.info("iASTMapper Fixing execution: " + (DFX_finished - DFX_started) + " milliseconds");
            editScript = new SimplifiedChawatheScriptGenerator().computeActions(mappingStore2.getMonoMappingStore());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Must check",e);
        }
        return new Diff(input.src, input.dst, mappingStore2.getMonoMappingStore(), editScript);
    }

    private void fixJavadoc(com.github.gumtreediff.matchers.MappingStore mappingStore) throws Exception {
        var Mappings = this.input.getAllMappings();
        for(var mapping : Mappings)
        {
            if(mapping.first.getParents().contains(mappingStore.src) && mapping.second.getParents().contains(mappingStore.dst)){
                if(!mappingStore.isSrcMapped(mapping.first) && !mappingStore.isDstMapped(mapping.second)){
                    if(TreeUtilFunctions.getParentUntilType(mapping.first,"Javadoc")!=null){
                        mappingStore.addMapping(mapping.first,mapping.second);
                    }
                    else if(mapping.first.getType().name.equals("VARARGS_TYPE")){
                        var p1 = mapping.first.getParent().getParent();
                        var p2 = mapping.second.getParent().getParent();
                        if(p1.getMetrics().hash == p2.getMetrics().hash && Mappings.getDsts(p1).contains(p2)){
                            mappingStore.addMappingRecursively(p1,p2);
                        }
                    }
                }
            }
        }
    }

    private static Tree findMirror(ITree iTree, Tree fullTree) throws Exception {
        List<Tree> treesBetweenPositions = getTreesExactPosition(fullTree, iTree.getPos(), iTree.getEndPos());
        for (Tree treeBetweenPosition : treesBetweenPositions) {
            if (treeBetweenPosition.getType().name.equals(iTree.getType().name))
                return treeBetweenPosition;
        }
        return null;
    }

    private static Tree findMirror(Tree t, Tree fullTree) throws Exception {
        List<Tree> treesBetweenPositions = getTreesExactPosition(fullTree, t.getPos(), t.getEndPos());
        for (Tree treeBetweenPosition : treesBetweenPositions) {
            if (treeBetweenPosition.getType().name.equals(t.getType().name))
                return treeBetweenPosition;
        }
        return null;
    }

    private static Tree mirrorTree(ITree iTree) throws Exception {
        String replacedType = iTree.getType().name;
        DefaultTree curr = new DefaultTree(TypeSet.type(replacedType));
        curr.setLabel(iTree.getLabel());
        curr.setPos(iTree.getPos());
        curr.setLength(iTree.getLength());
        for (ITree iChild : iTree.getChildren()) {
            Tree childMirror = mirrorTree(iChild);
            childMirror.setParent(curr);
            curr.addChild(childMirror);

        }
        return curr;
    }


    private static Tree whichTree(cs.model.algorithm.iASTMapper m, Tree srcMirror, Tree dstMirror, ITree input) throws Exception {
        ITree tempParent = input;
        while (tempParent.getParent() != null)
            tempParent = tempParent.getParent();
        Tree decision;
        if (tempParent == m.getSrc())
            decision = srcMirror;
        else if (tempParent == m.getDst())
            decision = dstMirror;
        else
            throw new Exception("Must check");
        return decision;
    }
}
