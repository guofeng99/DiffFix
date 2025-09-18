package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/* Created on 2024-10-31 */
public class SimpleFixingMatcher extends BaseFixingMatcher{

    private final static Logger logger = LoggerFactory.getLogger(SimpleFixingMatcher.class);

    @Override
    public void match(Tree src, Tree dst, ExtendedMultiMappingStore mappingStore) {
        if(src.getMetrics().hash==dst.getMetrics().hash)
            return;

        if(!(src.hasSameType(dst) && src.getType().name.equals(Constants.COMPILATION_UNIT)))
            return;

        initDS(src,dst,mappingStore);

        if(isDebug){
            long pass_started;
            long pass_finished;

            current_pass = "WarmUp";
            pass_started = System.currentTimeMillis();
            warmUp();
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByUnique";
            pass_started = System.currentTimeMillis();
            matchByUnique(getAllMappedSrcs()); // around context only one missing
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByChildren";
            pass_started = System.currentTimeMillis();
            fixByChildrenContext(getAllMappedSrcs()); // children context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByParent";
            pass_started = System.currentTimeMillis();
            fixByParentContext(getAllMappedSrcs()); // parent context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            current_pass = "fixUniqueAround";
//            pass_started = System.currentTimeMillis();
//            fixUniqueAround(getAllMappedSrcs());
//            pass_finished =  System.currentTimeMillis();
//            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByInner";
            pass_started = System.currentTimeMillis();
            fixByInnerContext(getAllMappedSrcs()); // fix calls; remove arbitrary mappings
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByNearby";
            pass_started = System.currentTimeMillis();
            fixByNearbyContext(getAllMappedSrcs()); // nearby statement; identical statement; nearby condition
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            current_pass = "matchByParentContextPre";
//            pass_started = System.currentTimeMillis();
//            matchByParentContextPre(getAllMappedSrcs());
//            pass_finished =  System.currentTimeMillis();
//            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            if(hasInfos){
//                current_pass = "fixByDecl";
//                pass_started = System.currentTimeMillis();
//                fixByDecl(getAllMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//
//                current_pass = "fixByUse";
//                pass_started = System.currentTimeMillis();
//                fixByUse(getAllMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//            }

            current_pass = "MatchByInner";
            pass_started = System.currentTimeMillis();
            matchByInnerContext(getAllMappedSrcs()); // inner call; this and null in return; conditional expression
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByNearby";
            pass_started = System.currentTimeMillis();
            matchByNearbyContext(getAllMappedSrcs()); // nearby statement;
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByChildren";
            pass_started = System.currentTimeMillis();
            matchByChildrenContext(getAllMappedSrcs()); // descendants context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByParent";
            pass_started = System.currentTimeMillis();
            matchByParentContext(getAllMappedSrcs()); // parent context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            if(hasInfos){
//                current_pass = "matchByUse";
//                pass_started = System.currentTimeMillis();
//                matchByUse(getAllMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//
//                current_pass = "matchByDecl";
//                pass_started = System.currentTimeMillis();
//                matchByDecl(getAllMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//            }

//            current_pass = "fixByParentContextPost";
//            pass_started = System.currentTimeMillis();
//            fixByParentContextPost(getAllMappedSrcs()); // parent context
//            pass_finished =  System.currentTimeMillis();
//            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
        }
        else{
            current_pass = "WarmUp";
            warmUp();

            current_pass = "MatchByUnique";
            matchByUnique(getAllMappedSrcs()); // around context only one missing

            current_pass = "FixByChildren";
            fixByChildrenContext(getAllMappedSrcs());

            current_pass = "FixByParent";
            fixByParentContext(getAllMappedSrcs()); // parent context

            current_pass = "FixByInner";
            fixByInnerContext(getAllMappedSrcs()); // fix calls; remove arbitrary mappings

            current_pass = "FixByNearby";
            fixByNearbyContext(getAllMappedSrcs()); // nearby statement; identical statement; nearby condition

//            current_pass = "matchByParentContextPre";
//            matchByParentContextPre(getAllMappedSrcs());

//            if(hasInfos){
//                current_pass = "fixByDecl";
//                fixByDecl(getAllMappedSrcs());
//
//                current_pass = "fixByUse";
//                fixByUse(getAllMappedSrcs());
//            }

            current_pass = "MatchByInner";
            matchByInnerContext(getAllMappedSrcs()); // inner call; this and null in return; conditional expression

            current_pass = "MatchByNearby";
            matchByNearbyContext(getAllMappedSrcs()); // nearby statement;

            current_pass = "MatchByChildren";
            matchByChildrenContext(getAllMappedSrcs()); // descendants context

            current_pass = "MatchByParent";
            matchByParentContext(getAllMappedSrcs()); // parent context

//            if(hasInfos){
//                current_pass = "matchByUse";
//                matchByUse(getAllMappedSrcs());
//
//                current_pass = "matchByDecl";
//                matchByDecl(getAllMappedSrcs());
//            }

//            current_pass = "fixByParentContextPost";
//            fixByParentContextPost(getAllMappedSrcs()); // parent context
        }
    }

    private void warmUp(){
        fixType();

        for(var srcNode:mappings.allMappedSrcs()){
            if(mapInSrc(srcNode))
                curMappedSrcs.add(srcNode);
        }

        for(var t:src.preOrder()){
            if(t.getType().name.equals(Constants.METHOD_DECLARATION) || t.getType().name.equals(Constants.INITIALIZER)){
                prevStmtInTraverse = null;
                initSrcStmtMap(t);
            }
        }

        for(var t:dst.preOrder()){
            if(t.getType().name.equals(Constants.METHOD_DECLARATION) || t.getType().name.equals(Constants.INITIALIZER)){
                prevStmtInTraverse = null;
                initDstStmtMap(t);
            }
        }

        initRenameMap();
    }

    private Set<Tree> getAllMappedSrcs(){
        var s = new TreeSet<>(comparator);
        s.addAll(mappings.allMappedSrcs());
        return s;
    }

    private void initSrcStmtMap(Tree t){
        for (var child : t.getChildren()) {
            if (TreeUtilFunctions.isLeafStatement(child.getType().name)) {
                if(prevStmtInTraverse!=null){
                    srcNextStmtMap.put(prevStmtInTraverse,child);
                    srcPrevStmtMap.put(child,prevStmtInTraverse);
                }
                srcIdenticalStmts.putIfAbsent(child.getMetrics().hash,new ArrayList<>());
                srcIdenticalStmts.get(child.getMetrics().hash).add(child);
                prevStmtInTraverse=child;
            }
            else initSrcStmtMap(child);
        }
    }

    private void initDstStmtMap(Tree t){
        for (var child : t.getChildren()) {
            if (TreeUtilFunctions.isLeafStatement(child.getType().name)) {
                if(prevStmtInTraverse!=null){
                    dstNextStmtMap.put(prevStmtInTraverse,child);
                    dstPrevStmtMap.put(child,prevStmtInTraverse);
                }
                dstIdenticalStmts.putIfAbsent(child.getMetrics().hash,new ArrayList<>());
                dstIdenticalStmts.get(child.getMetrics().hash).add(child);
                prevStmtInTraverse=child;
            }
            else initDstStmtMap(child);
        }
    }

    private void initRenameMap(){
        for(var src:mappings.allMappedSrcs()){
            for(var dst:mappings.getDsts(src)){
                if(src.getType().name.equals(Constants.METHOD_DECLARATION)){
                    var srcName = TreeUtilFunctions.findChildByType(src,Constants.SIMPLE_NAME);
                    var dstName = TreeUtilFunctions.findChildByType(dst,Constants.SIMPLE_NAME);
                    if(srcName!=null && dstName!=null && !srcName.getLabel().equals(dstName.getLabel())){
                        renameMap.put(srcName.getLabel(),dstName.getLabel());
                    }
                }
            }
        }
    }

    private void fixType(){
        for(var mapping:mappings){
            if(!mapping.first.hasSameType(mapping.second)){
                var src = mapping.first;
                var dst = mapping.second;
                removeFromMapping(src,dst);
            }
        }
    }

    @Override
    protected void addToMapping(Tree src,Tree dst){
        mappings.addMapping(src, dst);

        if(isDebug){
            if(newRemovedSrc.contains(src)){
                newRemovedSrc.remove(src);
                if(src.getMetadata("pass")!=null){
                    logger.info("current pass: {}, match src node that prev removed in pass: {}, node pos: {}, node type: {}",current_pass,src.getMetadata("pass"),src.getPos(),src.getType().name);
                    src.setMetadata("pass",current_pass);
                }
            }
            else{
                newMatchedSrc.add(src);
                if(src.getMetadata("pass")!=null){
                    logger.info("current pass: {}, match src node that prev removed in pass: {}, node pos: {}, node type: {}",current_pass,src.getMetadata("pass"),src.getPos(),src.getType().name);
                    src.setMetadata("pass",current_pass);
                }
                else{
                    logger.info("current pass: {}, match src node that prev not matched, node pos: {}, node type: {}",current_pass,src.getPos(),src.getType().name);
                    src.setMetadata("pass",current_pass);
                }
            }

            if(newRemovedDst.contains(dst)){
                newRemovedDst.remove(dst);
                if(dst.getMetadata("pass")!=null){
                    logger.info("current pass: {}, match dst node that prev removed in pass: {}, node pos: {}, node type: {}",current_pass,dst.getMetadata("pass"),dst.getPos(),dst.getType().name);
                    dst.setMetadata("pass",current_pass);
                }
            }
            else{
                newMatchedDst.add(dst);
                if(dst.getMetadata("pass")!=null){
                    logger.info("current pass: {}, match dst node that prev removed in pass: {}, node pos: {}, node type: {}",current_pass,dst.getMetadata("pass"),dst.getPos(),dst.getType().name);
                    dst.setMetadata("pass",current_pass);
                }
                else{
                    logger.info("current pass: {}, match dst node that prev not matched, node pos: {}, node type: {}",current_pass,dst.getPos(),dst.getType().name);
                    dst.setMetadata("pass",current_pass);
                }
            }
        }
    }

    @Override
    protected void removeFromMapping(Tree src,Tree dst){
        mappings.removeMapping(src, dst);

        if(isDebug){
            if(newMatchedSrc.contains(src)){
                newMatchedSrc.remove(src);
                if(src.getMetadata("pass")!=null){
                    logger.info("current pass: {}, remove src node that prev matched in pass: {}, node pos: {}, node type: {}",current_pass,src.getMetadata("pass"),src.getPos(),src.getType().name);
                    src.setMetadata("pass",current_pass);
                }
            }
            else{
                newRemovedSrc.add(src);
                if(src.getMetadata("pass")!=null){
                    logger.info("current pass: {}, remove src node that prev matched in pass: {}, node pos: {}, node type: {}",current_pass,src.getMetadata("pass"),src.getPos(),src.getType().name);
                    src.setMetadata("pass",current_pass);
                }
                else{
                    logger.info("current pass: {}, remove src node that prev matched, node pos: {}, node type: {}",current_pass,src.getPos(),src.getType().name);
                    src.setMetadata("pass",current_pass);
                }
            }

            if(newMatchedDst.contains(dst)){
                newMatchedDst.remove(dst);
                if(dst.getMetadata("pass")!=null){
                    logger.info("current pass: {}, remove dst node that prev matched in pass: {}, node pos: {}, node type: {}",current_pass,dst.getMetadata("pass"),dst.getPos(),dst.getType().name);
                    dst.setMetadata("pass",current_pass);
                }
            }
            else{
                newRemovedDst.add(dst);
                if(dst.getMetadata("pass")!=null){
                    logger.info("current pass: {}, remove dst node that prev matched in pass: {}, node pos: {}, node type: {}",current_pass,dst.getMetadata("pass"),dst.getPos(),dst.getType().name);
                    dst.setMetadata("pass",current_pass);
                }
                else{
                    logger.info("current pass: {}, remove dst node that prev matched, node pos: {}, node type: {}",current_pass,dst.getPos(),dst.getType().name);
                    dst.setMetadata("pass",current_pass);
                }
            }
        }
    }
}