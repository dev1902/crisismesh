import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/** FRONTEND: Swing GUI. Buttons call the backend (MeshNode) through event listeners. */
public class CrisisMeshGUI extends JFrame implements MeshNode.Listener {
    private final JTextArea log = new JTextArea();
    private final JTextField input = new JTextField();
    private final JLabel peersLabel = new JLabel("Peers: none");
    private final MeshNode node;

    public CrisisMeshGUI(String name) throws Exception {
        super("CrisisMesh - " + name);
        node = new MeshNode(name, this);

        log.setEditable(false);
        JButton send = new JButton("Send");
        JButton sos  = new JButton("SOS!");
        JButton need = new JButton("Need Help/Supplies");
        sos.setBackground(Color.RED); sos.setForeground(Color.WHITE); sos.setOpaque(true);

        send.addActionListener(e -> { String t = input.getText().trim();
            if (!t.isEmpty()) { node.broadcast("MSG", t); append("[me] " + t); input.setText(""); } });
        sos.addActionListener(e -> { node.broadcast("SOS", "EMERGENCY! Needs immediate help"); append("[me] SOS sent"); });
        need.addActionListener(e -> { node.broadcast("NEED", "Needs food/water/medical supplies"); append("[me] Help request sent"); });
        input.addActionListener(e -> send.doClick());

        JPanel buttons = new JPanel(new GridLayout(1, 3, 5, 5));
        buttons.add(send); buttons.add(sos); buttons.add(need);
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.add(input, BorderLayout.CENTER); bottom.add(buttons, BorderLayout.EAST);

        add(peersLabel, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setSize(560, 420);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        node.start();
        setVisible(true);
    }

    private void append(String s) { SwingUtilities.invokeLater(() -> log.append(s + "\n")); }

    @Override public void onMessage(String type, String sender, String text) {
        append("[" + type + "] " + sender + ": " + text);
    }
    @Override public void onPeersChanged(Collection<String> peers) {
        SwingUtilities.invokeLater(() -> peersLabel.setText("Peers nearby: " + peers));
    }

    public static void main(String[] a) throws Exception {
        String name = a.length > 0 ? a[0] : JOptionPane.showInputDialog("Enter your node name:");
        SwingUtilities.invokeLater(() -> { try { new CrisisMeshGUI(name); } catch (Exception e) { e.printStackTrace(); } });
    }
}
