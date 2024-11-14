import java.io.*
import java.net.Socket
import common.*
import common.models.*
import javax.swing.SwingUtilities

class MatrixClient(private val server: IMatrixServer) {
    
    private var useGui = false  // Флаг для включения/выключения GUI
    
    fun setGuiMode(enabled: Boolean) {
        useGui = enabled
    }
    
    // Обычный вывод в консоль без GUI
    private fun displayMatrixConsole(matrix: MatrixModel) {
        println("Матрица #${matrix.id}:")
        
        val minRow = matrix.elements.keys.minOfOrNull { it.first } ?: 0
        val maxRow = matrix.elements.keys.maxOfOrNull { it.first } ?: 0
        val minCol = matrix.elements.keys.minOfOrNull { it.second } ?: 0
        val maxCol = matrix.elements.keys.maxOfOrNull { it.second } ?: 0
        
        for (i in minRow..maxRow) {
            for (j in minCol..maxCol) {
                val value = matrix.elements[Pair(i, j)]
                print(if (value != null) String.format("%.1f\t", value) else "•\t")
            }
            println()
        }
        
        println("\nСписок элементов:")
        matrix.elements.forEach { (pos, value) ->
            println("(${pos.first}, ${pos.second}) = $value")
        }
        println("\n" + "=".repeat(40) + "\n")
    }
    
    // Метод для отображения одной матрицы (с GUI по запросу)
    fun displayMatrix(matrix: MatrixModel, showGui: Boolean = false) {
        displayMatrixConsole(matrix)
        
        if (useGui && showGui) {
            SwingUtilities.invokeLater {
                MatrixDisplayWindow(matrix).isVisible = true
            }
        }
    }
    
    fun displayAllMatrices() {
        val matrices = server.getAllMatrices()
        println("Найдено матриц: ${matrices.size}\n")
        
        matrices.forEach { matrix ->
            displayMatrix(matrix, false)  // Показываем все матрицы только в консоли
        }
    }

    fun displayMatrixById(id: Int) {
        val matrix = server.getMatrix(id)
        if (matrix != null) {
            displayMatrix(matrix, true)  // Показываем запрошенную матрицу и в GUI тоже
        } else {
            println("Матрица #$id не найдена")
        }
    }

    fun addMatrices(id1: Int, id2: Int): MatrixModel? {
        val matrix1 = server.getMatrix(id1)
        val matrix2 = server.getMatrix(id2)
        
        if (matrix1 == null || matrix2 == null) {
            println("Одна или обе матрицы не найдены")
            return null
        }

        val size1 = getMatrixSize(matrix1)
        val size2 = getMatrixSize(matrix2)

        if (size1 != size2) {
            println("Матрицы разного размера: ${size1.first}x${size1.second} и ${size2.first}x${size2.second}")
            return null
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

        return resultMatrix
    }

    private fun getMatrixSize(matrix: MatrixModel): Pair<Int, Int> {
        val rows = matrix.elements.keys.maxOfOrNull { it.first } ?: 0
        val cols = matrix.elements.keys.maxOfOrNull { it.second } ?: 0
        return Pair(rows, cols)
    }
}
