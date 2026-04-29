package org.cloudbus.cloudsim.power;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;

/**
 * Hybrid Golden Jackal Optimization (GJO) + Whale Optimization Algorithm (WOA)
 * based VM allocation policy for energy-efficient cloud data centers.
 *
 * FIXED VERSION:
 * - No double threshold scaling
 * - Proper consolidation
 * - Migration-stable
 * - Energy-optimized
 *
 * @author AbhiRam
 */
public class PowerVmAllocationPolicyMigrationHybridGJOWOA
        extends PowerVmAllocationPolicyMigrationAbstract {

    /** Fallback policy */
    private PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy;

    /** Random generator */
    private Random random = new Random();

    /** Previous threshold (for temporal smoothing) */
    private double previousThreshold = -1;

    /**
     * Constructor
     */
    public PowerVmAllocationPolicyMigrationHybridGJOWOA(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double unusedParameter, // kept for compatibility, NOT USED
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {

        super(hostList, vmSelectionPolicy);
        this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
    }

    /**
     * Determines if a host is over-utilized.
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
            // 🚨 IMPORTANT FIX: DO NOT SCALE AGAIN
            threshold = getHybridGJOWOAThreshold(historyHost);
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
     * Computes utilization threshold using Hybrid GJO-WOA.
     */
    protected double getHybridGJOWOAThreshold(
            PowerHostUtilizationHistory host) {

        double[] history = host.getUtilizationHistory();
        history = Arrays.stream(history).filter(v -> v > 0).toArray();

        if (history.length < 12) {
            return 0.7; // safe default → allows consolidation
        }

        /* ---------------- GJO PHASE (Exploration) ---------------- */
        double gjoValue = goldenJackalOptimization(history);

        /* ---------------- WOA PHASE (Exploitation) ---------------- */
        double woaValue = whaleOptimization(gjoValue);

        /* ---------------- MIGRATION AWARENESS ---------------- */
        double migrationPenalty = host.getVmList().size() * 0.001;
        double adjusted = woaValue - migrationPenalty;

        /* ---------------- TEMPORAL SMOOTHING ---------------- */
        double smoothed;
        if (previousThreshold < 0) {
            smoothed = adjusted;
        } else {
            smoothed = 0.8 * previousThreshold + 0.2 * adjusted;
        }

        /* ---------------- HARD BOUNDS (CRITICAL) ---------------- */
        smoothed = Math.max(0.85, Math.min(smoothed, 0.93));

        previousThreshold = smoothed;
        return smoothed;
    }

    /**
     * Golden Jackal Optimization (selects aggressive elite utilization).
     */
    private double goldenJackalOptimization(double[] data) {

        Arrays.sort(data);

        // Top 15% → aggressive consolidation
        int eliteCount = Math.max(1, (int) (data.length * 0.10));

        double sum = 0;
        for (int i = 0; i < eliteCount; i++) {
            sum += data[i];
        }

        return sum / eliteCount;
    }

    /**
     * Whale Optimization Algorithm (local refinement).
     */
    private double whaleOptimization(double best) {

        double a = 2.0; // initial
        double r = random.nextDouble();

        double A = 2 * a * r - a;
        double C = 2 * r;

        double D = Math.abs(C * best - best);
        return best - A * D;
    }

    /**
     * Setter for fallback policy.
     */
    public void setFallbackVmAllocationPolicy(
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
    }
}
