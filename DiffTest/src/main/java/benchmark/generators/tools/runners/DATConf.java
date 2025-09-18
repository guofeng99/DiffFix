package benchmark.generators.tools.runners;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.ConfigurationOptions;
import com.github.gumtreediff.matchers.GumtreeProperties;

import java.io.File;
import java.io.IOException;

public class DATConf {
    public String MATCHER;
    public int NRACTIONS;
    public int SIMACTIONS;
    public double bu_minsim;
    public int bu_minsize;
    public int st_minprio;
    public String st_priocalc;

    public static void getConf(String repo,String commit, String srcfile,String dstfile) throws IOException{
        var dirpath = "../DAT_infos/"+repo.hashCode()+"_"+commit.hashCode()+"_"+srcfile.hashCode()+"_"+dstfile.hashCode();
        File dir = new File(dirpath);
        if(dir.exists()){
            File confFile = new File(dirpath+"/bestconf.json");
            if(confFile.exists()){
                try {
                    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    DATConf datConf = mapper.readValue(confFile, DATConf.class);
                    datConf.initDAT();
                } catch (IOException e) {
                    throw e;
                }
            }
        }
    }

    public void initDAT(){
        DiffAutoTuning.matcherType = MATCHER;
        DiffAutoTuningMod.matcherType = MATCHER;
        GumtreeProperties properties = new GumtreeProperties();
        switch (MATCHER) {
            case "ClassicGumtree" -> {
                properties.tryConfigure(ConfigurationOptions.bu_minsim,bu_minsim);
                properties.tryConfigure(ConfigurationOptions.bu_minsize,bu_minsize);
                properties.tryConfigure(ConfigurationOptions.st_minprio,st_minprio);
                properties.tryConfigure(ConfigurationOptions.st_priocalc,st_priocalc);
            }
            case "HybridGumtree" -> {
                properties.tryConfigure(ConfigurationOptions.bu_minsize,bu_minsize);
                properties.tryConfigure(ConfigurationOptions.st_minprio,st_minprio);
                properties.tryConfigure(ConfigurationOptions.st_priocalc,st_priocalc);
            }
            default -> {
                properties.tryConfigure(ConfigurationOptions.st_minprio,st_minprio);
                properties.tryConfigure(ConfigurationOptions.st_priocalc,st_priocalc);
            }
        }
        DiffAutoTuning.properties = properties;
        DiffAutoTuningMod.properties = properties;
    }
}
