package org.wikibrain.loader;

/**
 * Created by toby on 7/15/14.
 * Refined by Shilad on 8/3/14.
 */

import com.google.gson.*;
import com.typesafe.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.wikibrain.conf.Configuration;
import org.wikibrain.utils.JvmUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Timer;

public class GraphicLoader extends JFrame {

    public static final String DEFAULT_H2_PATH = "\"${baseDir}\"/db/h2";
    public static final String DEFAULT_PG_HOST = "localhost";
    public static final String DEFAULT_PG_PORT = "5432";
    public static final String DEFAULT_PG_DB = "wikibrain";
    public static final String DEFAULT_HEAPSIZE = "4G";
    public static final String DEFAULT_BASEDIR = ".";
    public static final String DEFAULT_LANG = "simple";

    private JPanel mainPanel;

    private JPanel paramPanel;
    private JPanel phasePanel;
    private JPanel buttonPanel;
    private JPanel outputPanel;

    private JLabel commandLabel = new JLabel("Click the run button to see command");
    private JTextArea runLog;

    private JComboBox dataSourceSelection = new JComboBox(new String[] {"H2", "PostgreSQL"});

    private JTextField baseDir = new JTextField(DEFAULT_BASEDIR);
    private JTextField heapSize = new JTextField(DEFAULT_HEAPSIZE);
    private JTextField language = new JTextField(DEFAULT_LANG);

    private JPanel dbPanel = new JPanel();
    private JPanel h2Panel = new JPanel();
    private JTextField h2Path = new JTextField(DEFAULT_H2_PATH);

    private JPanel postgresPanel = new JPanel();
    private JTextField postgresHost = new JTextField(DEFAULT_PG_HOST);
    private JTextField postgresPort = new JTextField(DEFAULT_PG_PORT);
    private JTextField postgresDB = new JTextField(DEFAULT_PG_DB);

    private JTextField postgresUser = new JTextField();
    private JPasswordField postgresPass = new JPasswordField(20);

    private JCheckBox basicWikipediaButton = new JCheckBox("Basic data");
    private JCheckBox luceneButton = new JCheckBox("Lucene");
    private JCheckBox phrasesButton = new JCheckBox("Phrases");
    private JCheckBox conceptsButton = new JCheckBox("Concepts");
    private JCheckBox univeralButton = new JCheckBox("Universal links");
    private JCheckBox wikidataButton = new JCheckBox("Wikidata");
    private JCheckBox spatialButton = new JCheckBox("Spatial data");
    private JCheckBox srButton = new JCheckBox("Semantic relatedness");
    private Process process;
    private JButton runButton;
    private JButton defaultButton;

    public GraphicLoader() {
        super();
        this.setSize(1000, 600);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setTitle("WikiBrain Configuration");

        this.initParamPanel();
        this.initOutputPanel();
        this.initButtonPanel();
        this.initPhaseSelector();

        mainPanel = new JPanel(new GridBagLayout());
        this.getContentPane().add(mainPanel);
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.NORTH;
        c.weighty = 1.0;
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 0.1;
        mainPanel.add(paramPanel, c);

        c.gridx = 1;
        mainPanel.add(phasePanel, c);

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0.8;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        mainPanel.add(outputPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(buttonPanel, c);

        reset();
    }

    private void initOutputPanel() {
        outputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.insets = new Insets(0, 5, 10, 5);
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        outputPanel.add(commandLabel, c);
//        c.gridy = 1;
//        outputPanel.add(new JLabel("Estimated run time: 3414 min"), c);
//        c.gridy = 2;
//        outputPanel.add(new JLabel("Estimated disk space: 1.3 GB"), c);
        c.weighty = 1.0;
        c.gridy = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        runLog = new JTextArea(40, 80);
        runLog.setText("Output log:\n");
        runLog.setEnabled(true);
        outputPanel.add(new JScrollPane(runLog), c);
    }

    private void initButtonPanel() {
        buttonPanel = new JPanel(new GridLayout(1, 3));

        runButton = new JButton("Run");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (actionEvent.getSource().equals(runButton)) {
                    runOrStop();
                }
            }
        });
        runButton.setBackground(Color.GREEN);
        runButton.setOpaque(true);
        runButton.setBorderPainted(false);

        buttonPanel.add(runButton);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(actionEvent.getSource().equals(cancelButton)){
                    System.exit(0);
                }
            }
        });
        buttonPanel.add(cancelButton);

        defaultButton = new JButton("Restore Default");
        defaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (actionEvent.getSource().equals(defaultButton)) {
                    reset();
                }
            }
        });
        buttonPanel.add(defaultButton);
    }

    private void initParamPanel() {
        paramPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);

        // Base directory
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        paramPanel.add(new JLabel("Base directory"), c);

        c.gridx = 1;
        paramPanel.add(baseDir, c);

        c.gridx = 0;
        c.gridy = 1;
        paramPanel.add(new JLabel("Heap Size"), c);

        c.gridx = 1;
        paramPanel.add(heapSize, c);

        c.gridx = 0;
        c.gridy = 2;
        paramPanel.add(new JLabel("Language"), c);

        c.gridx = 1;
        paramPanel.add(language, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 4;
        paramPanel.add(new JLabel("Data source"), c);

        c.gridx = 1;
        paramPanel.add(dataSourceSelection, c);
        dataSourceSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while (dbPanel.getComponentCount() > 0) dbPanel.remove(0);
                if (dataSourceSelection.getSelectedIndex() == 0) {
                    dbPanel.add(h2Panel);
                } else {
                    dbPanel.add(postgresPanel);
                }
                dbPanel.revalidate();
                dbPanel.repaint();
                pack();
            }
        });

        h2Panel.setLayout(new GridLayout(0, 2));
        h2Panel.add(new JLabel("H2 Path"));
        h2Panel.add(h2Path);

        postgresPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c2 = new GridBagConstraints();

        c2.gridx = 0;
        c2.gridy = 0;
        c2.weightx = 0.4;
        c2.anchor = GridBagConstraints.EAST;
        postgresPanel.add(new JLabel("PG host: "), c2);
        c2.gridy = 1;
        postgresPanel.add(new JLabel("PG port: "), c2);
        c2.gridy = 2;
        postgresPanel.add(new JLabel("PG database: "), c2);
        c2.gridy = 3;
        postgresPanel.add(new JLabel("PG user: "), c2);
        c2.gridy = 4;
        postgresPanel.add(new JLabel("PG passwd: "), c2);

        c2.gridx = 1;
        c2.gridy = 0;
        c2.weightx = 0.5;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.anchor = GridBagConstraints.WEST;
        postgresPanel.add(postgresHost, c2);
        c2.gridy = 1;
        postgresPanel.add(postgresPort, c2);
        c2.gridy = 2;
        postgresPanel.add(postgresDB, c2);
        c2.gridy = 3;
        postgresPanel.add(postgresUser, c2);
        c2.gridy = 4;
        postgresPanel.add(postgresPass, c2);

        c.weightx = 1.0;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 5;
        paramPanel.add(dbPanel, c);
    }

    public void reset() {
        baseDir.setText(DEFAULT_BASEDIR);
        heapSize.setText(DEFAULT_HEAPSIZE);
        language.setText(DEFAULT_LANG);

        dataSourceSelection.setSelectedIndex(0);

        h2Path.setText(DEFAULT_H2_PATH);

        postgresUser.setText("");
        postgresPass.setText("");
        postgresPort.setText(DEFAULT_PG_PORT);
        postgresHost.setText(DEFAULT_PG_HOST);
        postgresDB.setText(DEFAULT_PG_DB);

        conceptsButton.setText("Concepts");
        wikidataButton.setText("Wikidata");
        phrasesButton.setText("Phrases");
        luceneButton.setText("Lucene");

        basicWikipediaButton.setSelected(true);
        luceneButton.setSelected(true);
        phrasesButton.setSelected(true);
        conceptsButton.setSelected(false);
        wikidataButton.setSelected(false);
        univeralButton.setSelected(false);
        spatialButton.setSelected(false);
        srButton.setSelected(false);

        basicWikipediaButton.setEnabled(false);
        luceneButton.setEnabled(true);
        phrasesButton.setEnabled(true);
        conceptsButton.setEnabled(true);
        wikidataButton.setEnabled(true);
        univeralButton.setEnabled(true);
        spatialButton.setEnabled(true);
        srButton.setEnabled(true);
    }


     private void initPhaseSelector(){
         phasePanel = new JPanel();
         phasePanel.setLayout(new GridLayout(0, 1));
         phasePanel.add(new JLabel("Please select phases:"));

         basicWikipediaButton.setEnabled(false);

         phasePanel.add(basicWikipediaButton);
         phasePanel.add(luceneButton);
         phasePanel.add(phrasesButton);
         phasePanel.add(conceptsButton);
         phasePanel.add(univeralButton);
         phasePanel.add(wikidataButton);
         phasePanel.add(spatialButton);
         phasePanel.add(srButton);

         ActionListener adapter = new ActionListener() {
             @Override
             public void actionPerformed(ActionEvent e) {
                 stageButtonClicked(e);
             }
         };

         luceneButton.addActionListener(adapter);
         phrasesButton.addActionListener(adapter);
         conceptsButton.addActionListener(adapter);
         univeralButton.addActionListener(adapter);
         wikidataButton.addActionListener(adapter);
         spatialButton.addActionListener(adapter);
         srButton.addActionListener(adapter);
     }

    public void stageButtonClicked(ActionEvent e){
        basicWikipediaButton.setEnabled(false);
        luceneButton.setEnabled(true);
        phrasesButton.setEnabled(true);
        conceptsButton.setEnabled(!language.getText().contains(","));
        wikidataButton.setEnabled(true);

        conceptsButton.setText("Concepts");
        wikidataButton.setText("Wikidata");
        phrasesButton.setText("Phrases");
        luceneButton.setText("Lucene");

        if(univeralButton.isSelected()){
            conceptsButton.setSelected(true);
            conceptsButton.setEnabled(false);
            conceptsButton.setText("Concepts (required by universal links)");
        }
        if(wikidataButton.isSelected()){
            conceptsButton.setSelected(true);
            conceptsButton.setEnabled(false);
            conceptsButton.setText("Concepts (required by wikidata)");
        }
        if(spatialButton.isSelected()){
            wikidataButton.setSelected(true);
            wikidataButton.setEnabled(false);
            wikidataButton.setText("Wikidata (required by spatial data)");
        }
        if(srButton.isSelected()){
            phrasesButton.setSelected(true);
            luceneButton.setSelected(true);
            phrasesButton.setEnabled(false);
            luceneButton.setEnabled(false);
            phrasesButton.setText("Phrases (required by SR)");
            luceneButton.setText("Lucene (required by SR)");
        }
    }

    public void runOrStop() {
        if (process != null) {
            process.destroy();
            appendToLog("\n\nPROCESS CANCELLED!");
            return;
        }

        String ref = new String();
        String dsSelection;
        if(dataSourceSelection.getSelectedIndex() == 0)
            dsSelection = "h2";
        else
            dsSelection = "psql";


        Config defaultConf = new Configuration().get();


        ref = defaultConf.toString().substring(defaultConf.toString().indexOf("{"), defaultConf.toString().lastIndexOf("}") + 1);
        JsonParser jp = new JsonParser();

        JsonObject refObj = new JsonObject();
        refObj.addProperty("baseDir", baseDir.getText());
        refObj.add("dao", new JsonObject());
        refObj.add("spatial", new JsonObject());
        refObj.get("dao").getAsJsonObject().add("dataSource", new JsonObject());
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().add("h2", new JsonObject());
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().add("psql", new JsonObject());
        refObj.get("spatial").getAsJsonObject().add("dao", new JsonObject());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().add("dataSource", new JsonObject());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().add("postgis", new JsonObject());


        if(dataSourceSelection.getSelectedIndex() == 0)
            refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().addProperty("default", "h2");
        else
            refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().addProperty("default", "psql");
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("h2").getAsJsonObject().addProperty("url", String.format("jdbc:h2:%s;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0;MAX_OPERATION_MEMORY=100000000", h2Path.getText()));
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("psql").getAsJsonObject().addProperty("url", String.format("jdbc:postgresql://%s/%s", postgresHost.getText(), postgresDB.getText()));
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("psql").getAsJsonObject().addProperty("postgresUser", postgresUser.getText());
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("psql").getAsJsonObject().addProperty("password", new String(postgresPass.getPassword()));
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("host", postgresHost.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("port", postgresPort.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("database", postgresDB.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("user", postgresUser.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("passwd", new String(postgresPass.getPassword()));


        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ref = gson.toJson(refObj);







        //System.out.print(ref);
        try {

            File file = new File("customized.conf");
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write(ref);
            output.close();
            /*
            Object[] options = {"Yes please", "No thanks"};
            int n = JOptionPane.showOptionDialog(new JFrame("Edit Configuration File"), "Configuration file has been generated \n Do you want to open the file for advanced settings? ", "Edit Configuration File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if(n == 0){
                Desktop.getDesktop().edit(file);

            }
            */
        }
        catch (Exception e){
            e.printStackTrace();
        }


        try {
//            StringBuffer output = new StringBuffer();
//            Process p;
//            this.setVisible(false);
//
//            p = Runtime.getRuntime().exec("mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller\n");
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String line = "";
//            while ((line = reader.readLine()) != null){
//                output.append(line + "\n");
//                System.out.println(line);
//
//            }
//            p.waitFor();
            //System.out.println(output.toString());

            /*
            String loader = new String("./wb-java.sh org.wikibrain.Loader");
            */
            java.util.List<String> argList = new ArrayList<String>();

            argList.add("-l");
            argList.add(language.getText());


            if(basicWikipediaButton.isSelected()){
                argList.add("-s");
                argList.add("fetchlinks");
                argList.add("-s");
                argList.add("download");
                argList.add("-s");
                argList.add("dumploader");
                argList.add("-s");
                argList.add("redirects");
                argList.add("-s");
                argList.add("wikitext");

            }
            if(luceneButton.isSelected()){
                argList.add("-s");
                argList.add("lucene");
            }
            if(phrasesButton.isSelected()){
                argList.add("-s");
                argList.add("phrases");
            }
            if(conceptsButton.isSelected()){
                argList.add("-s");
                argList.add("concepts");
            }
            if(univeralButton.isSelected()){
                argList.add("-s");
                argList.add("universal");
            }
            if(wikidataButton.isSelected()){
                argList.add("-s");
                argList.add("wikidata");
            }
            if(spatialButton.isSelected()){
                argList.add("-s");
                argList.add("spatial");
            }
            if(srButton.isSelected()){
                argList.add("-s");
                argList.add("sr");
            }
            argList.add("-c");
            argList.add("customized.conf");

            //System.out.println(loader);

            //p = Runtime.getRuntime().exec(loader);
            String arg[] = new String[argList.size()];
            arg = argList.toArray(arg);

            commandLabel.setText("org.wikibrain.Loader " + StringUtils.join(arg, " "));

            OutputStream out = new PrintStream(new LogOutputStream(System.out), true);
            OutputStream err = new PrintStream(new LogOutputStream(System.err), true);

            runButton.setText("Stop");
            runButton.setBackground(Color.RED);
            runLog.setText("");
            defaultButton.setEnabled(false);

            this.process = JvmUtils.launch(org.wikibrain.Loader.class, arg, out, err);
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                               @Override
                               public void run() {
                                   if (checkIfProcessHasFinished()) {
                                       timer.cancel();
                                   }
                               }
                           }, 1000, 100);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private synchronized boolean checkIfProcessHasFinished() {
        if (process == null) {
            return true;
        }
        try {
            int val = process.exitValue();
            if (val == 0) {
                appendToLog("\n\nLOADING COMPLETED SUCCESSFULLY!\n\n");
            } else {
                appendToLog("\n\nLOADING FAILED!\n\n\n");
            }
            process = null;
            runButton.setText("Run");
            runButton.setBackground(Color.GREEN);
            defaultButton.setEnabled(true);
            return true;
        } catch (IllegalThreadStateException e2) {
            return false;
        }
    }

    private void appendToLog(String text) {
        runLog.append(text);
        // scrolls the text area to the end of data
        runLog.setCaretPosition(runLog.getDocument().getLength());
    }



    public static void main(String[] args)
    {
        GraphicLoader w = new GraphicLoader();
        w.setVisible(true);
        w.setDefaultCloseOperation(EXIT_ON_CLOSE);

    }

    class LogOutputStream extends OutputStream {
        private final PrintStream stream;

        public LogOutputStream(PrintStream stream) {
            this.stream = stream;
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            final String text = new String(buffer, offset, length);
            stream.write(buffer, offset, length);
            appendToLog(text);
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }
    }

}

