package benchmark.gui.conf;

import benchmark.generators.tools.ASTDiffTool;
import benchmark.gui.viewers.DiffViewers;
import benchmark.utils.Configuration.Configuration;
import benchmark.utils.Configuration.ConfigurationFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class GuiConf {
    public Set<ASTDiffTool> enabled_tools = new LinkedHashSet<>();
    public Set<DiffViewers> enabled_viewers = new LinkedHashSet<>();

    public static GuiConf defaultConf() {
        GuiConf conf = new GuiConf();
        f.enabled_tools.add(ASTDiffTool.GOD);
        // conf.enabled_tools.add(ASTDiffTool.IJM);
        // conf.enabled_tools.add(ASTDiffTool.MTD);
        conf.enabled_tools.add(ASTDiffTool.RMD_Mod);
        // conf.enabled_tools.add(ASTDiffTool.IAM_Mod);
        // conf.enabled_tools.add(ASTDiffTool.DAT_Mod);
        // conf.enabled_tools.add(ASTDiffTool.GTS_Mod);
        // conf.enabled_tools.add(ASTDiffTool.GTG_Mod);
        conf.enabled_tools.add(ASTDiffTool.RMD);
        // conf.enabled_tools.add(ASTDiffTool.IAM);
        // conf.enabled_tools.add(ASTDiffTool.DAT);
        // conf.enabled_tools.add(ASTDiffTool.GTS);
        // conf.enabled_tools.add(ASTDiffTool.GTG);
        // conf.enabled_tools.add(ASTDiffTool.TRV);

        // Viewers
        conf.enabled_viewers.add(DiffViewers.MONACO);
//        conf.enabled_viewers.add(DiffViewers.VANILLA);
        return conf;
    }
}
