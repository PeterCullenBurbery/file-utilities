import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileSearchGUI {
    private JFrame frame;
    private JTextField directoryField;
    private JTextField regexField;
    private JTextArea resultArea;
    private JCheckBox caseInsensitiveCheckbox;
    private JLabel countLabel;
    private JTextField maxDepthField;
    private JTextField timeoutField;

    public FileSearchGUI() {
        frame = new JFrame("File Search");
        directoryField = new JTextField(30);
        regexField = new JTextField(30);
        resultArea = new JTextArea(20, 50);
        resultArea.setEditable(false);
        caseInsensitiveCheckbox = new JCheckBox("Case Insensitive");
        countLabel = new JLabel();
        maxDepthField = new JTextField("∞", 5);
        timeoutField = new JTextField("20", 5);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchFiles();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Directory:"));
        inputPanel.add(directoryField);
        inputPanel.add(new JLabel("Max Depth:"));
        inputPanel.add(maxDepthField);
        inputPanel.add(new JLabel("Timeout (s):"));
        inputPanel.add(timeoutField);
        inputPanel.add(new JLabel("Regex:"));
        inputPanel.add(regexField);
        inputPanel.add(caseInsensitiveCheckbox);
        inputPanel.add(searchButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(countLabel, BorderLayout.CENTER);

        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(resultArea), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void searchFiles() {
        String dirPath = directoryField.getText();
        String regex = regexField.getText();
        boolean caseInsensitive = caseInsensitiveCheckbox.isSelected();
        File dir = new File(dirPath);
        int maxDepth = maxDepthField.getText().equals("∞") ? Integer.MAX_VALUE : Integer.parseInt(maxDepthField.getText());
        long timeout = Long.parseLong(timeoutField.getText()) * 1_000_000_000L; // Convert seconds to nanoseconds

        if (!dir.isDirectory()) {
            resultArea.setText("Not a directory");
            return;
        }

        try {
            final Pattern patternCaseSensitive = Pattern.compile(regex);
            final Pattern patternCaseInsensitive = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

            final Pattern initialPattern = caseInsensitive ? patternCaseInsensitive : patternCaseSensitive;
            final Pattern oppositePattern = caseInsensitive ? patternCaseSensitive : patternCaseInsensitive;

            // Start timing for the initial search
            long startTime = System.nanoTime();

            // Perform the initial search
            List<String> initialFilesList = new ArrayList<>();
            searchFilesRecursive(dir, initialPattern, initialFilesList, 0, maxDepth, startTime, timeout);
            int initialCount = initialFilesList.size();
            String initialCountLabel = caseInsensitive ? "Case Insensitive Count: " : "Case Sensitive Count: ";

            // End timing for initial search
            long endTime = System.nanoTime();
            double initialDuration = (endTime - startTime) / 1e9; // Convert nanoseconds to seconds

            // Start timing for the opposite search
            startTime = System.nanoTime();

            // Perform the opposite search
            List<String> oppositeFilesList = new ArrayList<>();
            searchFilesRecursive(dir, oppositePattern, oppositeFilesList, 0, maxDepth, startTime, timeout);
            int oppositeCount = oppositeFilesList.size();
            String oppositeCountLabel = caseInsensitive ? "Case Sensitive Count: " : "Case Insensitive Count: ";

            // End timing for opposite search
            endTime = System.nanoTime();
            double oppositeDuration = (endTime - startTime) / 1e9; // Convert nanoseconds to seconds

            // Update the count label with the counts and times
            countLabel.setText("<html>" + initialCountLabel + initialCount + " (" + initialDuration + " s)<br>" +
                               oppositeCountLabel + oppositeCount + " (" + oppositeDuration + " s)</html>");

            // Display the files found in the initial search
            resultArea.setText("");
            for (String file : initialFilesList) {
                resultArea.append(file + "\n");
            }
        } catch (PatternSyntaxException ex) {
            resultArea.setText("Regex syntax error: " + ex.getDescription());
        }
    }

    private void searchFilesRecursive(File dir, Pattern pattern, List<String> filesFound, int currentDepth, int maxDepth, long startTime, long timeout) {
        if (currentDepth > maxDepth || System.nanoTime() - startTime > timeout) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    searchFilesRecursive(file, pattern, filesFound, currentDepth + 1, maxDepth, startTime, timeout);
                } else if (pattern.matcher(file.getName()).matches()) {
                    filesFound.add(file.getAbsolutePath());
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileSearchGUI());
    }
}
