package org.wikapidia.cookbook.wikiwalker;

import org.wikapidia.cookbook.wikiwalker.ui.WalkerViz;
import org.wikapidia.core.model.LocalPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Shilad Sen
 */
public class WikiWalker extends JFrame implements ActionListener {
    private final GraphSearcher searcher;
    private final WikAPIdiaWrapper wrapper;
    private final WalkerViz viz;
    private final JTextField srcField;
    private final JTextField destField;
    private final JLabel errorLabel;

    public WikiWalker(GraphSearcher searcher, WikAPIdiaWrapper wrapper, LocalPage start, LocalPage end, int width, int height) {
        this.searcher = searcher;
        this.wrapper = wrapper;

        setBackground(new Color(240, 240, 240));

        //Create and set up the window.
        setTitle("Comp 124 - Wiki Walker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setMinimumSize(getSize());
        this.setLayout(new BorderLayout());

        Container buttons = new Container();
        buttons.setLayout(new GridLayout(1, 6));

        JLabel srcLabel = new JLabel("Source article:   ");
        srcLabel.setHorizontalAlignment(JLabel.RIGHT);
        srcField = new JTextField();
        buttons.add(srcLabel);
        buttons.add(srcField);

        JLabel destLabel = new JLabel("Destination article:  ");
        destLabel.setHorizontalAlignment(JLabel.RIGHT);
        destField = new JTextField();

        JButton refresh = new JButton("refresh viz");
        refresh.addActionListener(this);

        buttons.add(destLabel);
        buttons.add(destField);
        buttons.add(refresh);
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        buttons.add(errorLabel);

        this.add(buttons, BorderLayout.NORTH);
        this.viz = new WalkerViz(start, end);
        getContentPane().add(viz, BorderLayout.CENTER);

        //Display the window.
        pack();
    }


    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        errorLabel.setText("");

        String srcText = srcField.getText();
        String destText = destField.getText();

        LocalPage src = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, srcText);
        LocalPage dest = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, destText);

        if (src == null || dest == null) {
            String unknown = "Unknown pages:";
            if (src == null) {
                unknown += " '" + srcText + "'";
            }
            if (dest == null) {
                unknown += " '" + destText + "'";
            }
            errorLabel.setText(unknown);
        } else {
            wrapper.setInteresting(Utils.LANG_SIMPLE, src.getLocalId(), true);
            wrapper.setInteresting(Utils.LANG_SIMPLE, dest.getLocalId(), true);
            if (searcher.shortestPath(src, dest) == null) {
                errorLabel.setText("No path between " + src.getTitle() + " and " + dest.getTitle());
            } else {
                viz.setPages(src, dest);
            }
        }
    }

    public static void main(String args[]) {
        final GraphSearcher searcher = new GraphSearcher();
        final WikAPIdiaWrapper wrapper = new WikAPIdiaWrapper(Utils.PATH_DB);
        final LocalPage sax = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Saxophone");
        final LocalPage bayes = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Bayes' theorem");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WikiWalker ex = new WikiWalker(searcher, wrapper, sax, bayes, 1280, 1024);
                ex.setVisible(true);
            }
        });
    }
}
