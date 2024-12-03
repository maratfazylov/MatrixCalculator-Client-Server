import common.IMatrixServer
import common.models.MatrixModel
import javax.swing.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class MainWindow(private val server: IMatrixServer) : JFrame("Матричный калькулятор") {
    private val matrixPanels = mutableMapOf<Int, JPanel>()
    private val mainPanel = JPanel(GridBagLayout())
    private val selectedMatrices = mutableListOf<Int>()
    
    // Кнопки операций
    private val addButton = JButton("Сложить")
    private val multiplyButton = JButton("Умножить")
    private val transposeButton = JButton("Транспонировать")
    
    init {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        size = Dimension(800, 600)
        setLocationRelativeTo(null)
        
        setupMainPanel()
        setupButtons()
        
        val topPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        topPanel.add(addButton)
        topPanel.add(multiplyButton)
        topPanel.add(transposeButton)
        
        val scrollPane = JScrollPane(mainPanel).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }
        
        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        
        refreshMatrices()
        updateButtonsState()
    }
    
    private fun setupMainPanel() {
        mainPanel.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            layout = GridBagLayout()
        }
    }
    
    private fun setupButtons() {
        addButton.apply {
            isEnabled = false
            addActionListener { addSelectedMatrices() }
        }
        
        multiplyButton.apply {
            isEnabled = false
            addActionListener { multiplySelectedMatrices() }
        }
        
        transposeButton.apply {
            isEnabled = false
            addActionListener { transposeSelectedMatrix() }
        }
    }
    
    private fun updateButtonsState() {
        addButton.isEnabled = selectedMatrices.size == 2
        multiplyButton.isEnabled = selectedMatrices.size == 2
        transposeButton.isEnabled = selectedMatrices.size == 1
    }
    
    private fun updateMatrixPanelColors() {
        matrixPanels.forEach { (id, panel) ->
            panel.background = when {
                id in selectedMatrices -> Color.YELLOW
                else -> Color.WHITE
            }
        }
    }
    
    private fun refreshMatrices() {
        SwingUtilities.invokeLater {
            mainPanel.removeAll()
            matrixPanels.clear()
            
            val matrices = server.getAllMatrices()
            val constraints = GridBagConstraints().apply {
                insets = Insets(5, 5, 5, 5)
                fill = GridBagConstraints.NONE
                weightx = 1.0
                weighty = 1.0
            }
            
            matrices.forEach { matrix ->
                val panel = createMatrixPanel(matrix)
                matrixPanels[matrix.id] = panel
                
                constraints.gridx = (matrix.id - 1) % 4
                constraints.gridy = (matrix.id - 1) / 4
                mainPanel.add(panel, constraints)
            }
            
            constraints.gridx = 0
            constraints.gridy = (matrices.size / 4) + 1
            constraints.weighty = 1.0
            constraints.fill = GridBagConstraints.BOTH
            mainPanel.add(JPanel(), constraints)
            
            mainPanel.revalidate()
            mainPanel.repaint()
        }
    }
    
    private fun createMatrixPanel(matrix: MatrixModel): JPanel {
        return JPanel(BorderLayout()).apply {
            val panelSize = Dimension(150, 150)
            preferredSize = panelSize
            minimumSize = panelSize
            maximumSize = panelSize
            
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
            
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    when {
                        e.clickCount == 2 -> {
                            if (matrix.id in selectedMatrices) {
                                selectedMatrices.remove(matrix.id)
                            } else if (selectedMatrices.size < 2) {
                                selectedMatrices.add(matrix.id)
                            }
                            updateMatrixPanelColors()
                            updateButtonsState()
                        }
                        e.clickCount == 1 -> {
                            if (background != Color.YELLOW) {
                                background = Color.LIGHT_GRAY
                                Timer(1000) { 
                                    background = if (matrix.id in selectedMatrices) 
                                        Color.YELLOW else Color.WHITE
                                    repaint()
                                }.start()
                            }
                        }
                    }
                }
            })
        }
    }
    
    private fun addSelectedMatrices() {
        if (selectedMatrices.size != 2) return
        
        val (id1, id2) = selectedMatrices.take(2)
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
            selectedMatrices.clear()
            refreshMatrices()
            showInfo("Результат сохранен как матрица #${resultMatrix.id}")
        } else {
            showError("Ошибка при сохранении результата")
        }
        
        updateButtonsState()
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
    
    private fun multiplySelectedMatrices() {
        if (selectedMatrices.size != 2) return
        
        // Получаем матрицы в порядке их выбора
        val firstSelectedId = selectedMatrices[0]  // Первая выбранная матрица
        val secondSelectedId = selectedMatrices[1] // Вторая выбранная матрица
        
        val firstMatrix = server.getMatrix(firstSelectedId)
        val secondMatrix = server.getMatrix(secondSelectedId)
        
        if (firstMatrix == null || secondMatrix == null) {
            showError("Одна или обе матрицы не найдены")
            return
        }

        val firstSize = getMatrixSize(firstMatrix)
        val secondSize = getMatrixSize(secondMatrix)

        // Проверка возможности умножения матриц
        if (firstSize.second != secondSize.first) {
            showError("""
                Невозможно умножить матрицы таких размеров:
                Матрица #$firstSelectedId: ${firstSize.first}×${firstSize.second}
                Матрица #$secondSelectedId: ${secondSize.first}×${secondSize.second}
                
                Для умножения матриц число столбцов первой матрицы (${firstSize.second})
                должно быть равно числу строк второй матрицы (${secondSize.first})
            """.trimIndent())
            return
        }

        val resultMatrix = MatrixModel(
            id = server.getAllMatrices().maxOfOrNull { it.id }?.plus(1) ?: 1
        )

        // Умножение матриц в правильном порядке
        for (i in 1..firstSize.first) {        // строки первой матрицы
            for (j in 1..secondSize.second) {   // столбцы второй матрицы
                var sum = 0.0
                for (k in 1..firstSize.second) { // столбцы первой = строки второй
                    val value1 = firstMatrix.elements[Pair(i, k)] ?: 0.0
                    val value2 = secondMatrix.elements[Pair(k, j)] ?: 0.0
                    sum += value1 * value2
                }
                resultMatrix.elements[Pair(i, j)] = sum
            }
        }

        saveAndShowResult(resultMatrix)
    }
    
    private fun transposeSelectedMatrix() {
        if (selectedMatrices.size != 1) return
        
        val id = selectedMatrices.first()
        val matrix = server.getMatrix(id)
        
        if (matrix == null) {
            showError("Матрица не найдена")
            return
        }

        val size = getMatrixSize(matrix)
        val resultMatrix = MatrixModel(
            id = server.getAllMatrices().maxOfOrNull { it.id }?.plus(1) ?: 1
        )

        for (i in 1..size.first) {
            for (j in 1..size.second) {
                val value = matrix.elements[Pair(i, j)] ?: 0.0
                resultMatrix.elements[Pair(j, i)] = value
            }
        }

        saveAndShowResult(resultMatrix)
    }
    
    private fun saveAndShowResult(resultMatrix: MatrixModel) {
        if (server.saveMatrix(resultMatrix)) {
            selectedMatrices.clear()
            refreshMatrices()
            showInfo("Результат сохранен как матрица #${resultMatrix.id}")
        } else {
            showError("Ошибка при сохранении результата")
        }
        
        updateButtonsState()
    }
} 