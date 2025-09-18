package org.refactoringminer.astDiff.matchers;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;

import gr.uom.java.xmi.diff.ReplaceAnonymousWithClassRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.actions.editscript.SimplifiedExtendedChawatheScriptGenerator;
import org.refactoringminer.astDiff.matchers.vanilla.MissingIdenticalSubtree;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.OptimizationData;
import org.refactoringminer.astDiff.moved.AllSubTreesMovedASTDiffGenerator;
import org.refactoringminer.astDiff.moved.MovedASTDiffGenerator;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.matchers.wrappers.*;
import org.refactoringminer.astDiff.utils.TreeUtilFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.refactoringminer.astDiff.utils.Helpers.findAppends;
import static org.refactoringminer.astDiff.utils.Helpers.findTreeContexts;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class ProjectASTDiffer
{
	private final static Logger logger = LoggerFactory.getLogger(ProjectASTDiffer.class);
	private final UMLModelDiff modelDiff;
	private List<Refactoring> modelDiffRefactorings;
	private final ProjectASTDiff projectASTDiff;
	private final MovedASTDiffGenerator movedDeclarationGenerator;
	private final Map<ASTDiff, OptimizationData> optimizationDataMap = new HashMap<>();

	public ProjectASTDiffer(UMLModelDiff modelDiff, Map<String, String> fileContentsBefore, Map<String, String> fileContentsAfter) throws RefactoringMinerTimedOutException {
		this.modelDiff = modelDiff;
		this.projectASTDiff = new ProjectASTDiff(fileContentsBefore, fileContentsAfter);
//		movedDeclarationGenerator = new MovedDeclarationGenerator(modelDiff, projectASTDiff);
		movedDeclarationGenerator = new AllSubTreesMovedASTDiffGenerator(modelDiff, projectASTDiff);
		diff();
	}

	public ProjectASTDiff getProjectASTDiff() {
		return projectASTDiff;
	}

	//kept for backward compatibility, but getProjectASTDiff() should be used instead
	public Set<ASTDiff> getDiffSet() {
		return projectASTDiff.getDiffSet();
	}

	private void diff() throws RefactoringMinerTimedOutException {
		buildDefUse();
		setComment();
		long start = System.currentTimeMillis();
		this.modelDiffRefactorings = modelDiff.getRefactorings();
		long finish = System.currentTimeMillis();
		logger.info("ModelDiff execution (part2): " + (finish - start) + " milliseconds");
		projectASTDiff.setRefactorings(this.modelDiffRefactorings);
		projectASTDiff.setModelDiff(modelDiff);
		projectASTDiff.setParentContextMap(modelDiff.getParentModel().getTreeContextMap());
		projectASTDiff.setChildContextMap(modelDiff.getChildModel().getTreeContextMap());
		long diff_execution_started = System.currentTimeMillis();
		makeASTDiff(modelDiff.getCommonClassDiffList(),false);
		makeASTDiff(withCorrectOrder(modelDiff.getClassRenameDiffList()),false);
		makeASTDiff(withCorrectOrder(modelDiff.getClassMoveDiffList()),false);
		makeASTDiff(modelDiff.getInnerClassMoveDiffList(),true);
		makeASTDiff(getExtraDiffs(),true);
		//Process the ModelDiffRefactorings once at the end
		UnifiedModelDiffRefactoringsMatcher unifiedModelDiffRefactoringsMatcher = new UnifiedModelDiffRefactoringsMatcher(projectASTDiff.getDiffSet(), optimizationDataMap, modelDiff, modelDiffRefactorings);
		processAllOptimizations(unifiedModelDiffRefactoringsMatcher.getNewlyGeneratedDiffsOptimizationMap());
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			new MissingIdenticalSubtree().match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
		long diff_execution_finished =  System.currentTimeMillis();
		logger.info("Diff execution: " + (diff_execution_finished - diff_execution_started) + " milliseconds");

		if(FixingMatcher.useFix){
			FixingMatcher.setInfos(true);
			long diff_fixing_started = System.currentTimeMillis();
			for (ASTDiff diff : projectASTDiff.getDiffSet()) {
				new FixingMatcher().match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
			}
			long diff_fixing_finished =  System.currentTimeMillis();
			logger.info("Fixing execution: " + (diff_fixing_finished - diff_fixing_started) + " milliseconds");
		}

		long movedDiff_execution_started =  System.currentTimeMillis();
		computeAllEditScripts();
		projectASTDiff.addMoveASTDiff(unifiedModelDiffRefactoringsMatcher.getNewlyGeneratedDiffsOptimizationMap().keySet());
		projectASTDiff.addMoveASTDiff(movedDeclarationGenerator.make());
		long movedDiff_execution_finished =  System.currentTimeMillis();
		logger.info("MovedDiff execution: " + (movedDiff_execution_finished - movedDiff_execution_started) + " milliseconds");
		computeMovedDiffsEditScripts();
	}

	private void buildDefUse(){
		// add: def use
		var srcModel = modelDiff.getParentModel();
		for (var srcClass : srcModel.getClassList()){
			if(modelDiff.getParentModel().getTreeContextMap().get(srcClass.getSourceFile())==null) continue;
			var srcTree = modelDiff.getParentModel().getTreeContextMap().get(srcClass.getSourceFile()).getRoot();
			for(var vd: srcClass.getFieldDeclarationMap().values()){
				Tree vdTree = TreeUtilFunctions.findByLocationInfo(srcTree, vd.getLocationInfo());
				if(vdTree!=null){
					vdTree.setMetadata("use",new ArrayList<>());
					for(var usedstatement : vd.getStatementsInScopeUsingVariable()){
						for(var usedvar : usedstatement.getVariables()){
							if (usedvar.getVariableDeclaration() == vd){
								Tree usedNode = TreeUtilFunctions.findByLocationInfo(srcTree, usedvar.getLocationInfo());
								if(usedNode!=null){
									if(!usedNode.isLeaf())
										usedNode=usedNode.postOrder().iterator().next();
									usedNode.setMetadata("decl",vdTree);
									((List<Tree>)vdTree.getMetadata("use")).add(usedNode);
								}
							}
						}
					}
				}
			}
			for (var srcMethod : srcClass.getOperations()){
				srcMethod.getAllVariableDeclarations().forEach(vd -> {
					Tree vdTree = TreeUtilFunctions.findByLocationInfo(srcTree, vd.getLocationInfo());
					if(vdTree!=null){
						vdTree.setMetadata("use",new ArrayList<>());
						for(var usedstatement : vd.getStatementsInScopeUsingVariable()){
							for(var usedvar : usedstatement.getVariables()){
								if (usedvar.getVariableDeclaration() == vd){
									Tree usedNode = TreeUtilFunctions.findByLocationInfo(srcTree, usedvar.getLocationInfo());
									if(usedNode!=null){
										if(!usedNode.isLeaf())
											usedNode=usedNode.postOrder().iterator().next();
										usedNode.setMetadata("decl",vdTree);
										((List<Tree>)vdTree.getMetadata("use")).add(usedNode);
									}
								}
							}
						}
					}
				});
			}
		}

		var dstModel = modelDiff.getChildModel();
		for (var dstClass : dstModel.getClassList()){
			if(modelDiff.getChildModel().getTreeContextMap().get(dstClass.getSourceFile())==null) continue;
			var dstTree = modelDiff.getChildModel().getTreeContextMap().get(dstClass.getSourceFile()).getRoot();
			for (var vd : dstClass.getFieldDeclarationMap().values()){
				Tree vdTree = TreeUtilFunctions.findByLocationInfo(dstTree, vd.getLocationInfo());
				if(vdTree!=null){
					vdTree.setMetadata("use",new ArrayList<>());
					for(var usedstatement : vd.getStatementsInScopeUsingVariable()){
						for(var usedvar : usedstatement.getVariables()){
							if (usedvar.getVariableDeclaration() == vd){
								Tree usedNode = TreeUtilFunctions.findByLocationInfo(dstTree, usedvar.getLocationInfo());
								if(usedNode!=null){
									if(!usedNode.isLeaf())
										usedNode=usedNode.postOrder().iterator().next();
									usedNode.setMetadata("decl",vdTree);
									((List<Tree>)vdTree.getMetadata("use")).add(usedNode);
								}
							}
						}
					}
				}
			}
			for (var dstMethod : dstClass.getOperations()){
				dstMethod.getAllVariableDeclarations().forEach(vd -> {
					Tree vdTree = TreeUtilFunctions.findByLocationInfo(dstTree, vd.getLocationInfo());
					if(vdTree!=null){
						vdTree.setMetadata("use",new ArrayList<>());
						for(var usedstatement : vd.getStatementsInScopeUsingVariable()){
							for(var usedvar : usedstatement.getVariables()){
								if (usedvar.getVariableDeclaration() == vd){
									Tree usedNode = TreeUtilFunctions.findByLocationInfo(dstTree, usedvar.getLocationInfo());
									if(usedNode!=null){
										if(!usedNode.isLeaf())
											usedNode=usedNode.postOrder().iterator().next();
										usedNode.setMetadata("decl",vdTree);
										((List<Tree>)vdTree.getMetadata("use")).add(usedNode);
									}
								}
							}
						}
					}
				});
			}
		}
	}

	private void setComment(){
		var srcModel = modelDiff.getParentModel();
		for (var srcClass : srcModel.getClassList()) {
			if(modelDiff.getParentModel().getTreeContextMap().get(srcClass.getSourceFile())==null) continue;
			var srcTree = modelDiff.getParentModel().getTreeContextMap().get(srcClass.getSourceFile()).getRoot();
			for (var srcMethod : srcClass.getOperations()) {
				if(srcMethod.getBody()!=null && srcMethod.getBody().getCompositeStatement()!=null){
					var comments = srcMethod.getComments();
					srcMethod.getBody().getCompositeStatement().getStatements().forEach(s -> {
						Tree sTree = TreeUtilFunctions.findByLocationInfo(srcTree, s.getLocationInfo());
						if(sTree!=null){
							s.getLeaves().forEach(l -> {
								Tree lTree = TreeUtilFunctions.findByLocationInfo(sTree, l.getLocationInfo());
								if(lTree!=null){
//									lTree.setMetadata("str",l.getString());
									for(var comment: comments){
										if(l.getLocationInfo().nextLine(comment.getLocationInfo())){
											lTree.setMetadata("pre-comment",comment.getText());
											break;
										}
										else if(l.getLocationInfo().subsumes(comment.getLocationInfo())){
											lTree.setMetadata("inline-comment",comment.getText());
											break;
										}
									}
								}
							});
						}
					});
				}
			}
		}

		var dstModel = modelDiff.getChildModel();
		for (var dstClass : dstModel.getClassList()) {
			if(modelDiff.getChildModel().getTreeContextMap().get(dstClass.getSourceFile())==null) continue;
			var dstTree = modelDiff.getChildModel().getTreeContextMap().get(dstClass.getSourceFile()).getRoot();
			for (var dstMethod : dstClass.getOperations()) {
				if(dstMethod.getBody()!=null && dstMethod.getBody().getCompositeStatement()!=null){
					var comments = dstMethod.getComments();
					dstMethod.getBody().getCompositeStatement().getStatements().forEach(s -> {
						Tree sTree = TreeUtilFunctions.findByLocationInfo(dstTree, s.getLocationInfo());
						if(sTree!=null){
							s.getLeaves().forEach(l -> {
								Tree lTree = TreeUtilFunctions.findByLocationInfo(sTree, l.getLocationInfo());
								if(lTree!=null){
//									lTree.setMetadata("str",l.getString());
									for(var comment: comments){
										if(l.getLocationInfo().nextLine(comment.getLocationInfo())){
											lTree.setMetadata("pre-comment",comment.getText());
											break;
										}
										else if(l.getLocationInfo().subsumes(comment.getLocationInfo())){
											lTree.setMetadata("inline-comment",comment.getText());
											break;
										}
									}
								}
							});
						}
					});
				}
			}
		}
	}

	private void processAllOptimizations(Map<ASTDiff, OptimizationData> newlyGeneratedDiffMap) {
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			new ASTDiffMappingOptimizer(optimizationDataMap.get(diff), diff, modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap()).
					match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
		for (Map.Entry<ASTDiff, OptimizationData> astDiffOptimizationDataEntry : newlyGeneratedDiffMap.entrySet()) {
			ASTDiff diff = astDiffOptimizationDataEntry.getKey();
			OptimizationData optimizationData = astDiffOptimizationDataEntry.getValue();
			new ASTDiffMappingOptimizer(optimizationData, diff, modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap()).
					match(diff.src.getRoot(), diff.dst.getRoot(), diff.getAllMappings());
		}
	}

	private List<? extends UMLAbstractClassDiff> withCorrectOrder(List<? extends UMLAbstractClassDiff> umlDiffs) {
		ArrayList<UMLAbstractClassDiff> result = new ArrayList<>(umlDiffs);
		Set<UMLAbstractClassDiff> seen = new HashSet<>();
		for (UMLAbstractClassDiff umlDiff : umlDiffs) {
			UMLAbstractClassDiff found = findDiffWith(result, umlDiff.getOriginalClassName(), umlDiff.getNextClassName());
			if (found != null && !seen.contains(found))
			{
				seen.add(found);
				result.remove(found);
				result.add(0, found);
			}
		}
		return result;
	}

	private UMLAbstractClassDiff findDiffWith(ArrayList<? extends UMLAbstractClassDiff> result, String originalClassName, String nextClassName) {
		for (UMLAbstractClassDiff umlAbstractClassDiff : result) {
			if (umlAbstractClassDiff.getOriginalClassName().equals(originalClassName)
				&&
				umlAbstractClassDiff.getNextClassName().equals(nextClassName))
				return umlAbstractClassDiff;
		}
		return null;
	}

	private List<? extends UMLAbstractClassDiff> getExtraDiffs() {
		List<UMLAbstractClassDiff> extraDiffs = new ArrayList<>();
		for (Refactoring modelDiffRefactoring : modelDiffRefactorings) {
			if (modelDiffRefactoring.getRefactoringType() == RefactoringType.REPLACE_ANONYMOUS_WITH_CLASS)
			{
				ReplaceAnonymousWithClassRefactoring replaceAnonymousWithClassRefactoring = (ReplaceAnonymousWithClassRefactoring) modelDiffRefactoring;
				extraDiffs.add(replaceAnonymousWithClassRefactoring.getDiff());
			}
		}
		return extraDiffs;
	}

	private void computeAllEditScripts() {
		long editScript_start = System.currentTimeMillis();
		for (ASTDiff diff : projectASTDiff.getDiffSet()) {
			diff.computeEditScript(modelDiff.getParentModel().getTreeContextMap(), modelDiff.getChildModel().getTreeContextMap(), new SimplifiedExtendedChawatheScriptGenerator());
		}
		long editScript_end = System.currentTimeMillis();
		logger.info("EditScript execution: " + (editScript_end - editScript_start) + " milliseconds");
	}

	private void computeMovedDiffsEditScripts() {
		for (ASTDiff diff : projectASTDiff.getMoveDiffSet()) {
			Tree srcRoot = diff.src.getRoot();
			Tree dstRoot = diff.dst.getRoot();
			diff.getAllMappings().addMapping(srcRoot, dstRoot); //This helps Chawathe to generate the editscript properly, however the mapping is actually incorrect
			diff.computeEditScript(null, null, new SimplifiedExtendedChawatheScriptGenerator());
			diff.getAllMappings().removeMapping(srcRoot, dstRoot); //Removes the mapping that was added to help Chawathe
		}
	}

	private void makeASTDiff(List<? extends UMLAbstractClassDiff> umlClassBaseDiffList, boolean mergeFlag){
		for (UMLAbstractClassDiff classDiff : umlClassBaseDiffList) {
			Collection<ASTDiff> appends = findAppends(projectASTDiff.getDiffSet(), classDiff.getOriginalClass().getSourceFile(), classDiff.getNextClass().getSourceFile());
			boolean decision = (!appends.isEmpty()) || mergeFlag;
			ASTDiff classASTDiff = process(classDiff, findTreeContexts(modelDiff, classDiff), decision);
			if (!appends.isEmpty()) {
				for (ASTDiff append : appends) {
					append.getAllMappings().mergeMappings(classASTDiff.getAllMappings());
				}
			}
			else {
				projectASTDiff.addASTDiff(classASTDiff);
			}
		}
	}

	private ASTDiff process(UMLAbstractClassDiff classDiff, Pair<TreeContext, TreeContext> treeContextPair,boolean mergeFlag){
		Tree srcTree = treeContextPair.first.getRoot();
		Tree dstTree = treeContextPair.second.getRoot();
		ExtendedMultiMappingStore mappingStore = new ExtendedMultiMappingStore(srcTree,dstTree);
		ASTDiff astDiff = new ASTDiff(classDiff.getOriginalClass().getLocationInfo().getFilePath(),
				classDiff.getNextClass().getLocationInfo().getFilePath(),
				treeContextPair.first,
				treeContextPair.second,
				mappingStore);
		optimizationDataMap.putIfAbsent(astDiff,
			new OptimizationData(new ArrayList<>(),
			new ExtendedMultiMappingStore(srcTree,dstTree)));
		OptimizationData optimizationData = optimizationDataMap.get(astDiff);
		new ClassDiffMatcher(optimizationData, classDiff, mergeFlag, modelDiffRefactorings).match(srcTree, dstTree, mappingStore);

		return astDiff;
	}
}
