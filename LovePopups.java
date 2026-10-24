import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LovePopups {

    // ===================== 可自定义配置区 =====================

    /** 弹窗数量 */
    private static final int POPUP_COUNT = 50;

    /** 弹窗生成间隔（毫秒） */
    private static final int SPAWN_INTERVAL_MS = 400;

    /**
     * 心形缩放比例（越大心形越大）
     * <pre>
     *   心形原始范围（参数方程）：
     *     x ∈ [-16, +16]，总宽 32
     *     y ∈ [-17, +5]，总高 22
     *   实际像素大小 ≈ 32 × scale × 22 × scale
     *   scale=15 → 心形宽约 480px、高约 330px，适配大多数 800+ 宽度屏幕
     * </pre>
     */
    private static final int HEART_SCALE = 15;

    /** 心形动画总时长（毫秒） */
    private static final int ANIMATION_DURATION_MS = 2000;

    /** 动画帧率（fps） */
    private static final int ANIMATION_FPS = 60;

    /** 单个弹窗尺寸（宽 × 高） */
    private static final Dimension POPUP_SIZE = new Dimension(200, 80);

    /** 13 种柔和背景色 */
    private static final Color[] COLORS = {
            new Color(255, 204, 229), // 樱花粉
            new Color(255, 228, 196), // 奶油杏
            new Color(255, 250, 205), // 柠檬奶黄
            new Color(201, 255, 229), // 薄荷绿
            new Color(189, 252, 201), // 嫩芽绿
            new Color(173, 216, 255), // 天蓝
            new Color(216, 191, 216), // 淡紫
            new Color(255, 218, 185), // 桃色
            new Color(255, 240, 245), // 淡玫粉
            new Color(230, 230, 250), // 薰衣草
            new Color(255, 222, 173), // 浅珊瑚
            new Color(176, 224, 230), // powder blue
            new Color(255, 239, 213)  // 玉米黄
    };

    /** 15 条暖心文案 */
    private static final String[] MESSAGES = {
            "愿你每天都开心",
            "你是最棒的！",
            "加油，未来可期",
            "今天也要元气满满",
            "被世界温柔以待",
            "你笑起来真好看",
            "记得照顾好自己",
            "晚安，好梦",
            "愿你被爱包围",
            "做自己就很好",
            "你值得一切美好",
            "生活明朗，万物可爱",
            "心想事成，万事如意",
            "每一天都是新的开始",
            "你是独一无二的"
    };

    // ===================== 核心代码区 =====================

    private static final Random RANDOM = new Random();

    /** 所有弹窗窗口的引用 */
    private final List<JWindow> popupWindows = new ArrayList<>();

    /** 动画起始位置（动画开始瞬间各弹窗的实际屏幕坐标快照） */
    private final List<Point> startPositions = new ArrayList<>();

    /** 每个弹窗的最终目标位置（心形坐标） */
    private final List<Point> targetPositions = new ArrayList<>();

    /** 主窗口（初始问候窗口） */
    private JFrame mainFrame;

    /** 弹窗生成定时器 */
    private Timer spawnTimer;

    /** 当前已生成的弹窗个数 */
    private int spawnedCount = 0;

    /**
     * 心形参数方程（标准经典心形）
     * <pre>
     *   x(t) = 16 · sin³(t)
     *   y(t) = 13 · cos(t) − 5 · cos(2t) − 2 · cos(3t) − cos(4t)
     * </pre>
     * 数学坐标系 y 向上为正，屏幕坐标系 y 向下为正，
     * 因此返回时对 y 做翻转：center.y − scale·y。
     *
     * @param t      参数，范围 [0, 2π]
     * @param center 心形中心（屏幕坐标）
     * @param scale  缩放比例
     * @return 心形上对应点的屏幕坐标
     */
    private static Point heartPoint(double t, Point center, int scale) {
        double x = 16 * Math.pow(Math.sin(t), 3);
        double y = 13 * Math.cos(t)
                - 5 * Math.cos(2 * t)
                - 2 * Math.cos(3 * t)
                - Math.cos(4 * t);
        int px = (int) Math.round(center.x + scale * x);
        int py = (int) Math.round(center.y - scale * y);
        return new Point(px, py);
    }

    /**
     * easeOutCubic 缓动函数 —— 先快后慢，适合"汇聚到目标"的视觉效果
     * <pre>
     *   f(t) = 1 − (1 − t)³
     *   t ∈ [0, 1] → 输出 ∈ [0, 1]
     * </pre>
     */
    private static double easeOutCubic(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    /** 线性插值 */
    private static Point lerp(Point a, Point b, double t) {
        int x = (int) Math.round(a.x + (b.x - a.x) * t);
        int y = (int) Math.round(a.y + (b.y - a.y) * t);
        return new Point(x, y);
    }

    // ===================== 入口 =====================

    public static void main(String[] args) {
        // ========== 修复 IDEA / Windows 下中文乱码 ==========
        // 在创建任何 Swing 组件之前，把全局默认字体强制设为支持中文的字体。
        // Windows 上优先 "微软雅黑"，其次 "SimSun(宋体)"、"Dialog"、"SansSerif"。
        String[] fontCandidates = {"微软雅黑", "Microsoft YaHei", "SimSun", "Dialog", "SansSerif"};
        Font chineseFont = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] installedFonts = ge.getAllFonts();
        for (String name : fontCandidates) {
            for (Font f : installedFonts) {
                if (f.getFontName().equalsIgnoreCase(name) || f.getFamily().equalsIgnoreCase(name)) {
                    chineseFont = f.deriveFont(Font.PLAIN, 14f);
                    break;
                }
            }
            if (chineseFont != null) break;
        }
        if (chineseFont == null) {
            chineseFont = new Font("SansSerif", Font.PLAIN, 14); // 兜底
        }
        // 覆盖所有 Swing 组件的默认字体
        UIManager.put("OptionPane.messageFont", chineseFont);
        UIManager.put("OptionPane.buttonFont", chineseFont);
        UIManager.put("OptionPane.titleTextFont", chineseFont);
        UIManager.put("OptionPane.inputDialogTitleFont", chineseFont);
        UIManager.put("OptionPane.questionDialogTitleFont", chineseFont);
        UIManager.put("OptionPane.warningDialogTitleFont", chineseFont);
        UIManager.put("OptionPane.errorDialogTitleFont", chineseFont);
        UIManager.put("OptionPane.informationDialogTitleFont", chineseFont);
        UIManager.put("Label.font", chineseFont);
        UIManager.put("Button.font", chineseFont);
        UIManager.put("Panel.font", chineseFont);
        UIManager.put("Window.font", chineseFont);
        UIManager.put("Frame.font", chineseFont);
        UIManager.put("OptionPane.font", chineseFont);
        // =====================================================

        SwingUtilities.invokeLater(() -> new LovePopups().initMainFrame());
    }

    // ===================== 初始窗口 =====================

    private void initMainFrame() {
        mainFrame = new JFrame("❤ 爱心弹窗");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(360, 200);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setLayout(new BorderLayout());

        JLabel label = new JLabel("你今天快乐吗！", SwingConstants.CENTER);
        label.setFont(new Font("微软雅黑", Font.BOLD, 24));
        label.setForeground(new Color(220, 20, 60));
        mainFrame.add(label, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));

        JButton happyBtn = new JButton("快乐 😊");
        happyBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        happyBtn.setBackground(new Color(255, 182, 193));
        happyBtn.setOpaque(true);
        happyBtn.setBorderPainted(false);
        happyBtn.setPreferredSize(new Dimension(120, 40));
        happyBtn.addActionListener(e -> startPopupGeneration());

        JButton sadBtn = new JButton("不快乐 😢");
        sadBtn.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        sadBtn.setBackground(new Color(173, 216, 230));
        sadBtn.setOpaque(true);
        sadBtn.setBorderPainted(false);
        sadBtn.setPreferredSize(new Dimension(120, 40));
        sadBtn.addActionListener(e -> {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            int maxX = screen.width - mainFrame.getWidth();
            int maxY = screen.height - mainFrame.getHeight();
            int nx = RANDOM.nextInt(Math.max(1, maxX));
            int ny = RANDOM.nextInt(Math.max(1, maxY));
            mainFrame.setLocation(nx, ny);

            JOptionPane.showMessageDialog(
                    mainFrame,
                    "不准选这个！请点另一个按钮~ ❤",
                    "嘿嘿",
                    JOptionPane.WARNING_MESSAGE
            );
        });

        btnPanel.add(happyBtn);
        btnPanel.add(sadBtn);
        mainFrame.add(btnPanel, BorderLayout.SOUTH);
        mainFrame.setVisible(true);
    }

    // ===================== 弹窗生成阶段 =====================

    /** 点击"快乐"后启动：每隔 SPAWN_INTERVAL_MS 生成一个随机弹窗，共 POPUP_COUNT 个 */
    private void startPopupGeneration() {
        mainFrame.setVisible(false);

        // 根据屏幕尺寸动态计算心形中心，确保不同分辨率下都居中
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Point heartCenter = new Point(screen.width / 2, screen.height / 2);

        spawnedCount = 0;
        popupWindows.clear();
        startPositions.clear();
        targetPositions.clear();

        // 预计算 POPUP_COUNT 个心形目标位置（t 从 0 到 2π 均匀分布）
        for (int i = 0; i < POPUP_COUNT; i++) {
            double t = 2 * Math.PI * i / POPUP_COUNT;
            targetPositions.add(heartPoint(t, heartCenter, HEART_SCALE));
        }

        spawnTimer = new Timer(SPAWN_INTERVAL_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createRandomPopup();
                spawnedCount++;
                if (spawnedCount >= POPUP_COUNT) {
                    spawnTimer.stop();
                    startHeartAnimation();
                }
            }
        });
        spawnTimer.start();
    }

    /** 创建一个随机位置、随机颜色、随机文案的无标题弹窗 */
    private void createRandomPopup() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxX = Math.max(1, screen.width - POPUP_SIZE.width);
        int maxY = Math.max(1, screen.height - POPUP_SIZE.height);
        int rx = RANDOM.nextInt(maxX);
        int ry = RANDOM.nextInt(maxY);

        Color bg = COLORS[RANDOM.nextInt(COLORS.length)];
        String msg = MESSAGES[RANDOM.nextInt(MESSAGES.length)];

        JWindow popup = new JWindow();
        popup.setSize(POPUP_SIZE);
        popup.setLocation(rx, ry);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel text = new JLabel(msg, SwingConstants.CENTER);
        text.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        text.setForeground(new Color(80, 20, 40));
        panel.add(text, BorderLayout.CENTER);

        popup.setContentPane(panel);
        popup.setVisible(true);

        popupWindows.add(popup);
    }

    // ===================== 心形动画阶段 =====================

    /**
     * 所有弹窗生成完毕后启动：
     *   1) 瞬间快照每个弹窗的当前位置作为起始点
     *   2) 以 ANIMATION_DURATION_MS 为总时长，推进全局进度 progress ∈ [0,1]
     *   3) 每帧用 easeOutCubic 缓动后对每个弹窗做 lerp：startPos → targetPos
     *   4) progress ≥ 1 时精确到位并停止动画
     */
    private void startHeartAnimation() {
        // 1. 快照起始位置（此时弹窗还停在各自的随机初始位置）
        startPositions.clear();
        for (JWindow win : popupWindows) {
            Point p = win.getLocation();
            startPositions.add(new Point(p.x, p.y));
        }

        // 2. 基于时间戳驱动进度，不受帧率波动影响
        long startTime = System.currentTimeMillis();
        int frameDelay = 1000 / ANIMATION_FPS;

        Timer animTimer = new Timer(frameDelay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;
                double rawProgress = Math.min(1.0, (double) elapsed / ANIMATION_DURATION_MS);
                double eased = easeOutCubic(rawProgress);

                // 更新所有弹窗位置
                for (int i = 0; i < popupWindows.size(); i++) {
                    Point start = startPositions.get(i);
                    Point target = targetPositions.get(i);
                    Point next = lerp(start, target, eased);
                    popupWindows.get(i).setLocation(next);
                }

                if (rawProgress >= 1.0) {
                    // 精确到位，防止浮点残留
                    for (int i = 0; i < popupWindows.size(); i++) {
                        popupWindows.get(i).setLocation(targetPositions.get(i));
                    }
                    ((Timer) e.getSource()).stop();
                    scheduleExit();
                }
            }
        });
        animTimer.start();
    }

    /** 心形动画完成后，延迟 5 秒自动 dispose 所有窗口并退出 JVM */
    private void scheduleExit() {
        new Timer(5000, e -> {
            for (JWindow win : popupWindows) {
                win.dispose();
            }
            popupWindows.clear();
            startPositions.clear();
            targetPositions.clear();
            if (mainFrame != null) {
                mainFrame.dispose();
            }
            System.exit(0);
        }).start();
    }
}
