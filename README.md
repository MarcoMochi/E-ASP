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
- To install and use E-ASP using maven go to the main folder (with file pom.xml) and from the terminal use the command:

  
   ```mvn clean javafx:run```

- Alternatively, to build the project manually, JAVAFX library version 18.0.2 is required and it is downloadable here: https://gluonhq.com/products/javafx/. For a precise guide on how to install it, we suggest: https://openjfx.io/openjfx-docs/.



# How to Use

To run the application use the command: 

```mvn clean javafx:run```

### Example
Once E-ASP is running, you will see a GUI that allows you to write or open an ASP encoding. By clicking on the **File** menu item you can start a new file using *"New"* or open an already existing one with *"Open"*. To make sure the tool is working try to open the file **"Test/base.lp"**.

You will see a button at the bottom of the open window with the text **"Justify"** in it. To its right are **two tickboxes**, one to enable justifications through the Rules and one through the Answer Set. Select one or both of the tickboxes and click on the Justify button. A new window will pop up asking you which atom to justify.

Click on the **"Select"** button to the right of the atom to justify and finally click on the **"Confirm"** button at the bottom of this window. In this case, it could be just the atom *"b"*. 
Finally, will appear a window showing the rules supporting the atom. You can click on the **"Back"** button to go back to the selection or close the window.

### Example
Select the **"Test/coloring.lp"** file for a more complete example. Select one or both of the tickboxes, click on the Justify button, and select one of the *color(T,C)* that are in the answer set. 

Depending on your choices between the justification through the *Rules* and/or through the *Answer Set*, you will see different justifications. If you decide to have justification with rules, you should receive the rule with the count aggregate. 

In this case, you can decide to further inspect it and it will open a new window showing all the positive and negative atoms occurring in the aggregate set.
