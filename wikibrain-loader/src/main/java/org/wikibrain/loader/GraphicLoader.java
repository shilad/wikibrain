package org.wikibrain.loader;

/**
 * Created by toby on 7/15/14.
 */

import org.wikibrain.Loader;

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
        if(username.getText().isEmpty())
            username.setText("\"\"");
        if(new String(pass.getPassword()).isEmpty())
            pass.setText("\"\"");
        ref = String.format("// A default configuration file in HOCON format, almost JSON format\n" +
                "// The file format is described at https://github.com/typesafehub/config.\n" +
                "\n" +
                "\n" +
                "// Parent directory for data files, downloads, scripts, etc.\n" +
                "baseDir : .\n" +
                "\n" +
                "\n" +
                "// Directory used for temporary files.\n" +
                "// Override this if you don't have hundreds of GBs free in your system's tmp directory.\n" +
                "tmpDir : ${baseDir}\"/.tmp\"\n" +
                "\n" +
                "\n" +
                "// Maximum number of threads that should run simultaneously\n" +
                "// defaults to Runtime.getRuntime().availableProcessors()\n" +
                "maxThreads : %s\n" +
                "\n" +
                "\n" +
                "// Language sets\n" +
                "// You can specify a custom language set from the command line.\n" +
                "// See EnvBuilder for more information.\n" +
                "languages : {\n" +
                "\n" +
                "    // by default use the languages that have local pages\n" +
                "    default : loaded\n" +
                "\n" +
                "    // languages that have local pages loaded\n" +
                "    loaded : { type : loaded }\n" +
                "\n" +
                "    // languages that have downloaded articles files\n" +
                "    downloaded : { type : downloaded }\n" +
                "\n" +
                "    // the largest world economies// A default configuration file in HOCON format, almost JSON format\n" +
                "// The file format is described at https://github.com/typesafehub/config.\n" +
                "\n" +
                "\n" +
                "// Parent directory for data files, downloads, scripts, etc.\n" +
                "baseDir : .\n" +
                "\n" +
                "\n" +
                "// Directory used for temporary files.\n" +
                "// Override this if you don't have hundreds of GBs free in your system's tmp directory.\n" +
                "tmpDir : ${baseDir}\"/.tmp\"\n" +
                "\n" +
                "\n" +
                "// Maximum number of threads that should run simultaneously\n" +
                "// defaults to Runtime.getRuntime().availableProcessors()\n" +
                "maxThreads : %s\n" +
                "\n" +
                "\n" +
                "// Language sets\n" +
                "// You can specify a custom language set from the command line.\n" +
                "// See EnvBuilder for more information.\n" +
                "languages : {\n" +
                "\n" +
                "    // by default use the languages that have local pages\n" +
                "    default : loaded\n" +
                "\n" +
                "    // languages that have local pages loaded\n" +
                "    loaded : { type : loaded }\n" +
                "\n" +
                "    // languages that have downloaded articles files\n" +
                "    downloaded : { type : downloaded }\n" +
                "\n" +
                "    // the largest world economies\n" +
                "    big-economies : {\n" +
                "        type : custom\n" +
                "        langCodes : [\n" +
                "            \"en\",\"de\",\"fr\",\"nl\",\"it\",\"pl\",\"es\",\"ru\",\"ja\",\"pt\",\"zh\",\n" +
                "            \"sv\",\"uk\",\"ca\",\"no\",\"fi\",\"cs\",\"hu\",\"ko\",\"id\",\"tr\",\"ro\",\n" +
                "            \"sk\",\"da\",\"he\",\"simple\"\n" +
                "        ]\n" +
                "    }\n" +
                "\n" +
                "    // the english languages\n" +
                "    all-english : {\n" +
                "        type : custom\n" +
                "        langCodes : [\"en\", \"simple\"]\n" +
                "    }\n" +
                "\n" +
                "    // This can be dynamically populated using the EnvBuilder or command line\n" +
                "    manual : {\n" +
                "        type : custom\n" +
                "        langCodes : []\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// Filesets downloaded by default\n" +
                "download : {\n" +
                "    matcher : [\"articles\", \"links\"]\n" +
                "    path : ${baseDir}\"/download\"\n" +
                "    listFile : ${download.path}\"/list.tsv\"\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// Configuration for the lucene search engine.\n" +
                "lucene : {\n" +
                "    version : \"4.3\"\n" +
                "    directory : ${baseDir}\"/db/lucene\"\n" +
                "    options : {\n" +
                "        default : plaintext\n" +
                "        plaintext : {\n" +
                "            type : plaintext\n" +
                "\n" +
                "            version : ${lucene.version}\n" +
                "            directory : ${lucene.directory}\n" +
                "            namespaces : [\"article\"]\n" +
                "\n" +
                "            // TokenizerOptions\n" +
                "            caseInsensitive : true\n" +
                "            useStopWords : true\n" +
                "            useStem : true\n" +
                "\n" +
                "            // TextFieldElements\n" +
                "            title : 0\n" +
                "            redirects : false\n" +
                "            plaintext : true\n" +
                "        }\n" +
                "\n" +
                "        esa : {\n" +
                "            type : esa\n" +
                "\n" +
                "            version : ${lucene.version}\n" +
                "            directory : ${lucene.directory}\n" +
                "            namespaces : [\"article\"]\n" +
                "\n" +
                "            // TokenizerOptions\n" +
                "            caseInsensitive : true\n" +
                "            useStopWords : true\n" +
                "            useStem : true\n" +
                "\n" +
                "            // TextFieldElements\n" +
                "            title : 1\n" +
                "            redirects : true\n" +
                "            plaintext : true\n" +
                "        }\n" +
                "    }\n" +
                "    searcher : {\n" +
                "        esa : {\n" +
                "            options : esa\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// multilingual string normalizers\n" +
                "stringnormalizers {\n" +
                "    default : identity\n" +
                "\n" +
                "    // do nothing\n" +
                "    identity : {\n" +
                "        type : identity\n" +
                "    }\n" +
                "\n" +
                "    // remove punctuation\n" +
                "    simple : {\n" +
                "        type : lucene\n" +
                "        version : ${lucene.version}\n" +
                "        caseInsensitive : false\n" +
                "        useStopWords : false\n" +
                "        useStem : false\n" +
                "    }\n" +
                "\n" +
                "    // removes punctuation, folds case\n" +
                "    foldcase : {\n" +
                "        type : lucene\n" +
                "        version : ${lucene.version}\n" +
                "        caseInsensitive : true\n" +
                "        useStopWords : false\n" +
                "        useStem : false\n" +
                "    }\n" +
                "\n" +
                "    // fold case, porter stemming\n" +
                "    stemmer : {\n" +
                "        type : lucene\n" +
                "        version : ${lucene.version}\n" +
                "        caseInsensitive : true\n" +
                "        useStopWords : false\n" +
                "        useStem : true\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// phrase analyzers resolve phrases to articles and vice-versa\n" +
                "phrases {\n" +
                "    // whether or not the phrase analyzers are being loaded.\n" +
                "    // will be overridden while saving corpora to the daos\n" +
                "    loading : false\n" +
                "\n" +
                "    // base path for all phrase analyzer database\n" +
                "    path : ${baseDir}\"/db/phrases/\"\n" +
                "\n" +
                "    // which analyzers should be loaded by the loader by default\n" +
                "    toLoad :  [ \"anchortext\" ]\n" +
                "\n" +
                "    // Analyzers\n" +
                "    analyzer : {\n" +
                "        default : fast-cascading\n" +
                "        stanford : {\n" +
                "            phraseDao : stanford\n" +
                "            localPageDao : default\n" +
                "            path : ${download.path}\"/stanford-dictionary.bz2\"\n" +
                "            url : \"http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/dictionary.bz2\"\n" +
                "            type : stanford\n" +
                "            phrasePruner : {\n" +
                "                type : string\n" +
                "                minCount : 3,\n" +
                "                maxRank : 10,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            pagePruner : {\n" +
                "                type : simple\n" +
                "                minCount : 3,\n" +
                "                maxRank : 15,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            dao : {\n" +
                "                isNew : ${phrases.loading}\n" +
                "                type : objectdb\n" +
                "                normalizer : default\n" +
                "            }\n" +
                "        }\n" +
                "        anchortext : {\n" +
                "            phraseDao : anchortext\n" +
                "            localPageDao : default\n" +
                "            localLinkDao : default\n" +
                "            type : anchortext\n" +
                "            phrasePruner : {\n" +
                "                type : string\n" +
                "                minCount : 1,\n" +
                "                maxRank : 10,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            pagePruner : {\n" +
                "                type : simple\n" +
                "                minCount : 1,\n" +
                "                maxRank : 15,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            dao : {\n" +
                "                isNew : ${phrases.loading}\n" +
                "                type : objectdb\n" +
                "                normalizer : default\n" +
                "            }\n" +
                "        }\n" +
                "        anchortext-foldcase : ${phrases.analyzer.anchortext} {\n" +
                "            dao.normalizer : foldcase\n" +
                "        }\n" +
                "        anchortext-stemmed : ${phrases.analyzer.anchortext} {\n" +
                "            dao.normalizer : stemmer\n" +
                "        }\n" +
                "        lucene : {\n" +
                "            type : lucene\n" +
                "            localPageDao : default\n" +
                "        }\n" +
                "        cascading : {\n" +
                "            type : cascading\n" +
                "            delegates : [ \"stanford\", \"lucene\" ]\n" +
                "        }\n" +
                "        fast-cascading : {\n" +
                "            type : cascading\n" +
                "            delegates : [ \"anchortext\", \"lucene\" ]\n" +
                "        }\n" +
                "        titleredirect{\n" +
                "            type: titleredirect\n" +
                "            useRedirects : true\n" +
                "            localPageDao : default\n" +
                "            redirectDao : default\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    linkProbability : {\n" +
                "        objectDb : {\n" +
                "            path : ${baseDir}\"/db/phrases/linkProbability\"\n" +
                "            phraseAnalyzer : anchortext\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// data access objects\n" +
                "dao : {\n" +
                "    dataSource : {\n" +
                "        default : %s\n" +
                "        h2 : {\n" +
                "           driver : org.h2.Driver\n" +
                "           url: \"jdbc:h2:%s;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0;MAX_OPERATION_MEMORY=100000000\"\n" +
                "           username : sa\n" +
                "           password : \"\"\n" +
                "\n" +
                "           // Connection pooling\n" +
                "           // This sets the total number of jdbc connections to a minimum of 16.\n" +
                "           // partitions defaults to max(8, num-logical-cores)\n" +
                "           partitions : default\n" +
                "           connectionsPerPartition : 2\n" +
                "        }\n" +
                "        psql : {\n" +
                "           driver : org.postgresql.Driver\n" +
                "           url: \"jdbc:postgresql://%s/%s\"\n" +
                "           username : %s\n" +
                "           password : %s\n" +
                "\n" +
                "           // Connection pooling\n" +
                "           // This sets the total number of jdbc connections to a minimum of 16.\n" +
                "           // partitions defaults to max(8, num-logical-cores)\n" +
                "           partitions : default\n" +
                "           connectionsPerPartition : 2\n" +
                "        }\n" +
                "    }\n" +
                "    metaInfo : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    sqlCachePath : ${baseDir}\"/db/sql-cache\"\n" +
                "    localPage : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "    pageView : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        db : {\n" +
                "            type : db\n" +
                "        }\n" +
                "    }\n" +
                "    interLanguageLink : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "    }\n" +
                "    localLink : {\n" +
                "        default : matrix\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        matrix : {\n" +
                "            type : matrix\n" +
                "            delegate : sql\n" +
                "            path : ${baseDir}\"/db/matrix/local-link\"\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "    rawPage : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "            localPageDao : sql\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    wikidata : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "            localPageDao : sql\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    universalPage : {\n" +
                "        default : wikidata\n" +
                "        wikidata : {\n" +
                "            type : sql\n" +
                "            mapper : purewikidata\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        monolingual : {\n" +
                "           type : sql\n" +
                "           mapper : monolingual\n" +
                "           dataSource : default\n" +
                "       }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    localCategory : {\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "    }\n" +
                "    localArticle : {\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    localCategoryMember : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource: default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    localArticle : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource: default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    localCategory : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource: default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "     }\n" +
                "\n" +
                "    universalLink : {\n" +
                "        default : sql-wikidata\n" +
                "        sql-wikidata : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "            mapper : purewikidata\n" +
                "            localLinkDao : sql\n" +
                "        }\n" +
                "        skeletal-sql-wikidata : {\n" +
                "            type : skeletal-sql\n" +
                "            mapper : purewikidata\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    redirect : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "\n" +
                "mapper : {\n" +
                "    default : purewikidata\n" +
                "    monolingual : {\n" +
                "        type : monolingual\n" +
                "        algorithmId : 0     // each algorithm must have a unique ID\n" +
                "        localPageDao : sql\n" +
                "    }\n" +
                "    purewikidata : {\n" +
                "        type : purewikidata\n" +
                "        algorithmId : 1\n" +
                "        localPageDao : sql\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "sr : {\n" +
                "\n" +
                "    disambig : {\n" +
                "        default : similarity\n" +
                "        topResult : {\n" +
                "            type : topResult\n" +
                "            phraseAnalyzer : default\n" +
                "        }\n" +
                "        topResultConsensus : {\n" +
                "            type : topResultConsensus\n" +
                "            phraseAnalyzers : [\"lucene\",\"stanford\",\"anchortext\"]\n" +
                "        }\n" +
                "        milnewitten : {\n" +
                "            type : milnewitten\n" +
                "            metric : milnewitten\n" +
                "            phraseAnalyzer : default\n" +
                "        }\n" +
                "        similarity : {\n" +
                "            type : similarity\n" +
                "            metric : inlinknotrain\n" +
                "            phraseAnalyzer : default\n" +
                "\n" +
                "            // how to score candidate senses. Possibilities are:\n" +
                "            //      popularity: just popularity\n" +
                "            //      similarity: just similarity\n" +
                "            //      product: similarity * popularity\n" +
                "            //      sum: similarity + popularity\n" +
                "            criteria : sum\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    concepts {\n" +
                "        path : ${baseDir}\"/dat/sr/concepts/\"\n" +
                "    }\n" +
                "\n" +
                "    blacklist {\n" +
                "        path : \"\"\n" +
                "    }\n" +
                "\n" +
                "    // The parent configuration for all vector-based SR metrics\n" +
                "    vectorbase {\n" +
                "            type : vector\n" +
                "            pageDao : default\n" +
                "            disambiguator : default\n" +
                "\n" +
                "            // Concrete metrics must override the generator\n" +
                "            generator : { type : OVERRIDE_THIS }\n" +
                "\n" +
                "            // Default vector similarity is cosine similarity\n" +
                "            similarity : { type : cosine }\n" +
                "\n" +
                "            // Method for creating a feature vector for textual phrases\n" +
                "            phrases : {\n" +
                "\n" +
                "                // coefficient penalize scores for each type of candidate\n" +
                "                weights : {\n" +
                "                    dab  : 1.0\n" +
                "                    sr   : 0.5\n" +
                "                    text : 0.5\n" +
                "                }\n" +
                "\n" +
                "                numCandidates {\n" +
                "                    used  : 1     // number of candidates actually used\n" +
                "                    dab   : 1     // number of disambiguation candidates\n" +
                "                    text  : 0     // number of candidates text heuristic can propose\n" +
                "                    sr    : 0     // number of related candidates sr heuristic can propose\n" +
                "                    perSr : 0     // number of candidates sr heuristic can propose per related candidate\n" +
                "                }\n" +
                "\n" +
                "                // lucene analyzer used to find similar text\n" +
                "                lucene : default\n" +
                "            }\n" +
                "\n" +
                "            // normalizers\n" +
                "            similaritynormalizer : percentile\n" +
                "            mostsimilarnormalizer : percentile\n" +
                "\n" +
                "            // Controls how phrase vectors are created. Values can be:\n" +
                "            //      none: do not create phrase vectors. disambiguate instead.\n" +
                "            //      generator: ask the feature generator to create the phrase vectors\n" +
                "            //      creator: ask the phrase vector create to create the phrase vectors\n" +
                "            //      both: first ask the generator, then the creator\n" +
                "            phraseMode : none\n" +
                "    }\n" +
                "\n" +
                "    metric {\n" +
                "        // when training, normalizers are not read from disk\n" +
                "        training : false\n" +
                "\n" +
                "        path : ${baseDir}\"/dat/sr/\"\n" +
                "        local : {\n" +
                "            default : ensemble\n" +
                "            ESA : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : esa\n" +
                "                    luceneSearcher : esa\n" +
                "                    concepts : ${sr.concepts.path}\n" +
                "                }\n" +
                "                similarity : { type : cosine }\n" +
                "                phraseMode : generator\n" +
                "            }\n" +
                "            word2vec : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : word2vec\n" +
                "                    corpus : standard\n" +
                "                    modelDir : ${baseDir}\"/dat/word2vec\"\n" +
                "                }\n" +
                "                similarity : { type : cosine }\n" +
                "                phraseMode : generator\n" +
                "            }\n" +
                "            ESAnotrain : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : esa\n" +
                "                    luceneSearcher : esa\n" +
                "                    concepts : ${sr.concepts.path}\n" +
                "                }\n" +
                "                similarity : { type : cosine }\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "                phraseMode : generator\n" +
                "            }\n" +
                "            outlink : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : links\n" +
                "                    outLinks : true\n" +
                "                    weightByPopularity : true\n" +
                "                    logTransform : true\n" +
                "                }\n" +
                "                similarity : {\n" +
                "                    type : cosine\n" +
                "                }\n" +
                "            }\n" +
                "            inlink : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : links\n" +
                "                    outLinks : false\n" +
                "                }\n" +
                "                similarity : {\n" +
                "                    type : google\n" +
                "                }\n" +
                "            }\n" +
                "            inlinknotrain : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : links\n" +
                "                    outLinks : false\n" +
                "                }\n" +
                "                similarity : {\n" +
                "                    type : google\n" +
                "                }\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "            }\n" +
                "            milnewitten : {\n" +
                "                type : milnewitten\n" +
                "                inlink : inlink\n" +
                "                outlink : outlink\n" +
                "                disambiguator : milnewitten\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "            }\n" +
                "            simplemilnewitten : {\n" +
                "                type : simplemilnewitten\n" +
                "            }\n" +
                "            fast-ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"milnewitten\",\"milnewittenout\"]\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "                ensemble : linear\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"ESA\",\"inlink\",\"outlink\",\"category\"]\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "                ensemble : linear\n" +
                "                resolvephrases : false\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            word2vec-ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"ESA\",\"inlink\",\"outlink\",\"category\",\"word2vec\",\"milnewitten\"]\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "                ensemble : linear\n" +
                "                resolvephrases : false\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            super-ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"ESA\",\"inlink\",\"outlink\",\"category\",\"mostsimilarcosine\"]\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "                ensemble : linear\n" +
                "                resolvephrases : false\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            mostsimilarcosine : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : mostsimilarconcepts\n" +
                "                    basemetric : ensemble\n" +
                "                    concepts : ${sr.concepts.path}\n" +
                "                }\n" +
                "            }\n" +
                "            category :{\n" +
                "                type : categorygraphsimilarity\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "                categoryMemberDao : default\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    corpus {\n" +
                "        standard : {\n" +
                "            path : ${baseDir}\"/dat/corpus/standard/\"\n" +
                "            wikifier : default\n" +
                "            rawPageDao : default\n" +
                "            localPageDao : default\n" +
                "            phraseAnalyzer : anchortext\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    ensemble {\n" +
                "        default : linear\n" +
                "        even : {\n" +
                "            type : even\n" +
                "        }\n" +
                "        linear : {\n" +
                "            type : linear\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    normalizer {\n" +
                "        defaultmaxresults : 100\n" +
                "        identity : {\n" +
                "            type : identity\n" +
                "        }\n" +
                "        logLoess : {\n" +
                "            type : loess\n" +
                "            log : true\n" +
                "        }\n" +
                "        loess : {\n" +
                "            type : loess\n" +
                "        }\n" +
                "        log : {\n" +
                "            type : log\n" +
                "        }\n" +
                "        percentile : {\n" +
                "            type : percentile\n" +
                "        }\n" +
                "        range : {\n" +
                "            type : range\n" +
                "            min : 0.0\n" +
                "            max : 1.0\n" +
                "            truncate : true\n" +
                "        }\n" +
                "        rank : {\n" +
                "            type : rank\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    explanationformatter {\n" +
                "        explanationformatter {\n" +
                "            localpagedao : sql\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    dataset : {\n" +
                "        dao : {\n" +
                "            resource : {\n" +
                "                type : resource\n" +
                "                disambig : topResult\n" +
                "                resolvePhrases : true\n" +
                "            }\n" +
                "        }\n" +
                "        defaultsets : [\"wordsim353.txt\",\"MC.txt\"]\n" +
                "        groups : {\n" +
                "                // large, commonly used datasets\n" +
                "                major-en : [\"wordsim353.txt\", \"MTURK-771.csv\", \"atlasify240.txt\", \"radinsky.txt\"]\n" +
                "        }\n" +
                "        // pairs under this threshold won't be used for most similar training.\n" +
                "        mostSimilarThreshold : 0.7\n" +
                "        records : ${baseDir}\"/dat/records/\"\n" +
                "    }\n" +
                "\n" +
                "    wikifier : {\n" +
                "        milnewitten : {\n" +
                "            phraseAnalyzer : anchortext\n" +
                "            sr : inlinknotrain\n" +
                "            localLinkDao : matrix\n" +
                "            useLinkProbabilityCache : true\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "// spatial\n" +
                "\n" +
                "spatial : {\n" +
                "\n" +
                "    dao : {\n" +
                "\n" +
                "        dataSource : {\n" +
                "\n" +
                "                // These all use keys standard to Geotools JDBC\n" +
                "                // see: http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html\n" +
                "                // change this part according to your DB settings\n" +
                "                default : postgis\n" +
                "                postgis : {\n" +
                "                    dbtype : postgis\n" +
                "                    host : %s\n" +
                "                    port : %s\n" +
                "                    schema : public\n" +
                "                    database : %s\n" +
                "                    user : %s\n" +
                "                    passwd : %s\n" +
                "                    max connections : 19\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        spatialData : {\n" +
                "            default : postgis\n" +
                "            postgis{\n" +
                "                dataSource : postgis\n" +
                "            }\n" +
                "        }\n" +
                "        spatialContainment : {\n" +
                "            default : postgis\n" +
                "            postgis{\n" +
                "                dataSource : postgis\n" +
                "            }\n" +
                "        }\n" +
                "        spatialNeighbor : {\n" +
                "            default : postgis\n" +
                "            postgis{\n" +
                "                dataSource : postgis\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "loader {\n" +
                "    groups {\n" +
                "        core : [ \"fetchlinks\", \"download\", \"dumploader\", \"redirects\", \"wikitext\", \"lucene\", \"phrases\"],\n" +
                "        multilingual-core : ${loader.groups.core} [\"concepts\"]\n" +
                "    }\n" +
                "    // Stages of the loading pipeline, used by PipelineLoader\n" +
                "    stages : [\n" +
                "            {\n" +
                "                name : fetchlinks,\n" +
                "                    class : org.wikibrain.download.RequestedLinkGetter\n" +
                "                extraArgs : [],\n" +
                "            },\n" +
                "            {\n" +
                "                name : download,\n" +
                "                class : org.wikibrain.download.DumpFileDownloader\n" +
                "                dependsOnStage : fetchlinks\n" +
                "                extraArgs : [],\n" +
                "            },\n" +
                "            {\n" +
                "                name : dumploader,\n" +
                "                class : org.wikibrain.loader.DumpLoader\n" +
                "                dependsOnStage : download\n" +
                "                loadsClass : LocalPage\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : redirects,\n" +
                "                class : org.wikibrain.loader.RedirectLoader\n" +
                "                dependsOnStage : dumploader\n" +
                "                loadsClass : Redirect\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : wikitext,\n" +
                "                class : org.wikibrain.loader.WikiTextLoader\n" +
                "                loadsClass : LocalLink\n" +
                "                dependsOnStage : redirects\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : lucene,\n" +
                "                class : org.wikibrain.loader.LuceneLoader\n" +
                "                loadsClass : LuceneSearcher\n" +
                "                dependsOnStage : wikitext\n" +
                "                extraArgs : [],\n" +
                "            },\n" +
                "            {\n" +
                "                name : phrases,\n" +
                "                class : org.wikibrain.loader.PhraseLoader\n" +
                "                loadsClass: PrunedCounts\n" +
                "                dependsOnStage : wikitext\n" +
                "                extraArgs : [\"-p\", \"anchortext\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : concepts,\n" +
                "                class : org.wikibrain.loader.ConceptLoader\n" +
                "                dependsOnStage : redirects\n" +
                "                loadsClass : UniversalPage\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : universal,\n" +
                "                class : org.wikibrain.loader.UniversalLinkLoader\n" +
                "                dependsOnStage : [ \"concepts\", \"wikitext\" ]\n" +
                "                loadsClass: UniversalLink\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : wikidata,\n" +
                "                class : org.wikibrain.wikidata.WikidataDumpLoader\n" +
                "                dependsOnStage : concepts\n" +
                "                loadsClass: WikidataEntity\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : spatial,\n" +
                "                class : org.wikibrain.spatial.loader.SpatialDataLoader\n" +
                "                dependsOnStage : wikidata\n" +
                "                loadsClass: Geometry\n" +
                "                extraArgs : [\"-s\", \"wikidata\"],\n" +
                "            }\n" +
                "            {\n" +
                "                name : sr,\n" +
                "                class : org.wikibrain.sr.SRBuilder\n" +
                "                dependsOnStage : [\"wikitext\", \"phrases\", \"lucene\"]\n" +
                "                extraArgs : [\"-m\", \"ensemble\", \"-o\", \"similarity\"],\n" +
                "            }\n" +
                "    ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// backup for integration tests\n" +
                "integration {\n" +
                "    dir : ${baseDir}\"/backup\"\n" +
                "}\n" +
                "\n" +
                "    big-economies : {\n" +
                "        type : custom\n" +
                "        langCodes : [\n" +
                "            \"en\",\"de\",\"fr\",\"nl\",\"it\",\"pl\",\"es\",\"ru\",\"ja\",\"pt\",\"zh\",\n" +
                "            \"sv\",\"uk\",\"ca\",\"no\",\"fi\",\"cs\",\"hu\",\"ko\",\"id\",\"tr\",\"ro\",\n" +
                "            \"sk\",\"da\",\"he\",\"simple\"\n" +
                "        ]\n" +
                "    }\n" +
                "\n" +
                "    // the english languages\n" +
                "    all-english : {\n" +
                "        type : custom\n" +
                "        langCodes : [\"en\", \"simple\"]\n" +
                "    }\n" +
                "\n" +
                "    // This can be dynamically populated using the EnvBuilder or command line\n" +
                "    manual : {\n" +
                "        type : custom\n" +
                "        langCodes : []\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// Filesets downloaded by default\n" +
                "download : {\n" +
                "    matcher : [\"articles\", \"links\"]\n" +
                "    path : ${baseDir}\"/download\"\n" +
                "    listFile : ${download.path}\"/list.tsv\"\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// Configuration for the lucene search engine.\n" +
                "lucene : {\n" +
                "    version : \"4.3\"\n" +
                "    directory : ${baseDir}\"/db/lucene\"\n" +
                "    options : {\n" +
                "        default : plaintext\n" +
                "        plaintext : {\n" +
                "            type : plaintext\n" +
                "\n" +
                "            version : ${lucene.version}\n" +
                "            directory : ${lucene.directory}\n" +
                "            namespaces : [\"article\"]\n" +
                "\n" +
                "            // TokenizerOptions\n" +
                "            caseInsensitive : true\n" +
                "            useStopWords : true\n" +
                "            useStem : true\n" +
                "\n" +
                "            // TextFieldElements\n" +
                "            title : 0\n" +
                "            redirects : false\n" +
                "            plaintext : true\n" +
                "        }\n" +
                "\n" +
                "        esa : {\n" +
                "            type : esa\n" +
                "\n" +
                "            version : ${lucene.version}\n" +
                "            directory : ${lucene.directory}\n" +
                "            namespaces : [\"article\"]\n" +
                "\n" +
                "            // TokenizerOptions\n" +
                "            caseInsensitive : true\n" +
                "            useStopWords : true\n" +
                "            useStem : true\n" +
                "\n" +
                "            // TextFieldElements\n" +
                "            title : 1\n" +
                "            redirects : true\n" +
                "            plaintext : true\n" +
                "        }\n" +
                "    }\n" +
                "    searcher : {\n" +
                "        esa : {\n" +
                "            options : esa\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// multilingual string normalizers\n" +
                "stringnormalizers {\n" +
                "    default : identity\n" +
                "\n" +
                "    // do nothing\n" +
                "    identity : {\n" +
                "        type : identity\n" +
                "    }\n" +
                "\n" +
                "    // remove punctuation\n" +
                "    simple : {\n" +
                "        type : lucene\n" +
                "        version : ${lucene.version}\n" +
                "        caseInsensitive : false\n" +
                "        useStopWords : false\n" +
                "        useStem : false\n" +
                "    }\n" +
                "\n" +
                "    // removes punctuation, folds case\n" +
                "    foldcase : {\n" +
                "        type : lucene\n" +
                "        version : ${lucene.version}\n" +
                "        caseInsensitive : true\n" +
                "        useStopWords : false\n" +
                "        useStem : false\n" +
                "    }\n" +
                "\n" +
                "    // fold case, porter stemming\n" +
                "    stemmer : {\n" +
                "        type : lucene\n" +
                "        version : ${lucene.version}\n" +
                "        caseInsensitive : true\n" +
                "        useStopWords : false\n" +
                "        useStem : true\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// phrase analyzers resolve phrases to articles and vice-versa\n" +
                "phrases {\n" +
                "    // whether or not the phrase analyzers are being loaded.\n" +
                "    // will be overridden while saving corpora to the daos\n" +
                "    loading : false\n" +
                "\n" +
                "    // base path for all phrase analyzer database\n" +
                "    path : ${baseDir}\"/db/phrases/\"\n" +
                "\n" +
                "    // which analyzers should be loaded by the loader by default\n" +
                "    toLoad :  [ \"anchortext\" ]\n" +
                "\n" +
                "    // Analyzers\n" +
                "    analyzer : {\n" +
                "        default : fast-cascading\n" +
                "        stanford : {\n" +
                "            phraseDao : stanford\n" +
                "            localPageDao : default\n" +
                "            path : ${download.path}\"/stanford-dictionary.bz2\"\n" +
                "            url : \"http://www-nlp.stanford.edu/pubs/crosswikis-data.tar.bz2/dictionary.bz2\"\n" +
                "            type : stanford\n" +
                "            phrasePruner : {\n" +
                "                type : string\n" +
                "                minCount : 3,\n" +
                "                maxRank : 10,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            pagePruner : {\n" +
                "                type : simple\n" +
                "                minCount : 3,\n" +
                "                maxRank : 15,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            dao : {\n" +
                "                isNew : ${phrases.loading}\n" +
                "                type : objectdb\n" +
                "                normalizer : default\n" +
                "            }\n" +
                "        }\n" +
                "        anchortext : {\n" +
                "            phraseDao : anchortext\n" +
                "            localPageDao : default\n" +
                "            localLinkDao : default\n" +
                "            type : anchortext\n" +
                "            phrasePruner : {\n" +
                "                type : string\n" +
                "                minCount : 1,\n" +
                "                maxRank : 10,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            pagePruner : {\n" +
                "                type : simple\n" +
                "                minCount : 1,\n" +
                "                maxRank : 15,\n" +
                "                minFraction : 0.001\n" +
                "            }\n" +
                "            dao : {\n" +
                "                isNew : ${phrases.loading}\n" +
                "                type : objectdb\n" +
                "                normalizer : default\n" +
                "            }\n" +
                "        }\n" +
                "        anchortext-foldcase : ${phrases.analyzer.anchortext} {\n" +
                "            dao.normalizer : foldcase\n" +
                "        }\n" +
                "        anchortext-stemmed : ${phrases.analyzer.anchortext} {\n" +
                "            dao.normalizer : stemmer\n" +
                "        }\n" +
                "        lucene : {\n" +
                "            type : lucene\n" +
                "            localPageDao : default\n" +
                "        }\n" +
                "        cascading : {\n" +
                "            type : cascading\n" +
                "            delegates : [ \"stanford\", \"lucene\" ]\n" +
                "        }\n" +
                "        fast-cascading : {\n" +
                "            type : cascading\n" +
                "            delegates : [ \"anchortext\", \"lucene\" ]\n" +
                "        }\n" +
                "        titleredirect{\n" +
                "            type: titleredirect\n" +
                "            useRedirects : true\n" +
                "            localPageDao : default\n" +
                "            redirectDao : default\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    linkProbability : {\n" +
                "        objectDb : {\n" +
                "            path : ${baseDir}\"/db/phrases/linkProbability\"\n" +
                "            phraseAnalyzer : anchortext\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// data access objects\n" +
                "dao : {\n" +
                "    dataSource : {\n" +
                "        default : h2\n" +
                "        h2 : {\n" +
                "           driver : org.h2.Driver\n" +
                "           url: \"jdbc:h2:\"${baseDir}\"/db/h2;LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0;MAX_OPERATION_MEMORY=100000000\"\n" +
                "           username : sa\n" +
                "           password : \"\"\n" +
                "\n" +
                "           // Connection pooling\n" +
                "           // This sets the total number of jdbc connections to a minimum of 16.\n" +
                "           // partitions defaults to max(8, num-logical-cores)\n" +
                "           partitions : default\n" +
                "           connectionsPerPartition : 2\n" +
                "        }\n" +
                "        psql : {\n" +
                "           driver : org.postgresql.Driver\n" +
                "           url: \"jdbc:postgresql://localhost/wikibrain\"\n" +
                "           username : toby\n" +
                "           password : \"\"\n" +
                "\n" +
                "           // Connection pooling\n" +
                "           // This sets the total number of jdbc connections to a minimum of 16.\n" +
                "           // partitions defaults to max(8, num-logical-cores)\n" +
                "           partitions : default\n" +
                "           connectionsPerPartition : 2\n" +
                "        }\n" +
                "    }\n" +
                "    metaInfo : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    sqlCachePath : ${baseDir}\"/db/sql-cache\"\n" +
                "    localPage : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "    pageView : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        db : {\n" +
                "            type : db\n" +
                "        }\n" +
                "    }\n" +
                "    interLanguageLink : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "    }\n" +
                "    localLink : {\n" +
                "        default : matrix\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        matrix : {\n" +
                "            type : matrix\n" +
                "            delegate : sql\n" +
                "            path : ${baseDir}\"/db/matrix/local-link\"\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "    rawPage : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "            localPageDao : sql\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    wikidata : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "            localPageDao : sql\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    universalPage : {\n" +
                "        default : wikidata\n" +
                "        wikidata : {\n" +
                "            type : sql\n" +
                "            mapper : purewikidata\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        monolingual : {\n" +
                "           type : sql\n" +
                "           mapper : monolingual\n" +
                "           dataSource : default\n" +
                "       }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    localCategory : {\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "    }\n" +
                "    localArticle : {\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    localCategoryMember : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource: default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    localArticle : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource: default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    localCategory : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource: default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "     }\n" +
                "\n" +
                "    universalLink : {\n" +
                "        default : sql-wikidata\n" +
                "        sql-wikidata : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "            mapper : purewikidata\n" +
                "            localLinkDao : sql\n" +
                "        }\n" +
                "        skeletal-sql-wikidata : {\n" +
                "            type : skeletal-sql\n" +
                "            mapper : purewikidata\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {}\n" +
                "    }\n" +
                "    redirect : {\n" +
                "        default : sql\n" +
                "        sql : {\n" +
                "            type : sql\n" +
                "            dataSource : default\n" +
                "        }\n" +
                "        live : {\n" +
                "            type : live\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "\n" +
                "mapper : {\n" +
                "    default : purewikidata\n" +
                "    monolingual : {\n" +
                "        type : monolingual\n" +
                "        algorithmId : 0     // each algorithm must have a unique ID\n" +
                "        localPageDao : sql\n" +
                "    }\n" +
                "    purewikidata : {\n" +
                "        type : purewikidata\n" +
                "        algorithmId : 1\n" +
                "        localPageDao : sql\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "sr : {\n" +
                "\n" +
                "    disambig : {\n" +
                "        default : similarity\n" +
                "        topResult : {\n" +
                "            type : topResult\n" +
                "            phraseAnalyzer : default\n" +
                "        }\n" +
                "        topResultConsensus : {\n" +
                "            type : topResultConsensus\n" +
                "            phraseAnalyzers : [\"lucene\",\"stanford\",\"anchortext\"]\n" +
                "        }\n" +
                "        milnewitten : {\n" +
                "            type : milnewitten\n" +
                "            metric : milnewitten\n" +
                "            phraseAnalyzer : default\n" +
                "        }\n" +
                "        similarity : {\n" +
                "            type : similarity\n" +
                "            metric : inlinknotrain\n" +
                "            phraseAnalyzer : default\n" +
                "\n" +
                "            // how to score candidate senses. Possibilities are:\n" +
                "            //      popularity: just popularity\n" +
                "            //      similarity: just similarity\n" +
                "            //      product: similarity * popularity\n" +
                "            //      sum: similarity + popularity\n" +
                "            criteria : sum\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    concepts {\n" +
                "        path : ${baseDir}\"/dat/sr/concepts/\"\n" +
                "    }\n" +
                "\n" +
                "    blacklist {\n" +
                "        path : \"\"\n" +
                "    }\n" +
                "\n" +
                "    // The parent configuration for all vector-based SR metrics\n" +
                "    vectorbase {\n" +
                "            type : vector\n" +
                "            pageDao : default\n" +
                "            disambiguator : default\n" +
                "\n" +
                "            // Concrete metrics must override the generator\n" +
                "            generator : { type : OVERRIDE_THIS }\n" +
                "\n" +
                "            // Default vector similarity is cosine similarity\n" +
                "            similarity : { type : cosine }\n" +
                "\n" +
                "            // Method for creating a feature vector for textual phrases\n" +
                "            phrases : {\n" +
                "\n" +
                "                // coefficient penalize scores for each type of candidate\n" +
                "                weights : {\n" +
                "                    dab  : 1.0\n" +
                "                    sr   : 0.5\n" +
                "                    text : 0.5\n" +
                "                }\n" +
                "\n" +
                "                numCandidates {\n" +
                "                    used  : 1     // number of candidates actually used\n" +
                "                    dab   : 1     // number of disambiguation candidates\n" +
                "                    text  : 0     // number of candidates text heuristic can propose\n" +
                "                    sr    : 0     // number of related candidates sr heuristic can propose\n" +
                "                    perSr : 0     // number of candidates sr heuristic can propose per related candidate\n" +
                "                }\n" +
                "\n" +
                "                // lucene analyzer used to find similar text\n" +
                "                lucene : default\n" +
                "            }\n" +
                "\n" +
                "            // normalizers\n" +
                "            similaritynormalizer : percentile\n" +
                "            mostsimilarnormalizer : percentile\n" +
                "\n" +
                "            // Controls how phrase vectors are created. Values can be:\n" +
                "            //      none: do not create phrase vectors. disambiguate instead.\n" +
                "            //      generator: ask the feature generator to create the phrase vectors\n" +
                "            //      creator: ask the phrase vector create to create the phrase vectors\n" +
                "            //      both: first ask the generator, then the creator\n" +
                "            phraseMode : none\n" +
                "    }\n" +
                "\n" +
                "    metric {\n" +
                "        // when training, normalizers are not read from disk\n" +
                "        training : false\n" +
                "\n" +
                "        path : ${baseDir}\"/dat/sr/\"\n" +
                "        local : {\n" +
                "            default : ensemble\n" +
                "            ESA : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : esa\n" +
                "                    luceneSearcher : esa\n" +
                "                    concepts : ${sr.concepts.path}\n" +
                "                }\n" +
                "                similarity : { type : cosine }\n" +
                "                phraseMode : generator\n" +
                "            }\n" +
                "            word2vec : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : word2vec\n" +
                "                    corpus : standard\n" +
                "                    modelDir : ${baseDir}\"/dat/word2vec\"\n" +
                "                }\n" +
                "                similarity : { type : cosine }\n" +
                "                phraseMode : generator\n" +
                "            }\n" +
                "            ESAnotrain : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : esa\n" +
                "                    luceneSearcher : esa\n" +
                "                    concepts : ${sr.concepts.path}\n" +
                "                }\n" +
                "                similarity : { type : cosine }\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "                phraseMode : generator\n" +
                "            }\n" +
                "            outlink : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : links\n" +
                "                    outLinks : true\n" +
                "                    weightByPopularity : true\n" +
                "                    logTransform : true\n" +
                "                }\n" +
                "                similarity : {\n" +
                "                    type : cosine\n" +
                "                }\n" +
                "            }\n" +
                "            inlink : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : links\n" +
                "                    outLinks : false\n" +
                "                }\n" +
                "                similarity : {\n" +
                "                    type : google\n" +
                "                }\n" +
                "            }\n" +
                "            inlinknotrain : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : links\n" +
                "                    outLinks : false\n" +
                "                }\n" +
                "                similarity : {\n" +
                "                    type : google\n" +
                "                }\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "            }\n" +
                "            milnewitten : {\n" +
                "                type : milnewitten\n" +
                "                inlink : inlink\n" +
                "                outlink : outlink\n" +
                "                disambiguator : milnewitten\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "            }\n" +
                "            simplemilnewitten : {\n" +
                "                type : simplemilnewitten\n" +
                "            }\n" +
                "            fast-ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"milnewitten\",\"milnewittenout\"]\n" +
                "                similaritynormalizer : identity\n" +
                "                mostsimilarnormalizer : identity\n" +
                "                ensemble : linear\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"ESA\",\"inlink\",\"outlink\",\"category\"]\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "                ensemble : linear\n" +
                "                resolvephrases : false\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            word2vec-ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"ESA\",\"inlink\",\"outlink\",\"category\",\"word2vec\",\"milnewitten\"]\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "                ensemble : linear\n" +
                "                resolvephrases : false\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            super-ensemble : {\n" +
                "                type : ensemble\n" +
                "                metrics : [\"ESA\",\"inlink\",\"outlink\",\"category\",\"mostsimilarcosine\"]\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "                ensemble : linear\n" +
                "                resolvephrases : false\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "            }\n" +
                "            mostsimilarcosine : ${sr.vectorbase} {\n" +
                "                generator : {\n" +
                "                    type : mostsimilarconcepts\n" +
                "                    basemetric : ensemble\n" +
                "                    concepts : ${sr.concepts.path}\n" +
                "                }\n" +
                "            }\n" +
                "            category :{\n" +
                "                type : categorygraphsimilarity\n" +
                "                disambiguator : default\n" +
                "                pageDao : default\n" +
                "                categoryMemberDao : default\n" +
                "                similaritynormalizer : percentile\n" +
                "                mostsimilarnormalizer : percentile\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    corpus {\n" +
                "        standard : {\n" +
                "            path : ${baseDir}\"/dat/corpus/standard/\"\n" +
                "            wikifier : default\n" +
                "            rawPageDao : default\n" +
                "            localPageDao : default\n" +
                "            phraseAnalyzer : anchortext\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    ensemble {\n" +
                "        default : linear\n" +
                "        even : {\n" +
                "            type : even\n" +
                "        }\n" +
                "        linear : {\n" +
                "            type : linear\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    normalizer {\n" +
                "        defaultmaxresults : 100\n" +
                "        identity : {\n" +
                "            type : identity\n" +
                "        }\n" +
                "        logLoess : {\n" +
                "            type : loess\n" +
                "            log : true\n" +
                "        }\n" +
                "        loess : {\n" +
                "            type : loess\n" +
                "        }\n" +
                "        log : {\n" +
                "            type : log\n" +
                "        }\n" +
                "        percentile : {\n" +
                "            type : percentile\n" +
                "        }\n" +
                "        range : {\n" +
                "            type : range\n" +
                "            min : 0.0\n" +
                "            max : 1.0\n" +
                "            truncate : true\n" +
                "        }\n" +
                "        rank : {\n" +
                "            type : rank\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    explanationformatter {\n" +
                "        explanationformatter {\n" +
                "            localpagedao : sql\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    dataset : {\n" +
                "        dao : {\n" +
                "            resource : {\n" +
                "                type : resource\n" +
                "                disambig : topResult\n" +
                "                resolvePhrases : true\n" +
                "            }\n" +
                "        }\n" +
                "        defaultsets : [\"wordsim353.txt\",\"MC.txt\"]\n" +
                "        groups : {\n" +
                "                // large, commonly used datasets\n" +
                "                major-en : [\"wordsim353.txt\", \"MTURK-771.csv\", \"atlasify240.txt\", \"radinsky.txt\"]\n" +
                "        }\n" +
                "        // pairs under this threshold won't be used for most similar training.\n" +
                "        mostSimilarThreshold : 0.7\n" +
                "        records : ${baseDir}\"/dat/records/\"\n" +
                "    }\n" +
                "\n" +
                "    wikifier : {\n" +
                "        milnewitten : {\n" +
                "            phraseAnalyzer : anchortext\n" +
                "            sr : inlinknotrain\n" +
                "            localLinkDao : matrix\n" +
                "            useLinkProbabilityCache : true\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "// spatial\n" +
                "\n" +
                "spatial : {\n" +
                "\n" +
                "    dao : {\n" +
                "\n" +
                "        dataSource : {\n" +
                "\n" +
                "                // These all use keys standard to Geotools JDBC\n" +
                "                // see: http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html\n" +
                "                // change this part according to your DB settings\n" +
                "                default : postgis\n" +
                "                postgis : {\n" +
                "                    dbtype : postgis\n" +
                "                    host : localhost\n" +
                "                    port : 5432\n" +
                "                    schema : public\n" +
                "                    database : wikibrain_spatial\n" +
                "                    user : toby\n" +
                "                    passwd : \"\"\n" +
                "                    max connections : 19\n" +
                "                }\n" +
                "            }\n" +
                "\n" +
                "        spatialData : {\n" +
                "            default : postgis\n" +
                "            postgis{\n" +
                "                dataSource : postgis\n" +
                "            }\n" +
                "        }\n" +
                "        spatialContainment : {\n" +
                "            default : postgis\n" +
                "            postgis{\n" +
                "                dataSource : postgis\n" +
                "            }\n" +
                "        }\n" +
                "        spatialNeighbor : {\n" +
                "            default : postgis\n" +
                "            postgis{\n" +
                "                dataSource : postgis\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "}\n" +
                "\n" +
                "loader {\n" +
                "    groups {\n" +
                "        core : [ \"fetchlinks\", \"download\", \"dumploader\", \"redirects\", \"wikitext\", \"lucene\", \"phrases\"],\n" +
                "        multilingual-core : ${loader.groups.core} [\"concepts\"]\n" +
                "    }\n" +
                "    // Stages of the loading pipeline, used by PipelineLoader\n" +
                "    stages : [\n" +
                "            {\n" +
                "                name : fetchlinks,\n" +
                "                    class : org.wikibrain.download.RequestedLinkGetter\n" +
                "                extraArgs : [],\n" +
                "            },\n" +
                "            {\n" +
                "                name : download,\n" +
                "                class : org.wikibrain.download.DumpFileDownloader\n" +
                "                dependsOnStage : fetchlinks\n" +
                "                extraArgs : [],\n" +
                "            },\n" +
                "            {\n" +
                "                name : dumploader,\n" +
                "                class : org.wikibrain.loader.DumpLoader\n" +
                "                dependsOnStage : download\n" +
                "                loadsClass : LocalPage\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : redirects,\n" +
                "                class : org.wikibrain.loader.RedirectLoader\n" +
                "                dependsOnStage : dumploader\n" +
                "                loadsClass : Redirect\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : wikitext,\n" +
                "                class : org.wikibrain.loader.WikiTextLoader\n" +
                "                loadsClass : LocalLink\n" +
                "                dependsOnStage : redirects\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : lucene,\n" +
                "                class : org.wikibrain.loader.LuceneLoader\n" +
                "                loadsClass : LuceneSearcher\n" +
                "                dependsOnStage : wikitext\n" +
                "                extraArgs : [],\n" +
                "            },\n" +
                "            {\n" +
                "                name : phrases,\n" +
                "                class : org.wikibrain.loader.PhraseLoader\n" +
                "                loadsClass: PrunedCounts\n" +
                "                dependsOnStage : wikitext\n" +
                "                extraArgs : [\"-p\", \"anchortext\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : concepts,\n" +
                "                class : org.wikibrain.loader.ConceptLoader\n" +
                "                dependsOnStage : redirects\n" +
                "                loadsClass : UniversalPage\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : universal,\n" +
                "                class : org.wikibrain.loader.UniversalLinkLoader\n" +
                "                dependsOnStage : [ \"concepts\", \"wikitext\" ]\n" +
                "                loadsClass: UniversalLink\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : wikidata,\n" +
                "                class : org.wikibrain.wikidata.WikidataDumpLoader\n" +
                "                dependsOnStage : concepts\n" +
                "                loadsClass: WikidataEntity\n" +
                "                extraArgs : [\"-d\"],\n" +
                "            },\n" +
                "            {\n" +
                "                name : spatial,\n" +
                "                class : org.wikibrain.spatial.loader.SpatialDataLoader\n" +
                "                dependsOnStage : wikidata\n" +
                "                loadsClass: Geometry\n" +
                "                extraArgs : [\"-s\", \"wikidata\"],\n" +
                "            }\n" +
                "            {\n" +
                "                name : sr,\n" +
                "                class : org.wikibrain.sr.SRBuilder\n" +
                "                dependsOnStage : [\"wikitext\", \"phrases\", \"lucene\"]\n" +
                "                extraArgs : [\"-m\", \"ensemble\", \"-o\", \"similarity\"],\n" +
                "            }\n" +
                "    ]\n" +
                "}\n" +
                "\n" +
                "\n" +
                "// backup for integration tests\n" +
                "integration {\n" +
                "    dir : ${baseDir}\"/backup\"\n" +
                "}\n", maxThread.getText(), maxThread.getText(), dsSelection, h2Path.getText(), postgresHost.getText(), postgresDB.getText(), username.getText(), new String(pass.getPassword()), postgresHost.getText(), portNo.getText(), postgresSpatialDB.getText(), username.getText(), new String(pass.getPassword()));

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

