package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;

public class FixingMatcher implements TreeMatcher {
    public static boolean useFix = true;
    public static int optLevel = 3;

    public static boolean testMoveOpt = false;

    public static void setDbg(boolean isDebug) {
        BaseFixingMatcher.isDebug = isDebug;
    }

    public static void setInfos(boolean hasInfos) {
        BaseFixingMatcher.hasInfos = hasInfos;
    }

    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        if(testMoveOpt){
            new MoveOptMatcher().match(src,dst,mappingStore);
        }
        else{
            switch (optLevel) {
                case -1 -> new NodeSizeCounterFixingMatcher().match(src, dst, mappingStore);
                case 0 -> new SimpleFixingMatcher().match(src,dst,mappingStore);
                case 1 -> new LightFixingMatcher().match(src,dst,mappingStore);
                case 2 -> new FastFixingMatcher().match(src,dst,mappingStore);
                default -> new BoostFixingMatcher().match(src,dst,mappingStore);
            }
        }
    }
}