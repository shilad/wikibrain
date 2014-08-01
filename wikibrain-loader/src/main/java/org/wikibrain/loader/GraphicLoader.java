package org.wikibrain.loader;

/**
 * Created by toby on 7/15/14.
 */

import com.google.gson.*;
import com.typesafe.config.Config;
import org.wikibrain.Loader;
import org.wikibrain.conf.Configuration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class GraphicLoader extends JFrame implements ItemListener{
    private JComboBox dataSourceSelection;
    private JLabel jLabel;
    private JTextField maxThread;
    //private JTextField heapSize;
    //private JTextField stackSize;
    private JTextField language;
    private JButton jButton;
    private JTextField h2Path = new JTextField("\"${baseDir}\"/db/h2");
    private JTextField username = new JTextField();
    private JTextField postgresHost = new JTextField("localhost");
    private JTextField portNo = new JTextField("5432");
    private JTextField postgresDB = new JTextField("wikibrain");
    private JTextField postgresSpatialDB = new JTextField("wikibrain_spatial");
    private JPasswordField pass = new JPasswordField(20);
    private JCheckBox basicWikipediaButton = new JCheckBox("Basic Wikipedia Data Structure");
    private JCheckBox luceneButton = new JCheckBox("Lucene");
    private JCheckBox phrasesButton = new JCheckBox("Phrases");
    private JCheckBox conceptsButton = new JCheckBox("Concepts");
    private JCheckBox univeralButton = new JCheckBox("Universal Links");
    private JCheckBox wikidataButton = new JCheckBox("Wikidata");
    private JCheckBox spatialButton = new JCheckBox("Spatial Data");
    private JCheckBox srButton = new JCheckBox("Semantic Relatedness Model");









    public GraphicLoader()
    {
        super();
        this.setSize(480, 300);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        JPanel jPanel1 = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        this.getContentPane().add(jPanel1);

        //this.add(getJTextField(), null);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        jPanel1.add(new JLabel("  Data source"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 0;
        jPanel1.add(getDataSourceSelection(), c);
        //jPanel1.add(getJLabel(), null);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        jPanel1.add(new JLabel("  Max Thread"), c);
        maxThread = getJTextField("-1");

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 1;
        jPanel1.add(maxThread, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        //jPanel1.add(new JLabel("  Heap Size"), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 2;
        //heapSize = getJTextField("4G");
        //jPanel1.add(heapSize, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        //jPanel1.add(new JLabel("  Stack Size"), c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 2;
        c.gridx = 1;
        c.gridy = 3;
        //stackSize = getJTextField("1G");
        //jPanel1.add(stackSize, c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 2;
        c.gridheight = 3;
        c.gridx = 0;
        c.gridy = 4;
        jPanel1.add(new JLabel("    "), c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridheight = 2;
        c.gridy = 4;
        jPanel1.add(new JLabel("  Language"), c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 2;
        c.gridheight = 2;
        c.gridx = 1;
        c.gridy = 4;
        language = getJTextField("simple");
        jPanel1.add(language, c);













        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 8;
        final JButton okButton = getJButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(actionEvent.getSource().equals(okButton)){
                    /*
                    System.out.println("button clicked");
                    System.out.println(dataSourceSelection.getSelectedItem());
                    System.out.println(maxThread.getText());
                    System.out.println(heapSize.getText());
                    System.out.println(stackSize.getText());
                    System.out.println(language.getText());
                    */

                    if(dataSourceSelection.getSelectedItem().toString().contentEquals("H2")){
                        Object[] options = {"No I don't want H2", "Yep I Know it"};
                        int n = JOptionPane.showOptionDialog(new JFrame("Wikibrain"), "You have selected H2 database. \n H2 works out of box and does not require further configuration. \n But it does not support the Wikibrain spatial module", "Wikibrain Data Source", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

                        if(n == 1){
                            //System.out.println("OK clicked");
                            JPanel panel = new JPanel(new GridLayout(6,2));

                            panel.add(new JLabel("H2 Path"));
                            panel.add(h2Path);

                            final JButton defaultButton = getJButton("Restore Default");
                            defaultButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent actionEvent) {
                                    if (actionEvent.getSource().equals(defaultButton)) {
                                        h2Path.setText("\"${baseDir}\"/db/h2");

                                    }


                                }
                            });
                            panel.add(new JLabel(" "));
                            panel.add(defaultButton);


                            String[] options2 = new String[]{"Cancel", "OK"};
                            int option = JOptionPane.showOptionDialog(null, panel, "Postgres Settings",
                                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                                    null, options2, options2[1]);
                            if(option == 1) // pressing OK button
                            {
                                char[] password = pass.getPassword();
                                //System.out.println("Your h2 path is  " + h2Path.getText());
                                int phaseOption = JOptionPane.showOptionDialog(null, getPhaseSelector(), "Phase Selector",
                                        JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                                        null, options2, options2[1]);
                                if(phaseOption == 1){
                                    createConfig();
                                }

                            }
                        }
                    }

                    if(dataSourceSelection.getSelectedItem().toString().contentEquals("Postgres")){
                        Object[] options = {"No I don't have Postgres ready", "Sure"};
                        int n = JOptionPane.showOptionDialog(new JFrame("Wikibrain"), "You have selected Postgres database. \n Make sure that you have already installed Postgres on your machine \n We will help you connect Wikibrain to your Postgres database", "Wikibrain Data Source", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

                        if(n == 1){
                            //System.out.println("OK clicked");
                            JPanel panel = new JPanel(new GridLayout(0,2));

                            panel.add(new JLabel("Postgres Host"));
                            panel.add(postgresHost);
                            panel.add(new JLabel("Port Number"));
                            panel.add(portNo);
                            panel.add(new JLabel("Wikibrain Database Name"));
                            panel.add(postgresDB);
                            panel.add(new JLabel("Wikibrain Spatial Database Name"));
                            panel.add(postgresSpatialDB);
                            panel.add(new JLabel("Postgres Username"));
                            panel.add(username);
                            panel.add(new JLabel("Postgres Password"));
                            panel.add(pass);


                            final JButton defaultButton = new JButton("Restore Default");
                            defaultButton.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent actionEvent) {
                                    if (actionEvent.getSource().equals(defaultButton)) {
                                        postgresHost.setText("localhost");
                                        portNo.setText("5432");
                                        postgresDB.setText("wikibrain");
                                        postgresSpatialDB.setText("wikibrain_spatial");
                                        username.setText("");
                                        pass.setText("");

                                    }


                                }
                            });

                            panel.add(new JLabel(""));


                            panel.add(defaultButton);
                            //panel.add(new JLabel("hello"));


                            String[] options2 = new String[]{"Cancel", "OK"};
                            int option = JOptionPane.showOptionDialog(null, panel, "Postgres Settings",
                                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                                    null, options2, options2[1]);
                            if(option == 1) // pressing OK button
                            {
                                char[] password = pass.getPassword();
                                /*
                                System.out.println("Your postgres host is  " + postgresHost.getText() + ":" + portNo.getText());
                                System.out.println("Your postgres db is  " + postgresDB.getText());
                                System.out.println("Your postgres spatial db is  " + postgresSpatialDB.getText());
                                System.out.println("Your username is:  " + username.getText());
                                System.out.println("Your password is:  " + new String(password));
                               */
                                int phaseOption = JOptionPane.showOptionDialog(null, getPhaseSelector(), "Phase Selector",
                                        JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                                        null, options2, options2[1]);
                                if(phaseOption == 1){
                                    createConfig();
                                }


                            }
                        }
                    }
                }


            }
        });

        jPanel1.add(okButton, c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 8;
        final JButton cancelButton = getJButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(actionEvent.getSource().equals(cancelButton)){
                    //System.out.println("cancel clicked");
                    System.exit(0);
                }


            }
        });
        jPanel1.add(cancelButton, c);


        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 1;
        c.gridx = 2;
        c.gridy = 8;
        final JButton defaultButton = getJButton("Restore Default");
        defaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (actionEvent.getSource().equals(defaultButton)) {
                    dataSourceSelection.setSelectedIndex(0);
                    //heapSize.setText("4G");
                    //stackSize.setText("1G");
                    maxThread.setText("-1");
                    language.setText("simple");

                }


            }
        });
        jPanel1.add(defaultButton, c);





        this.setTitle("WikiBrain Configuration");
    }

    private javax.swing.JLabel getJLabel() {
        if(jLabel == null) {
            jLabel = new javax.swing.JLabel();
            jLabel.setBorder(BorderFactory.createEmptyBorder());
            jLabel.setText("H2 database is easier to config\nPostgres provides better flexibility\n(Postgres is required for spatial module)");
        }
        return jLabel;
    }

    private JComboBox getDataSourceSelection() {
        if(dataSourceSelection == null) {
            String[] dataSources = {"H2", "Postgres"};
            dataSourceSelection = new JComboBox(dataSources);
            //dataSourceSelection.setBorder(BorderFactory.createTitledBorder("Select your data source"));
            //dataSourceSelection.setPreferredSize(new Dimension(600,80));

        }
        return  dataSourceSelection;
    }

    private javax.swing.JTextField getJTextField(String def) {

            JTextField jTextField = new javax.swing.JTextField();
            //jTextField.setBorder(BorderFactory.createTitledBorder(prompt));
           // jTextField.setBorder(BorderFactory.createTitledBorder(prompt));

            //jTextField.setPreferredSize(new Dimension(600, 40));
            //jTextField.setSize(600, 40);
            jTextField.setText(def);

        return jTextField;
    }

    private javax.swing.JButton getJButton(String s) {

        JButton jButton = new javax.swing.JButton();
        //jButton.setPreferredSize(new Dimension(100, 20));
            //jButton.setBounds(103, 110, 71, 27);
        jButton.setText(s);

        return jButton;
    }


    public static void main(String[] args)
    {
        GraphicLoader w = new GraphicLoader();
        w.setVisible(true);
        w.setDefaultCloseOperation(EXIT_ON_CLOSE);


    }


     private JPanel getPhaseSelector(){

         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
         panel.add(new JLabel("Please select phases you want to load"));
         panel.add(new JLabel(" "));

         basicWikipediaButton.setSelected(true);
         basicWikipediaButton.setEnabled(false);

         luceneButton.setSelected(true);

         phrasesButton.setSelected(true);

         if(language.getText().contains(",")){
            conceptsButton.setSelected(true);
             conceptsButton.setEnabled(false);
         }
         else{
             conceptsButton.setSelected(false);
         }

         univeralButton.setSelected(false);

         wikidataButton.setSelected(false);

         spatialButton.setSelected(false);
         if(dataSourceSelection.getSelectedIndex() == 0){
             spatialButton.setText("Spatial Data (require postgres database)");
             spatialButton.setEnabled(false);
         }

         srButton.setSelected(false);
         panel.setSize(480, 300);
         panel.add(basicWikipediaButton);
         panel.add(luceneButton);
         panel.add(phrasesButton);
         panel.add(conceptsButton);
         panel.add(univeralButton);
         panel.add(wikidataButton);
         panel.add(spatialButton);
         panel.add(srButton);

         luceneButton.addItemListener(this);
         phrasesButton.addItemListener(this);
         conceptsButton.addItemListener(this);
         univeralButton.addItemListener(this);
         wikidataButton.addItemListener(this);
         spatialButton.addItemListener(this);
         srButton.addItemListener(this);








         return  panel;



     }

    public void itemStateChanged(ItemEvent e){

        Object source = e.getItemSelectable();
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

    public void createConfig() {
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
        refObj.addProperty("maxThreads", maxThread.getText());
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
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("psql").getAsJsonObject().addProperty("username", username.getText());
        refObj.get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("psql").getAsJsonObject().addProperty("password", new String(pass.getPassword()));
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("host", postgresHost.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("port", portNo.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("database", postgresSpatialDB.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("user", username.getText());
        refObj.get("spatial").getAsJsonObject().get("dao").getAsJsonObject().get("dataSource").getAsJsonObject().get("postgis").getAsJsonObject().addProperty("passwd", new String(pass.getPassword()));


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
            StringBuffer output = new StringBuffer();
            Process p;
            this.setVisible(false);

            p = Runtime.getRuntime().exec("mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller\n");

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null){
                output.append(line + "\n");
                System.out.println(line);

            }
            p.waitFor();
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
            Loader.main(arg);


            //System.out.println(loader);
            System.exit(0);

        }
        catch (Exception e){
            e.printStackTrace();
        }





    }



}

