package ramo.klevis.ml.recogntion.face.ui;


import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;
import ramo.klevis.ml.recogntion.face.FaceRecognition;
import ramo.klevis.ml.ui.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.awt.event.KeyEvent.VK_ESCAPE;

/**
 * Created by Klevis Ramo
 */
@Slf4j
public class FaceRecogntionUI implements Runnable, WebcamPanel.Painter {
    public static final String FACE_RECOGNITION_SRC_MAIN_RESOURCES = "FaceRecognition/src/main/resources/images";
    private static final String BASE_PATH = "FaceRecognition/src/main/resources/";
    private JFrame mainFrame;
    private JFrame webcamFrame;
    private JPanel mainPanel;
    private static final int FRAME_WIDTH = 1024;
    private static final int FRAME_HEIGHT = 420;
    private ImagePanel sourceImagePanel;
    private FaceRecognition faceRecognition;
    private File selectedFile;
    private JTextField memberNameField;
    private final Font sansSerifBold = new Font("SansSerif", Font.BOLD, 28);
    private JPanel membersPhotosPanel;
    private JScrollPane scrollMembersPhotos;
    private JLabel whoIsLabel;
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private static final HaarCascadeDetector detector = new HaarCascadeDetector();
    private static final Stroke STROKE = new BasicStroke(4.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.0f }, 0.0f);

    private Webcam webcam = null;
    private WebcamPanel.Painter painter = null;
    private List<DetectedFace> faces = null;

    public FaceRecogntionUI() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        UIManager.put("Button.font", new FontUIResource(new Font("Dialog", Font.BOLD, 18)));
        UIManager.put("ProgressBar.font", new FontUIResource(new Font("Dialog", Font.BOLD, 18)));

    }

    public void initUI() throws Exception {

        faceRecognition = new FaceRecognition();
        faceRecognition.loadModel();
        // create main frame
        mainFrame = createMainFrame();
        webcamFrame = createWebcamFrame();

        mainPanel = new JPanel(new BorderLayout());

        JButton chooseButton = new JButton("Choose Face Image");
        chooseButton.addActionListener(e -> {
            chooseFileAction();
            mainPanel.updateUI();
        });

        JButton whoIsButton = new JButton("Who Is?");
        whoIsButton.addActionListener(event -> {
            try {
                String whoIs = faceRecognition.whoIs(selectedFile.getAbsolutePath());
                whoIsLabel.setFont(sansSerifBold);
                if (!whoIs.contains("Unknown")) {
                    whoIsLabel.setForeground(Color.GREEN.darker());
                } else {
                    whoIsLabel.setForeground(Color.RED.darker());
                }
                whoIsLabel.setText(whoIs);
                mainPanel.updateUI();
            } catch (IOException e) {
                log.error("", e);
                throw new RuntimeException(e);
            }
        });

        JButton webcamButton = new JButton("Webcam");
        webcamButton.addActionListener(e -> {
            boolean selected = selectedFile != null;
            webcamAction();
            mainPanel.updateUI();
            if (selected) {
                whoIsButton.doClick();
                mainPanel.updateUI();
            }
        });

        JButton registerNewMemberButton = new JButton("Register New Member");
        registerNewMemberButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    addPhoto(selectedFile.getAbsolutePath(), memberNameField.getText());
                    membersPhotosPanel.updateUI();
                    scrollMembersPhotos.updateUI();
                    mainPanel.updateUI();
                } catch (IOException e) {
                    log.error("", e);
                    throw new RuntimeException(e);
                }
            }
        });

        fillMainPanel(webcamButton, chooseButton, whoIsButton, registerNewMemberButton);

        mainPanel.updateUI();

        mainFrame.add(mainPanel, BorderLayout.CENTER);
        addSignature();
        mainFrame.setVisible(true);

    }

    private void webcamAction() {
        try {
            webcam = Webcam.getDefault();
            webcam.setViewSize(new Dimension(640, 480));
            webcam.open(true);

            WebcamPanel panel = new WebcamPanel(webcam, false);
            panel.setPreferredSize(WebcamResolution.VGA.getSize());
            panel.setPainter(this);
            panel.setFPSDisplayed(true);
            panel.setFPSLimited(true);
            panel.setFPSLimit(20);
            panel.setPainter(this);
            panel.start();

            painter = panel.getDefaultPainter();

            webcamFrame.add(panel);

            webcamFrame.pack();
            webcamFrame.setLocationRelativeTo(null);
            webcamFrame.setVisible(true);

            EXECUTOR.execute(this);

            BufferedImage image = webcam.getImage();
            File selfie = File.createTempFile("marcelo", ".jpg");
            ImageIO.write(image, ImageUtils.FORMAT_JPG, selfie);
            log.info("File created: {}", selfie.getAbsolutePath());
//            webcam.close();
            selectedFile = selfie;
            sourceImagePanel.setImage(selectedFile.getAbsolutePath());
            mainPanel.updateUI();
        } catch (IOException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    private void fillMainPanel(JButton webcamButton, JButton chooseButton, JButton predictButton, Component registerNewMemberButton) throws IOException {

        GridLayout layout = new GridLayout(1, 4);
        layout.setHgap(1);
        JPanel panelRegister = new JPanel(layout);
        memberNameField = new JTextField();

        panelRegister.add(registerNewMemberButton);
        panelRegister.add(memberNameField);
        panelRegister.add(webcamButton);
        panelRegister.add(chooseButton);
        panelRegister.add(predictButton);
        mainPanel.add(panelRegister, BorderLayout.NORTH);

        membersPhotosPanel = new JPanel(new GridLayout(1, 15));
        scrollMembersPhotos = new JScrollPane(membersPhotosPanel);
        mainPanel.add(scrollMembersPhotos, BorderLayout.SOUTH);

        File[] files = new File(BASE_PATH + "/images").listFiles();
        for (File file : Objects.requireNonNull(files)) {
            File[] images = file.listFiles();
            addPhoto(Objects.requireNonNull(images)[0].getAbsolutePath());

        }
        sourceImagePanel = new ImagePanel(150, 150);
        JPanel jPanel = new JPanel(new GridLayout(1, 2));
        jPanel.add(sourceImagePanel);
        whoIsLabel = new JLabel("?");
        whoIsLabel.setFont(sansSerifBold);
        whoIsLabel.setForeground(Color.BLUE.darker());
        jPanel.add(whoIsLabel);
        mainPanel.add(jPanel, BorderLayout.CENTER);
        mainPanel.updateUI();

    }

    private void addPhoto(String path) throws IOException {
        addPhoto(path, null);
    }

    private void addPhoto(String path, String name) throws IOException {
        ImagePanel imagePanel = new ImagePanel(150, 150, new File(path), name);
        faceRecognition.registerNewMember(imagePanel.getTitle(), new File(imagePanel.getFilePath()).getAbsolutePath());
        membersPhotosPanel.add(imagePanel);
    }


    public void chooseFileAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(new File(FACE_RECOGNITION_SRC_MAIN_RESOURCES).getAbsolutePath()));
        int action = chooser.showOpenDialog(null);
        if (action == JFileChooser.APPROVE_OPTION) {
            try {
                selectedFile = chooser.getSelectedFile();
                sourceImagePanel.setImage(selectedFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("", e);
                throw new RuntimeException(e);
            }
        }
    }

    private JFrame createMainFrame() {
        JFrame mainFrame = new JFrame();
        mainFrame.setTitle("Face Recognizer");
        mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        mainFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });
        return mainFrame;
    }

    private JFrame createWebcamFrame() {
        JFrame webcamFrame = new JFrame();
        webcamFrame.setTitle("Webcam");
        webcamFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        webcamFrame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        webcamFrame.setLocationRelativeTo(null);
        webcamFrame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if (e.getKeyChar() == VK_ESCAPE) {
                    webcamFrame.dispose();
                }
            }
        });
        webcamFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                if (webcam != null) {
                    webcam.close();
                }
            }
        });
        return webcamFrame;
    }

    private void addSignature() {
        JLabel signature = new JLabel("ramok.tech", SwingConstants.CENTER);
        signature.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 20));
        signature.setForeground(Color.BLUE);
        mainFrame.add(signature, BorderLayout.SOUTH);
    }

    @Override
    public void paintPanel(WebcamPanel panel, Graphics2D g2) {
        if (painter != null) {
            painter.paintPanel(panel, g2);
        }
    }

    @Override
    public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {

        if (painter != null) {
            painter.paintImage(panel, image, g2);
        }

        if (faces == null) {
            return;
        }

        Iterator<DetectedFace> dfi = faces.iterator();
        while (dfi.hasNext()) {

            DetectedFace face = dfi.next();
            Rectangle bounds = face.getBounds();

            int dx = (int) (0.1 * bounds.width);
            int dy = (int) (0.2 * bounds.height);
            int x = (int) bounds.x - dx;
            int y = (int) bounds.y - dy;
            int w = (int) bounds.width + 2 * dx;
            int h = (int) bounds.height + dy;

            g2.setStroke(STROKE);
            g2.setColor(Color.RED);
            g2.drawRect(x, y, w, h);
            g2.drawString(whoIsLabel.getText(), x, y - 10);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (!webcam.isOpen()) {
                return;
            }
            faces = detector.detectFaces(ImageUtilities.createFImage(webcam.getImage()));
        }
    }
}
