package org.wikibrain.cookbook.wikiwalker;

import org.wikibrain.cookbook.wikiwalker.ui.WalkerViz;
import org.wikibrain.core.model.LocalPage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The main WikiWalker program.
 *
 * @author Shilad Sen
 */
public class WikiWalker extends JFrame implements ActionListener {
    private final GraphSearcher searcher;
    private final WikiBrainWrapper wrapper;
    private final WalkerViz viz;
    private final JTextField srcField;
    private final JTextField destField;
    private final JLabel errorLabel;

    /**
     * Creates a WikiWalker window.
     *
     * @param searcher
     * @param wrapper
     * @param start
     * @param end
     * @param width
     * @param height
     */
    public WikiWalker(GraphSearcher searcher, WikiBrainWrapper wrapper, LocalPage start, LocalPage end, int width, int height) {
        this.searcher = searcher;
        this.wrapper = wrapper;

        setBackground(new Color(240, 240, 240));

        //Create and set up the window.
        setTitle("Comp 124 - Wiki Walker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(width, height);
        setPreferredSize(getSize());
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
        this.viz = new WalkerViz(wrapper, start, end);
        getContentPane().add(viz, BorderLayout.CENTER);

        //Display the window.
        pack();
    }

    /**
     * Event handler for the wiki walker.
     * @param actionEvent
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        errorLabel.setForeground(Color.RED);
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
            int distance = searcher.shortestDistance(src, dest);
            if (distance < 0) {
                errorLabel.setText("No path between " + src.getTitle() + " and " + dest.getTitle());
            } else {
                errorLabel.setForeground(Color.BLACK);
                errorLabel.setText("Shortest distance = " + distance);
                viz.setPages(src, dest);
            }
        }
    }

    public static void main(String args[]) {
        final WikiBrainWrapper wrapper = new WikiBrainWrapper(Utils.PATH_DB);
        final GraphSearcher searcher = new GraphSearcher(wrapper);
        final LocalPage sax = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Saxophone");
        final LocalPage bayes = wrapper.getLocalPageByTitle(Utils.LANG_SIMPLE, "Bayes' theorem");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WikiWalker ex = new WikiWalker(searcher, wrapper, sax, bayes, 1280, 800);
                ex.setVisible(true);
            }
        });
    }
}
