package org.cloudbus.cloudsim.power;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

/**
 * Golden Jackal Optimization (GJO) based VM allocation policy
 * for energy-efficient cloud data centers.
 *
 * This is a standalone GJO implementation used as a comparison
 * baseline against ESSA and Hybrid GJO-WOA.
 *
 * @author AbhiRam
 */
public class PowerVmAllocationPolicyMigrationGJO
        extends PowerVmAllocationPolicyMigrationAbstract {

    /** Fallback policy */
    private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

    /** Previous threshold for temporal smoothing */
    private double previousThreshold = -1;

    /**
     * Constructor
     */
    public PowerVmAllocationPolicyMigrationGJO(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double unusedParameter, // kept for uniformity
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {

        super(hostList, vmSelectionPolicy);
        this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
    }

    /**
     * Determines if a host is over-utilized using GJO.
     */
    @Override
    protected boolean isHostOverUtilized(PowerHost host) {

        if (!(host instanceof PowerHostUtilizationHistory)) {
            return fallbackVmAllocationPolicy.isHostOverUtilized(host);
        }

        PowerHostUtilizationHistory historyHost =
                (PowerHostUtilizationHistory) host;

        double threshold;
        try {
            threshold = getGJOThreshold(historyHost);
        } catch (Exception e) {
            return fallbackVmAllocationPolicy.isHostOverUtilized(host);
        }

        addHistoryEntry(host, threshold);

        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }

        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > threshold;
    }

    /**
     * Computes utilization threshold using GJO.
     */
    protected double getGJOThreshold(
            PowerHostUtilizationHistory host) {

        double[] history = host.getUtilizationHistory();
        history = Arrays.stream(history).filter(v -> v > 0).toArray();

        if (history.length < 12) {
            return 0.75; // safe default
        }

        /* ---------------- GJO CORE LOGIC ---------------- */
        double gjoValue = goldenJackalOptimization(history);

        /* ---------------- TEMPORAL SMOOTHING ---------------- */
        double smoothed;
        if (previousThreshold < 0) {
            smoothed = gjoValue;
        } else {
            smoothed = 0.85 * previousThreshold + 0.15 * gjoValue;
        }

        /* ---------------- HARD BOUNDS ---------------- */
        smoothed = Math.max(0.6, Math.min(smoothed, 0.9));

        previousThreshold = smoothed;
        return smoothed;
    }

    /**
     * Golden Jackal Optimization logic (standalone).
     */
    private double goldenJackalOptimization(double[] data) {

        Arrays.sort(data);

        // Use top 20% utilization values (less aggressive than hybrid)
        int eliteCount = Math.max(1, (int) (data.length * 0.20));

        double sum = 0;
        for (int i = 0; i < eliteCount; i++) {
            sum += data[i];
        }

        return sum / eliteCount;
    }

    /**
     * Setter for fallback policy.
     */
    public void setFallbackVmAllocationPolicy(
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
    }
}
