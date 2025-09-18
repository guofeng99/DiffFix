package rq;

import benchmark.metrics.computers.vanilla.CommitPerfectRatioBenchmarkComputer;
import benchmark.generators.tools.ASTDiffTool;
import benchmark.utils.Configuration.Configuration;
import benchmark.utils.Configuration.ConfigurationFactory;

import java.io.IOException;
import java.util.Map;

/***
 * What's the perfection ratio for each tool
 */
public class RQ7 implements RQ  {
    @Override
    public void run(Configuration[] conf) {
        try {
            rq7(conf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void rq7(Configuration[] confs) throws IOException {
        for (Configuration configuration : confs) {
            Map<ASTDiffTool, Integer> astDiffToolIntegerMap = new CommitPerfectRatioBenchmarkComputer(configuration).perfectRatio();
            RQ.writeToFile(astDiffToolIntegerMap, "out/rq7-" + configuration.getName() + ".csv");
        }
    }

    public static void main(String[] args) throws Exception {
        String version="v2.37";
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.RMD.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.RMD.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.RMD.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.RMD.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.IAM.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.IAM.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.IAM.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.IAM.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.DAT.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.DAT.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.DAT.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.DAT.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.GTS.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.GTS.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.GTS.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.GTS.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.GTG.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.GTG.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.GTG.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.GTG.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.RMD_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.RMD_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.RMD_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.RMD_Mod.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.IAM_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.IAM_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.IAM_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.IAM_Mod.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.DAT_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.DAT_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.DAT_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.DAT_Mod.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.GTS_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.GTS_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.GTS_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.GTS_Mod.name(),version);

        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.GTG_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.GTG_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.GTG_Mod.name(),version);
        new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.GTG_Mod.name(),version);

        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.RMD_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.RMD_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.RMD_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.RMD_MT.name(),version);

        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.IAM_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.IAM_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.IAM_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.IAM_MT.name(),version);

        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.DAT_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.DAT_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.DAT_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.DAT_MT.name(),version);

        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.GTS_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.GTS_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.GTS_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.GTS_MT.name(),version);

        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4j()).perfectRatio2("defects3",ASTDiffTool.GTG_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracle()).perfectRatio2("refOracle3",ASTDiffTool.GTG_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.defects4jTwoPointOne()).perfectRatio2("defects2",ASTDiffTool.GTG_MT.name(),version);
        // new CommitPerfectRatioBenchmarkComputer(ConfigurationFactory.refOracleTwoPointOne()).perfectRatio2("refOracle2",ASTDiffTool.GTG_MT.name(),version);
        }

    }
}
