package com.project.auto_complete_service.bktree;

public class LevenshteinDistance {

    public static int compute(String a, String b, int threshold) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        if (a.equals(b)) return 0;

        int la = a.length();
        int lb = b.length();

        if (Math.abs(la - lb) > threshold) return threshold + 1;

        // Ensure a is the shorter string
        if (la > lb) {
            String tmp = a; a = b; b = tmp;
            int tl = la; la = lb; lb = tl;
        }

        int[] dp = new int[la + 1];
        for (int i = 0; i <= la; i++) dp[i] = i;

        for (int j = 1; j <= lb; j++) {
            int prev = dp[0];
            dp[0] = j;

            // ✅ Remove rowMin early exit — it was incorrectly pruning valid paths
            // Early exit only safe on full rows, not mid-computation
            for (int i = 1; i <= la; i++) {
                int temp = dp[i];

                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i] = prev;
                } else {
                    dp[i] = 1 + Math.min(prev,
                                 Math.min(dp[i],
                                          dp[i - 1]));
                }
                prev = temp;
            }
        }

        return dp[la] <= threshold ? dp[la] : threshold + 1;
    }

    public static int compute(String a, String b) {
        if (a == null || b == null) return Integer.MAX_VALUE;
        if (a.equals(b)) return 0;

        int la = a.length();
        int lb = b.length();

        if (la > lb) {
            String tmp = a; a = b; b = tmp;
            int tl = la; la = lb; lb = tl;
        }

        int[] dp = new int[la + 1];
        for (int i = 0; i <= la; i++) dp[i] = i;

        for (int j = 1; j <= lb; j++) {
            int prev = dp[0];
            dp[0] = j;

            for (int i = 1; i <= la; i++) {
                int temp = dp[i];

                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i] = prev;
                } else {
                    dp[i] = 1 + Math.min(prev,
                                 Math.min(dp[i],
                                          dp[i - 1]));
                }
                prev = temp;
            }
        }

        return dp[la];
    }
}