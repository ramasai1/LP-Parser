import java.io.*;
import java.util.*;

public class Parse {
    public static void main(String[] args) {
        //name of the file
        String fileName = "air04_orig.lp", line = null, writeToFile = "air04_orig_z3.py";

        //OPT_TYPE OF THE OPTIMIZATION: MAX OR MIN
        String OPT_TYPE = null;

        //FINAL OBJECTIVE FUNCTION AFTER  EXTRACTION.
        String OBJECTIVE = null;

        //FINAL CONSTRAINTS AFTER EXTRACTION
        ArrayList<String> CONSTRAINTS = new ArrayList<>();

        //FINAL VARIABLE BOUNDS AFTER EXTRACTION
        List<String> VARIABLE_BOUNDS = new ArrayList<>();

        //FINAL INTEGER DEFINITIONS AFTER EXTRACTION
        List<String> INTEGER_DEFINITIONS = new ArrayList<>();

        //create StringBuilders
        StringBuilder readObjective = new StringBuilder();
        StringBuilder readConstraints = new StringBuilder();
        StringBuilder readVariableBounds = new StringBuilder();
        StringBuilder readIntegerDefinitions = new StringBuilder();
        StringBuilder writeIntegers = new StringBuilder();
        StringBuilder writeConstraints = new StringBuilder();
        StringBuilder writeVariableBounds = new StringBuilder();
        StringBuilder writeObjective = new StringBuilder();

        try {
            //read the file
            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            //loop through file that has just been read
            while((line = bufferedReader.readLine()) != null) {
                //Extracting the readObjective function.
                if(line.contains("min") || line.contains("max")) {
                    if(line.contains("min"))
                        OPT_TYPE = "minimize";

                    else if(line.contains("max"))
                        OPT_TYPE = "maximize";

                    readObjective.append(line);
                    line = bufferedReader.readLine();
                    while(line != null) {
                        line = bufferedReader.readLine();
                        if(line.equals("/* Constraints */"))
                            break;

                        readObjective.append(line);
                    }

                    //TODO: MAKE THIS CLEANER
                    String[] splits = readObjective.toString().split("min: ");
                    String[] tempSplit = splits[1].split(";");
                    OBJECTIVE = tempSplit[0].replaceAll(" x", "*x");
                    System.out.println(OBJECTIVE);
                }

                //Extracting the constraints.
                if(line.equals("/* Constraints */")) {
                    line = bufferedReader.readLine();
                    readConstraints.append(line);
                    while(line != null) {
                        line = bufferedReader.readLine();
                        if(line.equals("/* Variable bounds */"))
                            break;

                        readConstraints.append(line);
                    }

                    String[] splits = readConstraints.toString().split(";");
                    for(String split : splits) {
                        String[] temp = split.split(":");
                        CONSTRAINTS.add(temp[1]);
                    }
                    //System.out.println(CONSTRAINTS.get(0));
                }

                //extracting the variable bounds
                if(line.equals("/* Variable bounds */")) {
                    line = bufferedReader.readLine();
                    readVariableBounds.append(line);
                    while(line != null) {
                        line = bufferedReader.readLine();
                        if(line.equals("/* Integer definitions */"))
                            break;

                        readVariableBounds.append(line);
                    }

                    String[] splits = readVariableBounds.toString().split(";");
                    VARIABLE_BOUNDS = Arrays.asList(splits);
                }

                if(line.equals("/* Integer definitions */")) {
                    line = bufferedReader.readLine();
                    readIntegerDefinitions.append(line);
                    while(line != null) {
                        line = bufferedReader.readLine();
                        readIntegerDefinitions.append(line);
                    }

                    String[] splits = readIntegerDefinitions.toString().split(";");
                    INTEGER_DEFINITIONS = Arrays.asList(splits[0].split("int "));
                    //System.out.println(INTEGER_DEFINITIONS.get(1));
                }
            }
        } catch(FileNotFoundException e) {
            System.out.println("Unable to open file: " + fileName);
        } catch(IOException e) {
            System.out.println("Error reading file: " + fileName);
        }


        try {
            //make the file to write to
            BufferedWriter writer = new BufferedWriter(new FileWriter(writeToFile));
            writer.write("from z3 import *");
            writer.write("\n\n");

            //write the definition of the integers to the z3 python file
            writeIntegers.append(INTEGER_DEFINITIONS.get(1));
            writeIntegers.append(" = Ints('");
            String[] splits = INTEGER_DEFINITIONS.get(1).split(",");
            for(int i = 0; i < splits.length; i++) {
                writeIntegers.append(splits[i]);
                writeIntegers.append(" ");
            }

            writeIntegers.append("')\n");
            writer.write(writeIntegers.toString());

            //write the optimization function declaration in the python file
            writer.write("z1 = Real('z1')\n");
            writer.write("opt = Optimize()\n");

            //write the constraints into the python file
            for(int i = 0; i < CONSTRAINTS.size(); i++) {
                writeConstraints.append("opt.add(");
                writeConstraints.append(CONSTRAINTS.get(i));
                writeConstraints.append(")\n");
            }
            writer.write(writeConstraints.toString());

            //write the variable bounds into the python file
            writeVariableBounds.append("opt.add(");
            int i = 0;
            for(; i < VARIABLE_BOUNDS.size() - 1; i++) {
                writeVariableBounds.append(VARIABLE_BOUNDS.get(i));
                writeVariableBounds.append(",");
            }
            writeVariableBounds.append(VARIABLE_BOUNDS.get(i));
            writeVariableBounds.append(")");
            //System.out.println(writeVariableBounds.toString());
            writer.write(writeVariableBounds.toString());
            writer.write("\n");

            //write the objective function to the python file
            writeObjective.append("opt.add(z1 == ");
            writeObjective.append(OBJECTIVE);
            writeObjective.append(")");
            System.out.println(writeObjective.toString());
            writer.write(writeObjective.toString());
            writer.write("\n");

            //write minimize or maximize to python file
            if(OPT_TYPE.equals("minimize")) {
                writer.write("f1 = opt.minimize(z1)\n");
            } else {
                writer.write("f1 = opt.maximize(z1)\n");
            }

            //write the final command
            writer.write("if(opt.check() == sat):\n");
            writer.write("  print(opt.model())\n");

            //close the writer
            writer.close();
        } catch (IOException e) {
            System.out.println("Unable to write to file: " + writeToFile);
        }
    }
}
