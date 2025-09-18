package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.optimizations.LcsOptMatcherThetaB;
import com.github.gumtreediff.matchers.optimizations.UnmappedLeavesMatcherThetaC;
import com.github.gumtreediff.matchers.optimizations.InnerNodesMatcherThetaD;
import com.github.gumtreediff.matchers.optimizations.LeafMoveMatcherThetaE;
import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveOptMatcher implements TreeMatcher {
    public static boolean isDebug = false;

    private final static Logger logger = LoggerFactory.getLogger(MoveOptMatcher.class);

    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        var mappings = mappingStore.getMonoMappingStore();
        if(isDebug){
            long pass_started,pass_finished;

            pass_started = System.currentTimeMillis();
            new LcsOptMatcherThetaB().match(src, dst, mappings);
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: LcsOptMatcherThetaB, execution: {} milliseconds", pass_finished - pass_started);

            pass_started = System.currentTimeMillis();
            new UnmappedLeavesMatcherThetaC().match(src, dst, mappings);
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: UnmappedLeavesMatcherThetaC, execution: {} milliseconds", pass_finished - pass_started);

            pass_started = System.currentTimeMillis();
            new InnerNodesMatcherThetaD().match(src, dst, mappings);
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: InnerNodesMatcherThetaD, execution: {} milliseconds", pass_finished - pass_started);

            pass_started = System.currentTimeMillis();
            new LeafMoveMatcherThetaE().match(src, dst, mappings);
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: LeafMoveMatcherThetaE, execution: {} milliseconds", pass_finished - pass_started);
        }
        else{
            new LcsOptMatcherThetaB().match(src, dst, mappings);
            new UnmappedLeavesMatcherThetaC().match(src, dst, mappings);
            new InnerNodesMatcherThetaD().match(src, dst, mappings);
            new LeafMoveMatcherThetaE().match(src, dst, mappings);
        }

        for (var mapping : mappings) {
            if(mappingStore.isSrcMapped(mapping.first) && mappingStore.getDsts(mapping.first).contains(mapping.second)) {
                continue;
            }
            if(mappingStore.isSrcMapped(mapping.first) && mappingStore.isSrcUnique(mapping.first)){
                mappingStore.removeMapping(mapping.first, mappingStore.getDsts(mapping.first).iterator().next());
            }
            if(mappingStore.isDstMapped(mapping.second) && mappingStore.isDstUnique(mapping.second)){
                mappingStore.removeMapping(mappingStore.getSrcs(mapping.second).iterator().next(), mapping.second);
            }
            if(!mappingStore.isSrcMapped(mapping.first) && !mappingStore.isDstMapped(mapping.second)){
                mappingStore.addMapping(mapping.first, mapping.second);
            }
        }
    }
}