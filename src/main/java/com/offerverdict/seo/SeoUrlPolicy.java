package com.offerverdict.seo;

public final class SeoUrlPolicy {

    private SeoUrlPolicy() {
    }

    public static int alignToInterval(int salary, int interval) {
        if (interval <= 0) {
            return salary;
        }
        int rounded = (int) (Math.round((double) salary / interval) * interval);
        return rounded == 0 ? interval : rounded;
    }

    public static int clampSalary(int salary, int minSalary, int maxSalary) {
        return Math.min(Math.max(salary, minSalary), maxSalary);
    }

    public static int clampAndAlignSalary(int salary, int minSalary, int maxSalary, int interval) {
        return alignToInterval(clampSalary(salary, minSalary, maxSalary), interval);
    }

    public static double clampSalary(double salary, double minSalary, double maxSalary) {
        return Math.min(Math.max(salary, minSalary), maxSalary);
    }

    public static boolean isWithinRange(double salary, double minSalary, double maxSalary) {
        return salary >= minSalary && salary <= maxSalary;
    }
}
