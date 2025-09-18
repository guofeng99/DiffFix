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
public class FastFixingMatcher extends BaseFixingMatcher {
    private List<Tree> unMappedSrcImports;
    private List<Tree> unMappedDstImports;

    private Stack<MethodDeclInfo> methodDeclStack;
    private Stack<CompositeStmtInfo> compStmtStack;

    private List<Tree> unmatchedStmtInWholeSrc;
    private List<Tree> unmatchedStmtInWholeDst;

    private Set<Tree> skipDstDecls;

    private final static Logger logger = LoggerFactory.getLogger(FastFixingMatcher.class);

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
            warmUp(); // merge fixType; merge initRenameMap in matchInner
            pass_finished =  System.currentTimeMillis();
            logger.info("current pass: {}, execution: {} milliseconds",current_pass, pass_finished - pass_started);

            current_pass = "MatchByUnique";
            pass_started = System.currentTimeMillis();
            matchByUnique(getCurMappedSrcs()); // around context only one missing
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
            warmUp(); // merge fixType; merge initRenameMap in matchInner

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

    @Override
    protected void initDS(Tree src_, Tree dst_, ExtendedMultiMappingStore mappingStore){
        super.initDS(src_,dst_,mappingStore);

        unMappedSrcImports = new ArrayList<>();
        unMappedDstImports = new ArrayList<>();

        methodDeclStack = new Stack<>();
        compStmtStack = new Stack<>();

        unmatchedStmtInWholeSrc = new ArrayList<>();
        unmatchedStmtInWholeDst = new ArrayList<>();

        skipDstDecls = new HashSet<>();
    }


    private void warmUp(){
        for(var child:src.getChildren()){
            tryToFixType(child);
            switch(child.getType().name){
                case Constants.IMPORT_DECLARATION -> {
                    if(!mappings.isSrcMapped(child))
                        unMappedSrcImports.add(child);
                }
                case Constants.TYPE_DECLARATION -> {
                    var needTraverse = true;
                    if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                        var dstDecl = mappings.getDsts(child).iterator().next();
                        if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                            checkMappingRecursively(child,dstDecl);
                            curMappedSrcs.remove(child);
                            needTraverse = false;
                            skipDstDecls.add(dstDecl);
                        }
                    }
                    if(needTraverse){
                        var typeDeclInfo = new TypeDeclInfo(child);
                        typeDeclInfo.traverseSrc();
                        child.setMetadata("typeDeclInfo",typeDeclInfo);
                    }
                }
                case Constants.ENUM_DECLARATION -> {
                    var needTraverse = true;
                    if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                        var dstDecl = mappings.getDsts(child).iterator().next();
                        if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                            checkMappingRecursively(child,dstDecl);
                            curMappedSrcs.remove(child);
                            needTraverse = false;
                            skipDstDecls.add(dstDecl);
                        }
                    }
                    if(needTraverse){
                        var enumDeclInfo = new EnumDeclInfo(child);
                        enumDeclInfo.traverseSrc();
                        child.setMetadata("enumDeclInfo",enumDeclInfo);
                    }
                }
                default -> {
                    trivalTraverse(child);
                }
            }
        }

        for(var child:dst.getChildren()){
            switch(child.getType().name){
                case Constants.IMPORT_DECLARATION -> {
                    if(!mappings.isDstMapped(child))
                        unMappedDstImports.add(child);
                }
                case Constants.TYPE_DECLARATION -> {
                    if(!skipDstDecls.contains(child)){
                        var typeDeclInfo = new TypeDeclInfo(child);
                        typeDeclInfo.traverseDst();
                        child.setMetadata("typeDeclInfo",typeDeclInfo);
                    }
                }
                case Constants.ENUM_DECLARATION -> {
                    if(!skipDstDecls.contains(child)){
                        var enumDeclInfo = new EnumDeclInfo(child);
                        enumDeclInfo.traverseDst();
                        child.setMetadata("enumDeclInfo",enumDeclInfo);
                    }
                }
            }
        }
    }

    private class BasicInfo{
        Tree root;

        BasicInfo(Tree root){
            this.root = root;
        }

        public void processRootSrc(){}

        public void processRootDst(){}

        public void traverseSrc(){
            List<Tree> nodes = new ArrayList<>();
            processRootSrc();
            nodes.add(root);
            int pos=0;
            while(pos<nodes.size()){
                Tree cur=nodes.get(pos);
                tryToFixType(cur);

                switch(cur.getType().name){
                    case Constants.ANONYMOUS_CLASS_DECLARATION -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(cur) && mappings.isSrcUnique(cur)){
                            var dstDecl = mappings.getDsts(cur).iterator().next();
                            if(cur.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(cur,dstDecl);
                                curMappedSrcs.remove(cur);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse) {
                            var typeDeclInfo = new TypeDeclInfo(cur);
                            typeDeclInfo.traverseSrc();
                            cur.setMetadata("typeDeclInfo", typeDeclInfo);
                        }
                    }
                    case Constants.INFIX_EXPRESSION -> {
                        if(isRelationalOperator(cur.getChild(1))){
                            if(!methodDeclStack.isEmpty()){
                                var methodDeclInfo = methodDeclStack.peek();
                                methodDeclInfo.relExps.add(cur);
                            }
                        }
                        nodes.addAll(cur.getChildren());
                    }
                    case Constants.METHOD_INVOCATION -> {
                        if(!methodDeclStack.isEmpty()){
                            var methodDeclInfo = methodDeclStack.peek();

                            if(!compStmtStack.isEmpty()){
                                for(var compStmtInfo:compStmtStack){
                                    if(compStmtInfo.root.getPos()<methodDeclInfo.root.getPos())
                                        continue;
                                    if(compStmtInfo.root.getType().name.equals(Constants.BLOCK))
                                        compStmtInfo.methodCalls.add(cur);
                                }
                            }
                        }
                        nodes.addAll(cur.getChildren());
                    }
                    default -> {
                        nodes.addAll(cur.getChildren());
                    }
                }
                ++pos;
            }
        }

        public void traverseDst(){
            List<Tree> nodes = new ArrayList<>();
            processRootDst();
            nodes.add(root);
            int pos=0;
            while(pos<nodes.size()) {
                Tree cur = nodes.get(pos);

                switch(cur.getType().name) {
                    case Constants.ANONYMOUS_CLASS_DECLARATION -> {
                        if(!skipDstDecls.contains(cur)){
                            var typeDeclInfo = new TypeDeclInfo(cur);
                            typeDeclInfo.traverseDst();
                            cur.setMetadata("typeDeclInfo", typeDeclInfo);
                        }
                    }
                    case Constants.INFIX_EXPRESSION -> {
                        if (isRelationalOperator(cur.getChild(1))) {
                            if (!methodDeclStack.isEmpty()) {
                                var methodDeclInfo = methodDeclStack.peek();
                                methodDeclInfo.relExps.add(cur);
                            }
                        }
                        nodes.addAll(cur.getChildren());
                    }
                    case Constants.METHOD_INVOCATION -> {
                        if(!methodDeclStack.isEmpty()){
                            var methodDeclInfo = methodDeclStack.peek();

                            if(!compStmtStack.isEmpty()){
                                for(var compStmtInfo:compStmtStack){
                                    if(compStmtInfo.root.getPos()<methodDeclInfo.root.getPos())
                                        continue;
                                    if(compStmtInfo.root.getType().name.equals(Constants.BLOCK))
                                        compStmtInfo.methodCalls.add(cur);
                                }
                            }
                        }
                        nodes.addAll(cur.getChildren());
                    }
                    default -> {
                        nodes.addAll(cur.getChildren());
                    }
                }
                ++pos;
            }
        }
    }

    private class LeafStmtInfo extends BasicInfo{
        LeafStmtInfo(Tree root){
            super(root);
        }

        @Override
        public void processRootSrc(){
            if(!mappings.isSrcMapped(root))
                unmatchedStmtInWholeSrc.add(root);
        }

        @Override
        public void processRootDst(){
            if(!mappings.isDstMapped(root))
                unmatchedStmtInWholeDst.add(root);
        }
    }

    private class RetStmtInfo extends LeafStmtInfo{
        RetStmtInfo(Tree root){
            super(root);
        }

        @Override
        public void processRootSrc(){
            super.processRootSrc();

            if(!methodDeclStack.isEmpty()){
                var methodDeclInfo = methodDeclStack.peek();
                methodDeclInfo.retStmts.add(root);

                if(!mappings.isSrcMapped(root)){
                    if(!compStmtStack.isEmpty()){
                        for(var compStmtInfo:compStmtStack){
                            if(compStmtInfo.root.getPos()<methodDeclInfo.root.getPos())
                                continue;
                            compStmtInfo.unmatchedDescRetStmt.add(root);
                        }
                    }
                }
            }
        }

        @Override
        public void processRootDst(){
            super.processRootDst();

            if(!methodDeclStack.isEmpty()){
                var methodDeclInfo = methodDeclStack.peek();
                methodDeclInfo.retStmts.add(root);

                if(!mappings.isDstMapped(root)){
                    if(!compStmtStack.isEmpty()){
                        for(var compStmtInfo:compStmtStack){
                            if(compStmtInfo.root.getPos()<methodDeclInfo.root.getPos())
                                continue;
                            compStmtInfo.unmatchedDescRetStmt.add(root);
                        }
                    }
                }
            }
        }
    }

    private class ThrowStmtInfo extends LeafStmtInfo{
        ThrowStmtInfo(Tree root){
            super(root);
        }

        @Override
        public void processRootSrc(){
            super.processRootSrc();

            if(!mappings.isSrcMapped(root)){
                if(!compStmtStack.isEmpty()){
                    for(var compStmtInfo:compStmtStack){
                        compStmtInfo.unmatchedDescThrowStmt.add(root);
                    }
                }
            }
        }

        @Override
        public void processRootDst(){
            super.processRootDst();

            if(!mappings.isDstMapped(root)){
                if(!compStmtStack.isEmpty()){
                    for(var compStmtInfo:compStmtStack){
                        compStmtInfo.unmatchedDescThrowStmt.add(root);
                    }
                }
            }
        }
    }

    private class FieldDeclInfo extends BasicInfo{
        FieldDeclInfo(Tree root){
            super(root);
        }
    }

    private class CondInfo extends BasicInfo{
        CondInfo(Tree root){
            super(root);
        }
    }

    private class CompositeStmtInfo{
        Tree root;
        List<Tree> unmatchedChildrenStmt;
        List<Tree> unmatchedDescRetStmt;
        List<Tree> unmatchedDescThrowStmt;
        List<Tree> methodCalls; // only works in Block
        List<Tree> blocks; // only works in Block
        Tree cond;

        CompositeStmtInfo(Tree root){
            this.root = root;
            this.unmatchedChildrenStmt = new ArrayList<>();
            this.unmatchedDescRetStmt = new ArrayList<>();
            this.unmatchedDescThrowStmt = new ArrayList<>();
            this.methodCalls = new ArrayList<>();
            this.blocks = new ArrayList<>();
        }

        public void processRootSrc(){
            switch (root.getType().name){
                case Constants.IF_STATEMENT, Constants.WHILE_STATEMENT -> {
                    cond = root.getChild(0);
                    var condInfo = new CondInfo(cond);
                    condInfo.traverseSrc();
                    cond.setMetadata("condInfo",condInfo);
                }
                case Constants.FOR_STATEMENT, Constants.DO_STATEMENT -> {
                    var candidates = root.getChildren().stream().filter(c -> !TreeUtilFunctions.isStatement(c.getType().name)
                            && !c.getType().name.equals("VariableDeclarationExpression")).toList();
                    if(candidates.size()==1){
                        cond = candidates.get(0);
                        var condInfo = new CondInfo(cond);
                        condInfo.traverseSrc();
                        cond.setMetadata("condInfo",condInfo);
                    }
                }
                case Constants.BLOCK -> {
                    if(!methodDeclStack.isEmpty()){
                        var methodDeclInfo = methodDeclStack.peek();

                        if(!compStmtStack.isEmpty()){
                            for(var compStmtInfo:compStmtStack){
                                if(compStmtInfo.root.getPos()<methodDeclInfo.root.getPos())
                                    continue;
                                if(compStmtInfo.root.getType().name.equals(Constants.BLOCK))
                                    compStmtInfo.blocks.add(root);
                            }
                        }
                    }
                }
            }
        }

        public void processRootDst(){
            switch (root.getType().name){
                case Constants.IF_STATEMENT, Constants.WHILE_STATEMENT -> {
                    cond = root.getChild(0);
                    var condInfo = new CondInfo(cond);
                    condInfo.traverseDst();
                    cond.setMetadata("condInfo",condInfo);
                }
                case Constants.FOR_STATEMENT, Constants.DO_STATEMENT -> {
                    var candidates = root.getChildren().stream().filter(c -> !TreeUtilFunctions.isStatement(c.getType().name)
                            && !c.getType().name.equals("VariableDeclarationExpression")).toList();
                    if(candidates.size()==1){
                        cond = candidates.get(0);
                        var condInfo = new CondInfo(cond);
                        condInfo.traverseDst();
                        cond.setMetadata("condInfo",condInfo);
                    }
                }
                case Constants.BLOCK -> {
                    if(!methodDeclStack.isEmpty()){
                        var methodDeclInfo = methodDeclStack.peek();

                        if(!compStmtStack.isEmpty()){
                            for(var compStmtInfo:compStmtStack){
                                if(compStmtInfo.root.getPos()<methodDeclInfo.root.getPos())
                                    continue;
                                if(compStmtInfo.root.getType().name.equals(Constants.BLOCK))
                                    compStmtInfo.blocks.add(root);
                            }
                        }
                    }
                }
            }
        }

        public void traverseSrc(){
            if(!mappings.isSrcMapped(root))
                unmatchedStmtInWholeSrc.add(root);

            processRootSrc();

            for(var child:root.getChildren()){
                tryToFixType(child);
                var type = child.getType().name;
                if(TreeUtilFunctions.isCompositeStatement(type)){
                    var compositeStmtInfo = new CompositeStmtInfo(child);
                    compStmtStack.push(compositeStmtInfo);
                    compositeStmtInfo.traverseSrc();
                    compStmtStack.pop();
                    child.setMetadata("compositeStmtInfo",compositeStmtInfo);
                }
                else if(TreeUtilFunctions.isLeafStatement(type)){
                    var leafStmtInfo = getLeafStmtInfo(child, type);
                    leafStmtInfo.traverseSrc();
                    child.setMetadata("leafStmtInfo",leafStmtInfo);

                    if(prevStmtInTraverse!=null){
                        srcNextStmtMap.put(prevStmtInTraverse,child);
                        srcPrevStmtMap.put(child,prevStmtInTraverse);
                    }
                    srcIdenticalStmts.putIfAbsent(child.getMetrics().hash,new ArrayList<>());
                    srcIdenticalStmts.get(child.getMetrics().hash).add(child);
                    prevStmtInTraverse=child;
                }
                else if(child!=cond){
                    trivalTraverse(child);
                }
                if(!mappings.isSrcMapped(child)){
                    if(TreeUtilFunctions.isLeafStatement(type))
                        unmatchedChildrenStmt.add(child);
                }
            }
        }

        public void traverseDst(){
            if(!mappings.isDstMapped(root))
                unmatchedStmtInWholeDst.add(root);

            processRootDst();

            for(var child:root.getChildren()) {
                var type = child.getType().name;
                if (TreeUtilFunctions.isCompositeStatement(type)) {
                    var compositeStmtInfo = new CompositeStmtInfo(child);
                    compStmtStack.push(compositeStmtInfo);
                    compositeStmtInfo.traverseDst();
                    compStmtStack.pop();
                    child.setMetadata("compositeStmtInfo",compositeStmtInfo);
                } else if (TreeUtilFunctions.isLeafStatement(type)) {
                    var leafStmtInfo = getLeafStmtInfo(child, type);
                    leafStmtInfo.traverseDst();
                    child.setMetadata("leafStmtInfo",leafStmtInfo);

                    if(prevStmtInTraverse!=null){
                        dstNextStmtMap.put(prevStmtInTraverse,child);
                        dstPrevStmtMap.put(child,prevStmtInTraverse);
                    }
                    dstIdenticalStmts.putIfAbsent(child.getMetrics().hash,new ArrayList<>());
                    dstIdenticalStmts.get(child.getMetrics().hash).add(child);
                    prevStmtInTraverse=child;
                }
                if(!mappings.isDstMapped(child)) {
                    if(TreeUtilFunctions.isLeafStatement(type))
                        unmatchedChildrenStmt.add(child);
                }
            }
        }

        private LeafStmtInfo getLeafStmtInfo(Tree child, String type) {
            LeafStmtInfo leafStmtInfo;
            switch(type){
                case Constants.RETURN_STATEMENT -> {
                    leafStmtInfo = new RetStmtInfo(child);
                }
                case Constants.THROW_STATEMENT -> {
                    leafStmtInfo = new ThrowStmtInfo(child);
                }
                default -> {
                    leafStmtInfo = new LeafStmtInfo(child);
                }
            }
            return leafStmtInfo;
        }
    }

    private class MethodDeclInfo{
        Tree root;
        Tree name;
        List<Tree> params;
        Tree block;
        List<Tree> retStmts;
        List<Tree> relExps;

        MethodDeclInfo(Tree root){
            this.root = root;
            this.params = new ArrayList<>();
            this.retStmts = new ArrayList<>();
            this.relExps = new ArrayList<>();
        }

        public void traverseSrc(){
            for(var child:root.getChildren()) {
                tryToFixType(child);
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name=child;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstName = mappings.getDsts(child).iterator().next();
                            if(!child.getLabel().equals(dstName.getLabel())
                                    && dstName.getParent().getType().name.equals(Constants.METHOD_DECLARATION)){
                                renameMap.put(child.getLabel(),dstName.getLabel());
                            }
                        }
                    }
                    case "SingleVariableDeclaration" -> {
                        this.params.add(child);
                        trivalTraverse(child);
                    }

                    case Constants.BLOCK -> {
                        prevStmtInTraverse = null;
                        this.block = child;
                        var compositeStmtInfo = new CompositeStmtInfo(child);
                        compStmtStack.push(compositeStmtInfo);
                        compositeStmtInfo.traverseSrc();
                        compStmtStack.pop();
                        child.setMetadata("compositeStmtInfo",compositeStmtInfo);
                    }
                    default -> {
                        trivalTraverse(child);
                    }
                }
            }
        }

        public void traverseDst(){
            for(var child:root.getChildren()) {
                switch (child.getType().name) {
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case "SingleVariableDeclaration" -> {
                        this.params.add(child);
                    }
                    case Constants.BLOCK -> {
                        prevStmtInTraverse = null;
                        this.block = child;
                        var compositeStmtInfo = new CompositeStmtInfo(child);
                        compStmtStack.push(compositeStmtInfo);
                        compositeStmtInfo.traverseDst();
                        compStmtStack.pop();
                        child.setMetadata("compositeStmtInfo",compositeStmtInfo);
                    }
                }
            }
        }
    }

    private class TypeDeclInfo{
        Tree root;
        Tree name;
        List<Tree> unmatchedMethodDecls;

        TypeDeclInfo(Tree root){
            this.root = root;
            this.unmatchedMethodDecls = new ArrayList<>();
        }

        public void traverseSrc(){
            for(var child:root.getChildren()){
                tryToFixType(child);
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case Constants.METHOD_DECLARATION, Constants.INITIALIZER -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstDecl = mappings.getDsts(child).iterator().next();
                            if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(child,dstDecl);
                                curMappedSrcs.remove(child);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse){
                            var methodDeclInfo = new MethodDeclInfo(child);
                            methodDeclStack.push(methodDeclInfo);
                            methodDeclInfo.traverseSrc();
                            methodDeclStack.pop();
                            child.setMetadata("methodDeclInfo",methodDeclInfo);

                            if(!mappings.isSrcMapped(child))
                                unmatchedMethodDecls.add(child);
                        }
                    }
                    case Constants.TYPE_DECLARATION -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstDecl = mappings.getDsts(child).iterator().next();
                            if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(child,dstDecl);
                                curMappedSrcs.remove(child);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse){
                            var typeDeclInfo = new TypeDeclInfo(child);
                            typeDeclInfo.traverseSrc();
                            child.setMetadata("typeDeclInfo",typeDeclInfo);
                        }
                    }
                    case Constants.ENUM_DECLARATION -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstDecl = mappings.getDsts(child).iterator().next();
                            if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(child,dstDecl);
                                curMappedSrcs.remove(child);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse){
                            var enumDeclInfo = new EnumDeclInfo(child);
                            enumDeclInfo.traverseSrc();
                            child.setMetadata("enumDeclInfo",enumDeclInfo);
                        }
                    }
                    case Constants.FIELD_DECLARATION -> {
                        var fieldDeclInfo = new FieldDeclInfo(child);
                        fieldDeclInfo.traverseSrc();
                        child.setMetadata("fieldDeclInfo",fieldDeclInfo);
                    }
                    default -> {
                        trivalTraverse(child);
                    }
                }
            }
        }

        public void traverseDst(){
            for(var child:root.getChildren()){
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case Constants.METHOD_DECLARATION -> {
                        if(!skipDstDecls.contains(child)){
                            var methodDeclInfo = new MethodDeclInfo(child);
                            methodDeclStack.push(methodDeclInfo);
                            methodDeclInfo.traverseDst();
                            methodDeclStack.pop();
                            child.setMetadata("methodDeclInfo",methodDeclInfo);

                            if(!mappings.isDstMapped(child)){
                                unmatchedMethodDecls.add(child);
                            }
                        }
                    }
                    case Constants.TYPE_DECLARATION -> {
                        if(!skipDstDecls.contains(child)){
                            var typeDeclInfo = new TypeDeclInfo(child);
                            typeDeclInfo.traverseDst();
                            child.setMetadata("typeDeclInfo",typeDeclInfo);
                        }
                    }
                    case Constants.ENUM_DECLARATION -> {
                        if(!skipDstDecls.contains(child)){
                            var enumDeclInfo = new EnumDeclInfo(child);
                            enumDeclInfo.traverseDst();
                            child.setMetadata("enumDeclInfo",enumDeclInfo);
                        }
                    }
                    case Constants.FIELD_DECLARATION -> {
                        var fieldDeclInfo = new FieldDeclInfo(child);
                        fieldDeclInfo.traverseDst();
                        child.setMetadata("fieldDeclInfo",fieldDeclInfo);
                    }
                }
            }
        }
    }

    private class EnumConstDeclInfo {
        Tree root;
        Tree name;

        public EnumConstDeclInfo(Tree root){
            this.root = root;
        }

        public void traverseSrc(){
            for(var child:root.getChildren()){
                tryToFixType(child);
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case Constants.ANONYMOUS_CLASS_DECLARATION -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstDecl = mappings.getDsts(child).iterator().next();
                            if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(child,dstDecl);
                                curMappedSrcs.remove(child);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse) {
                            var typeDeclInfo = new TypeDeclInfo(child);
                            typeDeclInfo.traverseSrc();
                            child.setMetadata("typeDeclInfo", typeDeclInfo);
                        }
                    }
                    default -> {
                        trivalTraverse(child);
                    }
                }
            }
        }

        public void traverseDst(){
            for(var child:root.getChildren()){
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case Constants.ANONYMOUS_CLASS_DECLARATION -> {
                        if(!skipDstDecls.contains(child)){
                            var typeDeclInfo = new TypeDeclInfo(child);
                            typeDeclInfo.traverseDst();
                            child.setMetadata("typeDeclInfo", typeDeclInfo);
                        }
                    }
                }
            }
        }
    }

    private class EnumDeclInfo {
        Tree root;
        Tree name;

        EnumDeclInfo(Tree root){
            this.root = root;
        }

        public void traverseSrc(){
            for(var child:root.getChildren()){
                tryToFixType(child);
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case Constants.ENUM_CONSTANT_DECLARATION -> {
                        var enumConstDeclInfo = new EnumConstDeclInfo(child);
                        enumConstDeclInfo.traverseSrc();
                        child.setMetadata("enumConstDeclInfo", enumConstDeclInfo);
                    }
                    case Constants.METHOD_DECLARATION -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstDecl = mappings.getDsts(child).iterator().next();
                            if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(child,dstDecl);
                                curMappedSrcs.remove(child);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse){
                            var methodDeclInfo = new MethodDeclInfo(child);
                            methodDeclStack.push(methodDeclInfo);
                            methodDeclInfo.traverseSrc();
                            methodDeclStack.pop();
                            child.setMetadata("methodDeclInfo",methodDeclInfo);
                        }
                    }
                    case Constants.TYPE_DECLARATION -> {
                        var needTraverse = true;
                        if(mappings.isSrcMapped(child) && mappings.isSrcUnique(child)){
                            var dstDecl = mappings.getDsts(child).iterator().next();
                            if(child.getMetrics().hash==dstDecl.getMetrics().hash){
                                checkMappingRecursively(child,dstDecl);
                                curMappedSrcs.remove(child);
                                needTraverse = false;
                                skipDstDecls.add(dstDecl);
                            }
                        }
                        if(needTraverse){
                            var typeDeclInfo = new TypeDeclInfo(child);
                            typeDeclInfo.traverseSrc();
                            child.setMetadata("typeDeclInfo",typeDeclInfo);
                        }
                    }
                    case Constants.FIELD_DECLARATION -> {
                        var fieldDeclInfo = new FieldDeclInfo(child);
                        fieldDeclInfo.traverseSrc();
                        child.setMetadata("fieldDeclInfo",fieldDeclInfo);
                    }
                    default -> {
                        trivalTraverse(child);
                    }
                }
            }
        }

        public void traverseDst(){
            for(var child:root.getChildren()){
                switch (child.getType().name){
                    case Constants.SIMPLE_NAME -> {
                        this.name = child;
                    }
                    case Constants.ENUM_CONSTANT_DECLARATION -> {
                        var enumConstDeclInfo = new EnumConstDeclInfo(child);
                        enumConstDeclInfo.traverseDst();
                        child.setMetadata("enumConstDeclInfo",enumConstDeclInfo);
                    }
                    case Constants.METHOD_DECLARATION -> {
                        if(!skipDstDecls.contains(child)){
                            var methodDeclInfo = new MethodDeclInfo(child);
                            methodDeclStack.push(methodDeclInfo);
                            methodDeclInfo.traverseDst();
                            methodDeclStack.pop();
                            child.setMetadata("methodDeclInfo",methodDeclInfo);
                        }
                    }
                    case Constants.TYPE_DECLARATION -> {
                        if(!skipDstDecls.contains(child)){
                            var typeDeclInfo = new TypeDeclInfo(child);
                            typeDeclInfo.traverseDst();
                            child.setMetadata("typeDeclInfo",typeDeclInfo);
                        }
                    }
                    case Constants.FIELD_DECLARATION -> {
                        var fieldDeclInfo = new FieldDeclInfo(child);
                        fieldDeclInfo.traverseDst();
                        child.setMetadata("fieldDeclInfo",fieldDeclInfo);
                    }
                }
            }
        }
    }

    private void checkMappingRecursively(Tree src,Tree dst){
        checkMapping(src,dst);
//        curMappedSrcs.remove(src);
        if (dst.getChildren().size() == src.getChildren().size())
            for (int i = 0; i < src.getChildren().size(); i++)
                checkMappingRecursively(src.getChild(i), dst.getChild(i));
    }

    @Override
    protected List<Tree> getSrcUnmappedChildren(Tree src){
        var srcCompositeStmtInfo = (CompositeStmtInfo)src.getMetadata("compositeStmtInfo");
        return srcCompositeStmtInfo==null?
                src.getChildren().stream().filter(child -> !mappings.isSrcMapped(child)).toList():
                srcCompositeStmtInfo.unmatchedChildrenStmt;
    }

    @Override
    protected List<Tree> getDstUnmappedChildren(Tree dst){
        var dstCompositeStmtInfo = (CompositeStmtInfo)dst.getMetadata("compositeStmtInfo");
        return dstCompositeStmtInfo==null?
                dst.getChildren().stream().filter(child -> !mappings.isDstMapped(child)).toList():
                dstCompositeStmtInfo.unmatchedChildrenStmt;
    }

    @Override
    protected long getSrcUnmappedDestRetStmtCount(Tree src){
        var srcCompositeStmtInfo = (CompositeStmtInfo)src.getMetadata("compositeStmtInfo");
        return srcCompositeStmtInfo==null?
                getLeafStmts(src).stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.RETURN_STATEMENT)).count():
                srcCompositeStmtInfo.unmatchedDescRetStmt.size();
    }

    @Override
    protected long getDstUnmappedDestRetStmtCount(Tree dst){
        var dstCompositeStmtInfo = (CompositeStmtInfo)dst.getMetadata("compositeStmtInfo");
        return dstCompositeStmtInfo==null?
                getLeafStmts(dst).stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.RETURN_STATEMENT)).count():
                dstCompositeStmtInfo.unmatchedDescRetStmt.size();
    }

    @Override
    protected long getSrcUnmappedDestThrowStmtCount(Tree src){
        var srcCompositeStmtInfo = (CompositeStmtInfo)src.getMetadata("compositeStmtInfo");
        return srcCompositeStmtInfo==null?
                getLeafStmts(src).stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.THROW_STATEMENT)).count():
                srcCompositeStmtInfo.unmatchedDescThrowStmt.size();
    }

    @Override
    protected long getDstUnmappedDestThrowStmtCount(Tree dst){
        var dstCompositeStmtInfo = (CompositeStmtInfo)dst.getMetadata("compositeStmtInfo");
        return dstCompositeStmtInfo==null?
                getLeafStmts(dst).stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.THROW_STATEMENT)).count():
                dstCompositeStmtInfo.unmatchedDescThrowStmt.size();
    }

    @Override
    protected List<Tree> getSrcUnmappedMethodDecls(Tree src){
        var srcTypeDeclInfo = (TypeDeclInfo)src.getMetadata("typeDeclInfo");
        return srcTypeDeclInfo==null?
                src.getChildren().stream().filter(child -> !mappings.isSrcMapped(child) && child.getType().name.equals(Constants.METHOD_DECLARATION)).toList():
                srcTypeDeclInfo.unmatchedMethodDecls;
    }

    @Override
    protected List<Tree> getDstUnmappedMethodDecls(Tree dst){
        var dstTypeDeclInfo = (TypeDeclInfo)dst.getMetadata("typeDeclInfo");
        return dstTypeDeclInfo==null?
                dst.getChildren().stream().filter(child -> !mappings.isDstMapped(child) && child.getType().name.equals(Constants.METHOD_DECLARATION)).toList():
                dstTypeDeclInfo.unmatchedMethodDecls;
    }

    @Override
    protected List<Tree> getSrcUnmappedDestStmts(){
        return unmatchedStmtInWholeSrc.stream().filter(stmt -> !mappings.isSrcMapped(stmt)).toList();
    }

    @Override
    protected List<Tree> getDstUnmappedDestStmts(){
        return unmatchedStmtInWholeDst.stream().filter(stmt -> !mappings.isDstMapped(stmt)).toList();
    }

    @Override
    protected List<Tree> getSrcUnmappedImports(){
        return unMappedSrcImports;
    }

    @Override
    protected List<Tree> getDstUnmappedImports(){
        return unMappedDstImports;
    }

    @Override
    protected List<Tree> getSrcCalls(Tree src){
        var srcCompositeStmtInfo = (CompositeStmtInfo) src.getMetadata("compositeStmtInfo");
        return srcCompositeStmtInfo==null?
                TreeUtils.preOrder(src).stream().filter(
                        t ->t.getType().name.equals(Constants.METHOD_INVOCATION) &&
                                (!mappings.isSrcMapped(t) && !mappings.isSrcMappedConsideringSubTrees(t)
                                        && innerCallCheck(t) && innerCallCheck2(t,true)
                                        || maybeBadMappings.isSrcMapped(t))).toList():
                srcCompositeStmtInfo.methodCalls.stream().filter(t->
                        !mappings.isSrcMapped(t) && !mappings.isSrcMappedConsideringSubTrees(t)
                                && innerCallCheck(t) && innerCallCheck2(t,true)
                                || maybeBadMappings.isSrcMapped(t)
                ).toList();
    }

    @Override
    protected List<Tree> getDstCalls(Tree dst){
        var dstCompositeStmtInfo = (CompositeStmtInfo) dst.getMetadata("compositeStmtInfo");
        return dstCompositeStmtInfo==null?
                TreeUtils.preOrder(dst).stream().filter(
                        t -> t.getType().name.equals(Constants.METHOD_INVOCATION) &&
                                (!mappings.isDstMapped(t) && !mappings.isDstMappedConsideringSubTrees(t)
                                        && innerCallCheck(t) && innerCallCheck2(t,false)
                                        || maybeBadMappings.isDstMapped(t))).toList():
                dstCompositeStmtInfo.methodCalls.stream().filter(t->
                        !mappings.isDstMapped(t) && !mappings.isDstMappedConsideringSubTrees(t)
                                && innerCallCheck(t) && innerCallCheck2(t,false)
                                || maybeBadMappings.isDstMapped(t)
                ).toList();
    }

    @Override
    protected List<Tree> getSrcCalls2(Tree src){
        var srcCompositeStmtInfo = (CompositeStmtInfo) src.getMetadata("compositeStmtInfo");
        var srcBlocks = srcCompositeStmtInfo==null?
                TreeUtils.preOrder(src).stream().filter(t -> t.getType().name.equals(Constants.BLOCK)).toList():
                srcCompositeStmtInfo.blocks;

        if(srcBlocks.size() <=2){
            return srcCompositeStmtInfo==null?
                    TreeUtils.preOrder(src).stream().filter(
                            t -> t.getType().name.equals(Constants.METHOD_INVOCATION) && !mappings.isSrcMapped(t)
                    ).toList():
                    srcCompositeStmtInfo.methodCalls.stream().filter(t->!mappings.isSrcMapped(t)).toList();
        }
        return new ArrayList<>();
    }

    @Override
    protected List<Tree> getDstCalls2(Tree dst){
        var dstCompositeStmtInfo = (CompositeStmtInfo) dst.getMetadata("compositeStmtInfo");
        var dstBlocks = dstCompositeStmtInfo==null?
                TreeUtils.preOrder(dst).stream().filter(t -> t.getType().name.equals(Constants.BLOCK)).toList():
                dstCompositeStmtInfo.blocks;
        if(dstBlocks.size() <=2){
            return dstCompositeStmtInfo==null?
                    TreeUtils.preOrder(dst).stream().filter(
                            t -> t.getType().name.equals(Constants.METHOD_INVOCATION) && !mappings.isDstMapped(t)
                    ).toList():
                    dstCompositeStmtInfo.methodCalls.stream().filter(t->!mappings.isDstMapped(t)).toList();
        }
        return new ArrayList<>();
    }

    @Override
    protected List<Tree> getSrcReturns(Tree src){
        var srcMethodDeclInfo = (MethodDeclInfo)src.getMetadata("methodDeclInfo");
        return srcMethodDeclInfo==null?
                getLeafStmts(src).stream().filter(t-> t.getType().name.equals(Constants.RETURN_STATEMENT)).toList():
                srcMethodDeclInfo.retStmts;
    }

    @Override
    protected List<Tree> getDstReturns(Tree dst){
        var dstMethodDeclInfo = (MethodDeclInfo)dst.getMetadata("methodDeclInfo");
        return dstMethodDeclInfo==null?
                getLeafStmts(dst).stream().filter(t-> t.getType().name.equals(Constants.RETURN_STATEMENT)).toList():
                dstMethodDeclInfo.retStmts;
    }

    @Override
    protected List<Tree> getSrcUnmappedRelExps(Tree src){
        var srcMethodDeclInfo = (MethodDeclInfo)src.getMetadata("methodDeclInfo");
        return srcMethodDeclInfo==null?
                TreeUtils.preOrder(src).stream().filter(
                        t-> !mappings.isSrcMapped(t) && t.getType().name.equals(Constants.INFIX_EXPRESSION) && isRelationalOperator(t.getChild(1))).toList():
                srcMethodDeclInfo.relExps.stream().filter(t->!mappings.isSrcMapped(t)).toList();
    }

    @Override
    protected List<Tree> getDstUnmappedRelExps(Tree dst){
        var dstMethodDeclInfo = (MethodDeclInfo)dst.getMetadata("methodDeclInfo");
        return dstMethodDeclInfo==null?
                TreeUtils.preOrder(dst).stream().filter(
                        t-> !mappings.isDstMapped(t) && t.getType().name.equals(Constants.INFIX_EXPRESSION) && isRelationalOperator(t.getChild(1))).toList():
                dstMethodDeclInfo.relExps.stream().filter(t->!mappings.isDstMapped(t)).toList();
    }
}
