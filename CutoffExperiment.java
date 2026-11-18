import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Chương trình thực nghiệm khảo sát giá trị CUTOFF tối ưu cho thuật toán Mergesort.
 * Chương 2.2 (Mergesort - Cutoff to Insertion Sort).
 * @author Tô Hải Đăng
 */
public class CutoffExperiment {

    private static final int N = 1_000_000;
    private static final int TRIALS = 25;
    private static final long BASE_SEED = 20261110L;

    private static final int[] CUTOFF_VALUES = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 18, 20, 24, 28, 32, 40, 48, 64
    };

    public enum DataType {
        RANDOM, NEARLY_SORTED, REVERSE_SORTED, FEW_UNIQUE
    }

    public static int[] generateDataset(int n, DataType type, long seed) {
        Random rand = new Random(seed);
        int[] a = new int[n];

        switch (type) {
            case RANDOM:
                for (int i = 0; i < n; i++) {
                    a[i] = rand.nextInt();
                }
                break;

            case NEARLY_SORTED:
                for (int i = 0; i < n; i++) {
                    a[i] = i;
                }
                int swaps = Math.max(1, n / 50);
                for (int k = 0; k < swaps; k++) {
                    int i = rand.nextInt(n);
                    int j = rand.nextInt(n);
                    int swap = a[i];
                    a[i] = a[j];
                    a[j] = swap;
                }
                break;

            case REVERSE_SORTED:
                for (int i = 0; i < n; i++) {
                    a[i] = n - i;
                }
                break;

            case FEW_UNIQUE:
                for (int i = 0; i < n; i++) {
                    a[i] = rand.nextInt(10);
                }
                break;
        }
        return a;
    }


    public static void exportSampleDatasets(int sampleSize) throws IOException {
        File dir = new File("datasets");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (DataType type : DataType.values()) {
            int[] data = generateDataset(sampleSize, type, BASE_SEED);
            File file = new File(dir, "dataset_" + type.name().toLowerCase(Locale.ROOT) + "_" + sampleSize + ".txt");
            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                out.println(sampleSize);
                for (int val : data) {
                    out.println(val);
                }
            }
        }
        System.out.println("[+] Đã xuất 4 file dữ liệu mẫu vào thư mục ./datasets/");
    }

    public static double mean(double[] x) {
        if (x.length == 0) return Double.NaN;
        double sum = 0.0;
        for (double v : x) {
            sum += v;
        }
        return sum / x.length;
    }


    public static double stddev(double[] x) {
        if (x.length <= 1) return 0.0;
        double m = mean(x);
        double sumSq = 0.0;
        for (double v : x) {
            sumSq += (v - m) * (v - m);
        }
        return Math.sqrt(sumSq / (x.length - 1));
    }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        System.out.println("=== BẮT ĐẦU THÍ NGHIỆM KHẢO SÁT CUTOFF CHO MERGESORT (CHUẨN SEDGEWICK & WAYNE) ===");
        System.out.println("Người thực hiện: Tô Hải Đăng");
        System.out.println("Cấu hình: N = " + N + ", Số lần lặp (Trials) = " + TRIALS);
        System.out.println("OS: " + System.getProperty("os.name") + " | Java: " + System.getProperty("java.version"));

        exportSampleDatasets(10_000);

        System.out.print("[*] Đang chạy JVM Warm-up...");
        for (int w = 0; w < 6; w++) {
            int[] warmArr = generateDataset(200_000, DataType.RANDOM, BASE_SEED + w);
            MergeSort.sort(warmArr, 7);
        }
        System.out.println(" Hoàn tất!\n");


        try (PrintWriter csv = new PrintWriter(new FileWriter("cutoff_results.csv"))) {
            csv.println("DataType,N,Cutoff,MeanTimeMs,StdDevMs,MinTimeMs,MaxTimeMs,SpeedupPct");

            for (DataType type : DataType.values()) {
                System.out.println(">>> Phân phối dữ liệu: " + type.name());
                System.out.printf("%-8s | %-12s | %-12s | %-10s | %-10s | %-10s%n",
                        "CUTOFF", "Mean (ms)", "StdDev (ms)", "Min (ms)", "Max (ms)", "Cải thiện");
                System.out.println("---------------------------------------------------------------------------");

                int[][] baseArrays = new int[TRIALS][];
                for (int t = 0; t < TRIALS; t++) {
                    baseArrays[t] = generateDataset(N, type, BASE_SEED + t);
                }

                double baselineMean = 0.0;
                int bestCutoff = 1;
                double bestMean = Double.MAX_VALUE;

                for (int cutoff : CUTOFF_VALUES) {
                    System.gc();
                    double[] timesMs = new double[TRIALS];
                    for (int t = 0; t < TRIALS; t++) {
                        int[] workArr = Arrays.copyOf(baseArrays[t], N);


                        long startNano = System.nanoTime();
                        MergeSort.sort(workArr, cutoff);
                        long endNano = System.nanoTime();

                        if (!MergeSort.isSorted(workArr)) {
                            throw new IllegalStateException("Lỗi: Mảng chưa được sắp xếp đúng!");
                        }
                        timesMs[t] = (endNano - startNano) / 1_000_000.0;
                    }

                    double mu = mean(timesMs);
                    double sigma = stddev(timesMs);
                    double minT = Arrays.stream(timesMs).min().orElse(0.0);
                    double maxT = Arrays.stream(timesMs).max().orElse(0.0);

                    if (cutoff == 1) {
                        baselineMean = mu;
                    }
                    double speedup = ((baselineMean - mu) / baselineMean) * 100.0;

                    if (mu < bestMean) {
                        bestMean = mu;
                        bestCutoff = cutoff;
                    }

                    System.out.printf("%-8d | %-12.3f | %-12.3f | %-10.3f | %-10.3f | %+8.2f%%%n",
                            cutoff, mu, sigma, minT, maxT, speedup);

                    csv.printf("%s,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                            type.name(), N, cutoff, mu, sigma, minT, maxT, speedup);
                }
                System.out.printf("==> [KẾT LUẬN %s]: CUTOFF tốt nhất = %d (Mean = %.3f ms, nhanh hơn %.2f%% so với CUTOFF = 1)%n%n",
                        type.name(), bestCutoff, bestMean, ((baselineMean - bestMean) / baselineMean) * 100.0);
            }
        }
        System.out.println("[+] Đã lưu toàn bộ số liệu thống kê vào file: cutoff_results.csv");
    }
}
