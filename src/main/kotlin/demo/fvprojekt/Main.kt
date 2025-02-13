@file:Suppress("SameParameterValue", "MemberVisibilityCanBePrivate")

package demo.fvprojekt.demo.fvprojekt

import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import net.objecthunter.exp4j.operator.Operator
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder
import kotlin.math.*



data class GraphSettings(
    var backgroundColor: Color = Color(30, 30, 30),
    var gridColor: Color = Color(60, 60, 60),
    var axisColor: Color = Color.WHITE,
    var showGrid: Boolean = true,
    var showAxis: Boolean = true,
    var showDomainArea: Boolean = true,
    var showZeroPoints: Boolean = true,
    var showIntersections: Boolean = true,
    var pointSize: Int = 8,
    var domainAlpha: Float = 0.1f,
    var stepForNumericalSearch: Double = 0.01,
    var invertColors: Boolean = false,
    var leftPanelWidth: Int = 380,
    var defaultFunctionFormat: String = "f(x)=" // Új beállítás: alap hozzárendelési szabály
)


data class FunctionData(
    var expressionText: String = "x",
    var domainStart: Double = -10.0,
    var domainEnd: Double = 10.0,
    var lineColor: Color = Color.ORANGE,
    var lineStroke: Float = 2f,
    var visible: Boolean = true,
    var showDomain: Boolean = true,
    var expression: Expression? = null
)


data class WorldPoint(val x: Double, val y: Double)


data class IntegrationRegion(val expr: Expression, val a: Double, val b: Double, val color: Color)




class EquationEditorDialog(owner: Component, initialText: String) : JDialog(SwingUtilities.getWindowAncestor(owner), "Egyenlet szerkesztő", ModalityType.APPLICATION_MODAL) {
    private val txtEquation = JTextField(initialText, 25)
    var result: String? = null
        private set

    init {
        layout = BorderLayout(5, 5)
        val insertPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val buttons = listOf("sin()", "cos()", "tan()", "log()", "sqrt()", "/")
        buttons.forEach { label ->
            val btn = JButton(label)
            btn.addActionListener {
                val pos = txtEquation.caretPosition
                txtEquation.text = txtEquation.text.substring(0, pos) + label + txtEquation.text.substring(pos)
                txtEquation.requestFocusInWindow()
                txtEquation.caretPosition = pos + label.length
            }
            insertPanel.add(btn)
        }

        val btnOk = JButton("OK")
        val btnCancel = JButton("Mégse")
        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        btnPanel.add(btnOk)
        btnPanel.add(btnCancel)

        btnOk.addActionListener {
            result = txtEquation.text
            dispose()
        }
        btnCancel.addActionListener { dispose() }

        add(txtEquation, BorderLayout.NORTH)
        add(insertPanel, BorderLayout.CENTER)
        add(btnPanel, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(owner)
    }
}

/*
   Számológép dialógus
   */


class CalculatorDialog(owner: Frame?) : JDialog(owner, "Számológép", false) {
    private val display = JTextField()

    init {
        layout = BorderLayout(5, 5)
        display.font = Font("SansSerif", Font.BOLD, 20)
        display.isEditable = false
        display.horizontalAlignment = JTextField.RIGHT
        add(display, BorderLayout.NORTH)

        val panel = JPanel(GridLayout(6, 4, 5, 5))
        val buttons = arrayOf(
            "sin", "cos", "tan", "log",
            "(", ")", "C", "←",
            "7", "8", "9", "/",
            "4", "5", "6", "*",
            "1", "2", "3", "-",
            "0", ".", "=", "+"
        )
        buttons.forEach { text ->
            val btn = JButton(text)
            btn.font = Font("SansSerif", Font.BOLD, 18)
            btn.addActionListener {
                when (text) {
                    "=" -> {
                        try {
                            val expr = ExpressionBuilder(display.text)
                                .build()
                            val result = expr.evaluate()
                            display.text = result.toString()
                        } catch (ex: Exception) {
                            display.text = "Error"
                        }
                    }
                    "C" -> display.text = ""
                    "←" -> {
                        if (display.text.isNotEmpty()) {
                            display.text = display.text.substring(0, display.text.length - 1)
                        }
                    }
                    "sin", "cos", "tan", "log" -> display.text += "$text("
                    else -> display.text += text
                }
            }
            panel.add(btn)
        }
        add(panel, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(owner)
    }
}

/*
   Függvény bevitel panel
 */


class FunctionInputPanel(
    val onRemove: (panel: FunctionInputPanel) -> Unit
) : JPanel() {

    val txtExpression = JTextField("x", 18)
    val txtDomainStart = JTextField("-10", 5)
    val txtDomainEnd = JTextField("10", 5)
    val btnColor = JButton("Szín")
    val spnLineWidth = JSpinner(SpinnerNumberModel(2.0, 0.5, 20.0, 0.5))
    val chkVisible = JCheckBox("Látható", true)
    val chkDomain = JCheckBox("Domain", true)
    val btnRemove = JButton("✕")

    val btnEquationEditor = JButton("Szerk")

    init {
        layout = GridBagLayout()
        background = Color(45, 45, 45)
        val gbc = GridBagConstraints().apply {
            insets = Insets(3, 3, 3, 3)
            fill = GridBagConstraints.HORIZONTAL
        }

        // 1. Sor: Expression + egyenlet szerkesztő gomb
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1
        add(JLabel("f(x) =").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 4
        txtExpression.background = Color(60, 60, 60)
        txtExpression.foreground = Color.WHITE
        add(txtExpression, gbc)
        gbc.gridx = 5; gbc.gridy = 0; gbc.gridwidth = 1
        btnEquationEditor.background = Color(80, 80, 80)
        btnEquationEditor.foreground = Color.WHITE
        btnEquationEditor.toolTipText = "Egyenlet szerkesztése"
        add(btnEquationEditor, gbc)

        btnEquationEditor.addActionListener {
            val dlg = EquationEditorDialog(this, txtExpression.text)
            dlg.isVisible = true
            dlg.result?.let { newExpr ->
                txtExpression.text = newExpr
            }
        }

        // 2. Sor: Domain
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1
        add(JLabel("Domain: [").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1
        txtDomainStart.background = Color(60, 60, 60)
        txtDomainStart.foreground = Color.WHITE
        add(txtDomainStart, gbc)
        gbc.gridx = 2
        add(JLabel(";").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 3
        txtDomainEnd.background = Color(60, 60, 60)
        txtDomainEnd.foreground = Color.WHITE
        add(txtDomainEnd, gbc)
        gbc.gridx = 4
        add(JLabel("]").apply { foreground = Color.WHITE }, gbc)

        // 3. Sor: Szín és vonalvastagság
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1
        btnColor.background = Color.ORANGE
        btnColor.foreground = Color.BLACK
        add(btnColor, gbc)
        gbc.gridx = 1
        add(JLabel("Vonal:" ).apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 2
        spnLineWidth.preferredSize = Dimension(60, 22)
        add(spnLineWidth, gbc)

        // 4. Sor: CheckBox-ok és törlés gomb
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1
        chkVisible.background = background
        chkVisible.foreground = Color.WHITE
        add(chkVisible, gbc)
        gbc.gridx = 1
        chkDomain.background = background
        chkDomain.foreground = Color.WHITE
        add(chkDomain, gbc)
        gbc.gridx = 4
        btnRemove.background = Color(80, 50, 50)
        btnRemove.foreground = Color.WHITE
        btnRemove.toolTipText = "Függvény törlése"
        add(btnRemove, gbc)


        btnColor.addActionListener {
            val newCol = JColorChooser.showDialog(this, "Válassz színt", btnColor.background)
            if (newCol != null) btnColor.background = newCol
        }

        btnRemove.addActionListener { onRemove(this) }
    }

    fun toFunctionData(): FunctionData {
        var expr = txtExpression.text.trim().lowercase().replace(" ", "")
        if (expr.startsWith("f(x)=")) expr = expr.substring(5)
        val domainS = txtDomainStart.text.toDoubleOrNull() ?: -10.0
        val domainE = txtDomainEnd.text.toDoubleOrNull() ?: 10.0
        val lw = (spnLineWidth.value as? Double)?.toFloat() ?: 2f
        return FunctionData(
            expressionText = expr,
            domainStart = min(domainS, domainE),
            domainEnd = max(domainS, domainE),
            lineColor = btnColor.background,
            lineStroke = lw,
            visible = chkVisible.isSelected,
            showDomain = chkDomain.isSelected
        )
    }
}


class GraphPanel : JPanel() {
    var functionList: List<FunctionData> = emptyList()
    var graphSettings: GraphSettings = GraphSettings()
    var zeroPointsMap: Map<FunctionData, List<Double>> = emptyMap()
    var intersectionPoints: List<Pair<Double, Double>> = emptyList()
    var scale = 40.0


    var offsetX = 0
    var offsetY = 0
    private var lastDragX = 0
    private var lastDragY = 0


    var polygonMode: Boolean = false
    val polygonPoints: MutableList<WorldPoint> = mutableListOf()


    var integrationRegion: IntegrationRegion? = null


    var tangentLine: FunctionData? = null

    init {

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (!polygonMode) {
                    lastDragX = e.x
                    lastDragY = e.y
                }
            }
            override fun mouseClicked(e: MouseEvent) {
                if (polygonMode) {
                    val centerX = width / 2 + offsetX
                    val centerY = height / 2 + offsetY
                    val worldX = (e.x - centerX) / scale.toDouble()
                    val worldY = (centerY - e.y) / scale.toDouble()
                    polygonPoints.add(WorldPoint(worldX, worldY))
                    repaint()
                    if (e.clickCount >= 2 && polygonPoints.size >= 3) {
                        val area = computePolygonArea(polygonPoints)
                        val perimeter = computePolygonPerimeter(polygonPoints)
                        JOptionPane.showMessageDialog(this@GraphPanel,
                            "Poligon területe: %.4f\nPoligon kerülete: %.4f".format(area, perimeter),
                            "Poligon adatok", JOptionPane.INFORMATION_MESSAGE)
                        polygonMode = false
                        polygonPoints.clear()
                        repaint()
                    }
                }
            }
        })
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (!polygonMode) {
                    val dx = e.x - lastDragX
                    val dy = e.y - lastDragY
                    offsetX += dx
                    offsetY += dy
                    lastDragX = e.x
                    lastDragY = e.y
                    repaint()
                }
            }
        })
        addMouseWheelListener { e: MouseWheelEvent ->
            if (e.wheelRotation < 0) zoomIn() else zoomOut()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = graphSettings.backgroundColor
        g2.fillRect(0, 0, width, height)

        val centerX = width / 2 + offsetX
        val centerY = height / 2 + offsetY

        if (graphSettings.showGrid) {
            g2.color = graphSettings.gridColor
            val minGridSpacingPx = 30.0
            val rawStep = minGridSpacingPx / scale
            val worldStep = getNiceGridSpacing(rawStep)
            val leftWorld = -centerX / scale.toDouble()
            val rightWorld = (width - centerX) / scale.toDouble()
            var xGrid = floor(leftWorld / worldStep) * worldStep
            while (xGrid <= rightWorld) {
                val xPixel = centerX + (xGrid * scale).toInt()
                g2.drawLine(xPixel, 0, xPixel, height)
                xGrid += worldStep
            }
            val bottomWorld = -((height - centerY).toDouble() / scale)
            val topWorld = centerY.toDouble() / scale
            var yGrid = floor(bottomWorld / worldStep) * worldStep
            while (yGrid <= topWorld) {
                val yPixel = centerY - (yGrid * scale).toInt()
                g2.drawLine(0, yPixel, width, yPixel)
                yGrid += worldStep
            }
        }

        if (graphSettings.showAxis) {
            g2.color = graphSettings.axisColor
            g2.drawLine(0, centerY, width, centerY)
            g2.drawLine(centerX, 0, centerX, height)
        }

        if (graphSettings.showAxis) {
            g2.color = graphSettings.axisColor
            g2.font = Font("SansSerif", Font.PLAIN, 10)
            val fm = g2.fontMetrics
            val tickLength = 5
            val minTickSpacingPx = 30.0
            val rawStep = minTickSpacingPx / scale
            val gridWorldStep = getNiceGridSpacing(rawStep)

            var lastLabelX: Int? = null
            var xTick = floor(-centerX / scale.toDouble() / gridWorldStep) * gridWorldStep
            while (xTick <= (width - centerX) / scale.toDouble()) {
                val px = centerX + (xTick * scale).toInt()
                g2.drawLine(px, centerY - tickLength, px, centerY + tickLength)
                if (lastLabelX == null || abs(px - lastLabelX) > 30) {
                    val label = "%.2f".format(xTick)
                    val labelWidth = fm.stringWidth(label)
                    var labelY = centerY + tickLength + fm.height
                    if (centerY + tickLength + fm.height > height) {
                        labelY = centerY - tickLength
                    }
                    g2.drawString(label, px - labelWidth / 2, labelY)
                    lastLabelX = px
                }
                xTick += gridWorldStep
            }
            var lastLabelY: Int? = null
            var yTick = floor(-((height - centerY) / scale.toDouble()) / gridWorldStep) * gridWorldStep
            while (yTick <= centerY / scale.toDouble()) {
                val py = centerY - (yTick * scale).toInt()
                g2.drawLine(centerX - tickLength, py, centerX + tickLength, py)
                if (lastLabelY == null || abs(py - lastLabelY) > 15) {
                    val label = "%.2f".format(yTick)
                    val labelWidth = fm.stringWidth(label)
                    val labelX = centerX - tickLength - labelWidth - 2
                    val labelY = py + fm.height / 2 - 3
                    g2.drawString(label, labelX, labelY)
                    lastLabelY = py
                }
                yTick += gridWorldStep
            }
            g2.drawString("x", width - 15, centerY - 5)
            g2.drawString("y", centerX + 5, 15)
        }

        if (graphSettings.showDomainArea) {
            functionList.forEach { fd ->
                if (fd.visible && fd.showDomain) {
                    val x1 = centerX + (fd.domainStart * scale).toInt()
                    val x2 = centerX + (fd.domainEnd * scale).toInt()
                    val rectX = min(x1, x2)
                    val rectW = abs(x2 - x1)
                    val c = fd.lineColor
                    val domainColor = Color(c.red, c.green, c.blue, (graphSettings.domainAlpha * 255).toInt())
                    g2.color = domainColor
                    g2.fillRect(rectX, 0, rectW, height)
                    g2.color = c
                    g2.stroke = BasicStroke(1f)
                    g2.drawLine(x1, 0, x1, height)
                    g2.drawLine(x2, 0, x2, height)
                }
            }
        }


        integrationRegion?.let { region ->
            val a = region.a
            val b = region.b
            val expr = region.expr
            val steps = 500
            val dx = (b - a) / steps
            val polyPoints = mutableListOf<Point>()
            polyPoints.add(Point(centerX + (a * scale).toInt(), centerY))
            var xVal = a
            for (i in 0..steps) {
                val yVal = expr.setVariable("x", xVal).evaluate()
                val px = centerX + (xVal * scale).toInt()
                val py = centerY - (yVal * scale).toInt()
                polyPoints.add(Point(px, py))
                xVal += dx
            }
            polyPoints.add(Point(centerX + (b * scale).toInt(), centerY))
            val xPoints = polyPoints.map { it.x }.toIntArray()
            val yPoints = polyPoints.map { it.y }.toIntArray()
            val poly = Polygon(xPoints, yPoints, polyPoints.size)
            val fillColor = Color(region.color.red, region.color.green, region.color.blue, (graphSettings.domainAlpha * 255).toInt())
            g2.color = fillColor
            g2.fillPolygon(poly)
        }


        functionList.forEach { fd ->
            if (!fd.visible) return@forEach
            val expr = fd.expression ?: return@forEach
            drawFunction(g2, expr, fd.domainStart, fd.domainEnd, fd.lineColor, fd.lineStroke, centerX, centerY)
        }


        if (graphSettings.showZeroPoints) {
            g2.color = Color.MAGENTA
            zeroPointsMap.forEach { (fd, zlist) ->
                if (!fd.visible) return@forEach
                for (z in zlist) {
                    val px = (centerX + z * scale).toInt()
                    val py = centerY
                    g2.fillOval(
                        px - graphSettings.pointSize / 2,
                        py - graphSettings.pointSize / 2,
                        graphSettings.pointSize,
                        graphSettings.pointSize
                    )
                }
            }
        }


        if (graphSettings.showIntersections) {
            g2.color = Color.YELLOW
            intersectionPoints.forEach { (mx, my) ->
                val px = centerX + (mx * scale).toInt()
                val py = centerY - (my * scale).toInt()
                g2.fillOval(
                    px - graphSettings.pointSize / 2,
                    py - graphSettings.pointSize / 2,
                    graphSettings.pointSize,
                    graphSettings.pointSize
                )
            }
        }


        tangentLine?.let { tl ->
            val expr = tl.expression ?: return@let
            val origStroke = g2.stroke
            g2.stroke = BasicStroke(tl.lineStroke, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(10f, 10f), 0f)
            drawFunction(g2, expr, tl.domainStart, tl.domainEnd, tl.lineColor, tl.lineStroke, centerX, centerY)
            g2.stroke = origStroke
        }


        if (polygonMode && polygonPoints.isNotEmpty()) {
            g2.color = Color.CYAN
            val poly = polygonPoints.map { wp ->
                Point(centerX + (wp.x * scale).toInt(), centerY - (wp.y * scale).toInt())
            }
            poly.forEach { p ->
                g2.fillOval(p.x - 4, p.y - 4, 8, 8)
            }
            for (i in 0 until poly.size - 1) {
                val p1 = poly[i]
                val p2 = poly[i + 1]
                g2.drawLine(p1.x, p1.y, p2.x, p2.y)
                val mx = (p1.x + p2.x) / 2
                val my = (p1.y + p2.y) / 2
                val d = hypot(polygonPoints[i + 1].x - polygonPoints[i].x, polygonPoints[i + 1].y - polygonPoints[i].y)
                val sideLetter = ('a'.toInt() + i).toChar()
                val label = "$sideLetter: %.2f".format(d)
                g2.color = Color.WHITE
                g2.drawString(label, mx, my)
                g2.color = Color.CYAN
            }
        }
    }


    private fun drawFunction(
        g2: Graphics2D,
        expr: Expression,
        start: Double,
        end: Double,
        col: Color,
        strokeWidth: Float,
        centerX: Int,
        centerY: Int
    ) {
        val steps = 500
        val dx = (end - start) / steps
        val poly = mutableListOf<Point>()
        var xVal = start
        for (i in 0..steps) {
            val yVal = expr.setVariable("x", xVal).evaluate()
            val px = centerX + (xVal * scale).toInt()
            val py = centerY - (yVal * scale).toInt()
            poly.add(Point(px, py))
            xVal += dx
        }
        g2.color = col
        g2.stroke = BasicStroke(strokeWidth)
        for (i in 0 until poly.size - 1) {
            val p1 = poly[i]
            val p2 = poly[i + 1]
            g2.drawLine(p1.x, p1.y, p2.x, p2.y)
        }
    }

    // Segédfüggvény a "szép" rács lépés meghatározásához
    private fun getNiceGridSpacing(rawStep: Double): Double {
        val exponent = floor(log10(rawStep))
        val fraction = rawStep / 10.0.pow(exponent)
        val niceFraction = when {
            fraction < 1.5 -> 1.0
            fraction < 3.0 -> 2.0
            fraction < 7.0 -> 5.0
            else -> 10.0
        }
        return niceFraction * 10.0.pow(exponent)
    }

    fun zoomIn() {
        scale *= 1.2
        repaint()
    }

    fun zoomOut() {
        scale /= 1.2
        repaint()
    }

    fun resetView() {
        scale = 40.0
        offsetX = 0
        offsetY = 0
        repaint()
    }

    fun zoomToFit() {
        if (functionList.isEmpty()) return

        var globalMinX = Double.MAX_VALUE
        var globalMaxX = -Double.MAX_VALUE
        var globalMinY = Double.MAX_VALUE
        var globalMaxY = -Double.MAX_VALUE

        for (fd in functionList) {
            val expr = fd.expression ?: continue
            val steps = 1000
            val dx = (fd.domainEnd - fd.domainStart) / steps
            var x = fd.domainStart
            while (x <= fd.domainEnd) {
                val y = expr.setVariable("x", x).evaluate()
                if (y.isFinite()) {
                    globalMinX = min(globalMinX, x)
                    globalMaxX = max(globalMaxX, x)
                    globalMinY = min(globalMinY, y)
                    globalMaxY = max(globalMaxY, y)
                }
                x += dx
            }
        }
        if (globalMaxX <= globalMinX || globalMaxY <= globalMinY) return

        val margin = 40.0
        val panelWidth = width - 2 * margin
        val panelHeight = height - 2 * margin
        val scaleX = panelWidth / (globalMaxX - globalMinX)
        val scaleY = panelHeight / (globalMaxY - globalMinY)
        scale = min(scaleX, scaleY)
        val centerWorldX = (globalMinX + globalMaxX) / 2
        val centerWorldY = (globalMinY + globalMaxY) / 2
        offsetX = (-centerWorldX * scale).toInt()
        offsetY = (centerWorldY * scale).toInt()
        repaint()
    }

    private fun computePolygonArea(points: List<WorldPoint>): Double {
        var sum = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            sum += p1.x * p2.y - p2.x * p1.y
        }
        return abs(sum) / 2.0
    }

    private fun computePolygonPerimeter(points: List<WorldPoint>): Double {
        var sum = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            sum += hypot(p2.x - p1.x, p2.y - p1.y)
        }
        return sum
    }
}


/**
 * beállítások.
 */
class SettingsDialog(
    owner: Frame?,
    private val settings: GraphSettings,
    private val onSettingsChanged: () -> Unit
) : JDialog(owner, "Beállítások", true) {

    private val cmbTheme = JComboBox(arrayOf("FlatLaf Dark", "FlatLaf Light", "FlatLaf IntelliJ", "System"))
    private val chkGrid = JCheckBox("Rács megjelenítése", settings.showGrid)
    private val chkAxis = JCheckBox("Tengelyek megjelenítése", settings.showAxis)
    private val chkDomain = JCheckBox("Domain-sáv megjelenítése", settings.showDomainArea)
    private val chkZeros = JCheckBox("Zérushelyek megjelenítése", settings.showZeroPoints)
    private val chkIntersections = JCheckBox("Metszéspontok megjelenítése", settings.showIntersections)
    private val spnStep = JSpinner(SpinnerNumberModel(settings.stepForNumericalSearch, 1e-5, 1.0, 0.001))
    private val spnPointSize = JSpinner(SpinnerNumberModel(settings.pointSize, 1, 50, 1))
    private val btnBgColor = JButton("Háttér színe").apply {
        background = settings.backgroundColor
        foreground = Color.WHITE
    }
    private val chkInvert = JCheckBox("Invert színséma", settings.invertColors)
    private val spnLeftPanelWidth = JSpinner(SpinnerNumberModel(settings.leftPanelWidth, 200, 800, 10))
    private val txtDefaultMapping = JTextField(settings.defaultFunctionFormat, 10)

    init {
        layout = BorderLayout()
        val panel = JPanel(GridBagLayout())
        panel.background = Color(50, 50, 50)
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1
        panel.add(JLabel("Téma:").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 1
        cmbTheme.selectedItem = "FlatLaf Dark"
        panel.add(cmbTheme, gbc)

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2
        chkGrid.background = panel.background
        chkGrid.foreground = Color.WHITE
        panel.add(chkGrid, gbc)

        gbc.gridy = 2
        chkAxis.background = panel.background
        chkAxis.foreground = Color.WHITE
        panel.add(chkAxis, gbc)

        gbc.gridy = 3
        chkDomain.background = panel.background
        chkDomain.foreground = Color.WHITE
        panel.add(chkDomain, gbc)

        gbc.gridy = 4
        chkZeros.background = panel.background
        chkZeros.foreground = Color.WHITE
        panel.add(chkZeros, gbc)

        gbc.gridy = 5
        chkIntersections.background = panel.background
        chkIntersections.foreground = Color.WHITE
        panel.add(chkIntersections, gbc)

        gbc.gridy = 6; gbc.gridwidth = 1
        panel.add(JLabel("Numerikus keresés lépés:").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 6
        panel.add(spnStep, gbc)

        gbc.gridx = 0; gbc.gridy = 7
        panel.add(JLabel("Pontméret (zérus/metszés):").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 7
        panel.add(spnPointSize, gbc)

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2
        btnBgColor.addActionListener {
            val col = JColorChooser.showDialog(this, "Háttérszín választása", btnBgColor.background)
            if (col != null) btnBgColor.background = col
        }
        panel.add(btnBgColor, gbc)

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 1
        panel.add(JLabel("Invert színséma:").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 9
        panel.add(chkInvert, gbc)

        gbc.gridx = 0; gbc.gridy = 10; gbc.gridwidth = 1
        panel.add(JLabel("Bal panel szélessége (px):").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 10
        panel.add(spnLeftPanelWidth, gbc)

        gbc.gridx = 0; gbc.gridy = 11; gbc.gridwidth = 1
        panel.add(JLabel("Függvény hozzárendelési szabály:").apply { foreground = Color.WHITE }, gbc)
        gbc.gridx = 1; gbc.gridy = 11
        panel.add(txtDefaultMapping, gbc)

        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val btnOk = JButton("OK")
        val btnCancel = JButton("Mégse")
        bottomPanel.add(btnOk)
        bottomPanel.add(btnCancel)

        btnOk.addActionListener {
            applySettings()
            onSettingsChanged()
            dispose()
        }
        btnCancel.addActionListener { dispose() }

        add(panel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(owner)
    }

    private fun applySettings() {
        when (cmbTheme.selectedItem) {
            "FlatLaf Dark" -> trySetLookAndFeel("com.formdev.flatlaf.FlatDarkLaf")
            "FlatLaf Light" -> trySetLookAndFeel("com.formdev.flatlaf.FlatLightLaf")
            "FlatLaf IntelliJ" -> trySetLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf")
            "System" -> trySetLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }
        settings.showGrid = chkGrid.isSelected
        settings.showAxis = chkAxis.isSelected
        settings.showDomainArea = chkDomain.isSelected
        settings.showZeroPoints = chkZeros.isSelected
        settings.showIntersections = chkIntersections.isSelected
        settings.stepForNumericalSearch = (spnStep.value as Number).toDouble()
        settings.pointSize = (spnPointSize.value as Number).toInt()
        settings.backgroundColor = btnBgColor.background

        settings.invertColors = chkInvert.isSelected
        settings.leftPanelWidth = (spnLeftPanelWidth.value as Number).toInt()
        settings.defaultFunctionFormat = txtDefaultMapping.text
        if (settings.invertColors) {
            settings.backgroundColor = Color.WHITE
            settings.gridColor = Color.LIGHT_GRAY
            settings.axisColor = Color.BLACK
        } else {
            settings.backgroundColor = Color(30, 30, 30)
            settings.gridColor = Color(60, 60, 60)
            settings.axisColor = Color.WHITE
        }
    }

    private fun trySetLookAndFeel(lafClass: String) {
        try { UIManager.setLookAndFeel(lafClass) } catch (_: Exception) { }
        SwingUtilities.updateComponentTreeUI(this)
    }
}



class MainFrame : JFrame("Nagy Függvényábrázoló Példa") {

    private val graphPanel = GraphPanel().apply { preferredSize = Dimension(900, 700) }
    private val fvContainer = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 40, 40)
        border = EmptyBorder(5, 5, 5, 5)
    }
    private val scrollPane = JScrollPane(
        fvContainer,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    ).apply { preferredSize = Dimension(380, 700) }

    private val leftTabbedPane = JTabbedPane().apply {
        addTab("Függvények", JPanel(BorderLayout()).apply {
            border = TitledBorder("Függvények")
            add(scrollPane, BorderLayout.CENTER)
        })
        addTab("Magyarázat", JScrollPane(JTextArea(
            """
            Számítás menete:
            1. Írd be a függvény képletét, pl. f(x)= x^2 - 4.
            2. Állítsd be a domain-t (érvényes tartomány).
            3. Válaszd ki a színt és a vonalvastagságot.
            4. A "Számol & Rajzol" gombbal a függvény kirajzolódik,
               zérushelyek és metszéspontok meghatározása is megtörténik.
            5. Panninggal, zoomolással navigálhatsz.
            6. Az integrál, derivált, számológép funkciók segítségével extra műveletek végezhetők.
            7. A "Polygon Tool" gombgal poligon adatok is lekérhetők.
            """.trimIndent()
        ).apply {
            isEditable = false
            background = Color(40, 40, 40)
            foreground = Color.WHITE
            font = Font("Monospaced", Font.PLAIN, 12)
        }).also { it.border = TitledBorder("Magyarázat") })
    }
    private var leftPanelWidth = 380

    private var splitPane: JSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabbedPane, graphPanel).apply {
        dividerLocation = leftPanelWidth
        isOneTouchExpandable = true
    }

    private val lblCoordinates = JLabel(" ")


    private val btnAddFunction = JButton("+").apply {
        font = Font(font.name, Font.BOLD, 20)
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Új függvény hozzáadása"
    }
    private val btnClearFunctions = JButton("Függvények törlése").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
    }
    private val btnZoomIn = JButton("Zoom In").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Nagyítás"
    }
    private val btnZoomOut = JButton("Zoom Out").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Kicsinyítés"
    }
    private val btnResetView = JButton("Visszaállítás").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Alap nézet visszaállítása"
    }
    private val btnZoomToFit = JButton("Zoom to Fit").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Nézet igazítása"
    }
    private val btnPolygonTool = JToggleButton("Polygon Tool").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Poligon rajzolása (dupla kattintás a lezáráshoz)"
    }
    private val btnIntegrate = JButton("Integrál").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Integrál számítása és árnyékolása"
    }
    private val btnCalculator = JButton("Számológép").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Nyisd meg a számológépet"
    }

    private val btnDerivative = JButton("Derivált").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
        toolTipText = "Derivált számítása és tangentvonal kirajzolása"
    }
    private val btnSettings = JButton("Beállítások").apply {
        background = Color(80, 80, 80)
        foreground = Color.WHITE
    }
    private val btnCalc = JButton("Számol & Rajzol").apply {
        background = Color(100, 100, 100)
        foreground = Color.WHITE
        font = Font(font.name, Font.BOLD, 14)
    }

    private val menuBar = JMenuBar().apply {
        val fileMenu = JMenu("File")
        val miExportPNG = JMenuItem("Export PNG")
        miExportPNG.addActionListener { exportGraphPanel() }
        val miSaveLog = JMenuItem("Eredmény mentése")
        miSaveLog.addActionListener { saveLog() }
        fileMenu.add(miExportPNG)
        fileMenu.add(miSaveLog)
        add(fileMenu)
    }

    private val lblResult = JLabel("<html><i>Eredmények itt</i></html>").apply {
        foreground = Color.LIGHT_GRAY
        preferredSize = Dimension(380, 80)
    }

    private val functionButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        background = Color(40, 40, 40)
        border = TitledBorder("Függvény műveletek")
        add(btnAddFunction)
        add(btnClearFunctions)
    }
    private val viewButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        background = Color(40, 40, 40)
        border = TitledBorder("Nézet vezérlés")
        add(btnZoomIn)
        add(btnZoomOut)
        add(btnResetView)
        add(btnZoomToFit)
        add(btnPolygonTool)
    }
    private val miscButtonsPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        background = Color(40, 40, 40)
        border = TitledBorder("Extra funkciók")
        add(btnSettings)
        add(btnCalc)
        add(btnIntegrate)
        add(btnCalculator)
        add(btnDerivative)
    }
    private val topPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = Color(40, 40, 40)
        border = EmptyBorder(5, 5, 5, 5)
        add(functionButtonsPanel)
        add(viewButtonsPanel)
        add(miscButtonsPanel)
    }

    private val graphSettings = GraphSettings()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        layout = BorderLayout()
        jMenuBar = menuBar

        val leftPanel = JPanel(BorderLayout()).apply {
            background = Color(40, 40, 40)
            add(topPanel, BorderLayout.NORTH)
            add(leftTabbedPane, BorderLayout.CENTER)
            add(lblResult, BorderLayout.SOUTH)
        }
        splitPane.leftComponent = leftPanel
        add(splitPane, BorderLayout.CENTER)

        val statusBar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = Color(60, 60, 60)
            add(lblCoordinates)
        }
        add(statusBar, BorderLayout.SOUTH)

        btnAddFunction.addActionListener {
            addFunctionPanel(graphSettings.defaultFunctionFormat + " x", -10.0, 10.0, randomColor())
        }
        btnClearFunctions.addActionListener {
            fvContainer.removeAll()
            fvContainer.revalidate()
            fvContainer.repaint()
            graphPanel.functionList = emptyList()
            graphPanel.repaint()
        }
        btnCalc.addActionListener { doCalculate() }
        btnSettings.addActionListener {
            val dlg = SettingsDialog(this, graphSettings) {
                splitPane.dividerLocation = graphSettings.leftPanelWidth
                SwingUtilities.updateComponentTreeUI(this)
                graphPanel.repaint()
            }
            dlg.isVisible = true
        }
        btnZoomIn.addActionListener { graphPanel.zoomIn() }
        btnZoomOut.addActionListener { graphPanel.zoomOut() }
        btnResetView.addActionListener { graphPanel.resetView() }
        btnZoomToFit.addActionListener { graphPanel.zoomToFit() }
        btnPolygonTool.addActionListener {
            graphPanel.polygonMode = btnPolygonTool.isSelected
            if (!btnPolygonTool.isSelected) {
                graphPanel.polygonPoints.clear()
                graphPanel.repaint()
            }
        }
        btnIntegrate.addActionListener {
            val visibleFunctions = graphPanel.functionList.filter { it.visible && it.expression != null }
            if (visibleFunctions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nincs megjeleníthető függvény az integráláshoz.", "Hiba", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val functionNames = visibleFunctions.map { it.expressionText }.toTypedArray()
            val functionCombo = JComboBox(functionNames)
            val txtLower = JTextField("-1")
            val txtUpper = JTextField("1")
            val panel = JPanel(GridLayout(3, 2))
            panel.add(JLabel("Függvény:"))
            panel.add(functionCombo)
            panel.add(JLabel("Alsó határ:"))
            panel.add(txtLower)
            panel.add(JLabel("Felső határ:"))
            panel.add(txtUpper)
            val result = JOptionPane.showConfirmDialog(this, panel, "Integrál számítás", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
            if (result == JOptionPane.OK_OPTION) {
                try {
                    val lower = txtLower.text.toDouble()
                    val upper = txtUpper.text.toDouble()
                    if (lower >= upper) {
                        JOptionPane.showMessageDialog(this, "Az alsó határnak kisebbnek kell lennie, mint a felső.", "Hiba", JOptionPane.ERROR_MESSAGE)
                        return@addActionListener
                    }
                    val selectedIndex = functionCombo.selectedIndex
                    val selectedFunction = visibleFunctions[selectedIndex]
                    val expr = selectedFunction.expression!!
                    val area = integrate(expr, lower, upper, 1000)
                    graphPanel.integrationRegion = IntegrationRegion(expr, lower, upper, selectedFunction.lineColor)
                    graphPanel.repaint()
                    JOptionPane.showMessageDialog(this, "Az integrál értéke: %.4f".format(area))
                } catch (ex: Exception) {
                    JOptionPane.showMessageDialog(this, "Hiba: ${ex.message}", "Hiba", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        btnCalculator.addActionListener {
            CalculatorDialog(this).isVisible = true
        }

        btnDerivative.addActionListener {
            val visibleFunctions = graphPanel.functionList.filter { it.visible && it.expression != null }
            if (visibleFunctions.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nincs megjeleníthető függvény a derivált számításához.", "Hiba", JOptionPane.ERROR_MESSAGE)
                return@addActionListener
            }
            val functionNames = visibleFunctions.map { it.expressionText }.toTypedArray()
            val functionCombo = JComboBox(functionNames)
            val txtX = JTextField("0")
            val panel = JPanel(GridLayout(2, 2))
            panel.add(JLabel("Függvény:"))
            panel.add(functionCombo)
            panel.add(JLabel("x érték:"))
            panel.add(txtX)
            val result = JOptionPane.showConfirmDialog(this, panel, "Derivált számítás", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
            if (result == JOptionPane.OK_OPTION) {
                try {
                    val x0 = txtX.text.toDouble()
                    val h = 1e-5
                    val selectedIndex = functionCombo.selectedIndex
                    val selectedFunction = visibleFunctions[selectedIndex]
                    val expr = selectedFunction.expression!!
                    val fPlus = expr.setVariable("x", x0 + h).evaluate()
                    val fMinus = expr.setVariable("x", x0 - h).evaluate()
                    val derivative = (fPlus - fMinus) / (2 * h)
                    val fAtX = expr.setVariable("x", x0).evaluate()
                    JOptionPane.showMessageDialog(this, "A derivált értéke: %.4f".format(derivative))
                    // Tangent vonal: L(x) = f(x0) + f'(x0) * (x - x0)
                    val tangentExprStr = "$fAtX + $derivative*(x - $x0)"
                    val tangentExpr = ExpressionBuilder(tangentExprStr).variable("x").build()
                    // Állítsuk be a tangent vonalat egy új FunctionData-ként
                    val tangentFunction = FunctionData(
                        expressionText = tangentExprStr,
                        domainStart = selectedFunction.domainStart,
                        domainEnd = selectedFunction.domainEnd,
                        lineColor = Color.BLUE,
                        lineStroke = 2f,
                        visible = true,
                        showDomain = false,
                        expression = tangentExpr
                    )
                    graphPanel.tangentLine = tangentFunction
                    graphPanel.repaint()
                } catch (ex: Exception) {
                    JOptionPane.showMessageDialog(this, "Hiba: ${ex.message}", "Hiba", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
        graphPanel.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val centerX = graphPanel.width / 2 + graphPanel.offsetX
                val centerY = graphPanel.height / 2 + graphPanel.offsetY
                val x = (e.x - centerX) / graphPanel.scale.toDouble()
                val y = (centerY - e.y) / graphPanel.scale.toDouble()
                lblCoordinates.text = "x = ${"%.2f".format(x)} , y = ${"%.2f".format(y)}"
            }
        })

        graphPanel.graphSettings = graphSettings

        pack()
        setLocationRelativeTo(null)
    }

    private fun addFunctionPanel(expr: String, domainS: Double, domainE: Double, col: Color) {
        val fpanel = FunctionInputPanel { panelToRemove ->
            fvContainer.remove(panelToRemove)
            fvContainer.revalidate()
            fvContainer.repaint()
        }
        fpanel.txtExpression.text = expr
        fpanel.txtDomainStart.text = domainS.toString()
        fpanel.txtDomainEnd.text = domainE.toString()
        fpanel.btnColor.background = col
        fvContainer.add(fpanel)
        fvContainer.revalidate()
        fvContainer.repaint()
    }

    private fun findZeros(expr: Expression, start: Double, end: Double, step: Double = 0.01): List<Double> {
        val res = mutableListOf<Double>()
        var x = start
        val eps = 1e-7
        var prevVal = expr.setVariable("x", x).evaluate()
        while (x <= end) {
            val currentVal = expr.setVariable("x", x).evaluate()
            if (abs(currentVal) < eps) {
                res.add(x)
            } else if (prevVal * currentVal < 0) {
                res.add(x - (step / 2.0))
            }
            x += step
            prevVal = currentVal
        }
        return res.distinct().sorted()
    }

    private fun findIntersections(
        e1: Expression,
        e2: Expression,
        start: Double,
        end: Double,
        step: Double
    ): List<Pair<Double, Double>> {
        val list = mutableListOf<Pair<Double, Double>>()
        var x = start
        val eps = 1e-7
        var prevVal = e1.setVariable("x", x).evaluate() - e2.setVariable("x", x).evaluate()
        while (x <= end) {
            val diff = e1.setVariable("x", x).evaluate() - e2.setVariable("x", x).evaluate()
            if (abs(diff) < eps) {
                list.add(Pair(x, e1.setVariable("x", x).evaluate()))
            } else if (prevVal * diff < 0) {
                val mid = x - (step / 2.0)
                list.add(Pair(mid, e1.setVariable("x", mid).evaluate()))
            }
            x += step
            prevVal = diff
        }
        return list
    }

    private fun doCalculate() {
        try {
            val dataList = mutableListOf<FunctionData>()
            for (i in 0 until fvContainer.componentCount) {
                val fpanel = fvContainer.getComponent(i) as? FunctionInputPanel ?: continue
                val fd = fpanel.toFunctionData()
                if (fd.expressionText.isBlank()) continue

                val eBuilder = ExpressionBuilder(fd.expressionText).variable("x")
                MainFrame.globalFunctions.forEach { eBuilder.function(it) }
                eBuilder.operator(MainFrame.factorialOperator)
                val exp = eBuilder.build()
                fd.expression = exp
                dataList.add(fd)
            }

            val zeroMap = mutableMapOf<FunctionData, List<Double>>()
            for (fd in dataList) {
                val expr = fd.expression ?: continue
                if (!fd.visible) {
                    zeroMap[fd] = emptyList()
                    continue
                }
                zeroMap[fd] = findZeros(expr, fd.domainStart, fd.domainEnd, graphSettings.stepForNumericalSearch)
            }

            val allIntersections = mutableListOf<Pair<Double, Double>>()
            for (i in dataList.indices) {
                for (j in i + 1 until dataList.size) {
                    val f1 = dataList[i]
                    val f2 = dataList[j]
                    if (!f1.visible || !f2.visible) continue
                    val e1 = f1.expression ?: continue
                    val e2 = f2.expression ?: continue
                    val start = max(f1.domainStart, f2.domainStart)
                    val end = min(f1.domainEnd, f2.domainEnd)
                    if (end > start) {
                        allIntersections.addAll(
                            findIntersections(e1, e2, start, end, graphSettings.stepForNumericalSearch)
                        )
                    }
                }
            }

            graphPanel.functionList = dataList
            graphPanel.zeroPointsMap = zeroMap
            graphPanel.intersectionPoints = allIntersections
            graphPanel.repaint()

            val sb = StringBuilder("<html>")
            for (fd in dataList) {
                sb.append("<b>${fd.expressionText}</b>, domain=[${fd.domainStart}, ${fd.domainEnd}]<br>")
                val zs = zeroMap[fd].orEmpty()
                if (zs.isEmpty()) {
                    sb.append("<i>Nincs zérushely a domainben</i><br>")
                } else {
                    sb.append("Zérushelyek: ${zs.joinToString { "%.4f".format(it) }}<br>")
                }
                sb.append("<br>")
            }
            if (allIntersections.isNotEmpty()) {
                sb.append("<b>Metszéspontok:</b><br>")
                allIntersections.forEach { (mx, my) ->
                    sb.append("x=%.4f, y=%.4f<br>".format(mx, my))
                }
            } else {
                sb.append("<i>Nincsenek metszéspontok vagy nincs legalább 2 látható függvény.</i>")
            }
            sb.append("</html>")
            lblResult.text = sb.toString()

        } catch (ex: Exception) {
            ex.printStackTrace()
            JOptionPane.showMessageDialog(this, "Hiba: ${ex.message}", "Hiba", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun exportGraphPanel() {
        val fileChooser = JFileChooser()
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            if (!file.name.lowercase().endsWith(".png")) {
                file = file.parentFile.resolve(file.name + ".png")
            }
            val image = BufferedImage(graphPanel.width, graphPanel.height, BufferedImage.TYPE_INT_ARGB)
            val g = image.createGraphics()
            graphPanel.paint(g)
            g.dispose()
            try {
                ImageIO.write(image, "png", file)
                JOptionPane.showMessageDialog(this, "Export sikeres!")
            } catch (ex: Exception) {
                JOptionPane.showMessageDialog(this, "Export hiba: ${ex.message}")
            }
        }
    }

    private fun saveLog() {
        val fileChooser = JFileChooser()
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            if (!file.name.lowercase().endsWith(".txt")) {
                file = file.parentFile.resolve(file.name + ".txt")
            }
            try {
                file.writeText(lblResult.text.replace(Regex("<.*?>"), ""))
                JOptionPane.showMessageDialog(this, "Mentés sikeres!")
            } catch (ex: Exception) {
                JOptionPane.showMessageDialog(this, "Mentés hiba: ${ex.message}")
            }
        }
    }

    private fun randomColor(): Color {
        val h = Math.random().toFloat()
        val s = 0.5f + Math.random().toFloat() * 0.5f
        val b = 0.7f + Math.random().toFloat() * 0.3f
        return Color.getHSBColor(h, s, b)
    }

    private fun integrate(expr: Expression, a: Double, b: Double, steps: Int = 1000): Double {
        val h = (b - a) / steps
        var sum = 0.5 * (expr.setVariable("x", a).evaluate() + expr.setVariable("x", b).evaluate())
        for (i in 1 until steps) {
            val x = a + i * h
            sum += expr.setVariable("x", x).evaluate()
        }
        return sum * h
    }

    companion object {
        val globalFunctions = listOf(
            object : Function("abs", 1) {
                override fun apply(values: DoubleArray): Double = abs(values[0])
            },
            object : Function("sign", 1) {
                override fun apply(values: DoubleArray): Double {
                    val v = values[0]
                    return when {
                        v > 0.0 -> 1.0
                        v < 0.0 -> -1.0
                        else -> 0.0
                    }
                }
            },
            object : Function("floor", 1) {
                override fun apply(values: DoubleArray): Double = floor(values[0])
            },
            object : Function("ceil", 1) {
                override fun apply(values: DoubleArray): Double = ceil(values[0])
            },
            object : Function("log10", 1) {
                override fun apply(values: DoubleArray): Double = log10(values[0])
            },
            object : Function("log2", 1) {
                override fun apply(values: DoubleArray): Double = log(values[0], 2.0)
            }
        )

        val factorialOperator = object : Operator("!", 1, true, Operator.PRECEDENCE_POWER + 1) {
            override fun apply(vararg values: Double): Double {
                val x = values[0]
                val n = x.toInt()
                if (n < 0) throw ArithmeticException("Negatív számnak nincs faktoriálisa!")
                var f = 1.0
                for (i in 2..n) f *= i
                return f
            }
        }
    }
}


/**
 * Próbálja beállítani a FlatLaf Dark-ot, ha nem sikerül, akkor a rendszer LAF-ját.
 */
fun trySetGlobalLookAndFeel() {
    try {
        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf")
    } catch (ex: Exception) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) { }
    }
}


fun main() {
    SwingUtilities.invokeLater {
        trySetGlobalLookAndFeel()
        val frame = MainFrame()
        frame.isVisible = true
    }
}
