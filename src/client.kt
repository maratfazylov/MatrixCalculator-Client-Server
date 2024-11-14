import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.BorderLayout
import java.io.*
import java.net.Socket

// Пример класса для клиента
class Client(private val host: String, private val port: Int) {
    private val listeners = mutableListOf<(List<MatrixData>) -> Unit>()

    fun addMessageReceivedListener(listener: (List<MatrixData>) -> Unit) {
        listeners.add(listener)
    }

    fun start() {
        // Запускаем клиента и сразу запрашиваем данные
        requestMatrixData()
    }

    fun requestMatrixData() {
        try {
            val socket = Socket("localhost", 3305)
            val inputStream = ObjectInputStream(socket.getInputStream())

            // Получаем данные о матрицах от сервера
            val matrices = inputStream.readObject() as List<MatrixData>
            notifyListeners(matrices)

            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyListeners(matrices: List<MatrixData>) {
        listeners.forEach { listener ->
            listener(matrices)
        }
    }
}

// Пример класса для данных матрицы
data class MatrixData(val id: Int, val rows: Int, val cols: Int, val value: String) : Serializable