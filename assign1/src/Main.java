import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class Main {

    public static void printToCSV(List<Double> times, String fileName) {

        try {
            PrintWriter writer = new PrintWriter(fileName);
            double average = 0;
            StringBuilder sb = new StringBuilder();

            // Average
            for (Double item : times) average += item;
            average /= times.size();

            sb.append("Iteration;Execution Time\n");
            writer.write(sb.toString());

            sb.setLength(0);
            for (int iteration = 0; iteration < times.size(); iteration++) {
                sb.append(iteration);
                sb.append(';');
                sb.append(times.get(iteration));
                sb.append('\n');
                writer.write(sb.toString());
                sb.setLength(0);
            }

            sb.append("Average;");
            sb.append(average);
            writer.write(sb.toString());

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void partiallyPrintMatrix(int[][] matrix, int size) {
        int val = 6;
        if (size < val) val = size;

        System.out.println("\n\nMatrix (partially):");

        for (int i = 0; i < val; i++) {
            System.out.print('|');
            for (int j = 0; j < val; j++) {
                System.out.print(' ');
                System.out.print(matrix[i][j]);
                System.out.print(' ');
            }
            System.out.println('|');
        }
    }

    public static void initializeMatrixes(int[][] m1, int[][] m2, int[][] res, int n) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m1[i][j] = 1;
                m2[i][j] = i + 1;
                res[i][j] = 0;
            }
        }
    }

    public static void benchmark() {


        for (int size = 600; size <= 3000; size+=400) {      
            int[][] m1 = new int[size][size];
            int[][] m2 = new int[size][size];
            int[][] res = new int[size][size];
            Main.initializeMatrixes(m1, m2, res, size);

            // Mult
            List<Double> results = new ArrayList<>();
            for (int iteration = 0; iteration < 3; iteration++) {
                double start = System.nanoTime();
                Main.OnMult(m1, m2, res, size);
                double stop = System.nanoTime();
                results.add((stop - start) / 1000000000);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("../doc/execution_data/mult/java_mult_result_");
            sb.append(size);
            sb.append(".csv");
            Main.printToCSV(results, sb.toString());
            sb.setLength(0);
            sb.append("Finished mult ");
            sb.append(size);
            System.out.println(sb.toString());


            // Mult line
            List<Double> results2 = new ArrayList<>();
            for (int iteration = 0; iteration < 3; iteration++) {
                double start = System.nanoTime();
                Main.OnMultLine(m1, m2, res, size);
                double stop = System.nanoTime();
                results2.add((stop - start) / 1000000000);
            }
            sb.setLength(0);
            sb.append("../doc/execution_data/mult_line/java_mult_line_result_");
            sb.append(size);
            sb.append(".csv");
            Main.printToCSV(results2, sb.toString());
            sb.setLength(0);
            sb.append("Finished mult line ");
            sb.append(size);
            System.out.println(sb.toString());
        }
    }

    // Normal Multiplication
    public static void OnMult(int[][] m1, int[][] m2, int[][] res, int n) {

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    res[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }

    }

    // Line Multiplication
    public static void OnMultLine(int[][] m1, int[][] m2, int[][] res, int n) {

        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                for (int j = 0; j < n; j++) {
                    res[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }

    }

    // Block Multiplication
    public static void OnMultBlock(int[][] m1, int[][] m2, int[][] res, int n, int blockSize) {

        if( n % blockSize != 0 || n % blockSize != 0){
            System.out.println("Error");

        }    
        
        
        for(int i0 = 0; i0 < n; i0 += blockSize ){
            for(int i1 = 0; i1 < n; i1 += blockSize){
                for(int i2 = 0; i2 < n; i2 += blockSize){
                    for(int i = i0; i < i0 + blockSize; i++){
                        for(int j = i1; j < i1 + blockSize; j++){
                            for(int k = i2; k < i2 + blockSize; k++){
                                res[i][j] += m1[i][k] * m2[k][j]; 
                            }
                        }
                    }
                }
            }
        }    
       
    }

    public static void main(String[] args) {

        try (Scanner input = new Scanner(System.in)) {
            while (true) {
                double start = 0, stop = 0;
                int option = 0;

                // User input
                while (true) {
                    System.out.println("\n\nChoose method:\n1. Multiplication\n2. Line Multiplication\n3. Block Multiplication\n4. Benchmark mode\n5. Exit");
                    option = input.nextInt();
                    if (option >= 1 && option <= 5) break;
                    System.out.println("\nInvalid option (choose between 1 2 or 3)");
                }

                if (option == 5) break;

                if (option == 4) {
                    benchmark();


                    return;
                }

                System.out.print("Matrix dimensions:");
                int size = input.nextInt();


                // Matrix initialization
                int[][] m1 = new int[size][size];
                int[][] m2 = new int[size][size];
                int[][] res = new int[size][size];
                Main.initializeMatrixes(m1, m2, res, size);

                // Calculations
                switch (option) {
                    case 1: {
                        start = System.nanoTime();
                        Main.OnMult(m1, m2, res, size);
                        stop = System.nanoTime();
                        break;
                    }
                    case 2: {
                        start = System.nanoTime();
                        Main.OnMultLine(m1, m2, res, size);
                        stop = System.nanoTime();
                        break;
                    }
                    case 3: {
                        System.out.print("\nBlock size:");
                        int noBlocks = input.nextInt();
                        start = System.nanoTime();
                        Main.OnMultBlock(m1, m2, res, size, noBlocks);
                        stop = System.nanoTime();
                        break;
                    }
                    case 4: {

                    }
                    default: {
                        System.out.println("\nInternal error: Invalid option");
                        return;
                    }
                }

                // Results
                Main.partiallyPrintMatrix(res, size);

                float time = (float)(stop - start) / 1000000000;
                System.out.println("Time:" + (time) + "seconds");
                
            }
            input.close();
        }
    }
}