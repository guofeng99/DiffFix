package benchmark.metrics.computers.vanilla;

import benchmark.metrics.computers.BaseBenchmarkComputer;
import benchmark.metrics.models.BaseDiffComparisonResult;
import benchmark.generators.tools.ASTDiffTool;
import benchmark.utils.CaseInfo;
import benchmark.utils.Configuration.Configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static benchmark.utils.PathResolver.exportedFolderPathByCaseInfo;
import static benchmark.utils.PathResolver.getPaths;

/* Created by pourya on 2023-12-04 2:19 p.m. */
public class CommitPerfectRatioBenchmarkComputer extends BaseBenchmarkComputer {


    public CommitPerfectRatioBenchmarkComputer(Configuration configuration) {
        super(configuration);
    }

    public Map<ASTDiffTool, Integer> perfectRatio() throws IOException {
        Map<ASTDiffTool, Integer> result = new LinkedHashMap<>();
        for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
            result.put(tool, 0);
        }
        for (CaseInfo info : getConfiguration().getAllCases()) {
            String folderPath = exportedFolderPathByCaseInfo(info);
            Path dir = Paths.get(getConfiguration().getOutputFolder() + folderPath  + "/");
            System.out.println("Generating benchmark stats for " + info.getRepo() + " " + info.getCommit());
            List<Path> paths = getPaths(dir, 1);
            for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
                boolean miss = false;
                for (Path dirPath : paths) {
                    String toolPath = tool.name();
                    String godFullPath = dirPath.resolve(ASTDiffTool.GOD.name() + ".json").toString();
                    String toolFullPath = godFullPath.replace(ASTDiffTool.GOD.name(), toolPath);
                    if (!areFileContentsEqual(Paths.get(godFullPath), Paths.get(toolFullPath))) {
                        miss = true;
                        break;
                    }
                }
                if (!miss) result.put(tool, result.getOrDefault(tool, 0) + 1);
            }
        }
        return result;
    }

    public Map<ASTDiffTool, Integer> perfectRatio2File(String oracleName,String toolName,String version) throws IOException {
        Map<ASTDiffTool, Integer> result = new LinkedHashMap<>();
        for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
            result.put(tool, 0);
        }
        List<String> repoCommitFile = new ArrayList<>();
        for (CaseInfo info : getConfiguration().getAllCases()) {
            String folderPath = exportedFolderPathByCaseInfo(info);
            Path dir = Paths.get(getConfiguration().getOutputFolder() + folderPath  + "/");
            System.out.println("Generating benchmark stats for " + info.getRepo() + " " + info.getCommit());
            List<Path> paths = getPaths(dir, 1);
            for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
                String toolPath = tool.name();
                if (!toolPath.equals(toolName)) continue;
                boolean miss = false;
                for (Path dirPath : paths) {
                    String godFullPath = dirPath.resolve(ASTDiffTool.GOD.name() + ".json").toString();
                    String toolFullPath = godFullPath.replace(ASTDiffTool.GOD.name(), toolPath);
                    if (!areFileContentsEqual(Paths.get(godFullPath), Paths.get(toolFullPath))) {
                        miss = true;
                        System.out.println("miss in " + info.getRepo() + "/" + info.getCommit()+": "+dirPath.getFileName());
                        repoCommitFile.add(info.getRepo() + "/" + info.getCommit()+": "+dirPath.getFileName());
                    }
                }
                if (!miss) result.put(tool, result.getOrDefault(tool, 0) + 1);
            }
        }
        File file = toolName.endsWith("_Mod")?
                new File("out/"+oracleName+"-miss-file-"+toolName.replace("_Mod","")+"-"+version+".txt"):
                    toolName.endsWith("_MT")?
                new File("out/"+oracleName+"-miss-file-"+toolName.replace("_MT","-MoveOpt")+".txt"):
                new File("out/"+oracleName+"-miss-file-"+toolName+".txt");
////        File file = new File("out/"+oracleName+"-miss-file-"+toolName.replace("_Mod","")+"-"+version+".txt");
//        File file = new File("out/"+oracleName+"-miss-file-"+toolName+".txt");
////        File file = new File("out/"+oracleName+"-miss-file-"+toolName.replace("_MT","-MoveOpt")+".txt");
        BufferedWriter bf = null;
        // write repoCommit to file with try/catch
        try{
            bf = new BufferedWriter(new FileWriter(file));
            for (String s : repoCommitFile) {
                bf.write(s);
                bf.newLine();
            }
            bf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public Map<ASTDiffTool, Integer> perfectRatio2(String oracleName,String toolName,String version) throws IOException {
        Map<ASTDiffTool, Integer> result = new LinkedHashMap<>();
        for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
            result.put(tool, 0);
        }
        List<String> repoCommit = new ArrayList<>();
        for (CaseInfo info : getConfiguration().getAllCases()) {
            String folderPath = exportedFolderPathByCaseInfo(info);
            Path dir = Paths.get(getConfiguration().getOutputFolder() + folderPath  + "/");
            System.out.println("Generating benchmark stats for " + info.getRepo() + " " + info.getCommit());
            List<Path> paths = getPaths(dir, 1);
            for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
                String toolPath = tool.name();
                if (!toolPath.equals(toolName)) continue;
                boolean miss = false;
                for (Path dirPath : paths) {
                    String godFullPath = dirPath.resolve(ASTDiffTool.GOD.name() + ".json").toString();
                    String toolFullPath = godFullPath.replace(ASTDiffTool.GOD.name(), toolPath);
                    if (!areFileContentsEqual(Paths.get(godFullPath), Paths.get(toolFullPath))) {
                        miss = true;
                        System.out.println("miss in " + info.getRepo() + "/" + info.getCommit());
                        repoCommit.add(info.getRepo() + "/" + info.getCommit());
                        break;
                    }
                }
                if (!miss) result.put(tool, result.getOrDefault(tool, 0) + 1);
            }
        }
        File file = toolName.endsWith("_Mod")?
                new File("out/"+oracleName+"-miss-"+toolName.replace("_Mod","")+"-"+version+".txt"):
                toolName.endsWith("_MT")?
                        new File("out/"+oracleName+"-miss-"+toolName.replace("_MT","-MoveOpt")+".txt"):
                        new File("out/"+oracleName+"-miss-"+toolName+".txt");
        BufferedWriter bf = null;
        // write repoCommit to file with try/catch
        try{
            bf = new BufferedWriter(new FileWriter(file));
            for (String s : repoCommit) {
                bf.write(s);
                bf.newLine();
            }
            bf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Map<ASTDiffTool, Integer> perfectRatioRev(String oracleName,String toolName,String version) throws IOException {
        Map<ASTDiffTool, Integer> result = new LinkedHashMap<>();
        for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
            result.put(tool, 0);
        }
        List<String> repoCommit = new ArrayList<>();
        for (CaseInfo info : getConfiguration().getAllCases()) {
            String folderPath = exportedFolderPathByCaseInfo(info);
            Path dir = Paths.get(getConfiguration().getOutputFolder() + folderPath  + "/");
            System.out.println("Generating benchmark stats for " + info.getRepo() + " " + info.getCommit());
            List<Path> paths = getPaths(dir, 1);
            for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
                String toolPath = tool.name();
                if (!toolPath.equals(toolName)) continue;
                boolean miss = false;
                for (Path dirPath : paths) {
                    String godFullPath = dirPath.resolve(ASTDiffTool.GOD_Rev.name() + ".json").toString();
                    String toolFullPath = godFullPath.replace(ASTDiffTool.GOD_Rev.name(), toolPath);
                    if (!areFileContentsEqual(Paths.get(godFullPath), Paths.get(toolFullPath))) {
                        miss = true;
                        System.out.println("miss in " + info.getRepo() + "/" + info.getCommit());
                        repoCommit.add(info.getRepo() + "/" + info.getCommit());
                        break;
                    }
                }
                if (!miss) result.put(tool, result.getOrDefault(tool, 0) + 1);
            }
        }
        File file = new File("out/"+oracleName+"-miss-"+toolName.replace("_Mod","").replace("_Rev","-rev")+"-"+version+".txt");
//        File file = new File("out/"+oracleName+"-miss-"+toolName.replace("_Mod","").replace("_Rev","-rev")+".txt");
        BufferedWriter bf = null;
        // write repoCommit to file with try/catch
        try{
            bf = new BufferedWriter(new FileWriter(file));
            for (String s : repoCommit) {
                bf.write(s);
                bf.newLine();
            }
            bf.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public Map<ASTDiffTool, Integer> consistenceRatio(String oracleName) throws IOException {
        Map<ASTDiffTool, Integer> result = new LinkedHashMap<>();
        for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
            result.put(tool, 0);
        }
        Map<String,List<String>> repoCommit = new HashMap<>();
        for (CaseInfo info : getConfiguration().getAllCases()) {
            String folderPath = exportedFolderPathByCaseInfo(info);
            Path dir = Paths.get(getConfiguration().getOutputFolder() + folderPath  + "/");
            System.out.println("Generating benchmark stats for " + info.getRepo() + " " + info.getCommit());
            List<Path> paths = getPaths(dir, 1);
            for (ASTDiffTool tool : getConfiguration().getActiveTools()) {
//                if (!(tool.equals(ASTDiffTool.GTS_Mod) || tool.equals(ASTDiffTool.RMD_Mod))) continue;
//                if(!(tool.equals(ASTDiffTool.IAM_Mod) || tool.equals(ASTDiffTool.GTG_Mod))) continue;
                boolean miss = false;
                for (Path dirPath : paths) {
                    String toolPath = tool.name();
                    String revPath = tool.name()+"_Rev";
                    String toolFullPath = dirPath.resolve(toolPath + ".json").toString();
                    String revFullPath = dirPath.resolve(revPath + ".json").toString();
                    if (!areFileSizeEqual(Paths.get(revFullPath), Paths.get(toolFullPath))) {
                        miss = true;
                        System.out.println("miss in " + info.getRepo() + "/" + info.getCommit());
                        repoCommit.putIfAbsent(toolPath, new ArrayList<>());
                        repoCommit.get(toolPath).add(info.getRepo() + "/" + info.getCommit());
                        break;
                    }
                }
                if (!miss) result.put(tool, result.getOrDefault(tool, 0) + 1);
            }
        }

        for(var entry : repoCommit.entrySet()){
            File file = new File("out/"+oracleName+"-consistence-"+entry.getKey()+".txt");
            BufferedWriter bf = null;
            try{
                bf = new BufferedWriter(new FileWriter(file));
                for (String s : entry.getValue()) {
                    bf.write(s);
                    bf.newLine();
                }
                bf.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static boolean areFileContentsEqual(Path file1, Path file2) throws IOException {
        byte[] content1 = Files.readAllBytes(file1);
        byte[] content2 = Files.readAllBytes(file2);
        return Arrays.equals(content1, content2);
    }

    private static boolean areFileSizeEqual(Path file1, Path file2) throws IOException {
        return Files.size(file1) == Files.size(file2);
    }

    @Override
    public Collection<? extends BaseDiffComparisonResult> compute() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<? extends BaseDiffComparisonResult> compute(CaseInfo info) throws IOException {
        throw new UnsupportedOperationException();
    }
}
