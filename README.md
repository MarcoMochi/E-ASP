# E-ASP
E-ASP - Tool for debugging and justifying ASP programs. 

# Install and Settings

To use the tool is required the JavaFX library and Setting the paths to Clingo and the helper.lp file:

- JAVAFX library version 18.0.2 downloadable from here: https://gluonhq.com/products/javafx/. For a precise guide on how to install it, we suggest: https://openjfx.io/openjfx-docs/.
- To correctly use E-ASP is required to change the paths in the file src/debugger/DebuggerUtil.java:
  - "public static String solver" = "PATH TO CLINGO" I.e. : "/Users/marco/opt/miniconda3/envs/potassco/bin/clingo";
	- "public static String helper" = "PATH TO Debugger/Encoders/helper.lp" I.e: Users/marco/Documents/Projects/Debugger/Encoders/helper.lp"; 

# How to Use

To run a process and be able to interact with 
