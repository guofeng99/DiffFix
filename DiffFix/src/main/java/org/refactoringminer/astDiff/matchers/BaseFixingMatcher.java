package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;
import com.github.gumtreediff.tree.Type;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.matchers.statement.BasicTreeMatcher;
import org.refactoringminer.astDiff.matchers.statement.LeafMatcher;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/* Created on 2024-10-31 */
public class BaseFixingMatcher implements TreeMatcher{
    protected Tree src;
    protected Tree dst;
    protected ExtendedMultiMappingStore mappings;

    protected ExtendedMultiMappingStore maybeBadMappings;
    protected Set<Tree> keepMappingStmts;
    protected Map<String,String> renameMap;

    protected String current_pass;
    protected Set<Tree> newMatchedSrc;
    protected Set<Tree> newMatchedDst;
    protected Set<Tree> newRemovedSrc;
    protected Set<Tree> newRemovedDst;

    protected Tree prevStmtInTraverse;
    protected Map<Tree,Tree> srcNextStmtMap;
    protected Map<Tree,Tree> srcPrevStmtMap;
    protected Map<Tree,Tree> dstNextStmtMap;
    protected Map<Tree,Tree> dstPrevStmtMap;

    protected Map<Integer,List<Tree>> srcIdenticalStmts;
    protected Map<Integer,List<Tree>> dstIdenticalStmts;

    protected Set<Integer> visitedIdentical;
    protected Set<Tree> visitedIdentical2;

    protected Set<Tree> curMappedSrcs;

    public static boolean isDebug = false;
    public static boolean hasInfos = false;

    protected final static Logger logger = LoggerFactory.getLogger(BaseFixingMatcher.class);

    protected Comparator<Tree> comparator = new Comparator<Tree>() {
        @Override
        public int compare(Tree a, Tree b) {
            if(a.getPos()==b.getPos())
                return a.getMetrics().depth-b.getMetrics().depth;
            return a.getPos()-b.getPos();
        }
    };

    public boolean mapInSrc(Tree srcNode){
        var srcParent = srcNode;
        while(srcParent.getParent()!=null){
            srcParent = srcParent.getParent();
        }
        return srcParent==src;
    }

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
            matchByUnique(getCurMappedSrcs());
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByChildren";
            pass_started = System.currentTimeMillis();
            fixByChildrenContext(getCurMappedSrcs()); // children context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByParent";
            pass_started = System.currentTimeMillis();
            fixByParentContext(getCurMappedSrcs()); // parent context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            current_pass = "fixUniqueAround";
//            pass_started = System.currentTimeMillis();
//            fixUniqueAround(getCurMappedSrcs());
//            pass_finished =  System.currentTimeMillis();
//            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByInner";
            pass_started = System.currentTimeMillis();
            fixByInnerContext(getCurMappedSrcs()); // fix calls; remove arbitrary mappings
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "FixByNearby";
            pass_started = System.currentTimeMillis();
            fixByNearbyContext(getCurMappedSrcs()); // nearby statement; identical statement; nearby condition
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            current_pass = "matchByParentContextPre";
//            pass_started = System.currentTimeMillis();
//            matchByParentContextPre(getCurMappedSrcs());
//            pass_finished =  System.currentTimeMillis();
//            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//
//            if(hasInfos){
//                current_pass = "fixByDecl";
//                pass_started = System.currentTimeMillis();
//                fixByDecl(getCurMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//
//                current_pass = "fixByUse";
//                pass_started = System.currentTimeMillis();
//                fixByUse(getCurMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//            }

            current_pass = "MatchByInner";
            pass_started = System.currentTimeMillis();
            matchByInnerContext(getCurMappedSrcs()); // inner call; this and null in return; conditional expression
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByNearby";
            pass_started = System.currentTimeMillis();
            matchByNearbyContext(getCurMappedSrcs()); // nearby statement;
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByChildren";
            pass_started = System.currentTimeMillis();
            matchByChildrenContext(getCurMappedSrcs()); // descendants context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByParent";
            pass_started = System.currentTimeMillis();
            matchByParentContext(getCurMappedSrcs()); // parent context
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

//            if(hasInfos){
//                current_pass = "matchByUse";
//                pass_started = System.currentTimeMillis();
//                matchByUse(getCurMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//
//                current_pass = "matchByDecl";
//                pass_started = System.currentTimeMillis();
//                matchByDecl(getCurMappedSrcs());
//                pass_finished =  System.currentTimeMillis();
//                logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
//            }

//            current_pass = "fixByParentContextPost";
//            pass_started = System.currentTimeMillis();
//            fixByParentContextPost(getCurMappedSrcs()); // parent context
//            pass_finished =  System.currentTimeMillis();
//            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);
        }
        else{
            current_pass = "WarmUp";
            warmUp();

            current_pass = "MatchByUnique";
            matchByUnique(getCurMappedSrcs()); // around context only one missing

            current_pass = "FixByChildren";
            fixByChildrenContext(getCurMappedSrcs());

            current_pass = "FixByParent";
            fixByParentContext(getCurMappedSrcs()); // parent context

//            current_pass = "fixUniqueAround";
//            fixUniqueAround(getCurMappedSrcs());

            current_pass = "FixByInner";
            fixByInnerContext(getCurMappedSrcs()); // fix calls; remove arbitrary mappings

            current_pass = "FixByNearby";
            fixByNearbyContext(getCurMappedSrcs()); // nearby statement; identical statement; nearby condition

//            current_pass = "matchByParentContextPre";
//            matchByParentContextPre(getCurMappedSrcs());

//            if(hasInfos){
//                current_pass = "fixByDecl";
//                fixByDecl(getCurMappedSrcs());
//
//                current_pass = "fixByUse";
//                fixByUse(getCurMappedSrcs());
//            }

            current_pass = "MatchByInner";
            matchByInnerContext(getCurMappedSrcs()); // inner call; this and null in return; conditional expression

            current_pass = "MatchByNearby";
            matchByNearbyContext(getCurMappedSrcs()); // nearby statement;

            current_pass = "MatchByChildren";
            matchByChildrenContext(getCurMappedSrcs()); // descendants context

            current_pass = "MatchByParent";
            matchByParentContext(getCurMappedSrcs()); // parent context

//            if(hasInfos){
//                current_pass = "matchByUse";
//                matchByUse(getCurMappedSrcs());
//
//                current_pass = "matchByDecl";
//                matchByDecl(getCurMappedSrcs());
//            }

//            current_pass = "fixByParentContextPost";
//            fixByParentContextPost(getCurMappedSrcs()); // parent context
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

    // MatchByParentPre
    protected void matchByUnique(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            if(!mappings.isSrcUnique(src)) continue;
            if(TreeUtilFunctions.isCompositeStatement(src.getType().name)){
                var dst = mappings.getDsts(src).iterator().next();

                matchUniqueAroundInComposite(src,dst);
            }
            else if(src.getType().name.equals(Constants.ANONYMOUS_CLASS_DECLARATION)){
                var dst = mappings.getDsts(src).iterator().next();

                matchUniqueAroundInAnonymousClassDecl(src,dst);
            }
        }

        matchUniqueAroundInWholeFile();

        matchUniqueImport();
    }

    // FixByChildren
    protected void fixByChildrenContext(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            for (var dst : mappings.getDsts(src)) {
//                if(src.getMetrics().hash!=dst.getMetrics().hash){
//                    var flag = true;
//                    switch(src.getType().name){
//                        case Constants.METHOD_INVOCATION -> {
//                            flag = fixArbitraryMethodCallMapping(src,dst);
//                        }
//                        case Constants.CLASS_INSTANCE_CREATION -> {
//                            flag = fixArbitraryClassCreationMapping(src,dst);
//                        }
//                    }
//                    if(!flag) continue;
//                }
                var parentSrc = src.getParent();
                var parentDst = dst.getParent();
                if (parentSrc == null || parentDst == null) continue;
                if (parentSrc.hasSameType(parentDst)) {
                    switch(parentSrc.getType().name){
                        case Constants.INFIX_EXPRESSION -> {
                            if(src.getMetrics().hash==dst.getMetrics().hash){
                                if(mappings.isSrcMapped(parentSrc) ^ mappings.isDstMapped(parentDst)){
                                    fixInfixExpByChildren(parentSrc,parentDst,src,dst);
                                }
                                else if(!mappings.isSrcMapped(parentSrc) && !mappings.isDstMapped(parentDst)){
                                    fixInfixExpByChildren2(parentSrc,parentDst,src,dst);
                                }
                            }
                        }
                        case Constants.CLASS_INSTANCE_CREATION -> {
                            if(parentSrc.getChild(0).getMetrics().hash==parentDst.getChild(0).getMetrics().hash){
                                if(mappings.isSrcMapped(parentSrc) ^ mappings.isDstMapped(parentDst)){
                                    fixClassCreationByChildren(parentSrc,parentDst,src,dst);
                                }
                            }
                        }
                    }
                }
                else{
                    fixDiffTypeByChildren(parentSrc,parentDst,src,dst);
                }
            }
        }
    }

    protected void checkBlockMappingInMethodDecl(Tree src,Tree dst){
        var srcBlock = TreeUtilFunctions.findChildByType(src,Constants.BLOCK);
        var dstBlock = TreeUtilFunctions.findChildByType(dst,Constants.BLOCK);
        if(srcBlock!=null && dstBlock!=null && mappings.isSrcMapped(srcBlock)^mappings.isDstMapped(dstBlock)){
            if(mappings.isSrcMapped(srcBlock) && mappings.isSrcUnique(srcBlock)){
                var mappedDst = mappings.getDsts(srcBlock).iterator().next();
                removeFromMapping(srcBlock,mappedDst);
                addToMapping(srcBlock,dstBlock);
            }
            else if(mappings.isDstMapped(dstBlock) && mappings.isDstUnique(dstBlock)){
                var mappedSrc = mappings.getSrcs(dstBlock).iterator().next();
                removeFromMapping(mappedSrc,dstBlock);
                addToMapping(srcBlock,dstBlock);
            }
        }
    }

    // FixByParent
    protected void fixByParentContext(Set<Tree> mappedSrcs){
        for(var src:mappedSrcs){
            for (var dst : Set.copyOf(mappings.getDsts(src))) {
                switch(src.getType().name){
                    case Constants.METHOD_DECLARATION -> {
                        checkBlockMappingInMethodDecl(src,dst);
                    }
                    case Constants.BLOCK -> {
                        if(src.getMetrics().hash==dst.getMetrics().hash){
                            checkMappingRecursively2(src,dst);
                        }
                        else if(!src.getParent().hasSameType(dst.getParent())){
                            fixArbitraryMatchedBlockByCheckParent(src,dst);
                        }
                    }
                    case Constants.IF_STATEMENT -> {
                        if(src.getMetrics().hash==dst.getMetrics().hash){
                            checkMappingRecursively2(src,dst);
                        }
                        else{
                            var srcCond = src.getChild(0);
                            var dstCond = dst.getChild(0);
                            if(srcCond.getMetrics().hash!=dstCond.getMetrics().hash){
                                if(hasReverseCond(srcCond,dstCond)){
                                    fixStmtByReverseCondIfParent(src,dst);
                                }
                                else{
                                    fixInnerCond(srcCond,dstCond);
                                }
                            }
                            else{
                                var srcBlock = src.getChild(1);
                                var dstBlock = dst.getChild(1);
                                if(srcBlock.hasSameType(dstBlock) && srcBlock.getType().name.equals(Constants.BLOCK)
                                    && !(mappings.isSrcMapped(srcBlock) && mappings.getDsts(srcBlock).contains(dstBlock))){
                                    fixIfBlockByParent(srcBlock,dstBlock);
                                }
                            }
                        }
                    }
                    case Constants.SWITCH_STATEMENT -> {
                        if(src.getMetrics().hash!=dst.getMetrics().hash)
                            fixBreakBySwitchParent(src,dst);
                    }
                    case "ThisExpression" -> {
                        if(!src.getParent().hasSameType(dst.getParent())){
                            fixArbitraryMatchThisByCheckParent(src,dst);
                        }
                    }
                    case Constants.SIMPLE_NAME -> {
                        if(!src.getParent().hasSameType(dst.getParent())){
                            fixArbitraryMatchSimpleNameByCheckParent(src,dst);
                        }
                    }
                }
            }
        }
    }

    protected void fixByParentContextPost(Set<Tree> mappedSrcs){
        for(var src:mappedSrcs) {
            for (var dst : Set.copyOf(mappings.getDsts(src))) {
                if (src.getMetrics().hash == dst.getMetrics().hash)
                    continue;
                switch (src.getType().name) {
                    case Constants.METHOD_INVOCATION_ARGUMENTS -> {
                        var srcCall = src.getParent();
                        var dstCall = dst.getParent();
                        if(mappings.isSrcMapped(srcCall)^mappings.isDstMapped((dstCall))){
                            fixMethodArgsByParent1(srcCall,dstCall,src,dst);
                        }
                        else if(!mappings.isSrcMapped(srcCall) && !mappings.isDstMapped(dstCall)){
                            fixMethodArgsByParent2(src,dst);
                        }
                    }
                    case Constants.METHOD_INVOCATION -> {
                        fixMethodInvocationByParent(src,dst);
                    }
                    case Constants.STRING_LITERAL -> {
                        fixStringLiteralByParent(src,dst);
                    }
                }
            }
        }
    }

    protected void fixUniqueAround(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            for (var dst : Set.copyOf(mappings.getDsts(src))) {
                switch(src.getType().name){
                    case Constants.METHOD_INVOCATION -> {
                        fixUniqueAroundMethodCall(src,dst);
                    }
                    case Constants.RETURN_STATEMENT -> {
                        fixUniqueAroundRetStmt(src,dst);
                    }
                    case Constants.EXPRESSION_STATEMENT -> {
                        fixUniqueAroundExpStmt(src,dst);
                    }
                }
            }
        }
    }

    // FixByInner
    protected void fixByInnerContext(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            for (var dst : Set.copyOf(mappings.getDsts(src))) {
                if(src.getMetrics().hash!=dst.getMetrics().hash){
                    switch(src.getType().name){
                        case Constants.METHOD_INVOCATION -> {
                            fixArbitraryMethodCallMapping(src,dst);
                        }
                        case Constants.CLASS_INSTANCE_CREATION -> {
                            fixArbitraryClassCreationMapping(src,dst);
                        }
                    }
                }
                switch(src.getType().name){
                    case Constants.METHOD_INVOCATION -> {
                        fixUniqueAroundMethodCall(src,dst);
                    }
                    case Constants.RETURN_STATEMENT -> {
                        fixUniqueAroundRetStmt(src,dst);
                    }
                    case Constants.EXPRESSION_STATEMENT -> {
                        fixUniqueAroundExpStmt(src,dst);
                    }
                }
            }
        }
    }

    // FixByNearby
    protected void fixByNearbyContext(Set<Tree> mappedSrcs){
        List<Tree> newStmts = new ArrayList<>();
        for(var src:mappedSrcs){
            if(TreeUtilFunctions.isLeafStatement(src.getType().name))
                newStmts.add(src);
        }
        while(!newStmts.isEmpty()) {
            newStmts = fixByNearbyContextRound(newStmts);
        }
    }

    // MatchByNearby
    protected void matchByNearbyContext(Set<Tree> mappedSrcs){
        List<Tree> newStmts = new ArrayList<>();
        for(var src:mappedSrcs){
            if(TreeUtilFunctions.isLeafStatement(src.getType().name))
                newStmts.add(src);
        }
        while(!newStmts.isEmpty()) {
            newStmts = matchByNearbyContextRound(newStmts);
        }
    }

    // MatchByInner
    protected void matchByInnerContext(Set<Tree> mappedSrcs) {
        for (var src : mappedSrcs) {
            for (var dst : mappings.getDsts(src)) {
                if (src.getMetrics().hash == dst.getMetrics().hash)
                    continue;
                switch(src.getType().name){
                    case Constants.THROW_STATEMENT -> {
                        matchInnerStr(src,dst);
                    }
                    case Constants.METHOD_DECLARATION -> {
                        matchInnerUnique(src,dst);
                        matchInnerReturn(src,dst);
                        matchInnerCond(src,dst);
                    }
                    case Constants.BLOCK -> {
                        matchInnerCall(src,dst);
                    }
                }
            }
        }
    }

    // MatchByChildren
    protected void matchByChildrenContext(Set<Tree> mappedSrcs) {
        List<Tree> newTrees = new ArrayList<>();
        newTrees.addAll(mappedSrcs);
        while(!newTrees.isEmpty()) {
            newTrees = matchByChildrenContextRound(newTrees);
        }
    }

    // MatchByParent
    protected void matchByParentContext(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            for (var dst : mappings.getDsts(src)) {
                switch (src.getType().name){
                    case Constants.METHOD_DECLARATION,Constants.TYPE_DECLARATION -> {
                        matchByParentDecl(src,dst);
                    }
                    case Constants.METHOD_INVOCATION -> {
                        matchByParentMethodCall(src,dst);
                    }
                    case Constants.ASSIGNMENT -> {
                        matchByParentAssign(src,dst);
                    }
                    case Constants.INFIX_EXPRESSION -> {
                        matchByParentInfixExp(src,dst);
                    }
                    case Constants.IF_STATEMENT -> {
                        matchByParentIfStmt(src,dst);
                    }
//                    case Constants.BLOCK -> {
//                        matchByParentBlock(src,dst);
//                    }
                }       
            }
        }
    }

//    // matchByParentContextPre
//    protected void matchByParentContextPre(Set<Tree> mappedSrcs) {
//        for (var src : mappedSrcs) {
//            for (var dst : mappings.getDsts(src)) {
//                switch (src.getType().name) {
//                    case Constants.METHOD_DECLARATION -> {
//                        matchByParentMethodDeclPre(src,dst);
//                    }
//                }
//            }
//        }
//    }

    protected void fixByDecl(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            if (!(src.getType().name.equals(Constants.SIMPLE_NAME) || src.getType().name.equals(Constants.QUALIFIED_NAME))) {
                continue;
            }
            var check=false;
            switch(src.getParent().getType().name){
                case Constants.METHOD_INVOCATION_RECEIVER:
                case Constants.METHOD_INVOCATION_ARGUMENTS:
                case Constants.INFIX_EXPRESSION:
                    check=true;
            }
            if(!check) continue;
            Tree srcDecl = (Tree) src.getMetadata("decl");
            if(srcDecl==null) continue;
            for (var dst : Set.copyOf(mappings.getDsts(src))) {
                if(src.getMetrics().hash==dst.getMetrics().hash)
                    continue;
                if(src.getParent().hasSameType(dst.getParent())){
                    Tree dstDecl = (Tree) dst.getMetadata("decl");
                    if(src.getParent().getChildren().indexOf(src)==dst.getParent().getChildren().indexOf(dst)){

                    }
                    else{
                        if (srcDecl != null && dstDecl != null) {
                            if(!mappings.isSrcMapped(srcDecl) && !mappings.isDstMapped(dstDecl)){
                                Tree srcDeclType = getDeclType(srcDecl);
                                Tree dstDeclType = getDeclType(dstDecl);
                                if (srcDeclType != null && dstDeclType != null) {
                                    if(!compatibleType(srcDeclType,dstDeclType)){
                                        removeFromMapping(src, dst);
                                    }
                                }
                            }
                            else if((mappings.isSrcMapped(srcDecl)^mappings.isDstMapped(dstDecl)) && srcDecl.getMetrics().hash!=dstDecl.getMetrics().hash && src.getParent().getType().name.equals(Constants.INFIX_EXPRESSION)){
                                if(mappings.isSrcMapped(srcDecl) && mappings.isSrcUnique(srcDecl)){
                                    var mappedDst = mappings.getDsts(srcDecl).iterator().next();
                                    if(mappedDst.getMetrics().hash==srcDecl.getMetrics().hash)
                                        removeFromMapping(src,dst);
                                }
                                else if(mappings.isDstMapped(dstDecl) && mappings.isDstUnique(dstDecl)){
                                    var mappedSrc = mappings.getSrcs(dstDecl).iterator().next();
                                    if(mappedSrc.getMetrics().hash==dstDecl.getMetrics().hash)
                                        removeFromMapping(src,dst);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void fixByUse(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            if (!(src.getType().name.equals(Constants.SIMPLE_NAME) || src.getType().name.equals(Constants.QUALIFIED_NAME)))
                continue;
            if(!(src.getParent().getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER)))
                continue;
            for (var dst : mappings.getDsts(src)) {
                if(src.getMetrics().hash==dst.getMetrics().hash)
                    continue;
                if(src.getParent().hasSameType(dst.getParent())){
                    Tree srcDecl = (Tree) src.getMetadata("decl");
                    Tree dstDecl = (Tree) dst.getMetadata("decl");
                    Tree srcOutter = getOutterLeaf(src);
                    Tree dstOutter = getOutterLeaf(dst);
                    if(srcDecl==null || dstDecl==null || srcOutter==null || dstOutter==null) {
                        continue;
                    }
                    Tree srcDeclOutter = getOutterLeaf(srcDecl);
                    Tree dstDeclOutter = getOutterLeaf(dstDecl);
                    if (srcDeclOutter!=null && dstDeclOutter!=null
                            && srcDeclOutter.getParent()==srcOutter.getParent() && dstDeclOutter.getParent()==dstOutter.getParent()
                            && srcDeclOutter.hasSameType(dstDeclOutter)
                            && !(mappings.isSrcMapped(srcDeclOutter) && mappings.getDsts(srcDeclOutter).contains(dstDeclOutter))) {
                        Tree srcDeclType = getDeclType(srcDecl);
                        Tree dstDeclType = getDeclType(dstDecl);
                        if (srcDeclType != null && dstDeclType != null) {
                            if(srcDeclType.getMetrics().hash==dstDeclType.getMetrics().hash){
                                if(mappings.isSrcMapped(srcDecl) && mappings.isSrcUnique(srcDecl)){
                                    var mappedDst = mappings.getDsts(srcDecl).iterator().next();
                                    var mappedDstOutter = getOutterLeaf(mappedDst);
                                    if(mappedDstOutter!=null)
                                        removeMappingSubTree(srcDeclOutter,mappedDstOutter);
                                }
                                if(mappings.isDstMapped(dstDecl) && mappings.isDstUnique(dstDecl)){
                                    var mappedSrc = mappings.getSrcs(dstDecl).iterator().next();
                                    var mappedSrcOutter = getOutterLeaf(mappedSrc);
                                    if(mappedSrcOutter!=null)
                                        removeMappingSubTree(mappedSrcOutter,dstDeclOutter);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void matchByUse(Set<Tree> mappedSrcs){
        for (var src : mappedSrcs) {
            if (!(src.getType().name.equals(Constants.SIMPLE_NAME) || src.getType().name.equals(Constants.QUALIFIED_NAME))) {
                continue;
            }
            if(!(src.getParent().getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER)||src.getParent().getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)))
                continue;
            for (var dst : mappings.getDsts(src)) {
                if(src.getParent().hasSameType(dst.getParent())){
                    Tree srcDecl = (Tree) src.getMetadata("decl");
                    Tree dstDecl = (Tree) dst.getMetadata("decl");
                    if (srcDecl != null && dstDecl != null && !mappings.isSrcMapped(srcDecl) && !mappings.isDstMapped(dstDecl)) {
                        var srcType = getDeclType(srcDecl);
                        var dstType = getDeclType(dstDecl);
                        if(srcType!=null && dstType!=null){
                            if(srcType.getMetrics().hash==dstType.getMetrics().hash
                                    || srcType.hasSameType(dstType) && !srcType.getType().name.equals(Constants.SIMPLE_TYPE)){
                                matchDecl(srcDecl, dstDecl);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void matchByDecl(Set<Tree> mappedSrcs){
        List<Tree> newTrees = new ArrayList<>();
        for(var src: mappedSrcs) {
            for(var dst: mappings.getDsts(src)) {
                if (src.getMetadata("use")!=null && dst.getMetadata("use") != null) {
                    var srcUses = (List<Tree>) src.getMetadata("use");
                    var dstUses = (List<Tree>) dst.getMetadata("use");
                    var srcUnmappedUses = srcUses.stream().filter(t -> !mappings.isSrcMapped(t)).toList();
                    var dstUnmappedUses = dstUses.stream().filter(t -> !mappings.isDstMapped(t)).toList();
                    if (srcUnmappedUses.size() > 0 && srcUnmappedUses.size() < srcUses.size() && dstUnmappedUses.size() > 0 && dstUnmappedUses.size() < dstUses.size()) {
                        for (var srcUnmappedUse : srcUnmappedUses) {
                            var candidates = new ArrayList<Tree>();
                            if (srcUnmappedUse.getParent().getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)) {
                                var srcCall = srcUnmappedUse.getParent().getParent();
                                if (!mappings.isSrcMapped(srcCall) && srcCall.getParent().getType().name.equals(Constants.EXPRESSION_STATEMENT)) {
                                    var srcMethodName = TreeUtilFunctions.findChildByType(srcCall, Constants.SIMPLE_NAME);
                                    for (var dstUmappedUse : dstUnmappedUses) {
                                        if (mappings.isDstMapped(dstUmappedUse))
                                            continue;
                                        if (dstUmappedUse.getParent().getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)) {
                                            var dstCall = dstUmappedUse.getParent().getParent();
                                            if (!mappings.isDstMapped(dstCall) && dstCall.getParent().getType().name.equals(Constants.EXPRESSION_STATEMENT)) {
                                                var dstMethodName = TreeUtilFunctions.findChildByType(dstCall, Constants.SIMPLE_NAME);
                                                if (commonPrefixLen(srcMethodName, dstMethodName) > 3)
                                                    candidates.add(dstUmappedUse);
                                            }
                                        }
                                    }
                                }
                                if (candidates.size() == 1) {
                                    var dstCall = candidates.get(0).getParent().getParent();
                                    matchSubTree(srcCall, dstCall);
                                    newTrees.add(srcCall);
                                }
                            }
                        }
                    }
                }
                else if (src.getMetrics().hash == dst.getMetrics().hash && src.getMetrics().size>1) {
                    var h = src.getMetrics().hash;
                    var srcParent = src.getParent();
                    var dstParent = dst.getParent();
                    if(srcParent==null || dstParent==null || srcParent.hasSameType(dstParent))
                        continue;
                    if (srcParent.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT) && getDeclInit(srcParent) == src) {
                        if (srcParent.getMetadata("use") != null) {
                            var srcUses = (List<Tree>) srcParent.getMetadata("use");
                            for (var srcUse : srcUses) {
                                var srcOutter = getOutterStmt(srcUse);
                                if(srcOutter!=null){
                                    if (mappings.isSrcMapped(srcOutter) && mappings.isSrcUnique(srcOutter)) {
                                        var mappedDst = mappings.getDsts(srcOutter).iterator().next();
                                        for (var t : mappedDst.preOrder()) {
                                            if (!mappings.isDstMapped(t) && t.getMetrics().hash == h) {
                                                addToMappingRecursively(src, t);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (dstParent.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT) && getDeclInit(dstParent) == dst) {
                        if (dstParent.getMetadata("use") != null) {
                            var dstUses = (List<Tree>) dstParent.getMetadata("use");
                            for (var dstUse : dstUses) {
                                var dstOutter = getOutterStmt(dstUse);
                                if(dstOutter!=null){
                                    if (mappings.isDstMapped(dstOutter) && mappings.isDstUnique(dstOutter)) {
                                        var mappedSrc = mappings.getSrcs(dstOutter).iterator().next();
                                        for (var t : mappedSrc.preOrder()) {
                                            if (!mappings.isSrcMapped(t) && t.getMetrics().hash == h) {
                                                addToMappingRecursively(t, dst);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        while(!newTrees.isEmpty()){
            newTrees=matchByChildrenContextRound(newTrees);
        }
    }

    protected Set<Tree> getCurMappedSrcs(){
        return Set.copyOf(curMappedSrcs);
    }

    protected void initDS(Tree src_, Tree dst_, ExtendedMultiMappingStore mappingStore){
        src = src_;
        dst = dst_;

        mappings = mappingStore;

        maybeBadMappings = new ExtendedMultiMappingStore(src, dst);
        keepMappingStmts = new HashSet<>();
        renameMap = new HashMap<>();

        newMatchedSrc = new TreeSet<>(comparator);
        newMatchedDst = new TreeSet<>(comparator);
        newRemovedSrc = new TreeSet<>(comparator);
        newRemovedDst = new TreeSet<>(comparator);

        srcNextStmtMap = new HashMap<>();
        srcPrevStmtMap = new HashMap<>();
        dstNextStmtMap = new HashMap<>();
        dstPrevStmtMap = new HashMap<>();
        srcIdenticalStmts = new HashMap<>();
        dstIdenticalStmts = new HashMap<>();

        visitedIdentical = new HashSet<>();
        visitedIdentical2 = new HashSet<>();

        curMappedSrcs = new TreeSet<>(comparator);
    }

    protected Tree getNextStmtSrc(Tree src){
        return srcNextStmtMap.get(src);
    }

    protected Tree getPrevStmtSrc(Tree src){
        return srcPrevStmtMap.get(src);
    }

    protected Tree getNextStmtDst(Tree dst){
        return dstNextStmtMap.get(dst);
    }

    protected Tree getPrevStmtDst(Tree dst){
        return dstPrevStmtMap.get(dst);
    }


    protected void tryToFixType(Tree src){
        if(!mappings.isSrcMapped(src)) return;
        for(var dst:Set.copyOf(mappings.getDsts(src)))
            if(!src.hasSameType(dst))
                removeFromMapping(src,dst);
        if(mappings.isSrcMapped(src))
            if(mapInSrc(src)) curMappedSrcs.add(src);
    }

    protected void trivalTraverse(Tree src){
        for(var child:src.getChildren()){
            tryToFixType(child);
            trivalTraverse(child);
        }
    }

    protected int calcMappingHash(ExtendedMultiMappingStore mappings){
        int hash = 0;
        for(var mapping: mappings){
            if(mapping.first.getParent()!=null && mapping.second.getParent()!=null)
                hash ^= mapping.first.getMetrics().hash + mapping.second.getMetrics().hash +
                        mapping.first.getParent().getMetrics().hash + mapping.second.getParent().getMetrics().hash;
            else
                hash ^= mapping.first.getMetrics().hash + mapping.second.getMetrics().hash;
        }
        return hash;
    }

    protected void addToMappingRecursively(Tree src,Tree dst){
        addToMapping(src, dst);
        if (dst.getChildren().size() == src.getChildren().size())
            for (int i = 0; i < src.getChildren().size(); i++)
                addToMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    protected void removeFromMappingRecursively(Tree src,Tree dst){
        removeFromMapping(src, dst);
        if (dst.getChildren().size() == src.getChildren().size())
            for (int i = 0; i < src.getChildren().size(); i++)
                removeFromMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    protected void checkMapping(Tree src,Tree dst){
        if(mappings.isSrcMapped(src) && mappings.isDstMapped(dst)){
            if(mappings.getDsts(src).contains(dst)) return;
        }
        if(mappings.isSrcMapped(src))
            for(var mappedDst:Set.copyOf(mappings.getDsts(src)))
                removeFromMapping(src,mappedDst);
        if(mappings.isDstMapped(dst))
            for(var mappedSrc:Set.copyOf(mappings.getSrcs(dst)))
                removeFromMapping(mappedSrc,dst);
        addToMapping(src,dst);
    }

    protected void checkMappingRecursively2(Tree src,Tree dst){
        if(visitedIdentical2.contains(src)) return;
        checkMapping(src,dst);
        visitedIdentical2.add(src);
        if (dst.getChildren().size() == src.getChildren().size())
            for (int i = 0; i < src.getChildren().size(); i++)
                checkMappingRecursively2(src.getChild(i), dst.getChild(i));
    }

    protected void addToMapping(Tree src,Tree dst){
        mappings.addMapping(src, dst);
        curMappedSrcs.add(src);

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

    protected void removeFromMapping(Tree src,Tree dst){
        mappings.removeMapping(src, dst);
        curMappedSrcs.remove(src);

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

    protected List<Pair<Tree,Tree>> getLCSLeaves(List<Tree> srcLeaves,List<Tree> dstLeaves){
        int[][] dp = new int[srcLeaves.size()+1][dstLeaves.size()+1];
        int[] srcHash = new int[srcLeaves.size()];
        int[] dstHash = new int[dstLeaves.size()];
        for(int i=0;i<srcLeaves.size();i++){
            srcHash[srcLeaves.size()-1-i] = srcLeaves.get(i).getMetrics().hash;
        }
        for(int i=0;i<dstLeaves.size();i++){
            dstHash[dstLeaves.size()-1-i] = dstLeaves.get(i).getMetrics().hash;
        }
        for(int i=0;i<srcLeaves.size();i++){
            for(int j=0;j<dstLeaves.size();j++){
                if(srcHash[i]==dstHash[j]){
                    dp[i+1][j+1] = dp[i][j]+1;
                }
                else{
                    dp[i+1][j+1] = Math.max(dp[i][j+1],dp[i+1][j]);
                }
            }
        }
        List<Pair<Tree,Tree>> pairs = new ArrayList<>();
        int i = srcLeaves.size();
        int j = dstLeaves.size();
        while(i>0 && j>0){
            if(srcHash[i-1]==dstHash[j-1]){
                pairs.add(new Pair<>(srcLeaves.get(srcLeaves.size()-i),dstLeaves.get(dstLeaves.size()-j)));
                i--;
                j--;
            }
            else if(dp[i-1][j]>dp[i][j-1]){
                i--;
            }
            else{
                j--;
            }
        }
        return pairs;
    }

    protected Pair<List<Pair<Tree,Tree>>,Boolean> getSameLeaves(Tree srcSt, Tree dstSt){
        List<Tree> srcLeaves = TreeUtils.preOrder(srcSt).stream().filter(Tree::isLeaf).toList();
        List<Tree> dstLeaves = TreeUtils.preOrder(dstSt).stream().filter(Tree::isLeaf).toList();
        List<Pair<Tree,Tree>> pairs = getLCSLeaves(srcLeaves,dstLeaves);
        Set<Tree> pairSrcLeaves = new HashSet<>();
        Set<Tree> pairDstLeaves = new HashSet<>();
        for(var pair: pairs){
            pairSrcLeaves.add(pair.first);
            pairDstLeaves.add(pair.second);
        }
        List<Tree> unpairedSrcLeaves = srcLeaves.stream().filter(leaf -> !pairSrcLeaves.contains(leaf)).toList();
        List<Tree> unpairedDstLeaves = dstLeaves.stream().filter(leaf -> !pairDstLeaves.contains(leaf)).toList();

        Map<Integer,List<Tree>> uniqueSrcLeaves = new HashMap<>();
        Map<Integer,List<Tree>> uniqueDstLeaves = new HashMap<>();

        for(var leaf: unpairedSrcLeaves){
            uniqueSrcLeaves.putIfAbsent(leaf.getMetrics().hash,new ArrayList<>());
            uniqueSrcLeaves.get(leaf.getMetrics().hash).add(leaf);
        }

        for(var leaf: unpairedDstLeaves){
            uniqueDstLeaves.putIfAbsent(leaf.getMetrics().hash,new ArrayList<>());
            uniqueDstLeaves.get(leaf.getMetrics().hash).add(leaf);
        }

        boolean hasSameLeaves = false;

        for(var hash: uniqueSrcLeaves.keySet()){
            if(uniqueDstLeaves.containsKey(hash)){
                hasSameLeaves = true;
                var srcSameLeaves = uniqueSrcLeaves.get(hash);
                var dstSameLeaves = uniqueDstLeaves.get(hash);
                if(srcSameLeaves.size()==1 && dstSameLeaves.size()==1){
                    var srcLeaf = srcSameLeaves.get(0);
                    var dstLeaf = dstSameLeaves.get(0);
                    pairs.add(new Pair<>(srcLeaf,dstLeaf));
                    pairSrcLeaves.add(srcLeaf);
                    pairDstLeaves.add(dstLeaf);
                }
            }
        }

        unpairedSrcLeaves = srcLeaves.stream().filter(leaf -> !pairSrcLeaves.contains(leaf)).toList();
        unpairedDstLeaves = dstLeaves.stream().filter(leaf -> !pairDstLeaves.contains(leaf)).toList();

        for(var srcLeaf: unpairedSrcLeaves){
            for(var dstLeaf: unpairedDstLeaves){
                Tree srcVarDecl = (Tree)srcLeaf.getMetadata("decl");
                Tree dstVarDecl = (Tree)dstLeaf.getMetadata("decl");
                if(srcVarDecl!=null && dstVarDecl!=null && mappings.hasSrc(srcVarDecl) && mappings.getDsts(srcVarDecl).contains(dstVarDecl)){
                    Tree parentSrc = srcLeaf.getParent();
                    Tree parentDst = dstLeaf.getParent();
                    var b1 = parentSrc.getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER);
                    var b2 = parentDst.getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER);
                    var b3 = parentSrc.getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS);
                    var b4 = parentDst.getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS);
                    if(b1==b2 || b1&&b4 || b2&&b3)
                        pairs.add(new Pair<>(srcLeaf,dstLeaf));
                }
            }
        }

        return new Pair<>(pairs,hasSameLeaves || !pairs.isEmpty());
    }

    protected Boolean compareLeafMapping(Pair<List<Pair<Tree,Tree>>,Boolean> result1, Pair<List<Pair<Tree,Tree>>,Boolean> result2){
        if(result1.second && !result2.second) return true;
        if(result1.first.size()>result2.first.size()) return true;
        return false;
    }

    protected void removeMappingSubTree(Tree src, Tree dst){
        Set<Tree> mappedSrcSubTrees = new HashSet<>();
        for (Tree tree : src.preOrder()) {
            if (mappings.isSrcMapped(tree))
                mappedSrcSubTrees.add(tree);
        }
        Set<Tree> mappedDstSubTrees = new HashSet<>();
        for (Tree tree : dst.preOrder()) {
            if (mappings.isDstMapped(tree))
                mappedDstSubTrees.add(tree);
        }
        for(var mappedSrcSubTree: mappedSrcSubTrees){
            for(var mappedDstSubTree: mappings.getDsts(mappedSrcSubTree)){
                if(mappedDstSubTrees.contains(mappedDstSubTree)){
                    removeFromMapping(mappedSrcSubTree,mappedDstSubTree);
                    break;
                }
            }
        }

        checkParentAfterRemove(src,dst);
    }

    protected boolean withoutInnerMapping(Tree src, Tree dst) {
        Set<Tree> mappedSrcSubTrees = new HashSet<>();
        for (Tree tree : src.getDescendants()) {
            if (mappings.isSrcMapped(tree))
                mappedSrcSubTrees.add(tree);
        }
        Set<Tree> mappedDstSubTrees = new HashSet<>();
        for (Tree tree : dst.getDescendants()) {
            if (mappings.isDstMapped(tree))
                mappedDstSubTrees.add(tree);
        }
        for(var mappedSrcSubTree: mappedSrcSubTrees){
            for(var mappedDstSubTree: mappings.getDsts(mappedSrcSubTree)){
                if(mappedDstSubTrees.contains(mappedDstSubTree))
                    return false;
            }
        }
        return true;
    }

    protected void checkParentAfterRemove(Tree src, Tree dst){
        var srcParent = src.getParent();
        var dstParent = dst.getParent();
        if(srcParent!=null && dstParent!=null && mappings.isSrcMapped(srcParent) && mappings.getDsts(srcParent).contains(dstParent)){
            if(withoutInnerMapping(srcParent,dstParent)){
                removeFromMapping(srcParent,dstParent);
                checkParentAfterRemove(srcParent,dstParent);
            }
        }
    }

    protected void matchSubTree(Tree srcSt, Tree dstSt) {
        var mappings2 = new ExtendedMultiMappingStore(srcSt, dstSt);
        new LeafMatcher().match(srcSt, dstSt, mappings2);

        var mappings2rev = new ExtendedMultiMappingStore(dstSt, srcSt);
        new LeafMatcher().match(dstSt, srcSt, mappings2rev);

        var mappings2rev2 = new ExtendedMultiMappingStore(srcSt, dstSt);
        for(var mapping: mappings2rev){
            mappings2rev2.addMapping(mapping.second,mapping.first);
        }
        if(mappings2rev2.size()>mappings2.size())
            mappings2 = mappings2rev2;
        else if(calcMappingHash(mappings2)!=calcMappingHash(mappings2rev2)){
            var commonMappings = new ExtendedMultiMappingStore(srcSt,dstSt);
            for(var mapping: mappings2){
                for(var mapping2: mappings2rev2){
                    if(mapping.first==mapping2.first && mapping.second==mapping2.second){
                        commonMappings.addMapping(mapping.first,mapping.second);
                        break;
                    }
                }
            }
            mappings2 = commonMappings;
//            var es1 = new ExtendedChawatheScriptGenerator().computeActions(mappings2,projectASTDiff.getParentContextMap(),projectASTDiff.getChildContextMap());
//            var es2 = new ExtendedChawatheScriptGenerator().computeActions(mappings2rev2,projectASTDiff.getParentContextMap(),projectASTDiff.getChildContextMap());
//            if(es2.size()<es1.size()){
//                mappings2 = mappings2rev2;
//            }
        }

        var mappings3 = new ExtendedMultiMappingStore(srcSt, dstSt);

        for (Mapping mapping : mappings2) {
            if(mappings.isSrcMapped(mapping.first) || mappings.isDstMapped(mapping.second)){
                continue;
            }
            mappings3.addMapping(mapping.first,mapping.second);
        }

        for (Mapping mapping: mappings3) {
            var srcNode = mapping.first;
            var dstNode = mapping.second;
            if (srcNode.isLeaf() && dstNode.isLeaf()) {
                addToMapping(srcNode, dstNode);
            }
            else {
                var srcCnt = srcNode.getDescendants().stream().filter(d->d.isLeaf() && mappings3.isSrcMapped(d)).count();
                var dstCnt = dstNode.getDescendants().stream().filter(d->d.isLeaf() && mappings3.isDstMapped(d)).count();
                if(srcCnt>0 && dstCnt>0 && (srcCnt==dstCnt || !srcNode.getType().name.equals(Constants.METHOD_INVOCATION))){
                    addToMapping(srcNode,dstNode);
                }
            }
        }

        mapMissingLeaves(srcSt,dstSt);
        mapMissingInnerNodes(srcSt,dstSt);
        fixInnerMapping(srcSt,dstSt);
        fixInnerCalls(srcSt,dstSt);
    }

    protected void mapMissingLeaves(Tree srcSt, Tree dstSt){
//        List<Tree> srcMissingLeaves = TreeUtils.preOrder(srcSt).stream().filter(t -> t.isLeaf() && !mappings.isSrcMapped(t)).toList();
//        List<Tree> dstMissingLeaves = TreeUtils.preOrder(dstSt).stream().filter(t -> t.isLeaf() && !mappings.isDstMapped(t)).toList();
//        for(var srcLeaf: srcMissingLeaves){
//            for(var dstLeaf: dstMissingLeaves){
//                if(mappings.isDstMapped(dstLeaf)) continue;
////                if(srcLeaf.getMetrics().hash==dstLeaf.getMetrics().hash){
////                    addToMapping(srcLeaf,dstLeaf);
////                    break;
////                }
//                Tree srcVarDecl = (Tree)srcLeaf.getMetadata("decl");
//                Tree dstVarDecl = (Tree)dstLeaf.getMetadata("decl");
//                if(srcVarDecl!=null && dstVarDecl!=null && mappings.hasSrc(srcVarDecl) && mappings.getDsts(srcVarDecl).contains(dstVarDecl)){
//                    addToMapping(srcLeaf,dstLeaf);
//                    break;
//                }
//            }
//        }

        var result = getSameLeaves(srcSt,dstSt);
        for(var pair: result.first){
            if(!(mappings.isSrcMapped(pair.first) || mappings.isDstMapped(pair.second)) && pair.first.getParent().hasSameType(pair.second.getParent())){
                addToMapping(pair.first, pair.second);
            }
        }
    }

    protected void mapMissingInnerNodes(Tree srcSt, Tree dstSt){
        List<Tree> srcNodes = TreeUtils.preOrder(srcSt);
        List<Tree> dstNodes = TreeUtils.preOrder(dstSt);

        List<Tree> srcMissingInnerNodes = srcNodes.stream().filter(t -> !t.isLeaf() && !mappings.isSrcMapped(t)).toList();
        List<Tree> dstMissingInnerNodes = dstNodes.stream().filter(t -> !t.isLeaf() && !mappings.isDstMapped(t)).toList();

        ExtendedMultiMappingStore mappings2 = new ExtendedMultiMappingStore(srcSt,dstSt);

        for(var srcNode:srcNodes){
            if(!srcNode.isLeaf()) continue;
            for(var dstNode:dstNodes){
                if(!dstNode.isLeaf()) continue;
                if(mappings.isSrcMapped(srcNode) && mappings.getDsts(srcNode).contains(dstNode)){
                    mappings2.addMapping(srcNode,dstNode);
                }
            }
        }

        for(var srcMissingInnerNode: srcMissingInnerNodes){
            for(var dstMissingInnerNode: dstMissingInnerNodes){
                if(mappings.isDstMapped(dstMissingInnerNode)) continue;
                if(srcMissingInnerNode.hasSameType(dstMissingInnerNode)){
                    Set<Tree> srcLeaves = new HashSet<>(srcMissingInnerNode.getDescendants().stream().filter(Tree::isLeaf).toList());
                    Set<Tree> dstLeaves = new HashSet<>(dstMissingInnerNode.getDescendants().stream().filter(Tree::isLeaf).toList());
                    var mappedLeavesCnt = 0;
                    for(var mapping:mappings2){
                        if(srcLeaves.contains(mapping.first) && dstLeaves.contains(mapping.second)){
                            mappedLeavesCnt++;
                        }
                    }
                    if(mappedLeavesCnt>0) {
                        if(srcMissingInnerNode.getType().name.equals(Constants.METHOD_INVOCATION) && mappedLeavesCnt==1)
                            continue;
                        addToMapping(srcMissingInnerNode, dstMissingInnerNode);
                        break;
                    }
                }
            }
        }
    }

    protected void fixInnerMapping(Tree srcSt, Tree dstSt){
        List<Tree> srcNodes = TreeUtils.preOrder(srcSt);
        List<Tree> dstNodes = TreeUtils.preOrder(dstSt);

        List<Tree> srcInnerNodes = srcNodes.stream().filter(node -> !node.isLeaf()).toList();
        List<Tree> dstInnerNodes = dstNodes.stream().filter(node -> !node.isLeaf()).toList();

        ExtendedMultiMappingStore mappings2 = new ExtendedMultiMappingStore(srcSt,dstSt);

        for(var srcNode:srcNodes){
            for(var dstNode:dstNodes){
                if(mappings.isSrcMapped(srcNode) && mappings.getDsts(srcNode).contains(dstNode)){
                    mappings2.addMapping(srcNode,dstNode);
                }
            }
        }

        Map<Tree,Integer> mappedLeavesCnt = new HashMap<>();

        for(var mapping: mappings2){
            if(mapping.first.isLeaf() && mapping.second.isLeaf()){
                var pa = mapping.first;
                while(pa!=srcSt){
                    pa = pa.getParent();
                    mappedLeavesCnt.put(pa,mappedLeavesCnt.getOrDefault(pa,0)+1);
                };
                pa = mapping.second;
                while(pa!=dstSt){
                    pa=pa.getParent();
                    mappedLeavesCnt.put(pa,mappedLeavesCnt.getOrDefault(pa,0)+1);
                };
            }
        }

        Map<Type,List<Tree>> srcInnerNodesMap = new HashMap<>();
        Map<Type,List<Tree>> dstInnerNodesMap = new HashMap<>();

        for(var srcInnerNode:srcInnerNodes){
            if(!srcInnerNodesMap.containsKey(srcInnerNode.getType())){
                srcInnerNodesMap.put(srcInnerNode.getType(),new ArrayList<>());
            }
            srcInnerNodesMap.get(srcInnerNode.getType()).add(srcInnerNode);
        }

        for(var dstInnerNode:dstInnerNodes){
            if(!dstInnerNodesMap.containsKey(dstInnerNode.getType())){
                dstInnerNodesMap.put(dstInnerNode.getType(),new ArrayList<>());
            }
            dstInnerNodesMap.get(dstInnerNode.getType()).add(dstInnerNode);

        }

        for(var type: srcInnerNodesMap.keySet()){
            if(dstInnerNodesMap.containsKey(type)){
                List<Tree> srcInnerNodesOfType = srcInnerNodesMap.get(type);
                List<Tree> dstInnerNodesOfType = dstInnerNodesMap.get(type);
                if(srcInnerNodesOfType.size()>1){
                    Map<Tree,List<Tree>> srcInner = new HashMap<>();
                    Collections.sort(srcInnerNodesOfType,Comparator.comparingInt(t -> t.getMetrics().height));
                    for(var srcInnerNode: srcInnerNodesOfType){
                        var pa=srcInnerNode;
                        while(pa!=srcSt){
                            pa=pa.getParent();
                            if(srcInnerNodesOfType.contains(pa)){
                                if(!srcInner.containsKey(pa)){
                                    srcInner.put(pa,new ArrayList<>());
                                }
                                srcInner.get(pa).add(srcInnerNode);
                                break;
                            }
                        }
                    }
                    for(var srcInnerNode: srcInnerNodesOfType){
                        for(var mapping: mappings2){
                            if(mapping.first==srcInnerNode){
                                var dstInnerNode = mapping.second;
                                var sameLeavesInner = srcInnerNode;
                                boolean isFound = true;
                                while(srcInner.containsKey(sameLeavesInner) && isFound){
                                    isFound = false;
                                    for(var inner: srcInner.get(sameLeavesInner)){
                                        if(mappedLeavesCnt.getOrDefault(inner,0)==mappedLeavesCnt.getOrDefault(sameLeavesInner,0) && !mappings.isSrcMapped(inner)){
                                            sameLeavesInner = inner;
                                            isFound = true;
                                        }
                                    }
                                }
                                if(sameLeavesInner!=srcInnerNode){
                                    removeFromMapping(srcInnerNode,dstInnerNode);
                                    addToMapping(sameLeavesInner,dstInnerNode);
                                    mappings2.removeMapping(srcInnerNode,dstInnerNode);
                                    mappings2.addMapping(sameLeavesInner,dstInnerNode);
                                }
                                break;
                            }
                        }
                    }
                }
                if(dstInnerNodesOfType.size()>1){
                    Map<Tree,List<Tree>> dstInner = new HashMap<>();
                    Collections.sort(dstInnerNodesOfType,Comparator.comparingInt(t -> t.getMetrics().height));
                    for(var dstInnerNode: dstInnerNodesOfType){
                        var pa=dstInnerNode;
                        while(pa!=dstSt){
                            pa=pa.getParent();
                            if(dstInnerNodesOfType.contains(pa)){
                                if(!dstInner.containsKey(pa)){
                                    dstInner.put(pa,new ArrayList<>());
                                }
                                dstInner.get(pa).add(dstInnerNode);
                                break;
                            }
                        }
                    }
                    for(var dstInnerNode: dstInnerNodesOfType){
                        for(var mapping: mappings2){
                            if(mapping.second==dstInnerNode){
                                var srcInnerNode = mapping.first;
                                var sameLeavesInner = dstInnerNode;
                                boolean isFound = true;
                                while(dstInner.containsKey(sameLeavesInner) && isFound){
                                    isFound = false;
                                    for(var inner: dstInner.get(sameLeavesInner)){
                                        if(mappedLeavesCnt.getOrDefault(inner,0)==mappedLeavesCnt.getOrDefault(sameLeavesInner,0) && !mappings.isDstMapped(inner)){
                                            sameLeavesInner = inner;
                                            isFound = true;
                                        }
                                    }
                                }
                                if(sameLeavesInner!=dstInnerNode){
                                    removeFromMapping(srcInnerNode,dstInnerNode);
                                    addToMapping(srcInnerNode,sameLeavesInner);
                                    mappings2.removeMapping(srcInnerNode,dstInnerNode);
                                    mappings2.addMapping(srcInnerNode,sameLeavesInner);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    protected void fixInnerCalls(Tree srcSt,Tree dstSt){
        var srcCalls = srcSt.getDescendants().stream().filter(t -> t.getType().name.equals(Constants.METHOD_INVOCATION)).toList();
        var dstCalls = dstSt.getDescendants().stream().filter(t -> t.getType().name.equals(Constants.METHOD_INVOCATION)).toList();
        for(var srcCall:srcCalls){
            for(var dstCall:dstCalls){
                if(mappings.isSrcMapped(srcCall) && mappings.getDsts(srcCall).contains(dstCall)){
                    var res=getSameLeaves(srcCall,dstCall);
                    if(!res.second){
                        removeMappingSubTree(srcCall,dstCall);
                    }
                    else{
                        var srcReceiver = TreeUtilFunctions.findChildByType(srcCall,Constants.METHOD_INVOCATION_RECEIVER);
                        var dstReceiver = TreeUtilFunctions.findChildByType(dstCall,Constants.METHOD_INVOCATION_RECEIVER);
                        if(srcReceiver!=null && dstReceiver!=null && !mappings.isSrcMapped(srcReceiver) && !mappings.isDstMapped(dstReceiver)){
                            addToMapping(srcReceiver,dstReceiver);
                        }
                        var srcArgs = TreeUtilFunctions.findChildByType(srcCall,Constants.METHOD_INVOCATION_ARGUMENTS);
                        var dstArgs = TreeUtilFunctions.findChildByType(dstCall,Constants.METHOD_INVOCATION_ARGUMENTS);
                        if(srcArgs!=null && dstArgs!=null && !mappings.isSrcMapped(srcArgs) && !mappings.isDstMapped(dstArgs)){
                            addToMapping(srcArgs,dstArgs);
                        }
                    }
                }
            }
        }
    }

    protected List<Tree> getLeafStmts(Tree t){
        List<Tree> leafStmts = new ArrayList<>();
        for (var child : t.getChildren()) {
            if (TreeUtilFunctions.isLeafStatement(child.getType().name))
                leafStmts.add(child);
            else
                leafStmts.addAll(getLeafStmts(child));
        }
        return leafStmts;
    }

    protected List<Tree> getSrcUnmappedChildren(Tree src){
        return src.getChildren().stream().filter(child -> !mappings.isSrcMapped(child)).toList();
    }

    protected List<Tree> getDstUnmappedChildren(Tree dst){
        return dst.getChildren().stream().filter(child -> !mappings.isDstMapped(child)).toList();
    }

    protected long getSrcUnmappedDestRetStmtCount(Tree src){
        return getLeafStmts(src).stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.RETURN_STATEMENT)).count();
    }

    protected long getDstUnmappedDestRetStmtCount(Tree dst){
        return getLeafStmts(dst).stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.RETURN_STATEMENT)).count();
    }

    protected long getSrcUnmappedDestThrowStmtCount(Tree src){
        return getLeafStmts(src).stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.THROW_STATEMENT)).count();
    }

    protected long getDstUnmappedDestThrowStmtCount(Tree dst){
        return getLeafStmts(dst).stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.THROW_STATEMENT)).count();
    }

    protected void matchUniqueAroundInComposite(Tree src,Tree dst){
        var srcUnmappedChildren = getSrcUnmappedChildren(src);
        if(srcUnmappedChildren.isEmpty()) return;

        var dstUnmappedChildren = getDstUnmappedChildren(dst);
        if(dstUnmappedChildren.isEmpty()) return;

        var srcChildTypeMap = new HashMap<String,List<Tree>>();
        var dstChildTypeMap = new HashMap<String,List<Tree>>();
        for(var child: srcUnmappedChildren){
            srcChildTypeMap.putIfAbsent(child.getType().name,new ArrayList<>());
            srcChildTypeMap.get(child.getType().name).add(child);
        }
        for(var child: dstUnmappedChildren){
            dstChildTypeMap.putIfAbsent(child.getType().name,new ArrayList<>());
            dstChildTypeMap.get(child.getType().name).add(child);
        }
        var uniqueTypePairs = new ArrayList<Pair<Tree,Tree>>();
        for(var type: srcChildTypeMap.keySet()){
            if(dstChildTypeMap.containsKey(type)){
                var srcChildren = srcChildTypeMap.get(type);
                var dstChildren = dstChildTypeMap.get(type);
                if(srcChildren.size()==1 && dstChildren.size()==1){
                    uniqueTypePairs.add(new Pair<>(srcChildren.get(0),dstChildren.get(0)));
                }
            }
        }
        for(var pair: uniqueTypePairs){
            var srcSt= pair.first;
            var dstSt = pair.second;
            switch (srcSt.getType().name){
                case Constants.EXPRESSION_STATEMENT -> {
                    if(srcSt.getChild(0).getType()!=dstSt.getChild(0).getType()){
                        if(src.getChildren().size()==dst.getChildren().size()
                                && src.getChildren().indexOf(srcSt)==src.getChildren().size()-1
                                && dst.getChildren().indexOf(dstSt)==dst.getChildren().size()-1){
                            var b1 = srcSt.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION);
                            var b2 = dstSt.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION);
                            var b3 = srcSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT) && srcSt.getChild(0).getChild(2).getType().name.equals(Constants.BOOLEAN_LITERAL);
                            var b4 = dstSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT) && dstSt.getChild(0).getChild(2).getType().name.equals(Constants.BOOLEAN_LITERAL);
                            if(b1&&b4 || b2&&b3)
                                addToMapping(srcSt,dstSt);
                        }
                    }
                    else if(srcSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT)){
                        var result = getSameLeaves(srcSt, dstSt);
                        if(result.first.size()>1){
                            matchSubTree(srcSt,dstSt);
                        }
                    }
                    else{
                        var result = getSameLeaves(srcSt,dstSt);
                        if(result.second){
                            matchSubTree(srcSt, dstSt);
                        }
                    }
                }
                case Constants.VARIABLE_DECLARATION_STATEMENT -> {
                    var srcVD = TreeUtilFunctions.findChildByType(srcSt,Constants.VARIABLE_DECLARATION_FRAGMENT);
                    var dstVD = TreeUtilFunctions.findChildByType(dstSt,Constants.VARIABLE_DECLARATION_FRAGMENT);
                    if(srcVD!=null && dstVD!=null && srcVD.getChild(0).getMetrics().hash==dstVD.getChild(0).getMetrics().hash){
                        var srcType = getDeclType(srcVD);
                        var dstType = getDeclType(dstVD);
                        if(srcType!=null && dstType!=null){
                            if(srcType.getMetrics().hash==dstType.getMetrics().hash
                                    || srcType.hasSameType(dstType) && !srcType.getType().name.equals(Constants.SIMPLE_TYPE)){
                                matchSubTree(srcSt,dstSt);
                            }
                        }
                    }
                }
                case Constants.RETURN_STATEMENT -> {
                    var srcUnmappedDestRetStmtCount = getSrcUnmappedDestRetStmtCount(src);
                    var dstUnmappedDestRetStmtCount = getDstUnmappedDestRetStmtCount(dst);
                    if (srcUnmappedDestRetStmtCount==dstUnmappedDestRetStmtCount) {
                        if(srcUnmappedChildren.size()==1 && dstUnmappedChildren.size()==1 || srcUnmappedChildren.size()!=src.getChildren().size() && dstUnmappedChildren.size()!=dst.getChildren().size()){
                            addToMapping(srcSt, dstSt);
                            var result = getSameLeaves(srcSt,dstSt);
                            if(result.second){
                                matchSubTree(srcSt, dstSt);
                            }
                        }
                    }
                }
                case Constants.THROW_STATEMENT -> {
                    var srcUnmappedDestThrowStmtCount = getSrcUnmappedDestThrowStmtCount(src);
                    var dstUnmappedDestThrowStmtCount = getDstUnmappedDestThrowStmtCount(dst);
                    if (srcUnmappedDestThrowStmtCount==dstUnmappedDestThrowStmtCount) {
                        addToMapping(srcSt, dstSt);
                        var result = getSameLeaves(srcSt,dstSt);
                        if(result.second){
                            matchSubTree(srcSt, dstSt);
                        }
                    }
                }
                case Constants.IF_STATEMENT -> {
                    tryToMatchIf(srcSt,dstSt);
                }
            }
        }
    }

    protected List<Tree> getSrcUnmappedMethodDecls(Tree src){
        return src.getChildren().stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.METHOD_DECLARATION)).toList();
    }

    protected List<Tree> getDstUnmappedMethodDecls(Tree dst){
        return dst.getChildren().stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.METHOD_DECLARATION)).toList();
    }

    protected void matchUniqueAroundInAnonymousClassDecl(Tree src,Tree dst){
        var srcUnmappedMethodDecls = getSrcUnmappedMethodDecls(src);
        if(srcUnmappedMethodDecls.size()!=1) return;

        var dstUnmappedMethodDecls = getDstUnmappedMethodDecls(dst);
        if(dstUnmappedMethodDecls.size()!=1) return;

        var srcSt = srcUnmappedMethodDecls.get(0);
        var dstSt = dstUnmappedMethodDecls.get(0);
        var srcName = TreeUtilFunctions.findChildByType(srcSt,Constants.SIMPLE_NAME);
        var dstName = TreeUtilFunctions.findChildByType(dstSt,Constants.SIMPLE_NAME);
        if(srcName!=null && dstName!=null && srcName.getMetrics().hash==dstName.getMetrics().hash
                && !mappings.isSrcMappedConsideringSubTrees(srcSt) && !mappings.isDstMappedConsideringSubTrees(dstSt)){
            BasicTreeMatcher basicTreeMatcher = new BasicTreeMatcher();
            ExtendedMultiMappingStore mappings2 = new ExtendedMultiMappingStore(srcSt,dstSt);
            basicTreeMatcher.match(srcSt,dstSt,mappings2);
            for(var mapping: mappings2){
                addToMapping(mapping.first,mapping.second);
            }
        }
    }

    protected List<Tree> getSrcUnmappedDestStmts(){
        return src.getDescendants().stream().filter(node -> !mappings.isSrcMapped(node) && TreeUtilFunctions.isStatement(node.getType().name)).toList();
    }

    protected List<Tree> getDstUnmappedDestStmts(){
        return dst.getDescendants().stream().filter(node -> !mappings.isDstMapped(node) && TreeUtilFunctions.isStatement(node.getType().name)).toList();
    }

    protected void matchUniqueAroundInWholeFile(){
        List<Tree> srcUnmappedStmts = getSrcUnmappedDestStmts();
        if(srcUnmappedStmts.isEmpty()) return;
        List<Tree> dstUnmappedStmts = getDstUnmappedDestStmts();
        if(dstUnmappedStmts.isEmpty()) return;

        var srcStmtTypeMap = new HashMap<String,List<Tree>>();
        var dstStmtTypeMap = new HashMap<String,List<Tree>>();
        for(var child: srcUnmappedStmts){
            srcStmtTypeMap.putIfAbsent(child.getType().name,new ArrayList<>());
            srcStmtTypeMap.get(child.getType().name).add(child);
        }
        for(var child: dstUnmappedStmts){
            dstStmtTypeMap.putIfAbsent(child.getType().name,new ArrayList<>());
            dstStmtTypeMap.get(child.getType().name).add(child);
        }
        var uniqueTypePairs = new ArrayList<Pair<Tree,Tree>>();
        for(var type: srcStmtTypeMap.keySet()){
            if(dstStmtTypeMap.containsKey(type)){
                var srcChildren = srcStmtTypeMap.get(type);
                var dstChildren = dstStmtTypeMap.get(type);
                if(srcChildren.size()==1 && dstChildren.size()==1){
                    uniqueTypePairs.add(new Pair<>(srcChildren.get(0),dstChildren.get(0)));
                }
            }
        }

        for(var pair:uniqueTypePairs){
            var srcSt = pair.first;
            var dstSt = pair.second;
            switch (srcSt.getType().name){
                case Constants.IF_STATEMENT:{
                    tryToMatchIf(srcSt,dstSt);
                    break;
                }
                case Constants.THROW_STATEMENT:{
                    if(hasInnerMapping(srcSt,dstSt)){
                        matchSubTree(srcSt,dstSt);
                    }
                    break;
                }
                case Constants.ENHANCED_FOR_STATEMENT:{
                    if(srcSt.getChild(0).getMetrics().hash==dstSt.getChild(0).getMetrics().hash
                            && srcSt.getChild(1).getMetrics().hash==dstSt.getChild(1).getMetrics().hash){
                        addToMapping(srcSt,dstSt);
                        matchSubTree(srcSt.getChild(0),dstSt.getChild(0));
                        matchSubTree(srcSt.getChild(1),dstSt.getChild(1));
                        var srcBlock = TreeUtilFunctions.findChildByType(srcSt,Constants.BLOCK);
                        var dstBlock = TreeUtilFunctions.findChildByType(dstSt,Constants.BLOCK);
                        if(srcBlock!=null && dstBlock!=null && !mappings.isSrcMapped(srcBlock) && !mappings.isDstMapped(dstBlock)){
                            addToMapping(srcBlock,dstBlock);
                        }
                        var srcNext = getNextSibling(srcSt);
                        var dstNext = getNextSibling(dstSt);
                        if(srcNext!=null && dstNext!=null && !mappings.isSrcMapped(srcNext) && !mappings.isDstMapped(dstNext)
                                && srcNext.getMetrics().hash==dstNext.getMetrics().hash){
                            matchSubTree(srcNext,dstNext);
                        }
                    }
                    break;
                }
            }
        }
    }


    protected List<Tree> getSrcUnmappedImports(){
        return src.getChildren().stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.IMPORT_DECLARATION)).toList();
    }

    protected List<Tree> getDstUnmappedImports(){
        return dst.getChildren().stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.IMPORT_DECLARATION)).toList();
    }

    protected void matchUniqueImport(){
        var srcUnmappedImports = getSrcUnmappedImports();
        var dstUnmappedImports = getDstUnmappedImports();
        if(srcUnmappedImports.size()==1 && dstUnmappedImports.size()==1){
            var srcImport = srcUnmappedImports.get(0);
            var dstImport = dstUnmappedImports.get(0);
            List<Tree> srcLeaves = TreeUtils.preOrder(srcImport).stream().filter(Tree::isLeaf).toList();
            List<Tree> dstLeaves = TreeUtils.preOrder(dstImport).stream().filter(Tree::isLeaf).toList();
            if(srcLeaves.size()==1 && dstLeaves.size()==1){
                var srcType = srcLeaves.get(0).getLabel();
                var dstType = dstLeaves.get(0).getLabel();
                if(smallEdit(srcType,dstType)){
                    matchSubTree(srcImport,dstImport);
                }
                else{
                    var srcIdx = srcType.lastIndexOf(".");
                    var dstIdx = dstType.lastIndexOf(".");
                    if(srcIdx!=-1 && dstIdx!=-1 && srcType.substring(0,srcIdx).equals(dstType.substring(0,dstIdx))){
                        matchSubTree(srcImport,dstImport);
                    }
                }
            }
        }
    }

    protected boolean isLogicalOperator(Tree tree){
        switch(tree.getLabel()){
            case "&&":
            case "||":
            case "&":
            case "|":
            case "^":
            case "!":
                return true;
            default:
                return false;
        }
    }

    protected boolean isRelationalOperator(Tree tree){
        switch(tree.getLabel()){
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
                return true;
            default:
                return false;
        }
    }

    protected boolean isArithmeticOperator(Tree tree){
        switch(tree.getLabel()){
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
            case "--":
            case "++":
                return true;
            default:
                return false;
        }
    }

    protected boolean isCompatibleOp(Tree op1,Tree op2){
        return isLogicalOperator(op1) && isLogicalOperator(op2)
                || isRelationalOperator(op1) && isRelationalOperator(op2)
                || isArithmeticOperator(op1) && isArithmeticOperator(op2);
    }

    protected boolean isNotWrapIf(Tree srcSt,Tree dstSt){
        boolean notWrapped = true;
        var srcMappedSubIfs = srcSt.getDescendants().stream().filter(child -> mappings.isSrcMapped(child) && child.getType().name.equals(Constants.IF_STATEMENT)).toList();
        if(srcMappedSubIfs.size()>0){
            for(var srcMappedSubIf: srcMappedSubIfs){
                if(!notWrapped) break;
                for(var dstMappedSubIf: mappings.getDsts(srcMappedSubIf)){
                    for(var desc: dstMappedSubIf.getDescendants()){
                        if(desc.getMetrics().hash==dstSt.getMetrics().hash){
                            notWrapped = false;
                            break;
                        }
                    }
                }
            }
        }
        if(notWrapped){
            var dstMappedSubIfs = dstSt.getDescendants().stream().filter(child -> mappings.isDstMapped(child) && child.getType().name.equals(Constants.IF_STATEMENT)).toList();
            if(dstMappedSubIfs.size()>0){
                for(var dstMappedSubIf: dstMappedSubIfs){
                    if(!notWrapped) break;
                    for(var srcMappedSubIf: mappings.getSrcs(dstMappedSubIf)){
                        for(var desc: srcMappedSubIf.getDescendants()){
                            if(desc.getMetrics().hash==srcSt.getMetrics().hash){
                                notWrapped = false;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return notWrapped;
    }

    protected void tryToMatchIf(Tree srcSt,Tree dstSt){
        var canMatch = false;
        if(!(mappings.isSrcMapped(srcSt.getChild(1)) || mappings.isDstMapped(dstSt.getChild(1)))){
            canMatch = true;
        }
        else {
            var check=false;
            var srcCond = srcSt.getChild(0);
            var dstCond = dstSt.getChild(0);
            List<Tree> srcMappedBlock = new ArrayList<>();
            List<Tree> dstMappedBlock = new ArrayList<>();
            for(int i=1;i<srcSt.getChildren().size();i++){
                if(mappings.isSrcMapped(srcSt.getChild(i)) && mappings.isSrcUnique(srcSt.getChild(i))){
                    srcMappedBlock.add(srcSt.getChild(i));
                }
            }
            for(int i=1;i<dstSt.getChildren().size();i++){
                if(mappings.isDstMapped(dstSt.getChild(i)) && mappings.isDstUnique(dstSt.getChild(i))){
                    dstMappedBlock.add(dstSt.getChild(i));
                }
            }
            for(var srcBlock: srcMappedBlock){
                for(var dstBlock: dstMappedBlock){
                    if(dstSt.getChildren().contains(mappings.getDsts(srcBlock).iterator().next()) && srcSt.getChildren().contains(mappings.getSrcs(dstBlock).iterator().next())){
                        if(srcCond.hasSameType(dstCond) || srcSt.getChildren().indexOf(srcBlock)==dstSt.getChildren().indexOf(dstBlock)){
                            check = true;
                            break;
                        }
                    }
                }
            }
            if(check){
                canMatch = isNotWrapIf(srcSt,dstSt);
            }
        }
        if(canMatch){
            var result=getSameLeaves(srcSt.getChild(0),dstSt.getChild(0));
            if (result.second && (result.first.size()>1 || (result.first.size()==1 && result.first.get(0).first.getType().name.equals(Constants.SIMPLE_NAME)))) {
                if(result.first.size()>1 && result.first.stream().filter(t->t.first.getType().name.equals(Constants.SIMPLE_NAME)).count()==0){
                    List<Tree> srcLeaves = TreeUtils.preOrder(srcSt.getChild(0)).stream().filter(Tree::isLeaf).toList();
                    List<Tree> dstLeaves = TreeUtils.preOrder(dstSt.getChild(0)).stream().filter(Tree::isLeaf).toList();
                    if(result.first.size()*10<Math.max(srcLeaves.size(),dstLeaves.size())){
                        canMatch=false;
                    }
                }
                if(canMatch){
                    matchSubTree(srcSt.getChild(0), dstSt.getChild(0));
                    addToMapping(srcSt, dstSt);
                    if(!mappings.isSrcMapped(srcSt.getChild(1)) && !mappings.isDstMapped(dstSt.getChild(1)) && srcSt.getChild(1).hasSameType(dstSt.getChild(1))){
                        addToMapping(srcSt.getChild(1), dstSt.getChild(1));
                    }
                }
            }
        }
    }

    protected boolean smallEdit(String a,String b){
        var prefixlen = 0;
        var subfixlen = 0;
        var minLen=Math.min(a.length(),b.length());
        for(int i=0;i<minLen;i++){
            if(a.charAt(i)==b.charAt(i))
                prefixlen++;
            else
                break;
        }
        for(int i=0;i<minLen;i++){
            if(a.charAt(a.length()-1-i)==b.charAt(b.length()-1-i))
                subfixlen++;
            else
                break;
        }
        return prefixlen>1 && subfixlen>1 && prefixlen+subfixlen>=Math.max(a.length(),b.length())/2;
    }

    protected void fixInfixExpByChildren(Tree parentSrc,Tree parentDst,Tree src,Tree dst){
        var opSrc = parentSrc.getChild(1);
        var opDst = parentDst.getChild(1);
        if(isCompatibleOp(opSrc,opDst)){
            if(mappings.isSrcMapped(parentSrc) && mappings.isSrcUnique(parentSrc)){
                var mappedDst = mappings.getDsts(parentSrc).iterator().next();
                var opMappedDst = mappedDst.getChild(1);
                if(!isCompatibleOp(opSrc,opMappedDst)){
                    removeFromMapping(parentSrc,mappedDst);
                    if(mappings.isSrcMapped(opSrc) && mappings.isDstMapped(opMappedDst) && mappings.getDsts(opSrc).iterator().next()==opMappedDst){
                        removeFromMapping(opSrc,opMappedDst);
                    }
                    addToMapping(parentSrc,parentDst);
                    addToMapping(opSrc,opDst);
                }
            }
            else if(mappings.isDstMapped(parentDst) && mappings.isDstUnique(parentDst)){
                var mappedSrc = mappings.getSrcs(parentDst).iterator().next();
                var opMappedSrc = mappedSrc.getChild(1);
                if(!isCompatibleOp(opMappedSrc,opDst)){
                    removeFromMapping(mappedSrc,parentDst);
                    if(mappings.isSrcMapped(opMappedSrc) && mappings.isDstMapped(opDst) && mappings.getSrcs(opDst).iterator().next()==opMappedSrc){
                        removeFromMapping(opMappedSrc,opDst);
                    }
                    addToMapping(parentSrc,parentDst);
                    addToMapping(opSrc,opDst);
                }
            }
        }
    }

    protected void fixInfixExpByChildren2(Tree parentSrc,Tree parentDst,Tree src,Tree dst){
        if(parentSrc.getMetrics().hash==parentDst.getMetrics().hash){
            var leftOp = parentSrc.getChild(0);
            var op = parentSrc.getChild(1);
            var rightOp = parentSrc.getChild(2);
            if(mappings.isSrcMapped(leftOp) && mappings.isSrcUnique(leftOp)){
                var mappedLeftOp = mappings.getDsts(leftOp).iterator().next();
                if(mappedLeftOp.getParent()!=parentDst){
                    removeMappingSubTree(leftOp,mappedLeftOp);
                }
            }
            if(mappings.isSrcMapped(op) && mappings.isSrcUnique(op)){
                var mappedOp = mappings.getDsts(op).iterator().next();
                if(mappedOp.getParent()!=parentDst){
                    removeMappingSubTree(op,mappedOp);
                }
            }
            if(mappings.isSrcMapped(rightOp) && mappings.isSrcUnique(rightOp)){
                var mappedRightOp = mappings.getDsts(rightOp).iterator().next();
                if(mappedRightOp.getParent()!=parentDst){
                    removeMappingSubTree(rightOp,mappedRightOp);
                }
            }
            leftOp = parentDst.getChild(0);
            op = parentDst.getChild(1);
            rightOp = parentDst.getChild(2);
            if(mappings.isDstMapped(leftOp) && mappings.isDstUnique(leftOp)){
                var mappedLeftOp = mappings.getSrcs(leftOp).iterator().next();
                if(mappedLeftOp.getParent()!=parentSrc){
                    removeMappingSubTree(mappedLeftOp,leftOp);
                }
            }
            if(mappings.isDstMapped(op) && mappings.isDstUnique(op)){
                var mappedOp = mappings.getSrcs(op).iterator().next();
                if(mappedOp.getParent()!=parentSrc){
                    removeMappingSubTree(mappedOp,op);
                }
            }
            if(mappings.isDstMapped(rightOp) && mappings.isDstUnique(rightOp)){
                var mappedRightOp = mappings.getSrcs(rightOp).iterator().next();
                if(mappedRightOp.getParent()!=parentSrc){
                    removeMappingSubTree(mappedRightOp,rightOp);
                }
            }
            addToMappingRecursively(parentSrc,parentDst);
        }
    }

    protected void fixClassCreationByChildren(Tree parentSrc,Tree parentDst,Tree src,Tree dst){
        if(mappings.isSrcMapped(parentSrc) && mappings.isSrcUnique(parentSrc)){
            var mappedDst = mappings.getDsts(parentSrc).iterator().next();
            if(countMappedChild(parentSrc,parentDst)>countMappedChild(parentSrc,mappedDst)){
                removeMappingSubTree(parentSrc,mappedDst);
                matchSubTree(parentSrc,parentDst);
                matchSubTree(parentSrc,mappedDst);
            }
        }
        else if(mappings.isDstMapped(parentDst) && mappings.isDstUnique(parentDst)){
            var mappedSrc = mappings.getSrcs(parentDst).iterator().next();
            if(countMappedChild(parentSrc,parentDst)>countMappedChild(mappedSrc,parentDst)){
                removeMappingSubTree(mappedSrc,parentDst);
                matchSubTree(parentSrc,parentDst);
                matchSubTree(mappedSrc,parentDst);
            }
        }
    }

    protected void fixDiffTypeByChildren(Tree parentSrc,Tree parentDst,Tree src,Tree dst){
        if(!mappings.isSrcMapped(parentSrc)){
            if(parentSrc.getType().name.equals(Constants.PREFIX_EXPRESSION) && isArithmeticOperator(parentSrc.getChild(0))){
                var srcParents = parentSrc.getParents().stream().filter(p -> p.getType().name.equals(Constants.BLOCK)).findFirst();
                if(srcParents.isPresent() && mappings.isSrcMapped(srcParents.get())){
                    var mappedDstBlock = mappings.getDsts(srcParents.get()).iterator().next();
                    var candidates = TreeUtils.preOrder(mappedDstBlock).stream().filter(t->t.getPos()<dst.getPos() && !mappings.isDstMapped(t) && t.getMetrics().hash==parentSrc.getMetrics().hash).toList();
                    if(candidates.size()>0){
                        var candidate = candidates.get(candidates.size()-1);
                        removeMappingSubTree(src,dst);
                        matchSubTree(parentSrc,candidate);
                    }
                }
            }
            else if(parentSrc.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT) && parentDst.getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)){
                var srcVarName = TreeUtilFunctions.findChildByType(parentSrc,Constants.SIMPLE_NAME);
                if(srcVarName!=null && !mappings.isSrcMapped(srcVarName)){
                    if(mappings.isDstMapped(parentDst) && mappings.isDstUnique(parentDst)){
                        var mappedSrc = mappings.getSrcs(parentDst).iterator().next();
                        for(var argument:mappedSrc.getChildren()){
                            if(argument.getMetrics().hash==srcVarName.getMetrics().hash && mappings.isSrcMapped(argument)){
                                for(var mappedDst:mappings.getDsts(argument)){
                                    removeMappingSubTree(argument,mappedDst);
                                }
                            }
                        }
                    }
                }
            }
        }
        else if(!mappings.isDstMapped(parentDst)){
            if(parentDst.getType().name.equals(Constants.PREFIX_EXPRESSION) && isArithmeticOperator(parentDst.getChild(0))){
                var dstParents = parentDst.getParents().stream().filter(p -> p.getType().name.equals(Constants.BLOCK)).findFirst();
                if(dstParents.isPresent() && mappings.isDstMapped(dstParents.get())){
                    var mappedSrcBlock = mappings.getSrcs(dstParents.get()).iterator().next();
                    var candidates = TreeUtils.preOrder(mappedSrcBlock).stream().filter(t->t.getPos()<src.getPos() && !mappings.isSrcMapped(t) && t.getMetrics().hash==parentDst.getMetrics().hash).toList();
                    if(candidates.size()>0){
                        var candidate = candidates.get(candidates.size()-1);
                        removeMappingSubTree(src,dst);
                        matchSubTree(candidate,parentDst);
                    }
                }
            }
            else if(parentDst.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT) && parentSrc.getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)){
                var dstVarName = TreeUtilFunctions.findChildByType(parentDst,Constants.SIMPLE_NAME);
                if(dstVarName!=null && !mappings.isDstMapped(dstVarName)){
                    if(mappings.isSrcMapped(parentSrc) && mappings.isSrcUnique(parentSrc)){
                        var mappedDst = mappings.getDsts(parentSrc).iterator().next();
                        for(var argument:mappedDst.getChildren()){
                            if(argument.getMetrics().hash==dstVarName.getMetrics().hash && mappings.isDstMapped(argument)){
                                for(var mappedSrc:mappings.getSrcs(argument)){
                                    removeMappingSubTree(mappedSrc,argument);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean hasReverseCond(Tree srcCond,Tree dstCond){
        if(srcCond.getType().name.equals(Constants.PREFIX_EXPRESSION) && isLogicalOperator(srcCond.getChild(0)) && srcCond.getChild(1).hasSameType(dstCond)){
            return srcCond.getChild(1).getMetrics().hash==dstCond.getMetrics().hash || mappings.isSrcMapped(srcCond.getChild(1)) && mappings.getDsts(srcCond.getChild(1)).contains(dstCond);
        }
        if(dstCond.getType().name.equals(Constants.PREFIX_EXPRESSION) && isLogicalOperator(dstCond.getChild(0)) && dstCond.getChild(1).hasSameType(srcCond)){
            return dstCond.getChild(1).getMetrics().hash==srcCond.getMetrics().hash || mappings.isDstMapped(dstCond.getChild(1)) && mappings.getSrcs(dstCond.getChild(1)).contains(srcCond);
        }
        return false;
    }

    protected int countMappedChild(Tree src,Tree dst){
        int count=0;
        for(var child1:src.getChildren()){
            for(var child2:dst.getChildren()){
                if(mappings.isSrcMapped(child1) && mappings.isDstMapped(child2) && mappings.getDsts(child1).contains(child2)){
                    ++count;
                }
            }
        }
        return count;
    }

//    protected void fixStmtByIdenticalParent(Tree src,Tree dst){
//        var srcStmts = getLeafStmts(src);
//        var dstStmts = getLeafStmts(dst);
//        if(srcStmts.size()==dstStmts.size()){
//            for(int i=0;i<srcStmts.size();i++){
//                var srcOne = srcStmts.get(i);
//                var dstOne = dstStmts.get(i);
//                var b1 = mappings.isSrcMapped(srcOne) && mappings.isSrcUnique(srcOne);
//                var b2 = mappings.isDstMapped(dstOne)  && mappings.isDstUnique(dstOne);
//                if(b1 && b2){
//                    if(!mappings.getDsts(srcOne).contains(dstOne)){
//                        var srcMappping = mappings.getDsts(srcOne).iterator().next();
//                        var dstMappping = mappings.getSrcs(dstOne).iterator().next();
//                        removeMappingSubTree(srcOne,srcMappping);
//                        removeMappingSubTree(dstMappping,dstOne);
//                        matchSubTree(srcOne,dstOne);
//                    }
//                }
//                else if(b1 && !mappings.isDstMapped(dstOne)){
//                    var srcMappping = mappings.getDsts(srcOne).iterator().next();
//                    removeMappingSubTree(srcOne,srcMappping);
//                    matchSubTree(srcOne,dstOne);
//                }
//                else if(b2 && !mappings.isSrcMapped(srcOne)){
//                    var dstMappping = mappings.getSrcs(dstOne).iterator().next();
//                    removeMappingSubTree(dstMappping,dstOne);
//                    matchSubTree(srcOne,dstOne);
//                }
//            }
//        }
//    }

    protected void fixArbitraryMatchThisByCheckParent(Tree src,Tree dst){
        var srcParent = src.getParent();
        var dstParent = dst.getParent();
        var b1 = srcParent.getType().name.equals("ExpressionMethodReference");
        var b2 = dstParent.getType().name.equals("ExpressionMethodReference");
        var b3 = srcParent.getType().name.equals("FieldAccess");
        var b4 = dstParent.getType().name.equals("FieldAccess");
        if(b1&&b4 || b2&&b3){
            removeFromMapping(src,dst);
        }
    }

    protected void fixArbitraryMatchSimpleNameByCheckParent(Tree src,Tree dst){
        var srcParent = src.getParent();
        var dstParent = dst.getParent();
        if(!mappings.isSrcMapped(srcParent) && !mappings.isDstMapped(dstParent) && src.getLabel().length()<=2){
            var srcPP = srcParent.getParent();
            var dstPP = dstParent.getParent();
            if(srcPP!=null && dstPP!=null && !mappings.isSrcMapped(srcPP) && !mappings.isDstMapped(dstPP)){
                removeFromMapping(src,dst);
            }
        }
    }

    protected void fixArbitraryMatchedBlockByCheckParent(Tree src,Tree dst) {
        var srcParent = src.getParent();
        var dstParent = dst.getParent();
        var b1 = srcParent.getType().name.equals(Constants.WHILE_STATEMENT);
        var b2 = dstParent.getType().name.equals(Constants.WHILE_STATEMENT);
        var b3 = srcParent.getType().name.equals(Constants.IF_STATEMENT);
        var b4 = dstParent.getType().name.equals(Constants.IF_STATEMENT);
        if (b1 && b4 || b2 && b3) {
            if (countMappedChild(src, dst) == 0 && !hasInnerMapping(srcParent.getChild(0), dstParent.getChild(0))) {
                removeFromMapping(src, dst);
            }
        }
    }

    protected void fixStmtByReverseCondIfParent(Tree src,Tree dst){
        var srcStmts = getLeafStmts(src);
        var dstStmts = getLeafStmts(dst);
        if(srcStmts.size()==0 || dstStmts.size()==0) return;
        var srcLastStmt = srcStmts.get(srcStmts.size()-1);
        var dstLastStmt = dstStmts.get(dstStmts.size()-1);
        if(srcLastStmt.getType().name.equals(Constants.RETURN_STATEMENT) && dstLastStmt.getType().name.equals(Constants.RETURN_STATEMENT)){
            if(!mappings.isDstMapped(dstLastStmt)){
                var candidates = src.getParent().getChildren().stream().filter(t->t.getPos()>src.getEndPos() && t.getType().name.equals(Constants.RETURN_STATEMENT) && t.getMetrics().hash == dstLastStmt.getMetrics().hash).toList();
                if(candidates.size()>0){
                    var candidate = candidates.get(0);
                    if(mappings.isSrcMapped(candidate) && mappings.isSrcUnique(candidate)){
                        var mappedDst = mappings.getDsts(candidate).iterator().next();
                        if(mappedDst.getPos()<dst.getPos()){
                            removeMappingSubTree(candidate,mappedDst);
                        }
                    }
                    if(!mappings.isSrcMapped(candidate)){
                        matchSubTree(candidate,dstLastStmt);
                    }
                }
            }
            if(!mappings.isSrcMapped(srcLastStmt)){
                var candidates = dst.getParent().getChildren().stream().filter(t->t.getPos()>dst.getEndPos() && t.getType().name.equals(Constants.RETURN_STATEMENT) && t.getMetrics().hash == srcLastStmt.getMetrics().hash).toList();
                if(candidates.size()>0){
                    var candidate = candidates.get(0);
                    if(mappings.isDstMapped(candidate) && mappings.isDstUnique(candidate)){
                        var mappedSrc = mappings.getSrcs(candidate).iterator().next();
                        if(mappedSrc.getPos()<src.getPos()){
                            removeMappingSubTree(mappedSrc,candidate);
                        }
                    }
                    if(!mappings.isDstMapped(candidate)){
                        matchSubTree(srcLastStmt,candidate);
                    }
                }
            }
        }
    }

    protected void fixBreakBySwitchParent(Tree src,Tree dst){
        var srcStmts = src.getChildren();
        var dstStmts = dst.getChildren();
        var breakMap = new HashMap<Tree,Tree>();
        var breakRevMap = new HashMap<Tree,Tree>();
        var srcCases = new ArrayList<Tree>();
        for(int i=0;i<srcStmts.size();++i){
            var srcStmt=srcStmts.get(i);
            if(srcStmt.getType().name.equals(Constants.SWITCH_CASE)){
                for(int j=i+1;j<srcStmts.size();++j){
                    if(srcStmts.get(j).getType().name.equals(Constants.BREAK_STATEMENT)){
                        srcCases.add(srcStmt);
                        breakMap.put(srcStmt,srcStmts.get(j));
                        breakRevMap.put(srcStmts.get(j),srcStmt);
                        break;
                    }
                }
            }
        }
        for(int i=0;i<dstStmts.size();++i){
            var dstStmt=dstStmts.get(i);
            if(dstStmt.getType().name.equals(Constants.SWITCH_CASE)){
                for(int j=i+1;j<dstStmts.size();++j){
                    if(dstStmts.get(j).getType().name.equals(Constants.BREAK_STATEMENT)){
                        breakMap.put(dstStmt,dstStmts.get(j));
                        breakRevMap.put(dstStmts.get(j),dstStmt);
                        break;
                    }
                }
            }
        }
        if(breakMap.size()>2){
            for(var srcCase:srcCases){
                if(mappings.isSrcMapped(srcCase) && mappings.isSrcUnique(srcCase)){
                    var dstCase = mappings.getDsts(srcCase).iterator().next();
                    if(breakMap.containsKey(srcCase) && breakMap.containsKey(dstCase)){
                        var srcBreak = breakMap.get(srcCase);
                        var dstBreak = breakMap.get(dstCase);
                        if(!mappings.isSrcMapped(srcBreak) && !mappings.isDstMapped(dstBreak)){
                            addToMapping(srcBreak,dstBreak);
                        }
                        else if(mappings.isSrcMapped(srcBreak) && !mappings.isDstMapped(dstBreak)){
                            var dstMappedBreak = mappings.getDsts(srcBreak).iterator().next();
                            if(breakRevMap.containsKey(dstMappedBreak) && breakRevMap.get(dstMappedBreak)!=dstCase){
                                removeFromMapping(srcBreak,dstMappedBreak);
                                addToMapping(srcBreak,dstBreak);
                            }
                        }
                        else if(!mappings.isSrcMapped(srcBreak) && mappings.isDstMapped(dstBreak)){
                            var srcMappedBreak = mappings.getSrcs(dstBreak).iterator().next();
                            if(breakRevMap.containsKey(srcMappedBreak) && breakRevMap.get(srcMappedBreak)!=srcCase){
                                removeFromMapping(srcMappedBreak,dstBreak);
                                addToMapping(srcBreak,dstBreak);
                            }
                        }
                        else{
                            var srcMappedBreak = mappings.getSrcs(dstBreak).iterator().next();
                            var dstMappedBreak = mappings.getDsts(srcBreak).iterator().next();
                            if(srcMappedBreak!=srcBreak && dstMappedBreak!=dstBreak){
                                removeFromMapping(srcMappedBreak,dstBreak);
                                removeFromMapping(srcBreak,dstMappedBreak);
                                addToMapping(srcBreak,dstBreak);
                                if(breakRevMap.containsKey(srcMappedBreak) && breakRevMap.containsKey(dstMappedBreak)){
                                    var srcRevCase = breakRevMap.get(srcMappedBreak);
                                    var dstRevCase = breakRevMap.get(dstMappedBreak);
                                    if(mappings.isSrcMapped(srcRevCase) && mappings.getDsts(srcRevCase).contains(dstRevCase)){
                                        addToMapping(srcMappedBreak,dstMappedBreak);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected void fixInnerCond(Tree srcCondTree,Tree dstCondTree){
        var srcConds = flattenCond(srcCondTree);
        var dstConds = flattenCond(dstCondTree);
        if(srcConds.size()==dstConds.size()){
            for(int i=0;i<srcConds.size();i++){
                var srcCond = srcConds.get(i);
                var dstCond = dstConds.get(i);
                if(srcCond.getMetrics().hash==dstCond.getMetrics().hash && !(mappings.isSrcMapped(srcCond) && mappings.getDsts(srcCond).contains(dstCond))){
                    if(!mappings.isSrcMapped(srcCond) && !mappings.isDstMapped(dstCond)){
                        matchSubTree(srcCond,dstCond);
                    }
                    else if(mappings.isSrcMapped(srcCond) && mappings.isSrcUnique(srcCond)){
                        var dstMappedCond = mappings.getDsts(srcCond).iterator().next();
                        removeMappingSubTree(srcCond,dstMappedCond);
                        matchSubTree(srcCond,dstCond);
                    }
                    else if(mappings.isDstMapped(dstCond) && mappings.isDstUnique(dstCond)){
                        var srcMappedCond = mappings.getSrcs(dstCond).iterator().next();
                        removeMappingSubTree(srcMappedCond,dstCond);
                        matchSubTree(srcCond,dstCond);
                    }
                }
                else if(!(mappings.isSrcMapped(srcCond) || mappings.isDstMapped(dstCond))){
                    var result = getSameLeaves(srcCond,dstCond);
                    if(result.second && result.first.size()>1){
                        matchSubTree(srcCond,dstCond);
                    }
                }
            }
        }
        else if(srcConds.size()==1){
            var srcCond = srcConds.get(0);
            if(srcCond.getType().name.equals(Constants.INFIX_EXPRESSION) && !mappings.isSrcMapped(srcCond.getChild(2))){
                if(!(mappings.isSrcMapped(srcCond) && mappings.isSrcUnique(srcCond)))
                    return;
                var dstMappedCond = mappings.getDsts(srcCond).iterator().next();
                var leftOpr = srcCond.getChild(0);
                List<Tree> candidates = new ArrayList<>();
                for(var dstCond:dstConds){
                    if(dstCond.getType().name.equals(Constants.INFIX_EXPRESSION) && dstCond.getChild(0).getMetrics().hash==leftOpr.getMetrics().hash && !mappings.isDstMapped(dstCond.getChild(2))){
                        candidates.add(dstCond);
                    }
                }
                if(candidates.size()>0){
                    var leftOp = srcCond.getChild(1).getLabel();
                    Tree bestCandidate = candidates.get(0);
                    var bestLeftOp = bestCandidate.getChild(1).getLabel();
                    for(int i=1;i<candidates.size();i++){
                        var candidate = candidates.get(i);
                        if(!mappings.isDstMapped(candidate)){
                            var leftOp2 = candidate.getChild(1).getLabel();
                            if(leftOp2.equals(leftOp)){
                                if(!bestLeftOp.equals(leftOp)){
                                    bestCandidate = candidate;
                                    bestLeftOp = bestCandidate.getChild(1).getLabel();
                                }
                            }
                            else if(leftOp2.contains(leftOp) || leftOp.contains(leftOp2)){
                                if(!(bestLeftOp.contains(leftOp) || leftOp.contains(bestLeftOp))){
                                    bestCandidate = candidate;
                                    bestLeftOp = bestCandidate.getChild(1).getLabel();
                                }
                            }
                        }
                    }
                    if(dstMappedCond!=bestCandidate && (bestLeftOp.contains(leftOp) || leftOp.contains(bestLeftOp))){
                        removeMappingSubTree(srcCond,dstMappedCond);
                        matchSubTree(srcCond,bestCandidate);
                    }
                }
            }
        }
        else if(dstConds.size()==1){
            var dstCond = dstConds.get(0);
            if(dstCond.getType().name.equals(Constants.INFIX_EXPRESSION) && !mappings.isDstMapped(dstCond.getChild(2))){
                if(!(mappings.isDstMapped(dstCond) && mappings.isDstMapped(dstCond)))
                    return;
                var srcMappedCond = mappings.getSrcs(dstCond).iterator().next();
                var leftOpr = dstCond.getChild(0);
                List<Tree> candidates = new ArrayList<>();
                for(var srcCond:srcConds){
                    if(srcCond.getType().name.equals(Constants.INFIX_EXPRESSION) && srcCond.getChild(0).getMetrics().hash==leftOpr.getMetrics().hash && !mappings.isSrcMapped(srcCond.getChild(2))){
                        candidates.add(srcCond);
                    }
                }
                if(candidates.size()>0){
                    var leftOp = dstCond.getChild(1).getLabel();
                    Tree bestCandidate = candidates.get(0);
                    var bestLeftOp = bestCandidate.getChild(1).getLabel();
                    for(int i=1;i<candidates.size();i++){
                        var candidate = candidates.get(i);
                        if(!mappings.isSrcMapped(candidate)){
                            var leftOp2 = candidate.getChild(1).getLabel();
                            if(leftOp2.equals(leftOp)){
                                if(!bestLeftOp.equals(leftOp)){
                                    bestCandidate = candidate;
                                    bestLeftOp = bestCandidate.getChild(1).getLabel();
                                }
                            }
                            else if(leftOp2.contains(leftOp) || leftOp.contains(leftOp2)){
                                if(!(bestLeftOp.contains(leftOp) || leftOp.contains(bestLeftOp))){
                                    bestCandidate = candidate;
                                    bestLeftOp = bestCandidate.getChild(1).getLabel();
                                }
                            }
                        }
                    }
                    if(srcMappedCond!=bestCandidate && (bestLeftOp.contains(leftOp) || leftOp.contains(bestLeftOp))){
                        removeMappingSubTree(srcMappedCond,dstCond);
                        matchSubTree(bestCandidate,dstCond);
                    }
                }
            }
        }
    }

    protected void fixIfBlockByParent(Tree srcBlock,Tree dstBlock){
        if(!mappings.isDstMapped(dstBlock) && mappings.isSrcMapped(srcBlock) && mappings.isSrcUnique(srcBlock)){
            var mappedDstBlock = mappings.getDsts(srcBlock).iterator().next();
            if(mappedDstBlock.getParents().contains(dstBlock)){
                removeFromMapping(srcBlock,mappedDstBlock);
                addToMapping(srcBlock,dstBlock);
            }
        }
        else if(!mappings.isSrcMapped(srcBlock) && mappings.isDstMapped(dstBlock) && mappings.isDstUnique(dstBlock)){
            var mappedSrcBlock = mappings.getSrcs(dstBlock).iterator().next();
            if(mappedSrcBlock.getParents().contains(srcBlock)){
                removeFromMapping(mappedSrcBlock,dstBlock);
                addToMapping(srcBlock,dstBlock);
            }
        }
    }

    protected void fixMethodArgsByParent1(Tree srcCall,Tree dstCall, Tree src, Tree dst){
        if (mappings.isSrcMapped(srcCall) && mappings.isSrcUnique(srcCall)) {
            var mappedDstCall = mappings.getDsts(srcCall).iterator().next();
            var mappedDstArgs = TreeUtilFunctions.findChildByType(mappedDstCall, Constants.METHOD_INVOCATION_ARGUMENTS);
            if (mappedDstArgs != null && !mappings.isDstMapped(mappedDstArgs) && mappedDstArgs.getChildren().contains(dstCall) && countMappedChild(src, mappedDstArgs) > 0) {
                removeFromMapping(src, dst);
                addToMapping(src, mappedDstArgs);
            }
        } else if (mappings.isDstMapped(dstCall) && mappings.isDstUnique(dstCall)) {
            var mappedSrcCall = mappings.getSrcs(dstCall).iterator().next();
            var mappedSrcArgs = TreeUtilFunctions.findChildByType(mappedSrcCall, Constants.METHOD_INVOCATION_ARGUMENTS);
            if (mappedSrcArgs != null && !mappings.isSrcMapped(mappedSrcArgs) && mappedSrcArgs.getChildren().contains(srcCall) && countMappedChild(mappedSrcArgs, dst) > 0) {
                removeFromMapping(src, dst);
                addToMapping(mappedSrcArgs, dst);
            }
        }
    }

    protected void fixMethodArgsByParent2(Tree src, Tree dst){
        var visDst = new HashSet<Tree>();
        var visSrc = new HashSet<Tree>();
        for (var arg : src.getChildren()) {
            if (mappings.isSrcMapped(arg) && mappings.isSrcUnique(arg)) {
                var dstArg = mappings.getDsts(arg).iterator().next();
                if (dstArg.getParent().getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)) {
                    if (!visDst.contains(dstArg.getParent())) {
                        visDst.add(dstArg.getParent());
                    }
                }
            }
        }
        for (var arg : dst.getChildren()) {
            if (mappings.isDstMapped(arg) && mappings.isDstUnique(arg)) {
                var srcArg = mappings.getSrcs(arg).iterator().next();
                if (srcArg.getParent().getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)) {
                    if (!visSrc.contains(srcArg.getParent())) {
                        visSrc.add(srcArg.getParent());
                    }
                }
            }
        }
        if (!(visDst.size() == 1 && visSrc.size() == 1)) {
            removeFromMapping(src, dst);
            if (visDst.size() > 1) {
                for (var dstArgs : visDst) {
                    if (dstArgs.getChildren().size() == 1) {
                        var dstArg = dstArgs.getChild(0);
                        if (mappings.isDstMapped(dstArg) && mappings.isDstUnique(dstArg) && dstArg.getType().name.equals(Constants.SIMPLE_NAME)) {
                            var dstOutter = getOutterLeaf(dstArg);
                            if (dstOutter != null) {
                                var leftChoice = TreeUtils.preOrder(dstOutter).stream().filter(t -> !mappings.isDstMapped(t) && t.getMetrics().hash == dstArg.getMetrics().hash).toList();
                                if (!leftChoice.isEmpty())
                                    removeFromMapping(mappings.getSrcs(dstArg).iterator().next(), dstArg);
                            }
                        }
                    }
                }
            }
            if (visSrc.size() > 1) {
                for (var srcArgs : visSrc) {
                    if (srcArgs.getChildren().size() == 1) {
                        var srcArg = srcArgs.getChild(0);
                        if (mappings.isSrcMapped(srcArg) && mappings.isSrcUnique(srcArg) && srcArg.getType().name.equals(Constants.SIMPLE_NAME)) {
                            var srcOutter = getOutterLeaf(srcArg);
                            if (srcOutter != null) {
                                var leftChoice = TreeUtils.preOrder(srcOutter).stream().filter(t -> !mappings.isSrcMapped(t) && t.getMetrics().hash == srcArg.getMetrics().hash).toList();
                                if (!leftChoice.isEmpty())
                                    removeFromMapping(srcArg, mappings.getDsts(srcArg).iterator().next());
                            }
                        }
                    }
                }
            }
        }
    }

    protected void fixMethodInvocationByParent(Tree src,Tree dst){
        if (src.getParent().getType().name.equals(Constants.INFIX_EXPRESSION) && isLogicalOperator(src.getParent().getChild(1))) {
            var candidates = src.getParent().getChildren().stream().filter(t -> !mappings.isSrcMapped(t) && t.getType().name.equals(Constants.METHOD_INVOCATION) && t != src).toList();
            if (candidates.size() == 1) {
                var candidate = candidates.get(0);
                var result1 = sameInnerCall(candidate, dst);
                var result2 = sameInnerCall(src, dst);
                if (result1 > result2) {
                    removeMappingSubTree(src, dst);
                    matchSubTree(candidate, dst);
                }
            }
        }
        if (dst.getParent().getType().name.equals(Constants.INFIX_EXPRESSION) && isLogicalOperator(dst.getParent().getChild(1))) {
            var candidates = dst.getParent().getChildren().stream().filter(t -> !mappings.isDstMapped(t) && t.getType().name.equals(Constants.METHOD_INVOCATION) && t != dst).toList();
            if (candidates.size() == 1) {
                var candidate = candidates.get(0);
                var result1 = sameInnerCall(src, candidate);
                var result2 = sameInnerCall(src, dst);
                if (result1 > result2) {
                    removeMappingSubTree(src, dst);
                    matchSubTree(src, candidate);
                }
            }
        }
    }

    protected void fixStringLiteralByParent(Tree src,Tree dst){
        if (src.getParent().getType().name.equals(Constants.INFIX_EXPRESSION) && src.getParent().getChild(1).getLabel().equals("+")) {
            var candidates = src.getParent().getChildren().stream().filter(t -> !mappings.isSrcMapped(t) && t.getType().name.equals(Constants.STRING_LITERAL) && t != src).toList();
            if (candidates.size() == 1) {
                var candidate = candidates.get(0);
                var csl1 = commonSubfixLen(candidate, dst);
                var csl2 = commonSubfixLen(src, dst);
                var cpl1 = commonPrefixLen(candidate, dst);
                var cpl2 = commonPrefixLen(src, dst);
                if (csl1 > csl2 && cpl1 >= cpl2 || cpl1 > cpl2 && csl1 >= csl2) {
                    removeFromMapping(src, dst);
                    addToMapping(candidate, dst);
                }
            }
        }
        if (dst.getParent().getType().name.equals(Constants.INFIX_EXPRESSION) && dst.getParent().getChild(1).getLabel().equals("+")) {
            var candidates = dst.getParent().getChildren().stream().filter(t -> !mappings.isDstMapped(t) && t.getType().name.equals(Constants.STRING_LITERAL) && t != dst).toList();
            if (candidates.size() == 1) {
                var candidate = candidates.get(0);
                var csl1 = commonSubfixLen(src, candidate);
                var csl2 = commonSubfixLen(src, dst);
                var cpl1 = commonPrefixLen(src, candidate);
                var cpl2 = commonPrefixLen(src, dst);
                if (csl1 > csl2 && cpl1 >= cpl2 || cpl1 > cpl2 && csl1 >= csl2) {
                    removeFromMapping(src, dst);
                    addToMapping(src, candidate);
                }
            }
        }
    }

    protected void fixUniqueAroundMethodCall(Tree src, Tree dst){
        if(getInnerCallSize(src)==1 && getInnerCallSize(dst)==1){
            var srcBlock = TreeUtilFunctions.getParentUntilType(src,Constants.BLOCK);
            var dstBlock = TreeUtilFunctions.getParentUntilType(dst,Constants.BLOCK);
            if(srcBlock!=null && dstBlock!=null){
                var srcParent =srcBlock.getParent();
                var dstParent =dstBlock.getParent();
                var removed = false;
                if(!mappings.isSrcMapped(srcParent)){
                    var mappedLeaves = getMappedSrcTrees(srcParent);
                    if(mappedLeaves.size()==2){
                        removeMappingSubTree(src,dst);
                        removed = true;
                    }
                }
                if(!removed && !mappings.isDstMapped(dstParent)){
                    var mappedLeaves = getMappedDstTrees(dstParent);
                    if(mappedLeaves.size()==2){
                        removeMappingSubTree(src,dst);
                    }
                }
            }
        }
    }

    protected void fixUniqueAroundRetStmt(Tree src, Tree dst){
        if(src.getChildren().size()>0 && dst.getChildren().size()>0){
            var mappedSrcNodes = getMappedSrcTrees(src);
            var mappedDstNodes = getMappedDstTrees(dst);
            if(mappedSrcNodes.size()==1 && mappedDstNodes.size()==1){
                var srcMethodDecl = TreeUtilFunctions.getParentUntilType(src,Constants.METHOD_DECLARATION);
                var dstMethodDecl = TreeUtilFunctions.getParentUntilType(dst,Constants.METHOD_DECLARATION);
                if(srcMethodDecl!=null && dstMethodDecl!=null){
                    var srcBlock = TreeUtilFunctions.findChildByType(srcMethodDecl,Constants.BLOCK);
                    var dstBlock = TreeUtilFunctions.findChildByType(dstMethodDecl,Constants.BLOCK);
                    if(srcBlock!=null && dstBlock!=null){
                        if(srcBlock.getChildren().size()>1 && dstBlock.getChildren().size()>1){
                            var mappedSrcNodes2 = getMappedSrcTrees(srcBlock);
                            var mappedDstNodes2 = getMappedDstTrees(dstBlock);
                            if(mappedSrcNodes2.size()==2 && mappedDstNodes2.size()==2){
                                removeMappingSubTree(src,dst);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void fixUniqueAroundExpStmt(Tree src, Tree dst){
        var srcInner = src.getChild(0);
        var dstInner = dst.getChild(0);
        if(srcInner.getType().name.equals(Constants.ASSIGNMENT) && dstInner.hasSameType(srcInner)){
            var mappedSrcNodes = getMappedSrcTrees(src);
            var mappedDstNodes = getMappedDstTrees(dst);
            if(mappedSrcNodes.size()<=4 && mappedDstNodes.size()<=4){
                var srcBlock = TreeUtilFunctions.getParentUntilType(src,Constants.BLOCK);
                var dstBlock = TreeUtilFunctions.getParentUntilType(dst,Constants.BLOCK);
                var removed = false;
                if(srcBlock!=null && srcBlock.getParent().getType().name.equals(Constants.IF_STATEMENT)){
                    var mappedSrcNodes2 = getMappedSrcTrees(srcBlock.getParent());
                    if(mappedSrcNodes2.size()<=mappedSrcNodes.size()+1){
                        removeMappingSubTree(src,dst);
                        if(mappings.isSrcMapped(srcBlock) && mappings.getDsts(srcBlock).contains(dstBlock)){
                            removeFromMapping(srcBlock,dstBlock);
                        }
                        removed = true;
                    }
                }
                if(!removed && dstBlock!=null && dstBlock.getParent().getType().name.equals(Constants.IF_STATEMENT)){
                    var mappedDstNodes2 = getMappedDstTrees(dstBlock.getParent());
                    if(mappedDstNodes2.size()<=mappedDstNodes.size()+1){
                        removeMappingSubTree(src,dst);
                        if(mappings.isSrcMapped(srcBlock) && mappings.getDsts(srcBlock).contains(dstBlock)){
                            removeFromMapping(srcBlock,dstBlock);
                        }
                    }
                }
            }
        }
    }

    protected boolean fixArbitraryMethodCallMapping(Tree src,Tree dst){
        var srcMethodName = TreeUtilFunctions.findChildByType(src,Constants.SIMPLE_NAME);
        var dstMethodName = TreeUtilFunctions.findChildByType(dst,Constants.SIMPLE_NAME);
        if(srcMethodName!=null && dstMethodName!=null){
            if(mappings.isSrcMapped(srcMethodName) && mappings.isDstMapped(dstMethodName) && mappings.getDsts(srcMethodName).contains(dstMethodName)){
                var srcName = srcMethodName.getLabel();
                var dstName = dstMethodName.getLabel();
                var b1 = srcName.startsWith("get");
                var b2 = dstName.startsWith("get");
                var b3 = srcName.startsWith("is") || srcName.startsWith("has");
                var b4 = dstName.startsWith("is") || dstName.startsWith("has");
                var srcSubfix = srcName;
                var dstSubfix = dstName;
                if(b1 || srcName.startsWith("has"))
                    srcSubfix = srcName.substring(3);
                else if(b3)
                    srcSubfix = srcName.substring(2);
                if(b2 || dstName.startsWith("has"))
                    dstSubfix = dstName.substring(3);
                else if(b4)
                    dstSubfix = dstName.substring(2);
                var b5 = srcName.equals("indexOf") && dst.getParent().getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER);
                var b6 = dstName.equals("indexOf") && src.getParent().getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER);
                var b7 = srcName.equals("toUpperCase") && dstName.equals("length");
                var b8 = dstName.equals("toUpperCase") && srcName.equals("length");
                if((b1&&b4 || b2&&b3) && !(srcSubfix.endsWith(dstSubfix) || dstSubfix.endsWith(srcSubfix)) || b5 || b6 || b7 || b8){
                    removeFromMapping(srcMethodName,dstMethodName);
                    removeFromMapping(src,dst);
                    var srcArgs = TreeUtilFunctions.findChildByType(src,Constants.METHOD_INVOCATION_ARGUMENTS);
                    var dstArgs = TreeUtilFunctions.findChildByType(dst,Constants.METHOD_INVOCATION_ARGUMENTS);
                    if(srcArgs!=null && dstArgs!=null){;
                        if(mappings.isSrcMapped(srcArgs) && mappings.isDstMapped(dstArgs) && mappings.getDsts(srcArgs).contains(dstArgs))
                            removeFromMapping(srcArgs,dstArgs);
                    }
                    var srcReceiver = TreeUtilFunctions.findChildByType(src,Constants.METHOD_INVOCATION_RECEIVER);
                    var dstReceiver = TreeUtilFunctions.findChildByType(dst,Constants.METHOD_INVOCATION_RECEIVER);
                    if(srcReceiver!=null && dstReceiver!=null){
                        if(srcReceiver.getChild(0).isLeaf() && dstReceiver.getChild(0).isLeaf()){
                            if(mappings.isSrcMapped(srcReceiver) && mappings.isDstMapped(dstReceiver) && mappings.getDsts(srcReceiver).contains(dstReceiver))
                                removeFromMapping(srcReceiver,dstReceiver);
                            if(mappings.isSrcMapped(srcReceiver.getChild(0)) && mappings.isDstMapped(dstReceiver.getChild(0)) && mappings.getDsts(srcReceiver.getChild(0)).contains(dstReceiver.getChild(0)))
                                removeFromMapping(srcReceiver.getChild(0),dstReceiver.getChild(0));
                        }
                    }
                    return false;
                }
            }
        }

        var result = getSameLeaves(src,dst);
        if(!result.second){
            var b1 = mappings.isSrcMapped(src.getParent()) && mappings.getDsts(src.getParent()).contains(dst.getParent());
            var b2 = mappings.isSrcMapped(src.getParent().getParent()) && mappings.getDsts(src.getParent().getParent()).contains(dst.getParent().getParent());
            if(!(b1 || b2)){
                removeMappingSubTree(src,dst);
                return false;
            }
            else maybeBadMappings.addMapping(src,dst);
        }
        return true;
    }

    protected boolean fixArbitraryClassCreationMapping(Tree src,Tree dst){
        var result = getSameLeaves(src,dst);
        if(!result.second){
            var b1 = mappings.isSrcMapped(src.getParent()) && mappings.getDsts(src.getParent()).contains(dst.getParent());
            var b2 = mappings.isSrcMapped(src.getParent().getParent()) && mappings.getDsts(src.getParent().getParent()).contains(dst.getParent().getParent());
            if(!(b1 || b2)){
                removeMappingSubTree(src,dst);
                return false;
            }
            else maybeBadMappings.addMapping(src,dst);
        }
        return true;
    }

    protected boolean isMappedParent(Tree srcT, Tree dstT){
        if(srcT.getParent()==null || dstT.getParent()==null) return false;
        return mappings.isSrcMapped(srcT.getParent()) && mappings.getDsts(srcT.getParent()).contains(dstT.getParent());
    }

    protected boolean isMappedPrevStmt(Tree srcT, Tree dstT){
        var srcPrev = getPrevStmtSrc(srcT);
        var dstPrev = getPrevStmtDst(dstT);
        if(srcPrev==null || dstPrev==null) return false;
        return mappings.isSrcMapped(srcPrev) && mappings.getDsts(srcPrev).contains(dstPrev);
    }

    protected boolean isMappedNextStmt(Tree srcT, Tree dstT){
        var srcNext = getNextStmtSrc(srcT);
        var dstNext = getNextStmtDst(dstT);
        if(srcNext==null || dstNext==null) return false;
        return mappings.isSrcMapped(srcNext) && mappings.getDsts(srcNext).contains(dstNext);
    }

    protected boolean tryToFix(Tree srcSt,Tree dstSt){
        var leftMatched = false;
        var rightMatched = false;
        if(srcSt.getType().name.equals(Constants.EXPRESSION_STATEMENT)){
            if(!srcSt.getChild(0).hasSameType(dstSt.getChild(0))) return false;
            if(srcSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT)){
                var srcVar = srcSt.getChild(0).getChild(0);
                var dstVar = dstSt.getChild(0).getChild(0);
                if(srcVar.getMetrics().hash==dstVar.getMetrics().hash){
                    if(mappings.isSrcMapped(srcSt) && mappings.isSrcUnique(srcSt)){
                        leftMatched = true;
                    }
                    else if(mappings.isDstMapped(dstSt) && mappings.isDstUnique(dstSt)){
                        rightMatched = true;
                    }
                }
            }
            else if(srcSt.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION)){
                var srcMethodName = TreeUtilFunctions.findChildByType(srcSt.getChild(0),Constants.SIMPLE_NAME).getLabel();
                var dstMethodName = TreeUtilFunctions.findChildByType(dstSt.getChild(0),Constants.SIMPLE_NAME).getLabel();
                if(srcMethodName.equals(dstMethodName)){
                    if(mappings.isSrcMapped(srcSt) && mappings.isSrcUnique(srcSt)){
                        leftMatched = true;
                    }
                    else if(mappings.isDstMapped(dstSt) && mappings.isDstUnique(dstSt)){
                        rightMatched = true;
                    }
                }
            }
            if(leftMatched){
                var mappedDst = mappings.getDsts(srcSt).iterator().next();
                if(srcSt.getMetrics().hash==mappedDst.getMetrics().hash)
                    return false;
                var result1 = getSameLeaves(srcSt,dstSt);
                var result2 = getSameLeaves(srcSt,mappedDst);
                if(compareLeafMapping(result1,result2)
                        || result1.first.size()==result2.first.size() && !mappings.isDstMapped(mappedDst.getParent())){
                    removeMappingSubTree(srcSt,mappedDst);
                    matchSubTree(srcSt,dstSt);
                    if(result2.first.size()>1)
                        matchSubTree(srcSt,mappedDst);
                    return true;
                }
            }
            else if(rightMatched){
                var mappedSrc = mappings.getSrcs(dstSt).iterator().next();
                if(dstSt.getMetrics().hash==mappedSrc.getMetrics().hash)
                    return false;
                var result1 = getSameLeaves(srcSt,dstSt);
                var result2 = getSameLeaves(mappedSrc,dstSt);
                if(compareLeafMapping(result1,result2)
                        || result1.first.size()==result2.first.size() && !mappings.isSrcMapped(mappedSrc.getParent())){
                    removeMappingSubTree(mappedSrc,dstSt);
                    matchSubTree(srcSt,dstSt);
                    if(result2.first.size()>1)
                        matchSubTree(mappedSrc,dstSt);
                    return true;
                }
            }
        }
        else if(srcSt.getType().name.equals(Constants.RETURN_STATEMENT)){
            var srcComment = (String)srcSt.getMetadata("pre-comment");
            var dstComment = (String)dstSt.getMetadata("pre-comment");
            if(srcComment!=null && dstComment!=null && srcComment.equals(dstComment)){
                var srcMethodDecl = TreeUtilFunctions.getParentUntilType(srcSt,Constants.METHOD_DECLARATION);
                var dstMethodDecl = TreeUtilFunctions.getParentUntilType(dstSt,Constants.METHOD_DECLARATION);
                if(srcMethodDecl!=null && dstMethodDecl!=null && mappings.isSrcMapped(srcMethodDecl) && mappings.getDsts(srcMethodDecl).contains(dstMethodDecl)){
                    if(mappings.isSrcMapped(srcSt) && mappings.isSrcUnique(srcSt)){
                        var mappedDst = mappings.getDsts(srcSt).iterator().next();
                        var mappedDstMethodDecl = TreeUtilFunctions.getParentUntilType(mappedDst,Constants.METHOD_DECLARATION);
                        if(mappedDstMethodDecl!=dstMethodDecl){
                            removeMappingSubTree(srcSt,mappedDst);
                            addToMapping(srcSt,dstSt); // ? matchSubTree
                        }
                    }
                    else if(mappings.isDstMapped(dstSt) && mappings.isDstUnique(dstSt)){
                        var mappedSrc = mappings.getSrcs(dstSt).iterator().next();
                        var mappedSrcMethodDecl = TreeUtilFunctions.getParentUntilType(mappedSrc,Constants.METHOD_DECLARATION);
                        if(mappedSrcMethodDecl!=srcMethodDecl){
                            removeMappingSubTree(mappedSrc,dstSt);
                            addToMapping(srcSt,dstSt);
                        }
                    }
                }
            }
        }
        return false;
    }

    protected boolean isMappedPrev(Tree t,boolean isSrc){
        if(!t.getParent().getType().name.equals(Constants.BLOCK)) return false;
        var ts = t.getParent().getChildren();
        int idx = ts.indexOf(t);
        if(idx==0) return false;
        Tree prev = ts.get(idx-1);
        return isSrc?mappings.isSrcMapped(prev):mappings.isDstMapped(prev);
    }

    protected boolean isMappedNext(Tree t,boolean isSrc){
        if(!t.getParent().getType().name.equals(Constants.BLOCK)) return false;
        var ts = t.getParent().getChildren();
        int idx = ts.indexOf(t);
        if(idx==ts.size()-1) return false;
        Tree next = ts.get(idx+1);
        return isSrc?mappings.isSrcMapped(next):mappings.isDstMapped(next);
    }

    protected Tree getPrevSibling(Tree t){
        var ts = t.getParent().getChildren();
        int idx = ts.indexOf(t);
        if(idx==0) return null;
        return ts.get(idx-1);
    }

    protected Tree getNextSibling(Tree t){
        var ts = t.getParent().getChildren();
        int idx = ts.indexOf(t);
        if(idx==ts.size()-1) return null;
        return ts.get(idx+1);
    }

    protected boolean isMappedParent(Tree t,boolean isSrc){
        if(t.getParent().getType().name.equals(Constants.BLOCK)){
            var p = t.getParent();
            if(p.getParent().getType().name.equals(Constants.IF_STATEMENT))
                t = p;
        }
        return isSrc?mappings.isSrcMapped(t.getParent()):mappings.isDstMapped(t.getParent());
    }

    protected boolean tryToFixIdentical(Tree srcSt,Tree dstSt){
        var check=false;
        if(mappings.isSrcMapped(srcSt) && mappings.isSrcUnique(srcSt)){
            var mappedDst = mappings.getDsts(srcSt).iterator().next();
            if(srcSt.getMetrics().hash!=mappedDst.getMetrics().hash){
                check=true;
            }
            else if(!isMappedParent(mappedDst,false)){
                var cnt1 = 0;
                if(isMappedPrev(dstSt,false)) cnt1++;
                if(isMappedNext(dstSt,false)) cnt1++;
                var cnt2 = 0;
                if(isMappedPrev(mappedDst,false)) cnt2++;
                if(isMappedNext(mappedDst,false)) cnt2++;
                if(cnt1>cnt2) check=true;
            }
            else{
                if(srcSt.getMetadata("pre-comment")!=null && dstSt.getMetadata("pre-comment")!=null){
                    var srcPreComment = (String)srcSt.getMetadata("pre-comment");
                    var dstPreComment = (String)dstSt.getMetadata("pre-comment");
                    if(srcPreComment.equals(dstPreComment) && (mappedDst.getMetadata("pre-comment")==null || !mappedDst.getMetadata("pre-comment").equals(dstPreComment))){
                        check=true;
                    }
                }
                if(srcSt.getMetadata("inline-comment")!=null && dstSt.getMetadata("inline-comment")!=null){
                    var srcInlineComment = (String)srcSt.getMetadata("inline-comment");
                    var dstInlineComment = (String)dstSt.getMetadata("inline-comment");
                    if(srcInlineComment.equals(dstInlineComment) && (mappedDst.getMetadata("inline-comment")==null || !mappedDst.getMetadata("inline-comment").equals(dstInlineComment))){
                        check=true;
                    }
                }
            }
            if(check){
                removeMappingSubTree(srcSt,mappedDst);
                matchSubTree(srcSt,dstSt);
            }
        }
        else if(mappings.isDstMapped(dstSt) && mappings.isDstUnique(dstSt)){
            var mappedSrc = mappings.getSrcs(dstSt).iterator().next();
            if(dstSt.getMetrics().hash!=mappedSrc.getMetrics().hash){
                check=true;
            }
            else if(!isMappedParent(mappedSrc,true)){
                var cnt1 = 0;
                if(isMappedPrev(srcSt,true)) cnt1++;
                if(isMappedNext(srcSt,true)) cnt1++;
                var cnt2 = 0;
                if(isMappedPrev(mappedSrc,true)) cnt2++;
                if(isMappedNext(mappedSrc,true)) cnt2++;
                if(cnt1>cnt2) check=true;
            }
            else{
                if(dstSt.getMetadata("pre-comment")!=null && srcSt.getMetadata("pre-comment")!=null){
                    var srcPreComment = (String)srcSt.getMetadata("pre-comment");
                    var dstPreComment = (String)dstSt.getMetadata("pre-comment");
                    if(srcPreComment.equals(dstPreComment) && (mappedSrc.getMetadata("pre-comment")==null || !mappedSrc.getMetadata("pre-comment").equals(dstPreComment))){
                        check=true;
                    }
                }
                if(dstSt.getMetadata("inline-comment")!=null && srcSt.getMetadata("inline-comment")!=null){
                    var srcInlineComment = (String)srcSt.getMetadata("inline-comment");
                    var dstInlineComment = (String)dstSt.getMetadata("inline-comment");
                    if(srcInlineComment.equals(dstInlineComment) && (mappedSrc.getMetadata("inline-comment")==null || !mappedSrc.getMetadata("inline-comment").equals(dstInlineComment))){
                        check=true;
                    }
                }
            }
            if(check){
                removeMappingSubTree(mappedSrc,dstSt);
                matchSubTree(srcSt,dstSt);
            }
        }
        return check;
    }

    protected boolean tryToFix2(Tree srcSt,Tree dstSt){
        if(!(mappings.isSrcUnique(srcSt) && mappings.isDstUnique(dstSt))) return false;
        var dstMappedSt = mappings.getDsts(srcSt).iterator().next();
        var srcMappedSt = mappings.getSrcs(dstSt).iterator().next();
        if(srcSt.getType().name.equals(Constants.EXPRESSION_STATEMENT) && dstMappedSt.hasSameType(srcSt) && srcMappedSt.hasSameType(srcSt)){
            var srcStCType = srcSt.getChild(0).getType().name;
            var dstStCType = dstSt.getChild(0).getType().name;
            if(!srcStCType.equals(dstStCType)) return false;
            if(srcStCType.equals(Constants.METHOD_INVOCATION)){
                var result1 = getSameLeaves(srcSt,dstSt);
                var result2 = getSameLeaves(srcSt,dstMappedSt);
                var result3 = getSameLeaves(srcMappedSt,dstSt);
                if(compareLeafMapping(result1,result2) && compareLeafMapping(result1,result3)){
                    removeMappingSubTree(srcSt,dstMappedSt);
                    removeMappingSubTree(srcMappedSt,dstSt);
                    matchSubTree(srcSt,dstSt);
                    if(result2.first.size()>1)
                        matchSubTree(srcSt,dstMappedSt);
                    if(result3.first.size()>1)
                        matchSubTree(srcMappedSt,dstSt);
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean tryToFixNext(Tree srcSt,Tree dstSt){
        if(srcSt.getMetrics().hash==dstSt.getMetrics().hash){
            if(mappings.isSrcMapped(srcSt) && mappings.isSrcUnique(srcSt)){
                var mappedDst = mappings.getDsts(srcSt).iterator().next();
                removeMappingSubTree(srcSt,mappedDst);
                matchSubTree(srcSt,dstSt);
            }
            else if(mappings.isDstMapped(dstSt) && mappings.isDstUnique(dstSt)){
                var mappedSrc = mappings.getSrcs(dstSt).iterator().next();
                removeMappingSubTree(mappedSrc,dstSt);
                matchSubTree(srcSt,dstSt);
            }
            return true;
        }
        return false;
    }

    protected boolean tryToFixNext2(Tree srcSt,Tree dstSt){
        if(!(mappings.isSrcUnique(srcSt) && mappings.isDstUnique(dstSt))) return false;
        if(srcSt.getMetrics().hash==dstSt.getMetrics().hash){
            var dstMappedSt = mappings.getDsts(srcSt).iterator().next();
            var srcMappedSt = mappings.getSrcs(dstSt).iterator().next();
            removeMappingSubTree(srcSt,dstMappedSt);
            removeMappingSubTree(srcMappedSt,dstSt);
            matchSubTree(srcSt,dstSt);
            return true;
        }
        return false;
    }

    protected List<Tree> flattenCond(Tree cond_){
        List<Tree> conds = new ArrayList<>();
        Queue<Tree> curs = new LinkedList<>();
        curs.add(cond_);
        while(!curs.isEmpty()){
            var cond = curs.poll();
            if(cond.getType().name.equals(Constants.INFIX_EXPRESSION) && isLogicalOperator(cond.getChild(1))){
                curs.add(cond.getChild(0));
                for(int i=2;i<cond.getChildren().size();i++) {
                    curs.add(cond.getChild(i));
                }
            }
            else if(cond.getType().name.equals(Constants.PREFIX_EXPRESSION) && isLogicalOperator(cond.getChild(0))){
                curs.add(cond.getChild(1));
            }
            else if(cond.getType().name.equals("ParenthesizedExpression")){
                curs.add(cond.getChild(0));
            }
            else
                conds.add(cond);
        }
        return conds;
    }
    // could be further optimized update stmt state when inner node mapped
    protected boolean hasInnerMappedSrc(Tree src){
        return getMappedSrcTrees(src).size()>0;
    }

    protected boolean hasInnerMappedDst(Tree dst){
        return getMappedDstTrees(dst).size()>0;
    }

    protected boolean checkNearbyBetter(Tree srcStmt,Tree dstStmt,List<Tree> newStmts){
        if(srcStmt.getMetrics().hash==dstStmt.getMetrics().hash || keepMappingStmts.contains(srcStmt))
            return false;

        var srcPrevStmt = getPrevStmtSrc(srcStmt);
        if(srcPrevStmt!=null && !mappings.isSrcMapped(srcPrevStmt) && !hasInnerMappedSrc(srcPrevStmt) && srcPrevStmt.hasSameType(dstStmt)){
            var result1 = getSameLeaves(srcPrevStmt,dstStmt);
            var result2 = getSameLeaves(srcStmt,dstStmt);
            if(compareLeafMapping(result1,result2)){
                removeMappingSubTree(srcStmt,dstStmt);
                matchSubTree(srcPrevStmt,dstStmt);
                newStmts.add(srcPrevStmt);
                return true;
            }
            else if(srcPrevStmt.hasSameType(srcStmt)){
                if(srcPrevStmt.getType().name.equals(Constants.RETURN_STATEMENT)){
                    if(srcPrevStmt.getChildren().size()>0 &&
                            srcPrevStmt.getChild(0).getType().name.equals(Constants.BOOLEAN_LITERAL) &&
                            srcPrevStmt.getChild(0).getLabel().equals("true")){
                        var srcParent =srcPrevStmt.getParent();
                        if(srcParent.getType().name.equals(Constants.BLOCK)){
                            srcParent=srcParent.getParent();
                        }
                        if(srcParent.getType().name.equals(Constants.IF_STATEMENT)){
                            var cond=srcParent.getChild(0);
                            if(dstStmt.getChildren().size()>0){
                                var ret=dstStmt.getChild(0);
                                if(cond.getMetrics().hash==ret.getMetrics().hash){
                                    removeMappingSubTree(srcStmt,dstStmt);
                                    matchSubTree(cond,ret);
                                    addToMapping(srcPrevStmt,dstStmt);
                                    keepMappingStmts.add(srcPrevStmt);
                                    newStmts.add(srcPrevStmt);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        var srcNextStmt = getNextStmtSrc(srcStmt);
        if(srcNextStmt!=null && !mappings.isSrcMapped(srcNextStmt) && !hasInnerMappedSrc(srcNextStmt) && srcNextStmt.hasSameType(dstStmt)){
            var result1 = getSameLeaves(srcNextStmt,dstStmt);
            var result2 = getSameLeaves(srcStmt,dstStmt);
            if(compareLeafMapping(result1,result2)){
                removeMappingSubTree(srcStmt,dstStmt);
                matchSubTree(srcNextStmt,dstStmt);
                newStmts.add(srcNextStmt);
                return true;
            }
        }

        var dstPrevStmt = getPrevStmtDst(dstStmt);
        if(dstPrevStmt!=null && !mappings.isDstMapped(dstPrevStmt) && !hasInnerMappedDst(dstPrevStmt) && dstPrevStmt.hasSameType(srcStmt)){
            var result1 = getSameLeaves(srcStmt,dstPrevStmt);
            var result2 = getSameLeaves(srcStmt,dstStmt);
            if(compareLeafMapping(result1,result2)){
                removeMappingSubTree(srcStmt,dstStmt);
                matchSubTree(srcStmt,dstPrevStmt);
                newStmts.add(srcStmt);
                return true;
            }
            else if(dstPrevStmt.hasSameType(dstStmt)){
                if(dstPrevStmt.getType().name.equals(Constants.RETURN_STATEMENT)){
                    if(dstPrevStmt.getChildren().size()>0 &&
                            dstPrevStmt.getChild(0).getType().name.equals(Constants.BOOLEAN_LITERAL) &&
                            dstPrevStmt.getChild(0).getLabel().equals("true")){
                        var dstParent =dstPrevStmt.getParent();
                        if(dstParent.getType().name.equals(Constants.BLOCK)){
                            dstParent=dstParent.getParent();
                        }
                        if(dstParent.getType().name.equals(Constants.IF_STATEMENT)){
                            var cond=dstParent.getChild(0);
                            if(srcStmt.getChildren().size()>0){
                                var ret=srcStmt.getChild(0);
                                if(cond.getMetrics().hash==ret.getMetrics().hash){
                                    removeMappingSubTree(srcStmt,dstStmt);
                                    matchSubTree(ret,cond);
                                    addToMapping(srcStmt,dstPrevStmt);
                                    keepMappingStmts.add(srcStmt);
                                    newStmts.add(srcStmt);
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        var dstNextStmt = getNextStmtDst(dstStmt);
        if(dstNextStmt!=null && !mappings.isDstMapped(dstNextStmt) && !hasInnerMappedDst(dstNextStmt) && dstNextStmt.hasSameType(srcStmt)){
            var result1 = getSameLeaves(srcStmt,dstNextStmt);
            var result2 = getSameLeaves(srcStmt,dstStmt);
            if(compareLeafMapping(result1,result2)){
                removeMappingSubTree(srcStmt,dstStmt);
                matchSubTree(srcStmt,dstNextStmt);
                newStmts.add(srcStmt);
                return true;
            }

        }

        return false;
    }

    protected void checkNearbyWrongMatched(Tree srcStmt,Tree dstStmt,List<Tree> newStmts){
        var srcPrevStmt = getPrevStmtSrc(srcStmt);
        var srcNextStmt = getNextStmtSrc(srcStmt);
        var dstPrevStmt = getPrevStmtDst(dstStmt);
        var dstNextStmt = getNextStmtDst(dstStmt);

        var mapPrev = false;
        var mapNext = false;

        if(srcPrevStmt!=null && dstPrevStmt!=null) {
            if(mappings.isSrcMapped(srcPrevStmt) && mappings.getDsts(srcPrevStmt).contains(dstPrevStmt)){
                mapPrev = true;
            }
            else if(srcPrevStmt.hasSameType(dstPrevStmt)){
                if(mappings.isSrcMapped(srcPrevStmt)^mappings.isDstMapped(dstPrevStmt)){
                    mapPrev = srcPrevStmt.getMetrics().hash==dstPrevStmt.getMetrics().hash
                            ?tryToFixIdentical(srcPrevStmt,dstPrevStmt)
                            :tryToFix(srcPrevStmt,dstPrevStmt);
                }
                else if(mappings.isSrcMapped(srcPrevStmt) && mappings.isDstMapped(dstPrevStmt)){
                    mapPrev = tryToFix2(srcPrevStmt,dstPrevStmt);
                }
                if(mapPrev){
                    newStmts.add(srcPrevStmt);
                }
            }
        }
        if(srcNextStmt!=null && dstNextStmt!=null){
            if(mappings.isSrcMapped(srcNextStmt) && mappings.getDsts(srcNextStmt).contains(dstNextStmt)){
                mapNext = true;
            }
            else if(srcNextStmt.hasSameType(dstNextStmt)){
                if(mappings.isSrcMapped(srcNextStmt)^mappings.isDstMapped(dstNextStmt)){
                    if(srcStmt.getType().name.equals(Constants.SWITCH_CASE)){
                        mapNext = tryToFixNext(srcNextStmt,dstNextStmt);
                    }
                    else{
                        mapNext = srcNextStmt.getMetrics().hash==dstNextStmt.getMetrics().hash
                                ?tryToFixIdentical(srcNextStmt,dstNextStmt)
                                :tryToFix(srcNextStmt,dstNextStmt);
                    }
                }
                else if(mappings.isSrcMapped(srcNextStmt) && mappings.isDstMapped(dstNextStmt)) {
                    if(srcStmt.getType().name.equals(Constants.SWITCH_CASE)){
                        mapNext=tryToFixNext2(srcNextStmt,dstNextStmt);
                    }
                    else{
                        mapNext = tryToFix2(srcNextStmt,dstNextStmt);
                    }
                }
                if(mapNext){
                    newStmts.add(srcNextStmt);
                }
            }
        }
        if(mapPrev || mapNext)
            return;

        if(srcPrevStmt!=null && dstNextStmt!=null){
            if(srcPrevStmt.hasSameType(dstNextStmt) && mappings.isSrcMapped(srcPrevStmt)^mappings.isDstMapped(dstNextStmt)){
                var res = srcPrevStmt.getMetrics().hash==dstNextStmt.getMetrics().hash
                        ?tryToFixIdentical(srcPrevStmt,dstNextStmt)
                        :tryToFix(srcPrevStmt,dstNextStmt);
                if(res){
                    newStmts.add(srcPrevStmt);
                    return;
                }
            }
        }
        if(srcNextStmt!=null && dstPrevStmt!=null){
            if(srcNextStmt.hasSameType(dstPrevStmt) && mappings.isSrcMapped(srcNextStmt)^mappings.isDstMapped(dstPrevStmt)){
                var res = srcNextStmt.getMetrics().hash==dstPrevStmt.getMetrics().hash
                        ?tryToFixIdentical(srcNextStmt,dstPrevStmt)
                        :tryToFix(srcNextStmt,dstPrevStmt);
                if(res){
                    newStmts.add(srcNextStmt);
                }
            }
        }
    }

    protected void checkIdentical(Tree srcStmt,Tree dstStmt,List<Tree> newStmts){
        if(srcStmt.getMetrics().hash!=dstStmt.getMetrics().hash || keepMappingStmts.contains(srcStmt))
            return;
        var srcNextStmt = getNextStmtSrc(srcStmt);
        var dstPrevStmt = getPrevStmtDst(dstStmt);
        if(srcNextStmt!=null && dstPrevStmt!=null
            && srcNextStmt.getMetrics().hash==dstPrevStmt.getMetrics().hash
            && mappings.isSrcMapped(srcNextStmt) && mappings.getDsts(srcNextStmt).contains(dstPrevStmt)
            && srcStmt.getParent()==srcNextStmt.getParent() && dstStmt.getParent()==dstPrevStmt.getParent()){
            // ab<->ba | find ab in dst or ba in src
            for(var candidate: dstIdenticalStmts.get(srcStmt.getMetrics().hash)){
                if(candidate==dstStmt || mappings.isDstMapped(candidate)) continue;
                var candidateNext = getNextStmtDst(candidate);
                if(candidateNext!=null && !mappings.isDstMapped(candidateNext) && candidateNext.getMetrics().hash==srcNextStmt.getMetrics().hash){
                    removeMappingSubTree(srcStmt,dstStmt);
                    removeMappingSubTree(srcNextStmt,dstPrevStmt);
                    matchSubTree(srcStmt,candidate);
                    matchSubTree(srcNextStmt,candidateNext);
                    newStmts.add(srcStmt);
                    newStmts.add(srcNextStmt);
                    keepMappingStmts.add(srcStmt);
                    keepMappingStmts.add(srcNextStmt);
                    return;
                }
            }
            for(var candidate: srcIdenticalStmts.get(dstStmt.getMetrics().hash)){
                if(candidate==srcStmt || mappings.isSrcMapped(candidate)) continue;
                var candidatePrev = getPrevStmtSrc(candidate);
                if(candidatePrev!=null && !mappings.isSrcMapped(candidatePrev) && candidatePrev.getMetrics().hash==dstPrevStmt.getMetrics().hash){
                    removeMappingSubTree(srcStmt,dstStmt);
                    removeMappingSubTree(srcNextStmt,dstPrevStmt);
                    matchSubTree(candidatePrev,dstPrevStmt);
                    matchSubTree(candidate,dstStmt);
                    newStmts.add(candidatePrev);
                    newStmts.add(candidate);
                    keepMappingStmts.add(candidatePrev);
                    keepMappingStmts.add(candidate);
                    return;
                }
            }
        }

        var srcIdenticals = srcIdenticalStmts.get(srcStmt.getMetrics().hash);
        var dstIdenticals = dstIdenticalStmts.get(dstStmt.getMetrics().hash);

        if(srcIdenticals==null || dstIdenticals==null) return;

        if(srcIdenticals.size()==1 && dstIdenticals.size()>1){
            var dstPrev = getPrevSibling(dstStmt);
            var dstNext = getNextSibling(dstStmt);
            if(dstPrev!=null && dstNext!=null && !mappings.isDstMapped(dstPrev) && !mappings.isDstMapped(dstNext)) {
                var candidates = new ArrayList<Tree>();
                for (var dstIdentical : dstIdenticals) {
                    if (dstIdentical == dstStmt) continue;
                    var dstIdenticalPrev = getPrevSibling(dstIdentical);
                    var dstIdenticalNext = getNextSibling(dstIdentical);
                    if (dstIdenticalPrev != null && dstIdenticalNext != null
                            && (mappings.isDstMapped(dstIdenticalPrev) || mappings.isDstMapped(dstIdenticalNext))) {
                        candidates.add(dstIdentical);
                    }
                }
                if (candidates.size() == 1) {
                    removeMappingSubTree(srcStmt, dstStmt);
                    matchSubTree(srcStmt, candidates.get(0));
                    newStmts.add(srcStmt);
                    keepMappingStmts.add(srcStmt);
                }
            }
        }
        else if(dstIdenticals.size()==1 && srcIdenticals.size()>1){
            var srcPrev = getPrevSibling(srcStmt);
            var srcNext = getNextSibling(srcStmt);
            if(srcPrev!=null && srcNext!=null && !mappings.isSrcMapped(srcPrev) && !mappings.isSrcMapped(srcNext)){
                var candidates = new ArrayList<Tree>();
                for (var srcIdentical : srcIdenticals) {
                    if (srcIdentical == srcStmt) continue;
                    var srcIdenticalPrev = getPrevSibling(srcIdentical);
                    var srcIdenticalNext = getNextSibling(srcIdentical);
                    if (srcIdenticalPrev!=null &&  srcIdenticalNext !=null
                        && (mappings.isSrcMapped(srcIdenticalPrev) || mappings.isSrcMapped(srcIdenticalNext))) {
                        candidates.add(srcIdentical);
                    }
                }
                if (candidates.size() == 1) {
                    removeMappingSubTree(srcStmt,dstStmt);
                    matchSubTree(candidates.get(0), dstStmt);
                    newStmts.add(candidates.get(0));
                    keepMappingStmts.add(candidates.get(0));
                }
            }
        }

        else if(srcIdenticals.size()>1 && dstIdenticals.size()>1){
            var h = srcStmt.getMetrics().hash;
            if(visitedIdentical.contains(h))
                return;
            visitedIdentical.add(h);

            List<Tree> srcLeaves = new ArrayList<>();
            List<Tree> dstLeaves = new ArrayList<>();

            for(Tree st : srcIdenticals){
                if(mappings.isSrcMapped(st) && mappings.isSrcUnique(st)){
                    var mappedSt = mappings.getDsts(st).iterator().next();
                    if(mappedSt.getMetrics().hash==h){
                        srcLeaves.add(st);
                        dstLeaves.add(mappedSt);
                    }
                }
            }

            if(srcLeaves.size()>1){
                srcLeaves.sort(Comparator.comparingInt(Tree::getPos));
                dstLeaves.sort(Comparator.comparingInt(Tree::getPos));
                for(int i=0;i<srcLeaves.size();i++){
                    var srcSt = srcLeaves.get(i);
                    var dstSt = dstLeaves.get(i);
                    var dstMappedSt = mappings.getDsts(srcSt).iterator().next();
                    var srcMappedSt = mappings.getSrcs(dstSt).iterator().next();
                    if(dstSt!=dstMappedSt && srcLeaves.contains(srcMappedSt) && dstLeaves.contains(dstMappedSt)){
                        if(isMappedParent(srcSt,dstMappedSt) && !isMappedParent(srcSt,dstSt)) {
                            continue;
                        }
                        if(isMappedPrevStmt(srcSt,dstSt) && !isMappedPrevStmt(srcSt,dstMappedSt)
                                || isMappedNextStmt(srcSt,dstSt) && !isMappedNextStmt(srcSt,dstMappedSt)
                        ){
                            removeFromMappingRecursively(srcSt,dstMappedSt);
                            removeFromMappingRecursively(srcMappedSt,dstSt);
                            addToMappingRecursively(srcSt,dstSt);
                            addToMappingRecursively(srcMappedSt,dstMappedSt);
                            if(!newStmts.contains(srcSt))
                                newStmts.add(srcSt);
                            if(!newStmts.contains(srcMappedSt))
                                newStmts.add(srcMappedSt);
                        }
                    }
                }
            }
        }
    }

    protected void checkInnerStmtNearby(Tree src,Tree dst){
        var renameVars = new ArrayList<Tree>();
        for(var srcchild:src.preOrder()){
            if(!(mappings.isSrcMapped(srcchild) && mappings.isSrcUnique(srcchild)))
                return;
            if(srcchild.isLeaf()){
                var mappedDst = mappings.getDsts(srcchild).iterator().next();
                if(srcchild.getMetrics().hash!=mappedDst.getMetrics().hash){
                    if(!srcchild.getType().name.equals(Constants.SIMPLE_NAME))
                        return;
                    renameVars.add(srcchild);
                }
            }
        }
        if(renameVars.size()==1){
            var srcVar = renameVars.get(0);
            var dstVar = mappings.getDsts(srcVar).iterator().next();
            var srcPrev = getPrevStmtSrc(src);
            var dstPrev = getPrevStmtDst(dst);
            if(srcPrev!=null && srcPrev.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT)){
                var srcDecl = TreeUtilFunctions.findChildByType(srcPrev,Constants.VARIABLE_DECLARATION_FRAGMENT);
                var declVar = getDeclName(srcDecl);
                if(declVar!=null && declVar.getMetrics().hash==srcVar.getMetrics().hash){
                    var declInit = getDeclInit(srcDecl);
                    if(declInit!=null){
                        var candidates = declInit.getType().name.equals(Constants.CONDITIONAL_EXPRESSION)?
                                declInit.getChildren().stream().filter(e -> !mappings.isSrcMapped(e) && e.getMetrics().hash==dstVar.getMetrics().hash).toList():
                                TreeUtils.preOrder(declInit).stream().filter(e -> !mappings.isSrcMapped(e) && e.getMetrics().hash==dstVar.getMetrics().hash).toList();
                        if(candidates.size()==1){
                            removeFromMapping(srcVar,dstVar);
                            addToMapping(candidates.get(0),dstVar);
                        }
                    }
                }
            }
            if(dstPrev!=null && dstPrev.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT)){
                var dstDecl = TreeUtilFunctions.findChildByType(dstPrev,Constants.VARIABLE_DECLARATION_FRAGMENT);
                var declVar = getDeclName(dstDecl);
                if(declVar!=null && declVar.getMetrics().hash==dstVar.getMetrics().hash){
                    var declInit = getDeclInit(dstDecl);
                    if(declInit!=null){
                        var candidates = declInit.getType().name.equals(Constants.CONDITIONAL_EXPRESSION)?
                                declInit.getChildren().stream().filter(e -> !mappings.isDstMapped(e) && e.getMetrics().hash==srcVar.getMetrics().hash).toList():
                                TreeUtils.preOrder(declInit).stream().filter(e -> !mappings.isDstMapped(e) && e.getMetrics().hash==srcVar.getMetrics().hash).toList();
                        if(candidates.size()==1){
                            removeFromMapping(srcVar,dstVar);
                            addToMapping(srcVar,candidates.get(0));
                        }
                    }
                }
            }
        }
    }

    protected List<Tree> fixByNearbyContextRound(List<Tree> stmts){
        Collections.sort(stmts,Comparator.comparingInt(Tree::getPos));
        List<Tree> newStmts = new ArrayList<>();
        for (var src : stmts) {
            if(mappings.isSrcMapped(src) && mappings.isSrcUnique(src)){
                var dst = mappings.getDsts(src).iterator().next();
                if(checkNearbyBetter(src,dst,newStmts))
                    continue;
                checkNearbyWrongMatched(src,dst,newStmts);
                checkIdentical(src,dst,newStmts);
                if(src.getMetrics().hash!=dst.getMetrics().hash && src.getMetrics().structureHash==dst.getMetrics().structureHash){
                    checkInnerStmtNearby(src,dst);
                }
            }
        }
        return newStmts;
    }

    protected boolean tryToMatch(Tree srcSt,Tree dstSt){
        if(!srcSt.hasSameType(dstSt)) {
            var b1 = srcSt.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT);
            var b2 = dstSt.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT);
            var b3 = srcSt.getType().name.equals(Constants.EXPRESSION_STATEMENT) && srcSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT);
            var b4 = dstSt.getType().name.equals(Constants.EXPRESSION_STATEMENT) && dstSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT);
            if(b1&&b4){
                var srcInit = getDeclInit(srcSt.getChild(1));
                var dstAssign = dstSt.getChild(0).getChild(2);
                if(srcInit!=null && dstAssign!=null && srcInit.getMetrics().hash==dstAssign.getMetrics().hash){
                    matchSubTree(srcInit,dstAssign);
                }
            }
            else if(b2&&b3){
                var dstInit = getDeclInit(dstSt.getChild(1));
                var srcAssign = srcSt.getChild(0).getChild(2);
                if(dstInit!=null && srcAssign!=null && dstInit.getMetrics().hash==srcAssign.getMetrics().hash){
                    matchSubTree(srcAssign,dstInit);
                }
            }
            return false;
        }
        if(!onlyInnerMapping(srcSt,dstSt)){
            if(srcSt.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT)){
                var srcVD = TreeUtilFunctions.findChildByType(srcSt,Constants.VARIABLE_DECLARATION_FRAGMENT);
                var dstVD = TreeUtilFunctions.findChildByType(dstSt,Constants.VARIABLE_DECLARATION_FRAGMENT);
                if(srcVD!=null && dstVD!=null && srcVD.getChild(0).getMetrics().hash==dstVD.getChild(0).getMetrics().hash
                        && !mappings.isSrcMapped(srcVD) && !mappings.isDstMapped(dstVD)){
                    matchSubTree(srcSt,dstSt);
                    return true;
                }
            }
            return false;
        }
        if(srcSt.getMetrics().hash==dstSt.getMetrics().hash){
            matchSubTree(srcSt,dstSt);
            return true;
        }
        if(srcSt.getType().name.equals(Constants.EXPRESSION_STATEMENT)){
            if(!srcSt.getChild(0).hasSameType(dstSt.getChild(0))) {
                return false;
            }
            if(srcSt.getChild(0).getType().name.equals(Constants.ASSIGNMENT)){
                var srcVar = srcSt.getChild(0).getChild(0);
                var dstVar = dstSt.getChild(0).getChild(0);
                if(srcVar.getMetrics().hash==dstVar.getMetrics().hash){
                    matchSubTree(srcSt,dstSt);
                    return true;
                }
            }
            else if(srcSt.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION)){
                var srcCall = srcSt.getChild(0);
                var dstCall = dstSt.getChild(0);
                int count=0;
                var srcReceiver = TreeUtilFunctions.findChildByType(srcCall,Constants.METHOD_INVOCATION_RECEIVER);
                var dstReceiver = TreeUtilFunctions.findChildByType(dstCall,Constants.METHOD_INVOCATION_RECEIVER);
                if(srcReceiver!=null && dstReceiver!=null && srcReceiver.getMetrics().hash==dstReceiver.getMetrics().hash){
                    count++;
                }
                var srcName = TreeUtilFunctions.findChildByType(srcCall,Constants.SIMPLE_NAME);
                var dstName = TreeUtilFunctions.findChildByType(dstCall,Constants.SIMPLE_NAME);
                if(srcName!=null && dstName!=null && srcName.getLabel().equals(dstName.getLabel())){
                    count++;
                }
                if(count==2){
                    matchSubTree(srcSt,dstSt);
                    return true;
                }
            }
        }
        else if(srcSt.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT)){
            var srcVD = TreeUtilFunctions.findChildByType(srcSt,Constants.VARIABLE_DECLARATION_FRAGMENT);
            var dstVD = TreeUtilFunctions.findChildByType(dstSt,Constants.VARIABLE_DECLARATION_FRAGMENT);
            if(srcVD!=null && dstVD!=null && srcVD.getChild(0).getMetrics().hash==dstVD.getChild(0).getMetrics().hash){
                var srcType = getDeclType(srcVD);
                var dstType = getDeclType(dstVD);
                if(srcType!=null && dstType!=null){
                    if(srcType.getMetrics().hash==dstType.getMetrics().hash
                            || srcType.hasSameType(dstType) && !srcType.getType().name.equals(Constants.SIMPLE_TYPE)){
                        matchSubTree(srcSt,dstSt);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean tryToMatchCaseNext(Tree srcSt,Tree dstSt){
        if(srcSt.getType().name.equals(Constants.RETURN_STATEMENT)){
            if(!srcSt.hasSameType(dstSt)) return false;
            if(!onlyInnerMapping(srcSt,dstSt)) return false;
            matchSubTree(srcSt,dstSt);
            return true;
        }
        return false;
    }

    protected List<Tree> matchByNearbyContextRound(List<Tree> stmts){
        Collections.sort(stmts,Comparator.comparingInt(Tree::getPos));
        List<Tree> newStmts = new ArrayList<>();
        for (var src : stmts) {
            if(mappings.isSrcMapped(src) && mappings.isSrcUnique(src)){
                var dst = mappings.getDsts(src).iterator().next();
                var srcPrevStmt = getPrevStmtSrc(src);
                var dstPrevStmt = getPrevStmtDst(dst);
                if (srcPrevStmt!=null && dstPrevStmt!=null &&
                        !mappings.isSrcMapped(srcPrevStmt) && !mappings.isDstMapped(dstPrevStmt)) {
                    if (tryToMatch(srcPrevStmt, dstPrevStmt)) {
                        newStmts.add(srcPrevStmt);
                    }
                }

                var srcNextStmt = getNextStmtSrc(src);
                var dstNextStmt = getNextStmtDst(dst);
                if (srcNextStmt!=null && dstNextStmt!=null &&
                        !mappings.isSrcMapped(srcNextStmt) && !mappings.isDstMapped(dstNextStmt)) {
                    if(src.getType().name.equals(Constants.SWITCH_CASE)){
                        if(tryToMatchCaseNext(srcNextStmt,dstNextStmt))
                            newStmts.add(srcNextStmt);
                    }
                    else if(tryToMatch(srcNextStmt, dstNextStmt))
                        newStmts.add(srcNextStmt);
                }
            }
        }
        return newStmts;
    }

    protected boolean isRename(String src,String dst){
        return renameMap.containsKey(src) && renameMap.get(src).equals(dst);
    }

    protected boolean isRename(Tree src,Tree dst){
        var srcName = TreeUtilFunctions.findChildByType(src,Constants.SIMPLE_NAME);
        var dstName = TreeUtilFunctions.findChildByType(dst,Constants.SIMPLE_NAME);
        return srcName!=null && dstName!=null && isRename(srcName.getLabel(),dstName.getLabel());
    }

    protected int sameInnerCall(Tree srcCall,Tree dstCall){
        int count = 0;
        var srcReceiver = TreeUtilFunctions.findChildByType(srcCall,Constants.METHOD_INVOCATION_RECEIVER);
        var dstReceiver = TreeUtilFunctions.findChildByType(dstCall,Constants.METHOD_INVOCATION_RECEIVER);
        if(srcReceiver!=null && dstReceiver!=null){
            if(srcReceiver.getMetrics().hash==dstReceiver.getMetrics().hash)
                count++;
            else if(srcReceiver.getChild(0).getType().name.equals(Constants.STRING_LITERAL) && dstReceiver.getChild(0).isLeaf() || dstReceiver.getChild(0).getType().name.equals(Constants.STRING_LITERAL) && srcReceiver.getChild(0).isLeaf()){
                var srcStr = srcReceiver.getChild(0).getLabel();
                var dstStr = dstReceiver.getChild(0).getLabel();
                if(srcReceiver.getChild(0).getType().name.equals(Constants.STRING_LITERAL))
                    srcStr = srcStr.substring(1,srcStr.length()-1);
                if(dstReceiver.getChild(0).getType().name.equals(Constants.STRING_LITERAL))
                    dstStr = dstStr.substring(1,dstStr.length()-1);
                if(srcStr.endsWith(dstStr) || dstStr.endsWith(srcStr)){
                    count++;
                }
            }
        }
        var srcName = TreeUtilFunctions.findChildByType(srcCall,Constants.SIMPLE_NAME);
        var dstName = TreeUtilFunctions.findChildByType(dstCall,Constants.SIMPLE_NAME);
        if(srcName!=null && dstName!=null && (srcName.getLabel().equals(dstName.getLabel()) || isRename(srcName.getLabel(),dstName.getLabel()))){
            count++;
        }
        var srcArgs = TreeUtilFunctions.findChildByType(srcCall,Constants.METHOD_INVOCATION_ARGUMENTS);
        var dstArgs = TreeUtilFunctions.findChildByType(dstCall,Constants.METHOD_INVOCATION_ARGUMENTS);
        if(srcArgs!=null && dstArgs!=null){
            Set<Tree> vis = new HashSet<>();
            var srcArgList = srcArgs.getChildren();
            var dstArgList = dstArgs.getChildren();
            for(var srcArg: srcArgList){
                for(var dstArg: dstArgList){
                    if(!vis.contains(dstArg) && srcArg.getMetrics().hash==dstArg.getMetrics().hash){
                        vis.add(dstArg);
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    protected int getInnerCallSize(Tree call){
        int count = 0;
        var receiver = TreeUtilFunctions.findChildByType(call,Constants.METHOD_INVOCATION_RECEIVER);
        if(receiver!=null){
            count++;
        }
        var name = TreeUtilFunctions.findChildByType(call,Constants.SIMPLE_NAME);
        if(name!=null){
            count++;
        }
        var args = TreeUtilFunctions.findChildByType(call,Constants.METHOD_INVOCATION_ARGUMENTS);
        if(args!=null){
            count+=args.getChildren().size();
        }
        return count;
    }

    protected int commonPrefixLen(Tree a,Tree b){
        if(a==null || b==null) return 0;
        if(!a.hasSameType(b)) return 0;
        String strA = null;
        String strB = null;
        if(a.isLeaf()&&b.isLeaf()){
            strA = a.getLabel();
            strB = b.getLabel();
        }
        else if(a.getType().name.equals(Constants.METHOD_INVOCATION)){
            var srcName = TreeUtilFunctions.findChildByType(a,Constants.SIMPLE_NAME);
            var dstName = TreeUtilFunctions.findChildByType(b,Constants.SIMPLE_NAME);
            if(srcName!=null && dstName!=null){
                strA = srcName.getLabel();
                strB = dstName.getLabel();
            }
        }
        if(strA==null || strB==null) return 0;
        int n = Math.min(strA.length(),strB.length());
        int i = 0;
        for(;i<n;i++){
            if(strA.charAt(i)!=strB.charAt(i))
                break;
        }
        return i;
    }

    protected int commonSubfixLen(Tree a,Tree b){
        if(a==null || b==null) return 0;
        if(!a.hasSameType(b)) return 0;
        String strA = null;
        String strB = null;
        if(a.isLeaf()&&b.isLeaf()){
            strA = a.getLabel();
            strB = b.getLabel();
        }
        else if(a.getType().name.equals(Constants.METHOD_INVOCATION)){
            var srcName = TreeUtilFunctions.findChildByType(a,Constants.SIMPLE_NAME);
            var dstName = TreeUtilFunctions.findChildByType(b,Constants.SIMPLE_NAME);
            if(srcName!=null && dstName!=null){
                strA = srcName.getLabel();
                strB = dstName.getLabel();
            }
        }
        if(strA==null || strB==null) return 0;
        int n = Math.min(strA.length(),strB.length());
        int i = 0;
        for(;i<n;i++){
            if(strA.charAt(strA.length()-1-i)!=strB.charAt(strB.length()-1-i))
                break;
        }
        return i;
    }

    protected Tree getOutterStmt(Tree t){
        while(t!=null && !TreeUtilFunctions.isStatement(t.getType().name)){
            t = t.getParent();
        }
        return t;
    }

    protected Tree getOutterComposite(Tree t){
        while(t!=null && !TreeUtilFunctions.isCompositeStatement(t.getType().name)){
            t=t.getParent();
        }
        return t;
    }

    protected Tree getOutterLeaf(Tree t){
        while(t!=null && !TreeUtilFunctions.isLeafStatement(t.getType().name)){
            t = t.getParent();
        }
        return t;
    }

    protected boolean innerCallCheck(Tree t){
        while(t.getParent().getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER)){
            t=t.getParent().getParent();
        }
        if(t.getParent().getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT)) {
            return mappings.isSrcMapped(t.getParent().getParent());
        }
        if(t.getParent().getType().name.equals(Constants.ASSIGNMENT)) {
            return mappings.isSrcMapped(t.getParent());
        }
        return true;
    }

    protected  boolean innerCallCheck2(Tree t,boolean isSrc){
        if(getInnerCallSize(t)<=2){
            var outterStmt = getOutterLeaf(t);
            if(outterStmt!=null && outterStmt.getType().name.equals(Constants.RETURN_STATEMENT))
                return true;
            var outterComp = getOutterComposite(t);
            if(outterComp!=null){
                return isSrc?mappings.isSrcMappedConsideringSubTrees(outterComp):mappings.isDstMappedConsideringSubTrees(outterComp);
            }
        }
        return true;
    }

    protected List<Tree> getSrcCalls(Tree src){
        return TreeUtils.preOrder(src).stream().filter(
                t ->t.getType().name.equals(Constants.METHOD_INVOCATION) &&
                        (!mappings.isSrcMapped(t) && !mappings.isSrcMappedConsideringSubTrees(t)
                                && innerCallCheck(t) && innerCallCheck2(t,true)
                                || maybeBadMappings.isSrcMapped(t))).toList();
    }

    protected List<Tree> getDstCalls(Tree dst){
        return TreeUtils.preOrder(dst).stream().filter(
                t -> t.getType().name.equals(Constants.METHOD_INVOCATION) &&
                        (!mappings.isDstMapped(t) && !mappings.isDstMappedConsideringSubTrees(t)
                                && innerCallCheck(t) && innerCallCheck2(t,false)
                                || maybeBadMappings.isDstMapped(t))).toList();
    }

    protected List<Tree> getSrcCalls2(Tree src){
        var srcBlocks = TreeUtils.preOrder(src).stream().filter(t -> t.getType().name.equals(Constants.BLOCK)).toList();
        if(srcBlocks.size() <=2){
            return TreeUtils.preOrder(src).stream().filter(
                    t -> t.getType().name.equals(Constants.METHOD_INVOCATION) && !mappings.isSrcMapped(t)
                ).toList();
        }
        return new ArrayList<>();
    }

    protected List<Tree> getDstCalls2(Tree dst){
        var dstBlocks =  TreeUtils.preOrder(dst).stream().filter(t -> t.getType().name.equals(Constants.BLOCK)).toList();
        if(dstBlocks.size() <=2){
            return TreeUtils.preOrder(dst).stream().filter(
                    t -> t.getType().name.equals(Constants.METHOD_INVOCATION) && !mappings.isDstMapped(t)
                ).toList();
        }
        return new ArrayList<>();
    }

    protected void matchInnerCall(Tree src,Tree dst){
        var srcCalls = getSrcCalls(src);
        var dstCalls = getDstCalls(dst);

        if (!srcCalls.isEmpty() && !dstCalls.isEmpty()) {
            for (var srcCall : srcCalls) {
                List<Tree> candidates = new ArrayList<>();
                var candResult = 0;
                for (var dstCall : dstCalls) {
                    var result = sameInnerCall(srcCall, dstCall);
                    if (result == 0) continue;
                    if (result > candResult) {
                        candidates.clear();
                        candidates.add(dstCall);
                        candResult = result;
                    } else if (result == candResult) {
                        candidates.add(dstCall);
                    }
                }
                if (candResult > 0) {
                    Tree candidateCall = null;
                    if (candidates.size() == 1) {
                        candidateCall = candidates.get(0);
                    } else {
                        var srcSize = getInnerCallSize(srcCall);
                        var srcName = TreeUtilFunctions.findChildByType(srcCall, Constants.SIMPLE_NAME);
                        Comparator<? super Tree> comparator = (a, b) -> {
                            var dstNameA = TreeUtilFunctions.findChildByType(a, Constants.SIMPLE_NAME);
                            var dstNameB = TreeUtilFunctions.findChildByType(b, Constants.SIMPLE_NAME);
                            var commonPrefixA = commonPrefixLen(srcName, dstNameA);
                            var commonPrefixB = commonPrefixLen(srcName, dstNameB);
                            if (commonPrefixA != commonPrefixB)
                                return commonPrefixB - commonPrefixA;
                            var commonSubfixA = commonSubfixLen(srcName, dstNameA);
                            var commonSubfixB = commonSubfixLen(srcName, dstNameB);
                            if (commonSubfixA != commonSubfixB)
                                return commonSubfixB - commonSubfixA;
                            var sizeA = getInnerCallSize(a);
                            var sizeB = getInnerCallSize(b);
                            var diffA = Math.abs(sizeA - srcSize);
                            var diffB = Math.abs(sizeB - srcSize);
                            return diffA - diffB;
                        };
                        Collections.sort(candidates, comparator);
                        if (comparator.compare(candidates.get(0), candidates.get(1)) < 0)
                            candidateCall = candidates.get(0);
                    }
                    if (candidateCall != null) {
                        List<Tree> candidates2 = new ArrayList<>();
                        var candResult2 = 0;
                        for (var srcCall2 : srcCalls) {
                            var result = sameInnerCall(srcCall2, candidateCall);
                            if (result == 0) continue;
                            if (result > candResult2) {
                                candidates2.clear();
                                candidates2.add(srcCall2);
                                candResult2 = result;
                            } else if (result == candResult2) {
                                candidates2.add(srcCall2);
                            }
                        }
                        if (candidates2.size() > 1) {
                            var dstSize = getInnerCallSize(candidateCall);
                            var dstName = TreeUtilFunctions.findChildByType(candidateCall, Constants.SIMPLE_NAME);
                            Comparator<? super Tree> comparator = (a, b) -> {
                                var dstNameA = TreeUtilFunctions.findChildByType(a, Constants.SIMPLE_NAME);
                                var dstNameB = TreeUtilFunctions.findChildByType(b, Constants.SIMPLE_NAME);
                                var commonPrefixA = commonPrefixLen(dstName, dstNameA);
                                var commonPrefixB = commonPrefixLen(dstName, dstNameB);
                                if (commonPrefixA != commonPrefixB)
                                    return commonPrefixB - commonPrefixA;
                                var commonSubfixA = commonSubfixLen(dstName, dstNameA);
                                var commonSubfixB = commonSubfixLen(dstName, dstNameB);
                                if (commonSubfixA != commonSubfixB)
                                    return commonSubfixB - commonSubfixA;
                                var sizeA = getInnerCallSize(a);
                                var sizeB = getInnerCallSize(b);
                                var diffA = Math.abs(sizeA - dstSize);
                                var diffB = Math.abs(sizeB - dstSize);
                                return diffA - diffB;
                            };
                            Collections.sort(candidates2, comparator);
                            if (comparator.compare(candidates2.get(0), candidates2.get(1)) == 0 || candidates2.get(0) != srcCall)
                                candidateCall = null;
                        }
                    }
                    if (candidateCall != null) {
                        if (maybeBadMappings.isSrcMapped(srcCall)) {
                            if (srcCall.getMetrics().hash == candidateCall.getMetrics().hash) {
                                for (var badDst : Set.copyOf(maybeBadMappings.getDsts(srcCall))) {
                                    removeMappingSubTree(srcCall, badDst);
                                }
                                matchSubTree(srcCall, candidateCall);
                            }
                        } else if (maybeBadMappings.isDstMapped(candidateCall)) {
                            if (srcCall.getMetrics().hash == candidateCall.getMetrics().hash) {
                                for (var badSrc : Set.copyOf(maybeBadMappings.getSrcs(candidateCall))) {
                                    removeMappingSubTree(badSrc, candidateCall);
                                }
                                matchSubTree(srcCall, candidateCall);
                            }
                        } else if (isRename(srcCall, candidateCall)) {
                            matchSubTree(srcCall, candidateCall);
                        }
                        else if (candResult > 1 || Math.max(getInnerCallSize(srcCall), getInnerCallSize(candidateCall)) == 2 && commonPrefixLen(srcCall, candidateCall) > 1) {
                            var srcName = TreeUtilFunctions.findChildByType(srcCall, Constants.SIMPLE_NAME);
                            var dstName = TreeUtilFunctions.findChildByType(candidateCall, Constants.SIMPLE_NAME);
                            var check = true;
                            if (srcName != null && dstName != null) {
                                var b1 = srcName.getLabel().endsWith("Name") && dstName.getLabel().endsWith("Type");
                                var b2 = srcName.getLabel().endsWith("Type") && dstName.getLabel().endsWith("Name");
                                if (candResult == 1 && (b1 || b2)) check = false;
                            }
                            if (check){
                                matchSubTree(srcCall, candidateCall);
                            }
                        }
                    }
                }
            }
        }

        var srcCalls2 = getSrcCalls2(src);
        var dstCalls2 = getDstCalls2(dst);

        if(srcCalls2.size()==1 && dstCalls2.size()==1){
            var srcCall = srcCalls2.get(0);
            var dstCall = dstCalls2.get(0);
            if(srcCall.getMetrics().hash==dstCall.getMetrics().hash){
                matchSubTree(srcCall,dstCall);
            }
        }
    }

    protected List<Tree> getSrcReturns(Tree src){
        return getLeafStmts(src).stream().filter(t-> t.getType().name.equals(Constants.RETURN_STATEMENT)).toList();
    }

    protected List<Tree> getDstReturns(Tree dst){
        return getLeafStmts(dst).stream().filter(t-> t.getType().name.equals(Constants.RETURN_STATEMENT)).toList();
    }

    protected void matchInnerUnique(Tree src,Tree dst){
        var srcUniques = new HashMap<Integer,List>();
        var dstUniques = new HashMap<Integer,List>();
        for(var srcchild:src.preOrder()){
            if(srcchild.getMetrics().size>2 && !mappings.isSrcMapped(srcchild)){
                var h = srcchild.getMetrics().hash;
                if(!srcUniques.containsKey(h))
                    srcUniques.put(h,new ArrayList());
                srcUniques.get(h).add(srcchild);
            }
        }
        for(var dstchild:dst.preOrder()){
            if(dstchild.getMetrics().size>2 && !mappings.isDstMapped(dstchild)){
                var h = dstchild.getMetrics().hash;
                if(!dstUniques.containsKey(h))
                    dstUniques.put(h,new ArrayList());
                dstUniques.get(h).add(dstchild);
            }
        }
        for(var h:srcUniques.keySet()){
            if(dstUniques.containsKey(h)){
                var srcList = srcUniques.get(h);
                var dstList = dstUniques.get(h);
                if(srcList.size()==1 && dstList.size()==1){
                    checkMappingRecursively2((Tree)srcList.get(0),(Tree)dstList.get(0));
                }
            }
        }
    }

    protected void matchInnerReturn(Tree src,Tree dst){
        var srcReturns = getSrcReturns(src);
        if(srcReturns.isEmpty()) return;

        var dstReturns = getDstReturns(dst);
        if(dstReturns.isEmpty()) return;

        if(srcReturns.size()==1 && srcReturns.size()==1){
            var srcRet = srcReturns.get(0);
            var dstRet = dstReturns.get(0);
            if(!mappings.isSrcMapped(srcRet) && !mappings.isDstMapped(dstRet)){
                var res = getSameLeaves(srcRet,dstRet);
                if(res.second){
                    matchSubTree(srcRet,dstRet);
                }
            }
        }
        List<Tree> srcRetThisList = new ArrayList<>();
        List<Tree> dstRetThisList = new ArrayList<>();
        for(var srcRet:srcReturns){
            srcRetThisList.addAll(TreeUtils.preOrder(srcRet).stream().filter(t-> t.getType().name.equals("ThisExpression") && !mappings.isSrcMapped(t)).toList());
        }
        for(var dstRet:dstReturns){
            dstRetThisList.addAll(TreeUtils.preOrder(dstRet).stream().filter(t-> t.getType().name.equals("ThisExpression") && !mappings.isDstMapped(t)).toList());
        }
        if(srcRetThisList.size()==1 && dstRetThisList.size()==1){
            if(srcRetThisList.get(0).getParent().getType().name.equals(Constants.RETURN_STATEMENT)
                    || dstRetThisList.get(0).getParent().getType().name.equals(Constants.RETURN_STATEMENT)){
                addToMapping(srcRetThisList.get(0),dstRetThisList.get(0));
            }
        }
        List<Tree> srcRetNullList = new ArrayList<>();
        List<Tree> dstRetNullList = new ArrayList<>();
        for(var srcRet:srcReturns){
            srcRetNullList.addAll(TreeUtils.preOrder(srcRet).stream().filter(t-> t.getType().name.equals("NullLiteral") && !mappings.isSrcMapped(t)).toList());
        }
        for(var dstRet:dstReturns){
            dstRetNullList.addAll(TreeUtils.preOrder(dstRet).stream().filter(t-> t.getType().name.equals("NullLiteral") && !mappings.isDstMapped(t)).toList());
        }
        if(srcRetNullList.size()==1 && dstRetNullList.size()==1){
            if(srcRetNullList.get(0).getParent().getType().name.equals(Constants.RETURN_STATEMENT)
                    || dstRetNullList.get(0).getParent().getType().name.equals(Constants.RETURN_STATEMENT)){
                addToMapping(srcRetNullList.get(0),dstRetNullList.get(0));
            }
        }
    }

    protected List<Tree> getSrcUnmappedRelExps(Tree src){
        return TreeUtils.preOrder(src).stream().filter(
                t-> !mappings.isSrcMapped(t) && t.getType().name.equals(Constants.INFIX_EXPRESSION) && isRelationalOperator(t.getChild(1))).toList();
    }

    protected List<Tree> getDstUnmappedRelExps(Tree dst){
        return TreeUtils.preOrder(dst).stream().filter(
                t-> !mappings.isDstMapped(t) && t.getType().name.equals(Constants.INFIX_EXPRESSION) && isRelationalOperator(t.getChild(1))).toList();
    }

    protected void matchInnerCond(Tree src,Tree dst){
        List<Tree> srcUnmappedRelExps = getSrcUnmappedRelExps(src);
        if(srcUnmappedRelExps.size()!=1) return;

        List<Tree> dstUnmappedRelExps = getDstUnmappedRelExps(dst);
        if(dstUnmappedRelExps.size()!=1) return;

        var srcRelExp = srcUnmappedRelExps.get(0);
        var dstRelExp = dstUnmappedRelExps.get(0);
        var leftMatch = srcRelExp.getChild(0).getMetrics().hash==dstRelExp.getChild(0).getMetrics().hash;
        var rightMatch = srcRelExp.getChild(2).getMetrics().hash==dstRelExp.getChild(2).getMetrics().hash;
        if((leftMatch || rightMatch) && onlyInnerMapping(srcRelExp,dstRelExp)){
            var srcRelExpParent = srcRelExp.getParent();
            while(srcRelExpParent.getType().name.equals("ParenthesizedExpression")){
                srcRelExpParent = srcRelExpParent.getParent();
            }
            var dstRelExpParent = dstRelExp.getParent();
            while(dstRelExpParent.getType().name.equals("ParenthesizedExpression")){
                dstRelExpParent = dstRelExpParent.getParent();
            }
            var b1 = srcRelExpParent.getType().name.equals(Constants.IF_STATEMENT);
            var b2 = dstRelExpParent.getType().name.equals(Constants.IF_STATEMENT);
            var b3 = srcRelExpParent.getType().name.equals(Constants.CONDITIONAL_EXPRESSION);
            var b4 = dstRelExpParent.getType().name.equals(Constants.CONDITIONAL_EXPRESSION);
            if(b1&&b4 || b2&&b3){
                matchSubTree(srcRelExp,dstRelExp);
            }
        }
    }

    protected Set<Tree> getMappedSrcTrees(Tree src){
        Set<Tree> mappedSrcSubTrees = new HashSet<>();
        for (Tree tree : src.preOrder()) {
            if (mappings.isSrcMapped(tree))
                mappedSrcSubTrees.add(tree);
        }
        return mappedSrcSubTrees;
    }

    protected Set<Tree> getMappedDstTrees(Tree dst){
        Set<Tree> mappedDstSubTrees = new HashSet<>();
        for (Tree tree : dst.preOrder()) {
            if (mappings.isDstMapped(tree))
                mappedDstSubTrees.add(tree);
        }
        return mappedDstSubTrees;
    }

    protected boolean onlyInnerMapping(Tree src, Tree dst) {
        var mappedSrcSubTrees = getMappedSrcTrees(src);
        var mappedDstSubTrees = getMappedDstTrees(dst);
        if(mappedSrcSubTrees.size()!=mappedDstSubTrees.size())
            return false;
        var mappedCnt = 0;
        for(var mappedSrcSubTree: mappedSrcSubTrees){
            for(var mappedDstSubTree: mappings.getDsts(mappedSrcSubTree)){
                if(mappedDstSubTrees.contains(mappedDstSubTree))
                    mappedCnt++;
            }
        }
        return mappedCnt==mappedDstSubTrees.size();
    }

    protected boolean hasInnerMapping(Tree src, Tree dst){
        var mappedSrcSubTrees = getMappedSrcTrees(src);
        var mappedDstSubTrees = getMappedDstTrees(dst);
        for(var mappedSrcSubTree: mappedSrcSubTrees){
            for(var mappedDstSubTree: mappings.getDsts(mappedSrcSubTree)){
                if(mappedDstSubTrees.contains(mappedDstSubTree))
                    return true;
            }
        }
        return false;
    }

    protected Pair<Boolean,Boolean> includeMappings(Tree src, Tree dst) {
        var mappedSrcSubTrees = getMappedSrcTrees(src);
        var mappedDstSubTrees = getMappedDstTrees(dst);
        if(mappedSrcSubTrees.size()<=1 || mappedDstSubTrees.size()<=1)
            return new Pair<>(false,false);
        var mappedCnt = 0;
        for(var mappedSrcSubTree: mappedSrcSubTrees){
            for(var mappedDstSubTree: mappings.getDsts(mappedSrcSubTree)){
                if(mappedDstSubTrees.contains(mappedDstSubTree))
                    mappedCnt++;
            }
        }
        if(mappedCnt<=1) return new Pair<>(false,false);
        if(mappedCnt==mappedSrcSubTrees.size() && mappedSrcSubTrees.size()==mappedDstSubTrees.size()) return new Pair<>(true,true);
        if(mappedCnt==mappedSrcSubTrees.size()) return new Pair<>(true,false);
        if(mappedCnt==mappedDstSubTrees.size()) return new Pair<>(false,true);
        return new Pair<>(false,false);
    }

    protected void matchBlockByChildren(Tree ppSrc,Tree ppDst,Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if(parentSrc.getChildren().size()==1 && parentDst.getChildren().size()==1){
            if(ppSrc!=null && ppDst!=null && ppSrc.hasSameType(ppDst)){
                addToMapping(parentSrc,parentDst);
                newTrees.add(parentSrc);
            }
        }
    }

    protected void matchVVDByChildren(Tree ppSrc,Tree ppDst,Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        addToMapping(parentSrc,parentDst);
        newTrees.add(parentSrc);
        if(parentSrc.getChild(0).getType().name.equals("VARARGS_TYPE") && parentSrc.getChild(0).hasSameType(parentDst.getChild(0))){
            if(!mappings.isSrcMapped(parentSrc.getChild(0)) && !mappings.isDstMapped(parentDst.getChild(0))){
                addToMapping(parentSrc.getChild(0),parentDst.getChild(0));
            }
        }
    }

    protected void matchInfixExpByChildren(Tree ppSrc,Tree ppDst,Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if(parentSrc.getMetrics().hash==parentDst.getMetrics().hash){
            addToMapping(parentSrc,parentDst);
            newTrees.add(parentSrc);
        } else if (parentSrc.getChild(0) == src && parentDst.getChild(0) == dst) {
            if (onlyInnerMapping(parentSrc, parentDst) && isCompatibleOp(parentSrc.getChild(1), parentDst.getChild(1))
                    && parentSrc.getChild(2).hasSameType(parentDst.getChild(2))) {
                addToMapping(parentSrc, parentDst);
                newTrees.add(parentSrc);
            }
        } else if (parentSrc.getChild(2) == src && parentDst.getChild(2) == dst) {
            if (onlyInnerMapping(parentSrc, parentDst) && isCompatibleOp(parentSrc.getChild(1), parentDst.getChild(1))
                    && (parentSrc.getChild(0).hasSameType(parentDst.getChild(0))
                    || mappings.isSrcMapped(parentSrc.getChild(1)) && mappings.getDsts(parentSrc.getChild(1)).contains(parentDst.getChild(1))
                        && parentSrc.getChild(1).getMetrics().hash==parentDst.getChild(1).getMetrics().hash)) {
                addToMapping(parentSrc, parentDst);
                newTrees.add(parentSrc);
            }
        } else if (ppDst != null && ppDst.hasSameType(parentDst) && !mappings.isDstMapped(ppDst)) {
            if (includeMappings(parentSrc, ppDst).first && isCompatibleOp(parentSrc.getChild(1), ppDst.getChild(1))) {
                addToMapping(parentSrc, ppDst);
                mapMissingLeaves(parentSrc, ppDst);
                newTrees.add(parentSrc);
            }
        } else if (ppSrc != null && ppSrc.hasSameType(parentSrc) && !mappings.isSrcMapped(ppSrc)) {
            if (includeMappings(ppSrc, parentDst).second && isCompatibleOp(ppSrc.getChild(1), parentDst.getChild(1))) {
                addToMapping(ppSrc, parentDst);
                mapMissingLeaves(ppSrc, parentDst);
                newTrees.add(ppSrc);
            }
        }
    }

    protected void matchMethodCallArgsByChildren(Tree ppSrc,Tree ppDst,Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if (countMappedChild(parentSrc, parentDst) > 2
                || parentSrc.getChildren().size() == 1 && parentDst.getChildren().size() <= 2
                || parentDst.getChildren().size() == 1 && parentSrc.getChildren().size() <= 2) {
            if (!(mappings.isSrcMapped(ppSrc) || mappings.isDstMapped(ppDst))) {
                if (ppSrc.getChild(0).getType().name.equals(Constants.SIMPLE_NAME) && ppDst.getChild(0).getType().name.equals(Constants.SIMPLE_NAME)) {
                    addToMapping(parentSrc, parentDst);
                    addToMapping(ppSrc, ppDst);
                    newTrees.add(ppSrc);
                }
            }
        }
    }

    protected void matchIfStmtByChildren(Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if (parentSrc.getChild(0) == src && parentDst.getChild(0) == dst) {
            if (!(mappings.isSrcMapped(parentSrc.getChild(1)) || mappings.isDstMapped(parentDst.getChild(1))) && onlyInnerMapping(parentSrc, parentDst)) {
                addToMapping(parentSrc, parentDst);
                newTrees.add(parentSrc);
                addToMapping(parentSrc.getChild(1), parentDst.getChild(1));
            }
        } else if (parentSrc.getChild(1) == src && parentDst.getChild(1) == dst && parentSrc.getChildren().size() == 2 && parentDst.getChildren().size() == 2) {
            if (isNotWrapIf(parentSrc, parentDst)) {
                addToMapping(parentSrc, parentDst);
                newTrees.add(parentSrc);
            }
        }
    }

    protected void matchForStmtByChildren(Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if (parentSrc.getChildren().size() == parentDst.getChildren().size() && parentSrc.getChildren().indexOf(src) == parentDst.getChildren().indexOf(dst)) {
            addToMapping(parentSrc, parentDst);
            newTrees.add(parentSrc);
            for (int i = 0; i < parentSrc.getChildren().size(); i++) {
                var childSrc = parentSrc.getChild(i);
                var childDst = parentDst.getChild(i);
                if (!(mappings.isSrcMapped(childSrc) || mappings.isDstMapped(childDst))) {
                    if (childSrc.getType().name.equals(Constants.BLOCK))
                        addToMapping(childSrc, childDst);
                    else
                        matchSubTree(childSrc, childDst);
                }
            }
        }
    }

    protected void matchMethodCallByChildren(Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if(src.getType().name.equals(Constants.METHOD_INVOCATION_ARGUMENTS)){
            if(countMappedChild(src, dst)>0){
                var check=false;
                if(src.getChildren().size()==1 && dst.getChildren().size()==1 && src.getChild(0).getMetrics().hash==dst.getChild(0).getMetrics().hash){
                    if(src.getChild(0).getType().name.equals(Constants.CLASS_INSTANCE_CREATION)){
                        check=true;
                    }
                    else if(src.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION)){
                        var srcMethodName = TreeUtilFunctions.findChildByType(parentSrc,Constants.SIMPLE_NAME);
                        var dstMethodName = TreeUtilFunctions.findChildByType(parentDst,Constants.SIMPLE_NAME);
                        if(srcMethodName!=null && dstMethodName!=null && srcMethodName.getLabel().equals(dstMethodName.getLabel())){
                            check=true;
                        }
                    }
                }
                if(!check){
                    var srcChildren = new ArrayList<Tree>();
                    var dstChildren = new ArrayList<Tree>();
                    if(parentSrc.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER)){
                        srcChildren.add(parentSrc.getChild(0).getChild(0));
                    }
                    if(parentDst.getChild(0).getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER)){
                        dstChildren.add(parentDst.getChild(0).getChild(0));
                    }
                    for(var child:src.getChildren()){
                        srcChildren.add(child);
                    }
                    for(var child:dst.getChildren()){
                        dstChildren.add(child);
                    }
                    for(var srcChild:srcChildren){
                        for(var dstChild:dstChildren){
                            if(!mappings.isSrcMapped(srcChild) && !mappings.isDstMapped(dstChild) && srcChild.hasSameType(dstChild)){
                                Tree srcDecl = (Tree) srcChild.getMetadata("decl");
                                Tree dstDecl = (Tree) dstChild.getMetadata("decl");
                                if(srcDecl!=null && dstDecl!=null && mappings.isSrcMapped(srcDecl) && mappings.getDsts(srcDecl).contains(dstDecl)){
                                    addToMapping(srcChild,dstChild);
                                    check=true;
                                }
                            }
                        }
                    }
                }
                if(check){
                    addToMapping(parentSrc,parentDst);
                    newTrees.add(parentSrc);
                }
            }
        }
    }

    protected void matchByChildrenSameTypePP(Tree ppSrc,Tree ppDst,Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if (!mappings.isSrcMapped(parentSrc) && ppDst != null && !mappings.isDstMapped(ppDst) && parentSrc.hasSameType(ppDst)) {
            if (parentSrc.getType().name.equals(Constants.INFIX_EXPRESSION)) {
                if (includeMappings(parentSrc, ppDst).first) {
                    addToMapping(parentSrc, ppDst);
                    newTrees.add(parentSrc);
                    mapMissingLeaves(parentSrc, ppDst);
                    if (!(mappings.isSrcMapped(parentSrc.getChild(1)) || mappings.isDstMapped(ppDst.getChild(1)))) {
                        if (includeMappings(parentSrc.getChild(0), ppDst.getChild(0)).first && includeMappings(parentSrc.getChild(2), ppDst.getChild(2)).first) {
                            addToMapping(parentSrc.getChild(1), ppDst.getChild(1));
                        }
                    }
                }
            } else if (parentSrc.getType().name.equals(Constants.IF_STATEMENT)) {
                if (parentSrc.getChild(0) == src && ppDst.getChild(0) == parentDst) {
                    if (!(mappings.isSrcMapped(parentSrc.getChild(1)) || mappings.isDstMapped(ppDst.getChild(1)))) {
                        addToMapping(parentSrc, ppDst);
                        newTrees.add(parentSrc);
                        if(parentSrc.hasSameType(ppDst) && ppDst.getType().name.equals(Constants.BLOCK)){
                            addToMapping(parentSrc.getChild(1), ppDst.getChild(1));
                        }
                    }
                }
            }
        }
        if (!mappings.isDstMapped(parentDst) && ppSrc != null && !mappings.isSrcMapped(ppSrc) && parentDst.hasSameType(ppSrc)) {
            if (parentDst.getType().name.equals(Constants.INFIX_EXPRESSION)) {
                if (includeMappings(ppSrc, parentDst).second) {
                    addToMapping(ppSrc, parentDst);
                    newTrees.add(ppSrc);
                    mapMissingLeaves(ppSrc, parentDst);
                    if (!(mappings.isSrcMapped(ppSrc.getChild(1)) || mappings.isDstMapped(parentDst.getChild(1)))) {
                        if (includeMappings(ppSrc.getChild(0), parentDst.getChild(0)).second && includeMappings(ppSrc.getChild(2), parentDst.getChild(2)).second) {
                            addToMapping(ppSrc.getChild(1), parentDst.getChild(1));
                        }
                    }
                }
            } else if (parentDst.getType().name.equals(Constants.IF_STATEMENT)) {
                if (ppSrc.getChild(0) == parentSrc && parentDst.getChild(0) == dst) {
                    if (!(mappings.isSrcMapped(ppSrc.getChild(1)) || mappings.isDstMapped(parentDst.getChild(1)))) {
                        addToMapping(ppSrc, parentDst);
                        newTrees.add(ppSrc);
                        if(ppSrc.hasSameType(parentDst) && ppSrc.getType().name.equals(Constants.BLOCK)){
                            addToMapping(ppSrc.getChild(1), parentDst.getChild(1));
                        }
                    }
                }
            }
        }
    }

    protected void matchByChildrenDiffType(Tree parentSrc,Tree parentDst,Tree src,Tree dst,List<Tree> newTrees){
        if (parentSrc.getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER) && parentDst.getType().name.equals(Constants.ASSIGNMENT)) {
            var outterSrcCall = parentSrc.getParent();
            if (!mappings.isSrcMapped(outterSrcCall)) {
                var outterSrcCallName = TreeUtilFunctions.findChildByType(outterSrcCall, Constants.SIMPLE_NAME);
                if (outterSrcCallName != null) {
                    var receiverVar = parentDst.getChild(0);
                    var dstBlock = parentDst.getParents().stream().filter(t -> t.getType().name.equals(Constants.BLOCK)).findFirst();
                    if (dstBlock.isPresent()) {
                        var candidates = TreeUtils.preOrder(dstBlock.get()).stream()
                                .filter(t -> t.getPos() > parentDst.getEndPos() && !mappings.isDstMapped(t) && t.getType().name.equals(Constants.METHOD_INVOCATION)).toList();
                        for (var candidate : candidates) {
                            var candidateName = TreeUtilFunctions.findChildByType(candidate, Constants.SIMPLE_NAME);
                            var candidateReceiver = TreeUtilFunctions.findChildByType(candidate, Constants.METHOD_INVOCATION_RECEIVER);
                            if (candidateName != null && candidateReceiver != null
                                    && candidateName.getMetrics().hash == outterSrcCallName.getMetrics().hash
                                    && candidateReceiver.getChild(0).getMetrics().hash == receiverVar.getMetrics().hash) {
                                matchSubTree(outterSrcCall, candidate);
                                break;
                            }
                        }
                    }
                }
            }
        }
        else if (parentDst.getType().name.equals(Constants.METHOD_INVOCATION_RECEIVER) && parentSrc.getType().name.equals(Constants.ASSIGNMENT)) {
            var outterDstCall = parentDst.getParent();
            if (!mappings.isDstMapped(outterDstCall)) {
                var outterDstCallName = TreeUtilFunctions.findChildByType(outterDstCall, Constants.SIMPLE_NAME);
                if (outterDstCallName != null) {
                    var receiverVar = parentSrc.getChild(0);
                    var srcBlock = parentSrc.getParents().stream().filter(t -> t.getType().name.equals(Constants.BLOCK)).findFirst();
                    if (srcBlock.isPresent()) {
                        var candidates = TreeUtils.preOrder(srcBlock.get()).stream()
                                .filter(t -> t.getPos() > parentSrc.getEndPos() && !mappings.isSrcMapped(t) && t.getType().name.equals(Constants.METHOD_INVOCATION)).toList();
                        for (var candidate : candidates) {
                            var candidateName = TreeUtilFunctions.findChildByType(candidate, Constants.SIMPLE_NAME);
                            var candidateReceiver = TreeUtilFunctions.findChildByType(candidate, Constants.METHOD_INVOCATION_RECEIVER);
                            if (candidateName != null && candidateReceiver != null
                                    && candidateName.getMetrics().hash == outterDstCallName.getMetrics().hash
                                    && candidateReceiver.getChild(0).getMetrics().hash == receiverVar.getMetrics().hash) {
                                matchSubTree(candidate, outterDstCall);
                                break;
                            }
                        }
                    }
                }
            }
        }
        else if (parentSrc.getType().name.equals(Constants.RETURN_STATEMENT) && parentDst.getType().name.equals(Constants.ASSIGNMENT) && parentDst.getChild(2)==dst){
            var assignVar = parentDst.getChild(0);
            var dstBlock = TreeUtilFunctions.getParentUntilType(parentDst,Constants.BLOCK);
            if(dstBlock!=null){
                var candidates = dstBlock.getChildren().stream()
                        .filter(t -> t.getPos() > parentDst.getEndPos() && !mappings.isDstMapped(t)
                                && t.getType().name.equals(Constants.RETURN_STATEMENT) && t.getChildren().size()>0 && t.getChild(0).getMetrics().hash==assignVar.getMetrics().hash).toList();
                if(!candidates.isEmpty()){
                    if(!mappings.isSrcMapped(parentSrc)){
                        addToMapping(parentSrc,candidates.get(0));
                    }
                }
            }
        }
        else if (parentDst.getType().name.equals(Constants.RETURN_STATEMENT) && parentSrc.getType().name.equals(Constants.ASSIGNMENT) && parentSrc.getChild(2)==src){
            var assignVar = parentSrc.getChild(0);
            var srcBlock = TreeUtilFunctions.getParentUntilType(parentSrc,Constants.BLOCK);
            if(srcBlock!=null){
                var candidates = srcBlock.getChildren().stream()
                        .filter(t -> t.getPos() > parentSrc.getEndPos() && !mappings.isSrcMapped(t)
                                && t.getType().name.equals(Constants.RETURN_STATEMENT) && t.getChildren().size()>0 && t.getChild(0).getMetrics().hash==assignVar.getMetrics().hash).toList();
                if(!candidates.isEmpty()){
                    if(!mappings.isDstMapped(parentDst)){
                        addToMapping(candidates.get(0),parentDst);
                    }
                }
            }
        }
    }

    protected List<Tree> matchByChildrenContextRound(List<Tree> trees){
        Collections.sort(trees,Comparator.comparingInt(Tree::getPos));
        List<Tree> newTrees = new ArrayList<>();
        for (var src : trees) {
            if(!mappings.isSrcMapped(src))
                continue;
            for (var dst : mappings.getDsts(src)) {
                var parentSrc = src.getParent();
                var parentDst = dst.getParent();
                if (parentSrc == null || parentDst == null) continue;
                var ppSrc = parentSrc.getParent();
                var ppDst = parentDst.getParent();
                if (parentSrc.hasSameType(parentDst)) {
                    if (mappings.isSrcMapped(parentSrc) || mappings.isDstMapped(parentDst))
                        continue;
                    switch (parentSrc.getType().name) {
                        case Constants.WHILE_STATEMENT,Constants.EXPRESSION_STATEMENT,Constants.RETURN_STATEMENT,
                             "ParenthesizedExpression" -> {
                            addToMapping(parentSrc, parentDst);
                            newTrees.add(parentSrc);
                        }
                        case "PrefixExpression" -> {
                            if(src.getMetrics().hash == dst.getMetrics().hash){
                                addToMapping(parentSrc, parentDst);
                                var opSrc = parentSrc.getChild(0);
                                var opDst = parentDst.getChild(0);
                                if(!(mappings.isSrcMapped(opSrc) || mappings.isDstMapped(opDst))){
                                    addToMapping(opSrc,opDst);
                                }
                            }
                        }
                        case Constants.VARIABLE_DECLARATION_STATEMENT -> {
                            matchSubTree(parentSrc,parentDst);
                            newTrees.add(parentSrc);
                        }
                        case "SingleVariableDeclaration" -> {
                            if(ppSrc!=null && ppSrc.getType().name.equals("SingleVariableDeclaration"))
                                matchVVDByChildren(ppSrc,ppDst,parentSrc,parentDst,src,dst,newTrees);
                        }
                        case "ArrayAccess",Constants.ASSIGNMENT -> {
                            if (parentSrc.getChild(0) == src && parentDst.getChild(0) == dst) {
                                addToMapping(parentSrc, parentDst);
                                newTrees.add(parentSrc);
                            }
                        }
                        case Constants.BLOCK -> {
                            matchBlockByChildren(ppSrc,ppDst,parentSrc,parentDst,src,dst,newTrees);
                        }
                        case Constants.INFIX_EXPRESSION -> {
                            matchInfixExpByChildren(ppSrc,ppDst,parentSrc,parentDst,src,dst,newTrees);
                        }
                        case Constants.METHOD_INVOCATION_ARGUMENTS -> {
                            matchMethodCallArgsByChildren(ppSrc,ppDst,parentSrc,parentDst,src,dst,newTrees);
                        }
                        case Constants.IF_STATEMENT -> {
                            matchIfStmtByChildren(parentSrc,parentDst,src,dst,newTrees);
                        }
                        case Constants.FOR_STATEMENT -> {
                            matchForStmtByChildren(parentSrc,parentDst,src,dst,newTrees);
                        }
                        case Constants.METHOD_INVOCATION -> {
                            matchMethodCallByChildren(parentSrc,parentDst,src,dst,newTrees);
                        }
                    }
                }
                else {
                    matchByChildrenSameTypePP(ppSrc,ppDst,parentSrc,parentDst,src,dst,newTrees);
                    matchByChildrenDiffType(parentSrc,parentDst,src,dst,newTrees);
                }
            }
        }

        return newTrees;
    }

    protected void matchByParentDecl(Tree src,Tree dst){
        for(var srcchild:src.getChildren()){
            for(var dstchild:dst.getChildren()){
                if(!mappings.isSrcMapped(srcchild) && !mappings.isDstMapped(dstchild)){
                    if(srcchild.getMetrics().hash==dstchild.getMetrics().hash){
                        addToMapping(srcchild,dstchild);
                    }
                    else if(srcchild.hasSameType(dstchild)){
                        if(srcchild.getType().name.equals(Constants.SIMPLE_NAME)
                                || srcchild.getType().name.equals(Constants.BLOCK))
                            addToMapping(srcchild,dstchild);
                    }
                }
            }
        }
    }

    protected void matchByParentMethodCall(Tree src,Tree dst){
        var srcMethodName = TreeUtilFunctions.findChildByType(src,Constants.SIMPLE_NAME);
        var dstMethodName = TreeUtilFunctions.findChildByType(dst,Constants.SIMPLE_NAME);
        if(srcMethodName!=null && dstMethodName!=null){
            if(!mappings.isSrcMapped(srcMethodName) && !mappings.isDstMapped(dstMethodName)){
                addToMapping(srcMethodName,dstMethodName);
            }
        }
        var srcReceiver = TreeUtilFunctions.findChildByType(src,Constants.METHOD_INVOCATION_RECEIVER);
        var dstReceiver = TreeUtilFunctions.findChildByType(dst,Constants.METHOD_INVOCATION_RECEIVER);
        if(srcReceiver!=null && dstReceiver!=null){
            var srcInnerReceiver = srcReceiver.getChild(0);
            var dstInnerReceiver = dstReceiver.getChild(0);
            if(!mappings.isSrcMapped(srcInnerReceiver) && !mappings.isDstMapped(dstInnerReceiver) && srcInnerReceiver.hasSameType(dstInnerReceiver)){
                if(srcInnerReceiver.getType().name.equals(Constants.SIMPLE_NAME)){
                    addToMapping(srcInnerReceiver,dstInnerReceiver);
                    if(!mappings.isSrcMapped(srcReceiver) && !mappings.isDstMapped(dstReceiver)){
                        addToMapping(srcReceiver,dstReceiver);
                    }
                }
            }
        }
    }

    protected void matchByParentAssign(Tree src,Tree dst){
        var opSrc = src.getChild(1);
        var opDst = dst.getChild(1);
        if (!(mappings.isSrcMapped(opSrc) || mappings.isDstMapped(opDst))) {
            addToMapping(opSrc, opDst);
        }
        var assignSrc = src.getChild(2);
        var assignDst = dst.getChild(2);
        if(!(mappings.isSrcMapped(assignSrc) || mappings.isDstMapped(assignDst))){
            if(assignSrc.getType().name.equals(Constants.BOOLEAN_LITERAL)){
                var srcIf = src.getParents().stream().filter(t->t.getType().name.equals(Constants.IF_STATEMENT)).findFirst();
                if(srcIf.isPresent()){
                    var srcCond = srcIf.get().getChild(0);
                    if(!mappings.isSrcMapped(srcCond) && srcCond.getMetrics().hash==assignDst.getMetrics().hash){
                        addToMapping(srcCond,assignDst);
                    }
                }
            }
            else if(assignDst.getType().name.equals(Constants.BOOLEAN_LITERAL)){
                var dstIf = dst.getParents().stream().filter(t->t.getType().name.equals(Constants.IF_STATEMENT)).findFirst();
                if(dstIf.isPresent()){
                    var dstCond = dstIf.get().getChild(0);
                    if(!mappings.isDstMapped(dstCond) && assignSrc.getMetrics().hash==dstCond.getMetrics().hash){
                        addToMapping(assignSrc,dstCond);
                    }
                }
            }
        }
    }

    protected void matchByParentInfixExp(Tree src,Tree dst){
        for(int i=0;i<3;i++){
            var chSrc = src.getChild(i);
            var chDst = dst.getChild(i);
            if(!(mappings.isSrcMapped(chSrc) || mappings.isDstMapped(chDst)) && chSrc.hasSameType(chDst) && chSrc.isLeaf()){
                addToMapping(chSrc,chDst);
            }
        }
        var srcLeft = src.getChild(0);
        var srcRight = src.getChild(2);
        var dstLeft = dst.getChild(0);
        var dstRight = dst.getChild(2);
        if(!(mappings.isSrcMapped(srcLeft) || mappings.isDstMapped(dstLeft))){
            if(srcRight.getType().name.equals("NullLiteral")){
                if(dstLeft.getType().name.equals(Constants.METHOD_INVOCATION)){
                    var dstLeftReceiver = TreeUtilFunctions.findChildByType(dstLeft,Constants.METHOD_INVOCATION_RECEIVER);
                    if(dstLeftReceiver!=null && !mappings.isDstMapped(dstLeftReceiver.getChild(0)) &&
                            dstLeftReceiver.getChild(0).getMetrics().hash==srcLeft.getMetrics().hash){
                        matchSubTree(srcLeft,dstLeftReceiver.getChild(0));
                    }
                }
            }
            else if(dstRight.getType().name.equals("NullLiteral")){
                if(srcLeft.getType().name.equals(Constants.METHOD_INVOCATION)){
                    var srcLeftReceiver = TreeUtilFunctions.findChildByType(srcLeft,Constants.METHOD_INVOCATION_RECEIVER);
                    if(srcLeftReceiver!=null && !mappings.isSrcMapped(srcLeftReceiver.getChild(0)) &&
                            srcLeftReceiver.getChild(0).getMetrics().hash==dstLeft.getMetrics().hash){
                        matchSubTree(srcLeftReceiver.getChild(0),dstLeft);
                    }
                }
            }
        }
    }

    protected void matchInnerStr(Tree src,Tree dst){
        var srcStrs = TreeUtils.preOrder(src).stream().filter(t-> !mappings.isSrcMapped(t) && t.getType().name.equals(Constants.STRING_LITERAL)).toList();
        var dstStrs = TreeUtils.preOrder(dst).stream().filter(t-> !mappings.isDstMapped(t) && t.getType().name.equals(Constants.STRING_LITERAL)).toList();
        for(var srcStr:srcStrs){
            for(var dstStr:dstStrs){
                if(mappings.isDstMapped(dstStr))
                    continue;
                if(commonPrefixLen(srcStr,dstStr)>2){
                    addToMapping(srcStr,dstStr);
                    break;
                }
            }
        }
    }

    protected void matchByParentIfStmt(Tree src,Tree dst){
        var srcConds = flattenCond(src.getChild(0));
        var dstConds = flattenCond(dst.getChild(0));
        if(srcConds.size()==1 && dstConds.size()==1 && srcConds.get(0).hasSameType(dstConds.get(0))){
            var srcCond = srcConds.get(0);
            var dstCond = dstConds.get(0);
            if(!mappings.isSrcMapped(srcCond) && !mappings.isDstMapped(dstCond)){
                if(srcCond.getType().name.equals(Constants.SIMPLE_NAME))
                    addToMapping(srcCond,dstCond);
            }
        }
    }

    protected void matchByParentBlock(Tree src,Tree dst){
        if(src.getMetrics().hash==dst.getMetrics().hash){
            var srcStmts = getLeafStmts(src);
            var dstStmts = getLeafStmts(dst);
            if(srcStmts.size()==dstStmts.size()){
                for(int i=0;i<srcStmts.size();i++){
                    var srcStmt = srcStmts.get(i);
                    var dstStmt = dstStmts.get(i);
                    if(!(mappings.isSrcMapped(srcStmt) || mappings.isDstMapped(dstStmt))){
                        matchSubTree(srcStmt,dstStmt);
                    }
                }
            }
        }
    }

    protected void matchByParentMethodDeclPre(Tree src, Tree dst){
        var srcDecls = src.getChildren().stream().filter(c->c.getType().name.equals("SingleVariableDeclaration")).toList();
        var dstDecls = dst.getChildren().stream().filter(c->c.getType().name.equals("SingleVariableDeclaration")).toList();
        for(var srcDecl:srcDecls){
            var candidates = new ArrayList<Tree>();
            for (var dstDecl:dstDecls){
                if(!mappings.isSrcMapped(srcDecl) && !mappings.isDstMapped(dstDecl)){
                    var result = getSameLeaves(srcDecl,dstDecl);
                    if(result.first.size()>1){
//                        matchSubTree(srcDecl,dstDecl);
                        candidates.add(dstDecl);
                    }
                }
            }
            if(candidates.size()==1){
                matchSubTree(srcDecl,candidates.get(0));
            }
        }
    }

    protected Tree getDeclType(Tree t){
        Tree ty = null;
        if(t.getType().name.equals("SingleVariableDeclaration")){
            ty = t.getChild(0);
        }
        if(t.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT) && (t.getParent().getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT) || t.getParent().getType().name.equals(Constants.FIELD_DECLARATION))){
            for(var child:t.getParent().getChildren()){
                if(child.getType().name.endsWith("Type")){
                    ty = child;
                    break;
                }
            }
        }
        if(ty==null) return null;
        return ty.getType().name.endsWith("Type")?ty:null;
    }

    protected Tree getDeclName(Tree t){
        Tree name = null;
        if(t.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT)){
            name = t.getChild(0);
        }
        else if(t.getType().name.equals("SingleVariableDeclaration")){
            name = t.getChild(1);
        }
        if(name==null) return null;
        return name.getType().name.endsWith("Name")?name:null;
    }

    protected Tree getDeclInit(Tree t){
        Tree init = null;
        if(t.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT) && t.getChildren().size()>1){
            init = t.getChild(1);
        }
        else if(t.getType().name.equals("SingleVariableDeclaration")){
            if(t.getParent().getType().name.equals(Constants.ENHANCED_FOR_STATEMENT))
                init = t.getParent().getChild(1);
        }
        return init;
    }

    protected boolean checkDecl(Tree srcDecl,Tree dstDecl){
        var b1 = srcDecl.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT);
        var b2 = dstDecl.getType().name.equals(Constants.VARIABLE_DECLARATION_FRAGMENT);
        var b3 = srcDecl.getType().name.equals("SingleVariableDeclaration");
        var b4 = dstDecl.getType().name.equals("SingleVariableDeclaration");
        if(!(b1||b3)) return false;
        if(!(b2||b4)) return false;
        var srcDeclParent = srcDecl.getParent();
        var dstDeclParent = dstDecl.getParent();
        if(srcDeclParent==null || dstDeclParent==null) return false;
        var b5 = srcDeclParent.getType().name.equals(Constants.METHOD_DECLARATION);
        var b6 = dstDeclParent.getType().name.equals(Constants.METHOD_DECLARATION);
        return b5==b6;
    }

    protected void matchDecl(Tree srcDecl,Tree dstDecl){
        if(!checkDecl(srcDecl,dstDecl))
            return;
        Tree srcDeclType = getDeclType(srcDecl);
        Tree dstDeclType = getDeclType(dstDecl);
        Tree srcDeclName = getDeclName(srcDecl);
        Tree dstDeclName = getDeclName(dstDecl);
        if (srcDeclType != null && dstDeclType != null && srcDeclName != null && dstDeclName != null) {
            matchSubTree(srcDeclType, dstDeclType);
            addToMapping(srcDeclName, dstDeclName);
            if(srcDecl.hasSameType(dstDecl)){
                var srcDeclOutter = srcDecl.getParent();
                var dstDeclOutter = dstDecl.getParent();
                if(srcDeclOutter!=null && dstDeclOutter!=null && srcDeclOutter.hasSameType(dstDeclOutter)){
                    matchSubTree(srcDeclOutter,dstDeclOutter);
                }
                else
                    addToMapping(srcDecl, dstDecl);
            }
        }
        Tree srcDeclInit = getDeclInit(srcDecl);
        Tree dstDeclInit = getDeclInit(dstDecl);
        if (srcDeclInit != null && dstDeclInit != null && srcDeclInit.hasSameType(dstDeclInit)) {
            matchSubTree(srcDeclInit, dstDeclInit);
        }
        var srcDeclOutter = srcDecl.getParent();
        var dstDeclOutter = dstDecl.getParent();
        var b1=srcDeclOutter.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT);
        var b2=dstDeclOutter.getType().name.equals(Constants.VARIABLE_DECLARATION_STATEMENT);
        var b3=srcDeclOutter.getType().name.equals(Constants.ENHANCED_FOR_STATEMENT);
        var b4=dstDeclOutter.getType().name.equals(Constants.ENHANCED_FOR_STATEMENT);
        var b5=srcDeclOutter.getType().name.equals(Constants.METHOD_DECLARATION);
        var b6=dstDeclOutter.getType().name.equals(Constants.METHOD_DECLARATION);
        if(b1&&b4 || b2&&b3){
            matchSubTree(srcDeclOutter, dstDeclOutter);
        }
        else if(b5&&b6){
            var prevSrc = getPrevSibling(srcDecl);
            var prevDst = getPrevSibling(dstDecl);
            if(prevSrc!=null && prevDst!=null){
                if(checkDecl(prevSrc,prevDst)){
                    var prevSrcType = getDeclType(prevSrc);
                    var prevDstType = getDeclType(prevDst);
                    if(prevSrcType!=null && prevDstType!=null && prevSrcType.getMetrics().hash==prevDstType.getMetrics().hash){
                        matchSubTree(prevSrc,prevDst);
                    }
                }
            }
        }
    }

    protected boolean compatibleType(Tree ty1,Tree ty2){
        if(ty1.hasSameType(ty2)){
            if(ty1.getType().name.equals(Constants.SIMPLE_TYPE)){
                var b1 = ty1.getChild(0).getLabel().equals("String");
                var b2 = ty2.getChild(0).getLabel().equals("String");
                return b1==b2;
            }
            return true;
        }
        var result=getSameLeaves(ty1,ty2);
        if(result.second) return true;
        var b1 = ty1.getType().name.equals(Constants.PRIMITIVE_TYPE);
        var b2 = ty2.getType().name.equals(Constants.PRIMITIVE_TYPE);
        var b3 = ty1.getType().name.equals(Constants.SIMPLE_TYPE);
        var b4 = ty2.getType().name.equals(Constants.SIMPLE_TYPE);
        var b5 = false;
        var b6 = false;
        if(b3) b5 = ty1.getChild(0).getLabel().equals("String");
        if(b4) b6 = ty2.getChild(0).getLabel().equals("String");

        if(b1&&b4){
            var ty1str=ty1.getLabel();
            var ty2str=ty2.getChild(0).getLabel();
            if(ty2str.toLowerCase().contains(ty1str))
                return true;
        }
        else if(b2&&b3){
            var ty1str=ty1.getChild(0).getLabel();
            var ty2str=ty2.getLabel();
            if(ty1str.toLowerCase().contains(ty2str))
                return true;
        }
        return b1==b2 && b5==b6;
    }
}
