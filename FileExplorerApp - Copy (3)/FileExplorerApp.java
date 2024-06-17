import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Timer;

public class FileExplorerApp extends JFrame {
    private JTextField searchPatternField;
    private JTextField directoryField;
    private JTextField startPrefixField;
    private JCheckBox fileCheckBox;
    private JCheckBox folderCheckBox;
    private JTextField commandField;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private List<File> allResults;
    private int maxRecursionDepth = 0;  // Default recursion depth to 0
    private int minRecursionDepth = 0;
    private int maxHorizontal = Integer.MAX_VALUE;
    private String startPrefix = null;
    private SearchTask currentTask;
    private Timer timer;
    private long startTime;

    private JLabel filesScannedLabel;
    private JLabel foldersScannedLabel;
    private JLabel totalScannedLabel;
    private JLabel filesMatchedLabel;
    private JLabel foldersMatchedLabel;
    private JLabel totalMatchedLabel;
    private JLabel elapsedTimeLabel;
    private JLabel currentLocationLabel;

    private int filesScanned = 0;
    private int foldersScanned = 0;
    private int filesMatched = 0;
    private int foldersMatched = 0;

    public FileExplorerApp() {
        setTitle("File Explorer Application");
        setSize(1920, 1200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        allResults = new ArrayList<>();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Top panel for search parameters
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Search Pattern (Regex):"));
        searchPatternField = new JTextField(20);
        inputPanel.add(searchPatternField);
        inputPanel.add(new JLabel("Directory:"));
        directoryField = new JTextField(20);
        inputPanel.add(directoryField);
        inputPanel.add(new JLabel("Start Prefix:"));
        startPrefixField = new JTextField(20);
        inputPanel.add(startPrefixField);
        inputPanel.add(new JLabel("Commands:"));
        commandField = new JTextField(20);
        inputPanel.add(commandField);
        topPanel.add(inputPanel);

        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileCheckBox = new JCheckBox("File");
        folderCheckBox = new JCheckBox("Folder");
        fileCheckBox.setSelected(true);
        folderCheckBox.setSelected(true);
        checkBoxPanel.add(fileCheckBox);
        checkBoxPanel.add(folderCheckBox);

        JButton maxRecursionDepthButton = new JButton("Depth 0");
        maxRecursionDepthButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                maxRecursionDepth = 0;
            }
        });
        checkBoxPanel.add(maxRecursionDepthButton);

        JButton maxRecursionDepthInfinityButton = new JButton("Depth ∞");
        maxRecursionDepthInfinityButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                maxRecursionDepth = Integer.MAX_VALUE;
            }
        });
        checkBoxPanel.add(maxRecursionDepthInfinityButton);

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTask != null && !currentTask.isDone()) {
                    currentTask.cancel(true);
                    timer.stop();
                }
            }
        });
        checkBoxPanel.add(stopButton);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentTask != null && !currentTask.isDone()) {
                    currentTask.cancel(true);
                }
                parseCommands();
                startSearch();
            }
        });
        checkBoxPanel.add(searchButton);

        topPanel.add(checkBoxPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        // Current location display
        currentLocationLabel = new JLabel("Current Location: ");
        panel.add(currentLocationLabel, BorderLayout.CENTER);

        // Statistics display
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(3, 2));

        filesScannedLabel = new JLabel("Files Scanned: 0");
        foldersScannedLabel = new JLabel("Folders Scanned: 0");
        totalScannedLabel = new JLabel("Files and Folders Scanned: 0");
        filesMatchedLabel = new JLabel("Files Matched: 0");
        foldersMatchedLabel = new JLabel("Folders Matched: 0");
        totalMatchedLabel = new JLabel("Files and Folders Matched: 0");
        elapsedTimeLabel = new JLabel("Elapsed Time: 0.000 000s");

        statsPanel.add(filesScannedLabel);
        statsPanel.add(foldersScannedLabel);
        statsPanel.add(totalScannedLabel);
        statsPanel.add(filesMatchedLabel);
        statsPanel.add(foldersMatchedLabel);
        statsPanel.add(totalMatchedLabel);
        statsPanel.add(elapsedTimeLabel);

        panel.add(statsPanel, BorderLayout.SOUTH);

        // Result table
        String[] columnNames = {"Absolute Path"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Add action listeners for checkboxes to dynamically update display
        fileCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDisplay();
            }
        });
        folderCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDisplay();
            }
        });
    }

    private void parseCommands() {
        String commands = commandField.getText();
        maxRecursionDepth = Integer.MAX_VALUE;  // Default to infinity if not specified
        minRecursionDepth = 0;
        maxHorizontal = Integer.MAX_VALUE;

        String[] commandArray = commands.split("\\s+");
        for (String command : commandArray) {
            if (command.startsWith("-maxRecursionDepth=")) {
                try {
                    maxRecursionDepth = Integer.parseInt(command.substring(19));
                } catch (NumberFormatException e) {
                    maxRecursionDepth = Integer.MAX_VALUE;
                }
            } else if (command.startsWith("-minRecursionDepth=")) {
                try {
                    minRecursionDepth = Integer.parseInt(command.substring(19));
                } catch (NumberFormatException e) {
                    minRecursionDepth = 0;
                }
            } else if (command.startsWith("-maxHorizontal=")) {
                try {
                    maxHorizontal = Integer.parseInt(command.substring(15));
                } catch (NumberFormatException e) {
                    maxHorizontal = Integer.MAX_VALUE;
                }
            }
        }

        startPrefix = startPrefixField.getText().trim();
    }

    private void startSearch() {
        String pattern = searchPatternField.getText();
        String directoryPath = directoryField.getText();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            tableModel.setRowCount(0); // Clear previous results
            allResults.clear(); // Clear previous allResults
            filesScanned = 0;
            foldersScanned = 0;
            filesMatched = 0;
            foldersMatched = 0;
            startTime = System.nanoTime();

            currentTask = new SearchTask(directory, pattern, 0);
            currentTask.execute();

            timer = new Timer(100, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long elapsedTime = System.nanoTime() - startTime;
                    elapsedTimeLabel.setText(String.format("Elapsed Time: %.6f s", elapsedTime / 1e9));
                }
            });
            timer.start();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid directory!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateDisplay() {
        tableModel.setRowCount(0); // Clear current display

        for (File file : allResults) {
            if (file.isFile() && fileCheckBox.isSelected()) {
                tableModel.addRow(new Object[]{file.getAbsolutePath()});
            } else if (file.isDirectory() && folderCheckBox.isSelected()) {
                tableModel.addRow(new Object[]{file.getAbsolutePath()});
            }
        }
    }

    private class SearchTask extends SwingWorker<List<File>, File> {
        private final File directory;
        private final String pattern;
        private final Pattern regexPattern;
        private final int currentDepth;

        public SearchTask(File directory, String pattern, int currentDepth) {
            this.directory = directory;
            this.pattern = pattern;
            this.regexPattern = Pattern.compile(pattern);
            this.currentDepth = currentDepth;
        }

        @Override
        protected List<File> doInBackground() {
            searchFilesRecursive(directory, currentDepth);
            return allResults;
        }

        private void searchFilesRecursive(File directory, int depth) {
            if (depth > maxRecursionDepth || isCancelled()) {
                return;
            }

            File[] files = directory.listFiles();
            if (files != null) {
                boolean startSearching = startPrefix == null || startPrefix.isEmpty();
                for (File file : files) {
                    if (isCancelled()) return;

                    if (!startSearching && file.getName().compareTo(startPrefix) >= 0) {
                        startSearching = true;
                    }

                    if (startSearching) {
                        currentLocationLabel.setText("Current Location: " + file.getAbsolutePath());

                        if (file.isDirectory()) {
                            foldersScanned++;
                            if (depth >= minRecursionDepth) {
                                Matcher matcher = regexPattern.matcher(file.getName());
                                if (matcher.find()) {
                                    foldersMatched++;
                                    allResults.add(file);
                                    publish(file);
                                }
                            }
                            searchFilesRecursive(file, depth + 1);
                        } else {
                            filesScanned++;
                            if (depth >= minRecursionDepth) {
                                Matcher matcher = regexPattern.matcher(file.getName());
                                if (matcher.find()) {
                                    filesMatched++;
                                    allResults.add(file);
                                    publish(file);
                                }
                            }
                        }

                        // Update statistics
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                filesScannedLabel.setText("Files Scanned: " + filesScanned);
                                foldersScannedLabel.setText("Folders Scanned: " + foldersScanned);
                                totalScannedLabel.setText("Files and Folders Scanned: " + (filesScanned + foldersScanned));
                                filesMatchedLabel.setText("Files Matched: " + filesMatched);
                                foldersMatchedLabel.setText("Folders Matched: " + foldersMatched);
                                totalMatchedLabel.setText("Files and Folders Matched: " + (filesMatched + foldersMatched));
                            }
                        });
                    }
                }
            }
        }

        @Override
        protected void process(List<File> chunks) {
            updateDisplay();
        }

        @Override
        protected void done() {
            try {
                get(); // Ensure any exceptions are thrown
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                timer.stop();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FileExplorerApp().setVisible(true);
            }
        });
    }
}
