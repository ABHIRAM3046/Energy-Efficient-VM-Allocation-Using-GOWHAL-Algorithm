package org.cloudbus.cloudsim.examples.power.planetlab;

import java.io.IOException;

/**
 * A simulation of a heterogeneous power-aware data center using the Elite Sparrow Search Algorithm (ESSA)
 * for dynamic VM allocation optimization.
 * 
 * This example uses a real PlanetLab workload: 20110303.
 * @author AbhiRam
 * @since April 2, 2025
 */
public class whalemu {

    /**
     * The main method.
     * 
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        boolean enableOutput = false;
        boolean outputToFile = false;
        String inputFolder = whalemu.class.getClassLoader().getResource("workload/planetlab").getPath();
        String outputFolder = "output";
        String workload = "20110303"; // PlanetLab workload
        String vmAllocationPolicy = "whale"; // Whale Optimaisation Algorithm (WOA) for VM allocation
        String vmSelectionPolicy = "mu"; // Example selection policy
        String parameter = "1.5"; // Custom parameter for tuning WOA

        new PlanetLabRunner(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                parameter);
    }
}
