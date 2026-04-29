package org.cloudbus.cloudsim.examples.power.planetlab;



import java.io.IOException;

/**
 * A simulation of a heterogeneous power-aware data center using
 * Golden Jackal Optimization (GJO)
 * for dynamic VM allocation.
 *
 * This example uses a real PlanetLab workload: 20110303.
 *
 * @author AbhiRam
 * @since April 2025
 */
public class GJO_mmt {

    public static void main(String[] args) throws IOException {

        boolean enableOutput = false;
        boolean outputToFile = false;

        String inputFolder =
                Hybrid_mmt.class
                        .getClassLoader()
                        .getResource("workload/planetlab")
                        .getPath();

        String outputFolder = "output";
        String workload = "20110303";      // PlanetLab workload
        String vmAllocationPolicy = "gjo"; 
        String vmSelectionPolicy = "mmt";  
        String parameter = "1.0";          // Safety parameter

        new PlanetLabRunner(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                parameter
        );
    }
}

