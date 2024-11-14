import common.models.MatrixModel
import javax.swing.*
import java.awt.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.DefaultTableCellRenderer

class MatrixDisplayWindow(private val matrix: MatrixModel) : JFrame() {
    
    init {
        title = "Матрица #${matrix.id}"
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        
        // Находим размеры матрицы
        val rows = matrix.elements.keys.maxOfOrNull { it.first } ?: 0
        val cols = matrix.elements.keys.maxOfOrNull { it.second } ?: 0
        
        // Создаем модель таблицы
        val tableModel = DefaultTableModel(rows, cols)
        
        // Заполняем данными
        for (i in 1..rows) {
            for (j in 1..cols) {
                val value = matrix.elements[Pair(i, j)]
                tableModel.setValueAt(
                    value?.let { String.format("%.1f", it) } ?: "•",
                    i-1, j-1
                )
            }
        }
        
        // Создаем таблицу
        val table = JTable(tableModel).apply {
            setDefaultEditor(Object::class.java, null) // Делаем таблицу нередактируемой
            setShowGrid(true)
            gridColor = Color.BLACK
            tableHeader.isVisible = false // Скрываем заголовки
            
            // Устанавливаем размеры ячеек
            setRowHeight(40)
            columnModel.columns.toList().forEach { it.preferredWidth = 60 }
            
            // Центрируем значения в ячейках
            val centerRenderer = DefaultTableCellRenderer().apply {
                horizontalAlignment = JLabel.CENTER
            }
            columnModel.columns.toList().forEach { it.cellRenderer = centerRenderer }
        }
        
        // Добавляем таблицу на панель с прокруткой
        add(JScrollPane(table))
        
        // Устанавливаем размер окна
        setSize(cols * 70 + 50, rows * 50 + 50)
        setLocationRelativeTo(null) // Центрируем окно
    }
}

