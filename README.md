This repository contains all the source-code and experiments of this work.

# DiffFix 

our artifact DiffFix that is implemented based on the RM-ASTDiff project

see the `./DiffFix/README.md` for more details and try it

# DiffTest

 our extension to the DiffBenchmark project for experiments

	1. add functions to log experimental data
	src/main/java/benchmark/generators/BenchmarkHumanReadableDiffGenerator.java
	
	2. s iASTMapper and DiffAutoTuning
	src/main/java/benchmark/generators/tools/runners/
	DAT_infos.zip includes the saved parameters of DiffAutoTuning 
	
	3. apply DiffFix to five AST differencing tools
	src/main/java/benchmark/generators/tools/runners/

# DiffTrace 

experimental data and scripts for answering research questions

# DiffStudy 

data, scripts and human-verified results of the extended experiment
