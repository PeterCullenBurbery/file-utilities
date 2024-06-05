import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class PathModifierGUI {

    public static void main(String[] args) {
        JFrame frame = new JFrame("PATH Modifier GUI");
        frame.setSize(500, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField textField = new JTextField(30);

        JButton addButton = new JButton("Add to PATH");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modifyPath(textField.getText(), true);
            }
        });

        JButton removeButton = new JButton("Remove from PATH");
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                modifyPath(textField.getText(), false);
            }
        });

        JPanel panel = new JPanel();
        panel.add(textField);
        panel.add(addButton);
        panel.add(removeButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void modifyPath(String path, boolean add) {
        File pathFile = new File(path);

        // Check if the path is a directory or file
        if (pathFile.isFile()) {
            path = pathFile.getParent();
        }

        // Modify PATH using PowerShell
        try {
            String psScript = add ? "$env:Path += ';" + path + "'" : "$env:Path = $env:Path.Replace(';" + path + "','')";
            String command = "powershell.exe -Command \"" + psScript + "\"";
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                System.out.println(line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
