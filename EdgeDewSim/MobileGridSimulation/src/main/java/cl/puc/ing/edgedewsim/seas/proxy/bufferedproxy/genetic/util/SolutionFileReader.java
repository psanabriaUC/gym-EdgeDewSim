package cl.puc.ing.edgedewsim.seas.proxy.bufferedproxy.genetic.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SolutionFileReader {

    private final String file;
    private final int solutionSize;
    private BufferedReader solutionContiner;
    private String line;

    public SolutionFileReader(String file, int solutionSize) {
        this.file = file;
        this.solutionSize = solutionSize;
    }

    public Short[] loadSolution() throws IOException {
        solutionContiner = new BufferedReader(new FileReader(file));

        this.nextLine();
        Short[] solution = new Short[solutionSize];
        int jobPosition = 0;
        while (line != null) {
            solution[jobPosition] = Short.parseShort(line);
            jobPosition++;
            this.nextLine();
        }

        return solution;
    }

    private void nextLine() throws IOException {
        this.line = this.solutionContiner.readLine();
        if (line == null) return;
        this.line = this.line.trim();
        while (line.startsWith("#") ||
                line.equals(""))
            this.line = this.solutionContiner.readLine().trim();
    }

}
