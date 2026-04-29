package org.cloudbus.cloudsim.power;

import java.util.Arrays;
import java.util.List;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

/**
 * The Whale Optimization Algorithm (WOA) based VM allocation policy.
 * 
 * WOA optimizes VM placement in cloud data centers by mimicking whale hunting behavior
 * and dynamically adjusting migration decisions.
 * 
 * @author AbhiRam
 */
public class PowerVmAllocationPolicyMigrationWhaleOptimization extends PowerVmAllocationPolicyMigrationAbstract {

    /** The convergence parameter. */
    private double convergenceParameter = 0;

    /** The fallback VM selection policy. */
    private PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy;

    /** Exploration-exploitation balance factor */
    private double explorationFactor = 0.5;

    /**
     * Instantiates a new WOA-based VM allocation policy.
     * 
     * @param hostList the host list
     * @param vmSelectionPolicy the VM selection policy
     * @param convergenceParameter the convergence parameter
     * @param fallbackVmSelectionPolicy the fallback policy
     */
    public PowerVmAllocationPolicyMigrationWhaleOptimization(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double convergenceParameter,
            double explorationFactor,
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy) {
        super(hostList, vmSelectionPolicy);
        setConvergenceParameter(convergenceParameter);
        setExplorationFactor(explorationFactor);
        setFallbackVmSelectionPolicy(fallbackVmSelectionPolicy);
    }

    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        if (!(host instanceof PowerHostUtilizationHistory)) {
            return getFallbackVmSelectionPolicy().isHostOverUtilized(host);
        }

        PowerHostUtilizationHistory _host = (PowerHostUtilizationHistory) host;
        double upperThreshold = 0;
        try {
            upperThreshold = 1 - getConvergenceParameter() * getHostUtilizationWOA(_host);
        } catch (IllegalArgumentException e) {
            return getFallbackVmSelectionPolicy().isHostOverUtilized(host);
        }
        addHistoryEntry(host, upperThreshold);
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > upperThreshold;
    }

    protected double getHostUtilizationWOA(PowerHostUtilizationHistory host) throws IllegalArgumentException {
        double[] data = host.getUtilizationHistory();
        if (countNonZeroBeginning(data) >= 12) {
            return whaleOptimizationSearch(data);
        }
        return calculateMean(data);
    }

    private double whaleOptimizationSearch(double[] data) {
        double bestSolution = Arrays.stream(data).min().orElse(0);
        double adaptedSolution = bestSolution * (1 - getExplorationFactor());
        return adaptedSolution;
    }
    /**
     * Counts the number of non-zero values at the beginning of an array.
     * 
     * @param data input array
     * @return count of non-zero values at the beginning
     */
    private int countNonZeroBeginning(double[] data) {
        int count = 0;
        for (double value : data) {
            if (value == 0) break;
            count++;
        }
        return count;
    }
    
    /**
     * Calculates the mean of an array.
     * 
     * @param data input array
     * @return mean value
     */
    private double calculateMean(double[] data) {
        double sum = 0;
        int count = 0;
        for (double value : data) {
            if (value > 0) { // Ignore zero values
                sum += value;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }
    

    protected void setConvergenceParameter(double convergenceParameter) {
        if (convergenceParameter < 0) {
            Log.printLine("The convergence parameter cannot be less than zero. The passed value is: "
                    + convergenceParameter);
            System.exit(0);
        }
        this.convergenceParameter = convergenceParameter;
    }

    protected double getConvergenceParameter() {
        return convergenceParameter;
    }

    protected void setExplorationFactor(double explorationFactor) {
        if (explorationFactor < 0 || explorationFactor > 1) {
            Log.printLine("Exploration factor must be between 0 and 1. The passed value is: "
                    + explorationFactor);
            System.exit(0);
        }
        this.explorationFactor = explorationFactor;
    }

    protected double getExplorationFactor() {
        return explorationFactor;
    }

    public void setFallbackVmSelectionPolicy(
            PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy) {
        this.fallbackVmSelectionPolicy = fallbackVmSelectionPolicy;
    }

    public PowerVmAllocationPolicyMigrationAbstract getFallbackVmSelectionPolicy() {
        return fallbackVmSelectionPolicy;
    }
}
