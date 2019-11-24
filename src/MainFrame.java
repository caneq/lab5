import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.stream.IntStream;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private JFileChooser fileChooser = new JFileChooser();
    JMenu graphicsMenu;
    private JCheckBoxMenuItem showAxisMenuItem;
    private JCheckBoxMenuItem showMarkersMenuItem;
    private JCheckBoxMenuItem showRegionsMenuItem;
    Action rotateLeftGraphicsAction;
    Action rotateRightGraphicsAction;
    private GraphicsDisplay display = new GraphicsDisplay();
    private boolean fileLoaded = false;

    public MainFrame() {
        super("Построение графиков функций на основе заранее подготовленных файлов");
        setSize(WIDTH, HEIGHT);
        fileChooser.setCurrentDirectory(new File("."));
        Toolkit kit = Toolkit.getDefaultToolkit();
        setLocation((kit.getScreenSize().width - WIDTH) / 2, (kit.getScreenSize().height - HEIGHT) / 2);
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("Файл");
        menuBar.add(fileMenu);
        Action openGraphicsAction = new AbstractAction("Открыть файл с графиком") {
            public void actionPerformed(ActionEvent event) {
                if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION)
                    openGraphics(fileChooser.getSelectedFile());
            }
        };
        fileMenu.add(openGraphicsAction);
        graphicsMenu = new JMenu("График");
        menuBar.add(graphicsMenu);
        Action showAxisAction = new AbstractAction("Показывать оси координат") {
            public void actionPerformed(ActionEvent event) {
                display.setShowAxis(showAxisMenuItem.isSelected());
            }
        };

        showAxisMenuItem = new JCheckBoxMenuItem(showAxisAction);
        graphicsMenu.add(showAxisMenuItem);
        showAxisMenuItem.setSelected(true);


        Action showMarkersAction = new AbstractAction("Показывать маркеры точек") {
            public void actionPerformed(ActionEvent event) {
                display.setShowMarkers(showMarkersMenuItem.isSelected());
            }
        };
        showMarkersMenuItem = new JCheckBoxMenuItem(showMarkersAction);
        graphicsMenu.add(showMarkersMenuItem);
        showMarkersMenuItem.setSelected(true);

        graphicsMenu.addMenuListener(new GraphicsMenuListener());
        getContentPane().add(display, BorderLayout.CENTER);


        Action showRegionsAction = new AbstractAction("Показывать замкнутые области пересечения графика с Ox") {
            public void actionPerformed(ActionEvent event) {
                display.setShowRegions(showRegionsMenuItem.isSelected());
            }
        };
        showRegionsMenuItem = new JCheckBoxMenuItem(showRegionsAction);
        showRegionsMenuItem.setSelected(false);
        graphicsMenu.add(showRegionsMenuItem);

        rotateLeftGraphicsAction = new AbstractAction("Повернуть влево на 90°") {
            public void actionPerformed(ActionEvent event) {
                display.turnLeft();
            }
        };
        rotateRightGraphicsAction = new AbstractAction("Повернуть вправо на 90°") {
            public void actionPerformed(ActionEvent event) {
                display.turnRight();
            }
        };
        graphicsMenu.add(rotateLeftGraphicsAction);
        graphicsMenu.add(rotateRightGraphicsAction);


        for(int i = 0; i < graphicsMenu.getItemCount(); i++){
            graphicsMenu.getItem(i).setEnabled(fileLoaded);
        }

    }

    protected void openGraphics(File selectedFile) {
        try {
            if(selectedFile.getName().endsWith(".bin")) {
                DataInputStream in = new DataInputStream(new FileInputStream(selectedFile));
                Double[][] graphicsData = new Double[in.available() / (Double.SIZE / 8) / 2][];
                int i = 0;
                while (in.available() > 0) {
                    Double x = in.readDouble();
                    Double y = in.readDouble();
                    graphicsData[i++] = new Double[]{x, y};
                }
                if (graphicsData != null && graphicsData.length > 0) {
                    fileLoaded = true;
                    display.showGraphics(graphicsData);
                }
                in.close();
            }
            else {
                BufferedReader reader = new BufferedReader(new FileReader(selectedFile));
                ArrayList<String> strings = new ArrayList<String>();

                while (reader.ready()) {
                    strings.add(reader.readLine());
                }

                Double[][] graphicsData = new Double[strings.size()][2];

                for (int i = 0; i < strings.size(); i++) {
                    String[] str = strings.get(i).split(" ");
                    for (int j = 0; j < str.length; j++) {
                        String s = str[j];
                        if (j == 1) {
                            str[j] = str[j].replace(".", "");
                            boolean f = true;
                            for (int h = 0; h < str[j].length() - 1; h++) {
                                if (str[j].charAt(h) > str[j].charAt(h + 1)) {
                                    f = false;
                                    break;
                                }
                            }
                        }
                        graphicsData[i][j] = Double.valueOf(s);
                    }
                }

                if (graphicsData != null && graphicsData.length > 0) {
                    fileLoaded = true;
                    display.showGraphics(graphicsData);
                }
                reader.close();
            }
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(MainFrame.this, "Указанный файл не найден", "Ошибка загрузки данных", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(MainFrame.this, "Ошибка чтения координат точек из файла", "Ошибка загрузки данных",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private class GraphicsMenuListener implements MenuListener {
        public void menuSelected(MenuEvent e) {
            IntStream.range(0, graphicsMenu.getItemCount()).forEach(i -> graphicsMenu.getItem(i).setEnabled(fileLoaded));
        }
        public void menuDeselected(MenuEvent e) {
        }

        public void menuCanceled(MenuEvent e) {
        }

    }
}