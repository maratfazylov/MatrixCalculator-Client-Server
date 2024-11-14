import common.IMatrixServer
import common.models.MatrixModel
import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent

class MainWindow(private val server: IMatrixServer) : JFrame("Матричный калькулятор") {
    private val matrixPanels = mutableMapOf<Int, JPanel>()
    private val mainPanel = JPanel(GridBagLayout())
    private val controlPanel = JPanel()
    private val inputField = JTextField(20)
    
    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(800, 600)
        setLocationRelativeTo(null)
        
        setupControlPanel()
        setupMainPanel()
        
        add(JScrollPane(mainPanel), BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)
        
        refreshMatrices()
    }
    
    private fun setupControlPanel() {
        controlPanel.apply {
            layout = FlowLayout()
            
            add(JLabel("Введите: ID для просмотра или 'ID1 ID2' для сложения:"))
            add(inputField)
            
            val actionButton = JButton("Выполнить").apply {
                addActionListener { processInput() }
            }
            add(actionButton)
            
            inputField.addActionListener { processInput() }
        }
    }
    
    private fun setupMainPanel() {
        mainPanel.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
    }
    
    private fun processInput() {
        val input = inputField.text.trim()
        try {
            val numbers = input.split(" ").map { it.toInt() }
            when (numbers.size) {
                1 -> highlightMatrix(numbers[0])
                2 -> addMatrices(numbers[0], numbers[1])
                else -> showError("Введите одно или два числа через пробел")
            }
        } catch (e: NumberFormatException) {
            showError("Введите числовые ID матриц")
        }
        inputField.text = ""
    }
    
    private fun highlightMatrix(id: Int) {
        matrixPanels[id]?.apply {
            background = Color.YELLOW
            Timer(1000) { background = Color.WHITE }.start()
        }
    }
    
    private fun addMatrices(id1: Int, id2: Int) {
        val matrix1 = server.getMatrix(id1)
        val matrix2 = server.getMatrix(id2)
        
        if (matrix1 == null || matrix2 == null) {
            showError("Одна или обе матрицы не найдены")
            return
        }

        val size1 = getMatrixSize(matrix1)
        val size2 = getMatrixSize(matrix2)

        if (size1 != size2) {
            showError("Матрицы разного размера: ${size1.first}x${size1.second} и ${size2.first}x${size2.second}")
            return
        }

        val resultMatrix = MatrixModel(
            id = server.getAllMatrices().maxOfOrNull { it.id }?.plus(1) ?: 1
        )

        val (rows, cols) = size1
        for (i in 1..rows) {
            for (j in 1..cols) {
                val value1 = matrix1.elements[Pair(i, j)] ?: 0.0
                val value2 = matrix2.elements[Pair(i, j)] ?: 0.0
                resultMatrix.elements[Pair(i, j)] = value1 + value2
            }
        }

        if (server.saveMatrix(resultMatrix)) {
            refreshMatrices()
            showInfo("Результат сохранен как матрица #${resultMatrix.id}")
        } else {
            showError("Ошибка при сохранении результата")
        }
    }
    
    private fun refreshMatrices() {
        mainPanel.removeAll()
        matrixPanels.clear()
        
        val matrices = server.getAllMatrices()
        val constraints = GridBagConstraints().apply {
            insets = Insets(5, 5, 5, 5)
            fill = GridBagConstraints.NONE
        }
        
        matrices.forEach { matrix ->
            val panel = createMatrixPanel(matrix)
            matrixPanels[matrix.id] = panel
            
            constraints.gridx = (matrix.id - 1) % 3
            constraints.gridy = (matrix.id - 1) / 3
            mainPanel.add(panel, constraints)
        }
        
        mainPanel.revalidate()
        mainPanel.repaint()
    }
    
    private fun createMatrixPanel(matrix: MatrixModel): JPanel {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Матрица #${matrix.id}"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
            background = Color.WHITE
            
            val matrixPanel = JPanel(GridLayout(
                matrix.elements.keys.maxOfOrNull { it.first } ?: 0,
                matrix.elements.keys.maxOfOrNull { it.second } ?: 0,
                2, 2
            ))
            
            val rows = matrix.elements.keys.maxOfOrNull { it.first } ?: 0
            val cols = matrix.elements.keys.maxOfOrNull { it.second } ?: 0
            
            for (i in 1..rows) {
                for (j in 1..cols) {
                    val value = matrix.elements[Pair(i, j)]
                    val label = JLabel(
                        value?.let { String.format("%.1f", it) } ?: "•",
                        SwingConstants.CENTER
                    )
                    matrixPanel.add(label)
                }
            }
            
            add(matrixPanel, BorderLayout.CENTER)
            preferredSize = Dimension(120, 120)
        }
    }
    
    private fun getMatrixSize(matrix: MatrixModel): Pair<Int, Int> {
        val rows = matrix.elements.keys.maxOfOrNull { it.first } ?: 0
        val cols = matrix.elements.keys.maxOfOrNull { it.second } ?: 0
        return Pair(rows, cols)
    }
    
    private fun showError(message: String) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE)
    }
    
    private fun showInfo(message: String) {
        JOptionPane.showMessageDialog(this, message, "Информация", JOptionPane.INFORMATION_MESSAGE)
    }
} 