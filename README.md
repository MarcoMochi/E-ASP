# E-ASP
E-ASP - Tool for debugging and justifying ASP programs 

## Install and Settings

### Prerequisites:
- Java version: 18
- Maven
- Clingo version: 5.6.2 

### Setting:
To correctly use E-ASP is required to put the correct paths inside the set_path file:
  - CLINGO_PATH=$YOUR_CLINGO_PATH


The HELPER_PATH should be changed just in case of renaming or changing of the folder:
  - HELPER_PATH=$PATH_TO_/helper/helper.lp 
 
### Install:
- To install E-ASP using maven go to the main folder (with file pom.xml) and use command:

  
   ```console mvn clean javafx:run```

- Alternatively, to build the project manually, JAVAFX library version 18.0.2 is required and it is downloadable here: https://gluonhq.com/products/javafx/. For a precise guide on how to install it, we suggest: https://openjfx.io/openjfx-docs/.



# How to Use

