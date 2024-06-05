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

public class FileExplorerApp extends JFrame {
    private JTextField searchPatternField;
    private JTextField directoryField;
    private JCheckBox fileCheckBox;
    private JCheckBox folderCheckBox;
    private JTextField commandField;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private List<File> allResults;
    private int maxRecursionDepth = Integer.MAX_VALUE;
    private int minRecursionDepth = 0;
    private int maxHorizontal = Integer.MAX_VALUE;

    public FileExplorerApp() {
        setTitle("File Explorer Application");
        setSize(1920, 1200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        allResults = new ArrayList<>();

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(6, 1));

        // Search pattern input
        JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Search Pattern (Regex):"));
        searchPatternField = new JTextField(20);
        searchPanel.add(searchPatternField);
        panel.add(searchPanel);

        // Directory input
        JPanel directoryPanel = new JPanel();
        directoryPanel.add(new JLabel("Directory:"));
        directoryField = new JTextField(20);
        directoryPanel.add(directoryField);
        panel.add(directoryPanel);

        // File and folder checkboxes
        JPanel checkBoxPanel = new JPanel();
        fileCheckBox = new JCheckBox("File");
        folderCheckBox = new JCheckBox("Folder");
        checkBoxPanel.add(fileCheckBox);
        checkBoxPanel.add(folderCheckBox);
        panel.add(checkBoxPanel);

        // Command input
        JPanel commandPanel = new JPanel();
        commandPanel.add(new JLabel("Commands:"));
        commandField = new JTextField(30);
        commandPanel.add(commandField);
        panel.add(commandPanel);

        // Search button
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parseCommands();
                searchFiles();
            }
        });
        panel.add(searchButton);

        // Checkbox action listeners
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

        // Result table
        String[] columnNames = {"Absolute Path"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void parseCommands() {
        String commands = commandField.getText();
        maxRecursionDepth = Integer.MAX_VALUE;
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
    }

    private void searchFiles() {
        String pattern = searchPatternField.getText();
        String directoryPath = directoryField.getText();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            tableModel.setRowCount(0); // Clear previous results
            allResults.clear(); // Clear previous allResults
            SearchTask task = new SearchTask(directory, pattern, 0);
            task.execute();
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
            if (depth > maxRecursionDepth) {
                return;
            }

            File[] files = directory.listFiles();
            if (files != null) {
                List<File> matchedFiles = new ArrayList<>();
                for (File file : files) {
                    Matcher matcher = regexPattern.matcher(file.getName());
                    if (matcher.find() && depth >= minRecursionDepth) {
                        matchedFiles.add(file);
                    }
                    if (file.isDirectory()) {
                        searchFilesRecursive(file, depth + 1);
                    }
                }

                int count = 0;
                for (File file : matchedFiles) {
                    if (count >= maxHorizontal) {
                        break;
                    }
                    allResults.add(file);
                    publish(file);
                    count++;
                }
            }
        }

        @Override
        protected void process(List<File> chunks) {
            for (File file : chunks) {
                if ((file.isFile() && fileCheckBox.isSelected()) || (file.isDirectory() && folderCheckBox.isSelected())) {
                    tableModel.addRow(new Object[]{file.getAbsolutePath()});
                }
            }
        }

        @Override
        protected void done() {
            try {
                get(); // Ensure any exceptions are thrown
            } catch (Exception e) {
                e.printStackTrace();
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
