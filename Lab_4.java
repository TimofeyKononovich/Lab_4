import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.beans.Transient;

class GraphicsDisplay extends JPanel {
    private Double[][] graphicsData;
    // Флаговые переменные, задающие правила отображения графика
    private boolean showAxis = true;
    private boolean showMarkers = true;
    // Границы диапазона пространства, подлежащего отображению
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    // Используемый масштаб отображения
    private double scale;
    // Различные стили черчения линий
    private transient BasicStroke graphicsStroke;
    private transient BasicStroke axisStroke;
    private transient BasicStroke markerStroke;
    // Различные шрифты отображения надписей
    private Font axisFont;

    public GraphicsDisplay() {
        setBackground(Color.WHITE);
        // Сконструировать необходимые объекты, используемые в рисовании Перо для
        // рисования графика
        graphicsStroke = new BasicStroke(5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f,
                new float[] { 40, 10, 20, 10, 10, 10, 20, 10, 40 }, 0.0f);
        // Перо для рисования осей координат
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        // Перо для рисования контуров маркеров
        markerStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 90.0f, null, 0.0f);
        // Шрифт для подписей осей координат
        axisFont = new Font("Serif", Font.BOLD, 35);
    }

    // Данный метод вызывается из обработчика элемента меню "Открыть файл с
    // графиком"
    // главного окна приложения в случае успешной загрузки данных
    public void showGraphics(Double[][] graphicsData) {
        // Сохранить массив точек во внутреннем поле класса
        this.graphicsData = graphicsData;
        // Запросить перерисовку компонента, т.е. неявно вызвать paintComponent()
        repaint();
    }

    // Методы-модификаторы для изменения параметров отображения графика
    // Изменение любого параметра приводит к перерисовке области
    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    // Метод отображения всего компонента, содержащего график
    public void paintComponent(Graphics g) {
        // Шаг 1 - Вызвать метод предка для заливки области цветом заднего фона
        super.paintComponent(g);
        // Шаг 2 - Если данные графика не загружены - ничего не делать
        if (graphicsData == null || graphicsData.length == 0) {
            return;
        }
        // Шаг 3 - Определить минимальное и максимальное значения для координат X и Y\
        // Это необходимо для определения области пространства, подлежащей отображению
        minX = graphicsData[0][0];
        maxX = graphicsData[graphicsData.length - 1][0];
        minY = graphicsData[0][1];
        maxY = minY;
        // Найти минимальное и максимальное значение функции
        for (int i = 0; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < minY) {
                minY = graphicsData[i][1];
            }
            if (graphicsData[i][1] > maxY) {
                maxY = graphicsData[i][1];
            }
        }
        // Шаг 4 - Определить (исходя из размеров окна) масштабы по осям X и Y - сколько
        // пикселов
        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);
        // Шаг 5 - Чтобы изображение было неискажѐнным - масштаб должен быть одинаков
        // Выбираем за основу минимальный
        scale = Math.min(scaleX, scaleY);
        // Шаг 6 - корректировка границ отображаемой области согласно выбранному
        // масштабу
        if (scale == scaleX) {
            double yIncrement = (getSize().getHeight() / scale - (maxY - minY)) / 2;
            maxY += yIncrement;
            minY -= yIncrement;
        }
        if (scale == scaleY) {

            double xIncrement = (getSize().getWidth() / scale - (maxX - minX)) / 2;
            maxX += xIncrement;
            minX -= xIncrement;
        }
        // Шаг 7 - Сохранить текущие настройки холста
        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();
        // Шаг 8 - В нужном порядке вызвать методы отображения элементов графика
        // Порядок вызова методов имеет значение, т.к. предыдущий рисунок будет
        // затираться последующим
        // Первыми (если нужно) отрисовываются оси координат.
        if (showAxis) {
            paintAxis(canvas);
        }
        // Затем отображается сам график
        paintGraphics(canvas);
        // Затем (если нужно) отображаются маркеры точек, по которым строился график.
        if (showMarkers) {
            paintMarkers(canvas);
        }
        // Шаг 9 - Восстановить старые настройки холста
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    // Отрисовка графика по прочитанным координатам
    protected void paintGraphics(Graphics2D canvas) {
        // Выбрать линию для рисования графика
        canvas.setStroke(graphicsStroke);
        // Выбрать цвет линии
        canvas.setColor(Color.BLUE);
        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
            // Преобразовать значения (x,y) в точку на экране point
            Point2D.Double point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (i > 0) {
                // Не первая итерация цикла - вести линию в точку point
                graphics.lineTo(point.getX(), point.getY());
            } else {
                // Первая итерация цикла - установить начало пути в точку point
                graphics.moveTo(point.getX(), point.getY());
            }
        }
        // Отобразить график
        canvas.draw(graphics);
    }

    // Отображение маркеров точек, по которым рисовался график
    protected void paintMarkers(Graphics2D canvas) {
        // Шаг 1 - Установить специальное перо для черчения контуров маркеров
        canvas.setStroke(markerStroke);
        // Выбрать красный цвета для контуров маркеров
        canvas.setColor(Color.RED);
        // Шаг 2 - Организовать цикл по всем точкам графика
        for (int i = 0; i < graphicsData.length; i++) {
            // Выбрать зеленый цвет для закрашивания маркеров внутри
            canvas.setPaint(Color.GREEN);
            double x1 = graphicsData[i][1];
            int integer = (int) Math.abs(x1);
            int sum = 0;
            while (integer > 0) {
                sum += (integer % 10);
                integer /= 10;

            }
            if (sum < 10) {
                GeneralPath path = new GeneralPath();
                Point2D.Double center = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
                path.moveTo(center.x - 4, center.y + 3);
                path.lineTo(center.x + 4, center.y - 3);
                path.lineTo(center.x + 4, center.y + 4);
                path.lineTo(center.x - 3, center.y);
                canvas.draw(path);
            } else {
                // Выбрать красный цвет для закрашивания маркеров внутри
                canvas.setPaint(Color.RED);
                Ellipse2D.Double marker = new Ellipse2D.Double();
                Point2D.Double center = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
                Point2D.Double corner = shiftPoint(center, 3, 3);
                marker.setFrameFromCenter(center, corner);
                canvas.draw(marker); // Начертить контур маркера
                canvas.fill(marker);
            }
        }
    }

    // Метод, обеспечивающий отображение осей координат
    protected void paintAxis(Graphics2D canvas) {
        // Установить особое начертание для осей
        canvas.setStroke(axisStroke);
        // Оси рисуются чѐрным цветом
        canvas.setColor(Color.BLACK);
        // Стрелки заливаются чѐрным цветом
        canvas.setPaint(Color.BLACK);
        // Подписи к координатным осям делаются специальным шрифтом
        canvas.setFont(axisFont);
        // Создать объект контекста отображения текста - для получения характеристик
        // устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
        // Определить, должна ли быть видна ось Y на графике
        if (minX <= 0.0 && maxX >= 0.0) {

            canvas.draw(new Line2D.Double(xyToPoint(0, maxY), xyToPoint(0, minY)));
            // Стрелка оси Y
            GeneralPath arrow = new GeneralPath();
            // Установить начальную точку ломаной точно на верхний конец оси Y
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            // Вести левый "скат" стрелки в точку с относительными координатами (5,20)
            arrow.lineTo(arrow.getCurrentPoint().getX() + 5, arrow.getCurrentPoint().getY() + 20);
            // Вести нижнюю часть стрелки в точку с относительными координатами (-10, 0)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 10, arrow.getCurrentPoint().getY());
            // Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow);
            canvas.fill(arrow);
            // Нарисовать подпись к оси Y
            // Определить, сколько места понадобится для надписи "y"
            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
            // Вывести надпись в точке с вычисленными координатами
            canvas.drawString("y", (float) labelPos.getX() + 10, (float) (labelPos.getY() - bounds.getY()));
        }
        if (minY <= 0.0 && maxY >= 0.0) {

            canvas.draw(new Line2D.Double(xyToPoint(minX, 0), xyToPoint(maxX, 0)));

            GeneralPath arrow = new GeneralPath();

            Point2D.Double lineEnd = xyToPoint(maxX, 0);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());

            arrow.lineTo(arrow.getCurrentPoint().getX() - 20, arrow.getCurrentPoint().getY() - 5);

            arrow.lineTo(arrow.getCurrentPoint().getX(), arrow.getCurrentPoint().getY() + 10);

            arrow.closePath();
            canvas.draw(arrow);
            canvas.fill(arrow);

            Rectangle2D bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = xyToPoint(maxX, 0);

            canvas.drawString("x", (float) labelPos.getX() - 10, (float) (labelPos.getY() + bounds.getY()));
        }
    }

    /*
     * Метод-помощник, осуществляющий преобразование координат. Оно необходимо, т.к.
     * верхнему левому углу холста с координатами (0.0, 0.0) соответствует точка
     * графика с координатами (minX, maxY), где minX - это самое "левое" значение X,
     * а maxY - самое "верхнее" значение Y.
     */
    protected Point2D.Double xyToPoint(double x, double y) {
        // Вычисляем смещение X от самой левой точки (minX)
        double deltaX = x - minX;
        // Вычисляем смещение Y от точки верхней точки (maxY)
        double deltaY = maxY - y;
        return new Point2D.Double(deltaX * scale, deltaY * scale);
    }

    /*
     * Метод-помощник, возвращающий экземпляр класса Point2D.Double смещѐнный по
     * отношению к исходному на deltaX, deltaY К сожалению, стандартного метода,
     * выполняющего такую задачу, нет.
     */
    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
        // Инициализировать новый экземпляр точки
        Point2D.Double dest = new Point2D.Double();
        // Задать еѐ координаты как координаты существующей точки + заданные смещения
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }
}

class MainFrame extends JFrame {
    private static final int WIDTH = 1300;
    private static final int HEIGHT = 750;
    // Объект диалогового окна для выбора файлов
    private JFileChooser fileChooser = null;
    // Пункты меню
    private JCheckBoxMenuItem showAxisMenuItem;
    private JCheckBoxMenuItem showMarkersMenuItem;
    // Компонент-отображатель графика
    private GraphicsDisplay display = new GraphicsDisplay();
    // Флаг, указывающий на загруженность данных графика
    private boolean fileLoaded = false;

    public MainFrame() {
        super("Построение графика функции на основе заранее подготовленных файлов");
        // Установка размеров окна
        setSize(WIDTH, HEIGHT);
        Toolkit kit = Toolkit.getDefaultToolkit();
        // Отцентрировать окно приложения на экране
        setLocation((kit.getScreenSize().width - WIDTH) / 2, (kit.getScreenSize().height - HEIGHT) / 2);
        // Развѐртывание окна на весь экран
        setExtendedState(MAXIMIZED_BOTH);
        // Создать и установить полосу меню
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        // Добавить пункт меню "Файл"
        JMenu fileMenu = new JMenu("Файл");
        menuBar.add(fileMenu);
        // Создать действие по открытию файла
        Action openGraphicsAction = new AbstractAction("Открыть файл с графиком") {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (fileChooser == null) {
                    fileChooser = new JFileChooser();
                    fileChooser.setCurrentDirectory(new File("."));
                }
                if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    openGraphics(fileChooser.getSelectedFile());
                }
            }
        };
        // Добавить соответствующий элемент меню
        fileMenu.add(openGraphicsAction);
        // Создать пункт меню "График"
        JMenu graphicsMenu = new JMenu("График");
        menuBar.add(graphicsMenu);
        // Создать действие для реакции на активацию элемента "Показывать оси координат"
        Action showAxisAction = new AbstractAction("Показать оси координат") {
            @Override
            public void actionPerformed(ActionEvent event) {
                // свойство showAxis класса GraphicsDisplay истина, если элемент меню
                // showAxisMenuItem отмечен флажком, и ложь - в противном случае
                display.setShowAxis(showAxisMenuItem.isSelected());
            }
        };
        showAxisMenuItem = new JCheckBoxMenuItem(showAxisAction);
        // Добавить соответствующий элемент в меню
        graphicsMenu.add(showAxisMenuItem);
        // Элемент по умолчанию включен (отмечен флажком)
        showAxisMenuItem.setSelected(true);
        // Повторить действия для элемента "Показывать маркеры точек"
        Action showMarkersAction = new AbstractAction("Показать маркеры точек") {
            @Override
            public void actionPerformed(ActionEvent event) {
                display.setShowMarkers(showMarkersMenuItem.isSelected());
            }
        };
        showMarkersMenuItem = new JCheckBoxMenuItem(showMarkersAction);
        graphicsMenu.add(showMarkersMenuItem);
        // Элемент по умолчанию включен (отмечен флажком)
        showMarkersMenuItem.setSelected(true);
        // Зарегистрировать обработчик событий, связанных с меню "График"
        graphicsMenu.addMenuListener(new GraphicsMenuListener());
        // Установить GraphicsDisplay в цент граничной компоновки
        getContentPane().add(display, BorderLayout.CENTER);
    }

    // Считывание данных графика из существующего файла
    protected void openGraphics(File selectedFile) {
        try (DataInputStream in = new DataInputStream(new FileInputStream(selectedFile));) {
            // Шаг 1 - Открыть поток чтения данных, связанный с входным файловым потоком

            /*
             * Шаг 2 - Зная объѐм данных в потоке ввода можно вычислить, сколько памяти
             * нужно зарезервировать в массиве: Всего байт в потоке - in.available() байт;
             * Размер одного числа Double - Double.SIZE бит, или Double.SIZE/8 байт; Так как
             * числа записываются парами, то число пар меньше в 2 раза
             */
            Double[][] graphicsData = new Double[in.available() / (Double.SIZE / 8) / 2][];
            // Шаг 3 - Цикл чтения данных (пока в потоке есть данные)
            int i = 0;
            while (in.available() > 0) {
                // Первой из потока читается координата точки X
                Double x = in.readDouble();
                // Затем - значение графика Y в точке X
                Double y = in.readDouble();
                // Прочитанная пара координат добавляется в массив
                graphicsData[i++] = new Double[] { x, y };
            }
            // Шаг 4 - Проверка, имеется ли в списке в результате чтения хотя бы одна пара
            // координат
            if (graphicsData != null && graphicsData.length > 0) {
                // Да - установить флаг загруженности данных
                fileLoaded = true;
                // Вызывать метод отображения графика
                display.showGraphics(graphicsData);
            }
            // Шаг 5 - Закрыть входной поток
            in.close();
        } catch (FileNotFoundException ex) {
            // В случае исключительной ситуации типа "Файл не найден" показать сообщение об
            // ошибке
            JOptionPane.showMessageDialog(MainFrame.this, "Указанный файл не найден", "Ошибка загрузки данных",
                    JOptionPane.WARNING_MESSAGE);
            return;
        } catch (IOException ex) {
            // В случае ошибки ввода из файлового потока показать сообщение об ошибке
            JOptionPane.showMessageDialog(MainFrame.this, "Ошибка чтения координат точек из файла",
                    "Ошибка загрузки данных", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    // Класс-слушатель событий, связанных с отображением меню
    private class GraphicsMenuListener implements MenuListener {
        // Обработчик, вызываемый перед показом меню
        public void menuSelected(MenuEvent e) {
            // Доступность или недоступность элементов меню "График" определяется
            // загруженностью данных
            showAxisMenuItem.setEnabled(fileLoaded);
            showMarkersMenuItem.setEnabled(fileLoaded);
        }

        // Обработчик, вызываемый после того, как меню исчезло с экрана
        @Override
        public void menuDeselected(MenuEvent e) {
            System.out.println("Close");
        }

        // Обработчик, вызываемый в случае отмены выбора пункта меню (очень редкая
        // ситуация)
        @Override
        public void menuCanceled(MenuEvent e) {
            System.out.println("Close");
        }
    }
}

class Lab_4 {
    public static void main(String[] args) {
        // Создать и показать экземпляр главного окна приложения
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);
    }
}
